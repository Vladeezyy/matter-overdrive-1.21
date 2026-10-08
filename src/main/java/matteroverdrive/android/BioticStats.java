package matteroverdrive.android;

import java.util.LinkedHashMap;
import java.util.Map;

import matteroverdrive.init.MOItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.7.10 MatterOverdriveBioticStats: the 14 stats, their XP costs and how they depend on each other. */
public final class BioticStats {
    private static final Map<String, BioticStat> STATS = new LinkedHashMap<>();

    public static final BioticStat TELEPORT = add(new BioticStat("teleport", 48).hud().wheel());
    public static final BioticStat NANOBOTS = add(new BioticStat("nanobots", 26).hud());
    public static final BioticStat NANO_ARMOR = add(new BioticStat("nano_armor", 30).maxLevel(4).hud());
    public static final BioticStat FLOTATION = add(new BioticStat("floatation", 14).hud());
    public static final BioticStat SPEED = add(new BioticStat("speed", 18).maxLevel(4));
    public static final BioticStat HIGH_JUMP = add(new BioticStat("high_jump", 36).hud());
    public static final BioticStat EQUALIZER = add(new BioticStat("equalizer", 24).hud());
    public static final BioticStat SHIELD = add(new BioticStat("shield", 36).hud().wheel());
    public static final BioticStat ATTACK = add(new BioticStat("attack", 30).maxLevel(4));
    public static final BioticStat CLOAK = add(new BioticStat("cloak", 36).hud().wheel());
    public static final BioticStat NIGHT_VISION = add(new BioticStat("nightvision", 28).hud().wheel());
    public static final BioticStat MINIMAP = add(new BioticStat("minimap", 18));
    public static final BioticStat FLASH_COOLING = add(new BioticStat("flash_cooling", 28));
    public static final BioticStat SHOCKWAVE = add(new BioticStat("shockwave", 32));

    static {
        // 1.7.10 MatterOverdriveBioticStats.init / register
        HIGH_JUMP.requires(() -> new ItemStack(Items.PISTON));
        EQUALIZER.requires(() -> new ItemStack(MOItems.SPACETIME_EQUALIZER.get()));
        TELEPORT.requires(() -> new ItemStack(MOItems.H_COMPENSATOR.get())).disabledWhileActive(SHIELD);
        NANO_ARMOR.root(NANOBOTS).competitor(ATTACK);
        HIGH_JUMP.root(SPEED).disabledWhileActive(SHIELD);
        EQUALIZER.root(HIGH_JUMP);
        SHIELD.root(NANO_ARMOR).requires(() -> new ItemStack(MOItems.FORCEFIELD_EMITTER.get(), 2));
        ATTACK.competitor(NANO_ARMOR).root(NANOBOTS);
        CLOAK.root(SHIELD);
        MINIMAP.requires(() -> new ItemStack(Items.COMPASS));
        FLASH_COOLING.root(ATTACK);
        SHOCKWAVE.root(FLASH_COOLING);
    }

    private static BioticStat add(BioticStat stat) {
        STATS.put(stat.id(), stat);
        return stat;
    }

    public static Iterable<BioticStat> all() {
        return STATS.values();
    }

    public static BioticStat get(String id) {
        return STATS.get(id);
    }

    private BioticStats() {}
}
