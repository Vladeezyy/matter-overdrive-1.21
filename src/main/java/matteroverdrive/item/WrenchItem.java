package matteroverdrive.item;

import matteroverdrive.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7.10 Wrench: sneak-use dismantles a machine into an item that keeps its energy; use rotates it. */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MachineBlock)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player != null && player.isShiftKeyDown()) {
            MachineBlock.dismantle((ServerLevel) level, pos, state, player);
        } else {
            level.setBlock(pos, state.rotate(level, pos, Rotation.CLOCKWISE_90), 3);
        }
        return InteractionResult.SUCCESS;
    }
}
