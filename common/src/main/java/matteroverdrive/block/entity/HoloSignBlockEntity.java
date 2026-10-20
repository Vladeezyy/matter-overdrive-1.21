package matteroverdrive.block.entity;

import matteroverdrive.init.MOBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;

/** 1.7.10 TileEntityHoloSign: just the text (lines separated by \n). */
public class HoloSignBlockEntity extends matteroverdrive.compat.CompatBlockEntity {
    public static final int MAX_LENGTH = 512;
    private String text = "";

    public HoloSignBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.HOLO_SIGN.get(), pos, state);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("text", text);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        text = input.getStringOr("text", "");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
