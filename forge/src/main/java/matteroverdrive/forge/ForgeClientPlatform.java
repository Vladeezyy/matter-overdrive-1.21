package matteroverdrive.forge;

import matteroverdrive.client.ClientPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/** The client on Forge */
public final class ForgeClientPlatform implements ClientPlatform {
    @Override
    public BakedModel standaloneModel(ResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getModel(id);
    }
}
