package matteroverdrive.fabric;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.platform.transfer.IEnergyStorage;
import matteroverdrive.platform.transfer.IFluidHandler;
import matteroverdrive.platform.transfer.IFluidHandlerItem;
import matteroverdrive.platform.transfer.IItemHandler;
import matteroverdrive.platform.transfer.StorageKind;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import team.reborn.energy.api.EnergyStorage;

/**
 * The storages of the mod's blocks and items (what NeoForge registers as capabilities): used directly between the
 * mod's own blocks, and offered to other mods through Fabric's lookups (transfer API, Team Reborn Energy).
 */
final class FabricStorages {
    private static final Map<BlockEntityType<?>, BiFunction<BlockEntity, Direction, IEnergyStorage>> ENERGY = new HashMap<>();
    private static final Map<BlockEntityType<?>, BiFunction<BlockEntity, Direction, IItemHandler>> ITEMS = new HashMap<>();
    private static final Map<BlockEntityType<?>, BiFunction<BlockEntity, Direction, IFluidHandler>> FLUIDS = new HashMap<>();
    private static final Map<Item, Function<ItemStack, IEnergyStorage>> ITEM_ENERGY = new HashMap<>();
    private static final Map<Item, Function<ItemStack, IFluidHandlerItem>> ITEM_FLUIDS = new HashMap<>();
    /**
     * One adapter per block entity and kind: pending moves of a transaction live in the adapter, so two lookups of the
     * same block within one transaction must see the same one
     */
    private static final Map<BlockEntity, Object[]> EXPOSED = new WeakHashMap<>();

    private FabricStorages() {}

    static void register() {
        MatterOverdrive.registerCapabilities(new MatterOverdrive.Capabilities() {
            @Override
            public void energyItem(Function<ItemStack, IEnergyStorage> handler, Item... items) {
                for (Item item : items) ITEM_ENERGY.put(item, handler);
                EnergyStorage.ITEM.registerForItems((stack, context) -> new FabricTransfer.ExposedItemEnergy(context, handler), items);
            }

            @Override
            public void fluidItem(Function<ItemStack, IFluidHandlerItem> handler, Item... items) {
                for (Item item : items) ITEM_FLUIDS.put(item, handler);
                FluidStorage.ITEM.registerForItems((stack, context) -> new FabricTransfer.ExposedItemFluid(context, handler), items);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T extends BlockEntity> void energy(BlockEntityType<T> type, BiFunction<T, Direction, IEnergyStorage> handler) {
                ENERGY.put(type, (BiFunction<BlockEntity, Direction, IEnergyStorage>) handler);
                EnergyStorage.SIDED.registerForBlockEntity((be, side) -> exposed(be, 0, side), type);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T extends BlockEntity> void items(BlockEntityType<T> type, BiFunction<T, Direction, IItemHandler> handler) {
                ITEMS.put(type, (BiFunction<BlockEntity, Direction, IItemHandler>) handler);
                ItemStorage.SIDED.registerForBlockEntity((be, side) -> exposed(be, 1, side), type);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T extends BlockEntity> void fluids(BlockEntityType<T> type, BiFunction<T, Direction, IFluidHandler> handler) {
                FLUIDS.put(type, (BiFunction<BlockEntity, Direction, IFluidHandler>) handler);
                FluidStorage.SIDED.registerForBlockEntity((be, side) -> exposed(be, 2, side), type);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static synchronized <A> @Nullable A exposed(BlockEntity be, int kind, @Nullable Direction side) {
        Object[] adapters = EXPOSED.computeIfAbsent(be, key -> new Object[3]);
        if (adapters[kind] == null) {
            adapters[kind] = switch (kind) {
                case 0 -> {
                    IEnergyStorage handler = ENERGY.get(be.getType()).apply(be, side);
                    yield handler == null ? null : new FabricTransfer.ExposedEnergy(handler);
                }
                case 1 -> {
                    IItemHandler handler = ITEMS.get(be.getType()).apply(be, side);
                    yield handler == null ? null : FabricTransfer.exposeItems(handler);
                }
                default -> {
                    IFluidHandler handler = FLUIDS.get(be.getType()).apply(be, side);
                    yield handler == null ? null : new FabricTransfer.ExposedFluid(handler);
                }
            };
        }
        return (A) adapters[kind];
    }

    /** The mod's own storage of the block entity, or null */
    @SuppressWarnings("unchecked")
    static <T> @Nullable T own(StorageKind<T> kind, BlockEntity be, @Nullable Direction side) {
        Map<BlockEntityType<?>, ? extends BiFunction<BlockEntity, Direction, ?>> map = kind == StorageKind.ENERGY ? ENERGY : kind == StorageKind.FLUID ? FLUIDS : ITEMS;
        BiFunction<BlockEntity, Direction, ?> handler = map.get(be.getType());
        return handler == null ? null : (T) handler.apply(be, side);
    }

    static @Nullable IEnergyStorage ownItemEnergy(ItemStack stack) {
        Function<ItemStack, IEnergyStorage> handler = ITEM_ENERGY.get(stack.getItem());
        return handler == null ? null : handler.apply(stack);
    }

    static @Nullable IFluidHandlerItem ownItemFluid(ItemStack stack) {
        Function<ItemStack, IFluidHandlerItem> handler = ITEM_FLUIDS.get(stack.getItem());
        return handler == null ? null : handler.apply(stack);
    }

    /** Another mod's Fabric storage as a common handler */
    @SuppressWarnings("unchecked")
    static <T> @Nullable T foreign(StorageKind<T> kind, @Nullable Object storage) {
        if (storage == null) return null;
        if (kind == StorageKind.ENERGY) return (T) new FabricTransfer.Energy((EnergyStorage) storage);
        if (kind == StorageKind.FLUID) return (T) new FabricTransfer.Fluids((Storage<FluidVariant>) storage);
        // item storages without slots (rare) can't be shown as slots
        return storage instanceof SlottedStorage<?> slotted ? (T) new FabricTransfer.SlottedItems((SlottedStorage<ItemVariant>) slotted) : null;
    }

    /** The storage of a block: the mod's own, else another mod's */
    static <T> @Nullable T find(StorageKind<T> kind, Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            T own = own(kind, be, side);
            if (own != null) return own;
        }
        Direction from = side == null ? Direction.UP : side;
        Object storage = kind == StorageKind.ENERGY ? EnergyStorage.SIDED.find(level, pos, null, be, from)
                : kind == StorageKind.FLUID ? FluidStorage.SIDED.find(level, pos, null, be, from)
                : ItemStorage.SIDED.find(level, pos, null, be, from);
        return foreign(kind, storage);
    }
}
