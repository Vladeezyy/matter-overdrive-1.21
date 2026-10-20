package matteroverdrive.client;

import java.util.ServiceLoader;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * What the client code needs from the mod loader (only loaded on a physical client). Each loader module lists its
 * implementation in {@code META-INF/services/matteroverdrive.client.ClientPlatform}.
 */
public interface ClientPlatform {
    ClientPlatform INSTANCE = ServiceLoader.load(ClientPlatform.class, ClientPlatform.class.getClassLoader()).findFirst()
            .orElseThrow(() -> new IllegalStateException("No Matter Overdrive client platform for this mod loader"));

    /** An extra block model registered with {@link MOClient#extraModels()} (NeoForge "standalone" models, Fabric extra models) */
    BakedModel standaloneModel(ResourceLocation id);
}
