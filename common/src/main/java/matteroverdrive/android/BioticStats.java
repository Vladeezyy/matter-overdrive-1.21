package matteroverdrive.android;

import java.util.LinkedHashMap;
import java.util.Map;

import matteroverdrive.init.MOItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;

/** 1.7.10 MatterOverdriveBioticStats: the 14 stats, their XP costs and how they depend on each other. */
public final class BioticStats {
    private static final Map<String, BioticStat> STATS = new LinkedHashMap<>();

    public static final BioticStat TELEPORT = add(new StatImpls.Teleport().hud().wheel());
    public static final BioticStat NANOBOTS = add(new StatImpls.Nanobots().hud());
    public static final BioticStat NANO_ARMOR = add(new StatImpls.NanoArmor().maxLevel(4).hud());
    public static final BioticStat FLOTATION = add(new StatImpls.Flotation().hud());
    public static final BioticStat SPEED = add(new StatImpls.Speed().maxLevel(4));
    public static final BioticStat HIGH_JUMP = add(new StatImpls.HighJump().hud());
    public static final BioticStat EQUALIZER = add(new BioticStat("equalizer", 24).hud());
    public static final BioticStat SHIELD = add(new StatImpls.Shield().hud().wheel());
    public static final BioticStat ATTACK = add(new StatImpls.Attack().maxLevel(4));
    public static final BioticStat CLOAK = add(new StatImpls.Cloak().hud().wheel());
    public static final BioticStat NIGHT_VISION = add(new StatImpls.NightVision().hud().wheel());
    public static final BioticStat MINIMAP = add(new BioticStat("minimap", 18));
    public static final BioticStat FLASH_COOLING = add(new StatImpls.FlashCooling());
    public static final BioticStat SHOCKWAVE = add(new StatImpls.Shockwave().hud().wheel());

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

    /** 1.7.10 MOEventEnergyWeapon.Overheat + BioticStatFlashCooling: may cancel an overheat. */
    public static boolean flashCool(Player player) {
        AndroidData data = Android.get(player);
        int level = data.getUnlockedLevel(FLASH_COOLING);
        return data.isAndroid() && level > 0 && FLASH_COOLING.isEnabled(player, data, level)
                && player.getRandom().nextFloat() < StatImpls.FlashCooling.COOLDOWN_CHANGE;
    }

    /** Whether the player has the stat unlocked, enabled, and is an android (e.g. the equalizer against anomalies). */
    public static boolean has(Player player, BioticStat stat) {
        AndroidData data = Android.get(player);
        int level = data.getUnlockedLevel(stat);
        return data.isAndroid() && level > 0 && stat.isEnabled(player, data, level);
    }

    public static BioticStat get(String id) {
        return STATS.get(id);
    }

    private BioticStats() {}
}
