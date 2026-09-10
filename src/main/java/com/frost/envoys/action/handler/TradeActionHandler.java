package com.frost.envoys.action.handler;

import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.npc.merchant.NPCMerchant;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class TradeActionHandler implements NpcActionHandler<ActionTrade> {

    @Override
    public void execute(ActionTrade action, ActionContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            context.advance(action.nextActionId);
            return;
        }

        Entity entity = serverPlayer.serverLevel().getEntity(context.npcUuid());
        if (!(entity instanceof BaseNPC baseNpc)) {
            context.advance(action.nextActionId);
            return;
        }

        NPCMerchant merchant = new NPCMerchant(baseNpc, action, serverPlayer);
        context.waitForTrade();
        merchant.openTradingScreen(serverPlayer, Component.literal("Торговля"), 0, context::onTradeClose);
    }
}
