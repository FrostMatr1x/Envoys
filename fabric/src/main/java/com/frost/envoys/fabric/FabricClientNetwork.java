package com.frost.envoys.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class FabricClientNetwork {
    private FabricClientNetwork() {
    }

    public static void send(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
