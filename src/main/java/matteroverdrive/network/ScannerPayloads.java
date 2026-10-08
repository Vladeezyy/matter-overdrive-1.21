package matteroverdrive.network;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.PatternStorageBlockEntity;
import matteroverdrive.item.MatterScannerItem;
import matteroverdrive.matter.ItemPattern;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.function.Consumer;

/**
 * Matter scanner screen (1.7.10 PacketMatterScannerGetDatabase / PacketMatterScannerUpdate): the client asks for the
 * linked storage's patterns and picks the selected one; slot = the scanner's player inventory slot.
 */
public final class ScannerPayloads {
    /** Set by the client: receives the patterns for the open scanner screen. */
    public static Consumer<Patterns> onPatterns = p -> {};

    public record Request(int slot) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "scanner_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Request::slot, Request::new);

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    public record Patterns(int slot, boolean online, List<ItemPattern> patterns) implements CustomPacketPayload {
        public static final Type<Patterns> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "scanner_patterns"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Patterns> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Patterns::slot, ByteBufCodecs.BOOL, Patterns::online,
                ItemPattern.STREAM_CODEC.apply(ByteBufCodecs.list()), Patterns::patterns, Patterns::new);

        @Override
        public Type<Patterns> type() {
            return TYPE;
        }
    }

    public record Select(int slot, Holder<Item> item) implements CustomPacketPayload {
        public static final Type<Select> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "scanner_select"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Select> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Select::slot, ByteBufCodecs.holderRegistry(Registries.ITEM), Select::item, Select::new);

        @Override
        public Type<Select> type() {
            return TYPE;
        }
    }

    private static ItemStack scanner(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) return ItemStack.EMPTY;
        ItemStack stack = player.getInventory().getItem(slot);
        return stack.getItem() instanceof MatterScannerItem ? stack : ItemStack.EMPTY;
    }

    private static void send(ServerPlayer player, int slot, ItemStack scanner) {
        PatternStorageBlockEntity database = MatterScannerItem.getDatabase(player.level(), scanner);
        boolean online = database != null && database.isOnline();
        PacketDistributor.sendToPlayer(player, new Patterns(slot, online, online ? database.getPatterns() : List.of()));
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Request.TYPE, Request.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ItemStack scanner = scanner(player, payload.slot());
            if (!scanner.isEmpty()) send(player, payload.slot(), scanner);
        });
        registrar.playToServer(Select.TYPE, Select.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ItemStack scanner = scanner(player, payload.slot());
            if (!scanner.isEmpty()) MatterScannerItem.select(player.level(), scanner, payload.item().value());
        });
        registrar.playToClient(Patterns.TYPE, Patterns.STREAM_CODEC, (payload, context) -> onPatterns.accept(payload));
    }

    private ScannerPayloads() {}
}
