package matteroverdrive.compat;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.component.DataComponentType;

/** 1.21.1 stand-in for 1.21.5+ net.minecraft.core.component.DataComponentGetter. */
public interface DataComponentGetter {
    <T> @Nullable T get(DataComponentType<? extends T> component);

    default <T> T getOrDefault(DataComponentType<? extends T> component, T defaultValue) {
        T t = get(component);
        return t != null ? t : defaultValue;
    }

    default <T> @Nullable T get(Supplier<? extends DataComponentType<? extends T>> componentType) {
        return get(componentType.get());
    }

    default <T> T getOrDefault(Supplier<? extends DataComponentType<? extends T>> componentType, T value) {
        return getOrDefault(componentType.get(), value);
    }

    default boolean has(DataComponentType<?> componentType) {
        return get(componentType) != null;
    }
}
