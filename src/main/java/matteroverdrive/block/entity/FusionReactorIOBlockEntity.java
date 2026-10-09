package matteroverdrive.block.entity;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MatterTank;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 1.7.10 TileEntityFusionReactorPart (fusion_reactor_io): a port in the reactor ring that exposes the controller's
 * energy (out) and matter tank (in). The controller links its ports on every structure check.
 */
public class FusionReactorIOBlockEntity extends BlockEntity {
    private @Nullable BlockPos controller;

    public FusionReactorIOBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.FUSION_REACTOR_IO.get(), pos, state);
    }

    public void setController(@Nullable BlockPos controller) {
        if (!java.util.Objects.equals(this.controller, controller)) {
            this.controller = controller;
            setChanged();
            if (getLevel() != null) getLevel().invalidateCapabilities(getBlockPos());
        }
    }

    private @Nullable FusionReactorControllerBlockEntity controller() {
        if (controller != null && getLevel() != null && getLevel().isLoaded(controller)
                && getLevel().getBlockEntity(controller) instanceof FusionReactorControllerBlockEntity c && c.isValidStructure()) {
            return c;
        }
        return null;
    }

    public @Nullable IEnergyStorage getEnergy() {
        FusionReactorControllerBlockEntity c = controller();
        return c == null ? null : c.getEnergy();
    }

    public @Nullable MatterTank getMatter() {
        FusionReactorControllerBlockEntity c = controller();
        return c == null ? null : c.getMatterTank();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (controller != null) output.putLong("controller", controller.asLong());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        controller = input.getLong("controller").map(BlockPos::of).orElse(null);
    }
}
