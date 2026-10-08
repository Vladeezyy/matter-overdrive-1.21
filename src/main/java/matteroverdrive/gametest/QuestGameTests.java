package matteroverdrive.gametest;

import matteroverdrive.android.Android;
import matteroverdrive.dialog.DialogMessage;
import matteroverdrive.dialog.DialogPayloads;
import matteroverdrive.entity.MadScientist;
import matteroverdrive.init.MOEntities;
import matteroverdrive.init.MOItems;
import matteroverdrive.quest.PlayerQuests;
import matteroverdrive.quest.QuestEvents;
import matteroverdrive.quest.QuestStack;
import matteroverdrive.quest.Quests;
import matteroverdrive.quest.logic.CocktailOfAscensionLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Phase 7m checks: the mad scientist's trades and conversations, the Puny Humans and Cocktail of Ascension quests. */
final class QuestGameTests {
    static void addAll() {
        MOGameTests.add("mad_scientist_trades", 20, false, QuestGameTests::trades);
        MOGameTests.add("quest_puny_humans", 20, false, QuestGameTests::punyHumans);
        MOGameTests.add("quest_cocktail_of_ascension", 20, false, QuestGameTests::cocktail);
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        helper.assertTrue(ok, Component.literal(message));
    }

    private static MadScientist scientist(GameTestHelper helper, boolean junkie) {
        MadScientist npc = helper.spawnWithNoFreeWill(MOEntities.MAD_SCIENTIST.get(), new BlockPos(5, 1, 2));
        npc.setJunkie(junkie);
        return npc;
    }

    /** Chooses an option of the message on screen, as the server does for the client's interact packet. */
    private static void choose(MadScientist npc, ServerPlayer player, DialogMessage on, DialogMessage option) {
        int index = on.getOptions(npc, player).indexOf(option);
        if (index < 0) throw new IllegalStateException("option not offered");
        on.onOptionsInteract(npc, player, index);
    }

    /** 1.7.10 TradeHandlerMadScientist: the sure trades are always there (pills, dilithium, barrels, tea). */
    private static void trades(GameTestHelper helper) {
        MadScientist npc = scientist(helper, false);
        var offers = npc.getOffers();
        check(helper, offers.size() >= 8, "offers " + offers.size());
        check(helper, offers.stream().anyMatch(o -> o.getResult().is(MOItems.ANDROID_PILL_BLUE.get()) && o.getCostA().is(Items.EMERALD)),
                "no blue pill for emeralds");
        check(helper, offers.stream().anyMatch(o -> o.getCostA().is(MOItems.DILITHIUM_CRYSTAL.get()) && o.getResult().is(Items.EMERALD)),
                "doesn't buy dilithium");
        helper.succeed();
    }

    /** A human asks to become an android, brings the four parts, and the scientist converts them. */
    private static void punyHumans(GameTestHelper helper) {
        MadScientist npc = scientist(helper, false);
        ServerPlayer player = AndroidGameTests.player(helper);
        check(helper, DialogPayloads.startConversation(player, npc), "conversation didn't start");
        DialogMessage start = npc.getStartDialogMessage(player);
        check(helper, !MadScientist.Dialogs.convertMe.isVisible(npc, player), "convert offered without the quest");
        choose(npc, player, start, MadScientist.Dialogs.canYouConvert);
        QuestStack stack = PlayerQuests.get(player).findActive(Quests.PUNY_HUMANS);
        check(helper, stack != null && stack.isGiver(npc), "quest not given");
        check(helper, !MadScientist.Dialogs.convertMe.isVisible(npc, player), "convert offered without the parts");
        for (var part : new net.minecraft.world.item.Item[] {MOItems.ROGUE_ANDROID_HEAD.get(), MOItems.ROGUE_ANDROID_ARMS.get(),
                MOItems.ROGUE_ANDROID_LEGS.get(), MOItems.ROGUE_ANDROID_CHEST.get()}) {
            player.getInventory().add(new ItemStack(part));
        }
        check(helper, MadScientist.Dialogs.convertMe.isVisible(npc, player), "convert not offered with all parts");
        choose(npc, player, MadScientist.Dialogs.canYouConvert, MadScientist.Dialogs.convertMe);
        QuestEvents.manageQuestCompletion(player);
        PlayerQuests quests = PlayerQuests.get(player);
        check(helper, quests.getActiveQuests().isEmpty() && quests.hasCompletedQuest(new QuestStack(Quests.PUNY_HUMANS)), "quest not completed");
        check(helper, Android.get(player).isTurning(), "transformation didn't start");
        check(helper, !player.getInventory().contains(new ItemStack(MOItems.ROGUE_ANDROID_HEAD.get())), "parts not taken");
        check(helper, player.getInventory().contains(new ItemStack(MOItems.BATTERY.get()))
                && player.getInventory().contains(new ItemStack(MOItems.ANDROID_PILL_RED.get())), "no rewards");
        check(helper, !Quests.PUNY_HUMANS.canBeAccepted(new QuestStack(Quests.PUNY_HUMANS), player), "quest can be taken again");
        player.discard();
        helper.succeed();
    }

