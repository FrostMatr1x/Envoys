package com.frost.envoys.gui.screen.action;

import com.frost.envoys.action.model.ActionSavePoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingSavePointScreen extends Screen {

    private final Screen parentScreen;
    private final ActionSavePoint action;

    private EditBox nameEditBox;
    private EditBox cpEditBox;
    private EditBox nextActionIdEditBox;
    private Button exitButton;

    private String name = "";
    private String cp = "";
    private String nextActionId = "";
    private boolean exit;

    public SettingSavePointScreen(Screen parentScreen, ActionSavePoint action) {
        super(Component.literal("Точка сохранения"));
        this.parentScreen = parentScreen;
        this.action = action;
        this.name = action.saveId != null ? action.saveId : "";
        this.cp = action.checkpointUuid != null ? action.checkpointUuid : "";
        this.nextActionId = action.nextActionId != null ? action.nextActionId : "";
        this.exit = action.exitOnSave;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 55;

        this.nameEditBox = new EditBox(this.font, centerX + 10, startY, 200, 20, Component.literal("name"));
        this.nameEditBox.setValue(this.name);
        this.nameEditBox.setResponder(text -> this.name = text);
        this.nameEditBox.setTooltip(Tooltip.create(Component.literal("Ключ точки (например, quest_point). Разрешены [a-zA-Z0-9_]")));
        this.addRenderableWidget(this.nameEditBox);

        this.cpEditBox = new EditBox(this.font, centerX + 10, startY + 30, 200, 20, Component.literal("cp"));
        this.cpEditBox.setMaxLength(36);
        this.cpEditBox.setValue(this.cp);
        this.cpEditBox.setEditable(false);
        this.cpEditBox.setTooltip(Tooltip.create(Component.literal("Внутренний UUID точки. Генерируется автоматически")));
        this.addRenderableWidget(this.cpEditBox);

        this.nextActionIdEditBox = new EditBox(this.font, centerX + 10, startY + 60, 200, 20,
                Component.literal("nextActionId"));
        this.nextActionIdEditBox.setValue(this.nextActionId);
        this.nextActionIdEditBox.setResponder(text -> this.nextActionId = text);
        this.addRenderableWidget(this.nextActionIdEditBox);

        this.exitButton = Button.builder(exitLabel(), button -> {
            this.exit = !this.exit;
            button.setMessage(exitLabel());
        }).bounds(centerX - 160, startY + 95, 320, 20).build();
        this.addRenderableWidget(this.exitButton);

        this.addRenderableWidget(Button.builder(Component.literal("Назад"), button -> {
            this.save();
            if (this.parentScreen != null) {
                Minecraft.getInstance().setScreen(this.parentScreen);
            } else {
                this.onClose();
            }
        }).bounds(centerX - 100, this.height - 35, 200, 20).build());
    }

    private Component exitLabel() {
        return Component.literal("Прервать после сохранения: " + (this.exit ? "включено" : "выключено"));
    }

    private void save() {
        this.action.saveId = this.name.trim();
        this.action.checkpointUuid = this.cp.trim();
        this.action.nextActionId = this.nextActionId.trim();
        this.action.exitOnSave = this.exit;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 55;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Ключ точки:", centerX - 160, startY + 6, 0xA0A0A0);
        guiGraphics.drawString(this.font, "UUID точки:", centerX - 160, startY + 36, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Следующий ID:", centerX - 160, startY + 66, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
