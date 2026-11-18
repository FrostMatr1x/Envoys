package com.frost.envoys.network;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.config.NPCConfigManager;
import com.frost.envoys.network.payload.RequestSkinPayload;
import com.frost.envoys.network.payload.SaveNPCSkinPayload;
import com.frost.envoys.network.payload.SkinConfirmedPayload;
import com.frost.envoys.network.payload.SkinDataPayload;
import com.frost.envoys.network.payload.SkinInfoPayload;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.skin.service.SkinCacheService;
import com.frost.envoys.skin.service.SkinLocalService;
import com.frost.envoys.skin.service.SkinSyncService;
import com.frost.envoys.util.PathManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class ServerSkinPayloadHandler {

    public static void handleRequestSkin(final RequestSkinPayload payload, final NetworkContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            String uuidStr = payload.npcUuid().toString();

            SkinSyncService.loadSkinAsync(uuidStr, "FILE", false)
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            Network.sendToPlayer(player, 
                                    new SkinInfoPayload(payload.npcUuid(), "", result.hash(), result.model()));

                            Network.sendToPlayer(player, 
                                    new SkinDataPayload(payload.npcUuid(), result.pngData()));
                        } else {
                            EnvoysConstants.LOGGER.warn("[Envoys] Skin for NPC {} was not found on the server", payload.npcUuid());
                        }
                    });
        });
    }

    public static void handleSaveNPCSkin(final SaveNPCSkinPayload payload, final NetworkContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            if (!ServerPayloadHandler.hasTunerAccess(player)) {
                EnvoysConstants.LOGGER.warn("[Envoys] Player {} tried to save NPC skin without tuner!", player.getName().getString());
                return;
            }

            ServerLevel serverLevel = player.serverLevel();
            net.minecraft.world.entity.Entity target = serverLevel.getEntity(payload.npcUuid());
            if (target != null && player.distanceToSqr(target) > 64.0D) {
                EnvoysConstants.LOGGER.warn("[Envoys] Player {} tried to edit NPC skin {} from too far away!",
                        player.getName().getString(), payload.npcUuid());
                return;
            }

            NPCInteractManager manager = NPCInteractManager.byUUID(payload.npcUuid()).orElse(null);
            if (manager == null) {
                EnvoysConstants.LOGGER.warn("[Envoys] Skin save for unknown NPC {} ignored", payload.npcUuid());
                return;
            }

            if (!player.hasPermissions(4) && manager.passport.creativeTunerOnly) {
                EnvoysConstants.LOGGER.warn("[Envoys] Player {} tried to modify creative-only NPC skin!", player.getName().getString());
                return;
            }

            String skinType = payload.skinType();
            String normalizedType = skinType == null ? "" : skinType.toUpperCase(Locale.ROOT);
            if (!normalizedType.equals("NICKNAME") && !normalizedType.equals("URL") && !normalizedType.equals("FILE")) {
                EnvoysConstants.LOGGER.warn("[Envoys] Player {} sent invalid NPC skin type '{}'",
                        player.getName().getString(), skinType);
                return;
            }

            SkinSyncService.loadSkinAsync(payload.skinValue(), skinType, false)
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            manager.passport.skinType = skinType;
                            manager.passport.skinValue = payload.skinValue();
                            manager.passport.skinModel = payload.model();
                            manager.passport.skinHash = result.hash();
                            NPCConfigManager.save(manager);

                            if (target instanceof BaseNPC npc) {
                                npc.applySkinData(skinType, payload.skinValue(), payload.model(), result.hash());
                            }

                            Network.sendToPlayer(player, new SkinConfirmedPayload(payload.npcUuid(), result.hash()));
                            EnvoysConstants.LOGGER.info("[Envoys] Skin for NPC {} saved and applied in the world", payload.npcUuid());
                        }
                    });
        });
    }
}