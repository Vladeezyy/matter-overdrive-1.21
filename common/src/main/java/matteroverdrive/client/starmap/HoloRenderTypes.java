package matteroverdrive.client.starmap;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.PrimitiveTopology;

import matteroverdrive.MatterOverdrive;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * The 1.7.10 holograms drew with glBlendFunc(GL_ONE, GL_ONE), no depth writes and no culling: textured quads (beam,
 * star particles, holo icons), coloured quads / triangles (cubes, ships) and lines (orbits, wire spheres). The planet's
 * black core sphere writes depth (and adds nothing) so the far side of its wire sphere is hidden.
 */
public final class HoloRenderTypes {
    private static final DepthStencilState NO_DEPTH_WRITE = new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false);

    private static RenderPipeline.Builder base(String name) {
        return RenderPipeline.builder().withBindGroupLayout(BindGroupLayouts.GLOBALS).withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).withLocation(Identifier.fromNamespaceAndPath(MatterOverdrive.MODID,
                "pipeline/" + name)).withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE)).withCull(false)
                .withDepthStencilState(NO_DEPTH_WRITE);
    }

    public static final RenderPipeline TEXTURED = base("holo_textured").withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color").withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS).build();
    /** The textured holo with back faces culled (the android shield seen from outside). */
    public static final RenderPipeline TEXTURED_CULL = base("holo_textured_cull").withCull(true).withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color").withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS).build();
    public static final RenderPipeline QUADS = base("holo_quads").withVertexShader("core/position_color").withFragmentShader("core/position_color")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS).build();
    public static final RenderPipeline TRIANGLES = base("holo_triangles").withVertexShader("core/position_color")
            .withFragmentShader("core/position_color").withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build();
    public static final RenderPipeline DEPTH_QUADS = base("holo_depth_quads").withVertexShader("core/position_color")
            .withFragmentShader("core/position_color").withDepthStencilState(DepthStencilState.DEFAULT)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS).build();
    public static final RenderPipeline LINES = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MatterOverdrive.MODID, "pipeline/holo_lines"))
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE)).withDepthStencilState(NO_DEPTH_WRITE).build();

    /** 1.7.10 block screens: glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR). */
    public static final RenderPipeline SCREEN = base("holo_screen").withColorTargetState(new ColorTargetState(new BlendFunction(
                    com.mojang.blaze3d.platform.BlendFactor.ONE, com.mojang.blaze3d.platform.BlendFactor.ONE_MINUS_SRC_COLOR)))
            .withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color").withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS).build();
    private static final Map<Identifier, RenderType> SCREEN_TYPES = new HashMap<>();

    public static RenderType screen(Identifier texture) {
        return SCREEN_TYPES.computeIfAbsent(texture, t -> RenderType.create("mo_holo_screen", RenderSetup.builder(SCREEN).withTexture("Sampler0", t).sortOnUpload().createRenderSetup()));
    }

    /** The mod's pipelines (NeoForge's RegisterRenderPipelinesEvent; vanilla compiles unregistered ones on first use) */
    public static void registerPipelines(java.util.function.Consumer<RenderPipeline> event) {
        for (RenderPipeline pipeline : new RenderPipeline[] {SCREEN, TEXTURED, TEXTURED_CULL, QUADS, TRIANGLES, DEPTH_QUADS, LINES}) event.accept(pipeline);
    }

    private static final Map<Identifier, RenderType> TEXTURED_TYPES = new HashMap<>();
    public static final RenderType COLOR_QUADS = RenderType.create("mo_holo_quads", RenderSetup.builder(QUADS).sortOnUpload().createRenderSetup());
    public static final RenderType COLOR_TRIANGLES = RenderType.create("mo_holo_triangles", RenderSetup.builder(TRIANGLES).sortOnUpload().createRenderSetup());
    public static final RenderType DEPTH = RenderType.create("mo_holo_depth", RenderSetup.builder(DEPTH_QUADS).createRenderSetup());
    public static final RenderType LINE_TYPE = RenderType.create("mo_holo_lines", RenderSetup.builder(LINES).createRenderSetup());

    public static RenderType textured(Identifier texture) {
        return TEXTURED_TYPES.computeIfAbsent(texture, t -> RenderType.create("mo_holo_textured", RenderSetup.builder(TEXTURED).withTexture("Sampler0", t).sortOnUpload().createRenderSetup()));
    }

    private static final Map<Identifier, RenderType> CULLED_TYPES = new HashMap<>();

    public static RenderType texturedCulled(Identifier texture) {
        return CULLED_TYPES.computeIfAbsent(texture, t -> RenderType.create("mo_holo_textured_cull", RenderSetup.builder(TEXTURED_CULL).withTexture("Sampler0", t).sortOnUpload().createRenderSetup()));
    }

    private HoloRenderTypes() {}
}
