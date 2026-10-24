package matteroverdrive.block;


import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.matternet.MatterNetworkBlock;

/** Pattern Storage (1.7.10 BlockPatternStorage), rendered with the original pattern_storage.obj. */
public class PatternStorageBlock extends MachineBlock implements MatterNetworkBlock {

    /** Which drive slots hold a drive: the drives are part of the block model (baked once, not drawn every frame). */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty[] DRIVE = new net.minecraft.world.level.block.state.properties.BooleanProperty[6];

    static {
        for (int i = 0; i < DRIVE.length; i++) DRIVE[i] = net.minecraft.world.level.block.state.properties.BooleanProperty.create("drive_" + i);
    }

    public PatternStorageBlock(Properties properties) {
        super(MOBlockEntities.PATTERN_STORAGE, properties);
        var state = defaultBlockState();
        for (var drive : DRIVE) state = state.setValue(drive, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,
            net.minecraft.world.level.block.state.BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DRIVE);
    }

}
