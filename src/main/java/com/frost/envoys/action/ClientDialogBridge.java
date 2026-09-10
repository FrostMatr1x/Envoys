package com.frost.envoys.action;

import com.frost.envoys.action.model.ActionDialog;
import com.frost.envoys.gui.bridges.DialogGuiBridge;
import com.frost.envoys.gui.screen.NPCDialogScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientDialogBridge implements DialogGuiBridge {

    @Override
    public void open(Player player, UUID npcUuid, ActionDialog dialog, AnswerCallback callback) {
        List<NPCDialogScreen.DialogOption> options = new ArrayList<>();

        dialog.answers.forEach((answerText, nextActionId) -> {
            options.add(new NPCDialogScreen.DialogOption(
                Component.literal(answerText),
                () -> {
                    if (callback != null) {
                        callback.onAnswer(answerText);
                    }
                }
            ));
        });

        String title = "NPC";
        NPCInteractManager manager = NPCInteractManager.byUUID(npcUuid).orElse(null);
        if (manager != null && manager.passport != null && !manager.passport.npcName.isBlank()) {
            title = manager.passport.npcName;
        } else if (dialog.NPCName != null && !dialog.NPCName.isBlank()) {
            title = dialog.NPCName;
        }

        Minecraft.getInstance().setScreen(new NPCDialogScreen(
            Component.literal(title),
            dialog.npcMessage,
            options
        ));
    }
}
