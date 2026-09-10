package com.frost.envoys.gui.screen.action;

import java.util.ArrayList;
import java.util.List;

import com.frost.envoys.action.model.ActionCommand;

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

public class SettingCommandScreen extends Screen {

    private final Screen parentScreen;
    private final ActionCommand action;

    private EditBox nextIdEditBox;
    private CommandList commandList;
    private Button addCommandButton;

    public SettingCommandScreen(Screen parentScreen, ActionCommand action) {
        super(Component.literal("Редактор команд"));
        this.parentScreen = parentScreen;
        this.action = action;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2;

        int listHeight = this.height - 130;
        this.commandList = new CommandList(this.minecraft, this.width, listHeight, 35, 26);
        this.addRenderableWidget(this.commandList);

        for (String cmd : this.action.commands) {
            this.commandList.addCommand(cmd);
        }

        this.addCommandButton = Button.builder(
            Component.literal("Добавить команду"), 
            button -> this.commandList.addCommand("")
        ).bounds(centerX - 100, this.height - 85, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.addCommandButton);

        this.nextIdEditBox = new EditBox(this.font, centerX + 40, this.height - 60, 60, 20, Component.literal("ID"));
        this.nextIdEditBox.setValue(this.action.nextActionId != null ? this.action.nextActionId : "");
        this.nextIdEditBox.setTooltip(Tooltip.create(Component.literal("ID действия, которое выполнится далее. Пусто — конец цепочки. Формат: id_N")));
        this.addRenderableWidget(this.nextIdEditBox);

        this.addRenderableWidget(Button.builder(
            Component.literal("Назад"), 
            button -> {
                this.saveData();
                this.onClose();
            }
        ).bounds(centerX - 100, this.height - 35, buttonWidth, buttonHeight).build());
    }

    private void saveData() {
        this.action.nextActionId = this.nextIdEditBox.getValue().trim();
        this.action.commands.clear();

        for (CommandEntry entry : this.commandList.children()) {
            String cmd = entry.commandField.getValue().trim();
            if (!cmd.isEmpty()) {
                this.action.commands.add(cmd);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "ID следующего действия:", centerX - 100, this.height - 55, 0xA0A0A0);
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

    class CommandList extends ContainerObjectSelectionList<CommandEntry> {
        public CommandList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public void addCommand(String initialCmd) {
            CommandEntry entry = new CommandEntry();
            entry.commandField.setValue(initialCmd);
            this.addEntry(entry);
        }

        public void removeCommand(CommandEntry entry) {
            this.removeEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return 280;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width / 2 + 150;
        }
    }

    class CommandEntry extends ContainerObjectSelectionList.Entry<CommandEntry> {
        final EditBox commandField;
        private final Button deleteButton;
        private final List<GuiEventListener> children = new ArrayList<>();

        public CommandEntry() {
            this.commandField = new EditBox(Minecraft.getInstance().font, 0, 0, 220, 20, Component.literal("Команда"));
            this.commandField.setMaxLength(256);

            this.deleteButton = Button.builder(Component.literal("✖"), button -> {
                SettingCommandScreen.this.commandList.removeCommand(this);
            }).bounds(0, 0, 20, 20).build();

            this.children.add(this.commandField);
            this.children.add(this.deleteButton);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.commandField, this.deleteButton);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            this.commandField.setX(left + 5);
            this.commandField.setY(top);

            this.deleteButton.setX(left + 235);
            this.deleteButton.setY(top);

            this.commandField.render(guiGraphics, mouseX, mouseY, partialTick);
            this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}