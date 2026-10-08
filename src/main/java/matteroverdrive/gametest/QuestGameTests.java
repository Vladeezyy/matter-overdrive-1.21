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
        MOGameTests.add("mad_scientist_house", 20, false, QuestGameTests::house);
        MOGameTests.add("data_pad", 20, false, QuestGameTests::dataPad);
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

    /** The house is in the plains and desert village house pools, and its template builds the 1.7.10 house with him. */
    private static void house(GameTestHelper helper) {
        var level = helper.getLevel();
        var pools = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL);
        for (String pool : new String[] {"village/plains/houses", "village/desert/houses"}) {
            var templates = pools.getValue(net.minecraft.resources.ResourceLocation.withDefaultNamespace(pool)).templates;
            long ours = templates.stream().filter(e -> e.toString().contains("mad_scientist_house")).count();
            check(helper, ours == matteroverdrive.world.VillageHouses.WEIGHT, pool + ": " + ours);
        }
        var template = level.getStructureManager().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                matteroverdrive.MatterOverdrive.MODID, "village/mad_scientist_house")).orElseThrow();
        check(helper, template.getSize().equals(new net.minecraft.core.Vec3i(9, 9, 7)), "size " + template.getSize());
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
                .addProcessor(matteroverdrive.world.RandomCrateProcessor.INSTANCE).setFinalizeEntities(true);
        template.placeInWorld(level, origin, origin, settings, level.random, 2);
        check(helper, level.getBlockState(origin.offset(7, 1, 2)).is(matteroverdrive.init.MOBlocks.INSCRIBER.get()), "no inscriber");
        check(helper, level.getBlockState(origin.offset(1, 3, 5)).is(net.minecraft.world.level.block.Blocks.BOOKSHELF), "no bookshelves");
        check(helper, level.getBlockState(origin.offset(1, 1, 5)).getBlock() instanceof matteroverdrive.block.TritaniumCrateBlock, "no crate");
        check(helper, level.getBlockState(origin.offset(1, 1, 1)).is(net.minecraft.world.level.block.Blocks.OAK_DOOR), "no door");
        var scientists = level.getEntitiesOfClass(MadScientist.class, new net.minecraft.world.phys.AABB(origin).inflate(10));
        check(helper, scientists.size() == 1 && !scientists.get(0).removeWhenFarAway(1000), "scientists " + scientists.size());
        scientists.forEach(MadScientist::discard);
        helper.succeed();
    }

    /** The first quest brings a Data Pad; its Complete only works with every objective done, Abandon drops the quest. */
    private static void dataPad(GameTestHelper helper) {
        ServerPlayer player = AndroidGameTests.player(helper);
        QuestEvents.addQuest(player, new QuestStack(Quests.COCKTAIL_OF_ASCENSION));
        check(helper, player.getInventory().contains(new ItemStack(MOItems.DATA_PAD.get())), "no data pad with the first quest");
        QuestEvents.addQuest(player, new QuestStack(Quests.PUNY_HUMANS));
        check(helper, player.getInventory().countItem(MOItems.DATA_PAD.get()) == 1, "second data pad");
        matteroverdrive.quest.QuestPayloads.apply(player, matteroverdrive.quest.QuestPayloads.Action.COMPLETE, 0);
        check(helper, PlayerQuests.get(player).getActiveQuests().size() == 2, "completed with objectives left");
        QuestStack cocktail = PlayerQuests.get(player).findActive(Quests.COCKTAIL_OF_ASCENSION);
        cocktail.getData().putByte("CreeperKills", (byte) 5);
        cocktail.getData().putByte("GunpowderCount", (byte) 5);
        cocktail.getData().putByte("MushroomCount", (byte) 5);
        matteroverdrive.quest.QuestPayloads.apply(player, matteroverdrive.quest.QuestPayloads.Action.COMPLETE, 0);
        check(helper, PlayerQuests.get(player).hasCompletedQuest(new QuestStack(Quests.COCKTAIL_OF_ASCENSION)), "not completed");
        matteroverdrive.quest.QuestPayloads.apply(player, matteroverdrive.quest.QuestPayloads.Action.ABANDON, 0);
        check(helper, PlayerQuests.get(player).getActiveQuests().isEmpty(), "not abandoned");
        // a whitelisted pad only scans its blocks
        ItemStack pad = new ItemStack(MOItems.DATA_PAD.get());
        pad.set(matteroverdrive.init.MODataComponents.DATA_PAD_SCAN.get(),
                new matteroverdrive.item.DataPadItem.Scan(java.util.List.of(net.minecraft.world.level.block.Blocks.CARROTS), true, true));
        check(helper, matteroverdrive.item.DataPadItem.canScan(pad, net.minecraft.world.level.block.Blocks.CARROTS.defaultBlockState())
                && !matteroverdrive.item.DataPadItem.canScan(pad, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                && !matteroverdrive.item.DataPadItem.hasGui(pad), "scan whitelist");
        player.discard();
        helper.succeed();
    }
}
