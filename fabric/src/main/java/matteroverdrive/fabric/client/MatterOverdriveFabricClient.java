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
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
        ModelLoadingPlugin.register(context -> context.addModels(MOClient.extraModels()));
        MOClient.blockColors(ColorProviderRegistry.BLOCK::register);
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
            public <T extends BlockEntity> void blockEntity(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> provider) {
                BlockEntityRenderers.register(type, provider);
            }
        });
        MOClient.registerLayers((location, definition) -> EntityModelLayerRegistry.registerModelLayer(location, definition::get));
        MOClient.registerKeys(KeyBindingHelper::registerKeyBinding);
        registerRenderLayers();
        // Matter Plasma (1.7.10 matter_plasma_still / _flowing)
        FluidRenderHandlerRegistry.INSTANCE.register(MOFluids.MATTER_PLASMA.get(), MOFluids.MATTER_PLASMA_FLOWING.get(), new SimpleFluidRenderHandler(
                ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "block/matter_plasma_still"),
                ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "block/matter_plasma_flowing")));
        // 1.7.10 ModelTritaniumArmor with the layer textures of TritaniumArmorItem.getArmorTexture
        ArmorRenderer.register((pose, buffers, stack, entity, slot, light, contextModel) -> ArmorRenderer.renderPart(pose, buffers, light, stack,
                        TritaniumArmorModel.armorModel(stack), slot == EquipmentSlot.FEET ? TritaniumArmorModel.LAYER_2 : TritaniumArmorModel.LAYER_1),
                MOItems.TRITANIUM_HELMET.get(), MOItems.TRITANIUM_CHESTPLATE.get(), MOItems.TRITANIUM_LEGGINGS.get(), MOItems.TRITANIUM_BOOTS.get());

        ClientTickEvents.START_CLIENT_TICK.register(client -> MOClient.onClientTickStart());
        ClientTickEvents.END_CLIENT_TICK.register(client -> MOClient.onClientTickEnd());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MOClient.onLoggedOut());
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> MOClient.onTooltip(stack, lines));
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            AndroidHud.render(graphics, delta);
            QuestHud.render(graphics, delta);
        });
        // the camera's rotation is already in RenderSystem's model-view stack at this stage (1.21.1): an identity pose
        WorldRenderEvents.AFTER_ENTITIES.register(context -> ShieldRenderer.renderLevel(new PoseStack()));
    }

    /** NeoForge reads the render type from the block's model ("render_type"); Fabric needs it per block */
    private static void registerRenderLayers() {
        Map<String, RenderType> types = Map.of("minecraft:cutout", RenderType.cutout(), "minecraft:cutout_mipped", RenderType.cutoutMipped(),
                "minecraft:translucent", RenderType.translucent());
        Map<ResourceLocation, RenderType> modelTypes = new HashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(MatterOverdrive.MODID)) continue;
            JsonObject state = json(Platform.INSTANCE.modFile("assets", MatterOverdrive.MODID, "blockstates", id.getPath() + ".json"));
            if (state == null) continue;
            RenderType type = null;
            for (String model : models(state)) {
                ResourceLocation modelId = ResourceLocation.parse(model);
                RenderType modelType = modelTypes.computeIfAbsent(modelId, m -> {
                    JsonObject json = json(Platform.INSTANCE.modFile("assets", m.getNamespace(), "models", m.getPath() + ".json"));
                    return json != null && json.has("render_type") ? types.get(json.get("render_type").getAsString()) : RenderType.solid();
                });
                if (modelType != RenderType.solid()) type = modelType;
            }
            if (type != null) BlockRenderLayerMap.INSTANCE.putBlock(block, type);
        }
        BlockRenderLayerMap.INSTANCE.putFluids(RenderType.translucent(), MOFluids.MATTER_PLASMA.get(), MOFluids.MATTER_PLASMA_FLOWING.get());
    }

    private static java.util.List<String> models(JsonObject state) {
        java.util.List<String> models = new java.util.ArrayList<>();
        java.util.function.Consumer<JsonElement> variant = element -> {
            if (element.isJsonArray()) element.getAsJsonArray().forEach(e -> models.add(e.getAsJsonObject().get("model").getAsString()));
            else models.add(element.getAsJsonObject().get("model").getAsString());
        };
        if (state.has("variants")) state.getAsJsonObject("variants").entrySet().forEach(e -> variant.accept(e.getValue()));
        if (state.has("multipart")) state.getAsJsonArray("multipart").forEach(part -> variant.accept(part.getAsJsonObject().get("apply")));
        return models;
    }

    private static JsonObject json(Path file) {
        if (file == null || !Files.exists(file)) return null;
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            MatterOverdrive.LOGGER.warn("Could not read {}", file, e);
            return null;
        }
    }
}
