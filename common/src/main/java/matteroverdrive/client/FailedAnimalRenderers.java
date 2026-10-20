package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.7.10 EntityRendererFailed*: the vanilla renderers with the failed texture (1.21.4: no animal variants yet, the
 * texture is swapped directly; the 1.7.10 pig and cow textures keep their 64x32 layout).
 */
public final class FailedAnimalRenderers {
    private static final ResourceLocation PIG = tex("failed_pig");
    private static final ResourceLocation COW = tex("failed_cow");
    private static final ResourceLocation CHICKEN = tex("failed_chicken");
    private static final ResourceLocation SHEEP = tex("failed_sheep");

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/entity/" + name + ".png");
    }

    public static PigRenderer pig(EntityRendererProvider.Context context) {
        return new PigRenderer(context) {
            @Override
            public ResourceLocation getTextureLocation(PigRenderState state) {
                return PIG;
            }
        };
    }

    public static CowRenderer cow(EntityRendererProvider.Context context) {
        return new CowRenderer(context) {
            @Override
            public ResourceLocation getTextureLocation(LivingEntityRenderState state) {
                return COW;
            }
        };
    }

    public static ChickenRenderer chicken(EntityRendererProvider.Context context) {
        return new ChickenRenderer(context) {
            @Override
            public ResourceLocation getTextureLocation(ChickenRenderState state) {
                return CHICKEN;
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
