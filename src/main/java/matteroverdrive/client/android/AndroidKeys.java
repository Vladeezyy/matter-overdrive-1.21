package matteroverdrive.client.android;

import org.lwjgl.glfw.GLFW;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.android.AndroidClientHooks;
import matteroverdrive.android.AndroidData;
import matteroverdrive.android.BioticStat;
import matteroverdrive.android.BioticStats;
import matteroverdrive.network.AndroidPayloads;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 KeyHandler: X uses the selected android ability (and toggles others), Tab holds the ability wheel. Teleport
 * aims while X is held and jumps on release.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class AndroidKeys {
    /** Before 1.21.9 key categories are translation keys (the same one the 1.21.9+ KeyMapping.Category uses). */
    public static final String CATEGORY = "key.category." + MatterOverdrive.MODID + ".android";
    public static final KeyMapping ABILITY_USE = new KeyMapping("key." + MatterOverdrive.MODID + ".ability_use", GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping ABILITY_SWITCH = new KeyMapping("key." + MatterOverdrive.MODID + ".ability_switch", GLFW.GLFW_KEY_TAB, CATEGORY);
    private static boolean teleportHeld;

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ABILITY_USE);
        event.register(ABILITY_SWITCH);
        AndroidClientHooks.abilityKeyName = () -> ABILITY_USE.getTranslatedKeyMessage().getString();
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !Android.isAndroid(player)) {
            teleportHeld = false;
            return;
        }
        AndroidData data = Android.get(player);
        while (ABILITY_USE.consumeClick()) {
            PacketDistributor.sendToServer(new AndroidPayloads.Action());
        }
        // 1.7.10 BioticStatTeleport.manageActivate
        BioticStat teleport = BioticStats.TELEPORT;
        int level = data.getUnlockedLevel(teleport);
        if (level > 0 && teleport.isEnabled(player, data, level) && ABILITY_USE.isDown()) {
            teleportHeld = true;
        } else if (teleportHeld) {
            teleportHeld = false;
            Vec3 target = teleportTarget(player);
            if (target != null) PacketDistributor.sendToServer(new AndroidPayloads.Teleport(target.x, target.y, target.z));
        }
        while (ABILITY_SWITCH.consumeClick()) {
            if (mc.screen == null && AbilityWheelScreen.hasStats(player)) mc.setScreen(new AbilityWheelScreen());
        }
    }

    public static boolean isAimingTeleport() {
        return teleportHeld;
    }

    /** 1.7.10 BioticStatTeleport.getPos: the safe spot on top of the targeted block within 32 blocks, else 6 blocks ahead. */
    public static Vec3 teleportTarget(Player player) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 far = eye.add(player.getViewVector(1).scale(32));
        BlockHitResult hit = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            BlockPos safe = topSafeBlock(level, hit.getBlockPos(), hit.getDirection());
            return safe == null ? null : Vec3.atBottomCenterOf(safe);
        }
        return eye.add(player.getViewVector(1).scale(6));
    }

    /** 1.7.10 getTopSafeBlock: two free blocks above the hit (8 up, 3 when the top face was hit), else beside the face. */
    static BlockPos topSafeBlock(Level level, BlockPos pos, Direction side) {
        int heightCheck = side == Direction.UP ? 3 : 8;
        int air = 0;
        for (int y = pos.getY(); y < Math.min(pos.getY() + heightCheck, level.getMaxY()); y++) {
            BlockPos at = new BlockPos(pos.getX(), y, pos.getZ());
            BlockState state = level.getBlockState(at);
            if (state.is(Blocks.SOUL_SAND) || state.is(Blocks.BARRIER) || state.is(Blocks.BEDROCK)) return null;
            if (!state.isSolid() || !state.getFluidState().isEmpty()) {
                air++;
            } else {
                air = 0;
            }
            if (air >= 2) return at.below();
        }
        BlockPos beside = pos.relative(side);
        if (!level.getBlockState(beside.above()).isSolid() && !level.getBlockState(beside.above(2)).isSolid()) return beside;
        return null;
    }

    private AndroidKeys() {}
}
