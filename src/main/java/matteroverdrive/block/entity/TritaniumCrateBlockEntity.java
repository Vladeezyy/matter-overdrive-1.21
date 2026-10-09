package matteroverdrive.block.entity;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MOSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;

import java.util.List;

/** 1.7.10 TileEntityTritaniumCrate: 54 slots in the vanilla chest screen, crate open/close sounds, optional loot table. */
public class TritaniumCrateBlockEntity extends RandomizableContainerBlockEntity {
    public static final int SIZE = 54;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            playSound(MOSounds.CRATE_OPEN.get());
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            playSound(MOSounds.CRATE_CLOSE.get());
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == TritaniumCrateBlockEntity.this;
        }
    };

    public TritaniumCrateBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.TRITANIUM_CRATE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, items, registries);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, items, registries);
        }
    }

    /** The contents go into the dropped item (container component), not onto the ground. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.matteroverdrive.tritanium_crate");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.sixRows(id, inventory, this);
    }

    @Override
    public void startOpen(Player player) {
        if (!remove && !player.isSpectator()) {
            openers.incrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!remove && !player.isSpectator()) {
            openers.decrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
        }
    }

    public void recheckOpen() {
        if (!remove) {
            openers.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
        }
    }

    private void playSound(SoundEvent sound) {
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.5f, 1);
    }
}
