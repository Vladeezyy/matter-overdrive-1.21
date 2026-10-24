package matteroverdrive.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1.20.2+ GameTestHelper calls missing from 1.20.1 */
public final class GameTests {
    private GameTests() {}

    /** {@code matteroverdrive.compat.GameTests.<T>blockEntity(helper, pos)} */
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> T blockEntity(GameTestHelper helper, BlockPos pos) {
        return (T) helper.getBlockEntity(pos);
    }

    /** {@code helper.getTestRotation()} */
    public static net.minecraft.world.level.block.Rotation rotation(GameTestHelper helper) {
        return helper.testInfo.getRotation();
    }
}
