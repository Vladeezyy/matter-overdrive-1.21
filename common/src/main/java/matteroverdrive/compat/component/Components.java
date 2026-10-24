package matteroverdrive.compat.component;

import java.util.function.UnaryOperator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * The 1.20.5+ {@code ItemStack.get / set / has / remove / getOrDefault / update} for the 1.20.1 {@link DataComponentType}s:
 * values live in the stack's tag. A value that fails to decode reads as absent (and is logged once per call).
 */
public final class Components {
    private Components() {}

    public static <T> @Nullable T get(ItemStack stack, DataComponentType<? extends T> type) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        Tag value = tag.get(type.key());
        return value == null ? null : decode(type, value);
    }

    public static <T> @Nullable T get(CompoundTag tag, DataComponentType<? extends T> type) {
        Tag value = tag.get(type.key());
        return value == null ? null : decode(type, value);
    }

    private static <T> @Nullable T decode(DataComponentType<T> type, Tag value) {
        return type.codec().parse(NbtOps.INSTANCE, value).resultOrPartial(e -> matteroverdrive.MatterOverdrive.LOGGER.warn("Bad {}: {}", type, e)).orElse(null);
    }

    public static <T> T getOrDefault(ItemStack stack, DataComponentType<? extends T> type, T fallback) {
        T value = get(stack, type);
        return value != null ? value : fallback;
    }

    public static boolean has(ItemStack stack, DataComponentType<?> type) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(type.key());
    }

    /** Sets the value (null removes it); returns the previous one */
    public static <T> @Nullable T set(ItemStack stack, DataComponentType<T> type, @Nullable T value) {
        T previous = get(stack, type);
        if (value == null) {
            remove(stack, type);
        } else {
            put(stack.getOrCreateTag(), type, value);
        }
        return previous;
    }

    public static <T> void put(CompoundTag tag, DataComponentType<T> type, T value) {
        type.codec().encodeStart(NbtOps.INSTANCE, value).resultOrPartial(e -> matteroverdrive.MatterOverdrive.LOGGER.warn("Bad {}: {}", type, e))
                .ifPresent(encoded -> tag.put(type.key(), encoded));
    }

    public static <T> @Nullable T remove(ItemStack stack, DataComponentType<? extends T> type) {
        T previous = get(stack, type);
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(type.key());
            if (tag.isEmpty()) stack.setTag(null);
        }
        return previous;
    }

    public static <T> @Nullable T update(ItemStack stack, DataComponentType<T> type, T fallback, UnaryOperator<T> updater) {
        return set(stack, type, updater.apply(getOrDefault(stack, type, fallback)));
    }

    /** Copies one component from a stack to another (1.20.5 {@code copyFrom}) */
    public static <T> void copy(ItemStack from, ItemStack to, DataComponentType<T> type) {
        set(to, type, get(from, type));
    }
}
