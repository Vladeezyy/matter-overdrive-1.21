package matteroverdrive.quest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import matteroverdrive.init.MOEntities;
import matteroverdrive.init.MOItems;
import matteroverdrive.quest.logic.CollectItemLogic;
import matteroverdrive.quest.logic.CraftLogic;
import matteroverdrive.quest.logic.KillCreatureLogic;
import matteroverdrive.quest.logic.MineLogic;
import matteroverdrive.quest.logic.ScanBlockLogic;
import matteroverdrive.quest.logic.SingleEventLogic;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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

    // --- contract quests (1.7.10 MatterOverdriveQuests.initMatterOverdriveQuests) ---------------------------
    public static final Quest KILL_ANDROIDS = register(new Quest.RandomText("kill_androids", new KillCreatureLogic(12, 28, 40,
            new KillCreatureLogic.Target(matteroverdrive.entity.monster.RogueAndroid.class, () -> MOEntities.ROGUE_ANDROID.get()))
            .autoComplete(true), 1, 0).rewards(() -> new ItemStack(MOItems.ROGUE_ANDROID_HEAD.get())));
    /** 1.7.10: chicken, cow, cow (cows twice as likely), young ones only. */
    public static final Quest SACRIFICE = register(new Quest("sacrifice", new KillCreatureLogic(8, 15, 10,
            new KillCreatureLogic.Target(Chicken.class, () -> EntityType.CHICKEN), new KillCreatureLogic.Target(Cow.class, () -> EntityType.COW),
            new KillCreatureLogic.Target(Cow.class, () -> EntityType.COW)).onlyChildren().autoComplete(true), 0)
            .rewards(() -> new ItemStack(Items.SADDLE), () -> new ItemStack(Items.NAME_TAG)));
    public static final Quest DEPARTMENT_OF_AGRICULTURE = register(new Quest("department_of_agriculture", new CollectItemLogic(
            List.of(() -> new ItemStack(Items.WHEAT), () -> new ItemStack(Items.CARROT), () -> new ItemStack(Items.POTATO)), 31, 63, 3), 0)
            .rewards(() -> new ItemStack(Items.EMERALD, 4), () -> new ItemStack(Items.DIAMOND_HOE)));
    public static final Quest WEAPONS_OF_WAR = register(new Quest("weapons_of_war", new CraftLogic(List.of(() -> new ItemStack(Items.ANVIL)),
            1, 3, 60).autoComplete(true), 0)
            .rewards(() -> new ItemStack(MOItems.TRITANIUM_SWORD.get()), () -> new ItemStack(MOItems.TRITANIUM_CHESTPLATE.get())));
    public static final Quest ONE_TRUE_LOVE = register(new Quest("one_true_love", new MineLogic(() -> Blocks.DIAMOND_ORE, 1, 1, 180,
            () -> Blocks.DEEPSLATE_DIAMOND_ORE).autoComplete(true), 0).rewards(() -> new ItemStack(Items.EMERALD, 6)));
    public static final Quest IS_IT_REALLY_ME = register(new Quest("is_it_really_me", new SingleEventLogic(QuestEvents.Transport.class)
            .autoComplete(true), 120).rewards(() -> new ItemStack(MOItems.UPGRADE_RANGE.get(), 2)));
    public static final Quest BEAST_BELLY = register(new Quest("beast_belly", new SingleEventLogic(QuestEvents.AnomalyConsume.class), 210)
            .rewards(() -> new ItemStack(MOItems.GRAVITATIONAL_STABILIZER.get(), 2)));
    /** 1.7.10 gmo: scan carrots then potatoes with the mad scientist's pad; a hardened spine (+5 health, no glitches). */
    public static final Quest GMO = register(new Quest.Multi("gmo", 0,
            new ScanBlockLogic(() -> Blocks.CARROTS, 12, 24, 10).onlyDestroyable(),
            new ScanBlockLogic(() -> Blocks.POTATOES, 12, 24, 10).onlyDestroyable()).sequential().autoComplete()
            .rewards(Quests::hardenedSpine));

    private static ItemStack hardenedSpine() {
        ItemStack spine = new ItemStack(MOItems.TRITANIUM_SPINE.get());
        spine.set(matteroverdrive.init.MODataComponents.BIONIC_STATS.get(), new matteroverdrive.item.android.BionicPartItem.Stats(5, -1));
        spine.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Hardened Tritanium Spine"));
        return spine;
    }

    /** 1.7.10 MatterOverdriveQuests.contractGeneration: quest and weight. */
    public record Weighted(Quest quest, int weight) {}

    public static final List<Weighted> CONTRACT_GENERATION = List.of(new Weighted(KILL_ANDROIDS, 100), new Weighted(SACRIFICE, 100),
            new Weighted(DEPARTMENT_OF_AGRICULTURE, 100), new Weighted(WEAPONS_OF_WAR, 80), new Weighted(ONE_TRUE_LOVE, 100),
            new Weighted(IS_IT_REALLY_ME, 80), new Weighted(BEAST_BELLY, 60));

    /** 1.7.10 WeightedRandom.getRandomItem over contractGeneration. */
    public static Quest randomContract(net.minecraft.util.RandomSource random) {
        int total = CONTRACT_GENERATION.stream().mapToInt(Weighted::weight).sum();
        int roll = random.nextInt(total);
        for (Weighted w : CONTRACT_GENERATION) {
            roll -= w.weight();
            if (roll < 0) return w.quest();
        }
        return CONTRACT_GENERATION.get(0).quest();
    }

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
