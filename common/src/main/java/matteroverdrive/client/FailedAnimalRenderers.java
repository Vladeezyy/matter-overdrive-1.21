package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.ChickenVariant;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.CowVariant;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PigVariant;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;

/**
 * 1.7.10 EntityRendererFailed*: the vanilla renderers with the failed texture. Pig, cow and chicken have biome variants
 * now, so the render state gets a fixed variant with the normal model and the failed texture (the 1.7.10 pig and cow
 * textures are padded to the 64x64 layout of today's models by the resource generator).
 */
public final class FailedAnimalRenderers {
    private static final PigVariant PIG = new PigVariant(new ModelAndTexture<>(PigVariant.ModelType.NORMAL, tex("failed_pig")), SpawnPrioritySelectors.EMPTY);
    private static final CowVariant COW = new CowVariant(new ModelAndTexture<>(CowVariant.ModelType.NORMAL, tex("failed_cow")), SpawnPrioritySelectors.EMPTY);
    private static final ChickenVariant CHICKEN = new ChickenVariant(new ModelAndTexture<>(ChickenVariant.ModelType.NORMAL, tex("failed_chicken")),
            SpawnPrioritySelectors.EMPTY);
    private static final ResourceLocation SHEEP = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/entity/failed_sheep.png");

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "entity/" + name);
    }

    public static PigRenderer pig(EntityRendererProvider.Context context) {
        return new PigRenderer(context) {
            @Override
            public void extractRenderState(Pig pig, PigRenderState state, float partialTick) {
                super.extractRenderState(pig, state, partialTick);
                state.variant = PIG;
            }
        };
    }

    public static CowRenderer cow(EntityRendererProvider.Context context) {
        return new CowRenderer(context) {
            @Override
            public void extractRenderState(Cow cow, CowRenderState state, float partialTick) {
                super.extractRenderState(cow, state, partialTick);
                state.variant = COW;
            }
        };
    }

    public static ChickenRenderer chicken(EntityRendererProvider.Context context) {
        return new ChickenRenderer(context) {
            @Override
            public void extractRenderState(Chicken chicken, ChickenRenderState state, float partialTick) {
                super.extractRenderState(chicken, state, partialTick);
                state.variant = CHICKEN;
            }
        };
    }

    public static SheepRenderer sheep(EntityRendererProvider.Context context) {
        return new SheepRenderer(context) {
            @Override
            public ResourceLocation getTextureLocation(SheepRenderState state) {
                return SHEEP;
            }
        };
    }

    private FailedAnimalRenderers() {}
}
