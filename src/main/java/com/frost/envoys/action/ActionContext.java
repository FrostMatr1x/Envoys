package com.frost.envoys.action;

import java.util.UUID;

import com.frost.envoys.npc.entity.BaseNPC;

import net.minecraft.world.entity.player.Player;

public final class ActionContext {

    private final NPCInteractManager manager;
    private final ScriptRunner runner;
    private final Player player;

    ActionContext(NPCInteractManager manager, ScriptRunner runner, Player player) {
        this.manager = manager;
        this.runner = runner;
        this.player = player;
    }

    public UUID npcUuid() {
        return manager.npcUUID;
    }

    public NPCInteractManager manager() {
        return manager;
    }

    public ScriptRunner runner() {
        return runner;
    }

    public BaseNPC npc() {
        return runner.npc();
    }

    public Player player() {
        return player;
    }

    public void advance(String nextActionId) {
        runner.advance(nextActionId);
    }

    public void waitForDelay(int ticks) {
        runner.waitForDelay(ticks);
    }

    public void waitForMove() {
        runner.waitForMove();
    }

    public void waitForDialog() {
        runner.waitForDialog();
    }

    public void waitForTrade() {
        runner.waitForTrade();
    }

    public void onTradeClose() {
        runner.onTradeClose();
    }
}
