package com.frost.envoys.gui.screen.action;

import com.frost.envoys.action.model.ActionLoadPoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingLoadPointScreen extends Screen {

    private static final String SAVE_ID_TOOLTIP = "ID пула сохранений, из которого нужно загрузить последнюю точку";
    private static final String FALLBACK_TOOLTIP = "Переход, если сохранение не найдено";

    private final Screen parentScreen;
    private final ActionLoadPoint action;

    private EditBox saveIdEditBox;
    private EditBox fallbackActionIdEditBox;

    private String saveId = "";
    private String fallbackActionId = "";

    public SettingLoadPointScreen(Screen parentScreen, ActionLoadPoint action) {
        super(Component.literal("Настройка точки загрузки"));
        this.parentScreen = parentScreen;
        this.action = action;
        this.saveId = action.saveId != null ? action.saveId : "";
        this.fallbackActionId = action.nextActionId != null ? action.nextActionId : "";
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 30;

        this.saveIdEditBox = new EditBox(this.font, centerX + 10, startY, 200, 20, Component.literal("saveId"));
        this.saveIdEditBox.setValue(this.saveId);
        this.saveIdEditBox.setResponder(text -> this.saveId = text);
        this.saveIdEditBox.setTooltip(Tooltip.create(Component.literal(SAVE_ID_TOOLTIP)));
        this.addRenderableWidget(this.saveIdEditBox);

        this.fallbackActionIdEditBox = new EditBox(this.font, centerX + 10, startY + 30, 200, 20, Component.literal("fallbackActionId"));
        this.fallbackActionIdEditBox.setValue(this.fallbackActionId);
        this.fallbackActionIdEditBox.setResponder(text -> this.fallbackActionId = text);
        this.fallbackActionIdEditBox.setTooltip(Tooltip.create(Component.literal(FALLBACK_TOOLTIP)));
        this.addRenderableWidget(this.fallbackActionIdEditBox);

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
        this.action.nextActionId = this.fallbackActionId.trim();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 30;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, "ID пула сохранений:", centerX - 160, startY + 6, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Если сохранения нет:", centerX - 160, startY + 36, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
