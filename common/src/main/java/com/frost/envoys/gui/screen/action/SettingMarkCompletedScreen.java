package com.frost.envoys.gui.screen.action;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.model.ActionQuestMarkCompleted;
import com.frost.envoys.gui.screen.QuestPickerScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingMarkCompletedScreen extends Screen {

    private final Screen parentScreen;
    private final ActionQuestMarkCompleted action;
    private final NPCInteractManager manager;

    private EditBox questTargetEditBox;
    private EditBox nextActionIdEditBox;

    private String questTarget = "";
    private String nextActionId = "";

    public SettingMarkCompletedScreen(Screen parentScreen, ActionQuestMarkCompleted action, NPCInteractManager manager) {
        super(Component.translatable("envoys.setting.mark_completed.title"));
        this.parentScreen = parentScreen;
        this.action = action;
        this.manager = manager;
        this.questTarget = action.questTarget != null ? action.questTarget : "";
        this.nextActionId = action.nextActionId != null ? action.nextActionId : "";
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        this.questTargetEditBox = new EditBox(this.font, centerX + 10, startY - 2, 80, 20, Component.literal("questTarget"));
        this.questTargetEditBox.setMaxLength(128);
        this.questTargetEditBox.setValue(this.questTarget);
        this.questTargetEditBox.setResponder(text -> this.questTarget = text);
        this.questTargetEditBox.setTooltip(Tooltip.create(Component.translatable("envoys.setting.mark_completed.target_tooltip")));
        this.addRenderableWidget(this.questTargetEditBox);

        this.addRenderableWidget(Button.builder(
            Component.translatable("envoys.setting.mark_completed.select"),
            button -> Minecraft.getInstance().setScreen(new QuestPickerScreen(this, this.manager, selected -> {
                this.questTarget = selected;
                if (this.questTargetEditBox != null) {
                    this.questTargetEditBox.setValue(selected);
                }
            }))
        ).bounds(centerX + 110, startY - 2, 100, 20).build());

        this.nextActionIdEditBox = new EditBox(this.font, centerX + 10, startY + 28, 200, 20, Component.literal("nextActionId"));
        this.nextActionIdEditBox.setValue(this.nextActionId);
        this.nextActionIdEditBox.setResponder(text -> this.nextActionId = text);
        this.nextActionIdEditBox.setTooltip(Tooltip.create(Component.translatable("envoys.setting.mark_completed.next_tooltip")));
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

    private void save() {
        this.action.questTarget = this.questTarget.trim();
        this.action.nextActionId = this.nextActionId.trim();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.mark_completed.quest_target"), centerX - 160, startY, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.mark_completed.next_action"), centerX - 160, startY + 30, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
