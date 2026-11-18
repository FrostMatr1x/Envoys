package com.frost.envoys.fabric;

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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.lwjgl.glfw.GLFW;

public final class EnvoysFabricClient implements ClientModInitializer {
    private static final KeyMapping QUEST_WALL_KEY = new KeyMapping(
            "key.envoys.quest_wall",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.envoys");

    private static final CurrentQuestOverlay OVERLAY = new CurrentQuestOverlay();

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(QUEST_WALL_KEY);

        FabricClientNetworkRegistry.registerClientReceivers();

        EntityModelLayerRegistry.registerModelLayer(NPCModel.LAYER_LOCATION, () -> NPCModel.createBodyLayer(false));
        EntityModelLayerRegistry.registerModelLayer(NPCModel.LAYER_LOCATION_SLIM, () -> NPCModel.createBodyLayer(true));
        EntityRendererRegistry.register(ModEntities.BASE_NPC.get(), NPCRenderer::new);

        MenuScreens.register(ModMenus.NPC_MERCHANT_MENU.get(), NPCTradeScreen::new);

        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> OVERLAY.render(guiGraphics, deltaTracker));

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new LayoutReloadListener());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            ClientLuaCommands.register(dispatcher);
            ClientAnimCommands.register(dispatcher);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (QUEST_WALL_KEY.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new QuestWallScreen());
                }
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientAnimStore.scan();
            EmoteIntegration.requestServerList();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientQuestTracker.get().clear();
            ClientLuaScriptBridge.clear();
            ClientBackupManager.flushActive();
            EmoteIntegration.clearServerData();
            ClientAnimStore.invalidate();
        });
    }

    private static final class LayoutReloadListener implements ResourceManagerReloadListener, IdentifiableResourceReloadListener {
        private static final ResourceLocation ID =
                ResourceLocation.fromNamespaceAndPath(EnvoysConstants.MODID, "layouts");

        @Override
        public ResourceLocation getFabricId() {
            return ID;
        }

        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            TradeLayout.reload(resourceManager);
            DialogLayout.reload(resourceManager);
            QuestLayout.reload(resourceManager);
        }
    }
}
