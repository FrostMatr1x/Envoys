package com.frost.envoys.gui.screen.action;

import com.frost.envoys.action.model.ActionSavePoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingSavePointScreen extends Screen {

    private static final String SAVE_ID_TOOLTIP = "ID пула сохранений (например, chapter_1). Несколько чекпоинтов одной ветки могут иметь один save_id";
    private static final String CHECKPOINT_TOOLTIP = "Уникальный UUID точки. Генерируется автоматически и не редактируется";
    private static final String NEXT_TOOLTIP = "ID действия, которое выполнится далее. Пусто — конец цепочки. Формат: id_N";
    private static final String EXIT_TOOLTIP = "Завершить цепочку после сохранения, не переходя к следующему действию";

    private final Screen parentScreen;
    private final ActionSavePoint action;

    private EditBox saveIdEditBox;
    private EditBox checkpointUuidEditBox;
    private EditBox nextActionIdEditBox;

    private String saveId = "";
    private String nextActionId = "";
    private boolean exitOnSave;

    public SettingSavePointScreen(Screen parentScreen, ActionSavePoint action) {
        super(Component.literal("Настройка точки сохранения"));
        this.parentScreen = parentScreen;
        this.action = action;
        this.saveId = action.saveId != null ? action.saveId : "";
        this.nextActionId = action.nextActionId != null ? action.nextActionId : "";
        this.exitOnSave = action.exitOnSave;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 45;

        this.saveIdEditBox = new EditBox(this.font, centerX + 10, startY, 200, 20, Component.literal("saveId"));
        this.saveIdEditBox.setValue(this.saveId);
        this.saveIdEditBox.setResponder(text -> this.saveId = text);
        this.saveIdEditBox.setTooltip(Tooltip.create(Component.literal(SAVE_ID_TOOLTIP)));
        this.addRenderableWidget(this.saveIdEditBox);

        this.checkpointUuidEditBox = new EditBox(this.font, centerX + 10, startY + 30, 200, 20, Component.literal("checkpointUuid"));
        this.checkpointUuidEditBox.setValue(this.action.checkpointUuid != null ? this.action.checkpointUuid : "");
        this.checkpointUuidEditBox.setEditable(false);
        this.checkpointUuidEditBox.setTooltip(Tooltip.create(Component.literal(CHECKPOINT_TOOLTIP)));
        this.addRenderableWidget(this.checkpointUuidEditBox);

        this.nextActionIdEditBox = new EditBox(this.font, centerX + 10, startY + 60, 200, 20, Component.literal("nextActionId"));
        this.nextActionIdEditBox.setValue(this.nextActionId);
        this.nextActionIdEditBox.setResponder(text -> this.nextActionId = text);
        this.nextActionIdEditBox.setTooltip(Tooltip.create(Component.literal(NEXT_TOOLTIP)));
        this.addRenderableWidget(this.nextActionIdEditBox);

        this.addRenderableWidget(Checkbox.builder(Component.literal("Выйти из цепочки"), this.font)
            .pos(centerX - 160, startY + 95)
            .selected(this.exitOnSave)
            .onValueChange((checkbox, value) -> this.exitOnSave = value)
            .tooltip(Tooltip.create(Component.literal(EXIT_TOOLTIP)))
            .build());

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
        this.action.saveId = this.saveId.trim();
        this.action.nextActionId = this.nextActionId.trim();
        this.action.exitOnSave = this.exitOnSave;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 45;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, "ID пула сохранений:", centerX - 160, startY + 6, 0xA0A0A0);
        guiGraphics.drawString(this.font, "UUID чекпоинта:", centerX - 160, startY + 36, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Следующее действие:", centerX - 160, startY + 66, 0xA0A0A0);
        guiGraphics.drawString(this.font, "После сохранения:", centerX - 160, startY + 80, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
