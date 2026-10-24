package matteroverdrive.compat.component;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.compat.DataComponentGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** The components a block entity puts on its dropped item (1.20.5 {@code collectImplicitComponents}), as a tag. */
public final class DataComponentMap implements DataComponentGetter {
    private final CompoundTag tag;

    private DataComponentMap(CompoundTag tag) {
        this.tag = tag;
    }

    public static DataComponentMap of(CompoundTag tag) {
        return new DataComponentMap(tag);
    }

    public static DataComponentMap of(ItemStack stack) {
        return new DataComponentMap(stack.getTag() != null ? stack.getTag() : new CompoundTag());
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        return Components.get(tag, type);
    }

    public CompoundTag tag() {
        return tag;
    }

    public boolean isEmpty() {
        return tag.isEmpty();
    }

    /** Merges the components into the stack's tag */
    public void applyTo(ItemStack stack) {
        if (!tag.isEmpty()) stack.getOrCreateTag().merge(tag);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final CompoundTag tag = new CompoundTag();

        public <T> Builder set(DataComponentType<T> type, @Nullable T value) {
            if (value != null) Components.put(tag, type, value);
            return this;
        }

        public DataComponentMap build() {
            return new DataComponentMap(tag);
        }
    }
}
