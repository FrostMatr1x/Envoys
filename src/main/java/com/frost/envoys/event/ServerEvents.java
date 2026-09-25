package com.frost.envoys.event;

import com.frost.envoys.Envoys;
import com.frost.envoys.EnvoysCommand;
import com.frost.envoys.config.NPCConfigManager;
import com.frost.envoys.lua.LuaEngineManager;
import com.frost.envoys.npc.merchant.TradeCounterStore;
import com.frost.envoys.quest.PlayerQuestManager;
import com.frost.envoys.util.PathManager;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
        PathManager.initServer(event.getServer());
        NPCConfigManager.loadAll();
        TradeCounterStore.loadAll();
        LuaEngineManager.init();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LuaEngineManager.shutdownAll();
        NPCConfigManager.saveAll();
        TradeCounterStore.saveAll();
        PathManager.clearServer();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerQuestManager.sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerQuestManager.sync(serverPlayer);
        }
    }
}