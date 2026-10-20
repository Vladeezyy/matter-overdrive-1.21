package matteroverdrive.neoforge;

import java.util.HashMap;
import java.util.Map;

import matteroverdrive.client.ClientPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/** The client on NeoForge */
public final class NeoForgeClientPlatform implements ClientPlatform {
    private static final Map<Identifier, StandaloneModelKey<BlockStateModel>> KEYS = new HashMap<>();

    /** The standalone model key of an extra block model (NeoForge 21.5+ bakes standalone models by key) */
    static StandaloneModelKey<BlockStateModel> key(Identifier id) {
        return KEYS.computeIfAbsent(id, k -> new StandaloneModelKey<>(k::toString));
    }

    @Override
    public BlockStateModel standaloneModel(Identifier id) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(key(id));
    }
}
