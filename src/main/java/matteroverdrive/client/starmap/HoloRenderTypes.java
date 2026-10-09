package matteroverdrive.client.starmap;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The 1.7.10 holograms drew with glBlendFunc(GL_ONE, GL_ONE), no depth writes and no culling: textured quads (beam,
 * star particles, holo icons), coloured quads / triangles (cubes, ships) and lines (orbits, wire spheres). The planet's
 * black core sphere writes depth (and adds nothing) so the far side of its wire sphere is hidden.
 * (1.21.1: render types built from state shards; the 1.21.10 build uses render pipelines.)
 */
public final class HoloRenderTypes {
    private static final RenderStateShard.ShaderStateShard TEX_COLOR = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);
    private static final RenderStateShard.ShaderStateShard COLOR = RenderStateShard.POSITION_COLOR_SHADER;
    /** 1.7.10 block screens: glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR). */
    private static final RenderStateShard.TransparencyStateShard SCREEN_BLEND = new RenderStateShard.TransparencyStateShard("mo_holo_screen", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    private static RenderType.CompositeState.CompositeStateBuilder holo() {
        return RenderType.CompositeState.builder().setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL).setWriteMaskState(RenderStateShard.COLOR_WRITE);
    }

    private static RenderStateShard.TextureStateShard texture(ResourceLocation texture) {
        return new RenderStateShard.TextureStateShard(texture, false, false);
    }

    private static final Map<ResourceLocation, RenderType> SCREEN_TYPES = new HashMap<>();

    public static RenderType screen(ResourceLocation texture) {
        return SCREEN_TYPES.computeIfAbsent(texture, t -> RenderType.create("mo_holo_screen", DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, 1536, false, true, holo().setShaderState(TEX_COLOR).setTransparencyState(SCREEN_BLEND)
                        .setTextureState(texture(t)).createCompositeState(false)));
    }

    private static final Map<ResourceLocation, RenderType> TEXTURED_TYPES = new HashMap<>();
    public static final RenderType COLOR_QUADS = RenderType.create("mo_holo_quads", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
            1536, false, true, holo().setShaderState(COLOR).createCompositeState(false));
    public static final RenderType COLOR_TRIANGLES = RenderType.create("mo_holo_triangles", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES, 1536, false, true, holo().setShaderState(COLOR).createCompositeState(false));
    public static final RenderType DEPTH = RenderType.create("mo_holo_depth", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
            1536, false, false, holo().setShaderState(COLOR).setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE).createCompositeState(false));
    public static final RenderType LINE_TYPE = RenderType.create("mo_holo_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES,
            1536, false, false, RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(1.0)))
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING).setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET).setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL).createCompositeState(false));

    public static RenderType textured(ResourceLocation texture) {
        return TEXTURED_TYPES.computeIfAbsent(texture, t -> RenderType.create("mo_holo_textured", DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, 1536, false, true, holo().setShaderState(TEX_COLOR).setTextureState(texture(t)).createCompositeState(false)));
    }

    private static final Map<ResourceLocation, RenderType> CULLED_TYPES = new HashMap<>();

    /** The textured holo with back faces culled (the android shield seen from outside). */
    public static RenderType texturedCulled(ResourceLocation texture) {
        return CULLED_TYPES.computeIfAbsent(texture, t -> RenderType.create("mo_holo_textured_cull", DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, 1536, false, true, holo().setShaderState(TEX_COLOR).setTextureState(texture(t))
                        .setCullState(RenderStateShard.CULL).createCompositeState(false)));
    }

    private HoloRenderTypes() {}
}
