package matteroverdrive.client;

import net.minecraft.world.item.ItemStack;
import matteroverdrive.MatterOverdrive;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import matteroverdrive.network.FireWeaponPayload;
import net.minecraft.client.Minecraft;
import matteroverdrive.platform.Platform;

/**
 * Client side of the energy weapons (1.7.10 ClientWeaponHandler): the attack key fires instead of swinging, and
 * holding use zooms by the weapon's zoom factor.
 */
public final class WeaponClient {
    /** The attack key does not attack (nor swing) with a weapon in hand: holding it fires */
    public static boolean cancelsAttack() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getMainHandItem().getItem() instanceof EnergyWeaponItem;
    }

    public static void onClientTick() {
        onTick();
        onHum();
    }
    private static void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.options.keyAttack.isDown()) return;
        if (mc.player.getMainHandItem().getItem() instanceof EnergyWeaponItem weapon
                && !mc.player.getCooldowns().isOnCooldown(mc.player.getMainHandItem().getItem())
                && weapon.canFire(mc.player.getMainHandItem())) {
            Platform.INSTANCE.sendToServer(new FireWeaponPayload(mc.player.isUsingItem()));
        }
    }

    /** 1.7.10 RenderWeaponsBeam.playWeaponSound: the omni tool hums while it digs (one looping sound per player). */
    private static final java.util.Map<net.minecraft.world.entity.player.Player, OmniToolHum> HUMS = new java.util.WeakHashMap<>();
    private static void onHum() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (var player : mc.level.players()) {
            OmniToolHum hum = HUMS.get(player);
            if (OmniToolHum.digging(player) && (hum == null || hum.isStopped())) {
                hum = new OmniToolHum(player);
                HUMS.put(player, hum);
                mc.getSoundManager().play(hum);
            }
        }
    }

    private static final class OmniToolHum extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
        private final net.minecraft.world.entity.player.Player player;

        OmniToolHum(net.minecraft.world.entity.player.Player player) {
            super(matteroverdrive.init.MOSounds.OMNI_TOOL_HUM.get(), net.minecraft.sounds.SoundSource.PLAYERS, player.getRandom());
            this.player = player;
            this.looping = true;
            this.volume = 0.06f + player.getRandom().nextFloat() * 0.04f;
            this.pitch = 0.95f + player.getRandom().nextFloat() * 0.1f;
            tick();
        }

        static boolean digging(net.minecraft.world.entity.player.Player player) {
            return player.isUsingItem() && player.getUseItem().getItem() instanceof matteroverdrive.item.weapon.OmniToolItem;
        }

        @Override
        public void tick() {
            if (player.isRemoved() || !digging(player)) {
                stop();
                return;
            }
            x = player.getX();
            y = player.getY();
            z = player.getZ();
        }
    }
    /** The field of view modifier while zooming with a weapon */
    public static float fovModifier(net.minecraft.world.entity.player.Player player, float modifier) {
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof EnergyWeaponItem weapon && weapon.getZoom(player.getUseItem()) > 0) {
            return modifier * (1 - weapon.getZoom(player.getUseItem()));
        }
        return modifier;
    }

    private WeaponClient() {}

    /** 1.7.10 weapons stayed raised while firing; vanilla drops the hand after every successful use (itemUsed): true if done */
    public static boolean applyHandTransform(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.player.LocalPlayer player,
                                             net.minecraft.world.entity.HumanoidArm arm, ItemStack stack) {
        if (!player.isUsingItem() || player.getUseItem().getItem() != stack.getItem()) return false;
        pose.translate(arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 0.56f : -0.56f, -0.52f, -0.72f);
        return true;
    }

    /** Aim the weapon down the line of sight while firing or zooming, as seen by other players (null: the default pose) */
    public static net.minecraft.client.model.HumanoidModel.@org.jetbrains.annotations.Nullable ArmPose armPose(net.minecraft.world.entity.LivingEntity entity,
                                                                                                            net.minecraft.world.InteractionHand hand) {
        return entity.isUsingItem() && entity.getUsedItemHand() == hand ? net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW : null;
    }

    /** The weapons that keep their own hand pose */
    public static net.minecraft.world.item.Item[] weapons() {
        return new net.minecraft.world.item.Item[] {matteroverdrive.init.MOItems.PHASER.get(), matteroverdrive.init.MOItems.PHASER_RIFLE.get(),
                matteroverdrive.init.MOItems.PLASMA_SHOTGUN.get(), matteroverdrive.init.MOItems.ION_SNIPER.get(), matteroverdrive.init.MOItems.OMNI_TOOL.get()};
    }
}
