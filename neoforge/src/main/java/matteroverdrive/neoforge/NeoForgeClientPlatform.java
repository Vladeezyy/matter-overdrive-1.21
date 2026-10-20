package matteroverdrive.neoforge;

import matteroverdrive.client.ClientPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

/** The client on NeoForge */
public final class NeoForgeClientPlatform implements ClientPlatform {
    @Override
    public BakedModel standaloneModel(ResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(id);
    }
}
