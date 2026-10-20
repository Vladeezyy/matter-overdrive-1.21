package matteroverdrive.block;

import com.mojang.serialization.MapCodec;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;

/** Matter Recycler (1.7.10 BlockMatterRecycler). */
public class RecyclerBlock extends MachineBlock {
    public static final MapCodec<RecyclerBlock> CODEC = simpleCodec(RecyclerBlock::new);

    public RecyclerBlock(Properties properties) {
        super(MOBlockEntities.RECYCLER, properties);
    }

    @Override
    protected MapCodec<RecyclerBlock> codec() {
        return CODEC;
    }
}
