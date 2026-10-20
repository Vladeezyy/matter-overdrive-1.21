package matteroverdrive.neoforge;

import matteroverdrive.block.entity.DecomposerBlockEntity;
import matteroverdrive.block.entity.MatterPipeBlockEntity;
import matteroverdrive.gametest.MOGameTests;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOFluids;
import matteroverdrive.init.MOItems;
import matteroverdrive.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * NeoForge 21.9+ only: the mod's storages as other mods see them (energy, item and fluid handlers with root, nested
 * and aborted transactions, an item exchanged in its slot), and another storage (a vanilla chest) as the mod sees it.
 */
final class NeoForgeTransferTests {
    private NeoForgeTransferTests() {}

    static void register() {
        MOGameTests.addLoaderTest("neoforge_machine_energy", 20, NeoForgeTransferTests::machineEnergy);
        MOGameTests.addLoaderTest("neoforge_machine_items", 20, NeoForgeTransferTests::machineItems);
        MOGameTests.addLoaderTest("neoforge_pipe_fluid", 20, NeoForgeTransferTests::pipeFluid);
        MOGameTests.addLoaderTest("neoforge_battery_energy", 20, NeoForgeTransferTests::batteryEnergy);
        MOGameTests.addLoaderTest("neoforge_chest_handler", 20, NeoForgeTransferTests::chestHandler);
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        helper.assertTrue(ok, Component.literal(message));
    }

    private static void machineEnergy(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, MOBlocks.DECOMPOSER.get().defaultBlockState());
        DecomposerBlockEntity machine = (DecomposerBlockEntity) helper.getLevel().getBlockEntity(pos);
        var handler = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, pos, Direction.UP);
        check(helper, handler != null, "no energy handler on the decomposer");
        int before = machine.getEnergyHandler(null).getEnergyStored();
        try (var tx = Transaction.openRoot()) {
            check(helper, handler.insert(5000, tx) == 5000, "insert did not take 5000");
            check(helper, handler.getAmountAsLong() == before + 5000, "pending energy not shown");
        }
        check(helper, machine.getEnergyHandler(null).getEnergyStored() == before, "an aborted insert changed the energy");
        try (var tx = Transaction.openRoot()) {
            try (var nested = Transaction.open(tx)) {
                handler.insert(1000, nested);
            }
            handler.insert(3000, tx);
            tx.commit();
        }
        int after = machine.getEnergyHandler(null).getEnergyStored();
        check(helper, after == before + 3000, "committed 3000 (nested 1000 aborted), got " + (after - before));
        helper.succeed();
    }

    private static void machineItems(GameTestHelper helper) {
        matteroverdrive.matter.MatterRegistry.values(helper.getLevel().getServer());
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, MOBlocks.DECOMPOSER.get().defaultBlockState());
        DecomposerBlockEntity machine = (DecomposerBlockEntity) helper.getLevel().getBlockEntity(pos);
        var handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, pos, Direction.UP);
        check(helper, handler != null, "no item handler on the decomposer");
        try (var tx = Transaction.openRoot()) {
            int inserted = handler.insert(ItemResource.of(Items.IRON_INGOT), 10, tx);
            check(helper, inserted == 10, "inserted " + inserted + " of 10 iron");
            tx.commit();
        }
        ItemStack input = machine.getInventory().getStackInSlot(DecomposerBlockEntity.INPUT);
        check(helper, input.is(Items.IRON_INGOT) && input.getCount() == 10, "input slot holds " + input);
        try (var tx = Transaction.openRoot()) {
            check(helper, handler.extract(ItemResource.of(Items.IRON_INGOT), 10, tx) == 0, "took items out of the input slot");
        }
        helper.succeed();
    }

    private static void pipeFluid(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, MOBlocks.MATTER_PIPE.get().defaultBlockState());
        MatterPipeBlockEntity pipe = (MatterPipeBlockEntity) helper.getLevel().getBlockEntity(pos);
        var handler = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, pos, Direction.UP);
        check(helper, handler != null, "no fluid handler on the matter pipe");
        int moved;
        try (var tx = Transaction.openRoot()) {
            moved = handler.insert(FluidResource.of(MOFluids.MATTER_PLASMA.get()), 32, tx);
            tx.commit();
        }
        int stored = pipe.getTank().getFluidInTank(0).getAmount();
        check(helper, moved > 0 && stored == moved, "pipe took " + moved + " mB, holds " + stored);
        helper.succeed();
    }

    private static void batteryEnergy(GameTestHelper helper) {
        var slots = new ItemStacksResourceHandler(NonNullList.of(ItemStack.EMPTY, new ItemStack(MOItems.BATTERY.get())));
        var access = ItemAccess.forHandlerIndex(slots, 0);
        var handler = access.getCapability(Capabilities.Energy.ITEM);
        check(helper, handler != null, "no energy handler on the battery");
        try (var tx = Transaction.openRoot()) {
            check(helper, handler.insert(2000, tx) > 0, "the battery took no energy");
            tx.commit();
        }
        ItemStack charged = slots.getResource(0).toStack();
        int energy = MOItems.BATTERY.get().createEnergyHandler(charged).getEnergyStored();
        check(helper, energy > 0, "battery holds " + energy);
        helper.succeed();
    }

    private static void chestHandler(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        var handler = Platform.INSTANCE.itemHandler(helper.getLevel(), pos, Direction.UP).orElse(null);
        check(helper, handler != null && handler.getSlots() == 27, "the chest is not 27 slots: " + handler);
        ItemStack rest = handler.insertItem(0, new ItemStack(Items.DIAMOND, 5), false);
        check(helper, rest.isEmpty() && handler.getStackInSlot(0).getCount() == 5, "the chest did not take 5 diamonds");
        check(helper, handler.extractItem(0, 2, true).getCount() == 2 && handler.getStackInSlot(0).getCount() == 5, "simulated extract changed the chest");
        helper.succeed();
    }
}
