package matteroverdrive.block;


import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.7.10 BlockAndroidStation: a 9/16 table where androids buy biotic stats and fit bionic parts. Humans are refused. */
public class AndroidStationBlock extends MachineBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 9, 16);

    public AndroidStationBlock(Properties properties) {
        super(MOBlockEntities.ANDROID_STATION, properties);
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!Android.isAndroid(player)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("alert." + MatterOverdrive.MODID + ".not_android").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.SUCCESS;
        }
        return super.use(state, level, pos, player, hand, hit);
    }
}
