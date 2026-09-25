package com.frost.envoys.gui.screen.action;

import com.frost.envoys.client.gui.script.GraphNode;
import com.frost.envoys.client.gui.script.ScriptNodeTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingLookAtScreen extends Screen {

    private final Screen parentScreen;
    private final GraphNode node;

    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;
    private EditBox nextBox;
    private Button modeButton;

    private boolean coordsMode;
    private float x;
    private float y;
    private float z;
    private String nextId;

    public SettingLookAtScreen(Screen parentScreen, GraphNode node) {
        super(Component.literal("Настройка поворота"));
        this.parentScreen = parentScreen;
        this.node = node;
        this.coordsMode = "coords".equals(node.param("mode", "player"));
        this.x = node.floatParam("x", 0.0f);
        this.y = node.floatParam("y", 0.0f);
        this.z = node.floatParam("z", 0.0f);
        this.nextId = node.nextId == null ? "" : node.nextId;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 55;

        this.modeButton = Button.builder(modeLabel(), button -> {
            this.coordsMode = !this.coordsMode;
            button.setMessage(modeLabel());
            updateVisibility();
        }).bounds(centerX - 100, startY - 30, 200, 20).build();
        this.addRenderableWidget(this.modeButton);

        this.xBox = floatBox(centerX, startY, this.x);
        this.xBox.setResponder(text -> this.x = parseFloat(text, this.x));
        this.addRenderableWidget(this.xBox);

        this.yBox = floatBox(centerX, startY + 28, this.y);
        this.yBox.setResponder(text -> this.y = parseFloat(text, this.y));
        this.addRenderableWidget(this.yBox);

        this.zBox = floatBox(centerX, startY + 56, this.z);
        this.zBox.setResponder(text -> this.z = parseFloat(text, this.z));
        this.addRenderableWidget(this.zBox);

        this.nextBox = new EditBox(this.font, centerX + 10, startY + 90, 200, 20, Component.literal("next"));
        this.nextBox.setValue(this.nextId);
        this.nextBox.setResponder(text -> this.nextId = text);
        this.addRenderableWidget(this.nextBox);

        updateVisibility();

        this.addRenderableWidget(Button.builder(Component.literal("Назад"), button -> {
            this.save();
            if (this.parentScreen != null) {
                Minecraft.getInstance().setScreen(this.parentScreen);
            } else {
                this.onClose();
            }
        }).bounds(centerX - 100, this.height - 35, 200, 20).build());
    }

    private Component modeLabel() {
        return Component.literal("Цель: " + (this.coordsMode ? "координаты" : "игрок события"));
    }

    private void updateVisibility() {
        boolean coords = this.coordsMode;
        if (this.xBox != null) {
            this.xBox.setVisible(coords);
        }
        if (this.yBox != null) {
            this.yBox.setVisible(coords);
        }
        if (this.zBox != null) {
            this.zBox.setVisible(coords);
        }
    }

    private EditBox floatBox(int centerX, int yPos, float initial) {
        EditBox box = new EditBox(this.font, centerX + 10, yPos, 120, 20, Component.literal("coordinate"));
        box.setValue(Float.toString(initial));
        box.setFilter(text -> text.chars().allMatch(c -> c == '-' || c == '.' || Character.isDigit(c)));
        return box;
    }

    private void save() {
        this.node.params.put("mode", this.coordsMode ? "coords" : "player");
        this.node.params.put("x", Float.toString(this.x));
        this.node.params.put("y", Float.toString(this.y));
        this.node.params.put("z", Float.toString(this.z));
        this.node.nextId = this.nextId == null || this.nextId.isBlank() ? null : this.nextId.trim();
    }

    private static float parseFloat(String text, float fallback) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int startY = this.height / 2 - 55;
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Тип: " + ScriptNodeTypes.displayName(this.node.type),
                centerX - 160, startY - 45, 0xA0A0A0);
        if (this.coordsMode) {
            guiGraphics.drawString(this.font, "X:", centerX - 160, startY + 4, 0xA0A0A0);
            guiGraphics.drawString(this.font, "Y:", centerX - 160, startY + 32, 0xA0A0A0);
            guiGraphics.drawString(this.font, "Z:", centerX - 160, startY + 60, 0xA0A0A0);
        }
        guiGraphics.drawString(this.font, "Следующее действие:", centerX - 160, startY + 92, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
