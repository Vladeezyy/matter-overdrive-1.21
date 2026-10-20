package matteroverdrive.client.screen;

import java.util.function.Predicate;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** An edit box that rejects typed text its filter refuses (26.x: EditBox lost setFilter). */
public class FilteredEditBox extends EditBox {
    private Predicate<String> filter = s -> true;

    public FilteredEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    public void setFilter(Predicate<String> filter) {
        this.filter = filter;
    }

    @Override
    public void insertText(String input) {
        String before = getValue();
        int cursor = getCursorPosition();
        super.insertText(input);
        if (!filter.test(getValue())) {
            setValue(before);
            setCursorPosition(cursor);
            setHighlightPos(cursor);
        }
    }
}
