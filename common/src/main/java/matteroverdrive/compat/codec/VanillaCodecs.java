package matteroverdrive.compat.codec;

import java.util.List;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** The 1.20.5+ stream codecs of vanilla types (BlockPos.STREAM_CODEC, ItemStack.OPTIONAL_STREAM_CODEC, ...) for 1.20.1 */
public final class VanillaCodecs {
    private VanillaCodecs() {}

    private static FriendlyByteBuf friendly(ByteBuf buf) {
        return buf instanceof FriendlyByteBuf f ? f : new FriendlyByteBuf(buf);
    }

    public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = StreamCodec.of((b, v) -> friendly(b).writeBlockPos(v), b -> friendly(b).readBlockPos());
    public static final StreamCodec<ByteBuf, GlobalPos> GLOBAL_POS = StreamCodec.of((b, v) -> friendly(b).writeGlobalPos(v), b -> friendly(b).readGlobalPos());
    public static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of((b, v) -> friendly(b).writeUUID(v), b -> friendly(b).readUUID());
    /** Empty stacks allowed (1.20.1 writes them as "absent") */
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> ITEM_STACK_OPTIONAL = StreamCodec.of(FriendlyByteBuf::writeItem, FriendlyByteBuf::readItem);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> ITEM_STACK = ITEM_STACK_OPTIONAL;
    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> ITEM_STACK_OPTIONAL_LIST =
            ByteBufCodecs.collection(java.util.ArrayList::new, ITEM_STACK_OPTIONAL);
    public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> INGREDIENT = StreamCodec.of((b, v) -> v.toNetwork(b), Ingredient::fromNetwork);
}
