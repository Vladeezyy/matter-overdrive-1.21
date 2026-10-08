package matteroverdrive.gametest;

import matteroverdrive.block.entity.ContractMarketBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MODecorative;
import matteroverdrive.init.MOItems;
import matteroverdrive.item.ContractItem;
import matteroverdrive.item.SecurityProtocolItem;
import matteroverdrive.quest.PlayerQuests;
import matteroverdrive.quest.QuestEvents;
import matteroverdrive.quest.QuestStack;
import matteroverdrive.quest.Quests;
import matteroverdrive.quest.logic.PlaceBlockLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Phase 7r checks: the security protocol and machine claims, and the crash landing / we must know contracts. */
final class SecurityGameTests {
    static void addAll() {
        MOGameTests.add("security_protocol", 20, false, SecurityGameTests::protocol);
        MOGameTests.add("crash_landing", 20, false, SecurityGameTests::crashLanding);
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        helper.assertTrue(ok, Component.literal(message));
    }

    private static void sneakUse(ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(true);
        stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
    }

    private static boolean useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(pos);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        return stack.getItem().onItemUseFirst(stack, context).consumesAction();
    }

    /** Sneak-use makes it yours and cycles claim / access / remove; claims, access and removal on a machine. */
    private static void protocol(GameTestHelper helper) {
        ServerPlayer owner = AndroidGameTests.player(helper);
        ServerPlayer other = AndroidGameTests.player(helper);
        ItemStack protocol = new ItemStack(MOItems.SECURITY_PROTOCOL.get(), 3);
        sneakUse(other, protocol.copy());   // nothing for a plain use
        sneakUse(owner, protocol);
        check(helper, owner.getUUID().equals(SecurityProtocolItem.getOwner(protocol)) && SecurityProtocolItem.getType(protocol) == SecurityProtocolItem.CLAIM,
                "claim protocol " + protocol.getComponents());
        sneakUse(other, protocol);          // someone else's protocol doesn't cycle
        check(helper, SecurityProtocolItem.getType(protocol) == SecurityProtocolItem.CLAIM, "cycled by another player");
        sneakUse(owner, protocol);
        check(helper, SecurityProtocolItem.getType(protocol) == SecurityProtocolItem.ACCESS, "access");
        sneakUse(owner, protocol);
        check(helper, SecurityProtocolItem.getType(protocol) == SecurityProtocolItem.REMOVE, "remove");
        sneakUse(owner, protocol);
        check(helper, SecurityProtocolItem.getType(protocol) == SecurityProtocolItem.CLAIM, "back to claim");

        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, MOBlocks.CONTRACT_MARKET.get());
        var machine = helper.getBlockEntity(pos, ContractMarketBlockEntity.class);
        check(helper, machine.isUseableByPlayer(other) && machine.canRemove(other), "unclaimed machines are everyone's");
        check(helper, useOn(helper, owner, protocol, pos) && protocol.getCount() == 2 && owner.getUUID().equals(machine.getOwner()), "claim");
        check(helper, !useOn(helper, owner, protocol, pos) && protocol.getCount() == 2, "claimed twice");
        check(helper, machine.isUseableByPlayer(owner) && !machine.isUseableByPlayer(other) && !machine.canRemove(other), "rights");
        // the owner's [Access] protocol anywhere in the inventory lets the other player in (but not break it)
        ItemStack access = protocol.copyWithCount(1);
        access.set(matteroverdrive.init.MODataComponents.SECURITY_TYPE.get(), SecurityProtocolItem.ACCESS);
        other.getInventory().setItem(20, access);
        check(helper, machine.isUseableByPlayer(other) && !machine.canRemove(other), "access protocol");
        // the owner survives a save / load, and goes with the machine's item
        var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        check(helper, saved.contains("owner"), "owner not saved " + saved);
        check(helper, owner.getUUID().equals(machine.collectComponents().get(matteroverdrive.init.MODataComponents.SECURITY_OWNER.get())),
                "owner not on the item");
        // only the owner's [Remove] protocol removes the claim
        ItemStack foreign = new ItemStack(MOItems.SECURITY_PROTOCOL.get());
        sneakUse(other, foreign);
        sneakUse(other, foreign);
        sneakUse(other, foreign);
        check(helper, SecurityProtocolItem.getType(foreign) == SecurityProtocolItem.REMOVE, "foreign remove " + foreign.getComponents());
        check(helper, !useOn(helper, other, foreign, pos) && machine.hasOwner(), "removed by someone else");
        ItemStack remove = protocol.copyWithCount(1);
        remove.set(matteroverdrive.init.MODataComponents.SECURITY_TYPE.get(), SecurityProtocolItem.REMOVE);
        check(helper, useOn(helper, owner, remove, pos) && remove.isEmpty() && !machine.hasOwner(), "remove");
        owner.discard();
        other.discard();
        helper.succeed();
    }

    /** Crafting a security protocol completes crash landing: the relay and we must know at the crate's position. */
    private static void crashLanding(GameTestHelper helper) {
        ServerPlayer player = AndroidGameTests.player(helper);
        BlockPos crate = helper.absolutePos(new BlockPos(5, 1, 5));
        QuestStack contract = Quests.CRASH_LANDING.generate(helper.getLevel().random);
        contract.getData().putIntArray("Pos", new int[] {crate.getX(), crate.getY(), crate.getZ()});
        check(helper, ContractItem.getQuest(ContractItem.of(contract)) != null, "contract");
        QuestEvents.addQuest(player, contract);
        QuestStack active = PlayerQuests.get(player).findActive(Quests.CRASH_LANDING);
        QuestEvents.onEvent(player, new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(player,
                new ItemStack(MOItems.ISOLINEAR_CIRCUIT_MK1.get()), new net.minecraft.world.SimpleContainer(1)));
        check(helper, !active.isCompleted(), "completed by the wrong craft");
        QuestEvents.onEvent(player, new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(player,
                new ItemStack(MOItems.SECURITY_PROTOCOL.get()), new net.minecraft.world.SimpleContainer(1)));
        QuestEvents.manageQuestCompletion(player);
        check(helper, PlayerQuests.get(player).hasCompletedQuest(contract), "crash landing not completed " + active.getData());
        ItemStack relay = ItemStack.EMPTY;
        for (ItemStack s : player.getInventory()) {
            if (s.is(MODecorative.COILS.get().asItem())) relay = s;
        }
        check(helper, !relay.isEmpty() && "Communication Relay".equals(relay.getHoverName().getString()), "relay " + relay);
        QuestStack mustKnow = PlayerQuests.get(player).findActive(Quests.WE_MUST_KNOW);
        check(helper, mustKnow != null, "we must know not given");
        var logic = (PlaceBlockLogic) Quests.WE_MUST_KNOW.logic();
        check(helper, crate.equals(logic.getPos(mustKnow)), "pos not copied " + mustKnow.getData());
        var coils = MODecorative.COILS.get().defaultBlockState();
        // plain coils near the ship, or the relay too far away, don't count
        QuestEvents.onEvent(player, new QuestEvents.Place(crate.east(2), coils, new ItemStack(MODecorative.COILS.get())));
        QuestEvents.onEvent(player, new QuestEvents.Place(crate.east(5), coils, relay));
        check(helper, !mustKnow.isCompleted(), "counted the wrong placement " + mustKnow.getData());
        QuestEvents.onEvent(player, new QuestEvents.Place(crate.east(2).above(), coils, relay));
        QuestEvents.manageQuestCompletion(player);
        check(helper, PlayerQuests.get(player).hasCompletedQuest(mustKnow), "we must know not completed " + mustKnow.getData());
        player.discard();
        helper.succeed();
    }
}
