package matteroverdrive.fabric.client;

import java.util.HashMap;
import java.util.Map;

import matteroverdrive.client.ClientPlatform;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;

/** The client on Fabric */
public final class FabricClientPlatform implements ClientPlatform {
    private static final Map<Identifier, ExtraModelKey<BlockStateModel>> KEYS = new HashMap<>();

    /** The extra model key of an extra block model (1.21.5+: extra models are baked by key) */
    static ExtraModelKey<BlockStateModel> key(Identifier id) {
        return KEYS.computeIfAbsent(id, k -> ExtraModelKey.create(k::toString));
    }

    @Override
    public BlockStateModel standaloneModel(Identifier id) {
        return Minecraft.getInstance().getModelManager().getModel(key(id));
    }
}
