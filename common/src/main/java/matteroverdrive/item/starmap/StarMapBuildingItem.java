package matteroverdrive.item.starmap;

import java.util.List;

import matteroverdrive.starmap.BuildingItem;
import matteroverdrive.starmap.BuildingType;
import matteroverdrive.starmap.Planet;
import matteroverdrive.starmap.PlanetStatChange;
import matteroverdrive.starmap.PlanetStatType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 ItemBuildingBase / ShipFactory / ItemBuildingResidential / ItemBuildingMatterExtractor / ItemBuildingPowerGenerator /
 * ItemBuildingShipHangar: a building type, build length and the planet stats it changes.
 */
public class StarMapBuildingItem extends StarMapBuildableItem implements BuildingItem, PlanetStatChange {
    public enum Kind {
        /** +2 building spaces; only one per planet; 500 s. */
        BASE(BuildingType.BASE, 20 * 500),
        SHIP_FACTORY(BuildingType.SHIP_FACTORY, 20 * 400),
        /** 10000 population, -4 energy, -2 matter, +4 building spaces, happiness from energy / matter balance; 5 min. */
        RESIDENTIAL(BuildingType.RESIDENTIAL, 20 * 60 * 5),
        /** +10 matter, -6 energy; 12 min. */
        MATTER_EXTRACTOR(BuildingType.ORE_EXTRACTOR, 20 * 60 * 12),
        /** +8 energy, -2 matter; 12 min. */
        POWER_GENERATOR(BuildingType.GENERATOR, 20 * 60 * 12),
        /** +2 fleet spaces; 4 min. */
        SHIP_HANGAR(BuildingType.OTHER, 20 * 60 * 4);

        final BuildingType type;
        final int buildLength;

        Kind(BuildingType type, int buildLength) {
            this.type = type;
            this.buildLength = buildLength;
        }
    }

    private final Kind kind;

    public StarMapBuildingItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public BuildingType getType(ItemStack stack) {
        return kind.type;
    }

    @Override
    protected int getBuildLengthUnscaled(ItemStack stack, Planet planet) {
        return kind.buildLength;
    }

    /** 1.7.10: a second base can't be built; everything else always can (the planet checks spaces and the base). */
    @Override
    public boolean canBuild(ItemStack stack, Planet planet, List<Component> info) {
        if (kind == Kind.BASE && planet.hasBuildingType(BuildingType.BASE)) {
            info.add(Component.translatable("gui.matteroverdrive.starmap.has_base"));
            return false;
        }
        return true;
    }

    @Override
    public float changeStat(ItemStack stack, Planet planet, PlanetStatType stat, float original) {
        return switch (kind) {
            case BASE -> stat == PlanetStatType.BUILDINGS_SIZE ? original + 2 : original;
            case SHIP_FACTORY -> original;
            case RESIDENTIAL -> switch (stat) {
                case POPULATION_COUNT -> original + 10000;
                case ENERGY_PRODUCTION -> original - 4;
                case MATTER_PRODUCTION -> original - 2;
                case BUILDINGS_SIZE -> original + 4;
                case HAPPINESS -> original + (planet.getPowerProduction() >= 0 ? 0.5f : -0.4f) + (planet.getMatterProduction() >= 0 ? 0.5f : -0.6f);
                default -> original;
            };
            case MATTER_EXTRACTOR -> switch (stat) {
                case MATTER_PRODUCTION -> original + 10;
                case ENERGY_PRODUCTION -> original - 6;
                default -> original;
            };
            case POWER_GENERATOR -> switch (stat) {
                case ENERGY_PRODUCTION -> original + 8;
                case MATTER_PRODUCTION -> original - 2;
                default -> original;
            };
            case SHIP_HANGAR -> stat == PlanetStatType.FLEET_SIZE ? original + 2 : original;
        };
    }

    /** 1.7.10 ItemBuildingBase.addDetails formatted the building space increase into the details. */
    @Override
    protected String details() {
        String details = super.details();
        if (kind == Kind.BASE) {
            String[] lines = details.split("/n");
            if (lines.length >= 2) {
                lines[1] = String.format(lines[1], 2);
                return String.join("/n", lines);
            }
        }
        return details;
    }
}
