package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;

/**
 * 1.7.10 EntityRendererFailed*: the vanilla renderers with the failed texture (1.21.1 models still use the 1.7.10
 * 64x32 layouts, so the textures are used as they are).
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
            public ResourceLocation getTextureLocation(Pig pig) {
                return PIG;
            }
        };
    }

    public static CowRenderer cow(EntityRendererProvider.Context context) {
        return new CowRenderer(context) {
            @Override
            public ResourceLocation getTextureLocation(Cow cow) {
                return COW;
            }
        };
    }

    public static ChickenRenderer chicken(EntityRendererProvider.Context context) {
        return new ChickenRenderer(context) {
            @Override
            public ResourceLocation getTextureLocation(Chicken chicken) {
                return CHICKEN;
            }
        };
    }

    public static SheepRenderer sheep(EntityRendererProvider.Context context) {
        return new SheepRenderer(context) {
            @Override
            public ResourceLocation getTextureLocation(Sheep sheep) {
                return SHEEP;
            }
        };
    }

    private FailedAnimalRenderers() {}
}
