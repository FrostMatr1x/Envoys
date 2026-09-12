package com.frost.envoys.gui.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.NPCScriptData;
import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.frost.envoys.network.payload.SaveNPCScriptPayload;
import com.frost.envoys.quest.QuestDefinition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class QuestManagementScreen extends Screen {

    private final Screen parentScreen;
    private final NPCInteractManager manager;
    private final boolean isCreativeTuner;

    private QuestList questList;

    public QuestManagementScreen(Screen parentScreen, NPCInteractManager manager, boolean isCreativeTuner) {
        super(Component.literal("Квесты NPC"));
        this.parentScreen = parentScreen;
        this.manager = manager;
        this.isCreativeTuner = isCreativeTuner;
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
            Component.literal("Создать"),
            button -> Minecraft.getInstance().setScreen(new QuestEditScreen(this, this.manager, null, true))
        ).bounds(centerX - 105, this.height - 35, 100, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Назад"),
            button -> {
                this.saveAndSync();
                if (this.parentScreen != null) {
                    Minecraft.getInstance().setScreen(this.parentScreen);
                } else {
                    this.onClose();
                }
            }
        ).bounds(centerX + 5, this.height - 35, 100, 20).build());
    }

    private void refreshList() {
        if (this.questList != null) {
            this.removeWidget(this.questList);
        }

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
    }

    private void move(QuestDefinition quest, int delta) {
        if (this.manager == null || this.manager.quests == null || quest == null) {
            return;
        }
        int index = this.manager.quests.indexOf(quest);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= this.manager.quests.size()) {
            return;
        }
        Collections.swap(this.manager.quests, index, target);
        this.refreshList();
    }

    private void saveAndSync() {
        if (this.manager == null) {
            return;
        }
        NPCScriptData scriptData = NPCScriptData.fromManager(this.manager);
        String json = EntityActionAdapter.GSON.toJson(scriptData);

        PacketDistributor.sendToServer(
            new SaveNPCScriptPayload(this.manager.npcUUID, json)
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        boolean empty = this.manager == null || this.manager.quests == null || this.manager.quests.isEmpty();
        if (empty) {
            guiGraphics.drawCenteredString(this.font, Component.literal("Квестов нет"), this.width / 2, this.height / 2, 0xA0A0A0);
        }
    }

    @Override
    public void onClose() {
        this.saveAndSync();
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
        private final QuestDefinition quest;
        private final Button upButton;
        private final Button downButton;
        private final Button editButton;
        private final Button deleteButton;
        private final List<GuiEventListener> children = new ArrayList<>();

        public QuestEntry(QuestDefinition quest) {
            this.quest = quest;

            this.upButton = Button.builder(Component.literal("▲"), button -> QuestManagementScreen.this.move(this.quest, -1))
                .bounds(0, 0, 20, 20).build();
            this.downButton = Button.builder(Component.literal("▼"), button -> QuestManagementScreen.this.move(this.quest, 1))
                .bounds(0, 0, 20, 20).build();
            this.editButton = Button.builder(Component.literal("Изменить"), button ->
                Minecraft.getInstance().setScreen(new QuestEditScreen(QuestManagementScreen.this, QuestManagementScreen.this.manager, this.quest, false))
            ).bounds(0, 0, 75, 20).build();
            this.deleteButton = Button.builder(Component.literal("Удалить"), button -> {
                if (QuestManagementScreen.this.manager != null && QuestManagementScreen.this.manager.quests != null) {
                    QuestManagementScreen.this.manager.quests.remove(this.quest);
                }
                QuestManagementScreen.this.refreshList();
            }).bounds(0, 0, 60, 20).build();

            this.children.add(this.upButton);
            this.children.add(this.downButton);
            this.children.add(this.editButton);
            this.children.add(this.deleteButton);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.upButton, this.downButton, this.editButton, this.deleteButton);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            String localId = this.quest.localId != null ? this.quest.localId : "";
            String title = this.quest.title != null ? this.quest.title : "";
            String uuid = this.quest.questUuid != null ? this.quest.questUuid : "";
            String label = "[" + localId + "] " + title + " — " + this.quest.type + " (" + uuid + ")";
            guiGraphics.drawString(Minecraft.getInstance().font, label, left + 5, top + (height - 8) / 2, 0xFFFFFF, false);

            this.upButton.setX(left + width - 187);
            this.upButton.setY(top);
            this.downButton.setX(left + width - 165);
            this.downButton.setY(top);
            this.editButton.setX(left + width - 145);
            this.editButton.setY(top);
            this.deleteButton.setX(left + width - 65);
            this.deleteButton.setY(top);

            this.upButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.downButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.editButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
