package com.frost.envoys.neoforge;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.client.ClientLuaCommands;
import com.frost.envoys.client.ClientLuaScriptBridge;
import com.frost.envoys.client.EmoteIntegration;
import com.frost.envoys.client.anim.ClientAnimCommands;
import com.frost.envoys.client.anim.ClientAnimStore;
import com.frost.envoys.client.gui.DialogLayout;
import com.frost.envoys.client.gui.QuestLayout;
import com.frost.envoys.client.gui.TradeLayout;
import com.frost.envoys.client.gui.backup.ClientBackupManager;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class NeoForgeClientEvents {
    public static final KeyMapping QUEST_WALL_KEY = new KeyMapping(
            "key.envoys.quest_wall",
            KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.envoys");

    private NeoForgeClientEvents() {
    }

    public static final class ModBusEvents {
        private ModBusEvents() {
        }

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
                    ResourceLocation.fromNamespaceAndPath(EnvoysConstants.MODID, "current_quest_overlay"),
                    new CurrentQuestOverlay());
        }
    }

    public static final class GameBusEvents {
        private GameBusEvents() {
        }

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
            ClientLuaScriptBridge.clear();
            ClientBackupManager.flushActive();
            EmoteIntegration.clearServerData();
            ClientAnimStore.invalidate();
        }

        @SubscribeEvent
        public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
            ClientAnimStore.scan();
            EmoteIntegration.requestServerList();
        }

        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            ClientLuaCommands.register(event.getDispatcher());
            ClientAnimCommands.register(event.getDispatcher());
        }
    }
}
