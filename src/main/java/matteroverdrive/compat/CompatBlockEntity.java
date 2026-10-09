package matteroverdrive.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.21.1 bridge to the 1.21.10 block entity hooks: saves through {@link ValueOutput}/{@link ValueInput}, implicit
 * components through {@link DataComponentGetter}, and {@link #preRemoveSideEffects} (called by the block's onRemove).
 */
public abstract class CompatBlockEntity extends BlockEntity {
    protected CompatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected void saveAdditional(ValueOutput output) {}

    protected void loadAdditional(ValueInput input) {}

    public void removeComponentsFromTag(ValueOutput output) {}

    protected void applyImplicitComponents(DataComponentGetter components) {}

    /** 1.21.10 BlockEntity.preRemoveSideEffects: the block is being removed (not just its state changed). */
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

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

    @Override
    protected final void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        applyImplicitComponents(new DataComponentGetter() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(DataComponentType<? extends T> type) {
                return input.get((DataComponentType<T>) type);
            }
        });
    }
}
