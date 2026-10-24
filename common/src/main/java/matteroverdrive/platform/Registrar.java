package matteroverdrive.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers the entries of one registry for a mod (the common replacement of NeoForge's {@code DeferredRegister}, with
 * the same calls). The loader decides when the factories run: NeoForge on its register event, Fabric right away.
 */
public class Registrar<R> {
    private final ResourceKey<? extends Registry<R>> registryKey;
    private final String modId;
    private final List<RegistryEntry<R, ? extends R>> entries = new ArrayList<>();

    Registrar(ResourceKey<? extends Registry<R>> registryKey, String modId) {
        this.registryKey = registryKey;
        this.modId = modId;
    }

    public static <R> Registrar<R> create(ResourceKey<? extends Registry<R>> registryKey, String modId) {
        return new Registrar<>(registryKey, modId);
    }

    public static <R> Registrar<R> create(Registry<R> registry, String modId) {
        return new Registrar<>(registry.key(), modId);
    }

    public <T extends R> RegistryEntry<R, T> register(String name, Supplier<? extends T> factory) {
        ResourceLocation id = new net.minecraft.resources.ResourceLocation(this.modId, name);
        return this.add(new RegistryEntry<>(this.registryKey, id), factory);
    }

    <T extends R, E extends RegistryEntry<R, T>> E add(E entry, Supplier<? extends T> factory) {
        this.entries.add(entry);
        Platform.INSTANCE.register(this.registryKey, entry.getId(), factory);
        return entry;
    }

    String modId() {
        return this.modId;
    }

    public static Items items(String modId) {
        return new Items(modId);
    }

    public static Blocks blocks(String modId) {
        return new Blocks(modId);
    }

    public static DataComponents dataComponents(String modId) {
        return new DataComponents(modId);
    }

    /** Data component types: 1.20.1 has no registry for them, values live in the stack's NBT under the name */
    public static final class DataComponents {
        private final java.util.List<java.util.function.Supplier<?>> entries = new java.util.ArrayList<>();

        DataComponents(String modId) {}

        public <D> java.util.function.Supplier<matteroverdrive.compat.component.DataComponentType<D>> registerComponentType(
                String name, java.util.function.UnaryOperator<matteroverdrive.compat.component.DataComponentType.Builder<D>> builder) {
            matteroverdrive.compat.component.DataComponentType<D> type = builder.apply(matteroverdrive.compat.component.DataComponentType.builder()).build(name);
            java.util.function.Supplier<matteroverdrive.compat.component.DataComponentType<D>> entry = () -> type;
            entries.add(entry);
            return entry;
        }

        public java.util.List<java.util.function.Supplier<?>> getEntries() {
            return entries;
        }
    }

    /** Items ({@code DeferredRegister.Items}): entries are {@link ItemEntry}s */
    public static final class Items extends Registrar<net.minecraft.world.item.Item> {
        Items(String modId) {
            super(net.minecraft.core.registries.Registries.ITEM, modId);
        }

        public <I extends net.minecraft.world.item.Item> ItemEntry<I> register(String name, Supplier<? extends I> factory) {
            return this.add(new ItemEntry<>(new net.minecraft.resources.ResourceLocation(this.modId(), name)), factory);
        }

        public <I extends net.minecraft.world.item.Item> ItemEntry<I> registerItem(String name, Function<net.minecraft.world.item.Item.Properties, ? extends I> factory, net.minecraft.world.item.Item.Properties properties) {
            return this.register(name, () -> factory.apply(properties));
        }

        public <I extends net.minecraft.world.item.Item> ItemEntry<I> registerItem(String name, Function<net.minecraft.world.item.Item.Properties, ? extends I> factory) {
            return this.registerItem(name, factory, new net.minecraft.world.item.Item.Properties());
        }

        public ItemEntry<net.minecraft.world.item.Item> registerSimpleItem(String name, net.minecraft.world.item.Item.Properties properties) {
            return this.registerItem(name, net.minecraft.world.item.Item::new, properties);
        }

        public ItemEntry<net.minecraft.world.item.Item> registerSimpleItem(String name) {
            return this.registerSimpleItem(name, new net.minecraft.world.item.Item.Properties());
        }

        public ItemEntry<net.minecraft.world.item.BlockItem> registerSimpleBlockItem(String name, Supplier<? extends net.minecraft.world.level.block.Block> block, net.minecraft.world.item.Item.Properties properties) {
            return this.register(name, () -> new net.minecraft.world.item.BlockItem(block.get(), properties));
        }

        public ItemEntry<net.minecraft.world.item.BlockItem> registerSimpleBlockItem(String name, Supplier<? extends net.minecraft.world.level.block.Block> block) {
            return this.registerSimpleBlockItem(name, block, new net.minecraft.world.item.Item.Properties());
        }

        public ItemEntry<net.minecraft.world.item.BlockItem> registerSimpleBlockItem(BlockEntry<?> block) {
            return this.registerSimpleBlockItem(block.getId().getPath(), block, new net.minecraft.world.item.Item.Properties());
        }
    }

    /** Blocks ({@code DeferredRegister.Blocks}): entries are {@link BlockEntry}s */
    public static final class Blocks extends Registrar<net.minecraft.world.level.block.Block> {
        Blocks(String modId) {
            super(net.minecraft.core.registries.Registries.BLOCK, modId);
        }

        public <B extends net.minecraft.world.level.block.Block> BlockEntry<B> register(String name, Supplier<? extends B> factory) {
            return this.add(new BlockEntry<>(new net.minecraft.resources.ResourceLocation(this.modId(), name)), factory);
        }

        public <B extends net.minecraft.world.level.block.Block> BlockEntry<B> registerBlock(String name, Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, ? extends B> factory, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
            return this.register(name, () -> factory.apply(properties));
        }

        public BlockEntry<net.minecraft.world.level.block.Block> registerSimpleBlock(String name, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
            return this.registerBlock(name, net.minecraft.world.level.block.Block::new, properties);
        }
    }

    public <T extends R> RegistryEntry<R, T> register(String name, Function<ResourceLocation, ? extends T> factory) {
        ResourceLocation id = new net.minecraft.resources.ResourceLocation(this.modId, name);
        return this.register(name, () -> factory.apply(id));
    }

    public Collection<RegistryEntry<R, ? extends R>> getEntries() {
        return Collections.unmodifiableList(this.entries);
    }

    public ResourceKey<? extends Registry<R>> getRegistryKey() {
        return this.registryKey;
    }
}
