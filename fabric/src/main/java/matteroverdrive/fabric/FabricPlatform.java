package matteroverdrive.fabric;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;
import matteroverdrive.MatterOverdrive;
import matteroverdrive.fluid.MatterPlasmaFluid;
import matteroverdrive.platform.INBTSerializable;
import matteroverdrive.platform.Platform;
import matteroverdrive.platform.PlayerData;
import matteroverdrive.platform.transfer.IEnergyStorage;
import matteroverdrive.platform.transfer.IFluidHandler;
import matteroverdrive.platform.transfer.IFluidHandlerItem;
import matteroverdrive.platform.transfer.IItemHandler;
import matteroverdrive.platform.transfer.StorageKind;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import team.reborn.energy.api.EnergyStorage;

/**
 * Matter Overdrive on Fabric: Fabric API (and Team Reborn Energy for FE) where NeoForge has an API, vanilla behaviour
 * for NeoForge's hooks. Entries are queued and registered by {@link #registerAll()} in NeoForge's registry order.
 */
public final class FabricPlatform implements Platform {
    private record Pending(Identifier id, Supplier<?> factory) {}

    private static final Map<ResourceKey<?>, List<Pending>> PENDING = new LinkedHashMap<>();
    /** Sends a packet to the server; set by the client entry point (the client networking class is client-only) */
    static Consumer<CustomPacketPayload> clientSender = payload -> {
        throw new IllegalStateException("Packets to the server can only be sent from the client");
    };

    /** Registers the queued entries: attributes, data components, armor materials, then vanilla order, then modded registries */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void registerAll() {
        Set<Identifier> order = new LinkedHashSet<>();
        order.add(Registries.ATTRIBUTE.identifier());
        order.add(Registries.DATA_COMPONENT_TYPE.identifier());
        List<Identifier> modded = new ArrayList<>();
        // the registry of registries in registration order (its keySet() has no order)
        for (Registry<?> each : BuiltInRegistries.REGISTRY) {
            Identifier id = each.key().identifier();
            if (id.getNamespace().equals("minecraft")) order.add(id);
            else modded.add(id);
        }
        modded.sort(java.util.Comparator.comparing(Identifier::getNamespace).thenComparing(Identifier::getPath));
        order.addAll(modded);
        for (Identifier registryId : order) {
            List<Pending> entries = PENDING.remove(ResourceKey.createRegistryKey(registryId));
            if (entries == null) continue;
            Registry registry = BuiltInRegistries.REGISTRY.getValue(registryId);
            for (Pending entry : entries) Registry.register(registry, entry.id(), entry.factory().get());
        }
        if (!PENDING.isEmpty()) throw new IllegalStateException("Matter Overdrive entries for registries that do not exist: " + PENDING.keySet());
    }

