package com.frost.envoys.action;

import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.gui.bridges.TradeGuiBridge;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.npc.merchant.NPCMerchant;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class GlobalTradeBridge implements TradeGuiBridge {

    @Override
    public void open(Player player, UUID npcUuid, ActionTrade action, Runnable onClose) {
        if (player instanceof ServerPlayer serverPlayer) {
            Entity entity = serverPlayer.serverLevel().getEntity(npcUuid);

            if (entity instanceof BaseNPC baseNpc) {
                NPCMerchant merchant = new NPCMerchant(baseNpc, action, serverPlayer);
                merchant.openTradingScreen(serverPlayer, Component.translatable("envoys.gui.trade_title"), 0, onClose);
            }
        }
    }
}
