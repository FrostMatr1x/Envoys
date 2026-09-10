package com.frost.envoys.event;

import com.frost.envoys.Envoys;
import com.frost.envoys.EnvoysCommand;
import com.frost.envoys.config.NPCConfigManager;
import com.frost.envoys.npc.merchant.TradeCounterStore;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber(modid = Envoys.MODID)
public class ServerEvents {
        
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        EnvoysCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        NPCConfigManager.loadAll();
        TradeCounterStore.loadAll();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        NPCConfigManager.saveAll();
        TradeCounterStore.saveAll();
    }
}