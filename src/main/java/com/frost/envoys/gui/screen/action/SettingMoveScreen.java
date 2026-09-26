package com.frost.envoys.gui.screen.action;

import com.frost.envoys.action.model.ActionMove;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingMoveScreen extends Screen {

    private final Screen parentScreen;
    private final ActionMove action;

    private EditBox targetXEditBox;
    private EditBox targetYEditBox;
    private EditBox targetZEditBox;
    private EditBox nextActionIdEditBox;

    private float targetX;
    private float targetY;
    private float targetZ;
    private String nextActionId = "";

    public SettingMoveScreen(Screen parentScreen, ActionMove action) {
        super(Component.translatable("envoys.setting.move.title"));
        this.parentScreen = parentScreen;
        this.action = action;
        this.targetX = action.targetX;
        this.targetY = action.targetY;
        this.targetZ = action.targetZ;
        this.nextActionId = action.nextActionId != null ? action.nextActionId : "";
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 55;

        this.targetXEditBox = this.createFloatBox(centerX, startY, this.targetX);
        this.targetXEditBox.setResponder(text -> this.targetX = parseOrDefaultFloat(text, this.targetX));
        this.addRenderableWidget(this.targetXEditBox);

        this.targetYEditBox = this.createFloatBox(centerX, startY + 30, this.targetY);
        this.targetYEditBox.setResponder(text -> this.targetY = parseOrDefaultFloat(text, this.targetY));
        this.addRenderableWidget(this.targetYEditBox);

        this.targetZEditBox = this.createFloatBox(centerX, startY + 60, this.targetZ);
        this.targetZEditBox.setResponder(text -> this.targetZ = parseOrDefaultFloat(text, this.targetZ));
        this.addRenderableWidget(this.targetZEditBox);

        this.nextActionIdEditBox = new EditBox(this.font, centerX + 10, startY + 90, 200, 20, Component.literal("nextActionId"));
        this.nextActionIdEditBox.setValue(this.nextActionId);
        this.nextActionIdEditBox.setResponder(text -> this.nextActionId = text);
        this.nextActionIdEditBox.setTooltip(Tooltip.create(Component.translatable("envoys.setting.move.next_tooltip")));
        this.addRenderableWidget(this.nextActionIdEditBox);

        this.addRenderableWidget(Button.builder(
            Component.translatable("envoys.gui.common.back"),
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

    private EditBox createFloatBox(int centerX, int y, float initial) {
        EditBox box = new EditBox(this.font, centerX + 10, y, 120, 20, Component.literal("coordinate"));
        box.setValue(Float.toString(initial));
        box.setFilter(text -> text.chars().allMatch(c -> c == '-' || c == '.' || Character.isDigit(c)));
        return box;
    }

    private void save() {
        this.action.targetX = this.targetX;
        this.action.targetY = this.targetY;
        this.action.targetZ = this.targetZ;
        this.action.nextActionId = this.nextActionId.trim();
    }

    private float parseOrDefaultFloat(String text, float defaultValue) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 55;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.move.target_x"), centerX - 160, startY, 0xA0A0A0);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.move.target_y"), centerX - 160, startY + 30, 0xA0A0A0);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.move.target_z"), centerX - 160, startY + 60, 0xA0A0A0);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.move.next_action"), centerX - 160, startY + 90, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
