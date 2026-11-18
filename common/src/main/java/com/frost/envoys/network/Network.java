package com.frost.envoys.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class Network {
    private static NetworkManager manager;

    private Network() {
    }

    public static void init(NetworkManager manager) {
        Network.manager = manager;
    }

    private static NetworkManager get() {
        if (manager == null) {
            throw new IllegalStateException("Envoys network is not initialized");
        }
        return manager;
    }

    public static void sendToServer(CustomPacketPayload payload) {
        get().sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        get().sendToPlayer(player, payload);
    }

    public static void sendToAllPlayers(CustomPacketPayload payload) {
        get().sendToAllPlayers(payload);
    }
}
