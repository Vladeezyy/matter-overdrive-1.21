package matteroverdrive.client.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.dialog.DialogMessage;
import matteroverdrive.dialog.DialogNpc;
import matteroverdrive.dialog.DialogPayloads;
import matteroverdrive.dialog.DialogRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import matteroverdrive.compat.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 GuiDialog: letterbox bars, the NPC's line right-aligned left of a holo separator, the player's options on the
 * right (hover = white with a bar, options that can't be chosen in red), 20 ticks between choices.
 */
public class DialogScreen extends Screen {
    private static final int INTERACTION_DELAY = 20;
    private static final int HOLO = 0xFFA9E2FB, HOLO_RED = 0xFFE65014;
    private static final ResourceLocation SEPARATOR = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID,
            "textures/gui/elements/dialog_separator.png");
    private static final Random RANDOM = new Random();

    private final DialogNpc npc;
    private final Player player;
    private DialogMessage current;
    private long seed = RANDOM.nextLong();
    private float lastInteraction;

    public DialogScreen(DialogNpc npc, Player player) {
        super(Component.empty());
        this.npc = npc;
        this.player = player;
        this.current = npc.getStartDialogMessage(player);
    }

    public DialogMessage getCurrent() {
        return current;
    }

    public void setCurrent(DialogMessage message) {
        seed = RANDOM.nextLong();
        current = message;
    }

    public DialogNpc getNpc() {
        return npc;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (current == null || !npc.getEntity().isAlive()) {
            onClose();
            return;
        }
        int bar = height / 8;
        g.fill(0, 0, width, bar, 0xFF000000);
        g.fill(0, height - bar, width, height, 0xFF000000);
        g.fill(0, height - bar - 128, width, height - bar, 0x66000000);
        drawDialog(g, mouseX, mouseY);
        if (lastInteraction > 0) lastInteraction = Math.max(0, lastInteraction - partialTick);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private List<DialogMessage> visibleOptions() {
        List<DialogMessage> visible = new ArrayList<>();
        for (DialogMessage option : current.getOptions(npc, player)) {
            option.setSeed(seed);
            if (option.isVisible(npc, player)) visible.add(option);
        }
        return visible;
    }

    private int optionY(int index, int count) {
        return height - height / 8 - 60 + 18 * index - count * 18 / 2;
    }

    private void drawDialog(GuiGraphics g, int mouseX, int mouseY) {
        current.setSeed(seed);
        String message = current.getMessageText(npc, player);
        if (!message.isEmpty()) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (String part : message.split("<br>")) lines.addAll(font.split(Component.literal(part), width / 3));
            for (int i = 0; i < lines.size(); i++) {
                int y = height - height / 8 - 64 - lines.size() * font.lineHeight / 2 + font.lineHeight * i;
                g.drawString(font, lines.get(i), width / 2 - font.width(lines.get(i)) - 16, y, HOLO, false);
            }
        }
        matteroverdrive.compat.Gui.blit(g, SEPARATOR, width / 2 - 5, height - height / 8 - 128, 0, 0, 11, 128, 11, 128,
                ARGB.color(128, ARGB.scaleRGB(HOLO, 0.5f)));

        List<DialogMessage> options = visibleOptions();
        for (int i = 0; i < options.size(); i++) {
            DialogMessage option = options.get(i);
            int x = width / 2 + 26, y = optionY(i, options.size());
            String text = option.getQuestionText(npc, player);
            int w = font.width(text);
            boolean canInteract = option.canInteract(npc, player);
            String icon = option.getHoloIcon(npc, player);
            ResourceLocation iconTex = icon == null ? null
                    : ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/gui/holo/" + icon + ".png");
            if (mouseX > x && mouseX <= x + w && mouseY > y && mouseY <= y + font.lineHeight) {
                g.fill(x - 2, y - 4, x + w + 8, y + font.lineHeight + 4, 0x66000000);
                g.fill(x + w + 8, y - 4, x + w + 10, y + font.lineHeight + 4, canInteract ? 0x99FFFFFF : HOLO_RED);
                if (iconTex != null) matteroverdrive.compat.Gui.blit(g, iconTex, x - 18, y + font.lineHeight / 2 - 8, 0, 0, 16, 16, 16, 16);
                g.drawString(font, text, x + 2, y, canInteract ? 0xFFFFFFFF : HOLO_RED, false);
            } else {
                if (iconTex != null) {
                    matteroverdrive.compat.Gui.blit(g, iconTex, x - 18, y + font.lineHeight / 2 - 8, 0, 0, 16, 16, 16, 16, HOLO);
                }
                g.drawString(font, text, x, y, canInteract ? HOLO : HOLO_RED, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        boolean doubleClick = false;
        List<DialogMessage> all = current.getOptions(npc, player);
        List<DialogMessage> options = visibleOptions();
        for (int i = 0; i < options.size(); i++) {
            DialogMessage option = options.get(i);
            if (!option.canInteract(npc, player)) continue;
            int x = width / 2 + 26, y = optionY(i, options.size());
            int w = font.width(option.getQuestionText(npc, player));
            if (event.x() > x && event.x() <= x + w && event.y() > y && event.y() <= y + font.lineHeight) {
                choose(all.indexOf(option));
                return true;
            }
        }
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    /** 1.7.10 onQuestionClick: act on the client, tell the server which option of which message. */
    public void choose(int option) {
        if (lastInteraction > 0) return;
        lastInteraction = INTERACTION_DELAY;
        DialogMessage message = current;
        message.onOptionsInteract(npc, player, option);
        PacketDistributor.sendToServer(new DialogPayloads.Interact(npc.getEntity().getId(), DialogRegistry.getId(message), option));
    }

    @Override
    public void removed() {
        npc.setDialogPlayer(null);
        PacketDistributor.sendToServer(new DialogPayloads.Manage(npc.getEntity().getId(), false));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
