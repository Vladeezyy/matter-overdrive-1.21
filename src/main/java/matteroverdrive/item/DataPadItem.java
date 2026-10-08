package matteroverdrive.item;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import matteroverdrive.init.MODataComponents;
import matteroverdrive.init.MOSounds;
import matteroverdrive.quest.QuestEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1.7.10 DataPad: use opens the Data Pad screen (guide, active quests); use on a block holds a 2-second scan that quests
 * can count (1.7.10 MOEventScan). A pad may only scan whitelisted blocks, destroy what it scans and have no screen
 * (the mad scientist's pad).
 */
public class DataPadItem extends Item {
    public static final int SCAN_TIME = 20 * 2;
    /** Client: opens the Data Pad screen for the pad in this hand. */
    public static Consumer<InteractionHand> openScreen = hand -> {};

    /** 1.7.10 whitelist / Destroys / nogui tags. */
    public record Scan(List<Block> whitelist, boolean destroys, boolean noGui) {
        public static final Codec<Scan> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.BLOCK.byNameCodec().listOf().optionalFieldOf("whitelist", List.of()).forGetter(Scan::whitelist),
                Codec.BOOL.optionalFieldOf("destroys", false).forGetter(Scan::destroys),
                Codec.BOOL.optionalFieldOf("no_gui", false).forGetter(Scan::noGui)).apply(i, Scan::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Scan> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(net.minecraft.core.registries.Registries.BLOCK).apply(ByteBufCodecs.list()), Scan::whitelist,
                ByteBufCodecs.BOOL, Scan::destroys, ByteBufCodecs.BOOL, Scan::noGui, Scan::new);
    }

    /**
     * 1.7.10 page / SelectedActiveQuest / QuestInfoScroll / guideID + its page / Ordering / Category. Pages: 0 guide
     * entries, 1 guide description, 2 active quests.
     */
    public record State(int page, int selectedQuest, int scroll, String guide, int guidePage, int ordering, String category) {
        public static final int PAGE_ENTRIES = 0, PAGE_DESCRIPTION = 1, PAGE_QUESTS = 2;
        /** 1.7.10 getOrdering: 2 (the hand-placed groups) by default. */
        public static final State DEFAULT = new State(0, 0, 0, "", 0, 2, "general");
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("page", 0).forGetter(State::page),
                Codec.INT.optionalFieldOf("selected_quest", 0).forGetter(State::selectedQuest),
                Codec.INT.optionalFieldOf("scroll", 0).forGetter(State::scroll),
                Codec.STRING.optionalFieldOf("guide", "").forGetter(State::guide),
                Codec.INT.optionalFieldOf("guide_page", 0).forGetter(State::guidePage),
                Codec.INT.optionalFieldOf("ordering", 2).forGetter(State::ordering),
                Codec.STRING.optionalFieldOf("category", "general").forGetter(State::category)).apply(i, State::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, State::page, ByteBufCodecs.VAR_INT, State::selectedQuest, ByteBufCodecs.VAR_INT, State::scroll,
                ByteBufCodecs.stringUtf8(128), State::guide, ByteBufCodecs.VAR_INT, State::guidePage, ByteBufCodecs.VAR_INT, State::ordering,
                ByteBufCodecs.stringUtf8(64), State::category, State::new);

        public State withPage(int page) {
            return new State(page, selectedQuest, scroll, guide, guidePage, ordering, category);
        }

        public State withQuest(int selectedQuest, int scroll) {
            return new State(page, selectedQuest, scroll, guide, guidePage, ordering, category);
        }

        public State withGuide(String guide, int guidePage) {
            return new State(page, selectedQuest, scroll, guide, guidePage, ordering, category);
        }

        public State withOrdering(int ordering) {
            return new State(page, selectedQuest, scroll, guide, guidePage, ordering, category);
        }

        public State withCategory(String category) {
            return new State(page, selectedQuest, scroll, guide, guidePage, ordering, category);
        }
    }

    public DataPadItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static State getState(ItemStack pad) {
        return pad.getOrDefault(MODataComponents.DATA_PAD.get(), State.DEFAULT);
    }

    public static @Nullable Scan getScan(ItemStack pad) {
        return pad.get(MODataComponents.DATA_PAD_SCAN.get());
    }

    public static boolean hasGui(ItemStack pad) {
        Scan scan = getScan(pad);
        return scan == null || !scan.noGui();
    }

    /** 1.7.10 canScan: anything without a whitelist. */
    public static boolean canScan(ItemStack pad, BlockState state) {
        Scan scan = getScan(pad);
        return scan == null || scan.whitelist().isEmpty() || scan.whitelist().contains(state.getBlock());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() && hasGui(player.getItemInHand(hand))) openScreen.accept(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (player == null || player.isShiftKeyDown() || state.isAir() || !canScan(context.getItemInHand(), state)) return InteractionResult.PASS;
        player.startUsingItem(context.getHand());
        if (context.getLevel().isClientSide()) MatterScannerItem.startScanSound.accept(player);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return SCAN_TIME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    /** 1.7.10 onUsingTick: looking away (or at nothing) stops the scan. */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack pad, int remaining) {
        if (!(user instanceof Player player)) return;
        if (MatterScannerItem.trace(level, player).getType() != HitResult.Type.BLOCK) player.stopUsingItem();
    }

    /** 1.7.10 onEaten: the scan event, then (Destroys) the block goes; scanner_success. */
    @Override
    public ItemStack finishUsingItem(ItemStack pad, Level level, LivingEntity user) {
        if (level instanceof ServerLevel server && user instanceof Player player) {
            BlockHitResult hit = MatterScannerItem.trace(level, player);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hit.getBlockPos();
                QuestEvents.onEvent(player, new QuestEvents.Scan(pos, server.getBlockState(pos), pad));
                Scan scan = getScan(pad);
                if (scan != null && scan.destroys() && server.mayInteract(player, pos)) server.removeBlock(pos, false);
                server.playSound(null, player.getX(), player.getY(), player.getZ(), MOSounds.SCANNER_SUCCESS.get(), SoundSource.PLAYERS, 1, 1);
            }
        }
        return pad;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.matteroverdrive.data_pad.details").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
