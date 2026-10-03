package com.frost.envoys.network;

import com.frost.envoys.Envoys;
import com.frost.envoys.network.payload.AnimDataPayload;
import com.frost.envoys.network.payload.AnimListPayload;
import com.frost.envoys.network.payload.AnimOpResultPayload;
import com.frost.envoys.network.payload.AnimRemovedPayload;
import com.frost.envoys.network.payload.DeleteAnimPayload;
import com.frost.envoys.network.payload.PushAnimPayload;
import com.frost.envoys.network.payload.RequestAnimListPayload;
import com.frost.envoys.network.payload.RequestAnimPayload;
import com.frost.envoys.skin.service.AnimSyncService;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class ServerAnimPayloadHandler {

    public static void handleRequestAnimList(final RequestAnimListPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            AnimSyncService.listAnimsAsync().thenAccept(entries -> {
                List<AnimListPayload.AnimInfo> infos = new ArrayList<>(entries.size());
                for (AnimSyncService.AnimListEntry entry : entries) {
                    infos.add(new AnimListPayload.AnimInfo(entry.name(), entry.hash()));
                }
                player.server.execute(() -> PacketDistributor.sendToPlayer(player, new AnimListPayload(infos)));
            });
        });
    }

    public static void handleRequestAnim(final RequestAnimPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            AnimSyncService.loadAnimAsync(payload.name()).thenAccept(result -> {
                if (result.isSuccess()) {
                    player.server.execute(() -> PacketDistributor.sendToPlayer(player, new AnimDataPayload(result.name(), result.jsonData())));
                } else {
                    Envoys.LOGGER.warn("[Envoys] Animation '{}' was not found on the server: {}", payload.name(), result.errorMsg());
                }
            });
        });
    }

    public static void handlePushAnim(final PushAnimPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            if (!player.hasPermissions(4)) {
                Envoys.LOGGER.warn("[Envoys] Player {} tried to push animation '{}' without OP-4",
                        player.getName().getString(), payload.name());
                PacketDistributor.sendToPlayer(player,
                        new AnimOpResultPayload(false, "push", payload.name(), "envoys.cmd.anim.op_only"));
                return;
            }

            AnimSyncService.saveAnimAsync(payload.name(), payload.jsonData(), payload.hash()).thenAccept(result -> {
                player.server.execute(() -> {
                    PacketDistributor.sendToPlayer(player,
                            new AnimOpResultPayload(result.success(), "push", payload.name(), result.messageKey()));
                    if (result.success()) {
                        Envoys.LOGGER.info("[Envoys] Player {} pushed animation '{}'",
                                player.getName().getString(), payload.name());
                    } else {
                        Envoys.LOGGER.warn("[Envoys] Push of animation '{}' by {} rejected: {}",
                                payload.name(), player.getName().getString(), result.messageKey());
                    }
                });
            });
        });
    }

    public static void handleDeleteAnim(final DeleteAnimPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            if (!player.hasPermissions(4)) {
                Envoys.LOGGER.warn("[Envoys] Player {} tried to delete animation '{}' without OP-4",
                        player.getName().getString(), payload.name());
                PacketDistributor.sendToPlayer(player,
                        new AnimOpResultPayload(false, "delete", payload.name(), "envoys.cmd.anim.op_only"));
                return;
            }

            AnimSyncService.deleteAnimAsync(payload.name()).thenAccept(result -> {
                player.server.execute(() -> {
                    if (result.success()) {
                        PacketDistributor.sendToAllPlayers(new AnimRemovedPayload(payload.name()));
                        Envoys.LOGGER.info("[Envoys] Player {} deleted animation '{}'",
                                player.getName().getString(), payload.name());
                    } else {
                        Envoys.LOGGER.warn("[Envoys] Delete of animation '{}' by {} rejected: {}",
                                payload.name(), player.getName().getString(), result.messageKey());
                    }
                    PacketDistributor.sendToPlayer(player,
                            new AnimOpResultPayload(result.success(), "delete", payload.name(), result.messageKey()));
                });
            });
        });
    }
}
