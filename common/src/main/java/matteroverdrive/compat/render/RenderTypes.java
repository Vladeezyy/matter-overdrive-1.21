package matteroverdrive.compat.render;

import java.util.function.Function;

import net.minecraft.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

/**
 * 1.21.5's entity_translucent_emissive has no lightmap state although its pipeline samples the lightmap (Sampler2),
 * so drawing it on its own crashes ("Missing sampler Sampler2"). The same render type with the lightmap.
 */
public final class RenderTypes {
    private static final Function<ResourceLocation, RenderType> TRANSLUCENT_EMISSIVE = Util.memoize(tex -> RenderType.create(
            "mo_entity_translucent_emissive", 1536, true, true, RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE,
            RenderType.CompositeState.builder().setTextureState(new RenderStateShard.TextureStateShard(tex, TriState.FALSE, false))
                    .setLightmapState(RenderStateShard.LIGHTMAP).setOverlayState(RenderStateShard.OVERLAY).createCompositeState(true)));

    private RenderTypes() {}

    public static RenderType entityTranslucentEmissive(ResourceLocation texture) {
        return TRANSLUCENT_EMISSIVE.apply(texture);
    }
}
