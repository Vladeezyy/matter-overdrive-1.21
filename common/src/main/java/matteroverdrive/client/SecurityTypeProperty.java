package matteroverdrive.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import matteroverdrive.init.MODataComponents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Item model select property {@code matteroverdrive:security_type}: the security protocol's type (1.21.4 has no
 * {@code minecraft:component} select property; tools/backport.py rewrites the item definition to this one).
 */
public record SecurityTypeProperty() implements SelectItemModelProperty<Integer> {
    public static final SelectItemModelProperty.Type<SecurityTypeProperty, Integer> TYPE =
            SelectItemModelProperty.Type.create(MapCodec.unit(new SecurityTypeProperty()), Codec.INT);

    @Override
    public @Nullable Integer get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        return stack.get(MODataComponents.SECURITY_TYPE.get());
    }

    @Override
    public SelectItemModelProperty.Type<SecurityTypeProperty, Integer> type() {
        return TYPE;
    }
}
