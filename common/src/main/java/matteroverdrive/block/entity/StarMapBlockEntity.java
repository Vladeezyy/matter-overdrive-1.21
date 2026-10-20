package matteroverdrive.block.entity;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.starmap.GalacticPosition;
import matteroverdrive.starmap.Galaxy;
import matteroverdrive.starmap.GalaxyClient;
import matteroverdrive.starmap.GalaxyServer;
import matteroverdrive.starmap.Planet;
import matteroverdrive.starmap.SpaceBody;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;

/**
 * 1.7.10 TileEntityMachineStarMap: no energy, no upgrades. Shows the galaxy as a hologram at one of five zoom levels
 * (galaxy, quadrant, star, planet, planet stats - the last two only with a planet selected); "position" is where the
 * player is (the placer's homeworld) and "destination" the selected body. Placing it claims it for the placer.
 */
public class StarMapBlockEntity extends MachineBlockEntity {
    private GalacticPosition position = GalacticPosition.NONE;
    private GalacticPosition destination = GalacticPosition.NONE;
    private int zoomLevel;

    public StarMapBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.STAR_MAP.get(), pos, state, slots(), false, 0, 0, 0, 0, Set.of());
    }

    /** 1.7.10 RegisterSlots: Planet.SLOT_COUNT slots, used while no planet is selected. */
    private static MachineInventory.Builder slots() {
        MachineInventory.Builder b = MachineInventory.builder();
        for (int i = 0; i < Planet.SLOT_COUNT; i++) b.add(MachineInventory.Role.OTHER, r -> false, 1);
        return b;
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        return false;
    }

    // --- zoom / position (1.7.10 zoom, getMaxZoom, onPlaced) ---------------------------------------------

    private @Nullable Galaxy galaxy() {
        return level != null && level.isClientSide() ? GalaxyClient.getGalaxy() : GalaxyServer.getGalaxy();
    }

    public @Nullable Planet getPlanet() {
        Galaxy galaxy = galaxy();
        return galaxy == null ? null : galaxy.getPlanet(destination);
    }

    public int getMaxZoom() {
        return getPlanet() != null ? 4 : 2;
    }

    /** Sneak-use: the next zoom level, back to the galaxy after the last. */
    public void zoom() {
        setZoomLevel(zoomLevel < getMaxZoom() ? zoomLevel + 1 : 0);
        sync();
    }

    public void onPlaced(Player player) {
        Planet homeworld = level != null && level.isClientSide() ? GalaxyClient.getHomeworld(player) : GalaxyServer.getHomeworld(player);
        if (homeworld != null) position = GalacticPosition.of(homeworld);
        destination = position;
        claimFor(player);
        sync();
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public GalacticPosition getGalaxyPosition() {
        return position;
    }

    public void setGalacticPosition(GalacticPosition position) {
        this.position = position;
    }

    public GalacticPosition getDestination() {
        return destination;
    }

    public void setDestination(GalacticPosition destination) {
        this.destination = destination;
    }

    /** 1.7.10 getActiveSpaceBody (client): galaxy, quadrant, star or planet by zoom. */
    public @Nullable SpaceBody getActiveSpaceBody() {
        return switch (zoomLevel) {
            case 0 -> GalaxyClient.getGalaxy();
            case 1 -> GalaxyClient.getQuadrant(destination);
            case 2 -> GalaxyClient.getStar(destination);
            default -> GalaxyClient.getPlanet(destination);
        };
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new matteroverdrive.menu.StarMapMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putByte("ZoomLevel", (byte) zoomLevel);
        output.store("GalacticPosition", net.minecraft.nbt.CompoundTag.CODEC, position.toNBT());
        output.store("GalacticDestination", net.minecraft.nbt.CompoundTag.CODEC, destination.toNBT());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        zoomLevel = input.getByteOr("ZoomLevel", (byte) 0);
        position = input.read("GalacticPosition", net.minecraft.nbt.CompoundTag.CODEC).map(GalacticPosition::fromNBT).orElse(GalacticPosition.NONE);
        destination = input.read("GalacticDestination", net.minecraft.nbt.CompoundTag.CODEC).map(GalacticPosition::fromNBT).orElse(GalacticPosition.NONE);
    }
}
