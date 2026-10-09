package matteroverdrive.client.screen;

import java.util.ArrayList;
import java.util.List;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.android.AndroidData;
import matteroverdrive.android.BioticStat;
import matteroverdrive.android.BioticStats;
import matteroverdrive.init.MOSounds;
import matteroverdrive.menu.AndroidStationMenu;
import matteroverdrive.network.UnlockStatPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * 1.7.10 GuiAndroidStation: the biotic stat tree (a 4x4 grid of holo buttons with arrows from their roots), the
 * android's part slots either side of a rotating view of the player, and the player's XP.
 */
public class AndroidStationScreen extends MachineScreen<AndroidStationMenu> {
    private static final ResourceLocation SLOT_HOLO = tex("slot_holo");
    private static final ResourceLocation UP_ARROW = tex("up_arrow");
    private static final ResourceLocation BLACK_CIRCLE = tex("black_circle");
    private static final ResourceLocation[] PART_ICONS = {tex("android_slot_head"), tex("android_slot_arms"), tex("android_slot_legs"),
            tex("android_slot_chest"), tex("android_slot_other"), tex("holo_battery")};
    private static final String[] PART_NAMES = {"head", "arms", "legs", "chest", "other", "battery"};
    private static final int HOLO = 0xA9E2FB, HOLO_RED = 0xE65014, MATTER = 0xBFE4E6;

    /** 1.7.10 addStat(stat, x, y, direction): grid cell and where the arrow to its root points. */
    private record Cell(BioticStat stat, int x, int y, int dx, int dy) {
        int px() {
            return 54 + x * 30;
        }

        int py() {
            return 36 + y * 30;
        }
    }

    private static final List<Cell> CELLS = List.of(
            new Cell(BioticStats.TELEPORT, 0, 0, 0, 0), new Cell(BioticStats.NANOBOTS, 1, 1, 0, 0),
            new Cell(BioticStats.NANO_ARMOR, 0, 1, 1, 0), new Cell(BioticStats.FLOTATION, 2, 0, 0, 0),
            new Cell(BioticStats.SPEED, 3, 0, 0, 0), new Cell(BioticStats.HIGH_JUMP, 3, 1, 0, -1),
            new Cell(BioticStats.EQUALIZER, 3, 2, 0, -1), new Cell(BioticStats.SHIELD, 0, 2, 0, -1),
            new Cell(BioticStats.ATTACK, 2, 1, -1, 0), new Cell(BioticStats.CLOAK, 0, 3, 0, -1),
            new Cell(BioticStats.NIGHT_VISION, 1, 0, 0, 0), new Cell(BioticStats.MINIMAP, 1, 2, 0, 0),
            new Cell(BioticStats.FLASH_COOLING, 2, 2, 0, -1), new Cell(BioticStats.SHOCKWAVE, 2, 3, 0, -1));

    public AndroidStationScreen(AndroidStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private static ResourceLocation statIcon(BioticStat stat) {
        return tex("biotic_stat_" + stat.id());
    }

    private Player player() {
        return minecraft.player;
    }

    @Override
    protected boolean drawSlotBackground(Slot slot) {
        return !(slot instanceof AndroidStationMenu.PartSlot);
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        AndroidData data = Android.get(player());
        for (Cell cell : CELLS) {
            int level = data.getUnlockedLevel(cell.stat);
            int color = cell.stat.canBeUnlocked(player(), data, level) || data.isUnlocked(cell.stat, level)
                    ? (level <= 0 ? ARGB.color(255, ARGB.scaleRGB(HOLO, 0.5f)) : ARGB.color(255, HOLO))
                    : ARGB.color(255, ARGB.scaleRGB(HOLO_RED, 0.5f));
            int px = x + cell.px(), py = y + cell.py();
            scaled(g, SLOT_HOLO, px, py, 22, 18, color);
            scaled(g, statIcon(cell.stat), px + 3, py + 3, 16, 18, color);
            if (cell.dx != 0 || cell.dy != 0) {
                arrow(g, px + 11 + Math.round(cell.dx * 22 * 0.75f), py + 11 + Math.round(cell.dy * 22 * 0.75f), cell.dx, cell.dy, color);
            }
            if (cell.stat.maxLevel() > 1 && level > 0) {
                scaled(g, BLACK_CIRCLE, px + 14, py + 14, 10, 18, 0xFFFFFFFF);
                g.drawString(font, Integer.toString(level), px + 16, py + 16, 0xFFFFFFFF, false);
            }
        }
        // part slots: holo frames, an icon while empty
        for (Slot slot : menu.slots) {
            if (!(slot instanceof AndroidStationMenu.PartSlot part) || !slot.isActive()) continue;
            int sx = x + slot.x - 2, sy = y + slot.y - 2;
            scaled(g, SLOT_HOLO, sx, sy, 20, 18, ARGB.color(78 * 2, MATTER));
            if (!slot.hasItem()) {
                matteroverdrive.compat.Gui.blit(g, PART_ICONS[part.part], sx + 2, sy + 2, 0, 0, 16, 16, 16, 16, ARGB.color(160, MATTER));
            }
        }
        // the player, turning with the mouse (1.7.10 turned it with world time)
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 250, y + 98, x + 310, y + 212, 45, 0.0625f,
                x + mx, y + my, player());
        String xp = player().experienceLevel + " XP";
        g.drawString(font, xp, x + 280 - font.width(xp) / 2, y + imageHeight - 24, 0xFF55FF55, false);
    }

