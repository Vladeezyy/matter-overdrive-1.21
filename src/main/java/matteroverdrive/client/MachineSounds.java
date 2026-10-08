package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.GravitationalAnomalyBlockEntity;
import matteroverdrive.init.MOSounds;
import matteroverdrive.machine.MachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Looping block sounds: a machine's sound plays while it is active (1.7.10 MOTileEntityMachine.manageSound + MachineSound),
 * a gravitational anomaly always blows its wind, louder closer to it (GravitationalAnomalySound).
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class MachineSounds {
    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        MachineBlockEntity.clientSoundTick = MachineSounds::tickMachine;
        GravitationalAnomalyBlockEntity.clientSoundTick = MachineSounds::tickAnomaly;
    }

    private static void tickMachine(MachineBlockEntity machine) {
        if (!machine.isActive()) return;   // a playing sound stops itself
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        if (machine.clientSound instanceof SoundInstance playing && sounds.isActive(playing)) return;
        MachineLoop sound = new MachineLoop(machine);
        machine.clientSound = sound;
        sounds.play(sound);
    }

    private static void tickAnomaly(GravitationalAnomalyBlockEntity anomaly) {
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        if (anomaly.clientSound instanceof SoundInstance playing && sounds.isActive(playing)) return;
        AnomalyWind sound = new AnomalyWind(anomaly);
        anomaly.clientSound = sound;
        sounds.play(sound);
    }

    private abstract static class BlockLoop extends AbstractTickableSoundInstance {
        protected final BlockEntity block;

        BlockLoop(net.minecraft.sounds.SoundEvent event, BlockEntity block) {
            super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.block = block;
            this.looping = true;
            this.delay = 0;
            Vec3 c = block.getBlockPos().getCenter();
            this.x = c.x;
            this.y = c.y;
            this.z = c.z;
        }

        /** The volume follows the block, so a sound may start (or dip) at zero. */
        @Override
        public boolean canStartSilent() {
            return true;
        }

        protected boolean gone() {
            return block.isRemoved() || block.getLevel() != Minecraft.getInstance().level;
        }
    }

    private static final class MachineLoop extends BlockLoop {
        MachineLoop(MachineBlockEntity machine) {
            super(machine.getLoopSound(), machine);
            this.volume = Math.max(0, machine.getLoopVolume());
        }

        @Override
        public void tick() {
            MachineBlockEntity machine = (MachineBlockEntity) block;
            if (gone() || !machine.isActive()) {
                stop();
                return;
            }
            volume = Math.max(0, machine.getLoopVolume());
        }
    }

    /** No attenuation; full volume at the core, fading to silence at the anomaly's max range. */
    private static final class AnomalyWind extends BlockLoop {
        AnomalyWind(GravitationalAnomalyBlockEntity anomaly) {
            super(MOSounds.WINDY.get(), anomaly);
            this.attenuation = Attenuation.NONE;
            tick();
        }

        @Override
        public void tick() {
            if (gone()) {
                stop();
                return;
            }
            var player = Minecraft.getInstance().player;
            double range = ((GravitationalAnomalyBlockEntity) block).getMaxRange();
            double distance = player == null ? range : player.position().distanceTo(new Vec3(x, y, z));
            volume = range > 0 ? (float) Math.max(0, 1 - distance / range) : 0;
        }
    }

    private MachineSounds() {}
}
