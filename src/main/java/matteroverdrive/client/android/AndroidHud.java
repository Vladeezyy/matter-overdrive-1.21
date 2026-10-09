package matteroverdrive.client.android;

import java.util.List;
import java.util.Random;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.android.AndroidData;
import matteroverdrive.android.BioticStat;
import matteroverdrive.android.BioticStats;
import matteroverdrive.item.weapon.EnergyPackItem;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 1.7.10 GuiAndroidHud: health / energy / speed and weapon readouts top left, bionic parts and abilities top right,
 * a radar bottom left (1.7.10 minimap), the glitch overlay when hurt, the cloak tint, and the transformation screen.
 * The vanilla health, food and air bars are hidden for androids.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class AndroidHud {
    private static final int HOLO = 0xA9E2FB, HOLO_RED = 0xE65014;
    /** 1.7.10 baseGuiColor: COLOR_HOLO x0.5 at the default 50% HUD opacity. */
    private static final int BASE = ARGB.color(128, ARGB.scaleRGB(HOLO, 0.5f) | 0);
    private static final Random RANDOM = new Random();
    private static final String[] TRANSFORM_LINES = {"0", "1", "2", "3", "4"};

    /** Draws a whole texture of srcW x srcH scaled to w x h. */
    static void icon(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int srcW, int srcH, int color) {
        matteroverdrive.compat.Gui.blit(g, tex, x, y, 0, 0, w, h, srcW, srcH, srcW, srcH, color);
    }

    private static ResourceLocation elem(String name) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/gui/elements/" + name + ".png");
    }

    private static ResourceLocation gui(String name) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/gui/" + name + ".png");
    }

    @SubscribeEvent
    static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "android_hud"), AndroidHud::render);
    }

    /** 1.7.10 hideVanillaHudElements, and no player list while the ability wheel (same key) is open. */
    @SubscribeEvent
    static void hideVanilla(RenderGuiLayerEvent.Pre event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !Android.isAndroid(player)) return;
        var name = event.getName();
        if (name.equals(VanillaGuiLayers.PLAYER_HEALTH) || name.equals(VanillaGuiLayers.FOOD_LEVEL) || name.equals(VanillaGuiLayers.AIR_LEVEL)
                || name.equals(VanillaGuiLayers.TAB_LIST) && Minecraft.getInstance().screen instanceof AbilityWheelScreen) {
            event.setCanceled(true);
        }
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;
        AndroidData data = Android.get(player);
        if (data.isAndroid()) {
            if (BioticStats.CLOAK.isActive(player, data, 1)) {
                icon(g, elem("cloak_overlay"), 0, 0, g.guiWidth(), g.guiHeight(), 128, 128, ARGB.color(90, 0xFFFFFF));
            }
            renderStats(g, mc.font, player, data);
            renderBionicStats(g, mc.font, player, data);
            if (data.isUnlocked(BioticStats.MINIMAP, 1)) renderRadar(g, player);
            if (data.getGlitchTime() > 0 || Android.getEnergy(player) <= 0 && player.level().getGameTime() % 60 < 5) renderGlitch(g);
        } else if (data.isTurning()) {
            renderTransformation(g, mc.font, player, data);
        }
    }

    // --- top left: health, battery, speed; ammo and heat with a weapon ------------------------------------

    private static void renderStats(GuiGraphics g, Font font, Player player, AndroidData data) {
        int x = 12, y = 12;
        matteroverdrive.compat.Gui.blit(g, elem("android_bg_element"), x, y, 0, 0, 174, 11, 174, 11, BASE);
        y += 10;
        x += 5;
        double health = player.getHealth() / player.getAttributeBaseValue(Attributes.MAX_HEALTH);
        double energy = Android.getEnergy(player) / (double) Math.max(1, Android.getMaxEnergy(player));
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED) / player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        x += iconWithPercent(g, font, "health", health, x, y, 0, HOLO_RED, BASE, 18);
        x += iconWithPercent(g, font, "battery", energy, x, y, -2, HOLO_RED, BASE, 20);
        iconWithPercent(g, font, "person", speed, x, y, 1, BASE, BASE, 14);
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof EnergyWeaponItem weapon) {
            int wx = 17;
            y += 20;
            float ammo = EnergyWeaponItem.getEnergy(held) / (float) EnergyWeaponItem.getCapacity(held);
            int packs = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof EnergyPackItem) packs += stack.getCount();
            }
            wx += iconWithInfo(g, font, "ammo", Math.round(ammo * 100) + "% | " + packs, lerp(HOLO_RED, BASE, ammo), wx, y, 0, 18);
            float heat = EnergyWeaponItem.getHeat(held) / weapon.getMaxHeat(held);
            iconWithPercent(g, font, "temperature", heat, wx, y, 0, BASE, HOLO_RED, 18);
        }
    }

    private static int iconWithPercent(GuiGraphics g, Font font, String icon, double amount, int x, int y, int iconOffsetY, int from, int to, int size) {
        return iconWithInfo(g, font, icon, Math.round(amount * 100) + "%", lerp(from, to, (float) Mth.clamp(amount, 0, 1)), x, y, iconOffsetY, size);
    }

    private static int iconWithInfo(GuiGraphics g, Font font, String icon, String info, int color, int x, int y, int iconOffsetY, int size) {
        int c = ARGB.color(255, color);
        int src = icon.equals("battery") ? 16 : 18;
        icon(g, elem(icon), x, y + iconOffsetY, size, size, src, src, c);
        g.drawString(font, info, x + size + 2, y + size / 2 - font.lineHeight / 2 + iconOffsetY, c, false);
        return font.width(info) + 2 + size + 2;
    }

    private static int lerp(int from, int to, float t) {
        return ARGB.lerp(t, ARGB.color(255, from), ARGB.color(255, to));
    }

    // --- top right: parts and abilities ---------------------------------------------------------------

    private static void renderBionicStats(GuiGraphics g, Font font, Player player, AndroidData data) {
        int right = g.guiWidth() - 12;
        int count = 0;
        for (int slot = 0; slot < AndroidData.SLOTS; slot++) {
            ItemStack stack = data.getStack(slot);
            if (stack.isEmpty()) continue;
            int[] pos = cell(right, count++);
            icon(g, elem("android_feature_icon_bg"), pos[0], pos[1], 22, 22, 22, 22, ARGB.color(255, BASE));
            g.renderItem(stack, pos[0] + 3, pos[1] + 3);
        }
        for (BioticStat stat : BioticStats.all()) {
            int level = data.getUnlockedLevel(stat);
            if (level <= 0 || !stat.showOnHud(data, level)) continue;
            int[] pos = cell(right, count++);
            int color = ARGB.color(255, stat.isEnabled(player, data, level) ? BASE : HOLO_RED);
            if (stat.isActive(player, data, level)) {
                // first frame of the 4-frame active background
                matteroverdrive.compat.Gui.blit(g, elem("android_feature_icon_bg_active"), pos[0], pos[1], 0, 0, 22, 22, 22, 22, 22, 88, color);
            } else {
                icon(g, elem("android_feature_icon_bg"), pos[0], pos[1], 22, 22, 22, 22, color);
            }
            icon(g, elem("biotic_stat_" + stat.id()), pos[0] + 2, pos[1] + 2, 18, 18, 18, 18, color);
            int delay = stat.getDelay(player, data, level);
            if (delay > 0) {
                String text = (delay / 20) + "s";
                g.drawString(font, text, pos[0] + 22 - font.width(text), pos[1] + 22 - font.lineHeight - 1, ARGB.color(255, HOLO), false);
            }
        }
        matteroverdrive.compat.Gui.blit(g, elem("android_bg_element"), right - 174, 12, 0, 0, 174, 11, 174, 11, BASE);
    }

    /** 1.7.10 AndroidHudBionicStats: six per row, filled from the right edge below the bar. */
    private static int[] cell(int right, int count) {
        return new int[] {right - 22 - 24 * (count % 6), 26 + 24 * (count / 6)};
    }

    // --- bottom left: radar (1.7.10 minimap, without the terrain) -------------------------------------

    private static void renderRadar(GuiGraphics g, Player player) {
        int size = 64, cx = 12 + size / 2, cy = g.guiHeight() - 40 - size / 2;
        g.fill(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, 0x40000000);
        int edge = ARGB.color(160, HOLO);
        g.fill(cx - size / 2, cy - size / 2, cx + size / 2, cy - size / 2 + 1, edge);
        g.fill(cx - size / 2, cy + size / 2 - 1, cx + size / 2, cy + size / 2, edge);
        g.fill(cx - size / 2, cy - size / 2, cx - size / 2 + 1, cy + size / 2, edge);
        g.fill(cx + size / 2 - 1, cy - size / 2, cx + size / 2, cy + size / 2, edge);
        double range = 32;
        float yaw = (float) Math.toRadians(player.getYRot());
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range, 16, range),
                e -> e != player && !e.isInvisible());
        for (LivingEntity entity : entities) {
            double dx = entity.getX() - player.getX(), dz = entity.getZ() - player.getZ();
            // rotate so the player's view points up
            double rx = dx * Math.cos(yaw) + dz * Math.sin(yaw);
            double rz = -dx * Math.sin(yaw) + dz * Math.cos(yaw);
            int px = cx - (int) (rx / range * size / 2), py = cy - (int) (rz / range * size / 2);
            if (Math.abs(px - cx) >= size / 2 - 1 || Math.abs(py - cy) >= size / 2 - 1) continue;
            int color = entity instanceof Enemy ? 0xFFE65014 : entity instanceof Player ? 0xFFFFFFFF : 0xFF66FF66;
            g.fill(px - 1, py - 1, px + 1, py + 1, color);
        }
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, ARGB.color(255, HOLO));
    }

    // --- glitch and transformation --------------------------------------------------------------------

    private static void renderGlitch(GuiGraphics g) {
        int u = (int) (RANDOM.nextGaussian() * 64), v = (int) (RANDOM.nextGaussian() * 64);
        matteroverdrive.compat.Gui.blit(g, gui("glitch"), 0, 0, Math.abs(u), Math.abs(v), g.guiWidth(), g.guiHeight(), 1280 - 128, 720 - 128,
                1280, 720, ARGB.color(160, 0xFFFFFF));
    }

    /** 1.7.10 renderTransformAnimation: typed status lines, a spinner and the percentage. */
    private static void renderTransformation(GuiGraphics g, Font font, Player player, AndroidData data) {
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        int time = Android.TRANSFORM_TIME - data.getTurning();
        if (time % 40 > 0 && time % 40 < 3) renderGlitch(g);
        String text = typedText(time);
        g.drawString(font, text, cx - font.width(text) / 2, cy - 28, ARGB.color(255, HOLO), false);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float) Math.toRadians(-(player.level().getGameTime() * 10 % 360))));
        icon(g, elem("spinner"), -16, -16, 32, 32, 64, 64, 0xFFFFFFFF);
        g.pose().popPose();
        String percent = Math.round(time * 100f / Android.TRANSFORM_TIME) + "%";
        g.drawString(font, percent, cx - font.width(percent) / 2, cy - 3, ARGB.color(255, HOLO), false);
    }

    /** 1.7.10 AnimationTextTyping: each line types in then out at 2 ticks per letter; the last one stays. */
    private static String typedText(int time) {
        int t = time;
        for (String line : TRANSFORM_LINES) {
            String text = net.minecraft.client.resources.language.I18n.get("gui." + MatterOverdrive.MODID + ".android_hud.transforming.line." + line);
            int length = text.length() * 2;
            if (t < length) return text.substring(0, t / 2);
            t -= length;
            if (t < length) return text.substring(0, text.length() - t / 2);
            t -= length;
        }
        String last = net.minecraft.client.resources.language.I18n.get("gui." + MatterOverdrive.MODID + ".android_hud.transforming.line.final");
        return last.substring(0, Math.min(last.length(), t / 2));
    }

    private AndroidHud() {}
}
