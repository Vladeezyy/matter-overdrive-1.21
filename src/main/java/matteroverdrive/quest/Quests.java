package matteroverdrive.quest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.init.MOItems;
import matteroverdrive.quest.logic.BecomeAndroidLogic;
import matteroverdrive.quest.logic.CocktailOfAscensionLogic;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 MatterOverdriveQuests: the quests by id (the contract quests come with the contract market). */
public final class Quests {
    private static final Map<String, Quest> QUESTS = new LinkedHashMap<>();

    public static final Quest COCKTAIL_OF_ASCENSION = register(new Quest("cocktail_of_ascension", new CocktailOfAscensionLogic(), 512)
            .rewards(() -> new ItemStack(MOItems.ANDROID_PILL_RED.get()), () -> new ItemStack(MOItems.ANDROID_PILL_BLUE.get()),
                    () -> new ItemStack(MOItems.ANDROID_PILL_YELLOW.get())));
    public static final Quest PUNY_HUMANS = register(new Quest("puny_humans", new BecomeAndroidLogic(), 256)
            // 1.7.10 ItemStackReward(androidPill, 1) is one pill of damage 0: the red one
            .rewards(() -> new ItemStack(MOItems.BATTERY.get()), () -> new ItemStack(MOItems.ANDROID_PILL_RED.get())));

    public static Quest register(Quest quest) {
        QUESTS.put(quest.id(), quest);
        return quest;
    }

    public static @Nullable Quest get(String id) {
        return QUESTS.get(id);
    }

    public static Map<String, Quest> all() {
        return QUESTS;
    }

    private Quests() {}
}
