package com.frost.envoys.fabric;

import com.frost.envoys.network.NetworkManager;
import com.frost.envoys.platform.EnvoysPlatform;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class FabricNetwork implements NetworkManager {
    @Override
    public void sendToServer(CustomPacketPayload payload) {
        if (EnvoysPlatform.isClient()) {
            FabricClientNetwork.send(payload);
        }
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendToAllPlayers(CustomPacketPayload payload) {
        MinecraftServer server = EnvoysPlatform.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
