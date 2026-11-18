package com.frost.envoys.fabric;

import java.util.concurrent.CompletableFuture;

import com.frost.envoys.network.NetworkContext;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.player.Player;

public final class FabricClientNetworkContext implements NetworkContext {
    private final ClientPlayNetworking.Context context;

    public FabricClientNetworkContext(ClientPlayNetworking.Context context) {
        this.context = context;
    }

    @Override
    public CompletableFuture<Void> enqueueWork(Runnable task) {
        return CompletableFuture.runAsync(task, context.client());
    }

    @Override
    public Player player() {
        return context.player();
    }
}
