package com.frost.envoys.gui.screen.action;

import com.frost.envoys.client.gui.script.GraphNode;
import com.frost.envoys.client.gui.script.ScriptNodeTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SettingQuestCheckScreen extends Screen {

    private static final int ROW_HEIGHT = 26;
    private static final int LABEL_X_OFFSET = -190;
    private static final int FIELD_X_OFFSET = -100;

    private final Screen parentScreen;
    private final GraphNode node;
    private final List<EditBox> headBoxes = new ArrayList<>();

    private EditBox textBox;
    private EditBox questBox;
    private EditBox nextBox;

    private String text;
    private String quest;
    private String nextId;

    public SettingQuestCheckScreen(Screen parentScreen, GraphNode node) {
        super(Component.literal("Настройка развилки"));
        this.parentScreen = parentScreen;
        this.node = node;
        this.text = node.param("text", "");
        this.quest = node.param("quest", "");
        this.nextId = node.nextId == null ? "" : node.nextId;
        ensureDefaultOptions();
    }

    private void ensureDefaultOptions() {
        if (ScriptNodeTypes.QUEST_CHECK.equals(node.type) && node.options.size() < 2) {
            node.options.clear();
            node.options.add(new GraphNode.BranchOption("completed", "Выполнен", null));
            node.options.add(new GraphNode.BranchOption("not_completed", "Не выполнен", null));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.headBoxes.clear();

        int centerX = this.width / 2;
        int labelX = centerX + LABEL_X_OFFSET;
        int fieldX = centerX + FIELD_X_OFFSET;
        int top = 40;

        if (ScriptNodeTypes.DIALOGUE.equals(node.type)) {
            this.textBox = new EditBox(this.font, fieldX, top, 180, 20, Component.literal("text"));
            this.textBox.setValue(this.text);
            this.textBox.setResponder(t -> this.text = t);
            this.addRenderableWidget(this.textBox);
            top += ROW_HEIGHT;
        }
        if (ScriptNodeTypes.QUEST_CHECK.equals(node.type)) {
            this.questBox = new EditBox(this.font, fieldX, top, 180, 20, Component.literal("quest"));
            this.questBox.setValue(this.quest);
            this.questBox.setResponder(t -> this.quest = t);
            this.addRenderableWidget(this.questBox);
            top += ROW_HEIGHT;
        }

        this.nextBox = new EditBox(this.font, fieldX, top, 120, 20, Component.literal("next"));
        this.nextBox.setValue(this.nextId);
        this.nextBox.setResponder(t -> this.nextId = t);
        this.addRenderableWidget(this.nextBox);
        top += ROW_HEIGHT + 6;

        for (int i = 0; i < node.options.size(); i++) {
            int rowY = top + i * ROW_HEIGHT;
            EditBox head = new EditBox(this.font, fieldX, rowY, 120, 20, Component.literal("head"));
            head.setValue(node.options.get(i).headId == null ? "" : node.options.get(i).headId);
            this.addRenderableWidget(head);
            headBoxes.add(head);
        }

        if (!ScriptNodeTypes.QUEST_CHECK.equals(node.type)) {
            this.addRenderableWidget(Button.builder(Component.literal("+ Ветка"), button -> {
                this.save();
                int index = node.options.size() + 1;
                String key = ScriptNodeTypes.RANDOM.equals(node.type) ? Integer.toString(index) : "opt" + index;
                node.options.add(new GraphNode.BranchOption(key, "Вариант " + index, null));
                this.rebuildWidgets();
            }).bounds(labelX, top + node.options.size() * ROW_HEIGHT + 6, 90, 20).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("Назад"), button -> {
            this.save();
            if (this.parentScreen != null) {
                Minecraft.getInstance().setScreen(this.parentScreen);
            } else {
                this.onClose();
            }
        }).bounds(centerX - 100, this.height - 35, 200, 20).build());
    }

    private void save() {
        if (this.textBox != null) {
            node.params.put("text", this.text == null ? "" : this.text);
        }
        if (this.questBox != null) {
            node.params.put("quest", this.quest == null ? "" : this.quest);
        }
        node.nextId = this.nextId == null || this.nextId.isBlank() ? null : this.nextId.trim();

        for (int i = 0; i < headBoxes.size() && i < node.options.size(); i++) {
            String head = headBoxes.get(i).getValue().trim();
            node.options.get(i).headId = head.isEmpty() ? null : head;
        }
    }

    private String optionLabel(int index) {
        if (ScriptNodeTypes.QUEST_CHECK.equals(node.type)) {
            return index == 0 ? "Выполнено:" : "НЕ выполнено:";
        }
        return "Ветка #" + (index + 1) + ":";
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int labelX = centerX + LABEL_X_OFFSET;
        int top = 40;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Тип: " + ScriptNodeTypes.displayName(node.type), labelX, 24, 0xA0A0A0);

        if (ScriptNodeTypes.DIALOGUE.equals(node.type)) {
            guiGraphics.drawString(this.font, "Текст:", labelX, top + 6, 0xA0A0A0);
            top += ROW_HEIGHT;
        }
        if (ScriptNodeTypes.QUEST_CHECK.equals(node.type)) {
            guiGraphics.drawString(this.font, "Квест:", labelX, top + 6, 0xA0A0A0);
            top += ROW_HEIGHT;
        }
        guiGraphics.drawString(this.font, "Далее:", labelX, top + 6, 0xA0A0A0);
        top += ROW_HEIGHT + 6;

        for (int i = 0; i < node.options.size(); i++) {
            int rowY = top + i * ROW_HEIGHT;
            guiGraphics.drawString(this.font, optionLabel(i), labelX, rowY + 6, 0xA0A0A0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
