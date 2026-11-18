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
import com.frost.envoys.skin.model.SkinIndexEntry;
import com.frost.envoys.skin.model.SkinMetaData;
import com.frost.envoys.skin.service.SkinCacheService;
import com.frost.envoys.skin.service.SkinLocalService;
import com.frost.envoys.skin.service.SkinSyncService;
import com.frost.envoys.util.PathManager;
import com.frost.envoys.util.SkinModelUtil;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Locale;
import java.util.UUID;

public class ServerSkinPayloadHandler {

    private static final int MAX_SKIN_BYTES = 2 * 1024 * 1024;

    public static void handleRequestSkin(final RequestSkinPayload payload, final NetworkContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            UUID npcUuid = payload.npcUuid();
            NPCInteractManager manager = NPCInteractManager.byUUID(npcUuid).orElse(null);

            // Skins are stored on the server keyed by their value (nickname, URL or file name).
            // UUID itself is only a legacy fallback for old caches.
            String skinValue = manager != null ? manager.passport.skinValue : null;
            String skinType = manager != null ? manager.passport.skinType : null;
            if (skinValue == null || skinValue.isBlank()) {
                skinValue = npcUuid.toString();
                skinType = "FILE";
            }

            final String loadValue = skinValue;
            final String loadType = skinType == null || skinType.isBlank() ? "FILE" : skinType;

            SkinSyncService.loadSkinAsync(loadValue, loadType, false).thenAccept(result -> {
                if (!result.isSuccess()) {
                    EnvoysConstants.LOGGER.warn("[Envoys] Skin for NPC {} was not found on the server (value='{}', type='{}', reason='{}')",
                            npcUuid, loadValue, loadType, result.errorMsg());
                    return;
                }

                MinecraftServer server = player.getServer();
                if (server == null) return;

                server.execute(() -> {
                    Network.sendToPlayer(player,
                            new SkinInfoPayload(npcUuid, "", result.hash(), result.model()));
                    Network.sendToPlayer(player,
                            new SkinDataPayload(npcUuid, result.pngData()));
                });
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
            Entity target = serverLevel.getEntity(payload.npcUuid());
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

            byte[] pngData = payload.pngData();
            if (pngData != null && pngData.length > 0) {
                if (!validatePng(player, pngData)) {
                    return;
                }

                // The client already resolved and previewed the skin, so store those exact bytes.
                // This avoids a second Mojang request on the server (which could be rate-limited)
                // and also makes FILE skins work when only the client has the source image.
                String hash = SkinCacheService.calculateSHA256(pngData);
                String model = resolveModel(payload.model(), pngData);
                saveAndApply(player, payload, normalizedType, model, hash, pngData);
                return;
            }

            MinecraftServer server = player.getServer();
            SkinSyncService.loadSkinAsync(payload.skinValue(), normalizedType, false).thenAccept(result -> {
                if (!result.isSuccess()) {
                    EnvoysConstants.LOGGER.warn("[Envoys] Failed to load skin '{}' ({}) for NPC {}: {}",
                            payload.skinValue(), normalizedType, payload.npcUuid(), result.errorMsg());
                    return;
                }
                if (server == null) return;

                server.execute(() -> saveAndApply(player, payload, normalizedType,
                        resolveModel(payload.model(), result.pngData()), result.hash(), result.pngData()));
            });
        });
    }

    private static void saveAndApply(ServerPlayer player, SaveNPCSkinPayload payload, String skinType,
                                     String model, String hash, byte[] pngData) {
        NPCInteractManager manager = NPCInteractManager.byUUID(payload.npcUuid()).orElse(null);
        if (manager == null) {
            EnvoysConstants.LOGGER.warn("[Envoys] Skin save for unknown NPC {} ignored", payload.npcUuid());
            return;
        }

        String fileKey = SkinSyncService.resolveFileKey(payload.skinValue(), skinType);
        String source = switch (skinType) {
            case "URL" -> "url";
            case "FILE" -> "local";
            default -> "mojang";
        };

        try {
            SkinCacheService.saveToCache(
                    PathManager.getServerSkinCacheDir(),
                    fileKey,
                    pngData,
                    new SkinMetaData(fileKey, hash, model, source, System.currentTimeMillis())
            );
            SkinLocalService.addOrUpdateIndex(
                    PathManager.getServerIndexFile(),
                    new SkinIndexEntry(fileKey, fileKey + ".png", model, source, hash, System.currentTimeMillis())
            );
        } catch (Exception e) {
            EnvoysConstants.LOGGER.error("[Envoys] Failed to store skin '{}' for NPC {}", payload.skinValue(), payload.npcUuid(), e);
            return;
        }

        manager.passport.skinType = skinType;
        manager.passport.skinValue = payload.skinValue();
        manager.passport.skinModel = model;
        manager.passport.skinHash = hash;
        NPCConfigManager.save(manager);

        ServerLevel serverLevel = player.serverLevel();
        Entity target = serverLevel.getEntity(payload.npcUuid());
        if (target instanceof BaseNPC npc) {
            npc.applySkinData(skinType, payload.skinValue(), model, hash);
        }

        Network.sendToPlayer(player, new SkinConfirmedPayload(payload.npcUuid(), hash));
        EnvoysConstants.LOGGER.info("[Envoys] Skin for NPC {} saved and applied in the world", payload.npcUuid());
    }

    private static boolean validatePng(ServerPlayer player, byte[] pngData) {
        if (pngData.length > MAX_SKIN_BYTES) {
            EnvoysConstants.LOGGER.warn("[Envoys] Player {} sent a skin that is too large ({} bytes)",
                    player.getName().getString(), pngData.length);
            return false;
        }
        if (pngData.length < 8
                || (pngData[0] & 0xFF) != 0x89 || pngData[1] != 'P'
                || pngData[2] != 'N' || pngData[3] != 'G') {
            EnvoysConstants.LOGGER.warn("[Envoys] Player {} sent an invalid PNG skin", player.getName().getString());
            return false;
        }
        return true;
    }

    private static String resolveModel(String requestedModel, byte[] pngData) {
        if ("slim".equalsIgnoreCase(requestedModel)) {
            return "slim";
        }
        if ("classic".equalsIgnoreCase(requestedModel) || "default".equalsIgnoreCase(requestedModel)
                || "wide".equalsIgnoreCase(requestedModel)) {
            return "classic";
        }
        return "slim".equalsIgnoreCase(SkinModelUtil.detectModel(pngData)) ? "slim" : "classic";
    }
}
