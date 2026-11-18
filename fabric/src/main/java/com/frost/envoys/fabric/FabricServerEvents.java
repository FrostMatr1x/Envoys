package com.frost.envoys.fabric;


import com.frost.envoys.EnvoysCommand;
import com.frost.envoys.config.NPCConfigManager;
import com.frost.envoys.lua.LuaEngineManager;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.npc.merchant.TradeCounterStore;
import com.frost.envoys.quest.PlayerQuestManager;
import com.frost.envoys.quest.QuestKillHandler;
import com.frost.envoys.skin.service.AnimSyncService;
import com.frost.envoys.util.PathManager;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class FabricServerEvents {
    private FabricServerEvents() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EnvoysCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            FabricPlatform.setServer(server);
            PathManager.initServer(server);
            NPCConfigManager.loadAll();
            TradeCounterStore.loadAll();
            LuaEngineManager.init();
            AnimSyncService.refreshCache();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LuaEngineManager.shutdownAll();
            NPCConfigManager.saveAll();
            TradeCounterStore.saveAll();
            PathManager.clearServer();
            FabricPlatform.setServer(null);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PlayerQuestManager.sync(handler.player));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                PlayerQuestManager.sync(newPlayer));

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof BaseNPC npc) {
                npc.onLoadedIntoLevel();
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register(QuestKillHandler::handle);
    }

}
