package com.frost.envoys.gui.screen.action;

import com.frost.envoys.action.model.ActionQuestCheck;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingQuestCheckScreen extends Screen {

    private static final String COMPLETED_TOOLTIP = "ID действия, выполняемого, если квест выполнен (ветка «выполнено»). Пусто — конец цепочки. Формат: id_N";
    private static final String NOT_COMPLETED_TOOLTIP = "ID действия, выполняемого, если квест НЕ выполнен (ветка «не выполнено»). Пусто — конец цепочки. Формат: id_N";

    private final Screen parentScreen;
    private final ActionQuestCheck action;

    private EditBox actionIfCompletedEditBox;
    private EditBox actionIfNotCompletedEditBox;

    private String actionIfCompleted = "";
    private String actionIfNotCompleted = "";

    public SettingQuestCheckScreen(Screen parentScreen, ActionQuestCheck action) {
        super(Component.literal("Настройка проверки квеста"));
        this.parentScreen = parentScreen;
        this.action = action;
        this.actionIfCompleted = action.actionIfCompleted != null ? action.actionIfCompleted : "";
        this.actionIfNotCompleted = action.actionIfNotCompleted != null ? action.actionIfNotCompleted : "";
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 25;

        this.actionIfCompletedEditBox = new EditBox(this.font, centerX + 10, startY - 2, 200, 20, Component.literal("actionIfCompleted"));
        this.actionIfCompletedEditBox.setValue(this.actionIfCompleted);
        this.actionIfCompletedEditBox.setResponder(text -> this.actionIfCompleted = text);
        this.actionIfCompletedEditBox.setTooltip(Tooltip.create(Component.literal(COMPLETED_TOOLTIP)));
        this.addRenderableWidget(this.actionIfCompletedEditBox);

        this.actionIfNotCompletedEditBox = new EditBox(this.font, centerX + 10, startY + 28, 200, 20, Component.literal("actionIfNotCompleted"));
        this.actionIfNotCompletedEditBox.setValue(this.actionIfNotCompleted);
        this.actionIfNotCompletedEditBox.setResponder(text -> this.actionIfNotCompleted = text);
        this.actionIfNotCompletedEditBox.setTooltip(Tooltip.create(Component.literal(NOT_COMPLETED_TOOLTIP)));
        this.addRenderableWidget(this.actionIfNotCompletedEditBox);

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
        this.action.actionIfCompleted = this.actionIfCompleted.trim();
        this.action.actionIfNotCompleted = this.actionIfNotCompleted.trim();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 25;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Если выполнен (ID):", centerX - 160, startY, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Если НЕ выполнен (ID):", centerX - 160, startY + 30, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
