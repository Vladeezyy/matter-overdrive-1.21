"""1.21.1 -> 1.20.1: mechanical rewrites of the Java sources (run from the repo root; running it again changes nothing).

- the 1.20.5+ network codecs, payloads and data component types -> the stand-ins in matteroverdrive.compat.codec /
  matteroverdrive.compat.component;
- stack.get/set/has/remove/getOrDefault/update(MODataComponents.X.get(), ...) -> Components.get(stack, ...), since a
  1.20.1 ItemStack has no components (values live in its NBT);
- ResourceLocation factories -> new ResourceLocation.
"""
import pathlib
import re

COMPAT = 'matteroverdrive.compat'
RENAMES = [
    (r'\bnet\.minecraft\.network\.codec\.(StreamCodec|ByteBufCodecs|StreamDecoder|StreamEncoder|StreamMemberEncoder)\b', COMPAT + r'.codec.\1'),
    (r'\bnet\.minecraft\.network\.RegistryFriendlyByteBuf\b', COMPAT + '.codec.RegistryFriendlyByteBuf'),
    (r'\bnet\.minecraft\.network\.protocol\.common\.custom\.CustomPacketPayload\b', COMPAT + '.codec.CustomPacketPayload'),
    (r'\bnet\.minecraft\.core\.component\.(DataComponentType|DataComponentMap)\b', COMPAT + r'.component.\1'),
    (r'(?<![\w.])(?:net\.minecraft\.world\.item\.)?ItemStack\.OPTIONAL_STREAM_CODEC\b', COMPAT + '.codec.VanillaCodecs.ITEM_STACK_OPTIONAL'),
    (r'(?<![\w.])(?:net\.minecraft\.world\.item\.)?ItemStack\.OPTIONAL_LIST_STREAM_CODEC\b', COMPAT + '.codec.VanillaCodecs.ITEM_STACK_OPTIONAL_LIST'),
    (r'(?<![\w.])(?:net\.minecraft\.world\.item\.)?ItemStack\.STREAM_CODEC\b', COMPAT + '.codec.VanillaCodecs.ITEM_STACK'),
    (r'(?<![\w.])(?:net\.minecraft\.core\.)?BlockPos\.STREAM_CODEC\b', COMPAT + '.codec.VanillaCodecs.BLOCK_POS'),
    (r'(?<![\w.])(?:net\.minecraft\.core\.)?GlobalPos\.STREAM_CODEC\b', COMPAT + '.codec.VanillaCodecs.GLOBAL_POS'),
    (r'(?<![\w.])(?:net\.minecraft\.core\.)?UUIDUtil\.STREAM_CODEC\b', COMPAT + '.codec.VanillaCodecs.UUID_CODEC'),
    (r'(?<![\w.])(?:net\.minecraft\.world\.item\.crafting\.)?Ingredient\.CONTENTS_STREAM_CODEC\b', COMPAT + '.codec.VanillaCodecs.INGREDIENT'),
    (r'\bnet\.minecraft\.client\.DeltaTracker\b', COMPAT + '.DeltaTracker'),
    # tooltips get the level (1.20.5+: Item.TooltipContext)
    (r'(appendHoverText\(ItemStack \w+, )(?:Item\.)?TooltipContext (\w+)', r'\1@org.jetbrains.annotations.Nullable net.minecraft.world.level.Level \2'),
    (r'\.getTooltipLines\((?:net\.minecraft\.world\.item\.)?Item\.TooltipContext\.of\([^)]*\), ', '.getTooltipLines('),
    # entity data is defined on the entity itself (1.20.5+: a builder)
    (r'defineSynchedData\((?:net\.minecraft\.network\.syncher\.)?SynchedEntityData\.Builder \w+\)', 'defineSynchedData()'),
    (r'super\.defineSynchedData\(builder\)', 'super.defineSynchedData()'),
    (r'(?<![\w.])builder\.define\(', 'this.entityData.define('),
    (r'\bnet\.minecraft\.world\.entity\.SpawnPlacementTypes\.(\w+)', r'net.minecraft.world.entity.SpawnPlacements.Type.\1'),
    (r'\bimport net\.minecraft\.world\.entity\.SpawnPlacementType;', 'import net.minecraft.world.entity.SpawnPlacements;'),
    (r'(?<![\w.])SpawnPlacementType\b', 'SpawnPlacements.Type'),
    (r'\bTransparentBlock\b', 'GlassBlock'),
    (r'((?:\w+\.)*\w+(?:\(\))?)\.damageSources\(\)\.source\(', COMPAT + r'.Damage.source(\1, '),
    (r'(?:net\.minecraft\.core\.)?Holder<((?:net\.minecraft\.world\.entity\.ai\.attributes\.)?Attribute|(?:net\.minecraft\.world\.effect\.)?MobEffect)>', r'\1'),
    (r'\bnew AttributeModifier\(', COMPAT + '.Attrs.modifier('),
    (r'(?<![\w.])(\w+)\.addOrUpdateTransientModifier\(', COMPAT + r'.Attrs.addOrUpdate(\1, '),
    (r'(?<![\w.])(?!Stacks\b)(\w+)\.transmuteCopy\(', COMPAT + r'.Stacks.transmuteCopy(\1, '),
    (r'\bhitTargetOrDeflectSelf\(', 'onHit('),
    (r'\.usingConvertsTo\([^)]*\)', ''),
    (r'\b(\w+)\.save\((?:provider|registries|output\.lookup\(\)|[\w.]*registryAccess\(\)), (\w+)\)', r'\1.save(\2)'),
    (r'ItemStack\.parseOptional\([^,()]+(?:\(\))?, (\w+)\)', r'ItemStack.of(\1)'),
    (r'ItemStack\.parse\([^,()]+(?:\(\))?, (\w+)\)', r'java.util.Optional.of(ItemStack.of(\1)).filter(s -> !s.isEmpty())'),
    (r'getUseDuration\((\w+(?:\.\w+\(\))?), (?:\w+(?:\.\w+\(\))?)\)', r'getUseDuration(\1)'),
    (r'new DropExperienceBlock\((UniformInt\.of\(\d+, \d+\)), (\w+)\)', r'new DropExperienceBlock(\2, \1)'),
    (r'\.getDefaultDimensions\((\w+)\)', r'.getDimensions(\1)'),
    (r'\.getComponentsPatch\(\)', '.getTag()'),
    (r'\.getComponents\(\)', '.getTag()'),
    (r'\.saturationModifier\(', '.saturationMod('),
    (r'\.alwaysEdible\(\)', '.alwaysEat()'),
    (r'\.igniteForSeconds\((.+?)\);', r'.setSecondsOnFire((int) (\1));'),
    (r'\.hasInfiniteMaterials\(\)', '.getAbilities().instabuild'),
    (r'AttributeModifier\.Operation\.ADD_VALUE\b', 'AttributeModifier.Operation.ADDITION'),
    (r'AttributeModifier\.Operation\.ADD_MULTIPLIED_BASE\b', 'AttributeModifier.Operation.MULTIPLY_BASE'),
    (r'AttributeModifier\.Operation\.ADD_MULTIPLIED_TOTAL\b', 'AttributeModifier.Operation.MULTIPLY_TOTAL'),
    (r'case ADD_VALUE ->', 'case ADDITION ->'),
    (r'case ADD_MULTIPLIED_BASE ->', 'case MULTIPLY_BASE ->'),
    (r'case ADD_MULTIPLIED_TOTAL ->', 'case MULTIPLY_TOTAL ->'),
    (r'(\w+)\.removeLast\(\)', r'\1.remove(\1.size() - 1)'),
    (r'\bsaveCustomOnly\(registries\)', 'saveWithoutMetadata()'),
    (r'(\w+)\.<([\w.]+)>getBlockEntity\(', COMPAT + r'.GameTests.<\2>blockEntity(\1, '),
    (r'JsonParser\.parseReader\(', 'JsonParser.parseReader('),
    (r'\.ifSuccess\(', '.result().ifPresent('),
    (r'isCorrectToolForDrops\(ItemStack \w+, BlockState (\w+)\)', r'isCorrectToolForDrops(BlockState \1)'),
    (r'\bhelper\.makeMockServerPlayerInLevel\(\)', COMPAT + '.GameTests.mockServerPlayer(helper)'),
    (r'\.withCallback\(\((\w+), (\w+)\) ->', r'.withCallback((context, \1, \2) ->'),
    (r'(ValueOutput|ValueInput)\.of\(tag, registryAccess\(\)\)', r'\1.of(tag, level().registryAccess())'),
    (r'\.getBottomCenter\(\)', '.getCenter().subtract(0, 0.5, 0)'),
    (r'(\w+)\.canInteractWithBlock\(([^,]+), (\d+)\)', r'\1.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(\2)) <= (\3 + 4.5) * (\3 + 4.5)'),
    (r'\bMath\.clamp\(', 'net.minecraft.util.Mth.clamp('),
    (r'\.isSameItemSameComponents\(', '.isSameItemSameTags('),
    (r'\.getOrThrow\(\)', '.getOrThrow(false, e -> {})'),
    (r'\bTesselator\.getInstance\(\)\.begin\(', COMPAT + '.render.Vertex.begin('),
    (r'\.buildOrThrow\(\)', '.end()'),
    # screens: one scroll amount, and the background is drawn by the screen itself
    (r'mouseScrolled\(double (\w+), double (\w+), double \w+, double (\w+)\)', r'mouseScrolled(double \1, double \2, double \3)'),
    (r'mouseScrolled\((\w+), (\w+), \w+, (\w+)\)', r'mouseScrolled(\1, \2, \3)'),
    (r'renderBackground\(GuiGraphics (\w+), int \w+, int \w+, float \w+\)', r'renderBackground(GuiGraphics \1)'),
    (r'renderBackground\((\w+), \w+, \w+, \w+\)', r'renderBackground(\1)'),
    # blocks: use() with the hand
    (r'protected InteractionResult useWithoutItem\(BlockState (\w+), Level (\w+), BlockPos (\w+), Player (\w+), BlockHitResult (\w+)\)',
     r'public InteractionResult use(BlockState \1, Level \2, BlockPos \3, Player \4, net.minecraft.world.InteractionHand hand, BlockHitResult \5)'),
    (r'super\.useWithoutItem\((\w+), (\w+), (\w+), (\w+), (\w+)\)', r'super.use(\1, \2, \3, \4, hand, \5)'),
    (r'getUseDuration\(ItemStack (\w+), LivingEntity \w+\)', r'getUseDuration(ItemStack \1)'),
    (r'getUpdateTag\((?:net\.minecraft\.core\.)?HolderLookup\.Provider \w+\)', 'getUpdateTag()'),
    (r'\.saveCustomOnly\(\w+\)', '.saveWithoutMetadata()'),
    (r'getDefaultDimensions\(Pose (\w+)\)', r'getDimensions(Pose \1)'),
    (r'public void setByPlayer\(ItemStack (\w+), ItemStack \w+\)', r'public void setByPlayer(ItemStack \1)'),
    (r'\b(\w+)\.createSerializationContext\(([^()]*(?:\([^()]*\))?)\)', r'net.minecraft.resources.RegistryOps.create(\2, \1)'),
    (r'(?<![\w.])(?:net\.minecraft\.resources\.)?ResourceLocation\.(?:fromNamespaceAndPath|withDefaultNamespace|parse)\(', 'new net.minecraft.resources.ResourceLocation('),
]

