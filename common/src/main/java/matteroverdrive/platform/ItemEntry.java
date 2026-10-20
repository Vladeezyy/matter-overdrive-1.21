package matteroverdrive.platform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** A registered item ({@code DeferredItem}): usable wherever an {@link ItemLike} is */
public class ItemEntry<T extends Item> extends RegistryEntry<Item, T> implements ItemLike {
    ItemEntry(ResourceLocation id) {
        super(Registries.ITEM, id);
    }

    @Override
    public Item asItem() {
        return this.get();
    }

    public ItemStack toStack() {
        return new ItemStack(this.get());
    }

    public ItemStack toStack(int count) {
        return new ItemStack(this.get(), count);
    }
}
