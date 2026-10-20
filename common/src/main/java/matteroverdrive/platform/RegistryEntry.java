package matteroverdrive.platform;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * An entry registered through a {@link Registrar} (the common replacement of NeoForge's {@code DeferredHolder}).
 *
 * <p>Unlike {@code DeferredHolder} it is not itself a {@link Holder}: where a vanilla API wants a holder (effects,
 * attributes, armor materials...), pass {@link #holder()}, the registry's own {@code Holder.Reference}. Vanilla compares
 * holders by identity, so a wrapper holder stops matching once the game has saved and loaded the value.
 */
public class RegistryEntry<R, T extends R> implements Supplier<T> {
    private final ResourceKey<? extends Registry<R>> registryKey;
    private final ResourceKey<R> key;
    private volatile Holder<R> holder;

    RegistryEntry(ResourceKey<? extends Registry<R>> registryKey, ResourceLocation id) {
        this.registryKey = registryKey;
        this.key = ResourceKey.create(registryKey, id);
    }

    /** The registered value (throws before the registry has it) */
    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        return (T) this.holder().value();
    }

    /** The same as {@link #get()}, for code written against {@code Holder.value()} */
    public T value() {
        return this.get();
    }

    /** The registry's holder of this entry */
    @SuppressWarnings("unchecked")
    public Holder<R> holder() {
        Holder<R> holder = this.holder;
        if (holder == null) {
            Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.getValue(this.registryKey.location());
            if (registry == null) {
                throw new IllegalStateException("Registry " + this.registryKey.location() + " does not exist (yet) for " + this.key.location());
            }
            holder = registry.getOrThrow(this.key);
            this.holder = holder;
        }
        return holder;
    }

    public boolean isBound() {
        Registry<?> registry = BuiltInRegistries.REGISTRY.getValue(this.registryKey.location());
        return registry != null && registry.containsKey(this.key.location());
    }

    public ResourceLocation getId() {
        return this.key.location();
    }

    public ResourceKey<R> getKey() {
        return this.key;
    }

    /** True if {@code holder} is this entry */
    public boolean is(Holder<R> holder) {
        return holder.is(this.key);
    }

    @Override
    public String toString() {
        return "RegistryEntry[" + this.key.location() + "]";
    }
}
