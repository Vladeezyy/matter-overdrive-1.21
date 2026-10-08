package matteroverdrive.client.quest;

import java.util.ArrayList;
import java.util.List;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.item.DataPadItem;
import matteroverdrive.quest.PlayerQuests;
import matteroverdrive.quest.Quest;
import matteroverdrive.quest.QuestPayloads;
import matteroverdrive.quest.QuestStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 1.7.10 GuiDataPad (300 wide; 240 high here, 1.7.10's 260 doesn't fit a 240-high scaled screen) with the Active Quests
 * page (1.7.10 PageActiveQuests): the quest list, the selected quest's info, objectives (filled square = done) and
 * rewards, and the Current Quests / Complete / Abandon buttons. The guide pages come with the guide.
 */
public class DataPadScreen extends Screen {
    public static final int WIDTH = 300, HEIGHT = 240;
    private static final int HOLO = 0xFFA9E2FB, HOLO_DIM = ARGB.color(255, ARGB.scaleRGB(0xA9E2FB, 0.5f)), HOLO_GREEN = 0xFF18CF00,
            HOLO_RED = 0xFFE65014;
    private static final ResourceLocation BACKGROUND = id("data_pad");
    private static final ResourceLocation BUTTON = id("button_normal"), BUTTON_OVER = id("button_over"), BUTTON_DARK = id("button_over_dark");
    private static final ResourceLocation CLOSE = tex("textures/gui/elements/close_button.png");
    private static final ResourceLocation BAND = tex("textures/gui/elements/android_bg_element.png");
    private static final int LIST_X = 22, LIST_Y = 28, LIST_W = WIDTH - 28 - 44, ROW = 12, ROWS = 6;
    private static final int INFO_X = 22, INFO_Y = 116, INFO_W = WIDTH - 28 - 15, INFO_H = 80;
    private static final int BUTTONS_Y = HEIGHT - 28;
    private static final int[] QUESTS_BT = {WIDTH - 96, BUTTONS_Y, 22, 22}, COMPLETE_BT = {WIDTH - 72, BUTTONS_Y, 22, 22},
            ABANDON_BT = {WIDTH - 48, BUTTONS_Y + 4, 16, 16};

    private final InteractionHand hand;
    private DataPadItem.State state;
    private int left, top;
    private int listScroll;

