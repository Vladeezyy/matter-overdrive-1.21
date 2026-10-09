package matteroverdrive.item;

import matteroverdrive.block.entity.PatternStorageBlockEntity;
import matteroverdrive.init.MODataComponents;
import matteroverdrive.init.MOSounds;
import matteroverdrive.matter.ItemPattern;
import matteroverdrive.matter.MatterHelper;
import matteroverdrive.matter.MatterRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 1.7.10 MatterScanner: linked to a pattern storage (put it in the storage's scanner slot), it scans the block it
 * points at within 5 blocks while use is held for 60 ticks + the block's matter value, then adds 10% to that block's
 * pattern in the storage and destroys the block (no drops). Looking at another block selects it instead. In an
 * analyzer's database slot it sends the analyzed patterns to its storage. C opens its screen with the storage's patterns.
 */
public class MatterScannerItem extends Item {
    public static final int PROGRESS_PER_ITEM = 10;
    public static final int SCAN_TIME = 60;
    public static final int RANGE = 5;
    /** Set by the client: plays the looping scanning sound while the player scans. */
    public static Consumer<Player> startScanSound = p -> {};

    public MatterScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static @Nullable GlobalPos getLink(ItemStack scanner) {
        return scanner.get(MODataComponents.SCANNER_LINK.get());
    }

    public static void link(ItemStack scanner, Level level, BlockPos pos) {
        GlobalPos link = GlobalPos.of(level.dimension(), pos.immutable());
        if (!link.equals(getLink(scanner))) scanner.set(MODataComponents.SCANNER_LINK.get(), link);
    }

    /** 1.7.10 getLink: the linked pattern storage, if it is loaded in this level. */
    public static @Nullable PatternStorageBlockEntity getDatabase(Level level, ItemStack scanner) {
        GlobalPos link = getLink(scanner);
        if (link == null || link.dimension() != level.dimension() || !level.isLoaded(link.pos())) return null;
        return level.getBlockEntity(link.pos()) instanceof PatternStorageBlockEntity storage ? storage : null;
    }

    public static @Nullable ItemPattern getSelected(ItemStack scanner) {
        return scanner.get(MODataComponents.SCANNER_SELECTED.get());
    }

    /** 1.7.10 setSelected: the database's pattern for the item, or a new one at 0%. */
    public static void select(Level level, ItemStack scanner, Item item) {
        PatternStorageBlockEntity database = getDatabase(level, scanner);
        ItemPattern pattern = database == null ? null
                : database.getPatterns().stream().filter(p -> p.is(item)).findFirst().orElse(null);
        scanner.set(MODataComponents.SCANNER_SELECTED.get(), pattern != null ? pattern : new ItemPattern(item.builtInRegistryHolder(), 0));
    }

    /** 1.7.10 MatterHelper.CanScan: has matter and isn't bedrock or air. */
    public static boolean canScan(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(Blocks.BEDROCK.asItem()) && MatterHelper.hasMatter(stack);
    }

    public static BlockHitResult trace(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        return level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(RANGE)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    private static ItemStack blockItem(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() ? ItemStack.EMPTY : state.getCloneItemStack(level, pos, false);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (trace(level, player).getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        player.startUsingItem(hand);
        if (level.isClientSide()) startScanSound.accept(player);
        return InteractionResult.CONSUME;
    }

    /** 1.7.10 getMaxItemUseDuration: 60 ticks + the selected item's matter. */
    @Override
    public int getUseDuration(ItemStack scanner, LivingEntity entity) {
        ItemPattern selected = getSelected(scanner);
        if (selected != null && canScan(selected.toStack())) return SCAN_TIME + MatterRegistry.getAnySide(selected.item().value());
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    /** 1.7.10 onUsingTick: looking at another block selects it and stops the scan. */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack scanner, int remaining) {
        if (!(user instanceof Player player)) return;
        BlockHitResult hit = trace(level, player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.stopUsingItem();
            return;
        }
        if (level.isClientSide()) return;
        ItemStack target = blockItem(level, hit.getBlockPos());
        ItemPattern selected = getSelected(scanner);
        if (target.isEmpty()) return;
        if (selected == null || !selected.is(target.getItem())) {
            select(level, scanner, target.getItem());
            player.stopUsingItem();
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack scanner, Level level, LivingEntity user) {
        if (level instanceof ServerLevel server && user instanceof Player player) {
            BlockHitResult hit = trace(level, player);
            if (hit.getType() == HitResult.Type.BLOCK) {
                // 1.7.10 MOEventScan before the scan itself
                matteroverdrive.quest.QuestEvents.onEvent(player, new matteroverdrive.quest.QuestEvents.Scan(hit.getBlockPos(),
                        server.getBlockState(hit.getBlockPos()), scanner));
                scan(server, scanner, player, hit.getBlockPos());
            }
        }
        return scanner;
    }

    /** 1.7.10 Scan + TileEntityMachinePatternStorage.addItem messages. */
    public static boolean scan(ServerLevel level, ItemStack scanner, Player player, BlockPos pos) {
        PatternStorageBlockEntity database = getDatabase(level, scanner);
        ItemStack target = blockItem(level, pos);
        if (database == null || target.isEmpty()) return false;
        Component prefix = Component.literal("[").append(scanner.getHoverName()).append("] ").withStyle(ChatFormatting.YELLOW);
        Component name = target.getHoverName();
        Supplier<ItemPattern> pattern = () -> database.getPatterns().stream().filter(p -> p.is(target.getItem())).findFirst().orElse(null);
        Component message;
        boolean ok = false;
        ItemPattern existing = pattern.get();
        if (!canScan(target)) {
            message = Component.translatable("chat.matteroverdrive.scanner.cannot_analyze", name).withStyle(ChatFormatting.RED);
        } else if (existing != null && existing.isComplete()) {
            message = Component.translatable("chat.matteroverdrive.scanner.fully_analyzed", name).withStyle(ChatFormatting.RED);
        } else if (database.addProgress(target.getItem(), PROGRESS_PER_ITEM)) {
            ItemPattern now = pattern.get();
            message = Component.translatable("chat.matteroverdrive.scanner.added", name, now == null ? PROGRESS_PER_ITEM : now.progress())
                    .withStyle(ChatFormatting.GREEN);
            if (now != null) scanner.set(MODataComponents.SCANNER_SELECTED.get(), now);
            ok = true;
        } else {
            message = Component.translatable("chat.matteroverdrive.scanner.no_space", name).withStyle(ChatFormatting.RED);
        }
        player.displayClientMessage(prefix.copy().append(message), false);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ok ? MOSounds.SCANNER_SUCCESS.get() : MOSounds.SCANNER_FAIL.get(),
                SoundSource.PLAYERS, 1, 1);
        if (ok) level.destroyBlock(pos, false, player);
        return ok;
    }

    @Override
    public void appendHoverText(ItemStack scanner, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        GlobalPos link = getLink(scanner);
        if (link != null) {
            tooltip.accept(Component.translatable("tooltip.matteroverdrive.scanner.online", link.pos().toShortString()).withStyle(ChatFormatting.GREEN));
            ItemPattern selected = getSelected(scanner);
            if (selected != null) {
                tooltip.accept(Component.translatable("tooltip.matteroverdrive.scanner.selected", selected.toStack().getHoverName()).withStyle(ChatFormatting.GRAY));
                tooltip.accept(Component.translatable("tooltip.matteroverdrive.scanner.progress", selected.progress()).withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.accept(Component.translatable("tooltip.matteroverdrive.scanner.offline").withStyle(ChatFormatting.RED));
        }
        tooltip.accept(Component.translatable("tooltip.matteroverdrive.scanner.open", scannerKeyName.get()).withStyle(ChatFormatting.GRAY));
    }

    /** Set by the client: the key that opens the scanner screen. */
    public static Supplier<String> scannerKeyName = () -> "C";
}
