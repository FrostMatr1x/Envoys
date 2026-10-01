package com.frost.envoys.gui.screen.action;

import com.frost.envoys.client.gui.script.GraphNode;
import com.frost.envoys.client.gui.script.ScriptNodeTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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
    private final List<EditBox> answerBoxes = new ArrayList<>();
    private final List<Button> deleteButtons = new ArrayList<>();

    private EditBox textBox;
    private EditBox questBox;
    private EditBox nextBox;

    private String text;
    private String quest;
    private String nextId;

    public SettingQuestCheckScreen(Screen parentScreen, GraphNode node) {
        super(Component.translatable("envoys.setting.branch.title"));
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
            node.options.add(new GraphNode.BranchOption("completed", "Completed", null));
            node.options.add(new GraphNode.BranchOption("not_completed", "Not completed", null));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.headBoxes.clear();
        this.answerBoxes.clear();
        this.deleteButtons.clear();

        int centerX = this.width / 2;
        int labelX = centerX + LABEL_X_OFFSET;
        int fieldX = centerX + FIELD_X_OFFSET;
        int top = 40;

        if (ScriptNodeTypes.DIALOGUE.equals(node.type)) {
            this.textBox = new EditBox(this.font, fieldX, top, 180, 20, Component.literal("text"));
            this.textBox.setMaxLength(1028);
            this.textBox.setValue(this.text);
            this.textBox.setResponder(t -> this.text = t);
            this.addRenderableWidget(this.textBox);
            top += ROW_HEIGHT;
        }
        if (ScriptNodeTypes.QUEST_CHECK.equals(node.type)) {
            this.questBox = new EditBox(this.font, fieldX, top, 180, 20, Component.literal("quest"));
            this.questBox.setMaxLength(128);
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

        boolean dialogue = ScriptNodeTypes.DIALOGUE.equals(node.type);
        boolean deletable = !ScriptNodeTypes.QUEST_CHECK.equals(node.type);
        for (int i = 0; i < node.options.size(); i++) {
            int rowY = top + i * ROW_HEIGHT;
            GraphNode.BranchOption option = node.options.get(i);

            if (dialogue) {
                EditBox answer = new EditBox(this.font, fieldX, rowY, 118, 20, Component.literal("answer"));
                answer.setMaxLength(1028);
                answer.setValue(option.label == null ? "" : option.label);
                this.addRenderableWidget(answer);
                answerBoxes.add(answer);
            }

            int headX = dialogue ? fieldX + 122 : fieldX;
            int headWidth = dialogue ? 60 : 120;
            EditBox head = new EditBox(this.font, headX, rowY, headWidth, 20, Component.literal("head"));
            head.setMaxLength(64);
            head.setValue(option.headId == null ? "" : option.headId);
            head.setTooltip(Tooltip.create(Component.translatable("envoys.setting.branch.head_tooltip")));
            this.addRenderableWidget(head);
            headBoxes.add(head);

            if (deletable) {
                final int index = i;
                int deleteX = dialogue ? fieldX + 187 : fieldX + 125;
                Button delete = Button.builder(Component.literal("✖"), button -> {
                    this.save();
                    if (index >= 0 && index < node.options.size()) {
                        node.options.remove(index);
                    }
                    this.rebuildWidgets();
                }).bounds(deleteX, rowY, 20, 20).build();
                this.addRenderableWidget(delete);
                deleteButtons.add(delete);
            }
        }

        if (!ScriptNodeTypes.QUEST_CHECK.equals(node.type)) {
            this.addRenderableWidget(Button.builder(Component.translatable("envoys.setting.branch.add_option"), button -> {
                this.save();
                int index = node.options.size() + 1;
                String key = ScriptNodeTypes.RANDOM.equals(node.type) ? Integer.toString(index) : "opt" + index;
                node.options.add(new GraphNode.BranchOption(key, "Option " + index, null));
                this.rebuildWidgets();
            }).bounds(labelX, top + node.options.size() * ROW_HEIGHT + 6, 90, 20).build());
        }

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

            if (i < answerBoxes.size()) {
                String label = answerBoxes.get(i).getValue().trim();
                if (!label.isEmpty()) {
                    node.options.get(i).label = label;
                }
            }
        }
    }

    private Component optionLabel(int index) {
        if (ScriptNodeTypes.QUEST_CHECK.equals(node.type)) {
            return Component.translatable(index == 0
                    ? "envoys.setting.branch.completed" : "envoys.setting.branch.not_completed");
        }
        return Component.translatable("envoys.setting.branch.option_n", index + 1);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int labelX = centerX + LABEL_X_OFFSET;
        int top = 40;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.branch.type",
                ScriptNodeTypes.displayName(node.type)), labelX, 24, 0xA0A0A0);

        if (ScriptNodeTypes.DIALOGUE.equals(node.type)) {
            guiGraphics.drawString(this.font, Component.translatable("envoys.setting.branch.text"), labelX, top + 6, 0xA0A0A0);
            top += ROW_HEIGHT;
        }
        if (ScriptNodeTypes.QUEST_CHECK.equals(node.type)) {
            guiGraphics.drawString(this.font, Component.translatable("envoys.setting.branch.quest"), labelX, top + 6, 0xA0A0A0);
            top += ROW_HEIGHT;
        }
        guiGraphics.drawString(this.font, Component.translatable("envoys.setting.branch.next"), labelX, top + 6, 0xA0A0A0);
        top += ROW_HEIGHT + 6;

        for (int i = 0; i < node.options.size(); i++) {
            int rowY = top + i * ROW_HEIGHT;
            Component label = ScriptNodeTypes.DIALOGUE.equals(node.type)
                    ? Component.translatable("envoys.setting.branch.answer_n", i + 1)
                    : optionLabel(i);
            guiGraphics.drawString(this.font, label, labelX, rowY + 6, 0xA0A0A0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
