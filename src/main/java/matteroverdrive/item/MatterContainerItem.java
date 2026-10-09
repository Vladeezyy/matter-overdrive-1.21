package matteroverdrive.item;

import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOFluids;
import matteroverdrive.init.MOItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1.7.10 MatterContainer: a bucket for Matter Plasma holding 32 mB (stacks of 8). Scoops up a plasma source block or
 * places one; machines and pipes of other mods can fill and drain it through the fluid item capability.
 */
public class MatterContainerItem extends Item {
    public static final int CAPACITY = 32;
    private final boolean full;

    public MatterContainerItem(boolean full, Properties properties) {
        super(properties);
        this.full = full;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, full ? ClipContext.Fluid.NONE : ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos)) return net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand));
        if (!full) {
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.is(MOFluids.MATTER_PLASMA.get()) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            level.playSound(player, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1, 1);
            return net.minecraft.world.InteractionResultHolder.success(
                    ItemUtils.createFilledResult(stack, player, new ItemStack(MOItems.MATTER_CONTAINER_FULL.get())));
        }
        BlockPos target = pos.relative(hit.getDirection());
        BlockState there = level.getBlockState(target);
        if (!there.isAir() && !there.canBeReplaced(MOFluids.MATTER_PLASMA.get()) || !player.mayUseItemAt(target, hit.getDirection(), stack)) {
            return net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        if (!level.isClientSide()) {
            if (!there.isAir() && !there.liquid()) level.destroyBlock(target, true);
            level.setBlock(target, MOBlocks.MATTER_PLASMA.get().defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
        level.playSound(player, target, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
        ItemStack result = player.hasInfiniteMaterials() ? stack
                : ItemUtils.createFilledResult(stack, player, new ItemStack(MOItems.MATTER_CONTAINER.get()));
        return net.minecraft.world.InteractionResultHolder.success(result);
    }

    /** Empty ↔ full container with exactly 32 mB of Matter Plasma (1.7.10 FluidContainerRegistry). */
    public static class FluidHandler implements net.neoforged.neoforge.fluids.capability.IFluidHandlerItem {
        private ItemStack container;

        public FluidHandler(ItemStack container) {
            this.container = container;
        }

        private boolean full() {
            return container.is(MOItems.MATTER_CONTAINER_FULL.get());
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return full() ? new FluidStack(MOFluids.MATTER_PLASMA.get(), CAPACITY) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.is(MOFluids.MATTER_PLASMA.get());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (container.getCount() != 1 || full() || !isFluidValid(0, resource) || resource.getAmount() < CAPACITY) return 0;
            if (action.execute()) container = container.transmuteCopy(MOItems.MATTER_CONTAINER_FULL.get(), 1);
            return CAPACITY;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return isFluidValid(0, resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (container.getCount() != 1 || !full() || maxDrain < CAPACITY) return FluidStack.EMPTY;
            if (action.execute()) container = container.transmuteCopy(MOItems.MATTER_CONTAINER.get(), 1);
            return new FluidStack(MOFluids.MATTER_PLASMA.get(), CAPACITY);
        }
    }
}
