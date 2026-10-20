package matteroverdrive.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import matteroverdrive.item.weapon.EnergyWeaponItem;
import matteroverdrive.item.weapon.WeaponBarrelItem;
import matteroverdrive.item.weapon.WeaponModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Item model select property {@code matteroverdrive:barrel}: the installed barrel module's type ("damage", "fire", ...). */
public record BarrelProperty() implements SelectItemModelProperty<String> {
    public static final SelectItemModelProperty.Type<BarrelProperty, String> TYPE =
            SelectItemModelProperty.Type.create(MapCodec.unit(new BarrelProperty()), Codec.STRING);

    @Override
    public @Nullable String get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        return EnergyWeaponItem.getModule(stack, WeaponModule.SLOT_BARREL).getItem() instanceof WeaponBarrelItem barrel ? barrel.getType().id() : null;
    }

    @Override
    public SelectItemModelProperty.Type<BarrelProperty, String> type() {
        return TYPE;
    }
}
