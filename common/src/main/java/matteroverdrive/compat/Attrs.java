package matteroverdrive.compat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** 1.21 attribute modifiers are named by a ResourceLocation; 1.20.1 ones by a UUID: the UUID is derived from the id. */
public final class Attrs {
    private Attrs() {}

    public static UUID uuid(ResourceLocation id) {
        return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static AttributeModifier modifier(ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(uuid(id), id.toString(), amount, operation);
    }

    public static @Nullable AttributeModifier get(AttributeInstance attribute, ResourceLocation id) {
        return attribute.getModifier(uuid(id));
    }

    public static boolean has(AttributeInstance attribute, ResourceLocation id) {
        return attribute.getModifier(uuid(id)) != null;
    }

    public static void remove(AttributeInstance attribute, ResourceLocation id) {
        attribute.removeModifier(uuid(id));
    }

    public static void remove(AttributeInstance attribute, UUID id) {
        attribute.removeModifier(id);
    }

    /** 1.20.1 AttributeModifier.equals compares only the id */
    public static boolean same(AttributeModifier a, @Nullable AttributeModifier b) {
        return b != null && a.getId().equals(b.getId()) && a.getAmount() == b.getAmount() && a.getOperation() == b.getOperation();
    }

    public static void addOrUpdate(AttributeInstance attribute, AttributeModifier modifier) {
        attribute.removeModifier(modifier.getId());
        attribute.addTransientModifier(modifier);
    }
}
