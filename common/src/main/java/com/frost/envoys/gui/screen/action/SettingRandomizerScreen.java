package com.frost.envoys.gui.screen.action;

import java.util.ArrayList;
import java.util.List;

import com.frost.envoys.action.model.ActionRandomizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingRandomizerScreen extends Screen {

    private static final Component TOOLTIP = Component.translatable("envoys.setting.randomizer.tooltip");

    private static final int MAX_OPTIONS = 10;
    private static final int ROW_HEIGHT = 24;

    private final Screen parentScreen;
    private final ActionRandomizer action;

    private final List<String> options = new ArrayList<>();
    private final List<OptionRow> rows = new ArrayList<>();

    private Button addButton;

    public SettingRandomizerScreen(Screen parentScreen, ActionRandomizer action) {
        super(Component.translatable("envoys.setting.randomizer.title"));
        this.parentScreen = parentScreen;
        this.action = action;
        if (action.options != null) {
            this.options.addAll(action.options);
        }
        if (this.options.isEmpty()) {
            this.options.add("");
        }
    }

    @Override
    protected void init() {
        super.init();
        this.rows.clear();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 55;

        for (int i = 0; i < this.options.size(); i++) {
            int rowIndex = i;
            int y = startY + i * ROW_HEIGHT;

            EditBox field = new EditBox(this.font, centerX - 145, y, 200, 20, Component.literal("option"));
            field.setValue(this.options.get(i));
            field.setTooltip(Tooltip.create(TOOLTIP));
            field.setResponder(text -> {
                if (rowIndex < this.options.size()) {
                    this.options.set(rowIndex, text);
                }
            });
            this.addRenderableWidget(field);

            Button removeButton = Button.builder(
                Component.literal("✕"),
                btn -> {
                    if (this.options.size() > 1) {
                        this.removeOption(rowIndex);
                    }
                }
            ).bounds(centerX + 70, y, 20, 20).build();
            removeButton.active = true;
            removeButton.visible = this.options.size() > 1;
            this.addRenderableWidget(removeButton);

            this.rows.add(new OptionRow(field, removeButton));
        }

        this.addButton = Button.builder(
            Component.literal("+"),
            btn -> this.addOption()
        ).bounds(centerX + 100, startY, 20, 20).build();
        this.updateAddButtonState();
        this.addRenderableWidget(this.addButton);

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

    private void addOption() {
        if (this.options.size() >= MAX_OPTIONS) {
            return;
        }
        this.options.add("");
        this.rebuild();
    }

    private void removeOption(int index) {
        if (this.options.size() <= 1) {
            return;
        }
        this.options.remove(index);
        this.rebuild();
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    private void updateAddButtonState() {
        if (this.addButton != null) {
            this.addButton.active = this.options.size() < MAX_OPTIONS;
        }
    }

    private void save() {
        for (OptionRow row : this.rows) {
            String text = row.field.getValue();
            if (text != null) {
                int idx = this.rows.indexOf(row);
                if (idx >= 0 && idx < this.options.size()) {
                    this.options.set(idx, text);
                }
            }
        }
        this.action.options.clear();
        this.action.options.addAll(this.options);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 55;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("envoys.setting.randomizer.subtitle"), centerX, startY - 18, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class OptionRow {
        final EditBox field;
        final Button removeButton;

        OptionRow(EditBox field, Button removeButton) {
            this.field = field;
            this.removeButton = removeButton;
        }
    }
}
