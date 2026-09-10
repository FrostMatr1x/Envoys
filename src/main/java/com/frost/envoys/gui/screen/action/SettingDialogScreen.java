package com.frost.envoys.gui.screen.action;

import java.util.ArrayList;
import java.util.List;

import com.frost.envoys.action.model.ActionDialog;
import com.frost.envoys.util.ColorUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingDialogScreen extends Screen {

    private final Screen parentScreen;
    private EditBox phraseEditBox;
    private OptionList optionList;
    private Button addOptionButton;

    private final ActionDialog action;

    public SettingDialogScreen(Screen parentScreen, ActionDialog action) {
        super(Component.literal("Редактор диалогов"));
        this.parentScreen = parentScreen;
        this.action = action;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;

        this.phraseEditBox = new EditBox(this.font, centerX, 35, buttonWidth, 20, Component.literal("Текст фразы"));
        this.phraseEditBox.setMaxLength(1028);

        this.phraseEditBox.setValue(this.action.npcMessage != null ? ColorUtils.toFormattedString(this.action.npcMessage) : "");
        this.addRenderableWidget(this.phraseEditBox);

        int listHeight = this.height - 150;
        this.optionList = new OptionList(this.minecraft, this.width, listHeight, 75, 26);
        this.addRenderableWidget(this.optionList);

        this.action.answers.forEach((answerText, nextActionId) -> {
            this.optionList.addOption(answerText, nextActionId);
        });

        this.addOptionButton = Button.builder(
            Component.literal("Добавить вариант"), 
            button -> {
                if (this.optionList.getOptionCount() < 4) {
                    this.optionList.addOption("", "");
                }
                button.active = this.optionList.getOptionCount() < 4;
            }
        ).bounds(centerX, this.height - 65, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.addOptionButton);

        this.addRenderableWidget(Button.builder(
            Component.literal("Назад"), 
            button -> {
                this.saveData();
                
                if (this.parentScreen != null) {
                    Minecraft.getInstance().setScreen(this.parentScreen);
                } else {
                    this.onClose();
                }
            }
        ).bounds(centerX, this.height - 40, buttonWidth, buttonHeight).build());

        this.addOptionButton.active = this.optionList.getOptionCount() < 4;
    }

    private void saveData() {
        this.action.npcMessage = ColorUtils.parse(this.phraseEditBox.getValue().trim());
        
        this.action.answers.clear();
        for (OptionEntry entry : this.optionList.children()) {
            String text = entry.optionTextField.getValue().trim();
            String nextId = entry.nextIdField.getValue().trim();
            if (!text.isEmpty()) {
                this.action.answers.put(text, nextId);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        int centerX = this.width / 2 - 200 / 2;
        guiGraphics.drawString(this.font, "Текст фразы NPC:", centerX, 23, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    class OptionList extends ContainerObjectSelectionList<OptionEntry> {
        public OptionList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public void addOption(String initialText, String initialId) {
            if (this.getOptionCount() < 4) {
                OptionEntry entry = new OptionEntry();
                entry.optionTextField.setValue(initialText);
                entry.nextIdField.setValue(initialId);
                this.addEntry(entry);
            }
        }

        public void removeOption(OptionEntry entry) {
            this.removeEntry(entry);
        }

        public int getOptionCount() {
            return this.children().size();
        }

        @Override
        public int getRowWidth() {
            return 290;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width / 2 + 155;
        }
    }

    class OptionEntry extends ContainerObjectSelectionList.Entry<OptionEntry> {
        final EditBox optionTextField;
        final EditBox nextIdField;
        private final Button deleteButton;
        private final List<GuiEventListener> children = new ArrayList<>();

        public OptionEntry() {
            this.optionTextField = new EditBox(Minecraft.getInstance().font, 0, 0, 160, 20, Component.literal("Ответ"));
            this.optionTextField.setMaxLength(128);

            this.nextIdField = new EditBox(Minecraft.getInstance().font, 0, 0, 45, 20, Component.literal("ID"));
            this.nextIdField.setMaxLength(10);
            this.nextIdField.setTooltip(Tooltip.create(Component.literal("ID действия, которое выполнится далее. Пусто — конец цепочки. Формат: id_N")));

            this.deleteButton = Button.builder(Component.literal("✖"), button -> {
                SettingDialogScreen.this.optionList.removeOption(this);
                SettingDialogScreen.this.addOptionButton.active = true;
            }).bounds(0, 0, 20, 20).build();

            this.children.add(this.optionTextField);
            this.children.add(this.nextIdField);
            this.children.add(this.deleteButton);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.optionTextField, this.nextIdField, this.deleteButton);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            this.optionTextField.setX(left + 5);
            this.optionTextField.setY(top);

            this.nextIdField.setX(left + 170);
            this.nextIdField.setY(top);

            this.deleteButton.setX(left + 220);
            this.deleteButton.setY(top);

            this.optionTextField.render(guiGraphics, mouseX, mouseY, partialTick);
            this.nextIdField.render(guiGraphics, mouseX, mouseY, partialTick);
            this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}