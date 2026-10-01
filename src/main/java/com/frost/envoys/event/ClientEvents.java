package com.frost.envoys.event;

import com.frost.envoys.Envoys;
import com.frost.envoys.client.ClientLuaCommands;
import com.frost.envoys.client.gui.DialogLayout;
import com.frost.envoys.client.gui.QuestLayout;
import com.frost.envoys.client.gui.TradeLayout;
import com.frost.envoys.client.overlay.CurrentQuestOverlay;
import com.frost.envoys.client.quest.ClientQuestTracker;
import com.frost.envoys.gui.screen.NPCTradeScreen;
import com.frost.envoys.gui.screen.QuestWallScreen;
import com.frost.envoys.init.ModEntities;
import com.frost.envoys.init.ModMenus;
import com.frost.envoys.npc.entity.NPCModel;
import com.frost.envoys.npc.entity.NPCRenderer;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ClientEvents {

    public static final KeyMapping QUEST_WALL_KEY = new KeyMapping(
        "key.envoys.quest_wall",
        KeyConflictContext.UNIVERSAL,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        "key.categories.envoys");

    @EventBusSubscriber(modid = Envoys.MODID, value = Dist.CLIENT)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(NPCModel.LAYER_LOCATION, () -> NPCModel.createBodyLayer(false));
            event.registerLayerDefinition(NPCModel.LAYER_LOCATION_SLIM, () -> NPCModel.createBodyLayer(true));
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.BASE_NPC.get(), NPCRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.NPC_MERCHANT_MENU.get(), NPCTradeScreen::new);
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(QUEST_WALL_KEY);
        }

        @SubscribeEvent
        public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> {
                TradeLayout.reload(manager);
                DialogLayout.reload(manager);
                QuestLayout.reload(manager);
            });
        }

        @SubscribeEvent
        public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(Envoys.MODID, "current_quest_overlay"),
                new CurrentQuestOverlay());
        }
    }

    @EventBusSubscriber(modid = Envoys.MODID, value = Dist.CLIENT)
    public static class GameBusEvents {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            while (QUEST_WALL_KEY.consumeClick()) {
                if (minecraft.screen == null) {
                    minecraft.setScreen(new QuestWallScreen());
                }
            }
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientQuestTracker.get().clear();
            com.frost.envoys.client.ClientLuaScriptBridge.clear();
            com.frost.envoys.client.gui.backup.ClientBackupManager.flushActive();
        }

        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            ClientLuaCommands.register(event.getDispatcher());
        }
    }
}