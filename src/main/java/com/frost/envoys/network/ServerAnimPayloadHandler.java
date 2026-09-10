package com.frost.envoys.network;

import com.frost.envoys.Envoys;
import com.frost.envoys.network.payload.AnimDataPayload;
import com.frost.envoys.network.payload.AnimListPayload;
import com.frost.envoys.network.payload.RequestAnimListPayload;
import com.frost.envoys.network.payload.RequestAnimPayload;
import com.frost.envoys.skin.service.AnimSyncService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ServerAnimPayloadHandler {

    public static void handleRequestAnimList(final RequestAnimListPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            File worldDir = player.getServer().getWorldPath(LevelResource.ROOT).toFile();

            AnimSyncService.listAnimsAsync(worldDir).thenAccept(entries -> {
                List<AnimListPayload.AnimInfo> infos = new ArrayList<>(entries.size());
                for (AnimSyncService.AnimListEntry entry : entries) {
                    infos.add(new AnimListPayload.AnimInfo(entry.name(), entry.hash()));
                }
                PacketDistributor.sendToPlayer(player, new AnimListPayload(infos));
            });
        });
    }

    public static void handleRequestAnim(final RequestAnimPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            File worldDir = player.getServer().getWorldPath(LevelResource.ROOT).toFile();

            AnimSyncService.loadAnimAsync(payload.name(), worldDir).thenAccept(result -> {
                if (result.isSuccess()) {
                    PacketDistributor.sendToPlayer(player, new AnimDataPayload(result.name(), result.jsonData()));
                } else {
                    Envoys.LOGGER.warn("[Envoys] Анимация '{}' не найдена на сервере: {}", payload.name(), result.errorMsg());
                }
            });
        });
    }
}
