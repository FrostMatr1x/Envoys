package com.frost.envoys.gui.screen;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.gui.screen.action.ItemSelectorScreen;
import com.frost.envoys.quest.QuestDefinition;
import com.frost.envoys.quest.QuestInventoryUtil;
import com.frost.envoys.quest.QuestType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class QuestEditScreen extends Screen {

    private static final Component ENTITY_TOOLTIP = Component.translatable("envoys.gui.quest_edit.entity_tooltip");

    private final Screen parentScreen;
    private final NPCInteractManager manager;
    private final QuestDefinition original;
    private final boolean isNew;

    private String localId = "";
    private String title = "";
    private String questUuid = "";
    private boolean visibleInGui = true;
    private QuestType type = QuestType.BOOLEAN;

    private String itemId = "";
    private int itemCount = 1;
    private boolean consumeItems = true;

    private int requiredCompletions = 1;

    private String entityId = "";
    private int killCount = 1;

    private EditBox localIdEditBox;
    private EditBox titleEditBox;
    private EditBox uuidEditBox;
    private Checkbox visibleCheckbox;
    private Button typeButton;

    private Button itemButton;
    private EditBox itemCountEditBox;
    private Checkbox consumeCheckbox;

    private EditBox requiredCompletionsEditBox;

    private EditBox entityIdEditBox;
    private EditBox killCountEditBox;

    private Button saveButton;

    public QuestEditScreen(Screen parentScreen, NPCInteractManager manager, QuestDefinition original, boolean isNew) {
        super(Component.translatable(isNew ? "envoys.gui.quest_edit.title_new" : "envoys.gui.quest_edit.title_edit"));
        this.parentScreen = parentScreen;
        this.manager = manager;
        this.original = original;
        this.isNew = isNew;

        if (original != null) {
            this.localId = original.localId != null ? original.localId : "";
            this.title = original.title != null ? original.title : "";
            this.questUuid = original.questUuid != null ? original.questUuid : "";
            this.visibleInGui = original.visibleInGui;
            this.type = original.type != null ? original.type : QuestType.BOOLEAN;
            this.itemId = original.itemId != null ? original.itemId : "";
            this.itemCount = Math.max(1, original.itemCount);
            this.consumeItems = original.consumeItems;
            this.requiredCompletions = Math.max(1, original.requiredCompletions);
            this.entityId = original.entityId != null ? original.entityId : "";
            this.killCount = Math.max(1, original.killCount);
        } else {
            this.questUuid = QuestDefinition.generateUuid();
            this.type = QuestType.BOOLEAN;
            this.visibleInGui = true;
            this.itemCount = 1;
            this.consumeItems = true;
            this.requiredCompletions = 1;
            this.killCount = 1;
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 40;
        int labelX = centerX - 160;
        int controlX = centerX + 10;
        int controlWidth = 200;

        this.localIdEditBox = new EditBox(this.font, controlX, startY - 2, controlWidth, 20, Component.literal("local_id"));
        this.localIdEditBox.setValue(this.localId);
        this.localIdEditBox.setFilter(text -> text.isEmpty() || text.chars().allMatch(c ->
            (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-'));
        this.localIdEditBox.setResponder(text -> this.localId = text);
        this.addRenderableWidget(this.localIdEditBox);

        this.titleEditBox = new EditBox(this.font, controlX, startY + 24, controlWidth, 20, Component.literal("title"));
        this.titleEditBox.setValue(this.title);
        this.titleEditBox.setResponder(text -> this.title = text);
        this.addRenderableWidget(this.titleEditBox);

        this.uuidEditBox = new EditBox(this.font, controlX, startY + 50, controlWidth, 20, Component.literal("quest_uuid"));
        this.uuidEditBox.setMaxLength(36);
        this.uuidEditBox.setValue(this.questUuid);
        this.uuidEditBox.setResponder(text -> this.questUuid = text);
        this.addRenderableWidget(this.uuidEditBox);

        this.visibleCheckbox = Checkbox.builder(Component.translatable("envoys.gui.quest_edit.visible_in_gui"), this.font)
            .pos(labelX, startY + 78)
            .selected(this.visibleInGui)
            .onValueChange((checkbox, selected) -> this.visibleInGui = selected)
            .build();
        this.addRenderableWidget(this.visibleCheckbox);

        this.typeButton = Button.builder(
            this.typeLabel(),
            button -> {
                this.type = switch (this.type) {
                    case ITEM -> QuestType.BOOLEAN;
                    case BOOLEAN -> QuestType.KILL;
                    case KILL -> QuestType.ITEM;
                };
                button.setMessage(this.typeLabel());
                this.applyTypeVisibility();
            }
        ).bounds(labelX, startY + 104, 140, 20).build();
        this.addRenderableWidget(this.typeButton);

        int typeRow = startY + 134;

        this.itemButton = Button.builder(
            Component.translatable("envoys.gui.quest_edit.select_item"),
            button -> Minecraft.getInstance().setScreen(new ItemSelectorScreen(this, this.currentItemStack(), stack -> {
                Item item = stack.getItem();
                this.itemId = BuiltInRegistries.ITEM.getKey(item).toString();
                this.itemCount = Math.max(1, stack.getCount());
                if (this.itemCountEditBox != null) {
                    this.itemCountEditBox.setValue(Integer.toString(this.itemCount));
                }
            }))
        ).bounds(controlX, typeRow, 140, 20).build();
        this.addRenderableWidget(this.itemButton);

        this.itemCountEditBox = new EditBox(this.font, controlX, typeRow + 26, 80, 20, Component.literal("item_count"));
        this.itemCountEditBox.setValue(Integer.toString(this.itemCount));
        this.itemCountEditBox.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        this.itemCountEditBox.setResponder(text -> this.itemCount = parseIntOr(text, 1));
        this.addRenderableWidget(this.itemCountEditBox);

        this.consumeCheckbox = Checkbox.builder(Component.translatable("envoys.gui.quest_edit.consume_items"), this.font)
            .pos(labelX, typeRow + 52)
            .selected(this.consumeItems)
            .onValueChange((checkbox, selected) -> this.consumeItems = selected)
            .build();
        this.addRenderableWidget(this.consumeCheckbox);

        this.requiredCompletionsEditBox = new EditBox(this.font, controlX, typeRow, 80, 20, Component.literal("required_completions"));
        this.requiredCompletionsEditBox.setValue(Integer.toString(this.requiredCompletions));
        this.requiredCompletionsEditBox.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        this.requiredCompletionsEditBox.setResponder(text -> this.requiredCompletions = parseIntOr(text, 1));
        this.addRenderableWidget(this.requiredCompletionsEditBox);

        this.entityIdEditBox = new EditBox(this.font, controlX, typeRow, controlWidth, 20, Component.literal("entity_id"));
        this.entityIdEditBox.setValue(this.entityId);
        this.entityIdEditBox.setResponder(text -> this.entityId = text);
        this.entityIdEditBox.setTooltip(Tooltip.create(ENTITY_TOOLTIP));
        this.addRenderableWidget(this.entityIdEditBox);

        this.killCountEditBox = new EditBox(this.font, controlX, typeRow + 26, 80, 20, Component.literal("kill_count"));
        this.killCountEditBox.setValue(Integer.toString(this.killCount));
        this.killCountEditBox.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        this.killCountEditBox.setResponder(text -> this.killCount = parseIntOr(text, 1));
        this.addRenderableWidget(this.killCountEditBox);

        this.saveButton = Button.builder(
            Component.translatable("envoys.gui.common.save"),
            button -> this.save()
        ).bounds(centerX - 100, this.height - 35, 95, 20).build();
        this.addRenderableWidget(this.saveButton);

        this.addRenderableWidget(Button.builder(
            Component.translatable("envoys.gui.common.cancel"),
            button -> this.cancel()
        ).bounds(centerX + 5, this.height - 35, 95, 20).build());

        this.applyTypeVisibility();
    }

    private void applyTypeVisibility() {
        boolean item = this.type == QuestType.ITEM;
        boolean bool = this.type == QuestType.BOOLEAN;
        boolean kill = this.type == QuestType.KILL;

        setVisible(this.itemButton, item);
        setVisible(this.itemCountEditBox, item);
        setVisible(this.consumeCheckbox, item);
        setVisible(this.requiredCompletionsEditBox, bool);
        setVisible(this.entityIdEditBox, kill);
        setVisible(this.killCountEditBox, kill);
    }

    private void setVisible(net.minecraft.client.gui.components.AbstractWidget widget, boolean visible) {
        if (widget != null) {
            widget.visible = visible;
            widget.active = visible;
        }
    }

    private ItemStack currentItemStack() {
        Item item = QuestInventoryUtil.resolveItem(this.itemId);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, Math.max(1, this.itemCount));
    }

    private static int parseIntOr(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Component typeLabel() {
        String key = switch (this.type) {
            case ITEM -> "envoys.gui.quest_edit.type.item";
            case BOOLEAN -> "envoys.gui.quest_edit.type.boolean";
            case KILL -> "envoys.gui.quest_edit.type.kill";
        };
        return Component.translatable("envoys.gui.quest_edit.type_label", Component.translatable(key));
    }

    private Component validate() {
        String lid = this.localId.trim();
        if (lid.isEmpty()) {
            return Component.translatable("envoys.gui.quest_edit.error_local_id_required");
        }
        String uuid = this.questUuid.trim();
        if (uuid.isEmpty()) {
            return Component.translatable("envoys.gui.quest_edit.error_uuid_required");
        }
        if (this.manager != null && this.manager.quests != null) {
            for (QuestDefinition quest : this.manager.quests) {
                if (quest == null || quest == this.original) {
                    continue;
                }
                if (lid.equals(quest.localId)) {
                    return Component.translatable("envoys.gui.quest_edit.error_local_id_used");
                }
                if (uuid.equals(quest.questUuid)) {
                    return Component.translatable("envoys.gui.quest_edit.error_uuid_used");
                }
            }
        }
        if (this.type == QuestType.ITEM) {
            if (this.itemId.trim().isEmpty()) {
                return Component.translatable("envoys.gui.quest_edit.error_item_required");
            }
            if (this.itemCount < 1) {
                return Component.translatable("envoys.gui.quest_edit.error_count");
            }
        } else if (this.type == QuestType.BOOLEAN) {
            if (this.requiredCompletions < 1) {
                return Component.translatable("envoys.gui.quest_edit.error_completions");
            }
        } else if (this.type == QuestType.KILL) {
            if (this.entityId.trim().isEmpty()) {
                return Component.translatable("envoys.gui.quest_edit.error_entity_required");
            }
            if (this.killCount < 1) {
                return Component.translatable("envoys.gui.quest_edit.error_kills");
            }
        }
        return null;
    }

    private void applyTo(QuestDefinition quest) {
        quest.localId = this.localId.trim();
        quest.title = this.title.trim();
        quest.questUuid = this.questUuid.trim();
        quest.visibleInGui = this.visibleInGui;
        quest.type = this.type;
        quest.itemId = this.itemId.trim();
        quest.itemCount = this.itemCount;
        quest.consumeItems = this.consumeItems;
        quest.requiredCompletions = this.requiredCompletions;
        quest.entityId = this.entityId.trim();
        quest.killCount = this.killCount;
    }

    private void save() {
        if (this.validate() != null) {
            return;
        }
        if (this.isNew) {
            QuestDefinition quest = new QuestDefinition();
            this.applyTo(quest);
            if (this.manager != null) {
                if (this.manager.quests == null) {
                    this.manager.quests = new java.util.ArrayList<>();
                }
                this.manager.quests.add(quest);
            }
        } else if (this.original != null) {
            this.applyTo(this.original);
        }
        this.returnToParent();
    }

    private void cancel() {
        this.returnToParent();
    }

    private void returnToParent() {
        if (this.parentScreen != null) {
            Minecraft.getInstance().setScreen(this.parentScreen);
        } else {
            this.onClose();
        }
    }

    @Override
    public void onClose() {
        this.returnToParent();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Component error = this.validate();
        if (this.saveButton != null) {
            this.saveButton.active = error == null;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = 40;
        int labelX = centerX - 160;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, "local_id:", labelX, startY, 0xA0A0A0);
        guiGraphics.drawString(this.font, Component.translatable("envoys.gui.quest_edit.label_title"), labelX, startY + 26, 0xA0A0A0);
        guiGraphics.drawString(this.font, "quest_uuid:", labelX, startY + 52, 0xA0A0A0);

        int typeRow = startY + 134;
        if (this.type == QuestType.ITEM) {
            guiGraphics.drawString(this.font, Component.translatable("envoys.gui.quest_edit.label_item"), labelX, typeRow + 6, 0xA0A0A0);
            Component shown = this.itemId.isEmpty() ? Component.translatable("envoys.gui.quest_edit.not_selected") : Component.literal(this.itemId);
            guiGraphics.drawString(this.font, shown, labelX + 60, typeRow + 6, 0xFFFFFF);
            Item item = QuestInventoryUtil.resolveItem(this.itemId);
            if (item != null) {
                guiGraphics.renderFakeItem(new ItemStack(item), centerX + 10 - 22, typeRow + 1);
            }
            guiGraphics.drawString(this.font, Component.translatable("envoys.gui.quest_edit.label_count"), labelX, typeRow + 32, 0xA0A0A0);
        } else if (this.type == QuestType.BOOLEAN) {
            guiGraphics.drawString(this.font, Component.translatable("envoys.gui.quest_edit.label_completions"), labelX, typeRow + 6, 0xA0A0A0);
        } else if (this.type == QuestType.KILL) {
            guiGraphics.drawString(this.font, "entity_id:", labelX, typeRow + 6, 0xA0A0A0);
            guiGraphics.drawString(this.font, Component.translatable("envoys.gui.quest_edit.label_kills"), labelX, typeRow + 32, 0xA0A0A0);
        }

        if (error != null) {
            guiGraphics.drawCenteredString(this.font, error, centerX, this.height - 50, 0xFFFF5555);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
