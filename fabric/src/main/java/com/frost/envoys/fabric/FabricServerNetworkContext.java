package com.frost.envoys.fabric;

import java.util.concurrent.CompletableFuture;

import com.frost.envoys.network.NetworkContext;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.player.Player;

public final class FabricServerNetworkContext implements NetworkContext {
    private final ServerPlayNetworking.Context context;

    public FabricServerNetworkContext(ServerPlayNetworking.Context context) {
        this.context = context;
    }

    @Override
    public CompletableFuture<Void> enqueueWork(Runnable task) {
        return CompletableFuture.runAsync(task, context.server());
    }

    @Override
    public Player player() {
        return context.player();
    }
}
