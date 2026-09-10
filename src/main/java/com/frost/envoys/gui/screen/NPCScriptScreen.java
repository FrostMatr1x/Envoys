package com.frost.envoys.gui.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.NPCScriptData;
import com.frost.envoys.action.event.NpcEventData;
import com.frost.envoys.action.model.ActionChat;
import com.frost.envoys.action.model.ActionCommand;
import com.frost.envoys.action.model.ActionDelay;
import com.frost.envoys.action.model.ActionDialog;
import com.frost.envoys.action.model.ActionMove;
import com.frost.envoys.action.model.ActionQuestCheck;
import com.frost.envoys.action.model.ActionQuestGive;
import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.action.model.EntityActionData;
import com.frost.envoys.action.serialization.EntityActionAdapter;
import com.frost.envoys.gui.screen.action.SettingChatScreen;
import com.frost.envoys.gui.screen.action.SettingCommandScreen;
import com.frost.envoys.gui.screen.action.SettingDelayScreen;
import com.frost.envoys.gui.screen.action.SettingDialogScreen;
import com.frost.envoys.gui.screen.action.SettingMoveScreen;
import com.frost.envoys.gui.screen.action.SettingQuestCheckScreen;
import com.frost.envoys.gui.screen.action.SettingQuestGiveScreen;
import com.frost.envoys.gui.screen.action.SettingTradeScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class NPCScriptScreen extends Screen {

    private final Screen parentScreen;
    private final NPCInteractManager manager;
    private final NpcEventData event;
    private final boolean isCreativeTuner;

    private ActionList actionList;
    private ActionType selectedTypeToAdd = ActionType.DIALOD;
    private EditBox idInputField;

    public NPCScriptScreen(Screen parentScreen, NPCInteractManager manager, NpcEventData event, boolean isCreativeTuner) {
        super(Component.literal("Цепочка действий"));
        this.parentScreen = parentScreen;
        this.manager = manager;
        this.event = event;
        this.isCreativeTuner = isCreativeTuner;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 100;
        int idFieldWidth = 40;
        int centerX = this.width / 2;

        int listHeight = this.height - 85;
        this.actionList = new ActionList(this.minecraft, this.width, listHeight, 40, 24);
        this.addRenderableWidget(this.actionList);

        if (this.event.actions() != null) {
            for (EntityActionData action : this.event.actions()) {
                this.actionList.addAction(action);
            }
        }

        final ActionType[] availableTypes = this.isCreativeTuner
                ? new ActionType[]{ ActionType.DIALOD, ActionType.TRADE, ActionType.COMMAND, ActionType.QUEST_GIVE,
                        ActionType.QUEST_CHECK, ActionType.MOVE, ActionType.DELAY, ActionType.CHAT }
                : new ActionType[]{ ActionType.DIALOD };

        this.addRenderableWidget(Button.builder(
            Component.literal("Тип: " + this.selectedTypeToAdd.getDisplayName()),
            button -> {
                int currentIndex = 0;
                for (int i = 0; i < availableTypes.length; i++) {
                    if (availableTypes[i] == this.selectedTypeToAdd) {
                        currentIndex = i;
                        break;
                    }
                }
                int nextIndex = (currentIndex + 1) % availableTypes.length;
                this.selectedTypeToAdd = availableTypes[nextIndex];
                button.setMessage(Component.literal("Тип: " + this.selectedTypeToAdd.getDisplayName()));
            }
        ).bounds(centerX - 190, this.height - 35, buttonWidth, 20).build());

        this.idInputField = new EditBox(this.font, centerX - 80, this.height - 35, idFieldWidth, 20, Component.literal("ID"));
        this.idInputField.setValue("id_" + (this.actionList.children().size() + 1));
        this.addRenderableWidget(this.idInputField);

        this.addRenderableWidget(Button.builder(
            Component.literal("Добавить"),
            button -> {
                String id = this.idInputField.getValue().trim();
                if (id.isEmpty()) {
                    id = "id_1";
                }

                while (this.containsActionId(id)) {
                    id = incrementId(id);
                }

                EntityActionData newAction = switch (this.selectedTypeToAdd) {
                    case DIALOD -> new ActionDialog(id, manager.passport.npcName);
                    case TRADE -> new ActionTrade(id);
                    case COMMAND -> new ActionCommand(id);
                    case QUEST_GIVE -> new ActionQuestGive(id);
                    case QUEST_CHECK -> new ActionQuestCheck(id);
                    case MOVE -> new ActionMove(id);
                    case DELAY -> new ActionDelay(id);
                    case CHAT -> new ActionChat(id);
                };

                this.event.actions().add(newAction);
                this.actionList.addAction(newAction);

                this.idInputField.setValue(incrementId(id));
            }
        ).bounds(centerX - 30, this.height - 35, buttonWidth, 20).build());

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
        ).bounds(centerX + 80, this.height - 35, buttonWidth, 20).build());
    }

    private boolean containsActionId(String id) {
        if (this.event.actions() == null) {
            return false;
        }
        for (EntityActionData action : this.event.actions()) {
            if (action != null && id.equals(action.getId())) {
                return true;
            }
        }
        return false;
    }

    private String incrementId(String id) {
        Matcher matcher = Pattern.compile("(.*?)(\\d+)$").matcher(id);
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            String numStr = matcher.group(2);
            try {
                int num = Integer.parseInt(numStr) + 1;
                String format = "%0" + numStr.length() + "d";
                return prefix + String.format(format, num);
            } catch (NumberFormatException e) {
                return id + "_1";
            }
        }
        return id + "_1";
    }

    private void saveAndSync() {
        NPCScriptData scriptData = NPCScriptData.fromManager(this.manager);
        String json = EntityActionAdapter.GSON.toJson(scriptData);

        PacketDistributor.sendToServer(
            new com.frost.envoys.network.payload.SaveNPCScriptPayload(this.manager.npcUUID, json)
        );
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

    class ActionList extends ContainerObjectSelectionList<ActionEntry> {
        public ActionList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public void addAction(EntityActionData action) {
            this.addEntry(new ActionEntry(action));
        }

        public void removeAction(ActionEntry entry) {
            this.removeEntry(entry);
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

    class ActionEntry extends ContainerObjectSelectionList.Entry<ActionEntry> {
        private final Button configureButton;
        private final Button deleteButton;
        private final List<GuiEventListener> children = new ArrayList<>();
        private final ActionType type;
        private final String id;

        public ActionEntry(EntityActionData actionData) {
            this.id = actionData.getId();

            this.type = switch (actionData.getType()) {
                case "dialog" -> ActionType.DIALOD;
                case "trade" -> ActionType.TRADE;
                case "command" -> ActionType.COMMAND;
                case "quest_give" -> ActionType.QUEST_GIVE;
                case "quest_check" -> ActionType.QUEST_CHECK;
                case "move" -> ActionType.MOVE;
                case "delay" -> ActionType.DELAY;
                case "chat" -> ActionType.CHAT;
                default -> ActionType.DIALOD;
            };

            this.configureButton = Button.builder(Component.literal("Настроить"), button -> {
                if (actionData instanceof ActionDialog dialogAction) {
                    Minecraft.getInstance().setScreen(new SettingDialogScreen(NPCScriptScreen.this, dialogAction));
                } else if (actionData instanceof ActionTrade tradeAction) {
                    Minecraft.getInstance().setScreen(new SettingTradeScreen(NPCScriptScreen.this, tradeAction));
                } else if (actionData instanceof ActionCommand commandAction) {
                    Minecraft.getInstance().setScreen(new SettingCommandScreen(NPCScriptScreen.this, commandAction));
                } else if (actionData instanceof ActionQuestGive questGiveAction) {
                    Minecraft.getInstance().setScreen(new SettingQuestGiveScreen(NPCScriptScreen.this, questGiveAction));
                } else if (actionData instanceof ActionQuestCheck questCheckAction) {
                    Minecraft.getInstance().setScreen(new SettingQuestCheckScreen(NPCScriptScreen.this, questCheckAction));
                } else if (actionData instanceof ActionMove moveAction) {
                    Minecraft.getInstance().setScreen(new SettingMoveScreen(NPCScriptScreen.this, moveAction));
                } else if (actionData instanceof ActionDelay delayAction) {
                    Minecraft.getInstance().setScreen(new SettingDelayScreen(NPCScriptScreen.this, delayAction));
                } else if (actionData instanceof ActionChat chatAction) {
                    Minecraft.getInstance().setScreen(new SettingChatScreen(NPCScriptScreen.this, chatAction));
                }
            }).bounds(0, 0, 75, 20).build();

            this.deleteButton = Button.builder(Component.literal("Удалить"), button -> {
                NPCScriptScreen.this.actionList.removeAction(this);
                if (NPCScriptScreen.this.event.actions() != null) {
                    NPCScriptScreen.this.event.actions().remove(actionData);
                }
            }).bounds(0, 0, 60, 20).build();

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
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            String label = "[" + this.id + "] " + this.type.getDisplayName();
            guiGraphics.drawString(Minecraft.getInstance().font, label, left + 5, top + (height - 8) / 2, 0xFFFFFF, false);

            this.configureButton.setX(left + width - 145);
            this.configureButton.setY(top);

            this.deleteButton.setX(left + width - 65);
            this.deleteButton.setY(top);

            this.configureButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    enum ActionType {
        DIALOD("Диалог"),
        TRADE("Трейд"),
        COMMAND("Команда"),
        QUEST_GIVE("Выдача квеста"),
        QUEST_CHECK("Проверка квеста"),
        MOVE("Передвижение"),
        DELAY("Ожидание"),
        CHAT("Сообщение в чат");

        private final String displayName;

        ActionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
