package matteroverdrive.fabric.client;

import matteroverdrive.client.ClientPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/** The client on Fabric */
public final class FabricClientPlatform implements ClientPlatform {
    @Override
    public BakedModel standaloneModel(ResourceLocation id) {
        // Fabric's extra models (ModelLoadingPlugin.addModels) by their plain id
        return Minecraft.getInstance().getModelManager().getModel(id);
    }
}
