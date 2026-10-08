package matteroverdrive.client.android;

import java.util.ArrayList;
import java.util.List;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.android.AndroidData;
import matteroverdrive.android.BioticStat;
import matteroverdrive.android.BioticStats;
import matteroverdrive.network.AndroidPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 1.7.10 GuiAndroidHud radial menu: while the switch key is held the unlocked wheel abilities sit on a ring; pointing
 * at one selects it (sent to the server when the key is released).
 */
public class AbilityWheelScreen extends Screen {
    private static final int HOLO = 0xA9E2FB;
    private static final int RADIUS = 56;
    private final List<BioticStat> stats = new ArrayList<>();
    private int selected = -1;

    public AbilityWheelScreen() {
        super(Component.translatable("key." + MatterOverdrive.MODID + ".ability_switch"));
    }

    static List<BioticStat> wheelStats(Player player) {
        AndroidData data = Android.get(player);
        List<BioticStat> list = new ArrayList<>();
        for (BioticStat stat : BioticStats.all()) {
            if (stat.showOnWheel() && data.isUnlocked(stat, 1)) list.add(stat);
        }
        return list;
    }

    static boolean hasStats(Player player) {
        return !wheelStats(player).isEmpty();
    }

    @Override
    protected void init() {
        stats.clear();
        stats.addAll(wheelStats(minecraft.player));
        String active = Android.get(minecraft.player).getActiveStat();
        for (int i = 0; i < stats.size(); i++) {
            if (stats.get(i).id().equals(active)) selected = i;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2, cy = height / 2;
        double dx = mouseX - cx, dy = mouseY - cy;
        // 1.7.10: a direction past 20% of the ring picks the slice it points into (slice 0 straight up, clockwise)
        if (Math.sqrt(dx * dx + dy * dy) > RADIUS * 0.2 && !stats.isEmpty()) {
            double angle = (Math.toDegrees(Math.atan2(dx, -dy)) + 360) % 360;
            double slice = 360.0 / stats.size();
            selected = (int) Math.floor(((angle + slice / 2) % 360) / slice);
        }
        for (int i = 0; i < stats.size(); i++) {
            double a = Math.toRadians(360.0 / stats.size() * i);
            int x = cx + (int) Math.round(Math.sin(a) * RADIUS) - 11, y = cy - (int) Math.round(Math.cos(a) * RADIUS) - 11;
            int color = ARGB.color(i == selected ? 255 : 120, HOLO);
            AndroidHud.icon(g, tex("slot_holo"), x, y, 22, 22, 18, 18, color);
            AndroidHud.icon(g, tex("biotic_stat_" + stats.get(i).id()), x + 3, y + 3, 16, 16, 18, 18, color);
        }
        if (selected >= 0) {
            Component name = Component.translatable("biotic_stat." + MatterOverdrive.MODID + "." + stats.get(selected).id() + ".name");
            g.drawCenteredString(font, name, cx, cy - 4, ARGB.color(255, HOLO));
        }
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/gui/elements/" + name + ".png");
    }

    private void choose() {
        if (selected >= 0 && selected < stats.size()) {
            ClientPacketDistributor.sendToServer(new AndroidPayloads.SelectStat(stats.get(selected).id()));
            Android.get(minecraft.player).setActiveStat(stats.get(selected).id());
        }
        onClose();
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (AndroidKeys.ABILITY_SWITCH.matches(event)) {
            choose();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        choose();
        return true;
    }
}