COMPONENT_CALL = re.compile(r'\.(get|set|has|remove|getOrDefault|update)\(\s*((?:matteroverdrive\.init\.)?MODataComponents\.\w+\.get\(\))')
# DataComponentGetter / DataComponentMap.Builder receivers keep their own methods
SKIP_RECEIVERS = re.compile(r'(?:^|\.)(components|input|collectComponents\(\))$')


def receiver_start(text, end):
    """Start index of the expression that ends right before `end` (the '.' of the call)."""
    i = end
    while i > 0:
        c = text[i - 1]
        if c in ')]':
            depth, open_c = 0, '(' if c == ')' else '['
            j = i - 1
            while j >= 0:
                if text[j] in ')]':
                    depth += 1
                elif text[j] in '([':
                    depth -= 1
                    if depth == 0:
                        break
                j -= 1
            i = j
        elif c.isalnum() or c in '_.':
            i -= 1
        elif c in ' \n\t' and text[i] == '.':
            # chained call on a new line: ".get(...)\n        .set(" - keep going over the whitespace
            k = i - 1
            while k > 0 and text[k - 1] in ' \n\t':
                k -= 1
            if text[k - 1] in ')]' or text[k - 1].isalnum():
                i = k
            else:
                break
        else:
            break
    # a leading "new Foo(...)" belongs to the receiver
    m = re.search(r'new\s+$', text[:i])
    if m:
        i = m.start()
    return i


