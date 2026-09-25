package com.frost.envoys.gui.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.model.ActionChat;
import com.frost.envoys.action.model.ActionCommand;
import com.frost.envoys.action.model.ActionDelay;
import com.frost.envoys.action.model.ActionMove;
import com.frost.envoys.action.model.ActionQuestAdvanceStep;
import com.frost.envoys.action.model.ActionQuestGive;
import com.frost.envoys.action.model.ActionLoadPoint;
import com.frost.envoys.action.model.ActionQuestMarkCompleted;
import com.frost.envoys.action.model.ActionSavePoint;
import com.frost.envoys.action.model.ActionStart;
import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.action.model.EntityActionData;
import com.frost.envoys.client.gui.script.ActionGraph;
import com.frost.envoys.client.gui.script.GraphActionBridge;
import com.frost.envoys.client.gui.script.GraphNode;
import com.frost.envoys.client.gui.script.ScriptNodeTypes;
import com.frost.envoys.gui.screen.action.SettingAdvanceStepScreen;
import com.frost.envoys.gui.screen.action.SettingQuestCheckScreen;
import com.frost.envoys.gui.screen.action.SettingChatScreen;
import com.frost.envoys.gui.screen.action.SettingCommandScreen;
import com.frost.envoys.gui.screen.action.SettingDelayScreen;
import com.frost.envoys.gui.screen.action.SettingLookAtScreen;
import com.frost.envoys.gui.screen.action.SettingMarkCompletedScreen;
import com.frost.envoys.gui.screen.action.SettingMoveScreen;
import com.frost.envoys.gui.screen.action.SettingLoadPointScreen;
import com.frost.envoys.gui.screen.action.SettingQuestGiveScreen;
import com.frost.envoys.gui.screen.action.SettingSavePointScreen;
import com.frost.envoys.gui.screen.action.SettingStartScreen;
import com.frost.envoys.gui.screen.action.SettingTradeScreen;

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

public class NPCScriptScreen extends Screen {

    private final Screen parentScreen;
    private final NPCInteractManager manager;
    private final ActionGraph graph;
    private final boolean isCreativeTuner;
    private final Runnable onModified;
    private final Map<GraphNode, EntityActionData> pendingEdits = new HashMap<>();

    private NodeList nodeList;
    private NodeType selectedTypeToAdd = NodeType.SAY;
    private EditBox idInputField;
    private Button typeButton;
    private boolean pendingDirectEdit;

    public NPCScriptScreen(Screen parentScreen, NPCInteractManager manager, ActionGraph graph,
                           boolean isCreativeTuner, Runnable onModified) {
        super(Component.literal("Визуальный сценарий"));
        this.parentScreen = parentScreen;
        this.manager = manager;
        this.graph = graph;
        this.isCreativeTuner = isCreativeTuner;
        this.onModified = onModified;
    }

