package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MOSounds;
import matteroverdrive.network.BoltHitPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 1.7.10 PlasmaBolt.onHit: a puff in the bolt's colour; 80% of the time sparks (PhaserBoltRecoil) fly off the hit side
 * and a big bolt (render size above 0.5) sizzles, ricocheting off blocks; living targets bleed red dust.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class BoltHitEffects {
    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        BoltHitPayload.onClient = BoltHitEffects::onHit;
    }

    private static void onHit(BoltHitPayload hit) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        RandomSource rand = level.getRandom();
        float r = (hit.color() >> 16 & 255) / 255f, g = (hit.color() >> 8 & 255) / 255f, b = (hit.color() & 255) / 255f;
        Particle puff = mc.particleEngine.createParticle(ParticleTypes.POOF, hit.x(), hit.y(), hit.z(), 0, 0, 0);
        if (puff instanceof SingleQuadParticle quad) quad.setColor(r, g, b);
        int setting = mc.options.particles().get().ordinal();   // 1.7.10 particleSetting: 0 all, 1 decreased, 2 minimal
        if (rand.nextFloat() < 0.8f) {
            int sparks = Math.max(0, (int) (16 * hit.size()) - (int) (8 * hit.size()) * setting);
            for (int i = 0; i < sparks; i++) {
                mc.particleEngine.add(new Spark(level, hit, r, g, b));
            }
            if (hit.size() > 0.5f) {
                play(mc, MOSounds.SIZZLE.get(), hit, rand.nextFloat() * 0.2f + 0.4f, rand.nextFloat() * 0.6f + 0.7f);
                if (hit.kind() == BoltHitPayload.BLOCK) {
                    play(mc, MOSounds.LASER_RICOCHET.get(), hit, rand.nextFloat() * 0.2f + 0.6f, rand.nextFloat() * 0.2f + 1);
                }
            }
        }
        if (hit.kind() == BoltHitPayload.LIVING) {
            for (int i = 0; i < Math.max(0, 10 - 5 * setting); i++) {
                level.addParticle(DustParticleOptions.REDSTONE, hit.x() + rand.nextDouble() * 0.4 - 0.2, hit.y() + rand.nextDouble() * 0.4 - 0.2,
                        hit.z() + rand.nextDouble() * 0.4 - 0.2, 0, 0, 0);
            }
        }
    }

    private static void play(Minecraft mc, SoundEvent sound, BoltHitPayload hit, float volume, float pitch) {
        mc.getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.PLAYERS, volume, pitch,
                net.minecraft.client.resources.sounds.SoundInstance.createUnseededRandom(), hit.x(), hit.y(), hit.z()));
    }

    /** 1.7.10 PhaserBoltRecoil: a full-bright generic spark in the bolt's colour, thrown off the hit side, shrinking. */
    private static final class Spark extends SingleQuadParticle {
        private final float baseSize;

        Spark(ClientLevel level, BoltHitPayload hit, float r, float g, float b) {
            super(level, hit.x(), hit.y(), hit.z(), hit.nx() * 30, hit.ny() * 30, hit.nz() * 30, sprite(level.getRandom().nextInt(2)));
            this.xd += (random.nextFloat() - 0.5f) * 0.2f;
            this.yd += (random.nextFloat() - 0.5f) * 0.2f;
            this.zd += (random.nextFloat() - 0.5f) * 0.2f;
            setColor(r, g, b);
            this.quadSize *= random.nextFloat() * 0.5f + 1;
            this.baseSize = quadSize;
            this.lifetime = (int) (8 / (Math.random() * 0.8 + 0.2));
            this.gravity = 0.75f;      // 1.7.10 motionY -= 0.03
            this.friction = 0.999f;     // the vanilla tick slows it on the ground like 1.7.10
            this.hasPhysics = true;
        }

        private static TextureAtlasSprite sprite(int index) {
            return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES)
                    .getSprite(ResourceLocation.withDefaultNamespace("generic_" + index));
        }

        @Override
        protected Layer getLayer() {
            return Layer.OPAQUE;
        }

        @Override
        public int getLightColor(float partialTick) {
            int light = super.getLightColor(partialTick);
            return 240 | (light >> 16 & 255) << 16;
        }

        @Override
        public float getQuadSize(float partialTick) {
            float f = (age + partialTick) / lifetime;
            return baseSize * (1 - f * f);
        }
    }

    private BoltHitEffects() {}
}
