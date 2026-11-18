package com.frost.envoys.gui.screen.action;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TradeOptionScreen extends Screen {

    private static final Component TITLE = Component.translatable("envoys.setting.trade_option.title");
    private static final Component PRICE_LABEL = Component.translatable("envoys.setting.trade_option.price_label");
    private static final Component MAX_TRADES_LABEL = Component.translatable("envoys.setting.trade_option.max_trades_label");
    private static final Component RESET_TIME_LABEL = Component.translatable("envoys.setting.trade_option.reset_time_label");
    private static final Component HINT_1 = Component.translatable("envoys.setting.trade_option.max_trades_hint");
    private static final Component HINT_2 = Component.translatable("envoys.setting.trade_option.reset_time_hint");
    private static final Component SAVE_BTN = Component.translatable("envoys.gui.common.save");
    private static final Component BACK_BTN = Component.translatable("envoys.gui.common.back");

    private final Screen parentScreen;
    private final SettingTradeScreen.Trade trade;

    private EditBox priceMultiplierBox;
    private EditBox maxTradesBox;
    private EditBox resetTimeBox;

    private float priceMultiplier;
    private int maxTrades;
    private int resetTime;

    public TradeOptionScreen(Screen parentScreen, SettingTradeScreen.Trade trade) {
        super(TITLE);
        this.parentScreen = parentScreen;
        this.trade = trade;
        this.priceMultiplier = trade.getPriceMultiplier();
        this.maxTrades = trade.getMaxTrades();
        this.resetTime = trade.getResetTime();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int boxX = centerX + 40;
        int boxWidth = 70;
        int labelX = centerX - 150;

        this.priceMultiplierBox = new EditBox(this.font, boxX, 40, boxWidth, 18, Component.literal(""));
        this.priceMultiplierBox.setValue(String.valueOf(this.priceMultiplier));
        this.addRenderableWidget(this.priceMultiplierBox);

        this.maxTradesBox = new EditBox(this.font, boxX, 70, boxWidth, 18, Component.literal(""));
        this.maxTradesBox.setValue(String.valueOf(this.maxTrades));
        this.addRenderableWidget(this.maxTradesBox);

        this.resetTimeBox = new EditBox(this.font, boxX, 100, boxWidth, 18, Component.literal(""));
        this.resetTimeBox.setValue(String.valueOf(this.resetTime));
        this.addRenderableWidget(this.resetTimeBox);

        this.addRenderableWidget(Button.builder(SAVE_BTN, button -> this.save())
                .bounds(centerX - 100, this.height - 32, 95, 20).build());

        this.addRenderableWidget(Button.builder(BACK_BTN, button -> this.onClose())
                .bounds(centerX + 5, this.height - 32, 95, 20).build());
    }

    private void save() {
        try {
            float parsed = Float.parseFloat(this.priceMultiplierBox.getValue().trim());
            if (Float.isFinite(parsed) && parsed > 0f) {
                this.trade.setPriceMultiplier(parsed);
            }
        } catch (NumberFormatException ignored) {
        }
        try {
            this.trade.setMaxTrades(Integer.parseInt(this.maxTradesBox.getValue().trim()));
        } catch (NumberFormatException ignored) {
        }
        try {
            this.trade.setResetTime(Integer.parseInt(this.resetTimeBox.getValue().trim()));
        } catch (NumberFormatException ignored) {
        }
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int labelX = centerX - 150;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 10, 0xFFFFFF);

        guiGraphics.drawString(this.font, PRICE_LABEL, labelX, 45, 0xA0A0A0);
        guiGraphics.drawString(this.font, MAX_TRADES_LABEL, labelX, 75, 0xA0A0A0);
        guiGraphics.drawString(this.font, RESET_TIME_LABEL, labelX, 105, 0xA0A0A0);

        guiGraphics.drawCenteredString(this.font, HINT_1, centerX, 135, 0x808080);
        guiGraphics.drawCenteredString(this.font, HINT_2, centerX, 148, 0x808080);
    }

    @Override
    public void onClose() {
        if (this.parentScreen != null && this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
