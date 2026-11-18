package com.frost.envoys.gui.bridges;

import java.util.UUID;

import com.frost.envoys.action.model.ActionTrade;

import net.minecraft.world.entity.player.Player;

public interface TradeGuiBridge {

    void open(Player player, UUID npcUuid, ActionTrade action, Runnable onClose);
}
