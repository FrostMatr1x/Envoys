package com.frost.envoys.action.handler;

import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.model.ActionChat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ChatActionHandler implements NpcActionHandler<ActionChat> {

    @Override
    public void execute(ActionChat action, ActionContext context) {
        String raw = action.message != null ? action.message : "";
        Component message = Component.literal(raw);

        if (action.isGlobal) {
            for (Player player : context.npc().level().players()) {
                player.sendSystemMessage(message);
            }
        } else if (context.player() instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message);
        }

        context.advance(action.nextActionId);
    }
}
