package matteroverdrive.dialog;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/** 1.7.10 DialogRegistry: messages get ids in registration order (the same on both sides), sent in interact packets. */
public final class DialogRegistry {
    private static final List<DialogMessage> MESSAGES = new ArrayList<>();

    public static <T extends DialogMessage> T register(T message) {
        MESSAGES.add(message);
        return message;
    }

    public static int getId(@Nullable DialogMessage message) {
        return message == null ? -1 : MESSAGES.indexOf(message);
    }

    public static @Nullable DialogMessage get(int id) {
        return id >= 0 && id < MESSAGES.size() ? MESSAGES.get(id) : null;
    }

    /**
     * 1.7.10 DialogFactory.constructMultipleLineDialog: a chain of lines {@code <key>.<i>.line}, each the only option of
     * the previous one, asked with {@code <key>.<i>.question} when it exists or else the given literal.
     */
    public static DialogMessage[] multipleLines(DialogMessage first, String key, int lines, String nextLineQuestion) {
        DialogMessage[] messages = new DialogMessage[lines];
        messages[0] = register(first);
        first.message(key + ".0.line").question(key + ".question");
        DialogMessage last = first;
        for (int i = 1; i < lines; i++) {
            DialogMessage child = register(new DialogMessage()).message(key + "." + i + ".line");
            child.question(key + "." + i + ".question").questionLiteral(nextLineQuestion);
            child.setParent(last);
            last.addOption(child);
            last = child;
            messages[i] = child;
        }
        return messages;
    }

    private DialogRegistry() {}
}
