package matteroverdrive.fabric.client;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.client.MOClient;
import matteroverdrive.client.PhaserBeamRenderer;
import matteroverdrive.client.TritaniumArmorModel;
import matteroverdrive.client.android.AndroidHud;
import matteroverdrive.client.android.ShieldRenderer;
import matteroverdrive.client.quest.QuestHud;
import matteroverdrive.fabric.FabricClientHooks;
import matteroverdrive.init.MOFluids;
import matteroverdrive.init.MOItems;
import matteroverdrive.platform.Platform;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** The client on Fabric: Fabric API's client callbacks (and the client mixins) to the common client code */
public final class MatterOverdriveFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientHooks.registerNetworking();
        MOClient.init();
        FabricObjModels.register();
        ModelLoadingPlugin.register(context -> MOClient.extraModels().forEach(id -> context.addModel(FabricClientPlatform.key(id),
                net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel.blockStateModel(id))));
        MOClient.blockColors((tints, block) -> net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(tints, block));
        // the star map screen's hologram (1.21.6+ GUI: a picture-in-picture renderer)
        net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry.register(context ->
                new matteroverdrive.client.starmap.StarMapPipRenderer());
        // 1.21.4+: the select properties of the client item definitions (vanilla registers its own the same way)
        MOClient.registerSelectProperties(net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties.ID_MAPPER::put);
        MOClient.registerScreens(new MOClient.Screens() {
            @Override
            public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type, MOClient.ScreenFactory<M, U> factory) {
                MenuScreens.register(type, factory::create);
            }
        });
        MOClient.registerRenderers(new MOClient.Renderers() {
            @Override
            public <E extends Entity> void entity(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                EntityRendererRegistry.register(type, provider);
            }

            @Override
            public <T extends BlockEntity, S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> void blockEntity(
                    BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                BlockEntityRenderers.register(type, provider);
            }
        });
        MOClient.registerLayers((location, definition) -> ModelLayerRegistry.registerModelLayer(location, definition::get));
        // 1.21.9+: key categories are registered in vanilla's sort order list
        MOClient.registerKeyCategories(category -> net.minecraft.client.KeyMapping.Category.register(category.id()));
        MOClient.registerKeys(KeyMappingHelper::registerKeyMapping);
        // Matter Plasma (1.7.10 matter_plasma_still / _flowing)
        FluidRenderingRegistry.register(MOFluids.MATTER_PLASMA.get(), MOFluids.MATTER_PLASMA_FLOWING.get(), MOClient.matterPlasmaModel());
        // 1.7.10 ModelTritaniumArmor with the layer textures of TritaniumArmorItem.getArmorTexture
        // as vanilla's armor layer and NeoForge: the armor model poses itself from the render state (copying the context
        // model's transforms tilted the armor stands' sleeves and lost the player's leggings)
        ArmorRenderer.register((pose, collector, stack, state, slot, light, contextModel) -> collector.submitModel(
                        TritaniumArmorModel.armorModel(stack), state, pose,
                        net.minecraft.client.renderer.rendertype.RenderTypes.armorCutoutNoCull(TritaniumArmorModel.armorTexture(stack)), light,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, state.outlineColor),
                MOItems.TRITANIUM_HELMET.get(), MOItems.TRITANIUM_CHESTPLATE.get(), MOItems.TRITANIUM_LEGGINGS.get(), MOItems.TRITANIUM_BOOTS.get());

        ClientTickEvents.START_CLIENT_TICK.register(client -> MOClient.onClientTickStart());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MOClient.onClientTickEnd();
            matteroverdrive.client.PerfScene.onTick();
        });
        // PerfScene counts frames (NeoForge: RenderFrameEvent.Post)
        LevelRenderEvents.END_MAIN.register(context -> matteroverdrive.client.PerfScene.onFrame());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MOClient.onLoggedOut());
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> MOClient.onTooltip(stack, lines));
        HudElementRegistry.addLast(MOClient.ANDROID_HUD, AndroidHud::render);
        HudElementRegistry.addLast(MOClient.QUEST_HUD, QuestHud::render);
        // with the level's submits, as NeoForge's SubmitCustomGeometryEvent
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> ShieldRenderer.renderLevel(context.poseStack(), context.submitNodeCollector()));
    }
}
