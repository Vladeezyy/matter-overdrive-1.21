package matteroverdrive.world.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import matteroverdrive.compat.CompatBlockEntity;
import matteroverdrive.init.MOFeatures;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * {@code matteroverdrive:copy_components}: puts what the broken machine carries as an item (energy, matter, owner,
 * transporter destinations) on the dropped stack; the 1.20.1 form of {@code minecraft:copy_components} from the block
 * entity, which 1.20.1 does not have.
 */
public class CopyComponentsFunction extends LootItemConditionalFunction {
    protected CopyComponentsFunction(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (context.getParamOrNull(LootContextParams.BLOCK_ENTITY) instanceof CompatBlockEntity entity) {
            entity.collectComponents().applyTo(stack);
        }
        return stack;
    }

    @Override
    public LootItemFunctionType getType() {
        return MOFeatures.COPY_COMPONENTS.get();
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<CopyComponentsFunction> {
        @Override
        public CopyComponentsFunction deserialize(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions) {
            return new CopyComponentsFunction(conditions);
        }
    }
}
