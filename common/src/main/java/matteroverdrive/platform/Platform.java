package matteroverdrive.platform;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.platform.transfer.IEnergyStorage;
import matteroverdrive.platform.transfer.IFluidHandler;
import matteroverdrive.platform.transfer.IFluidHandlerItem;
import matteroverdrive.platform.transfer.IItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.compat.codec.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * What the common code needs from the mod loader. Each loader module implements it and lists the class in
 * {@code META-INF/services/matteroverdrive.platform.Platform}.
 */
public interface Platform {
    Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
            .orElseThrow(() -> new IllegalStateException("No Matter Overdrive platform for this mod loader"));

    String name();

    boolean isModLoaded(String modId);

    /** Whether this is a physical client */
    boolean isClient();

    // --- registration -------------------------------------------------------------------------------

    /** Registers the value under the id (NeoForge: a deferred register, Fabric: in NeoForge's order) */
    <R, T extends R> void register(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, Supplier<? extends T> factory);

    CreativeModeTab.Builder creativeTabBuilder();

    /** The matter plasma fluid (NeoForge: with its fluid type) */
    matteroverdrive.fluid.MatterPlasmaFluid matterPlasma(boolean source);

    /** A spawn egg for an entity type registered later (NeoForge DeferredSpawnEggItem) */
    net.minecraft.world.item.SpawnEggItem spawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight, Item.Properties properties);

    <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks);

    /** A menu type whose client side menu is made from extra data the server wrote (NeoForge IMenuTypeExtension) */
    <T extends AbstractContainerMenu> MenuType<T> menuType(MenuFactory<T> factory);

    interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int containerId, net.minecraft.world.entity.player.Inventory inventory, RegistryFriendlyByteBuf extraData);
    }

    /** Opens the menu, writing the extra data its client side needs */
    void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData);

    /** Replaces the spawn placement of the entity type (NeoForge: RegisterSpawnPlacementsEvent REPLACE) */
    <T extends Mob> void replaceSpawnPlacement(EntityType<T> type, SpawnPlacements.Type placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate);

    // --- network --------------------------------------------------------------------------------------

    void sendToServer(CustomPacketPayload payload);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload);

    void sendToTrackingChunk(ServerLevel level, BlockPos pos, CustomPacketPayload payload);

    /** Whether the player's client knows this payload (not vanilla clients or game-test mock players) */
    boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);

    // --- player data ------------------------------------------------------------------------------------

    <T extends INBTSerializable<CompoundTag>> T getData(Player player, PlayerData<T> data);

    <T extends INBTSerializable<CompoundTag>> void syncData(ServerPlayer player, PlayerData<T> data);

    // --- other mods' storages (NeoForge capabilities, Fabric transfer API and Team Reborn Energy) ----------

    /** A cached view of one block's storage of a kind, from one side (valid while {@code isValid} says so) */
    <T> BlockLookup<T> blockLookup(matteroverdrive.platform.transfer.StorageKind<T> kind, ServerLevel level, BlockPos pos, @Nullable Direction side, java.util.function.BooleanSupplier isValid);

    interface BlockLookup<T> {
        @Nullable
        T get();
    }

    Optional<IEnergyStorage> energyHandler(Level level, BlockPos pos, @Nullable Direction side);

    Optional<IEnergyStorage> energyHandler(ItemStack stack);

    Optional<IItemHandler> itemHandler(Level level, BlockPos pos, @Nullable Direction side);

    Optional<IFluidHandler> fluidHandler(Level level, BlockPos pos, @Nullable Direction side);

    Optional<IFluidHandlerItem> fluidHandler(ItemStack stack);

    // --- data ---------------------------------------------------------------------------------------------

    /** Whether fire can burn the block from that side (NeoForge IBlockExtension.isFlammable; Fabric: its flammable registry) */
    boolean isFlammable(BlockState state, Level level, BlockPos pos, Direction side);

    /** What crafting leaves of the stack (NeoForge: per stack, vanilla: per item) */
    ItemStack craftingRemainder(ItemStack stack);

    /** A file in the mod's jar (or dev resources) */
    java.nio.file.Path modFile(String... path);

    /** The block's storages changed (NeoForge: its capability caches drop it; Fabric caches look them up each time) */
    void invalidateCapabilities(Level level, BlockPos pos);

    /** The base matter value of the item from the {@code matteroverdrive:matter} item data map, or null */
    @Nullable
    Integer matterValue(Item item);

    /** The next oxidation stage of a copper block (NeoForge data map; Fabric: vanilla's table, which Fabric fills) */
    @Nullable
    Block nextOxidation(Block block);

    /** The waxed variant of a copper block */
    @Nullable
    Block waxed(Block block);
}
