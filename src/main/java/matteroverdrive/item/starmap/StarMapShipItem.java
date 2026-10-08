package matteroverdrive.item.starmap;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import matteroverdrive.init.MOItems;
import matteroverdrive.starmap.BuildingType;
import matteroverdrive.starmap.Planet;
import matteroverdrive.starmap.ShipItem;
import matteroverdrive.starmap.ShipType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 ItemScoutShip (180 s) and ItemColonizerShip (250 s; on arrival builds a base and claims the planet). */
public class StarMapShipItem extends StarMapBuildableItem implements ShipItem {
    private final ShipType type;

    public StarMapShipItem(ShipType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public ShipType getType(ItemStack stack) {
        return type;
    }

    @Override
    protected int getBuildLengthUnscaled(ItemStack stack, Planet planet) {
        return type == ShipType.COLONIZER ? 20 * 250 : 20 * 180;
    }

    /** 1.7.10: a colonizer only where there's no base yet. */
    @Override
    public boolean canBuild(ItemStack stack, Planet planet, List<Component> info) {
        return type != ShipType.COLONIZER || !planet.hasBuildingType(BuildingType.BASE);
    }

    /** 1.7.10 ItemColonizerShip.onTravel: the ship becomes the planet's base, the planet the ship owner's. */
    @Override
    public void onTravel(ItemStack stack, Planet to) {
        if (type != ShipType.COLONIZER) return;
        UUID owner = getOwnerID(stack);
        if (owner == null) return;
        ItemStack base = new ItemStack(MOItems.BUILDING_BASE.get());
        ((StarMapBuildableItem) base.getItem()).setOwner(base, owner);
        if (to.canBuild((StarMapBuildingItem) base.getItem(), base, new ArrayList<>())) {
            stack.setCount(0);
            to.addBuilding(base);
            to.setOwnerUUID(owner);
        }
    }
}
