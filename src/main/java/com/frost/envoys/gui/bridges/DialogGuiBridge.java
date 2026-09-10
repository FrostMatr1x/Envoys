package com.frost.envoys.gui.bridges;

import java.util.UUID;

import com.frost.envoys.action.model.ActionDialog;

import net.minecraft.world.entity.player.Player;

public interface DialogGuiBridge {

    void open(Player player, UUID npcUuid, ActionDialog dialog, AnswerCallback callback);

    @FunctionalInterface
    interface AnswerCallback {

        /**
         * Сюда GUI должен вернуть выбранный ключ ответа.
         * Обычно это key из dialog.answers.
         */
        void onAnswer(String answerKey);
    }
}
