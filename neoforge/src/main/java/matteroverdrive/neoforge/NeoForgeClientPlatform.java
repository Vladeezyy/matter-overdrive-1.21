package matteroverdrive.neoforge;

import java.util.HashMap;
import java.util.Map;

import matteroverdrive.client.ClientPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/** The client on NeoForge */
public final class NeoForgeClientPlatform implements ClientPlatform {
    private static final Map<ResourceLocation, StandaloneModelKey<BlockStateModel>> KEYS = new HashMap<>();

    /** The standalone model key of an extra block model (NeoForge 21.5+ bakes standalone models by key) */
    static StandaloneModelKey<BlockStateModel> key(ResourceLocation id) {
        return KEYS.computeIfAbsent(id, StandaloneModelKey::new);
    }

    @Override
    public BlockStateModel standaloneModel(ResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(key(id));
    }
}
