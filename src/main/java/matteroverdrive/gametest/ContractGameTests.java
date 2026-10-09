package matteroverdrive.gametest;

import matteroverdrive.block.entity.ContractMarketBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOEntities;
import matteroverdrive.init.MOItems;
import matteroverdrive.item.ContractItem;
import matteroverdrive.item.DataPadItem;
import matteroverdrive.quest.PlayerQuests;
import matteroverdrive.quest.QuestEvents;
import matteroverdrive.quest.QuestPayloads;
import matteroverdrive.quest.QuestStack;
import matteroverdrive.quest.Quests;
import matteroverdrive.quest.logic.CollectItemLogic;
import matteroverdrive.quest.logic.KillCreatureLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Phase 7q checks: the contract market, contracts and the contract quests. */
final class ContractGameTests {
    static void addAll() {
        MOGameTests.add("contract_market", 20, false, ContractGameTests::market);
        MOGameTests.add("contract_quests", 20, false, ContractGameTests::quests);
        MOGameTests.add("contract_gmo", 20, false, ContractGameTests::gmo);
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        helper.assertTrue(ok, message);
    }

    /** The market waits 30 minutes, then puts a contract in; the next one waits 5 more minutes per filled slot. */
    private static void market(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, MOBlocks.CONTRACT_MARKET.get());
        var market = helper.<ContractMarketBlockEntity>getBlockEntity(pos);
        int wait = market.getTimeUntilNextQuest();
        check(helper, wait > ContractMarketBlockEntity.QUEST_GENERATE_DELAY_MIN - 40, "first delay " + wait);
        market.loadWithComponents(new net.minecraft.nbt.CompoundTag(), helper.getLevel().registryAccess());
        helper.runAfterDelay(2, () -> {
            ItemStack contract = market.getContract(0);
            check(helper, contract.is(MOItems.CONTRACT.get()) && ContractItem.getQuest(contract) != null, "no contract: " + contract);
            int next = market.getTimeUntilNextQuest();
            check(helper, next > ContractMarketBlockEntity.QUEST_GENERATE_DELAY_MIN + ContractMarketBlockEntity.QUEST_GENERATE_DELAY_PER_SLOT - 40,
                    "next delay " + next);
            // taking the contract (1.7.10 QUEST_ACTION_ADD) uses it up and starts its quest
            ServerPlayer player = AndroidGameTests.player(helper);
            player.getInventory().setItem(3, contract.copy());
            QuestStack quest = ContractItem.getQuest(contract);
            QuestPayloads.apply(player, QuestPayloads.Action.ADD, 3);
            check(helper, player.getInventory().getItem(3).isEmpty() && PlayerQuests.get(player).hasQuest(quest), "contract not taken");
            player.discard();
            helper.succeed();
        });
    }

    /** Kill androids, gather crops (taken on completion), craft anvils, mine diamonds. */
    private static void quests(GameTestHelper helper) {
        ServerPlayer player = AndroidGameTests.player(helper);
        var random = helper.getLevel().random;
        // kill androids: counts rogue androids, completes itself
        QuestStack kill = Quests.KILL_ANDROIDS.generate(random);
        QuestEvents.addQuest(player, kill);
        var logic = (KillCreatureLogic) Quests.KILL_ANDROIDS.logic();
        int max = logic.getMaxKillCount(kill);
        check(helper, max >= 12 && max < 28, "kill count " + max);
        var android = MOEntities.ROGUE_ANDROID.get().create(helper.getLevel());
        for (int i = 0; i < max; i++) {
            QuestEvents.onEvent(player, new LivingDeathEvent(android, helper.getLevel().damageSources().playerAttack(player)));
        }
        QuestEvents.manageQuestCompletion(player);
        check(helper, PlayerQuests.get(player).hasCompletedQuest(kill), "androids quest not completed");
        check(helper, player.getInventory().contains(new ItemStack(MOItems.ROGUE_ANDROID_HEAD.get())), "no android part reward");
        // department of agriculture: have the crop, completing takes it
        QuestStack crops = Quests.DEPARTMENT_OF_AGRICULTURE.generate(random);
        QuestEvents.addQuest(player, crops);
        var collect = (CollectItemLogic) Quests.DEPARTMENT_OF_AGRICULTURE.logic();
        ItemStack crop = collect.getItem(crops);
        int need = collect.getMaxItemCount(crops);
        check(helper, need >= 32 && need < 64, "crop count " + need);
        player.getInventory().add(new ItemStack(crop.getItem(), need + 3));
        check(helper, collect.isObjectiveCompleted(crops, player, 0), "crops not counted");
        QuestPayloads.apply(player, QuestPayloads.Action.COMPLETE, PlayerQuests.get(player).getActiveQuests().indexOf(crops));
        check(helper, PlayerQuests.get(player).hasCompletedQuest(crops) && player.getInventory().countItem(crop.getItem()) == 3,
                "crops not taken: " + player.getInventory().countItem(crop.getItem()));
        // weapons of war: crafting anvils
        QuestStack war = Quests.WEAPONS_OF_WAR.generate(random);
        QuestEvents.addQuest(player, war);
        int crafts = ((matteroverdrive.quest.logic.CraftLogic) Quests.WEAPONS_OF_WAR.logic()).getMaxCraftCount(war);
        for (int i = 0; i < crafts; i++) {
            QuestEvents.onEvent(player, new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(player,
                    new ItemStack(Items.ANVIL), new net.minecraft.world.SimpleContainer(9)));
        }
        QuestEvents.manageQuestCompletion(player);
        check(helper, PlayerQuests.get(player).hasCompletedQuest(war), "weapons of war not completed after " + crafts);
        // one true love: one diamond, deepslate counts
        QuestStack love = Quests.ONE_TRUE_LOVE.generate(random);
        QuestEvents.addQuest(player, love);
        BlockPos ore = helper.absolutePos(new BlockPos(4, 1, 4));
        QuestEvents.onEvent(player, new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(helper.getLevel(), ore,
                Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState(), player));
        QuestEvents.manageQuestCompletion(player);
        check(helper, PlayerQuests.get(player).hasCompletedQuest(love), "one true love not completed");
        player.discard();
        helper.succeed();
    }

    /** G.M.O.: the mad scientist's crate holds its contract and his pad; carrots then potatoes, scanned with that pad. */
    private static void gmo(GameTestHelper helper) {
        ServerPlayer player = AndroidGameTests.player(helper);
        var contents = matteroverdrive.world.MadScientistCrateProcessor.contents(helper.getLevel().random);
        QuestStack gmo = ContractItem.getQuest(contents.get(0));
        ItemStack pad = contents.get(1);
        check(helper, gmo != null && gmo.getQuest() == Quests.GMO, "crate contract " + contents.get(0));
        check(helper, !DataPadItem.hasGui(pad) && DataPadItem.canScan(pad, Blocks.CARROTS.defaultBlockState()), "crate pad " + pad);
        QuestEvents.addQuest(player, gmo.copy());
        QuestStack active = PlayerQuests.get(player).findActive(Quests.GMO);
        var multi = (matteroverdrive.quest.Quest.Multi) Quests.GMO;
        check(helper, multi.getObjectivesCount(active, player) == 1, "sequential: one objective at first");
        // potatoes don't count before the carrots are done; a pad that doesn't destroy doesn't count at all
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        QuestEvents.onEvent(player, new QuestEvents.Scan(at, Blocks.POTATOES.defaultBlockState(), pad));
        QuestEvents.onEvent(player, new QuestEvents.Scan(at, Blocks.CARROTS.defaultBlockState(), new ItemStack(MOItems.DATA_PAD.get())));
        check(helper, active.getData().getCompound("1").getShort("BlockScan") == 0
                && active.getData().getCompound("0").getShort("BlockScan") == 0, "counted the wrong scans " + active.getData());
        for (int i = 0; i < 24; i++) QuestEvents.onEvent(player, new QuestEvents.Scan(at, Blocks.CARROTS.defaultBlockState(), pad));
        check(helper, multi.getObjectivesCount(active, player) == 2, "carrots done should show the potatoes " + active.getData());
        for (int i = 0; i < 24; i++) QuestEvents.onEvent(player, new QuestEvents.Scan(at, Blocks.POTATOES.defaultBlockState(), pad));
        QuestEvents.manageQuestCompletion(player);
        check(helper, PlayerQuests.get(player).hasCompletedQuest(gmo), "gmo not completed " + active.getData());
        ItemStack spine = ItemStack.EMPTY;
        for (ItemStack s : matteroverdrive.compat.ContainerItems.of(player.getInventory())) {
            if (s.is(MOItems.TRITANIUM_SPINE.get())) spine = s;
        }
        check(helper, !spine.isEmpty() && ((matteroverdrive.item.android.BionicPartItem) spine.getItem()).maxHealthBonus(spine) == 5, "hardened spine " + spine);
        player.discard();
        helper.succeed();
    }
}
