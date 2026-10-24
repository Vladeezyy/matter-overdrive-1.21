package matteroverdrive.forge;

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
import matteroverdrive.compat.codec.CustomPacketPayload;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.fluid.MatterPlasmaFluid;
import matteroverdrive.matter.MatterValueFiles;
import matteroverdrive.platform.Platform;
import matteroverdrive.platform.PlayerDataStore;
import matteroverdrive.platform.transfer.IEnergyStorage;
import matteroverdrive.platform.transfer.IFluidHandler;
import matteroverdrive.platform.transfer.IFluidHandlerItem;
import matteroverdrive.platform.transfer.IItemHandler;
import matteroverdrive.platform.transfer.StorageKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Matter Overdrive on Forge 1.20.1: the Forge APIs the common code reaches through {@link Platform} */
public final class ForgePlatform implements Platform {
    /** Deferred registers per registry, filled by the common code before {@link #registerAll} */
    private static final Map<ResourceKey<?>, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();
    private static final java.util.List<Consumer<SpawnPlacementRegisterEvent>> SPAWN_PLACEMENTS = new java.util.ArrayList<>();

    /** Matter Plasma's fluid type (1.7.10 BlockFluidMatterPlasma: water-like, viscosity 8000, light 15) */
    static final Supplier<FluidType> MATTER_PLASMA_TYPE = register(ForgeRegistries.Keys.FLUID_TYPES, "matter_plasma",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.matteroverdrive.matter_plasma").lightLevel(15)
                    .viscosity(8000).canSwim(true).canDrown(true).canExtinguish(true).supportsBoating(true)
                    .sound(net.minecraftforge.common.SoundActions.BUCKET_FILL, net.minecraft.sounds.SoundEvents.BUCKET_FILL)
                    .sound(net.minecraftforge.common.SoundActions.BUCKET_EMPTY, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY)) {
                @Override
                public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
                    // Matter Plasma textures (1.7.10 matter_plasma_still / _flowing), shown by tanks of other mods
                    consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions() {
                        @Override
                        public ResourceLocation getStillTexture() {
                            return new ResourceLocation(MatterOverdrive.MODID, "block/matter_plasma_still");
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return new ResourceLocation(MatterOverdrive.MODID, "block/matter_plasma_flowing");
                        }
                    });
                }
            });

    static void registerAll(IEventBus modBus) {
        REGISTERS.values().forEach(register -> register.register(modBus));
    }

    static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
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
        return "forge";
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
        return new ForgeSpawnEggItem(type, background, highlight, properties);
    }

    @Override
    public MatterPlasmaFluid matterPlasma(boolean source) {
        return source ? new MatterPlasmaFluid.Source() {
            @Override
            public FluidType getFluidType() {
                return MATTER_PLASMA_TYPE.get();
            }
        } : new MatterPlasmaFluid.Flowing() {
            @Override
            public FluidType getFluidType() {
                return MATTER_PLASMA_TYPE.get();
            }
        };
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return BlockEntityType.Builder.of(factory::apply, blocks).build(null);
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> menuType(MenuFactory<T> factory) {
        return IForgeMenuType.create((id, inventory, buf) -> factory.create(id, inventory, RegistryFriendlyByteBuf.of(buf, inventory.player.level().registryAccess())));
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData) {
        NetworkHooks.openScreen(player, provider, buf -> extraData.accept(RegistryFriendlyByteBuf.of(buf, player.level().registryAccess())));
    }

    @Override
    public <T extends Mob> void replaceSpawnPlacement(EntityType<T> type, SpawnPlacements.Type placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
        SPAWN_PLACEMENTS.add(event -> event.register(type, placement, heightmap, predicate, SpawnPlacementRegisterEvent.Operation.REPLACE));
    }

    // --- network ---

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ForgeNetwork.sendToServer(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ForgeNetwork.sendToPlayer(player, payload);
    }

    @Override
    public void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        ForgeNetwork.sendToTrackingAndSelf(entity, payload);
    }

    @Override
    public void sendToTrackingChunk(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
        ForgeNetwork.sendToTrackingChunk(level, pos, payload);
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return ForgeNetwork.canSend(player);
    }

    // --- player data ---

    @Override
    public PlayerDataStore playerData(Player player) {
        return ForgeCaps.playerData(player);
    }

    // --- storages ---

    private static <T> Capability<?> capability(StorageKind<T> kind) {
        if (kind == StorageKind.ENERGY) return ForgeCapabilities.ENERGY;
        if (kind == StorageKind.FLUID) return ForgeCapabilities.FLUID_HANDLER;
        return ForgeCapabilities.ITEM_HANDLER;
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nullable T common(StorageKind<T> kind, @Nullable Object forge) {
        if (forge == null) return null;
        if (kind == StorageKind.ENERGY) return (T) ForgeTransfer.fromForge((net.minecraftforge.energy.IEnergyStorage) forge);
        if (kind == StorageKind.FLUID) return (T) ForgeTransfer.fromForge((net.minecraftforge.fluids.capability.IFluidHandler) forge);
        return (T) ForgeTransfer.fromForge((net.minecraftforge.items.IItemHandler) forge);
    }

    /** The NeoForge BlockCapabilityCache's job: the neighbour's capability, re-read when its LazyOptional is invalidated */
    @Override
    public <T> BlockLookup<T> blockLookup(StorageKind<T> kind, ServerLevel level, BlockPos pos, @Nullable Direction side, BooleanSupplier isValid) {
        Capability<?> capability = capability(kind);
        return new BlockLookup<>() {
            private @Nullable LazyOptional<?> optional;
            private @Nullable BlockEntity entity;
            private Object forge;
            private T wrapped;

            @Override
            public @Nullable T get() {
                if (!isValid.getAsBoolean()) return null;
                LazyOptional<?> current = this.optional;
                if (current == null || !current.isPresent() || this.entity == null || this.entity.isRemoved()) {
                    BlockEntity be = level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
                    this.entity = be;
                    current = be == null ? LazyOptional.empty() : be.getCapability(capability, side);
                    if (current.isPresent()) {
                        LazyOptional<?> listened = current;
                        current.addListener(invalidated -> {
                            if (this.optional == listened) this.optional = null;
                        });
                    }
                    this.optional = current;
                }
                Object resolved = current.resolve().orElse(null);
                if (resolved != this.forge) {
                    this.forge = resolved;
                    this.wrapped = common(kind, resolved);
                }
                return this.wrapped;
            }
        };
    }

    private static <C> Optional<C> blockCapability(Level level, BlockPos pos, @Nullable Direction side, Capability<C> capability) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity == null ? Optional.empty() : entity.getCapability(capability, side).resolve();
    }

    @Override
    public Optional<IEnergyStorage> energyHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return blockCapability(level, pos, side, ForgeCapabilities.ENERGY).map(ForgeTransfer::fromForge);
    }

    @Override
    public Optional<IEnergyStorage> energyHandler(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).resolve().map(ForgeTransfer::fromForge);
    }

    @Override
    public Optional<IItemHandler> itemHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return blockCapability(level, pos, side, ForgeCapabilities.ITEM_HANDLER).map(ForgeTransfer::fromForge);
    }

    @Override
    public Optional<IFluidHandler> fluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return blockCapability(level, pos, side, ForgeCapabilities.FLUID_HANDLER).map(ForgeTransfer::fromForge);
    }

    @Override
    public Optional<IFluidHandlerItem> fluidHandler(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().map(h -> (IFluidHandlerItem) ForgeTransfer.fromForge(h));
    }

    @Override
    public void invalidateCapabilities(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity != null) ForgeCaps.invalidate(entity);
    }

    // --- data and hooks ---

    @Override
    public boolean isFlammable(BlockState state, Level level, BlockPos pos, Direction side) {
        return state.isFlammable(level, pos, side);
    }

    @Override
    public ItemStack craftingRemainder(ItemStack stack) {
        return stack.getCraftingRemainingItem();
    }

    @Override
    public Path modFile(String... path) {
        return ModList.get().getModFileById(MatterOverdrive.MODID).getFile().findResource(path);
    }

    @Override
    public @Nullable Integer matterValue(Item item) {
        return MatterValueFiles.get(item);
    }

    @Override
    public @Nullable Block nextOxidation(Block block) {
        return WeatheringCopper.getNext(block).orElse(null);
    }

    @Override
    public @Nullable Block waxed(Block block) {
        return HoneycombItem.WAXABLES.get().get(block);
    }
}
