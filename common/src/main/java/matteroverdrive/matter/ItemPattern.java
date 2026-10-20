package matteroverdrive.matter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 ItemPattern: an analysed item and how complete its pattern is (0-100). Replicating from an incomplete
 * pattern is more likely to fail.
 */
public record ItemPattern(Holder<Item> item, int progress) {
    public static final int MAX_PROGRESS = 100;

    public static final Codec<ItemPattern> CODEC = RecordCodecBuilder.create(i -> i.group(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(ItemPattern::item),
            Codec.intRange(0, MAX_PROGRESS).fieldOf("progress").forGetter(ItemPattern::progress)
    ).apply(i, ItemPattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemPattern> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(Registries.ITEM), ItemPattern::item,
            ByteBufCodecs.VAR_INT, ItemPattern::progress,
            ItemPattern::new);

    public boolean is(Item other) {
        return item.value() == other;
    }

    public boolean isComplete() {
        return progress >= MAX_PROGRESS;
    }

    public ItemPattern withProgress(int amount) {
        return new ItemPattern(item, Math.min(MAX_PROGRESS, progress + amount));
    }

    public ItemStack toStack() {
        return new ItemStack(item);
    }
}
