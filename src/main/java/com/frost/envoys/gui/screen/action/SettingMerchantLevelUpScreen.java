package com.frost.envoys.gui.screen.action;

import com.frost.envoys.action.model.ActionMerchantLevelUp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingMerchantLevelUpScreen extends Screen {

    private static final Component NEXT_TOOLTIP = Component.translatable("envoys.setting.merchant_level_up.next_id_tooltip");

    private final Screen parentScreen;
    private final ActionMerchantLevelUp action;

    private EditBox nextActionIdEditBox;

    private String nextActionId = "";

    public SettingMerchantLevelUpScreen(Screen parentScreen, ActionMerchantLevelUp action) {
        super(Component.translatable("envoys.setting.merchant_level_up.title"));
        this.parentScreen = parentScreen;
        this.action = action;
        this.nextActionId = action.nextActionId != null ? action.nextActionId : "";
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 20;

        this.nextActionIdEditBox = new EditBox(this.font, centerX + 10, startY, 200, 20, Component.literal("nextActionId"));
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
        this.action.nextActionId = this.nextActionId.trim();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 20;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.merchant_level_up.next_action_label"), centerX - 160, startY + 6, 0xA0A0A0);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.merchant_level_up.description"), centerX - 160, startY + 36, 0x808080);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
