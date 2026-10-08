package matteroverdrive.client.guide;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.MatterOverdrive;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 MOGuideEntry / MOGuideEntryBlock / MOGuideEntryItem: a Data Pad guide entry with its icon stacks, its file
 * ({@code guide/<lang>/<file>.xml}), group and hand-placed position. Block/item entries take the stack's name.
 */
public final class GuideEntry {
    private final String name;
    private final String file;
    private final boolean namedByStack;
    private final Supplier<List<ItemStack>> icons;
    private String group;
    private int guiX, guiY;

    GuideEntry(String name, String file, boolean namedByStack, Supplier<List<ItemStack>> icons) {
        this.name = name;
        this.file = file;
        this.namedByStack = namedByStack;
        this.icons = icons;
    }

    public String name() {
        return name;
    }

    public String file() {
        return file;
    }

    public @Nullable String group() {
        return group;
    }

    GuideEntry place(String group, int guiX, int guiY) {
        this.group = group;
        this.guiX = guiX;
        this.guiY = guiY;
        return this;
    }

    public int guiX() {
        return guiX;
    }

    public int guiY() {
        return guiY;
    }

    public List<ItemStack> icons() {
        return icons.get();
    }

    public ItemStack icon() {
        List<ItemStack> list = icons();
        return list.isEmpty() ? ItemStack.EMPTY : list.get(0);
    }

    public String getDisplayName() {
        if (namedByStack && !icon().isEmpty()) return icon().getHoverName().getString();
        return I18n.get("guide." + MatterOverdrive.MODID + ".entry." + name);
    }
}
