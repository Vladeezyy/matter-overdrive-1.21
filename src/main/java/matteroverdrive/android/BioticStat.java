package matteroverdrive.android;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 AbstractBioticStat: an android ability or upgrade bought with XP levels at an android station. Stats form a
 * tree (a root must be fully unlocked first), may lock out competitors, may need items, and may be disabled while
 * another stat is active.
 */
public class BioticStat {
    private final String id;
    private final int xp;
    private int maxLevel = 1;
    private BioticStat root;
    private final List<BioticStat> competitors = new ArrayList<>();
    private final List<Supplier<ItemStack>> requiredItems = new ArrayList<>();
    private final List<BioticStat> enabledBlacklist = new ArrayList<>();
    private boolean showOnHud;
    private boolean showOnWheel;

    public BioticStat(String id, int xp) {
        this.id = id;
        this.xp = xp;
    }

    public String id() {
        return id;
    }

    public int xp() {
        return xp;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public BioticStat root() {
        return root;
    }

    public List<BioticStat> competitors() {
        return competitors;
    }

    public List<ItemStack> requiredItems() {
        return requiredItems.stream().map(Supplier::get).toList();
    }

    public boolean showOnHud() {
        return showOnHud;
    }

    public boolean showOnWheel() {
        return showOnWheel;
    }

    BioticStat maxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
        return this;
    }

    BioticStat root(BioticStat root) {
        this.root = root;
        return this;
    }

    BioticStat competitor(BioticStat stat) {
        competitors.add(stat);
        return this;
    }

    BioticStat requires(Supplier<ItemStack> item) {
        requiredItems.add(item);
        return this;
    }

    BioticStat disabledWhileActive(BioticStat stat) {
        enabledBlacklist.add(stat);
        return this;
    }

    BioticStat hud() {
        showOnHud = true;
        return this;
    }

    BioticStat wheel() {
        showOnWheel = true;
        return this;
    }

    // --- unlocking (1.7.10 canBeUnlocked / onUnlock) ----------------------------------------------

    public boolean competitorsUnlocked(AndroidData data) {
        return competitors.stream().anyMatch(c -> data.isUnlocked(c, 0));
    }

    public boolean canBeUnlocked(Player player, AndroidData data, int level) {
        if (root != null && !data.isUnlocked(root, root.maxLevel())) return false;
        if (competitorsUnlocked(data)) return false;
        if (!player.isCreative()) {
            for (ItemStack required : requiredItems()) {
                if (player.getInventory().countItem(required.getItem()) < required.getCount()) return false;
            }
        }
        return data.isAndroid() && (player.isCreative() || player.experienceLevel >= xp);
    }

    /** Takes the XP levels and the required items. */
    public void onUnlock(Player player, int level) {
        player.giveExperienceLevels(-xp);
        if (player.isCreative()) return;
        for (ItemStack required : requiredItems()) {
            int left = required.getCount();
            for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(required.getItem())) {
                    int take = Math.min(left, stack.getCount());
                    stack.shrink(take);
                    left -= take;
                }
            }
        }
    }

    /** Values for the %s placeholders of the stat's details text (1.7.10 getDetails). */
    public Object[] detailArgs(int level) {
        return new Object[0];
    }

    // --- behaviour (1.7.10 IBionicStat hooks), overridden by the stats that do something ----------------

    /** 1.7.10 isEnabled: off while a blacklisted stat is active. */
    public boolean isEnabled(Player player, AndroidData data, int level) {
        return enabledBlacklist.stream().noneMatch(s -> s.isActive(player, data, data.getUnlockedLevel(s)));
    }

    /** Whether a toggled or timed ability is currently running (shield up, cloaked...). */
    public boolean isActive(Player player, AndroidData data, int level) {
        return false;
    }

    /** Every tick while unlocked and enabled. */
    public void onAndroidTick(ServerPlayer player, AndroidData data, int level) {}
}
