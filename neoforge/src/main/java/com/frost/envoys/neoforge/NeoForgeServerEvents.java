package com.frost.envoys.neoforge;

import com.frost.envoys.EnvoysCommand;
import com.frost.envoys.config.NPCConfigManager;
import com.frost.envoys.lua.LuaEngineManager;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.npc.merchant.TradeCounterStore;
import com.frost.envoys.quest.PlayerQuestManager;
import com.frost.envoys.skin.service.AnimSyncService;
import com.frost.envoys.util.PathManager;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public final class NeoForgeServerEvents {
    private NeoForgeServerEvents() {
    }

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
        AnimSyncService.refreshCache();
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

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof BaseNPC npc) {
            npc.onLoadedIntoLevel();
        }
    }
}
