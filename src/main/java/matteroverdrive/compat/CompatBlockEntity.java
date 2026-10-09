package matteroverdrive.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Bridge to the 1.21.6+ block entity saves through {@link ValueOutput} / {@link ValueInput}. */
public abstract class CompatBlockEntity extends BlockEntity {
    protected CompatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected void saveAdditional(ValueOutput output) {}

    protected void loadAdditional(ValueInput input) {}

    public void removeComponentsFromTag(ValueOutput output) {}

    @Override
    protected final void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveAdditional(ValueOutput.of(tag, registries));
    }

    @Override
    protected final void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadAdditional(ValueInput.of(tag, registries));
    }

    @Override
    public final void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        removeComponentsFromTag(ValueOutput.of(tag, net.minecraft.core.RegistryAccess.EMPTY));
    }
}
