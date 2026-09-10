package com.frost.envoys.gui.screen.action;

import com.frost.envoys.action.model.ActionQuestGive;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingQuestGiveScreen extends Screen {

    private static final String NEXT_TOOLTIP = "ID действия, которое выполнится далее. Пусто — конец цепочки. Формат: id_N";

    private final Screen parentScreen;
    private final ActionQuestGive action;

    private EditBox questTextEditBox;
    private EditBox questTypeEditBox;
    private EditBox nextActionIdEditBox;

    private String questText = "";
    private String questType = "";
    private String nextActionId = "";

    public SettingQuestGiveScreen(Screen parentScreen, ActionQuestGive action) {
        super(Component.literal("Настройка выдачи квеста"));
        this.parentScreen = parentScreen;
        this.action = action;
        this.questText = action.questText != null ? action.questText : "";
        this.questType = action.questType != null ? action.questType : "";
        this.nextActionId = action.nextActionId != null ? action.nextActionId : "";
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 45;

        this.questTextEditBox = new EditBox(this.font, centerX + 10, startY - 2, 200, 20, Component.literal("questText"));
        this.questTextEditBox.setValue(this.questText);
        this.questTextEditBox.setResponder(text -> this.questText = text);
        this.addRenderableWidget(this.questTextEditBox);

        this.questTypeEditBox = new EditBox(this.font, centerX + 10, startY + 28, 200, 20, Component.literal("questType"));
        this.questTypeEditBox.setValue(this.questType);
        this.questTypeEditBox.setResponder(text -> this.questType = text);
        this.addRenderableWidget(this.questTypeEditBox);

        this.nextActionIdEditBox = new EditBox(this.font, centerX + 10, startY + 58, 200, 20, Component.literal("nextActionId"));
        this.nextActionIdEditBox.setValue(this.nextActionId);
        this.nextActionIdEditBox.setResponder(text -> this.nextActionId = text);
        this.nextActionIdEditBox.setTooltip(Tooltip.create(Component.literal(NEXT_TOOLTIP)));
        this.addRenderableWidget(this.nextActionIdEditBox);

        this.addRenderableWidget(Button.builder(
            Component.literal("Назад"),
            button -> {
                this.save();
                if (this.parentScreen != null) {
                    Minecraft.getInstance().setScreen(this.parentScreen);
                } else {
                    this.onClose();
                }
            }
        ).bounds(centerX - 100, this.height - 35, 200, 20).build());
    }

    private void save() {
        this.action.questText = this.questText.trim();
        this.action.questType = this.questType.trim();
        this.action.nextActionId = this.nextActionId.trim();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 45;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Текст квеста:", centerX - 160, startY, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Тип квеста [TEMP/TODO]:", centerX - 160, startY + 30, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Следующее действие:", centerX - 160, startY + 60, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
