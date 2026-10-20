package matteroverdrive.platform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/** A registered block ({@code DeferredBlock}): usable wherever an {@link ItemLike} is (its block item) */
public class BlockEntry<T extends Block> extends RegistryEntry<Block, T> implements ItemLike {
    BlockEntry(ResourceLocation id) {
        super(Registries.BLOCK, id);
    }

    @Override
    public Item asItem() {
        return this.get().asItem();
    }

    public ItemStack toStack() {
        return new ItemStack(this.get());
    }

    public ItemStack toStack(int count) {
        return new ItemStack(this.get(), count);
    }
}
