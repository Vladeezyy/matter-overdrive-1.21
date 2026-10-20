"""1.21.1 -> 26.1.2: mechanical rewrites of the Java sources (run from the repo root, again after each compile round).

    python3 tools/port/port_26.py <vanilla 26.1.2 sources dir> [neoforge sources dir]

- imports of Minecraft classes that moved: the new package of the same top-level class name, when it is unique in the
  26.1.2 sources;
- renames: ResourceLocation -> Identifier, MobSpawnType -> EntitySpawnReason, ItemInteractionResult -> InteractionResult
  (PASS_TO_DEFAULT_BLOCK_INTERACTION -> TRY_WITH_EMPTY_HAND, SKIP_DEFAULT_BLOCK_INTERACTION -> PASS), net.minecraft.Util,
  getMinBuildHeight/getMaxBuildHeight, MethodsReturnNonnullByDefault (gone).
Running it again changes nothing.
"""
import os
import pathlib
import re
import sys

SRC = pathlib.Path(sys.argv[1])
EXTRA = [pathlib.Path(p) for p in sys.argv[2:]]

index = {}
existing = set()
for root in [SRC] + EXTRA:
    for path in root.rglob('*.java'):
        rel = path.relative_to(root).with_suffix('')
        fqn = '.'.join(rel.parts)
        existing.add(fqn)
        index.setdefault(rel.parts[-1], []).append(fqn)


def exists(fqn):
    # a class or a nested class of one
    parts = fqn.split('.')
    for i in range(len(parts), 0, -1):
        if '.'.join(parts[:i]) in existing:
            return True
    return False


RENAMES = [
    (r'\bResourceLocation\b', 'Identifier'),
    (r'\bMobSpawnType\b', 'EntitySpawnReason'),
    (r'\bItemInteractionResult\.PASS_TO_DEFAULT_BLOCK_INTERACTION\b', 'InteractionResult.TRY_WITH_EMPTY_HAND'),
    (r'\bItemInteractionResult\.SKIP_DEFAULT_BLOCK_INTERACTION\b', 'InteractionResult.PASS'),
    (r'\bItemInteractionResult\.sidedSuccess\([^()]*\)', 'InteractionResult.SUCCESS'),
    (r'\bItemInteractionResult\b', 'InteractionResult'),
    (r'\bInteractionResult\.sidedSuccess\([^()]*\)', 'InteractionResult.SUCCESS'),
    (r'import net\.minecraft\.Util;', 'import net.minecraft.util.Util;'),
    (r'\bgetMinBuildHeight\(\)', 'getMinY()'),
    (r'\bgetMaxBuildHeight\(\)', 'getMaxY()'),
    (r'\n@MethodsReturnNonnullByDefault\n', '\n'),
    (r'@MethodsReturnNonnullByDefault\s*', ''),
    (r'import net\.minecraft\.MethodsReturnNonnullByDefault;\n', ''),
    # classes renamed in 1.21.2 - 26.1 (the import step then finds the new package)
    (r'\bFarmBlock\b', 'FarmlandBlock'),
    (r'\bWaterlilyBlock\b', 'LilyPadBlock'),
    (r'\bDimensionTransition\b', 'TeleportTransition'),
    (r'\bWeightedRandomList\b', 'WeightedList'),
    (r'\bUseAnim\b', 'ItemUseAnimation'),
    (r'\bClickType\b', 'ContainerInput'),
    (r'\bDimensionDataStorage\b', 'SavedDataStorage'),
    (r'\bGuiGraphics\b', 'GuiGraphicsExtractor'),
    (r'\bFastColor\.ARGB32\.', 'ARGB.'),
    (r'import net\.minecraft\.util\.FastColor;', 'import net.minecraft.util.ARGB;'),
    # 1.21.2 - 26.1 API renames
    (r'\.getType\(\)\.is\((?=[\w.]*(?:EntityTypeTags|EntityTags)\.)', '.is('),
    (r'\bgetRestrictCenter\(', 'getHomePosition('),
    (r'\brestrictTo\(', 'setHomeTo('),
    (r'\bhasRestriction\(', 'hasHome('),
    (r'\bgetRestrictRadius\(', 'getHomeRadius('),
    (r'\bisWithinRestriction\(', 'isWithinHome('),
    (r'\bInteractionResult\.sidedSuccess\((?:[^()]|\([^()]*\))*\)', 'InteractionResult.SUCCESS'),
    (r'new ItemParticleOption\(ParticleTypes\.ITEM, new ItemStack\(([^()]+(?:\([^()]*\))?)\)\)', r'new ItemParticleOption(ParticleTypes.ITEM, \1)'),
    (r'new ItemParticleOption\(ParticleTypes\.ITEM, (\w+)\)', r'new ItemParticleOption(ParticleTypes.ITEM, \1.getItem())'),
    # hurt overrides: the server half (1.21.2); the client half of 1.21.1's hurt only returned false
    (r'public boolean hurt\(DamageSource (\w+), float (\w+)\)', r'public boolean hurtServer(ServerLevel level, DamageSource \1, float \2)'),
    (r'super\.hurt\((\w+), (\w+)\)', r'super.hurtServer(level, \1, \2)'),
]

changed = 0
for base in ['common/src/main/java', 'common/src/selftest/java', 'neoforge/src', 'fabric/src']:
    for path in pathlib.Path(base).rglob('*.java'):
        text = path.read_text(encoding='utf-8')
        new = text
        for pattern, repl in RENAMES:
            new = re.sub(pattern, repl, new)

        def relocate(match):
            static, fqn = match.group(1) or '', match.group(2)
            if not fqn.startswith(('net.minecraft.', 'com.mojang.')) or exists(fqn.rstrip('.*') if fqn.endswith('.*') else fqn):
                return match.group(0)
            parts = fqn.split('.')
            # the first part that is a class name (upper case), the rest are nested names or a static member
            for i, part in enumerate(parts):
                if part[:1].isupper():
                    candidates = [c for c in index.get(part, []) if c.startswith(('net.minecraft.', 'com.mojang.'))]
                    if len(candidates) == 1:
                        return 'import ' + static + '.'.join([candidates[0]] + parts[i + 1:]) + ';'
                    break
            return match.group(0)

        new = re.sub(r'import (static )?([\w.]+(?:\.\*)?);', relocate, new)
        # an Identifier import that the rename produced from net.minecraft.resources.ResourceLocation is right already
        if new != text:
            path.write_text(new, encoding='utf-8')
            changed += 1
print(changed, 'files changed')
