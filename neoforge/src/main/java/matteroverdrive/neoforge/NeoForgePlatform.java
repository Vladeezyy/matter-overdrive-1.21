package matteroverdrive.neoforge;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

/** Matter Overdrive on NeoForge: the NeoForge APIs the common code reaches through {@link Platform} */
public final class NeoForgePlatform implements Platform {
    /** Deferred registers per registry, filled by the common code before {@link #registerAll} */
    private static final Map<ResourceKey<?>, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Supplier<AttachmentType<?>>> ATTACHMENTS = new LinkedHashMap<>();
    private static final java.util.List<Consumer<RegisterSpawnPlacementsEvent>> SPAWN_PLACEMENTS = new java.util.ArrayList<>();

    /** Matter Plasma's fluid type (1.7.10 BlockFluidMatterPlasma: water-like, viscosity 8000, light 15) */
    static final Supplier<FluidType> MATTER_PLASMA_TYPE = register(NeoForgeRegistries.Keys.FLUID_TYPES, "matter_plasma",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.matteroverdrive.matter_plasma").lightLevel(15)
                    .viscosity(8000).canSwim(true).canDrown(true).canExtinguish(true).supportsBoating(true)
                    .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL, net.minecraft.sounds.SoundEvents.BUCKET_FILL)
                    .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY)));

    static void registerAll(IEventBus modBus) {
        // the player data first: its attachment types are registered from the common list
        for (PlayerData<?> data : matteroverdrive.init.MOAttachments.ALL) attachment(data);
        REGISTERS.values().forEach(register -> register.register(modBus));
    }

    static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        MatterOverdrive.registerSpawnPlacements();
        SPAWN_PLACEMENTS.forEach(placement -> placement.accept(event));
    }

    @SuppressWarnings("unchecked")
    private static <R> DeferredRegister<R> registerFor(ResourceKey<? extends Registry<R>> registry) {
        return (DeferredRegister<R>) REGISTERS.computeIfAbsent(registry, key -> DeferredRegister.create((ResourceKey<? extends Registry<R>>) key, MatterOverdrive.MODID));
    }

    private static <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String name, Supplier<T> factory) {
        return registerFor(registry).register(name, factory);
    }

    @Override
    public String name() {
        return "neoforge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    // --- registration ---

    @Override
    public <R, T extends R> void register(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, Supplier<? extends T> factory) {
        registerFor(registry).register(id.getPath(), factory);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.SPAWN_EGGS);
    }

    @Override
    public SpawnEggItem spawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight, Item.Properties properties) {
        // 1.21.2+: entity types are registered before items
        return new SpawnEggItem(type.get(), properties);
    }

    @Override
    public MatterPlasmaFluid matterPlasma(boolean source) {
        return source ? new NeoForgeMatterPlasma.Source() : new NeoForgeMatterPlasma.Flowing();
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return new BlockEntityType<>(factory::apply, blocks);
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> menuType(MenuFactory<T> factory) {
        return IMenuTypeExtension.create(factory::create);
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData) {
        player.openMenu(provider, extraData);
    }

    @Override
    public <T extends Mob> void replaceSpawnPlacement(EntityType<T> type, SpawnPlacementType placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
        SPAWN_PLACEMENTS.add(event -> event.register(type, placement, heightmap, predicate, RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }

    // --- network ---

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }

    @Override
    public void sendToTrackingChunk(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), payload);
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection != null && player.connection.hasChannel(type);
    }

    // --- player data: attachment types; NeoForge 21.3 has no attachment sync, NeoForgePlayerData sends them ---

    @SuppressWarnings("unchecked")
    private static <T extends INBTSerializable<CompoundTag>> void attachment(PlayerData<T> data) {
        Supplier<AttachmentType<?>> type = register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, data.id().getPath(), () -> AttachmentType.serializable(
                        () -> new SerializableData<>(data.factory().get()))
                .copyOnDeath().build());
        ATTACHMENTS.put(data.id(), (Supplier<AttachmentType<?>>) (Supplier<?>) type);
    }

    /** The common data in NeoForge's serializable attachment */
    static final class SerializableData<T extends INBTSerializable<CompoundTag>> implements net.neoforged.neoforge.common.util.INBTSerializable<CompoundTag> {
        final T value;

        SerializableData(T value) {
            this.value = value;
        }

        @Override
        public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
            return this.value.serializeNBT(provider);
        }

        @Override
        public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag tag) {
            this.value.deserializeNBT(provider, tag);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends INBTSerializable<CompoundTag>> AttachmentType<SerializableData<T>> type(PlayerData<T> data) {
        return (AttachmentType<SerializableData<T>>) ATTACHMENTS.get(data.id()).get();
    }

    @Override
    public <T extends INBTSerializable<CompoundTag>> T getData(Player player, PlayerData<T> data) {
        return player.getData(type(data)).value;
    }

    @Override
    public <T extends INBTSerializable<CompoundTag>> void syncData(ServerPlayer player, PlayerData<T> data) {
        NeoForgePlayerData.sync(player, data);
    }

    // --- storages ---

    @SuppressWarnings("unchecked")
    private static <T> BlockCapability<?, Direction> capability(StorageKind<T> kind) {
        if (kind == StorageKind.ENERGY) return Capabilities.EnergyStorage.BLOCK;
        if (kind == StorageKind.FLUID) return Capabilities.FluidHandler.BLOCK;
        return Capabilities.ItemHandler.BLOCK;
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nullable T common(StorageKind<T> kind, @Nullable Object neo) {
        if (neo == null) return null;
        if (kind == StorageKind.ENERGY) return (T) NeoForgeTransfer.fromNeo((net.neoforged.neoforge.energy.IEnergyStorage) neo);
        if (kind == StorageKind.FLUID) return (T) NeoForgeTransfer.fromNeo((net.neoforged.neoforge.fluids.capability.IFluidHandler) neo);
        return (T) NeoForgeTransfer.fromNeo((net.neoforged.neoforge.items.IItemHandler) neo);
    }

    @Override
    public <T> BlockLookup<T> blockLookup(StorageKind<T> kind, ServerLevel level, BlockPos pos, @Nullable Direction side, BooleanSupplier isValid) {
        BlockCapabilityCache<?, Direction> cache = BlockCapabilityCache.create(capability(kind), level, pos, side, isValid, () -> {});
        return new BlockLookup<>() {
            private Object neo;
            private T wrapped;

            @Override
            public @Nullable T get() {
                Object current = cache.getCapability();
                if (current != this.neo) {
                    // the cache keeps the same object until the neighbour changes: wrap it once
                    this.neo = current;
                    this.wrapped = common(kind, current);
                }
                return this.wrapped;
            }
        };
    }

    @Override
    public Optional<IEnergyStorage> energyHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return Optional.ofNullable(NeoForgeTransfer.fromNeo(level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side)));
    }

    @Override
    public Optional<IEnergyStorage> energyHandler(ItemStack stack) {
        return Optional.ofNullable(NeoForgeTransfer.fromNeo(stack.getCapability(Capabilities.EnergyStorage.ITEM)));
    }

    @Override
    public Optional<IItemHandler> itemHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return Optional.ofNullable(NeoForgeTransfer.fromNeo(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)));
    }

    @Override
    public Optional<IFluidHandler> fluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return Optional.ofNullable(NeoForgeTransfer.fromNeo(level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side)));
    }

    @Override
    public Optional<IFluidHandlerItem> fluidHandler(ItemStack stack) {
        var handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        return handler == null ? Optional.empty() : Optional.of((IFluidHandlerItem) NeoForgeTransfer.fromNeo(handler));
    }

    @Override
    public void invalidateCapabilities(Level level, BlockPos pos) {
        level.invalidateCapabilities(pos);
    }

    // --- data and hooks ---

    @Override
    public boolean isFlammable(BlockState state, Level level, BlockPos pos, Direction side) {
        return state.isFlammable(level, pos, side);
    }

    @Override
    public ItemStack craftingRemainder(ItemStack stack) {
        return stack.getItem().getCraftingRemainder(stack);
    }

    @Override
    public Path modFile(String... path) {
        return ModList.get().getModFileById(MatterOverdrive.MODID).getFile().findResource(path);
    }

    @Override
    public @Nullable Integer matterValue(Item item) {
        return item.builtInRegistryHolder().getData(MatterValues.MATTER);
    }

    @Override
    public @Nullable Block nextOxidation(Block block) {
        var next = block.builtInRegistryHolder().getData(NeoForgeDataMaps.OXIDIZABLES);
        return next == null ? null : next.nextOxidationStage();
    }

    @Override
    public @Nullable Block waxed(Block block) {
        var waxed = block.builtInRegistryHolder().getData(NeoForgeDataMaps.WAXABLES);
        return waxed == null ? null : waxed.waxed();
    }
}
