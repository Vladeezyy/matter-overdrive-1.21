package matteroverdrive.client.starmap;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import matteroverdrive.MatterOverdrive;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/**
 * The 1.7.10 holograms drew with glBlendFunc(GL_ONE, GL_ONE), no depth writes and no culling: textured quads (beam,
 * star particles, holo icons), coloured quads / triangles (cubes, ships) and lines (orbits, wire spheres). The planet's
 * black core sphere writes depth (and adds nothing) so the far side of its wire sphere is hidden.
 */
public final class HoloRenderTypes {
    private static RenderPipeline.Builder base(String name) {
        return RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET).withLocation(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID,
                "pipeline/" + name)).withBlend(BlendFunction.ADDITIVE).withCull(false).withDepthWrite(false);
    }

    public static final RenderPipeline TEXTURED = base("holo_textured").withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color").withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS).build();
    public static final RenderPipeline QUADS = base("holo_quads").withVertexShader("core/position_color").withFragmentShader("core/position_color")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS).build();
    public static final RenderPipeline TRIANGLES = base("holo_triangles").withVertexShader("core/position_color")
            .withFragmentShader("core/position_color").withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES).build();
    public static final RenderPipeline DEPTH_QUADS = base("holo_depth_quads").withVertexShader("core/position_color")
            .withFragmentShader("core/position_color").withDepthWrite(true)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS).build();
    public static final RenderPipeline LINES = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "pipeline/holo_lines"))
            .withBlend(BlendFunction.ADDITIVE).withDepthWrite(false).build();

    /** 1.7.10 block screens: glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR). */
    public static final RenderPipeline SCREEN = base("holo_screen").withBlend(new BlendFunction(
                    com.mojang.blaze3d.platform.SourceFactor.ONE, com.mojang.blaze3d.platform.DestFactor.ONE_MINUS_SRC_COLOR))
            .withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color").withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS).build();
    private static final Map<ResourceLocation, RenderType> SCREEN_TYPES = new HashMap<>();

    public static RenderType screen(ResourceLocation texture) {
        return SCREEN_TYPES.computeIfAbsent(texture, t -> RenderType.create("mo_holo_screen", 1536, false, true, SCREEN,
                RenderType.CompositeState.builder().setTextureState(new RenderStateShard.TextureStateShard(t, false)).createCompositeState(false)));
    }

    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SCREEN);
        event.registerPipeline(TEXTURED);
        event.registerPipeline(QUADS);
        event.registerPipeline(TRIANGLES);
        event.registerPipeline(DEPTH_QUADS);
        event.registerPipeline(LINES);
    }

    private static final Map<ResourceLocation, RenderType> TEXTURED_TYPES = new HashMap<>();
    public static final RenderType COLOR_QUADS = RenderType.create("mo_holo_quads", 1536, false, true, QUADS,
            RenderType.CompositeState.builder().createCompositeState(false));
    public static final RenderType COLOR_TRIANGLES = RenderType.create("mo_holo_triangles", 1536, false, true, TRIANGLES,
            RenderType.CompositeState.builder().createCompositeState(false));
    public static final RenderType DEPTH = RenderType.create("mo_holo_depth", 1536, false, false, DEPTH_QUADS,
            RenderType.CompositeState.builder().createCompositeState(false));
    public static final RenderType LINE_TYPE = RenderType.create("mo_holo_lines", 1536, false, false, LINES,
            RenderType.CompositeState.builder().setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(1.0))).createCompositeState(false));

    public static RenderType textured(ResourceLocation texture) {
        return TEXTURED_TYPES.computeIfAbsent(texture, t -> RenderType.create("mo_holo_textured", 1536, false, true, TEXTURED,
                RenderType.CompositeState.builder().setTextureState(new RenderStateShard.TextureStateShard(t, false)).createCompositeState(false)));
    }

    private HoloRenderTypes() {}
}
