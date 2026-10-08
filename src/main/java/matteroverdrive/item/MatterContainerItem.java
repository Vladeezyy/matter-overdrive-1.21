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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

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
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, full ? ClipContext.Fluid.NONE : ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos)) return InteractionResult.FAIL;
        if (!full) {
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.is(MOFluids.MATTER_PLASMA.get()) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) return InteractionResult.PASS;
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            level.playSound(player, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1, 1);
            return InteractionResult.SUCCESS.heldItemTransformedTo(
                    ItemUtils.createFilledResult(stack, player, new ItemStack(MOItems.MATTER_CONTAINER_FULL.get())));
        }
        BlockPos target = pos.relative(hit.getDirection());
        BlockState there = level.getBlockState(target);
        if (!there.isAir() && !there.canBeReplaced(MOFluids.MATTER_PLASMA.get()) || !player.mayUseItemAt(target, hit.getDirection(), stack)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            if (!there.isAir() && !there.liquid()) level.destroyBlock(target, true);
            level.setBlock(target, MOBlocks.MATTER_PLASMA.get().defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
        level.playSound(player, target, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
        ItemStack result = player.hasInfiniteMaterials() ? stack
                : ItemUtils.createFilledResult(stack, player, new ItemStack(MOItems.MATTER_CONTAINER.get()));
        return InteractionResult.SUCCESS.heldItemTransformedTo(result);
    }

    /** Empty ↔ full container with exactly 32 mB of Matter Plasma (1.7.10 FluidContainerRegistry). */
    public static class FluidHandler extends ItemAccessResourceHandler<FluidResource> {
        public FluidHandler(ItemAccess access) {
            super(access, 1);
        }

        @Override
        protected FluidResource getResourceFrom(ItemResource resource, int index) {
            return resource.is(MOItems.MATTER_CONTAINER_FULL.get()) ? FluidResource.of(MOFluids.MATTER_PLASMA.get()) : FluidResource.EMPTY;
        }

        @Override
        protected int getAmountFrom(ItemResource resource, int index) {
            return resource.is(MOItems.MATTER_CONTAINER_FULL.get()) ? CAPACITY : 0;
        }

        @Override
        protected ItemResource update(ItemResource resource, int index, FluidResource newResource, int newAmount) {
            if (newAmount == 0) return ItemResource.of(MOItems.MATTER_CONTAINER.get());
            if (newAmount == CAPACITY && newResource.getFluid() == MOFluids.MATTER_PLASMA.get()) return ItemResource.of(MOItems.MATTER_CONTAINER_FULL.get());
            return ItemResource.EMPTY;
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.getFluid() == MOFluids.MATTER_PLASMA.get();
        }

        @Override
        protected int getCapacity(int index, FluidResource resource) {
            return CAPACITY;
        }
    }
}