    /** The junkie's cocktail: creepers with a shovel, gunpowder, nether mushrooms, then he turns into a mutant. */
    private static void cocktail(GameTestHelper helper) {
        MadScientist npc = scientist(helper, true);
        ServerPlayer player = AndroidGameTests.player(helper);
        DialogPayloads.startConversation(player, npc);
        DialogMessage start = npc.getStartDialogMessage(player);
        check(helper, start.getOptions(npc, player).contains(MadScientist.Dialogs.cocktailOfAscension), "cocktail quest not offered");
        DialogMessage[] lines = MadScientist.Dialogs.cocktailQuest;
        choose(npc, player, lines[lines.length - 1], MadScientist.Dialogs.acceptCocktail);
        QuestStack stack = PlayerQuests.get(player).findActive(Quests.COCKTAIL_OF_ASCENSION);
        check(helper, stack != null, "quest not given");
        // creepers killed with a shovel count, others don't
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
        for (int i = 0; i < 6; i++) {
            var creeper = EntityType.CREEPER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            QuestEvents.onEvent(player, new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(creeper,
                    helper.getLevel().damageSources().playerAttack(player)));
        }
        check(helper, CocktailOfAscensionLogic.getCreeperKills(stack) == 5, "creeper kills " + CocktailOfAscensionLogic.getCreeperKills(stack));
        // gunpowder: taken from the picked-up stack (plus the 1.7.10 extra one)
        ItemEntity powder = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.GUNPOWDER, 8));
        QuestEvents.onEvent(player, new net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre(player, powder));
        check(helper, CocktailOfAscensionLogic.getGunpowder(stack) == 5 && powder.getItem().getCount() == 2,
                "gunpowder " + CocktailOfAscensionLogic.getGunpowder(stack) + ", left " + powder.getItem().getCount());
        // red mushrooms only count in the Nether
        ItemEntity mushrooms = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.RED_MUSHROOM, 5));
        QuestEvents.onEvent(player, new net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre(player, mushrooms));
        check(helper, CocktailOfAscensionLogic.getMushrooms(stack) == 0, "overworld mushrooms counted");
        check(helper, !MadScientist.Dialogs.cocktailComplete.isVisible(npc, player), "complete offered too early");
        stack.getData().putByte("MushroomCount", (byte) 5);
        check(helper, MadScientist.Dialogs.cocktailComplete.isVisible(npc, player), "complete not offered");
        npc.setDialogPlayer(player);
        choose(npc, player, npc.getStartDialogMessage(player), MadScientist.Dialogs.cocktailComplete);
        check(helper, npc.isRemoved(), "scientist still there");
        var mutants = helper.getLevel().getEntitiesOfClass(matteroverdrive.entity.monster.MutantScientist.class, npc.getBoundingBox().inflate(2));
        check(helper, mutants.size() == 1, "mutants " + mutants.size());
        QuestEvents.manageQuestCompletion(player);
        check(helper, PlayerQuests.get(player).hasCompletedQuest(new QuestStack(Quests.COCKTAIL_OF_ASCENSION)), "quest not completed");
        check(helper, player.getInventory().contains(new ItemStack(MOItems.ANDROID_PILL_YELLOW.get())), "no pill rewards");
        mutants.forEach(m -> m.discard());
        player.discard();
        helper.succeed();
    }
}
