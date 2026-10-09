package matteroverdrive.client.quest;

import java.util.ArrayList;
import java.util.List;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.item.ContractItem;
import matteroverdrive.quest.Quest;
import matteroverdrive.quest.QuestPayloads;
import matteroverdrive.quest.QuestStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import matteroverdrive.compat.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 GuiQuestPreview: the contract sheet (contract.png, 200 x 225) with the quest title in blue, its info,
 * objectives and rewards in a scroll area, and "[ Accept ]" (red when it can't be taken).
 */
public class ContractScreen extends Screen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/gui/contract.png");
    private static final int WIDTH = 200, HEIGHT = 225, TEXT = 0xFF505758, TITLE = 0xFF2394E3, ACCEPT = 0xFF279F33, RED = 0xFFE65014;
    private static final int INFO_X = 18, INFO_Y = 68, INFO_W = WIDTH - 18 - 14, INFO_H = 120;
    private final InteractionHand hand;
    private int left, top, scroll;

    public ContractScreen(InteractionHand hand) {
        super(Component.translatable("item.matteroverdrive.contract"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    private QuestStack quest() {
        return ContractItem.getQuest(minecraft.player.getItemInHand(hand));
    }

    private boolean canAccept(QuestStack stack) {
        return stack.getQuest().canBeAccepted(stack, minecraft.player);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        QuestStack stack = quest();
        if (stack == null) {
            onClose();
            return;
        }
        Quest quest = stack.getQuest();
        matteroverdrive.compat.Gui.blit(g, BACKGROUND, left, top, 0, 0, WIDTH, HEIGHT, 200, 229);
        // title: bold, scaled to fit 100 px (at most 1.8x)
        Component title = Component.literal(quest.getTitle(stack, minecraft.player)).withStyle(ChatFormatting.BOLD);
        float scale = Math.min(100f / Math.max(1, font.width(title)), 1.8f);
        g.pose().pushPose();
        g.pose().translate(left + 24, top + 30, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, title, 0, 0, TITLE, false);
        g.pose().popPose();
        // info, objectives, rewards
        int w = 165;
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String part : quest.getInfo(stack, minecraft.player).replace("/n/", "\n").split("\n")) {
            lines.addAll(font.split(Component.literal(part), w));
        }
        lines.add(FormattedCharSequence.EMPTY);
        for (int i = 0; i < stack.getObjectivesCount(minecraft.player); i++) {
            boolean done = stack.isObjectiveCompleted(minecraft.player, i);
            lines.addAll(font.split(Component.literal((done ? "■ " : "□ ") + quest.getObjective(stack, minecraft.player, i))
                    .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.DARK_GREEN), w));
        }
        lines.add(FormattedCharSequence.EMPTY);
        lines.add(Component.translatable("gui.matteroverdrive.contract.rewards").withStyle(ChatFormatting.DARK_PURPLE).getVisualOrderText());
        lines.add(Component.literal("   +" + quest.getXpReward(stack, minecraft.player) + "xp").withStyle(ChatFormatting.DARK_PURPLE).getVisualOrderText());
        List<ItemStack> rewards = quest.getRewards(stack, minecraft.player);
        int content = lines.size() * font.lineHeight + (rewards.isEmpty() ? 0 : 20);
        int maxScroll = Math.max(0, content - INFO_H);
        scroll = Math.clamp(scroll, 0, maxScroll);
        g.enableScissor(left + INFO_X, top + INFO_Y, left + INFO_X + INFO_W, top + INFO_Y + INFO_H);
        int y = top + INFO_Y - scroll;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, left + INFO_X, y, TEXT, false);
            y += font.lineHeight;
        }
        for (int i = 0; i < rewards.size(); i++) {
            int rx = left + INFO_X + 8 + i * 20, ry = y + 1;
            g.renderItem(rewards.get(i), rx, ry);
            g.renderItemDecorations(font, rewards.get(i), rx, ry);
            if (in(mouseX, mouseY, rx, ry, 16, 16) && in(mouseX, mouseY, left + INFO_X, top + INFO_Y, INFO_W, INFO_H)) {
                matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, rewards.get(i), mouseX, mouseY);
            }
        }
        g.disableScissor();
        if (maxScroll > 0) {
            int bar = Math.max(8, INFO_H * INFO_H / content);
            int by = top + INFO_Y + (INFO_H - bar) * scroll / maxScroll;
            g.fill(left + INFO_X + INFO_W + 2, by, left + INFO_X + INFO_W + 4, by + bar, TEXT);
        }
        boolean accept = canAccept(stack);
        Component label = Component.literal("[ ").append(Component.translatable("gui.matteroverdrive.contract.accept")).append(" ]");
        int bx = left + 14, by = top + HEIGHT - 28;
        boolean over = accept && in(mouseX, mouseY, bx, by, 68, 12);
        g.drawString(font, over ? label.copy().withStyle(ChatFormatting.UNDERLINE) : label, bx + 34 - font.width(label) / 2, by + 2,
                accept ? ACCEPT : RED, false);
        if (in(mouseX, mouseY, bx, by, 68, 12)) {
            matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, Component.translatable("gui.matteroverdrive.contract.accept_tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        boolean doubleClick = false;
        QuestStack stack = quest();
        if (stack != null && canAccept(stack) && in(event.x(), event.y(), left + 14, top + HEIGHT - 28, 68, 12)) {
            int slot = hand == InteractionHand.OFF_HAND ? 40 : minecraft.player.getInventory().selected;
            PacketDistributor.sendToServer(new QuestPayloads.QuestAction(QuestPayloads.Action.ADD, slot));
            onClose();
            return true;
        }
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY) * 9;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
