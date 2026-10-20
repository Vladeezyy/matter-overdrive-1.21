"""Methods that stopped overriding anything (found by tools/port/OverrideCheck.java), per Minecraft version:
    python3 tools/port/fix_lost_overrides.py 1.21.1|26
- getCloneItemStack of five blocks: NeoForge 1.21.1's signature (state, hit, level, pos, player); vanilla's is
  (level, pos, state) in 1.21.1 and (level, pos, state, includeData) in 26.x, which Fabric (and NeoForge 26.x) call.
- ThrownBlock.canBeCollidedWith(): 26.x takes the other entity.
Safe to run again."""
import pathlib
import re
import sys

version = sys.argv[1]
root = pathlib.Path(__file__).resolve().parent.parent.parent / 'common/src/main/java/com/github/alexthe666/rats'

for name in ('CheeseCauldronBlock', 'MilkCauldronBlock', 'RatCageDecoratedBlock', 'RatTubeBlock', 'RatHoleBlock'):
    p = root / 'server/block' / (name + '.java')
    s = p.read_text()
    m = re.search(r'(\t@Override\n)?\tpublic ItemStack getCloneItemStack\(BlockState state, HitResult target, LevelReader (\w+), BlockPos pos, Player player\) \{', s)
    if not m and version != '1.21.1':
        # already the 1.21.1 vanilla override (merged from main)
        m = re.search(r'(\t@Override\n)?\tpublic ItemStack getCloneItemStack\(LevelReader (\w+), BlockPos pos, BlockState state\) \{', s)
    if m:
        level = m.group(2)
        if version == '1.21.1':
            new = f'\t@Override\n\tpublic ItemStack getCloneItemStack(LevelReader {level}, BlockPos pos, BlockState state) {{'
        else:
            new = f'\t@Override\n\tprotected ItemStack getCloneItemStack(LevelReader {level}, BlockPos pos, BlockState state, boolean includeData) {{'
        s = s.replace(m.group(0), new)
        p.write_text(s)

if version != '1.21.1':
    p = root / 'server/entity/projectile/ThrownBlock.java'
    s = p.read_text()
    s = s.replace('\tpublic boolean canBeCollidedWith() {', '\t@Override\n\tpublic boolean canBeCollidedWith(@org.jetbrains.annotations.Nullable Entity other) {')
    p.write_text(s)
