package com.frost.envoys.network;

import com.frost.envoys.Envoys;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerSkinPayloadHandler {

    public static void handleRequestSkin(final RequestSkinPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            File worldDir = player.getServer().getWorldPath(LevelResource.ROOT).toFile();
            String uuidStr = payload.npcUuid().toString();

            SkinSyncService.loadSkinAsync(uuidStr, "FILE", worldDir, false)
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            PacketDistributor.sendToPlayer(player, 
                                    new SkinInfoPayload(payload.npcUuid(), "", result.hash(), result.model()));

                            PacketDistributor.sendToPlayer(player, 
                                    new SkinDataPayload(payload.npcUuid(), result.pngData()));
                        } else {
                            Envoys.LOGGER.warn("[Envoys] Скин для NPC {} не найден на сервере", payload.npcUuid());
                        }
                    });
        });
    }

    public static void handleSaveNPCSkin(final SaveNPCSkinPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() == null) return;

            ServerPlayer player = (ServerPlayer) context.player();
            File worldDir = player.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();

            SkinSyncService.loadSkinAsync(payload.skinValue(), payload.skinType(), worldDir, false)
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            NPCInteractManager manager = NPCInteractManager.byUUID(payload.npcUuid())
                                    .orElseGet(() -> new NPCInteractManager(payload.npcUuid()));

                            manager.passport.skinType = payload.skinType();
                            manager.passport.skinValue = payload.skinValue();
                            manager.passport.skinModel = payload.model();
                            manager.passport.skinHash = result.hash();
                            NPCConfigManager.save(manager);

                            if (player.level() instanceof ServerLevel serverLevel) {
                                net.minecraft.world.entity.Entity entity = serverLevel.getEntity(payload.npcUuid());
                                if (entity instanceof BaseNPC npc) {
                                    npc.applySkinData(payload.skinType(), payload.skinValue(), payload.model(), result.hash());
                                }
                            }

                            PacketDistributor.sendToPlayer(player, new SkinConfirmedPayload(payload.npcUuid(), result.hash()));
                            Envoys.LOGGER.info("[Envoys] Скин для NPC {} сохранен и применен в мире!", payload.npcUuid());
                        }
                    });
        });
    }
}