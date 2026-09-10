package com.frost.envoys.event;

import com.frost.envoys.Envoys;
import com.frost.envoys.gui.screen.NPCTradeScreen;
import com.frost.envoys.init.ModEntities;
import com.frost.envoys.init.ModMenus;
import com.frost.envoys.npc.entity.NPCModel;
import com.frost.envoys.npc.entity.NPCRenderer;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Envoys.MODID, value = Dist.CLIENT)
public class ClientEvents {

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
}