    /** A square texture of src px drawn at size px. */
    private static void scaled(GuiGraphics g, ResourceLocation tex, int x, int y, int size, int src, int color) {
        matteroverdrive.compat.Gui.blit(g, tex, x, y, 0, 0, size, size, src, src, src, src, color);
    }

    /** 1.7.10 up_arrow holo icon, turned towards the stat's root. */
    private static void arrow(GuiGraphics g, int cx, int cy, int dx, int dy, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        float angle = dx == 1 ? 90 : dx == -1 ? -90 : dy == 1 ? 180 : 0;
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float) Math.toRadians(angle)));
        matteroverdrive.compat.Gui.blit(g, UP_ARROW, -4, -4, 0, 0, 7, 7, 7, 7, color);
        g.pose().popPose();
    }

    @Override
    protected void homeTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        AndroidData data = Android.get(player());
        for (Cell cell : CELLS) {
            if (in(mx, my, cell.px(), cell.py(), 22, 22)) {
                matteroverdrive.compat.Gui.setComponentTooltipForNextFrame(g, font, tooltip(cell.stat, data, data.getUnlockedLevel(cell.stat)), mouseX, mouseY);
                return;
            }
        }
        for (Slot slot : menu.slots) {
            if (slot instanceof AndroidStationMenu.PartSlot part && slot.isActive() && !slot.hasItem() && in(mx, my, slot.x, slot.y, 16, 16)) {
                matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, Component.translatable("gui." + MatterOverdrive.MODID + ".biopart." + PART_NAMES[part.part]), mouseX, mouseY);
            }
        }
    }

    /** 1.7.10 AbstractBioticStat.onTooltip. */
    private List<Component> tooltip(BioticStat stat, AndroidData data, int level) {
        List<Component> lines = new ArrayList<>();
        MutableComponent name = Component.translatable("biotic_stat." + MatterOverdrive.MODID + "." + stat.id() + ".name");
        if (stat.maxLevel() > 1) name.append(String.format(" [%s/%s]", level, stat.maxLevel()));
        lines.add(name.withStyle(ChatFormatting.WHITE));
        String details = Component.translatable("biotic_stat." + MatterOverdrive.MODID + "." + stat.id() + ".details", stat.detailArgs(level)).getString();
        for (String line : details.split("/n/")) {
            lines.add(Component.literal(line.trim()).withStyle(ChatFormatting.GRAY));
        }
        MutableComponent requires = Component.empty();
        boolean any = false;
        if (stat.root() != null) {
            requires.append(Component.literal("[").append(Component.translatable("biotic_stat." + MatterOverdrive.MODID + "." + stat.root().id() + ".name"))
                    .append(stat.root().maxLevel() > 1 ? " " + stat.root().maxLevel() + "]" : "]").withStyle(ChatFormatting.GOLD));
            any = true;
        }
        for (ItemStack item : stat.requiredItems()) {
            if (any) requires.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            if (item.getCount() > 1) requires.append(Component.literal(item.getCount() + "x").withStyle(ChatFormatting.WHITE));
            requires.append(Component.literal("[").append(item.getHoverName()).append("]").withStyle(ChatFormatting.WHITE));
            any = true;
        }
        if (any) {
            lines.add(Component.translatable("gui." + MatterOverdrive.MODID + ".requires").append(": ").append(requires));
        }
        if (!stat.competitors().isEmpty()) {
            MutableComponent locks = Component.translatable("gui." + MatterOverdrive.MODID + ".locks").append(": ");
            for (BioticStat competitor : stat.competitors()) {
                locks.append("[").append(Component.translatable("biotic_stat." + MatterOverdrive.MODID + "." + competitor.id() + ".name")).append("] ");
            }
            lines.add(locks.withStyle(ChatFormatting.RED));
        }
        if (level < stat.maxLevel()) {
            lines.add(Component.literal("XP: " + stat.xp()).withStyle(player().experienceLevel < stat.xp() ? ChatFormatting.RED : ChatFormatting.GREEN));
        }
        return lines;
    }

    /** 1.7.10 ElementBioStat.onAction: buy the next level if it can be unlocked. */
    @Override
    protected boolean homeClicked(double mx, double my) {
        AndroidData data = Android.get(player());
        for (Cell cell : CELLS) {
            if (!in(mx, my, cell.px(), cell.py(), 22, 22)) continue;
            int level = data.getUnlockedLevel(cell.stat);
            if (level < cell.stat.maxLevel() && cell.stat.canBeUnlocked(player(), data, level + 1)) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(MOSounds.BIOTIC_STAT_UNLOCK.get(), 1));
                PacketDistributor.sendToServer(new UnlockStatPayload(cell.stat.id(), level + 1));
            }
            return true;
        }
        return false;
    }
}
