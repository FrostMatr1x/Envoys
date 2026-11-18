package com.frost.envoys.neoforge;

import java.util.concurrent.CompletableFuture;

import com.frost.envoys.network.NetworkContext;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForgeNetworkContext implements NetworkContext {
    private final IPayloadContext context;

    public NeoForgeNetworkContext(IPayloadContext context) {
        this.context = context;
    }

    @Override
    public CompletableFuture<Void> enqueueWork(Runnable task) {
        return context.enqueueWork(task);
    }

    @Override
    public Player player() {
        return context.player();
    }
}
