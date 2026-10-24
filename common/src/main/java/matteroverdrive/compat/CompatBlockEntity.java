package matteroverdrive.compat;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.compat.component.DataComponentMap;
import matteroverdrive.compat.component.DataComponentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.20.1 bridge to the 1.21.10 block entity hooks: saves through {@link ValueOutput}/{@link ValueInput}, the
 * components an item of the block carries ({@link #collectImplicitComponents} for the drop, {@link #applyImplicitComponents}
 * on placement, both kept in the item's NBT), and {@link #preRemoveSideEffects} (called by the block's onRemove).
 */
public abstract class CompatBlockEntity extends BlockEntity {
    /** Built-in registries, for loading before the block entity has a level (chunk loading) */
    private static final RegistryAccess BUILT_IN = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    protected CompatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected HolderLookup.Provider registries() {
        return level != null ? level.registryAccess() : BUILT_IN;
    }

    protected void saveAdditional(ValueOutput output) {}

    protected void loadAdditional(ValueInput input) {}

    public void removeComponentsFromTag(ValueOutput output) {}

    protected void collectImplicitComponents(DataComponentMap.Builder components) {}

    protected void applyImplicitComponents(DataComponentGetter components) {}

    /** What an item of this block carries (1.20.5 {@code collectComponents}) */
    public final DataComponentMap collectComponents() {
        DataComponentMap.Builder builder = DataComponentMap.builder();
        collectImplicitComponents(builder);
        return builder.build();
    }

    /** Placed from an item: take over what the item carries (1.20.5 {@code applyComponentsFromItemStack}) */
    public final void applyComponentsFromItemStack(ItemStack stack) {
        applyImplicitComponents(DataComponentMap.of(stack));
        setChanged();
    }

    /** Set around a setBlock that 1.21.10 would do with UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS (the dev scene). */
    public static boolean skipRemoveSideEffects;

    /** 1.21.10 BlockEntity.preRemoveSideEffects: the block is being removed (not just its state changed). */
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    @Override
    protected final void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveAdditional(ValueOutput.of(tag, registries()));
    }

    @Override
    public final void load(CompoundTag tag) {
        super.load(tag);
        loadAdditional(ValueInput.of(tag, registries()));
    }
}
