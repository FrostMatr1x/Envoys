package com.frost.envoys.gui.screen.action;

import com.frost.envoys.action.model.ActionDelay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingDelayScreen extends Screen {

    private static final String NEXT_TOOLTIP = "ID действия, которое выполнится далее. Пусто — конец цепочки. Формат: id_N";
    private static final char[] UNITS = { 'h', 'm', 's', 't' };

    private final Screen parentScreen;
    private final ActionDelay action;

    private EditBox durationEditBox;
    private Button timeUnitButton;
    private EditBox nextActionIdEditBox;

    private int duration;
    private char timeUnit;
    private String nextActionId = "";

    public SettingDelayScreen(Screen parentScreen, ActionDelay action) {
        super(Component.literal("Настройка ожидания"));
        this.parentScreen = parentScreen;
        this.action = action;
        this.duration = action.duration;
        this.timeUnit = action.timeUnit;
        this.nextActionId = action.nextActionId != null ? action.nextActionId : "";
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        this.durationEditBox = new EditBox(this.font, centerX + 10, startY - 2, 80, 20, Component.literal("duration"));
        this.durationEditBox.setValue(Integer.toString(this.duration));
        this.durationEditBox.setFilter(text -> text.chars().allMatch(Character::isDigit));
        this.durationEditBox.setResponder(text -> this.duration = parseOrDefaultInt(text, this.duration));
        this.addRenderableWidget(this.durationEditBox);

        this.timeUnitButton = Button.builder(
            Component.literal("Единица: " + unitLabel(this.timeUnit)),
            button -> {
                int currentIndex = 0;
                for (int i = 0; i < UNITS.length; i++) {
                    if (UNITS[i] == this.timeUnit) {
                        currentIndex = i;
                        break;
                    }
                }
                int nextIndex = (currentIndex + 1) % UNITS.length;
                this.timeUnit = UNITS[nextIndex];
                button.setMessage(Component.literal("Единица: " + unitLabel(this.timeUnit)));
            }
        ).bounds(centerX + 100, startY - 2, 110, 20).build();
        this.addRenderableWidget(this.timeUnitButton);

        this.nextActionIdEditBox = new EditBox(this.font, centerX + 10, startY + 28, 200, 20, Component.literal("nextActionId"));
        this.nextActionIdEditBox.setValue(this.nextActionId);
        this.nextActionIdEditBox.setResponder(text -> this.nextActionId = text);
        this.nextActionIdEditBox.setTooltip(Tooltip.create(Component.literal(NEXT_TOOLTIP)));
        this.addRenderableWidget(this.nextActionIdEditBox);

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
        this.action.duration = Math.max(0, this.duration);
        this.action.timeUnit = this.timeUnit;
        this.action.nextActionId = this.nextActionId.trim();
    }

    private int parseOrDefaultInt(String text, int defaultValue) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String unitLabel(char unit) {
        return switch (unit) {
            case 'h' -> "часы";
            case 'm' -> "минуты";
            case 't' -> "тики";
            default -> "секунды";
        };
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Длительность:", centerX - 160, startY, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Следующее действие:", centerX - 160, startY + 30, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
