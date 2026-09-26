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

    private final Screen parentScreen;
    private final ActionLoadPoint action;

    private EditBox targetEditBox;
    private EditBox nextActionIdEditBox;

    private String target = "";
    private String nextActionId = "";

    public SettingLoadPointScreen(Screen parentScreen, ActionLoadPoint action) {
        super(Component.translatable("envoys.setting.load_point.title"));
        this.parentScreen = parentScreen;
        this.action = action;
        this.target = action.saveId != null ? action.saveId : "";
        this.nextActionId = action.nextActionId != null ? action.nextActionId : "";
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 35;

        this.targetEditBox = new EditBox(this.font, centerX + 10, startY, 200, 20, Component.literal("target"));
        this.targetEditBox.setValue(this.target);
        this.targetEditBox.setResponder(text -> this.target = text);
        this.targetEditBox.setTooltip(Tooltip.create(Component.translatable("envoys.setting.load_point.target_tooltip")));
        this.addRenderableWidget(this.targetEditBox);

        this.nextActionIdEditBox = new EditBox(this.font, centerX + 10, startY + 30, 200, 20,
                Component.literal("nextActionId"));
        this.nextActionIdEditBox.setValue(this.nextActionId);
        this.nextActionIdEditBox.setResponder(text -> this.nextActionId = text);
        this.nextActionIdEditBox.setTooltip(Tooltip.create(Component.translatable("envoys.setting.load_point.next_id_tooltip")));
        this.addRenderableWidget(this.nextActionIdEditBox);

        this.addRenderableWidget(Button.builder(Component.translatable("envoys.gui.common.back"), button -> {
            this.save();
            if (this.parentScreen != null) {
                Minecraft.getInstance().setScreen(this.parentScreen);
            } else {
                this.onClose();
            }
        }).bounds(centerX - 100, this.height - 35, 200, 20).build());
    }

    private void save() {
        this.action.saveId = this.target.trim();
        this.action.nextActionId = this.nextActionId.trim();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 35;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xA0A0A0);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.load_point.name"), centerX - 160, startY + 6, 0xA0A0A0);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.load_point.next_id"),
                centerX - 160, startY + 36, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
