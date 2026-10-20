"""26.1.2 -> 26.2: mechanical rewrites of the Java sources (run from the repo root; running it again changes nothing).

- the screen and the HUD live in Minecraft.gui: mc.screen -> mc.gui.screen(), mc.setScreen(x) -> mc.gui.setScreen(x),
  options.hideGui -> gui.hud (isHidden / MOClientCompat.setHudHidden);
- entity type constants in EntityTypes; BlockPos.getCenter / getBottomCenter -> Vec3.atCenterOf / atBottomCenterOf;
- coloured and copper block collections (Items.CONCRETE.white(), Items.CHISELED_COPPER.waxed().oxidized());
- GameRenderer accessors (mainCamera, mainRenderTarget, renderBuffers, featureRenderDispatcher), LightCoordsUtil,
  Language.has, the uniform / alt font ids, team colours.
"""
import pathlib
import re

MC = r'(\b(?:mc|minecraft|this\.minecraft|Minecraft\.getInstance\(\)|net\.minecraft\.client\.Minecraft\.getInstance\(\)))'
RENAMES = [
    (MC + r'\.screen\b(?!\()', r'\1.gui.screen()'),
    (MC + r'\.setScreen\(', r'\1.gui.setScreen('),
    (MC + r'\.options\.hideGui = (true|false)', r'matteroverdrive.client.MOClientCompat.setHudHidden(\1, \2)'),
    (MC + r'\.options\.hideGui\b', r'\1.gui.hud.isHidden()'),
    (r'\bnet\.minecraft\.world\.entity\.EntityType\.([A-Z][A-Z0-9_]+)\b', r'net.minecraft.world.entity.EntityTypes.\1'),
    (r'(?<![.\w])EntityType\.([A-Z][A-Z0-9_]+)\b', r'EntityTypes.\1'),
    (r'([\w.]+(?:\([^()]*\))?)\.getCenter\(\)', r'net.minecraft.world.phys.Vec3.atCenterOf(\1)'),
    (r'([\w.]+(?:\([^()]*\))?)\.getBottomCenter\(\)', r'net.minecraft.world.phys.Vec3.atBottomCenterOf(\1)'),
    (r'\b(Items|Blocks)\.WAXED_OXIDIZED_CHISELED_COPPER\b', r'\1.CHISELED_COPPER.waxed().oxidized()'),
    (r'\b(Items|Blocks)\.OXIDIZED_CHISELED_COPPER\b', r'\1.CHISELED_COPPER.weathering().oxidized()'),
    (r'\b(Items|Blocks)\.CHISELED_COPPER\b(?!\.)', r'\1.CHISELED_COPPER.weathering().unaffected()'),
    (r'\b(Items|Blocks)\.WHITE_CONCRETE_POWDER\b', r'\1.CONCRETE_POWDER.white()'),
    (r'\b(Items|Blocks)\.WHITE_CONCRETE\b', r'\1.CONCRETE.white()'),
    (r'\.getMainCamera\(\)', '.mainCamera()'),
    (r'\.getMainRenderTarget\(\)', '.mainRenderTarget()'),
    (r'\bmc\.renderBuffers\(\)', 'mc.gameRenderer.renderBuffers()'),
    (r'\.getFeatureRenderDispatcher\(\)', '.featureRenderDispatcher()'),
    (r'net\.minecraft\.client\.renderer\.LevelRenderer\.getLightCoords\(', 'net.minecraft.util.LightCoordsUtil.getLightCoords('),
    (r'\bLevelRenderer\.getLightCoords\(', 'net.minecraft.util.LightCoordsUtil.getLightCoords('),
    (r'net\.minecraft\.client\.resources\.language\.I18n\.exists\(', 'net.minecraft.locale.Language.getInstance().has('),
    (r'\bMinecraft\.UNIFORM_FONT\b', 'net.minecraft.resources.Identifier.withDefaultNamespace("uniform")'),
    (r'\bMinecraft\.ALT_FONT\b', 'net.minecraft.resources.Identifier.withDefaultNamespace("alt")'),
    (r'\.setColor\(net\.minecraft\.ChatFormatting\.(\w+)\)', r'.setColor(java.util.Optional.of(net.minecraft.world.scores.TeamColor.\1))'),
    (r'\.markPosForPostprocessing\(', '.markPosForPostProcessing('),
]

changed = 0
for base in ['common/src/main/java', 'neoforge/src', 'fabric/src']:
    for path in pathlib.Path(base).rglob('*.java'):
        text = path.read_text(encoding='utf-8')
        new = text
        for pattern, repl in RENAMES:
            new = re.sub(pattern, repl, new)
        if re.search(r'(?<![.\w])EntityTypes\.', new) and 'import net.minecraft.world.entity.EntityTypes;' not in new:
            new = re.sub(r'(import net\.minecraft\.world\.entity\.EntityType;\n)', r'\1import net.minecraft.world.entity.EntityTypes;\n', new, count=1)
            if 'import net.minecraft.world.entity.EntityTypes;' not in new:
                new = re.sub(r'^(package [\w.]+;\n)', r'\1\nimport net.minecraft.world.entity.EntityTypes;\n', new, count=1, flags=re.M)
        if new != text:
            path.write_text(new, encoding='utf-8')
            changed += 1
print(changed, 'files changed')
