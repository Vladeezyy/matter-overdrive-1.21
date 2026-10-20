package matteroverdrive.fabric;

import matteroverdrive.block.entity.DecomposerBlockEntity;
import matteroverdrive.block.entity.MatterPipeBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOFluids;
import matteroverdrive.init.MOItems;
import matteroverdrive.platform.Platform;
import matteroverdrive.platform.transfer.IItemHandler;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import team.reborn.energy.api.EnergyStorage;

/**
 * Fabric only: the mod's storages as other Fabric mods see them (transfer API, Team Reborn Energy), with transactions
 * that commit, abort and nest, and other mods' storages (here a vanilla chest) as the mod sees them.
 */
public final class FabricTransferTests {
    private static final String AREA = "matteroverdrive:gametest_area";

    @GameTest(template = AREA)
    public void machineEnergyTransactions(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, MOBlocks.DECOMPOSER.get().defaultBlockState());
        DecomposerBlockEntity machine = (DecomposerBlockEntity) helper.getLevel().getBlockEntity(pos);
        EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), pos, Direction.UP);
        helper.assertTrue(storage != null, "no TR energy storage on the decomposer");
        int before = machine.getEnergyHandler(null).getEnergyStored();
        try (Transaction tx = Transaction.openOuter()) {
            helper.assertTrue(storage.insert(5000, tx) == 5000, "insert did not take 5000");
            helper.assertTrue(storage.getAmount() == before + 5000, "pending energy not shown");
            // aborted
        }
        helper.assertTrue(machine.getEnergyHandler(null).getEnergyStored() == before, "an aborted insert changed the energy");
        try (Transaction tx = Transaction.openOuter()) {
            try (Transaction nested = tx.openNested()) {
                storage.insert(1000, nested);
                // aborted nested
            }
            storage.insert(3000, tx);
            tx.commit();
        }
        int after = machine.getEnergyHandler(null).getEnergyStored();
        helper.assertTrue(after == before + 3000, "committed 3000 (nested 1000 aborted), got " + (after - before));
        helper.succeed();
    }

    @GameTest(template = AREA)
    public void machineItemsTransactions(GameTestHelper helper) {
        // the matter table is built on first use (in a game: when a player joins); the input slot takes items with matter
        matteroverdrive.matter.MatterRegistry.values(helper.getLevel().getServer());
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, MOBlocks.DECOMPOSER.get().defaultBlockState());
        DecomposerBlockEntity machine = (DecomposerBlockEntity) helper.getLevel().getBlockEntity(pos);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), pos, Direction.UP);
        helper.assertTrue(storage != null, "no item storage on the decomposer");
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = storage.insert(ItemVariant.of(Items.IRON_INGOT), 10, tx);
            helper.assertTrue(inserted == 10, "inserted " + inserted + " of 10 iron");
            tx.commit();
        }
        ItemStack input = machine.getInventory().getStackInSlot(DecomposerBlockEntity.INPUT);
        helper.assertTrue(input.is(Items.IRON_INGOT) && input.getCount() == 10, "input slot holds " + input);
        // the output slot only gives, the input only takes
        try (Transaction tx = Transaction.openOuter()) {
            helper.assertTrue(storage.extract(ItemVariant.of(Items.IRON_INGOT), 10, tx) == 0, "took items out of the input slot");
        }
        helper.succeed();
    }

    @GameTest(template = AREA)
    public void pipeFluid(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, MOBlocks.MATTER_PIPE.get().defaultBlockState());
        MatterPipeBlockEntity pipe = (MatterPipeBlockEntity) helper.getLevel().getBlockEntity(pos);
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(helper.getLevel(), pos, Direction.UP);
        helper.assertTrue(storage != null, "no fluid storage on the matter pipe");
        long droplets;
        try (Transaction tx = Transaction.openOuter()) {
            droplets = storage.insert(FluidVariant.of(MOFluids.MATTER_PLASMA.get()), 32 * FabricTransfer.DROPLETS_PER_MB, tx);
            tx.commit();
        }
        int stored = pipe.getTank().getFluidInTank(0).getAmount();
        helper.assertTrue(droplets > 0 && stored * FabricTransfer.DROPLETS_PER_MB == droplets, "pipe took " + droplets + " droplets, holds " + stored + " mB");
        helper.succeed();
    }

    @GameTest(template = AREA)
    public void batteryItemEnergy(GameTestHelper helper) {
        ItemStack battery = new ItemStack(MOItems.BATTERY.get());
        ContainerItemContext context = FabricTransfer.stackContext(battery);
        EnergyStorage storage = context.find(EnergyStorage.ITEM);
        helper.assertTrue(storage != null, "no TR energy storage on the battery");
        try (Transaction tx = Transaction.openOuter()) {
            helper.assertTrue(storage.insert(2000, tx) > 0, "the battery took no energy");
            tx.commit();
        }
        ItemStack charged = context.getItemVariant().toStack();
        int energy = MOItems.BATTERY.get().createEnergyHandler(charged).getEnergyStored();
        helper.assertTrue(energy > 0 && energy == storage.getAmount(), "battery holds " + energy + ", storage says " + storage.getAmount());
        helper.succeed();
    }

    @GameTest(template = AREA)
    public void vanillaChestAsHandler(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        IItemHandler handler = Platform.INSTANCE.itemHandler(helper.getLevel(), pos, Direction.UP).orElse(null);
        helper.assertTrue(handler != null && handler.getSlots() == 27, "the chest is not 27 slots: " + handler);
        ItemStack rest = handler.insertItem(0, new ItemStack(Items.DIAMOND, 5), false);
        helper.assertTrue(rest.isEmpty() && handler.getStackInSlot(0).getCount() == 5, "the chest did not take 5 diamonds");
        helper.assertTrue(handler.extractItem(0, 2, true).getCount() == 2 && handler.getStackInSlot(0).getCount() == 5, "simulated extract changed the chest");
        helper.succeed();
    }
}