    @Override
    public String name() {
        return "fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    // --- registration ---

    @Override
    public <R, T extends R> void register(ResourceKey<? extends Registry<R>> registry, Identifier id, Supplier<? extends T> factory) {
        PENDING.computeIfAbsent(registry, key -> new ArrayList<>()).add(new Pending(id, factory));
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricCreativeModeTab.builder();
    }

    @Override
    public MatterPlasmaFluid matterPlasma(boolean source) {
        return source ? new MatterPlasmaFluid.Source() : new MatterPlasmaFluid.Flowing();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SpawnEggItem spawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight, Item.Properties properties) {
        // entity types are registered before items here (vanilla registry order)
        return new SpawnEggItem(properties.spawnEgg(type.get()));
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return FabricBlockEntityTypeBuilder.create(factory::apply, blocks).build();
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> menuType(MenuFactory<T> factory) {
        // the extra data travels as bytes and is read back with the client's registries
        return new ExtendedMenuType<>((int id, Inventory inventory, byte[] data) ->
                factory.create(id, inventory, new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), inventory.player.registryAccess())), ByteBufCodecs.BYTE_ARRAY);
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        extraData.accept(buf);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        player.openMenu(new ExtendedMenuProvider<byte[]>() {
            @Override
            public byte[] getScreenOpeningData(ServerPlayer opener) {
                return data;
            }

            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player opener) {
                return provider.createMenu(id, inventory, opener);
            }
        });
    }

    @Override
    public <T extends Mob> void replaceSpawnPlacement(EntityType<T> type, SpawnPlacementType placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
        // the mod's own entity types have no placement yet
        SpawnPlacements.register(type, placement, heightmap, predicate);
    }

    // --- network ---

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        clientSender.accept(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        for (ServerPlayer player : PlayerLookup.tracking(entity)) ServerPlayNetworking.send(player, payload);
        if (entity instanceof ServerPlayer self) ServerPlayNetworking.send(self, payload);
    }

    @Override
    public void sendToTrackingChunk(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
        for (ServerPlayer player : PlayerLookup.tracking(level, pos)) ServerPlayNetworking.send(player, payload);
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection != null && ServerPlayNetworking.canSend(player, type);
    }

    // --- player data ---

    @Override
    public <T extends INBTSerializable<CompoundTag>> T getData(Player player, PlayerData<T> data) {
        return FabricPlayerData.get(player, data);
    }

    @Override
    public <T extends INBTSerializable<CompoundTag>> void syncData(ServerPlayer player, PlayerData<T> data) {
        FabricPlayerData.sync(player, data);
    }

    // --- storages: the mod's own blocks directly, other mods' through Fabric's transfer API and TR Energy ---

    @Override
    public <T> BlockLookup<T> blockLookup(StorageKind<T> kind, ServerLevel level, BlockPos pos, @Nullable Direction side, BooleanSupplier isValid) {
        if (kind == StorageKind.ENERGY) return cached(BlockApiCache.create(EnergyStorage.SIDED, level, pos), kind, side);
        if (kind == StorageKind.FLUID) return cached(BlockApiCache.create(FluidStorage.SIDED, level, pos), kind, side);
        return cached(BlockApiCache.create(ItemStorage.SIDED, level, pos), kind, side);
    }

    private static <T, A> BlockLookup<T> cached(BlockApiCache<A, Direction> cache, StorageKind<T> kind, @Nullable Direction side) {
        return new BlockLookup<>() {
            private Object found;
            private T wrapped;

            @Override
            public @Nullable T get() {
                BlockEntity be = cache.getBlockEntity();
                if (be != null) {
                    T own = FabricStorages.own(kind, be, side);
                    if (own != null) return own;
                }
                Object current = cache.find(side == null ? Direction.UP : side);
                if (current != this.found) {
                    // the same storage object until the block changes: wrap it once
                    this.found = current;
                    this.wrapped = FabricStorages.foreign(kind, current);
                }
                return this.wrapped;
            }
        };
    }

    @Override
    public Optional<IEnergyStorage> energyHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return Optional.ofNullable(FabricStorages.find(StorageKind.ENERGY, level, pos, side));
    }

    @Override
    public Optional<IEnergyStorage> energyHandler(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        IEnergyStorage own = FabricStorages.ownItemEnergy(stack);
        if (own != null) return Optional.of(own);
        ContainerItemContext context = FabricTransfer.stackContext(stack);
        EnergyStorage storage = context.find(EnergyStorage.ITEM);
        return storage == null ? Optional.empty() : Optional.of(new FabricTransfer.ItemEnergy(storage, context, stack));
    }

    @Override
    public Optional<IItemHandler> itemHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return Optional.ofNullable(FabricStorages.find(StorageKind.ITEM, level, pos, side));
    }

    @Override
    public Optional<IFluidHandler> fluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return Optional.ofNullable(FabricStorages.find(StorageKind.FLUID, level, pos, side));
    }

    @Override
    public Optional<IFluidHandlerItem> fluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        IFluidHandlerItem own = FabricStorages.ownItemFluid(stack);
        if (own != null) return Optional.of(own);
        ContainerItemContext context = FabricTransfer.stackContext(stack);
        Storage<FluidVariant> storage = context.find(FluidStorage.ITEM);
        return storage == null ? Optional.empty() : Optional.of(new FabricTransfer.ItemFluids(storage, context));
    }

    @Override
    public void invalidateCapabilities(Level level, BlockPos pos) {
        // Fabric's block API caches follow the block entity themselves
    }

    // --- data and hooks ---

    @Override
    public boolean isFlammable(BlockState state, Level level, BlockPos pos, Direction side) {
        // NeoForge: getFlammability > 0, the fire block's burn odds (Fabric's flammable registry feeds them)
        return ((FireBlock) Blocks.FIRE).getBurnOdds(state) > 0;
    }

    @Override
    public ItemStack craftingRemainder(ItemStack stack) {
        var remainder = stack.getItem().getCraftingRemainder();
        return remainder == null ? ItemStack.EMPTY : remainder.create();
    }

    @Override
    public Path modFile(String... path) {
        return FabricLoader.getInstance().getModContainer(MatterOverdrive.MODID).orElseThrow()
                .findPath(String.join("/", path)).orElse(null);
    }

    @Override
    public @Nullable Integer matterValue(Item item) {
        return FabricMatterValues.get(item);
    }

    @Override
    public @Nullable Block nextOxidation(Block block) {
        return WeatheringCopper.getNext(block).orElse(null);
    }

    @Override
    public @Nullable Block stripped(Block block) {
        return AxeItem.STRIPPABLES.get(block);
    }

    @Override
    public @Nullable Block waxed(Block block) {
        return HoneycombItem.WAXABLES.get().get(block);
    }
}
