package matteroverdrive.item.weapon;

import matteroverdrive.init.MOSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.7.10 OmniTool: fires 7 damage plasma bolts every 18 ticks for 512 FE (attack key; heat (heat + 4) x 2.7 of 80,
 * accuracy 0.3 + heat / max x 5, range 24, speed 3), and digs while use is held: a beam breaks the block it points at up
 * to 24 blocks away, at destroy speed 8 and able to harvest anything, for up to 240 ticks per use. 1.7.10 charged
 * 28 FE x 0.007 per digging tick, which rounded to nothing, so digging is free; it stops when the tool overheats.
 * Takes colour modules and the damage or fire barrel, no sights.
 */
public class OmniToolItem extends EnergyWeaponItem {
    public static final int RANGE = 24;
    private static final int MAX_USE_TIME = 240;
    /** Server side: the block each player is digging and how far it is. */
    private static final Map<Player, Dig> DIGGING = new WeakHashMap<>();

    private record Dig(BlockPos pos, float progress) {}

    public OmniToolItem(Properties properties) {
        super(properties, RANGE, 18, 7, 512, 80, 3, 0);
    }

    @Override
    protected float baseAccuracy(ItemStack weapon, boolean zoomed) {
        return 0.3f + getHeat(weapon) / getMaxHeat(weapon) * 5;
    }

    @Override
    public boolean supportsSlot(int slot) {
        return slot != WeaponModule.SLOT_SIGHTS;
    }

    @Override
    protected void fire(ServerLevel level, LivingEntity shooter, ItemStack weapon, boolean zoomed) {
        spawnBolt(level, shooter, weapon, getDamage(weapon, shooter), getAccuracy(weapon, shooter, zoomed));
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), MOSounds.LASER_FIRE.get(), net.minecraft.sounds.SoundSource.PLAYERS,
                0.5f + level.getRandom().nextFloat() * 0.2f, 1.2f + level.getRandom().nextFloat() * 0.2f);
        addHeatAfterShot(weapon, level, shooter, (getHeat(weapon) + 4) * 2.7f);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        if (getEnergy(tool) < getEnergyPerShot(tool) && EnergyPackItem.reload(player, tool)) {
            player.getCooldowns().addCooldown(tool.getItem(), 40);
            return net.minecraft.world.InteractionResultHolder.success(player.getItemInHand(hand));
        }
        if (isOverheated(tool)) return net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand));
        player.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_TIME;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 8;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    /** The block the digging beam points at, ignoring fluids and entities. */
    public static BlockHitResult traceBlock(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        return level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(RANGE)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    /** 1.7.10 onUsingTick: block damage grows by the player's relative hardness (through the damage modules) each tick. */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack tool, int remaining) {
        if (!(level instanceof ServerLevel server) || !(user instanceof ServerPlayer player)) return;
        if (isOverheated(tool)) {
            player.stopUsingItem();
            return;
        }
        BlockHitResult hit = traceBlock(level, player);
        Dig dig = DIGGING.get(player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            stop(server, player);
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !level.mayInteract(player, pos) || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) {
            stop(server, player);
            return;
        }
        if (dig == null || !dig.pos().equals(pos)) {
            stop(server, player);
            dig = new Dig(pos, 0);
        }
        float progress = dig.progress() + modifyStat(WeaponStat.DAMAGE, tool, state.getDestroyProgress(player, level, pos));
        if (progress >= 1) {
            player.connection.send(new ClientboundLevelEventPacket(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state), false));
            player.gameMode.destroyBlock(pos);
            server.destroyBlockProgress(player.getId(), pos, -1);
            DIGGING.remove(player);
            return;
        }
        server.destroyBlockProgress(player.getId(), pos, (int) (progress * 10));
        DIGGING.put(player, new Dig(pos, progress));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level instanceof ServerLevel server && entity instanceof Player player) stop(server, player);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel server && entity instanceof Player player) stop(server, player);
        return stack;
    }

    private static void stop(ServerLevel level, Player player) {
        Dig dig = DIGGING.remove(player);
        if (dig != null) level.destroyBlockProgress(player.getId(), dig.pos(), -1);
    }
}
