package matteroverdrive.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.InscriberBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOItems;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.menu.MachineMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * A scripted test scene for development: {@code ./gradlew runScene} joins the {@code mo_scene} world, builds the
 * machines in front of the player, opens every GUI page and saves screenshots to {@code run/screenshots/scene_*.png},
 * then quits. Inert unless the {@code matteroverdrive.scene} system property is set.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class DevScene {
    private static final boolean ENABLED = System.getProperty("matteroverdrive.scene") != null;
    private static final List<Step> STEPS = new ArrayList<>();
    private static int tick;
    private static BlockPos inscriberPos, solarPos;

    private record Step(int at, Consumer<Minecraft> action) {}

    static {
        at(40, mc -> server(mc, DevScene::build));
        at(60, mc -> MatterOverdrive.LOGGER.info("[scene] client matter: iron_block={} cake={}",
                matteroverdrive.matter.MatterRegistry.getClient(Items.IRON_BLOCK), matteroverdrive.matter.MatterRegistry.getClient(Items.CAKE)));
        at(100, mc -> shot(mc, "world"));
        at(105, mc -> openMachine(mc, inscriberPos));
        at(125, mc -> shot(mc, "inscriber_home"));
        at(130, mc -> page(mc, MachineMenu.Page.UPGRADES));
        at(140, mc -> shot(mc, "inscriber_upgrades"));
        at(145, mc -> page(mc, MachineMenu.Page.CONFIG));
        at(155, mc -> shot(mc, "inscriber_config"));
        at(160, mc -> mc.player.closeContainer());
        at(165, mc -> openMachine(mc, solarPos));
        at(185, mc -> shot(mc, "solar_home"));
        at(190, mc -> mc.player.closeContainer());
        at(200, mc -> mc.stop());
    }

    private static void at(int t, Consumer<Minecraft> action) {
        STEPS.add(new Step(t, action));
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        // The window is usually unfocused while an agent runs this: never pause, or the integrated server stops.
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
            mc.setScreen(null);
        }
        tick++;
        for (Step step : STEPS) {
            if (step.at() == tick) {
                MatterOverdrive.LOGGER.info("[scene] tick {}", tick);
                step.action().accept(mc);
            }
        }
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(mc.player.getUUID())));
    }

    /** Machines on a stone floor north of the player, the player facing them; noon, clear sky. */
    private static void build(ServerPlayer player) {
        ServerLevel level = player.level();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        player.setGameMode(GameType.CREATIVE);
        BlockPos base = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-3, -1, -6), base.offset(3, -1, 0))) {
            level.setBlockAndUpdate(p, Blocks.SMOOTH_STONE.defaultBlockState());
        }
        // Clear the previous run's scene without machine side effects (they would drop their contents),
        // then remove any items already lying around.
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-3, 0, -6), base.offset(3, 4, 0))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        }
        level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(8)).forEach(e -> e.discard());
        inscriberPos = base.offset(-1, 0, -3);
        solarPos = base.offset(1, 0, -3);
        level.setBlockAndUpdate(inscriberPos, MOBlocks.INSCRIBER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(solarPos, MOBlocks.SOLAR_PANEL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(base.offset(0, 0, -5), MOBlocks.TRITANIUM_ORE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(-2, 0, -5), MOBlocks.DILITHIUM_ORE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(2, 0, -5), MOBlocks.TRITANIUM_BLOCK.get().defaultBlockState());

        InscriberBlockEntity inscriber = (InscriberBlockEntity) level.getBlockEntity(inscriberPos);
        inscriber.getEnergy().set(300000);
        inscriber.getInventory().setStack(InscriberBlockEntity.MAIN, new ItemStack(MOItems.ISOLINEAR_CIRCUIT_MK1.get(), 8));
        inscriber.getInventory().setStack(InscriberBlockEntity.SECONDARY, new ItemStack(Items.GOLD_INGOT, 8));
        inscriber.getInventory().setStack(inscriber.getBatterySlot(), MOItems.BATTERY.get().charged());
        for (int i = 0; i < inscriber.getInventory().size(); i++) {
            if (inscriber.getInventory().spec(i).role() == matteroverdrive.machine.MachineInventory.Role.UPGRADE) {
                inscriber.getInventory().setStack(i, new ItemStack(MOItems.UPGRADE_SPEED.get()));
                break;
            }
        }

        var inv = player.getInventory();
        inv.clearContent();
        ItemStack[] hotbar = {MOItems.BATTERY.get().charged(), new ItemStack(MOItems.BATTERY.get()), MOItems.HC_BATTERY.get().charged(),
                new ItemStack(MOItems.CREATIVE_BATTERY.get()), new ItemStack(MOItems.UPGRADE_SPEED.get()),
                new ItemStack(MOItems.UPGRADE_HYPER_SPEED.get()), new ItemStack(MOItems.TRITANIUM_WRENCH.get()),
                new ItemStack(MOItems.INSCRIBER.get()), new ItemStack(MOItems.SOLAR_PANEL.get())};
        for (int i = 0; i < hotbar.length; i++) inv.setItem(i, hotbar[i]);
        for (int i = 0; i < 8; i++) inv.setItem(9 + i, new ItemStack(MOItems.TAB_ORDER.get(i).get()));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(MOItems.TRITANIUM_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(MOItems.TRITANIUM_CHESTPLATE.get()));
        inv.setSelectedSlot(6);
        player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 35f, false);
    }

    private static void openMachine(Minecraft mc, BlockPos pos) {
        server(mc, player -> {
            if (player.level().getBlockEntity(pos) instanceof MachineBlockEntity machine) {
                player.openMenu(machine, pos);
            }
        });
    }

    private static void page(Minecraft mc, MachineMenu.Page page) {
        if (mc.screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof MachineMenu<?> menu) {
            menu.page = page;
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "scene_" + name + ".png", mc.getMainRenderTarget(), 1,
                msg -> MatterOverdrive.LOGGER.info("[scene] {}", msg.getString()));
    }

    private DevScene() {}
}