def rewrite_vertices(text):
    """vc.addVertex(...).setX(...)...; -> Vertex.of(vc, ...).setX(...)....end();"""
    out, pos = [], 0
    for m in re.finditer(r'(?<![\w.])(\w+)\.addVertex\(', text):
        if m.start() < pos:
            continue
        depth, i = 0, m.end() - 1
        while i < len(text):
            c = text[i]
            if c in '([{':
                depth += 1
            elif c in ')]}':
                depth -= 1
            elif c == ';' and depth == 0:
                break
            i += 1
        out.append(text[pos:m.start()])
        out.append(f'{COMPAT}.render.Vertex.of({m.group(1)}, ' + text[m.end():i].rstrip() + '.end()')
        pos = i
    out.append(text[pos:])
    return ''.join(out)


def rewrite_components(text):
    out, pos = [], 0
    for m in COMPONENT_CALL.finditer(text):
        dot = m.start()
        if dot < pos:
            continue
        start = receiver_start(text, dot)
        receiver = text[start:dot].strip()
        if not receiver or SKIP_RECEIVERS.search(receiver) or receiver.endswith('Components'):
            continue
        out.append(text[pos:start])
        out.append(f'{COMPAT}.component.Components.{m.group(1)}({receiver}, {m.group(2)}')
        pos = m.end()
    out.append(text[pos:])
    return ''.join(out)