    @Override
    protected void init() {
        super.init();

        if (!pendingEdits.isEmpty()) {
            for (Map.Entry<GraphNode, EntityActionData> entry : pendingEdits.entrySet()) {
                GraphActionBridge.applyAction(entry.getKey(), entry.getValue());
            }
            pendingEdits.clear();
            markModified();
        } else if (pendingDirectEdit) {
            pendingDirectEdit = false;
            markModified();
        }

        reorderStartFirst();

        int centerX = this.width / 2;

        this.nodeList = new NodeList(this.minecraft, this.width, this.height - 85, 40, 24);
        this.addRenderableWidget(this.nodeList);
        for (GraphNode node : graph.nodes) {
            this.nodeList.addNode(node);
        }

        this.typeButton = Button.builder(Component.literal("Тип: " + this.selectedTypeToAdd.display), button ->
                Minecraft.getInstance().setScreen(new NodeTypeSelectScreen(this, this.selectedTypeToAdd, selected -> {
                    this.selectedTypeToAdd = selected;
                    this.typeButton.setMessage(Component.literal("Тип: " + selected.display));
                }))).bounds(centerX - 195, this.height - 35, 120, 20).build();
        this.addRenderableWidget(this.typeButton);

        this.idInputField = new EditBox(this.font, centerX - 60, this.height - 35, 60, 20, Component.literal("ID"));
        this.idInputField.setValue(graph.nextId());
        this.idInputField.setTooltip(Tooltip.create(Component.literal("Уникальный ID узла.")));
        this.addRenderableWidget(this.idInputField);

        this.addRenderableWidget(Button.builder(Component.literal("Добавить"), button -> {
            String id = this.idInputField.getValue().trim();
            if (id.isEmpty() || graph.node(id) != null) {
                id = graph.nextId();
            }
            GraphNode node = newNode(id, this.selectedTypeToAdd.type);
            graph.addNode(node);
            this.nodeList.addNode(node);
            this.idInputField.setValue(graph.nextId());
            markModified();
        }).bounds(centerX - 5, this.height - 35, 90, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Назад"), button -> {
            if (this.parentScreen != null) {
                Minecraft.getInstance().setScreen(this.parentScreen);
            } else {
                this.onClose();
            }
        }).bounds(centerX + 90, this.height - 35, 100, 20).build());
    }

    private void reorderStartFirst() {
        for (int i = 0; i < graph.nodes.size(); i++) {
            GraphNode node = graph.nodes.get(i);
            if (ScriptNodeTypes.START.equals(node.type)) {
                if (i != 0) {
                    graph.nodes.remove(i);
                    graph.nodes.add(0, node);
                }
                break;
            }
        }
    }

    private GraphNode newNode(String id, String type) {
        GraphNode node = new GraphNode(id, type);
        switch (type) {
            case ScriptNodeTypes.SAVE_POINT -> {
                node.params.put("name", "checkpoint");
                node.params.put("cp", java.util.UUID.randomUUID().toString());
                node.params.put("exit", "false");
            }
            case ScriptNodeTypes.LOAD_POINT -> node.params.put("target", "");
            case ScriptNodeTypes.WAIT -> node.params.put("ticks", "20");
            case ScriptNodeTypes.MOVE -> {
                node.params.put("x", "0.0");
                node.params.put("y", "0.0");
                node.params.put("z", "0.0");
            }
            case ScriptNodeTypes.LOOK_AT -> node.params.put("mode", "player");
            case ScriptNodeTypes.DIALOGUE -> {
                node.params.put("text", "");
                node.options.add(new GraphNode.BranchOption("1", "Вариант 1", null));
                node.options.add(new GraphNode.BranchOption("2", "Вариант 2", null));
            }
            case ScriptNodeTypes.RANDOM -> {
                node.options.add(new GraphNode.BranchOption("1", "Вариант 1", null));
                node.options.add(new GraphNode.BranchOption("2", "Вариант 2", null));
            }
            case ScriptNodeTypes.QUEST_CHECK -> {
                node.params.put("quest", "");
                node.options.add(new GraphNode.BranchOption("completed", "Выполнен", null));
                node.options.add(new GraphNode.BranchOption("not_completed", "Не выполнен", null));
            }
            default -> {
            }
        }
        return node;
    }

    private void markModified() {
        if (onModified != null) {
            onModified.run();
        }
    }

    private void openSettings(GraphNode node) {
        if (node.isBranch()) {
            this.pendingDirectEdit = true;
            Minecraft.getInstance().setScreen(new SettingQuestCheckScreen(this, node));
            return;
        }
        if (ScriptNodeTypes.LOOK_AT.equals(node.type)) {
            this.pendingDirectEdit = true;
            Minecraft.getInstance().setScreen(new SettingLookAtScreen(this, node));
            return;
        }
        EntityActionData action = GraphActionBridge.toAction(node);
        if (action == null) {
            return;
        }
        pendingEdits.put(node, action);
        if (action instanceof ActionStart start) {
            Minecraft.getInstance().setScreen(new SettingStartScreen(this, start));
        } else if (action instanceof ActionSavePoint savePoint) {
            Minecraft.getInstance().setScreen(new SettingSavePointScreen(this, savePoint));
        } else if (action instanceof ActionLoadPoint loadPoint) {
            Minecraft.getInstance().setScreen(new SettingLoadPointScreen(this, loadPoint));
        } else if (action instanceof ActionChat chat) {
            Minecraft.getInstance().setScreen(new SettingChatScreen(this, chat));
        } else if (action instanceof ActionDelay delay) {
            Minecraft.getInstance().setScreen(new SettingDelayScreen(this, delay));
        } else if (action instanceof ActionMove move) {
            Minecraft.getInstance().setScreen(new SettingMoveScreen(this, move));
        } else if (action instanceof ActionCommand command) {
            Minecraft.getInstance().setScreen(new SettingCommandScreen(this, command));
        } else if (action instanceof ActionTrade trade) {
            Minecraft.getInstance().setScreen(new SettingTradeScreen(this, trade));
        } else if (action instanceof ActionQuestGive give) {
            Minecraft.getInstance().setScreen(new SettingQuestGiveScreen(this, give, manager));
        } else if (action instanceof ActionQuestAdvanceStep advance) {
            Minecraft.getInstance().setScreen(new SettingAdvanceStepScreen(this, advance, manager));
        } else if (action instanceof ActionQuestMarkCompleted completed) {
            Minecraft.getInstance().setScreen(new SettingMarkCompletedScreen(this, completed, manager));
        } else {
            pendingEdits.remove(node);
        }
    }

    private void deleteNode(GraphNode node) {
        graph.removeNode(node.id);
        markModified();
        this.rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public enum NodeType {
        START(ScriptNodeTypes.START, "Старт"),
        SAY(ScriptNodeTypes.SAY, "Сообщение"),
        WAIT(ScriptNodeTypes.WAIT, "Ожидание"),
        MOVE(ScriptNodeTypes.MOVE, "Движение"),
        COMMAND(ScriptNodeTypes.COMMAND, "Команда"),
        TRADE(ScriptNodeTypes.TRADE, "Трейд"),
        DIALOGUE(ScriptNodeTypes.DIALOGUE, "Диалог"),
        RANDOM(ScriptNodeTypes.RANDOM, "Рандом"),
        QUEST_CHECK(ScriptNodeTypes.QUEST_CHECK, "Проверка квеста"),
        QUEST_START(ScriptNodeTypes.QUEST_START, "Выдать квест"),
        QUEST_ADVANCE(ScriptNodeTypes.QUEST_ADVANCE, "Продвинуть этап"),
        QUEST_COMPLETE(ScriptNodeTypes.QUEST_COMPLETE, "Завершить квест"),
        LOOK_AT(ScriptNodeTypes.LOOK_AT, "Поворот к цели"),
        SAVE_POINT(ScriptNodeTypes.SAVE_POINT, "Метка"),
        LOAD_POINT(ScriptNodeTypes.LOAD_POINT, "Переход");

        public final String type;
        public final String display;

        NodeType(String type, String display) {
            this.type = type;
            this.display = display;
        }
    }

    class NodeList extends ContainerObjectSelectionList<NodeEntry> {
        public NodeList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public void addNode(GraphNode node) {
            this.addEntry(new NodeEntry(node));
        }

        public void removeNode(NodeEntry entry) {
            this.removeEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return 340;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width / 2 + 180;
        }
    }

    class NodeEntry extends ContainerObjectSelectionList.Entry<NodeEntry> {
        private final GraphNode node;
        private final Button configureButton;
        private final Button deleteButton;
        private final List<GuiEventListener> children = new ArrayList<>();

        public NodeEntry(GraphNode node) {
            this.node = node;

            this.configureButton = Button.builder(Component.literal("Настроить"), button -> openSettings(this.node))
                    .bounds(0, 0, 75, 20).build();
            this.deleteButton = Button.builder(Component.literal("Удалить"), button -> deleteNode(this.node))
                    .bounds(0, 0, 60, 20).build();
            this.deleteButton.active = !ScriptNodeTypes.START.equals(this.node.type);

            this.children.add(this.configureButton);
            this.children.add(this.deleteButton);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.configureButton, this.deleteButton);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            String next = node.nextId == null ? "-" : node.nextId;
            String label;
            if (ScriptNodeTypes.isSavePoint(node.type)) {
                String exit = node.boolParam("exit", false) ? " (прервать)" : "";
                label = "[" + node.id + "] Точка сохранения \"" + node.param("name", "") + "\"" + exit + " → " + next;
            } else if (ScriptNodeTypes.isLoadPoint(node.type)) {
                label = "[" + node.id + "] Загрузить точку \"" + node.param("target", "") + "\" → " + next;
            } else {
                label = "[" + node.id + "] " + ScriptNodeTypes.displayName(node.type) + " → " + next;
            }
            guiGraphics.drawString(Minecraft.getInstance().font, label, left + 5, top + (height - 8) / 2, 0xFFFFFF, false);

            this.configureButton.setX(left + width - 145);
            this.configureButton.setY(top);
            this.deleteButton.setX(left + width - 65);
            this.deleteButton.setY(top);

            this.configureButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
