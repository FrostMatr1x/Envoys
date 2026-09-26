package com.frost.envoys.gui.screen.action;

import com.frost.envoys.action.model.ActionChat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingChatScreen extends Screen {

    private static final Component NEXT_TOOLTIP = Component.translatable("envoys.setting.chat.next_id_tooltip");

    private final Screen parentScreen;
    private final ActionChat action;

    private EditBox messageEditBox;
    private Checkbox isGlobalCheckbox;
    private EditBox nextActionIdEditBox;

    private String message = "";
    private boolean isGlobal;
    private String nextActionId = "";

    public SettingChatScreen(Screen parentScreen, ActionChat action) {
        super(Component.translatable("envoys.setting.chat.title"));
        this.parentScreen = parentScreen;
        this.action = action;
        this.message = action.message != null ? action.message : "";
        this.isGlobal = action.isGlobal;
        this.nextActionId = action.nextActionId != null ? action.nextActionId : "";
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        this.messageEditBox = new EditBox(this.font, centerX + 10, startY - 2, 200, 20, Component.literal("message"));
        this.messageEditBox.setValue(this.message);
        this.messageEditBox.setResponder(text -> this.message = text);
        this.messageEditBox.setMaxLength(256);
        this.addRenderableWidget(this.messageEditBox);

        this.isGlobalCheckbox = Checkbox.builder(Component.translatable("envoys.setting.chat.global"), this.font)
            .pos(centerX - 160, startY + 25)
            .selected(this.isGlobal)
            .onValueChange((checkbox, selected) -> this.isGlobal = selected)
            .build();
        this.addRenderableWidget(this.isGlobalCheckbox);

        this.nextActionIdEditBox = new EditBox(this.font, centerX + 10, startY + 58, 200, 20, Component.literal("nextActionId"));
        this.nextActionIdEditBox.setValue(this.nextActionId);
        this.nextActionIdEditBox.setResponder(text -> this.nextActionId = text);
        this.nextActionIdEditBox.setTooltip(Tooltip.create(NEXT_TOOLTIP));
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
        this.action.message = this.message.trim();
        this.action.isGlobal = this.isGlobal;
        this.action.nextActionId = this.nextActionId.trim();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.chat.message_label"), centerX - 160, startY, 0xA0A0A0);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.chat.next_action_label"), centerX - 160, startY + 60, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
