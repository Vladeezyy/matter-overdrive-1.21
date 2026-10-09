package matteroverdrive.compat.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** The 1.21.5 build adds the missing lightmap to entity_translucent_emissive here; 1.21.4's is fine. */
public final class RenderTypes {
    private RenderTypes() {}

    public static RenderType entityTranslucentEmissive(ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }
}