    public DataPadScreen(InteractionHand hand) {
        super(Component.translatable("item.matteroverdrive.data_pad"));
        this.hand = hand;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    private static ResourceLocation holo(String name) {
        return tex("textures/gui/holo/" + name + ".png");
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        state = DataPadItem.getState(pad());
    }

    private ItemStack pad() {
        return minecraft.player.getItemInHand(hand);
    }

    private List<QuestStack> quests() {
        return PlayerQuests.get(minecraft.player).getActiveQuests();
    }

    private QuestStack selected() {
        List<QuestStack> quests = quests();
        return quests.isEmpty() ? null : quests.get(Math.clamp(state.selectedQuest(), 0, quests.size() - 1));
    }

    private void setState(DataPadItem.State next) {
        state = next;
        ClientPacketDistributor.sendToServer(new QuestPayloads.DataPadState(hand == InteractionHand.OFF_HAND, next));
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private boolean canComplete(QuestStack stack) {
        if (stack == null) return false;
        for (int i = 0; i < stack.getObjectivesCount(minecraft.player); i++) {
            if (!stack.isObjectiveCompleted(minecraft.player, i)) return false;
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (!(pad().getItem() instanceof DataPadItem)) {
            onClose();
            return;
        }
        int mx = mouseX - left, my = mouseY - top;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, WIDTH, HEIGHT);
        boolean overClose = in(mx, my, WIDTH - 32, 20, 9, 9);
        g.blit(RenderPipelines.GUI_TEXTURED, CLOSE, left + WIDTH - 32, top + 20, overClose ? 9 : 0, 0, 9, 9, 18, 9);
        renderQuests(g, mx, my, mouseX, mouseY);
        // buttons
        QuestStack selected = selected();
        button(g, mx, my, QUESTS_BT, "question_mark", 20, false, HOLO, mouseX, mouseY, "gui.matteroverdrive.quest.active_quests");
        button(g, mx, my, COMPLETE_BT, "tick", 16, canComplete(selected), HOLO_GREEN, mouseX, mouseY, "gui.matteroverdrive.quest.complete");
        button(g, mx, my, ABANDON_BT, "mini_quit", 16, selected != null, HOLO_RED, mouseX, mouseY, "gui.matteroverdrive.quest.abandon");
    }

    private void button(GuiGraphics g, int mx, int my, int[] b, String icon, int iconSize, boolean enabled, int color, int mouseX, int mouseY,
                        String tooltip) {
        boolean over = enabled && in(mx, my, b[0], b[1], b[2], b[3]);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, !enabled ? BUTTON_DARK : over ? BUTTON_OVER : BUTTON, left + b[0], top + b[1], b[2], b[3]);
        int size = Math.min(iconSize, b[2] - 4);
        g.blit(RenderPipelines.GUI_TEXTURED, holo(icon), left + b[0] + (b[2] - size) / 2, top + b[1] + (b[3] - size) / 2, 0, 0, size, size,
                iconSize, iconSize, iconSize, iconSize, enabled ? color : HOLO_DIM);
        if (in(mx, my, b[0], b[1], b[2], b[3])) g.setTooltipForNextFrame(font, Component.translatable(tooltip), mouseX, mouseY);
    }

    private void renderQuests(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        List<QuestStack> quests = quests();
        QuestStack selected = selected();
        // the list (1.7.10 MOElementListBox of ListElementQuest: centred titles, "‣ " on the selected one)
        for (int i = 0; i < ROWS && listScroll + i < quests.size(); i++) {
            QuestStack stack = quests.get(listScroll + i);
            Quest quest = stack.getQuest();
            String title = quest == null ? stack.getQuestId() : quest.getTitle(stack, minecraft.player);
            boolean isSelected = stack == selected;
            int ty = top + LIST_Y + i * ROW;
            int tw = font.width(title);
            if (isSelected) g.drawString(font, "‣ " + title, left + LIST_X + LIST_W / 2 - tw / 2 - 8, ty, HOLO, false);
            else g.drawString(font, title, left + LIST_X + LIST_W / 2 - tw / 2, ty, HOLO_DIM, false);
        }
        if (quests.isEmpty()) {
            Component none = Component.translatable("gui.matteroverdrive.quest.none");
            g.drawString(font, none, left + LIST_X + LIST_W / 2 - font.width(none) / 2, top + LIST_Y, HOLO_DIM, false);
        }
        // the band between list and info (1.7.10 android_bg_element at 20% holo)
        g.blit(RenderPipelines.GUI_TEXTURED, BAND, left + 60, top + 102, 0, 0, 174, 11, 174, 11, ARGB.color(51, 0xA9E2FB));
        if (selected == null || selected.getQuest() == null) return;
        // info, objectives, rewards
        Quest quest = selected.getQuest();
        List<FormattedCharSequence> lines = new ArrayList<>();
        String info = quest.getInfo(selected, minecraft.player).replace("/n/", "\n");
        for (String part : info.split("\n")) lines.addAll(font.split(Component.literal(part), INFO_W));
        lines.add(FormattedCharSequence.EMPTY);
        for (int i = 0; i < selected.getObjectivesCount(minecraft.player); i++) {
            boolean done = selected.isObjectiveCompleted(minecraft.player, i);
            // 1.7.10 QuestFactory.getFormattedQuestObjective: filled / empty square, green / dark green
            Component objective = Component.literal((done ? "■ " : "□ ") + quest.getObjective(selected, minecraft.player, i))
                    .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.DARK_GREEN);
            lines.addAll(font.split(objective, INFO_W));
        }
        lines.add(FormattedCharSequence.EMPTY);
        lines.add(Component.translatable("gui.matteroverdrive.quest.rewards", quest.getXpReward(selected, minecraft.player))
                .withStyle(ChatFormatting.GOLD).getVisualOrderText());
        List<ItemStack> rewards = quest.getRewards(selected, minecraft.player);
        int contentHeight = lines.size() * font.lineHeight + (rewards.isEmpty() ? 0 : 20);
        int maxScroll = Math.max(0, contentHeight - INFO_H);
        int scroll = Math.clamp(state.scroll(), 0, maxScroll);
        g.enableScissor(left + INFO_X, top + INFO_Y, left + INFO_X + INFO_W, top + INFO_Y + INFO_H);
        int y = top + INFO_Y - scroll;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, left + INFO_X, y, HOLO, false);
            y += font.lineHeight;
        }
        for (int i = 0; i < rewards.size(); i++) {
            int rx = left + INFO_X + 8 + i * 20, ry = y + 2;
            g.renderItem(rewards.get(i), rx, ry);
            g.renderItemDecorations(font, rewards.get(i), rx, ry);
            if (mouseX >= rx && mouseX < rx + 16 && mouseY >= ry && mouseY < ry + 16 && mouseY >= top + INFO_Y && mouseY < top + INFO_Y + INFO_H) {
                g.setTooltipForNextFrame(font, rewards.get(i), mouseX, mouseY);
            }
        }
        g.disableScissor();
        // 1.7.10 ElementScrollGroup scroller in holo colour
        if (maxScroll > 0) {
            int bar = Math.max(8, INFO_H * INFO_H / contentHeight);
            int by = top + INFO_Y + (INFO_H - bar) * scroll / maxScroll;
            g.fill(left + INFO_X + INFO_W + 4, by, left + INFO_X + INFO_W + 6, by + bar, HOLO);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x() - left, my = event.y() - top;
        if (in(mx, my, WIDTH - 32, 20, 9, 9)) {
            onClose();
            return true;
        }
        List<QuestStack> quests = quests();
        if (in(mx, my, LIST_X, LIST_Y, LIST_W, ROWS * ROW)) {
            int index = listScroll + (int) ((my - LIST_Y) / ROW);
            if (index < quests.size()) setState(new DataPadItem.State(state.page(), index, 0));
            return true;
        }
        QuestStack selected = selected();
        if (in(mx, my, COMPLETE_BT[0], COMPLETE_BT[1], COMPLETE_BT[2], COMPLETE_BT[3]) && canComplete(selected)) {
            ClientPacketDistributor.sendToServer(new QuestPayloads.QuestAction(QuestPayloads.Action.COMPLETE, quests.indexOf(selected)));
            return true;
        }
        if (in(mx, my, ABANDON_BT[0], ABANDON_BT[1], ABANDON_BT[2], ABANDON_BT[3]) && selected != null) {
            ClientPacketDistributor.sendToServer(new QuestPayloads.QuestAction(QuestPayloads.Action.ABANDON, quests.indexOf(selected)));
            setState(new DataPadItem.State(state.page(), 0, 0));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double mx = mouseX - left, my = mouseY - top;
        if (in(mx, my, INFO_X, INFO_Y, INFO_W + 8, INFO_H)) {
            state = new DataPadItem.State(state.page(), state.selectedQuest(), Math.max(0, state.scroll() - (int) Math.signum(scrollY) * 9));
            return true;
        }
        if (in(mx, my, LIST_X, LIST_Y, LIST_W, ROWS * ROW)) {
            listScroll = Math.clamp(listScroll - (int) Math.signum(scrollY), 0, Math.max(0, quests().size() - ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** 1.7.10 PageActiveQuests.onGuiClose: keeps the info scroll on the pad. */
    @Override
    public void removed() {
        setState(state);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
