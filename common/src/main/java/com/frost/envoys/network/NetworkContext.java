package com.frost.envoys.network;

import java.util.concurrent.CompletableFuture;

import net.minecraft.world.entity.player.Player;

public interface NetworkContext {
    CompletableFuture<Void> enqueueWork(Runnable task);

    Player player();
}
