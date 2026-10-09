package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.GravitationalAnomalyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 1.7.10 TileEntityGravitationalAnomaly.spawnParticles + GravitationalAnomalyParticle: every tick a dark grey speck
 * appears on the sphere of the anomaly's break range and is pulled into the core (a tenth of the way per tick),
 * shrinking through the generic sprites 7 to 0.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class AnomalyParticles {
    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        GravitationalAnomalyBlockEntity.clientParticles = AnomalyParticles::spawn;
    }

    private static void spawn(GravitationalAnomalyBlockEntity anomaly) {
        if (!(anomaly.getLevel() instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();
        // MOMathHelper.randomSpherePoint
        double radius = anomaly.getBlockBreakRange();
        double theta = 2 * Math.PI * rand.nextDouble(), phi = Math.acos(2 * rand.nextDouble() - 1);
        Vec3 centre = anomaly.getBlockPos().getCenter();
        Vec3 at = centre.add(radius * Math.sin(phi) * Math.cos(theta), radius * Math.sin(phi) * Math.sin(theta), radius * Math.cos(phi));
        Minecraft.getInstance().particleEngine.add(new Speck(level, at, centre));
    }

    private static final class Speck extends TextureSheetParticle {
        private final Vec3 centre;
        private final float baseSize;

        Speck(ClientLevel level, Vec3 at, Vec3 centre) {
            super(level, at.x, at.y, at.z, 0, 0, 0);
            setSprite(sprite(7));
            this.centre = centre;
            float grey = (float) (Math.random() * 0.3);
            setColor(grey, grey, grey);
            this.quadSize *= 0.75f;
            this.baseSize = quadSize;
            this.lifetime = (int) (8 / (Math.random() * 0.8 + 0.2));
            this.hasPhysics = false;    // 1.7.10 noClip
            this.gravity = 0;
            this.friction = 1;
        }

        private static TextureAtlasSprite sprite(int index) {
            // before 1.21.9 the particle atlas belongs to the particle engine
            var atlas = (net.minecraft.client.renderer.texture.TextureAtlas) Minecraft.getInstance().getTextureManager()
                    .getTexture(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_PARTICLES);
            return atlas.getSprite(ResourceLocation.withDefaultNamespace("generic_" + index));
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }

        /** 1.7.10 renderParticle: full size after the first 32nd of its life. */
        @Override
        public float getQuadSize(float partialTick) {
            return baseSize * Mth.clamp((age + partialTick) / lifetime * 32, 0, 1);
        }

        /** 1.7.10 onUpdate: move, then head for the centre at a tenth of the distance per tick. */
        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            setSprite(sprite(Math.max(0, 7 - age * 8 / lifetime)));
            move(xd, yd, zd);
            xd = (centre.x - x) * 0.1;
            yd = (centre.y - y) * 0.1;
            zd = (centre.z - z) * 0.1;
        }
    }

    private AnomalyParticles() {}
}