changed = 0
for base in ['common/src/main/java', 'forge/src', 'fabric/src']:
    for path in pathlib.Path(base).rglob('*.java'):
        if '/matteroverdrive/compat/' in path.as_posix():
            continue  # the stand-ins themselves
        text = path.read_text(encoding='utf-8')
        new = text
        for pattern, repl in RENAMES:
            new = re.sub(pattern, repl, new)
        new = rewrite_components(new)
        new = rewrite_vertices(new)
        if ('/block/' in path.as_posix() or path.name.endswith('Block.java')) and 'extends' in new:
            # 1.20.1 BlockBehaviour methods are public
            new = re.sub(r'protected ((?:[\w.]+ )?)(rotate|mirror|getRenderShape|onRemove|neighborChanged|getShape|getCollisionShape|updateShape|tick|randomTick|onPlace|hasAnalogOutputSignal|getAnalogOutputSignal|canSurvive|getVisualShape|getOcclusionShape|skipRendering|getShadeBrightness|propagatesSkylightDown|entityInside|getSignal|getDirectSignal|isSignalSource|getMenuProvider|triggerEvent|useShapeForLightOcclusion|getLightBlock|attack|isPathfindable|getFluidState|getInteractionShape|getBlockSupportShape)\(', r'public \1\2(', new)
            new = re.sub(r'\n    public static final MapCodec<\w+> CODEC = simpleCodec\([^;]*\);\n', '\n', new)
            new = re.sub(r'\n    @Override\n    protected MapCodec<[\w<>? ]+> codec\(\) \{\n        return CODEC;\n    \}\n', '\n', new)
            if 'MapCodec' not in new.replace('import com.mojang.serialization.MapCodec;', ''):
                new = new.replace('import com.mojang.serialization.MapCodec;\n', '')
        if new != text:
            path.write_text(new, encoding='utf-8')
            changed += 1
print(changed, 'files changed')
