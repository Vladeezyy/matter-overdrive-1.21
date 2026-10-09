package matteroverdrive.client.quest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MOSounds;
import matteroverdrive.quest.PlayerQuests;
import matteroverdrive.quest.Quest;
import matteroverdrive.quest.QuestStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 1.7.10 GuiQuestHud: "Started:" a quest (lower left), "Completed:" with the XP counting up (upper right) and the
 * changed objectives (left middle), each faded in and out by its timeline, queued one after another.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class QuestHud {
    private static final float FADE_IN = 30, FADE_OUT = 60;
    private static final float OBJECTIVES_TIME = 200, OBJECTIVES_FADE = 20;
    private static final int HOLO = 0xA9E2FB;

    private static final Queue<QuestStack> completedQueue = new ArrayDeque<>();
    private static final Queue<QuestStack> startedQueue = new ArrayDeque<>();
    private static final Queue<QuestStack[]> objectivesQueue = new ArrayDeque<>();

    private static String completedName, startedName;
    private static int completedXp;
    private static String[] objectives;
    /** Timelines: time played and total length; -1 = not playing. */
    private static float completedTime = -1, completedLength, startedTime = -1, startedLength, objectivesTime = -1, objectivesLength;

    /** Compares two syncs of the player's quests (1.7.10 ADD / UPDATE / COMPLETE packets). */
    public static void onSync(Player player, PlayerQuests before, PlayerQuests after) {
        for (QuestStack stack : after.getActiveQuests()) {
            QuestStack old = before.getActiveQuests().stream().filter(q -> q.getQuestId().equals(stack.getQuestId())).findFirst().orElse(null);
            if (old == null) startedQueue.add(stack);
            else if (!old.getData().equals(stack.getData())) {
                objectivesQueue.add(new QuestStack[] {old, stack});
                if (objectivesTime >= 0 && objectivesTime < OBJECTIVES_TIME - OBJECTIVES_FADE) objectivesTime = OBJECTIVES_TIME - OBJECTIVES_FADE;
            }
        }
        if (after.getCompletedQuests().size() > before.getCompletedQuests().size()) {
            completedQueue.addAll(after.getCompletedQuests().subList(before.getCompletedQuests().size(), after.getCompletedQuests().size()));
        }
    }

    @SubscribeEvent
    static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CHAT, ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "quest_hud"), QuestHud::render);
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!completedQueue.isEmpty() && completedTime < 0 && startedTime < 0) {
            QuestStack stack = completedQueue.poll();
            Quest quest = stack.getQuest();
            mc.player.playSound(MOSounds.QUEST_COMPLETE.get(), 1, 1);
            completedName = quest == null ? stack.getQuestId() : quest.getTitle(stack, mc.player);
            completedXp = quest == null ? 0 : quest.getXpReward(stack, mc.player);
            // fade in, hold 15 ticks per character, fade out
            completedLength = FADE_IN + completedName.length() * 15 + FADE_OUT;
            completedTime = 0;
        }
        if (!startedQueue.isEmpty() && startedTime < 0 && completedTime < 0) {
            QuestStack stack = startedQueue.poll();
            Quest quest = stack.getQuest();
            mc.player.playSound(MOSounds.QUEST_STARTED.get(), 1, 1);
            startedName = quest == null ? stack.getQuestId() : quest.getTitle(stack, mc.player);
            startedLength = FADE_IN + 20 * 5 + startedName.length() * 5 + OBJECTIVES_FADE;
            startedTime = 0;
        }
        if (!objectivesQueue.isEmpty() && objectivesTime < 0) {
            QuestStack[] change = objectivesQueue.poll();
            Quest quest = change[1].getQuest();
            if (quest != null) {
                int count = quest.getObjectivesCount(change[1], mc.player);
                objectives = new String[count];
                int show = 0;
                for (int i = 0; i < count; i++) {
                    String now = quest.getObjective(change[1], mc.player, i);
                    if (!now.equals(quest.getObjective(change[0], mc.player, i))) {
                        objectives[i] = now;
                        show = Math.max(show, now.length() * 4);
                    }
                }
                objectivesLength = OBJECTIVES_FADE + show + OBJECTIVES_FADE;
                objectivesTime = 0;
            }
        }
    }

    /** Fade in over {@code in}, hold, fade out over {@code out} (quad easing like MOAnimationTimeline). */
    private static float value(float time, float length, float in, float out) {
        if (time < in) {
            float t = time / in;
            return t * t;
        }
        if (time > length - out) {
            float t = (length - time) / out;
            return Math.max(0, 1 - (1 - t) * (1 - t));
        }
        return 1;
    }

    private static int color(float v) {
        return ARGB.color(20 + (int) (235 * v), HOLO);
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        Font font = mc.font;
        float dt = delta.getGameTimeDeltaTicks();
        int sw = g.guiWidth(), sh = g.guiHeight();
        if (completedTime >= 0) {
            float v = value(completedTime, completedLength, FADE_IN, FADE_OUT);
            int y = (int) (sh * 0.15);
            Component title = Component.literal(completedName).withStyle(net.minecraft.ChatFormatting.BOLD);
            int titleWidth = (int) (font.width(title) * 1.5);
            g.pose().pushPose();
            g.pose().translate(sw - titleWidth - 30 - v * 30, y - 20, 0);
            g.pose().scale(1.5f, 1.5f, 1);
            g.drawString(font, title, 0, 40, color(v), true);
            g.pose().popPose();
            g.drawString(font, Component.translatable("gui." + MatterOverdrive.MODID + ".quest.completed"), sw - titleWidth - 20 - (int) (v * 40),
                    y + 28, color(v), true);
            if (completedXp > 0) g.drawString(font, "+" + (int) (v * completedXp) + "xp", sw - 50 - (int) (20 * v), y + 58, color(v), true);
            completedTime += dt;
            if (completedTime > completedLength) completedTime = -1;
        }
        if (startedTime >= 0) {
            float v = value(startedTime, startedLength, FADE_IN, OBJECTIVES_FADE);
            int y = (int) (sh * 0.65);
            g.pose().pushPose();
            g.pose().translate(-10 + v * 30, y, 0);
            g.pose().scale(1.5f, 1.5f, 1);
            g.drawString(font, Component.literal(startedName).withStyle(net.minecraft.ChatFormatting.BOLD), 0, 0, color(v), true);
            g.pose().popPose();
            g.drawString(font, Component.translatable("gui." + MatterOverdrive.MODID + ".quest.started"), (int) (v * 20), y - 12, color(v), true);
            startedTime += dt;
            if (startedTime > startedLength) startedTime = -1;
        }
        if (objectivesTime >= 0 && objectives != null) {
            float v = value(objectivesTime, objectivesLength, OBJECTIVES_FADE, OBJECTIVES_FADE);
            int oy = 0;
            List<String> shown = new ArrayList<>();
            for (String o : objectives) if (o != null) shown.add(o);
            for (String o : shown) {
                g.drawString(font, "[ " + o + " ]", (int) (v * 20), (int) (sh * 0.5) + oy, color(v), true);
                oy += font.lineHeight + 2;
            }
            objectivesTime += dt;
            if (objectivesTime > objectivesLength) objectivesTime = -1;
        }
    }

    private QuestHud() {}
}
