"""26.2 -> 26.3: mechanical rewrites of the Java sources (run from the repo root; running it again changes nothing).

- blocks have no codec(): the CODEC field and the codec() override go (block/ only; processors and loot modifiers
  keep theirs);
- render pipeline classes moved to com.mojang.renderpearl.api.pipeline; PoseStack.mulPose(quaternion) -> rotate;
- entities: invulnerableTime / hurtMarked accessors, PushReaction names, getStructureTemplateManager;
- keys: InputConstants.Type.KEYBOARD.
"""
import pathlib
import re

RENAMES = [
    (r'com\.mojang\.blaze3d\.pipeline\.(RenderPipeline|BlendFunction|ColorTargetState|DepthStencilState|BlendEquation)\b',
     r'com.mojang.renderpearl.api.pipeline.\1'),
    (r'com\.mojang\.blaze3d\.platform\.(BlendFactor|CompareOp|PolygonMode)\b', r'com.mojang.renderpearl.api.pipeline.\1'),
    (r'com\.mojang\.blaze3d\.PrimitiveTopology\b', 'com.mojang.renderpearl.api.pipeline.PrimitiveTopology'),
    # quaternion rotations (Axis.XP.rotationDegrees, new Quaternionf, orientations); matrices keep mulPose
    (r'\.mulPose\((Axis\.\w+\.rotation(?:Degrees)?\(|new Quaternionf\(|new org\.joml\.Quaternionf\(|com\.mojang\.math\.Axis\.)', r'.rotate(\1'),
    (r'\bPushReaction\.DESTROY\b', 'PushReaction.POPPED'),
    (r'\bPushReaction\.NORMAL\b', 'PushReaction.PUSH_PULL'),
    (r'\bPushReaction\.BLOCK\b', 'PushReaction.IMMOVEABLE'),
    (r'\.getStructureManager\(\)', '.getStructureTemplateManager()'),
    (r'InputConstants\.Type\.KEYSYM\b', 'InputConstants.Type.KEYBOARD'),
    # hurtMarked (velocity sent to the client next tick) is syncVelocity
    (r'\.hurtMarked = true', '.syncVelocity = true'),
    (r'(\w+)\.invulnerableTime = ([^;]+);', r'\1.setInvulnerableTime(\2);'),
    (r'(\w+)\.invulnerableTime\b', r'\1.getInvulnerableTime()'),
]

changed = 0
for base in ['common/src/main/java', 'neoforge/src', 'fabric/src']:
    for path in pathlib.Path(base).rglob('*.java'):
        text = path.read_text(encoding='utf-8')
        new = text
        for pattern, repl in RENAMES:
            new = re.sub(pattern, repl, new)
        if '/block/' in path.as_posix() and 'extends' in new:
            new = re.sub(r'\n    public static final MapCodec<\w+> CODEC = simpleCodec\([^;]*\);\n', '\n', new)
            new = re.sub(r'\n    @Override\n    protected MapCodec<[\w<>? ]+> codec\(\) \{\n        return CODEC;\n    \}\n', '\n', new)
            if 'MapCodec' not in new.replace('import com.mojang.serialization.MapCodec;', ''):
                new = new.replace('import com.mojang.serialization.MapCodec;\n', '')
        if new != text:
            path.write_text(new, encoding='utf-8')
            changed += 1
print(changed, 'files changed')
