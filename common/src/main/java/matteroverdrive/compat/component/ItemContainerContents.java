package matteroverdrive.compat.component;

import java.util.List;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import matteroverdrive.compat.codec.ByteBufCodecs;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.compat.codec.StreamCodec;
import matteroverdrive.compat.codec.VanillaCodecs;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/** 1.20.1 stand-in for the 1.20.5+ {@code ItemContainerContents}: a small inventory kept on an item (slot + stack). */
public final class ItemContainerContents {
    private record Slot(int slot, ItemStack item) {
        static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 255).fieldOf("slot").forGetter(Slot::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(Slot::item)).apply(i, Slot::new));
    }

    public static final ItemContainerContents EMPTY = new ItemContainerContents(NonNullList.create());
    public static final Codec<ItemContainerContents> CODEC = Slot.CODEC.listOf().xmap(ItemContainerContents::fromSlots, ItemContainerContents::asSlots);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemContainerContents> STREAM_CODEC =
            VanillaCodecs.ITEM_STACK_OPTIONAL.apply(ByteBufCodecs.list(256)).map(ItemContainerContents::fromItems, c -> c.items);

    private final NonNullList<ItemStack> items;

    private ItemContainerContents(NonNullList<ItemStack> items) {
        this.items = items;
    }

    private static ItemContainerContents fromSlots(List<Slot> slots) {
        int size = slots.stream().mapToInt(Slot::slot).max().orElse(-1) + 1;
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        for (Slot slot : slots) items.set(slot.slot(), slot.item());
        return new ItemContainerContents(items);
    }

    private List<Slot> asSlots() {
        List<Slot> slots = new java.util.ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) slots.add(new Slot(i, items.get(i)));
        }
        return slots;
    }

    public static ItemContainerContents fromItems(List<ItemStack> items) {
        int size = items.size();
        while (size > 0 && items.get(size - 1).isEmpty()) size--;
        NonNullList<ItemStack> copy = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < size; i++) copy.set(i, items.get(i).copy());
        return new ItemContainerContents(copy);
    }

    public void copyInto(NonNullList<ItemStack> into) {
        for (int i = 0; i < into.size(); i++) into.set(i, i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
    }

    public Stream<ItemStack> stream() {
        return items.stream().map(ItemStack::copy);
    }

    public Stream<ItemStack> nonEmptyStream() {
        return items.stream().filter(s -> !s.isEmpty()).map(ItemStack::copy);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ItemContainerContents other) || other.items.size() != items.size()) return false;
        for (int i = 0; i < items.size(); i++) {
            if (!ItemStack.matches(items.get(i), other.items.get(i))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return items.size();
    }
}
