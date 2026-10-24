package matteroverdrive.forge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.platform.PlayerDataStore;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * The NeoForge 1.21 capability registrations of the common code as Forge 1.20.1 capability providers: attached to the
 * mod's block entities and items, one LazyOptional per side kept until the block entity goes or
 * {@link #invalidate} (the common invalidateCapabilities) asks for fresh ones. Also the player data capability.
 */
final class ForgeCaps {
    private static final ResourceLocation ID = new ResourceLocation(MatterOverdrive.MODID, "storage");
    static final Capability<PlayerDataStore> PLAYER_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    /** Block entity type → capability → handler factory */
    private static final Map<BlockEntityType<?>, Map<Capability<?>, BiFunction<BlockEntity, Direction, ?>>> BLOCKS = new IdentityHashMap<>();
    private static final Map<Item, Map<Capability<?>, Function<ItemStack, ?>>> ITEMS = new IdentityHashMap<>();
    private static final Map<BlockEntity, BlockProvider> PROVIDERS = Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile boolean registered;

    private ForgeCaps() {}

    /** Once the registries are frozen (common setup) */
    static synchronized void registerAll() {
        if (registered) return;
        registered = true;
        MatterOverdrive.registerCapabilities(new MatterOverdrive.Capabilities() {
            @Override
            public void energyItem(Function<ItemStack, matteroverdrive.platform.transfer.IEnergyStorage> handler, Item... items) {
                for (Item item : items) ITEMS.computeIfAbsent(item, i -> new HashMap<>()).put(ForgeCapabilities.ENERGY, stack -> ForgeTransfer.toForge(handler.apply(stack)));
            }

            @Override
            public void fluidItem(Function<ItemStack, matteroverdrive.platform.transfer.IFluidHandlerItem> handler, Item... items) {
                for (Item item : items) ITEMS.computeIfAbsent(item, i -> new HashMap<>()).put(ForgeCapabilities.FLUID_HANDLER_ITEM, stack -> ForgeTransfer.toForgeItem(handler.apply(stack)));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T extends BlockEntity> void energy(BlockEntityType<T> type, BiFunction<T, Direction, matteroverdrive.platform.transfer.IEnergyStorage> handler) {
                block(type, ForgeCapabilities.ENERGY, (be, side) -> ForgeTransfer.toForge(handler.apply((T) be, side)));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T extends BlockEntity> void items(BlockEntityType<T> type, BiFunction<T, Direction, matteroverdrive.platform.transfer.IItemHandler> handler) {
                block(type, ForgeCapabilities.ITEM_HANDLER, (be, side) -> ForgeTransfer.toForge(handler.apply((T) be, side)));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T extends BlockEntity> void fluids(BlockEntityType<T> type, BiFunction<T, Direction, matteroverdrive.platform.transfer.IFluidHandler> handler) {
                block(type, ForgeCapabilities.FLUID_HANDLER, (be, side) -> ForgeTransfer.toForge(handler.apply((T) be, side)));
            }
        });
    }

    private static void block(BlockEntityType<?> type, Capability<?> capability, BiFunction<BlockEntity, Direction, ?> handler) {
        BLOCKS.computeIfAbsent(type, t -> new HashMap<>()).put(capability, handler);
    }

    static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerDataStore.class);
    }

    static void onAttachBlockEntity(AttachCapabilitiesEvent<BlockEntity> event) {
        var handlers = BLOCKS.get(event.getObject().getType());
        if (handlers == null) return;
        BlockProvider provider = new BlockProvider(event.getObject(), handlers);
        event.addCapability(ID, provider);
        event.addListener(provider::invalidate);
        PROVIDERS.put(event.getObject(), provider);
    }

    static void onAttachItem(AttachCapabilitiesEvent<ItemStack> event) {
        var handlers = ITEMS.get(event.getObject().getItem());
        if (handlers == null) return;
        ItemProvider provider = new ItemProvider(event.getObject(), handlers);
        event.addCapability(ID, provider);
        event.addListener(provider::invalidate);
    }

    static void onAttachEntity(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) return;
        PlayerProvider provider = new PlayerProvider();
        event.addCapability(new ResourceLocation(MatterOverdrive.MODID, "player_data"), provider);
        event.addListener(provider.optional::invalidate);
    }

    /** Death and the return from the End: the new player gets the old one's data */
    static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        playerData(event.getEntity()).copyFrom(playerData(original), event.getEntity().level().registryAccess());
        original.invalidateCaps();
    }

    static PlayerDataStore playerData(Player player) {
        return player.getCapability(PLAYER_DATA).orElseThrow(() -> new IllegalStateException("No Matter Overdrive data on " + player));
    }

    static void invalidate(BlockEntity entity) {
        BlockProvider provider = PROVIDERS.get(entity);
        if (provider != null) provider.invalidate();
    }

    private static final class BlockProvider implements ICapabilityProvider {
        private final BlockEntity entity;
        private final Map<Capability<?>, BiFunction<BlockEntity, Direction, ?>> handlers;
        /** capability → per side (index 6: no side) */
        private final Map<Capability<?>, LazyOptional<?>[]> cache = new HashMap<>();

        BlockProvider(BlockEntity entity, Map<Capability<?>, BiFunction<BlockEntity, Direction, ?>> handlers) {
            this.entity = entity;
            this.handlers = handlers;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
            var handler = handlers.get(capability);
            if (handler == null) return LazyOptional.empty();
            LazyOptional<?>[] sides = cache.computeIfAbsent(capability, c -> new LazyOptional<?>[7]);
            int index = side == null ? 6 : side.ordinal();
            LazyOptional<?> optional = sides[index];
            if (optional == null) {
                Object value = handler.apply(entity, side);
                optional = value == null ? LazyOptional.empty() : LazyOptional.of(() -> value);
                sides[index] = optional;
            }
            return (LazyOptional<T>) optional;
        }

        void invalidate() {
            List<LazyOptional<?>> all = new ArrayList<>();
            for (LazyOptional<?>[] sides : cache.values()) {
                for (LazyOptional<?> optional : sides) if (optional != null) all.add(optional);
            }
            cache.clear();
            all.forEach(LazyOptional::invalidate);
        }
    }

    private static final class ItemProvider implements ICapabilityProvider {
        private final ItemStack stack;
        private final Map<Capability<?>, Function<ItemStack, ?>> handlers;
        private final Map<Capability<?>, LazyOptional<?>> cache = new HashMap<>();

        ItemProvider(ItemStack stack, Map<Capability<?>, Function<ItemStack, ?>> handlers) {
            this.stack = stack;
            this.handlers = handlers;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
            var handler = handlers.get(capability);
            if (handler == null) return LazyOptional.empty();
            return (LazyOptional<T>) cache.computeIfAbsent(capability, c -> {
                Object value = handler.apply(stack);
                return value == null ? LazyOptional.empty() : LazyOptional.of(() -> value);
            });
        }

        void invalidate() {
            cache.values().forEach(LazyOptional::invalidate);
            cache.clear();
        }
    }

    private static final class PlayerProvider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerDataStore store = new PlayerDataStore();
        final LazyOptional<PlayerDataStore> optional = LazyOptional.of(() -> store);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
            return PLAYER_DATA.orEmpty(capability, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return store.serializeNBT(registries());
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            store.deserializeNBT(registries(), tag);
        }

        private static net.minecraft.core.HolderLookup.Provider registries() {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            return server != null ? server.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
        }
    }
}
