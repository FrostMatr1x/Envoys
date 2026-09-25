package com.frost.envoys.gui.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.quest.QuestDefinition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class QuestPickerScreen extends Screen {

    private final Screen parentScreen;
    private final NPCInteractManager manager;
    private final Consumer<String> onSelect;

    private QuestList questList;

    public QuestPickerScreen(Screen parentScreen, NPCInteractManager manager, Consumer<String> onSelect) {
        super(Component.literal("Выбор квеста"));
        this.parentScreen = parentScreen;
        this.manager = manager;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        int listHeight = this.height - 80;
        this.questList = new QuestList(this.minecraft, this.width, listHeight, 40, 24);
        this.addRenderableWidget(this.questList);

        if (this.manager != null && this.manager.quests != null) {
            for (QuestDefinition quest : this.manager.quests) {
                if (quest != null) {
                    this.questList.addQuest(quest);
                }
            }
        }

        this.addRenderableWidget(Button.builder(
            Component.literal("Отмена"),
            button -> {
                if (this.parentScreen != null) {
                    Minecraft.getInstance().setScreen(this.parentScreen);
                } else {
                    this.onClose();
                }
            }
        ).bounds(centerX - 100, this.height - 35, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        boolean empty = this.manager == null || this.manager.quests == null || this.manager.quests.isEmpty();
        if (empty) {
            guiGraphics.drawCenteredString(this.font, Component.literal("У квестов NPC ничего нет"), this.width / 2, this.height / 2, 0xFFFF5555);
        }
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

    class QuestList extends ContainerObjectSelectionList<QuestEntry> {
        public QuestList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public void addQuest(QuestDefinition quest) {
            this.addEntry(new QuestEntry(quest));
        }

        @Override
        public int getRowWidth() {
            return 320;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width / 2 + 170;
        }
    }

    class QuestEntry extends ContainerObjectSelectionList.Entry<QuestEntry> {
        private final Button selectButton;
        private final List<GuiEventListener> children = new ArrayList<>();

        public QuestEntry(QuestDefinition quest) {
            String localId = quest.localId != null ? quest.localId : "";
            String title = quest.title != null ? quest.title : "";
            String label = title.isBlank() ? localId : title;

            this.selectButton = Button.builder(Component.literal(label), button -> {
                if (QuestPickerScreen.this.onSelect != null) {
                    QuestPickerScreen.this.onSelect.accept(localId);
                }
                if (QuestPickerScreen.this.parentScreen != null) {
                    Minecraft.getInstance().setScreen(QuestPickerScreen.this.parentScreen);
                } else {
                    QuestPickerScreen.this.onClose();
                }
            }).bounds(0, 0, 240, 20).build();

            this.children.add(this.selectButton);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.selectButton);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            this.selectButton.setX(left + (width - 240) / 2);
            this.selectButton.setY(top);
            this.selectButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
