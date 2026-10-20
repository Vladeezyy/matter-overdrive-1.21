package matteroverdrive;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MOEntities;
import matteroverdrive.init.MOItems;
import matteroverdrive.item.BatteryItem;
import matteroverdrive.machine.MachineBlockEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** The mod on both loaders: the loader entry points call {@link #init()} and register what the lists below give. */
public final class MatterOverdrive {
    public static final String MODID = "matteroverdrive";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Creates every registry entry, in NeoForge's order (the loaders register them) */
    public static void init() {
        matteroverdrive.init.MODataComponents.COMPONENTS.getEntries();
        matteroverdrive.init.MOFluids.FLUIDS.getEntries();
        matteroverdrive.init.MODecorative.init();
        matteroverdrive.init.MOBlocks.BLOCKS.getEntries();
        MOItems.ITEMS.getEntries();
        MOBlockEntities.BLOCK_ENTITIES.getEntries();
        matteroverdrive.init.MOMenus.MENUS.getEntries();
        matteroverdrive.init.MORecipes.TYPES.getEntries();
        matteroverdrive.init.MORecipes.SERIALIZERS.getEntries();
        matteroverdrive.init.MOFeatures.FEATURES.getEntries();
        MOEntities.ENTITIES.getEntries();
        matteroverdrive.init.MOSounds.SOUNDS.getEntries();
        matteroverdrive.init.MOStructures.TYPES.getEntries();
        matteroverdrive.init.MOStructures.PIECES.getEntries();
        matteroverdrive.init.MOStructures.PROCESSORS.getEntries();
        matteroverdrive.init.MOAttributes.ATTRIBUTES.getEntries();
        matteroverdrive.init.MOCreativeTabs.TABS.getEntries();
        // 1.21.5+: game test bodies are test function registry entries
        matteroverdrive.gametest.MOGameTests.functions().getEntries();
    }

    /** What the mod's blocks and items expose to other mods' pipes and cables */
    public interface Capabilities {
        void energyItem(java.util.function.Function<net.minecraft.world.item.ItemStack, matteroverdrive.platform.transfer.IEnergyStorage> handler, net.minecraft.world.item.Item... items);

        void fluidItem(java.util.function.Function<net.minecraft.world.item.ItemStack, matteroverdrive.platform.transfer.IFluidHandlerItem> handler, net.minecraft.world.item.Item... items);

        <T extends net.minecraft.world.level.block.entity.BlockEntity> void energy(BlockEntityType<T> type, java.util.function.BiFunction<T, net.minecraft.core.Direction, matteroverdrive.platform.transfer.IEnergyStorage> handler);

        <T extends net.minecraft.world.level.block.entity.BlockEntity> void items(BlockEntityType<T> type, java.util.function.BiFunction<T, net.minecraft.core.Direction, matteroverdrive.platform.transfer.IItemHandler> handler);

        <T extends net.minecraft.world.level.block.entity.BlockEntity> void fluids(BlockEntityType<T> type, java.util.function.BiFunction<T, net.minecraft.core.Direction, matteroverdrive.platform.transfer.IFluidHandler> handler);
    }

    public static void registerCapabilities(Capabilities event) {
        event.fluidItem(matteroverdrive.item.MatterContainerItem.FluidHandler::new, MOItems.MATTER_CONTAINER.get(), MOItems.MATTER_CONTAINER_FULL.get());
        event.energyItem(stack -> MOItems.PORTABLE_DECOMPOSER.get().createEnergyHandler(stack), MOItems.PORTABLE_DECOMPOSER.get());
        for (BatteryItem battery : new BatteryItem[] {MOItems.BATTERY.get(), MOItems.HC_BATTERY.get(), MOItems.CREATIVE_BATTERY.get()}) {
            event.energyItem(battery::createEnergyHandler, battery);
        }
        for (var weapon : new matteroverdrive.item.weapon.EnergyWeaponItem[] {MOItems.PHASER.get(), MOItems.PHASER_RIFLE.get(), MOItems.PLASMA_SHOTGUN.get(), MOItems.ION_SNIPER.get(), MOItems.OMNI_TOOL.get()}) {
            event.energyItem(weapon::createEnergyHandler, weapon);
        }
        machine(event, MOBlockEntities.SOLAR_PANEL.get());
        machine(event, MOBlockEntities.CHARGING_STATION.get());
        event.items(MOBlockEntities.WEAPON_STATION.get(), (be, side) -> be.getInventory().automation());
        machine(event, MOBlockEntities.INSCRIBER.get());
        machine(event, MOBlockEntities.DECOMPOSER.get());
        machine(event, MOBlockEntities.RECYCLER.get());
        machine(event, MOBlockEntities.ANALYZER.get());
        machine(event, MOBlockEntities.TRANSPORTER.get());
        event.items(MOBlockEntities.CONTRACT_MARKET.get(), (be, side) -> be.getInventory().automation());
        machine(event, MOBlockEntities.PATTERN_STORAGE.get());
        machine(event, MOBlockEntities.REPLICATOR.get());
        event.fluids(MOBlockEntities.MATTER_PIPE.get(), (pipe, side) -> pipe.getTank());
        machine(event, MOBlockEntities.GRAVITATIONAL_STABILIZER.get());
        machine(event, MOBlockEntities.FUSION_REACTOR_CONTROLLER.get());
        event.energy(MOBlockEntities.FUSION_REACTOR_IO.get(), (io, side) -> io.getEnergy());
        event.fluids(MOBlockEntities.FUSION_REACTOR_IO.get(), (io, side) -> io.getMatter());
    }

    private static <T extends MachineBlockEntity> void machine(Capabilities event, BlockEntityType<T> type) {
        event.energy(type, MachineBlockEntity::getEnergyHandler);
        event.items(type, (be, side) -> be.getInventory().automation());
        event.fluids(type, (be, side) -> be.getMatterTank());
    }

    /** The mod's mobs and their attributes */
    public static void registerAttributes(java.util.function.BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier> event) {
        event.accept(MOEntities.ROGUE_ANDROID.get(), matteroverdrive.entity.monster.MeleeRogueAndroid.createAttributes().build());
        event.accept(MOEntities.RANGED_ROGUE_ANDROID.get(), matteroverdrive.entity.monster.RangedRogueAndroid.createAttributes().build());
        event.accept(MOEntities.MAD_SCIENTIST.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.5).build());
        event.accept(MOEntities.MUTANT_SCIENTIST.get(), matteroverdrive.entity.monster.MutantScientist.createAttributes().build());
        event.accept(MOEntities.FAILED_PIG.get(), net.minecraft.world.entity.animal.pig.Pig.createAttributes().build());
        event.accept(MOEntities.FAILED_COW.get(), net.minecraft.world.entity.animal.cow.Cow.createAttributes().build());
        event.accept(MOEntities.FAILED_CHICKEN.get(), net.minecraft.world.entity.animal.chicken.Chicken.createAttributes().build());
        event.accept(MOEntities.FAILED_SHEEP.get(), net.minecraft.world.entity.animal.sheep.Sheep.createAttributes().build());
    }

    /** Rogue androids spawn on the ground like monsters (replaces the default placement) */
    public static void registerSpawnPlacements() {
        for (var type : java.util.List.of(MOEntities.ROGUE_ANDROID.get(), MOEntities.RANGED_ROGUE_ANDROID.get())) {
            matteroverdrive.platform.Platform.INSTANCE.replaceSpawnPlacement(type, net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, matteroverdrive.entity.monster.RogueAndroid::checkSpawnRules);
        }
    }

    private MatterOverdrive() {}
}
