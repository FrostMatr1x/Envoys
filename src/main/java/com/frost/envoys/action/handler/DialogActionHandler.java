package com.frost.envoys.action.handler;

import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.model.ActionDialog;
import com.frost.envoys.network.payload.OpenDialogPayload;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DialogActionHandler implements NpcActionHandler<ActionDialog> {

    @Override
    public void execute(ActionDialog action, ActionContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            context.advance(action.nextActionId);
            return;
        }

        PacketDistributor.sendToPlayer(
            serverPlayer,
            new OpenDialogPayload(
                context.npcUuid(),
                action.getId(),
                action.NPCName,
                action.npcMessage,
                action.answers
            )
        );

        context.waitForDialog();
    }
}
