package matteroverdrive.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.quest.QuestEvents;
import net.minecraft.locale.Language;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * 1.7.10 DialogMessage / DialogMessageRandom: a line the NPC says ({@link #getMessageText}) and how it appears as an
 * option of its parent ({@link #getQuestionText}). Texts are translation keys resolved on the client; a "random"
 * message splits its translation by ';' and picks one variation by the screen's seed, like 1.7.10.
 */
public class DialogMessage {
    /** Client: show this message in the open dialog screen. */
    public static Consumer<DialogMessage> showOnClient = m -> {};
    /** Client: the message the open dialog screen shows. */
    public static java.util.function.Supplier<DialogMessage> shownOnClient = () -> null;

    protected @Nullable String messageKey;
    protected @Nullable String questionKey;
    /** Literal question text (1.7.10 constructMultipleLineDialog's ". . . . . ."). */
    protected @Nullable String questionLiteral;
    protected boolean random;
    protected @Nullable DialogMessage parent;
    protected final List<DialogMessage> options = new ArrayList<>();
    protected @Nullable String holoIcon;
    protected long seed;

    public DialogMessage message(String key) {
        messageKey = key(key);
        return this;
    }

    public DialogMessage question(String key) {
        questionKey = key(key);
        return this;
    }

    public DialogMessage questionLiteral(String text) {
        questionLiteral = text;
        return this;
    }

    /** 1.7.10 DialogMessageRandom: variations separated by ';'. */
    public DialogMessage random() {
        random = true;
        return this;
    }

    public DialogMessage holoIcon(String icon) {
        holoIcon = icon;
        return this;
    }

    public DialogMessage addOption(DialogMessage option) {
        options.add(option);
        return this;
    }

    public void setParent(DialogMessage parent) {
        this.parent = parent;
    }

    public @Nullable DialogMessage getParent() {
        return parent;
    }

    /** 1.7.10 dialog keys ("dialog.mad_scientist...") live under the mod namespace. */
    static String key(String key) {
        int dot = key.indexOf('.');
        return dot < 0 ? key : key.substring(0, dot) + "." + MatterOverdrive.MODID + key.substring(dot);
    }

    public List<DialogMessage> getOptions(DialogNpc npc, Player player) {
        return options;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    private String pick(@Nullable String key, DialogNpc npc, Player player) {
        if (key == null) return "";
        String text = Language.getInstance().getOrDefault(key);
        if (random) {
            String[] variations = text.split(";");
            text = variations[RandomSource.create(seed).nextInt(variations.length)];
        }
        return text.replace("%1$s", player.getName().getString()).replace("%2$s", npc.getEntity().getName().getString());
    }

    public String getMessageText(DialogNpc npc, Player player) {
        return pick(messageKey, npc, player);
    }

    public String getQuestionText(DialogNpc npc, Player player) {
        // 1.7.10 constructMultipleLineDialog used the line's own question only when it had a translation
        if (questionLiteral != null && (questionKey == null || !Language.getInstance().has(questionKey))) return questionLiteral;
        return pick(questionKey, npc, player);
    }

    public @Nullable String getHoloIcon(DialogNpc npc, Player player) {
        return holoIcon;
    }

    /** Called on the parent when one of its options is chosen (both sides). */
    public void onOptionsInteract(DialogNpc npc, Player player, int option) {
        List<DialogMessage> list = getOptions(npc, player);
        if (option >= 0 && option < list.size()) list.get(option).onInteract(npc, player);
    }

    /** The message becomes active: the screen shows it (client), the NPC and the quests hear of it (server). */
    public void onInteract(DialogNpc npc, Player player) {
        if (player.level().isClientSide()) {
            showOnClient.accept(nextShown(npc, player));
        } else {
            npc.onPlayerInteract(player, this);
            QuestEvents.onEvent(player, new QuestEvents.DialogInteract(npc.getEntity(), this));
        }
    }

    /** The message the screen shows after this one is chosen (itself, or the start message for "back" options). */
    protected DialogMessage nextShown(DialogNpc npc, Player player) {
        return this;
    }

    public boolean canInteract(DialogNpc npc, Player player) {
        return true;
    }

    public boolean isVisible(DialogNpc npc, Player player) {
        return true;
    }
}
