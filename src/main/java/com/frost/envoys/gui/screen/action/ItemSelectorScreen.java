package com.frost.envoys.gui.screen.action;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ItemSelectorScreen extends Screen {

    private static final Component TITLE = Component.literal("Выбор и настройка предмета");
    private static final Component SEARCH_HINT = Component.literal("Поиск...");
    private static final Component NBT_HINT = Component.literal("Компоненты [...] или NBT {...}");
    private static final Component SELECT_BTN = Component.literal("Выбрать");
    private static final Component CANCEL_BTN = Component.literal("Отмена");
    private static final Component FROM_HAND_BTN = Component.literal("Из руки");

    private final Screen parentScreen;
    private final Consumer<ItemStack> onSelect;

    private EditBox searchBox;
    private EditBox nbtBox;

    private final List<Item> allItems = new ArrayList<>();
    private final List<Item> matchingItems = new ArrayList<>();

    private Item selectedItem = Items.DIAMOND;
    private ItemStack selectedStack = ItemStack.EMPTY;
    private int count = 1;
    private int gridScrollOffset = 0;
    private String errorMessage = "";

    public ItemSelectorScreen(Screen parentScreen, ItemStack currentStack, Consumer<ItemStack> onSelect) {
        super(TITLE);
        this.parentScreen = parentScreen;
        this.onSelect = onSelect;

        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                this.allItems.add(item);
            }
        }

        if (currentStack != null && !currentStack.isEmpty()) {
            this.selectedItem = currentStack.getItem();
            this.count = currentStack.getCount();
            this.selectedStack = currentStack.copy();
        } else {
            this.selectedItem = Items.DIAMOND;
            this.count = 1;
            this.selectedStack = new ItemStack(Items.DIAMOND, 1);
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        this.searchBox = new EditBox(this.font, centerX - 80, 22, 160, 18, SEARCH_HINT);
        this.searchBox.setHint(SEARCH_HINT);
        this.searchBox.setResponder(text -> filterItems());
        this.addRenderableWidget(this.searchBox);

        this.nbtBox = new EditBox(this.font, centerX - 72, 126, 115, 18, NBT_HINT);
        this.nbtBox.setHint(NBT_HINT);
        this.nbtBox.setMaxLength(8192);

        loadNbtFromStack(this.selectedStack);

        this.nbtBox.setResponder(text -> updateSelectedStack());
        this.addRenderableWidget(this.nbtBox);

        this.addRenderableWidget(Button.builder(FROM_HAND_BTN, button -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                ItemStack handStack = this.minecraft.player.getOffhandItem();

                if (!handStack.isEmpty()) {
                    this.selectedItem = handStack.getItem();
                    this.count = handStack.getCount();
                    this.selectedStack = handStack.copy();
                    loadNbtFromStack(this.selectedStack);
                    updateSelectedStack();
                }
            }
        }).bounds(centerX + 46, 126, 50, 18).build());

        this.addRenderableWidget(Button.builder(SELECT_BTN, button -> {
            updateSelectedStack();
            if (this.selectedStack != null && !this.selectedStack.isEmpty()) {
                this.onSelect.accept(this.selectedStack.copy());
                this.onClose();
            } else if (this.selectedItem != null && this.selectedItem != Items.AIR) {
                ItemStack fallbackStack = new ItemStack(this.selectedItem, this.count);
                this.onSelect.accept(fallbackStack);
                this.onClose();
            }
        }).bounds(centerX - 100, this.height - 28, 95, 20).build());

        this.addRenderableWidget(Button.builder(CANCEL_BTN, button -> this.onClose())
                .bounds(centerX + 5, this.height - 28, 95, 20).build());

        filterItems();
        updateSelectedStack();
    }

    private void loadNbtFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || this.minecraft == null || this.minecraft.level == null) {
            if (this.nbtBox != null) this.nbtBox.setValue("");
            return;
        }

        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) {
            if (this.nbtBox != null) this.nbtBox.setValue("");
            return;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty() && patch.size() == 1) {
            if (this.nbtBox != null) {
                this.nbtBox.setValue(customData.copyTag().toString());
            }
            return;
        }

        HolderLookup.Provider registries = this.minecraft.level.registryAccess();

        try {
            RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
            DataResult<Tag> result = DataComponentPatch.CODEC.encodeStart(ops, patch);

            if (result.isSuccess() && result.result().isPresent()) {
                CompoundTag wrapper = new CompoundTag();
                wrapper.put("components", result.result().get());
                if (this.nbtBox != null) this.nbtBox.setValue(wrapper.toString());
                return;
            }
        } catch (Exception ignored) {
        }

        if (customData != null && !customData.isEmpty()) {
            if (this.nbtBox != null) this.nbtBox.setValue(customData.copyTag().toString());
        } else {
            if (this.nbtBox != null) this.nbtBox.setValue("");
        }
    }

    private void filterItems() {
        String query = this.searchBox != null ? this.searchBox.getValue().trim().toLowerCase() : "";
        this.matchingItems.clear();

        for (Item item : this.allItems) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase();
            String name = item.getDescription().getString().toLowerCase();

            if (query.isEmpty() || id.contains(query) || name.contains(query)) {
                this.matchingItems.add(item);
            }
        }
        this.gridScrollOffset = 0;
    }

    private void updateSelectedStack() {
        this.errorMessage = "";
        if (this.selectedItem == null || this.selectedItem == Items.AIR) {
            this.selectedStack = ItemStack.EMPTY;
            return;
        }

        String input = this.nbtBox != null ? this.nbtBox.getValue().trim() : "";
        ItemStack stack = new ItemStack(this.selectedItem, this.count);

        if (!input.isEmpty()) {
            if (this.minecraft == null || this.minecraft.level == null) {
                this.errorMessage = "Мир не загружен!";
                this.selectedStack = stack;
                return;
            }

            HolderLookup.Provider registries = this.minecraft.level.registryAccess();
            boolean parseFailed = false;

            if (input.startsWith("[")) {
                String cleanedInput = input.replaceAll("^\\[\\s*", "[");
                String itemKey = BuiltInRegistries.ITEM.getKey(this.selectedItem).toString();
                String fullString = itemKey + cleanedInput;

                try {
                    ItemParser parser = new ItemParser(registries);
                    ItemParser.ItemResult result = parser.parse(new StringReader(fullString));

                    stack = new ItemStack(result.item().value(), this.count);
                    stack.applyComponents(result.components());
                } catch (CommandSyntaxException e) {
                    this.errorMessage = e.getMessage();
                    parseFailed = true;
                }
            } else if (input.startsWith("{")) {
                try {
                    CompoundTag tag = TagParser.parseTag(input);

                    if (tag.contains("id")) {
                        Optional<ItemStack> parsedFull = ItemStack.parse(registries, tag);

                        if (parsedFull.isPresent() && !parsedFull.get().isEmpty()) {
                            stack = parsedFull.get();
                            stack.setCount(this.count);
                            this.selectedItem = stack.getItem();
                        } else {
                            this.errorMessage = "Не удалось распарсить предмет";
                            parseFailed = true;
                        }
                    }
                    else if (tag.contains("components")) {
                        CompoundTag fullTag = new CompoundTag();
                        fullTag.putString("id", BuiltInRegistries.ITEM.getKey(this.selectedItem).toString());
                        fullTag.putInt("count", this.count);
                        fullTag.put("components", tag.get("components"));

                        Optional<ItemStack> parsedComp = ItemStack.parse(registries, fullTag);
                        if (parsedComp.isPresent() && !parsedComp.get().isEmpty()) {
                            stack = parsedComp.get();
                            stack.setCount(this.count);
                        } else {
                            this.errorMessage = "Не удалось распарсить компоненты";
                            parseFailed = true;
                        }
                    } 
                    else {
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    }
                } catch (CommandSyntaxException e) {
                    this.errorMessage = e.getMessage();
                    parseFailed = true;
                } catch (Exception e) {
                    this.errorMessage = "Ошибка NBT: " + e.getMessage();
                    parseFailed = true;
                }
            } else if (input.contains("[")) {
                try {
                    ItemParser parser = new ItemParser(registries);
                    ItemParser.ItemResult result = parser.parse(new StringReader(input));

                    stack = new ItemStack(result.item().value(), this.count);
                    stack.applyComponents(result.components());
                } catch (CommandSyntaxException e) {
                    this.errorMessage = e.getMessage();
                    parseFailed = true;
                }
            } else {
                this.errorMessage = "Формат: '[...]', '{...}' или 'item_id[...]'";
                parseFailed = true;
            }

            if (parseFailed) {
                this.selectedStack = stack;
                return;
            }
        } else if (this.selectedStack != null && !this.selectedStack.isEmpty() && this.selectedStack.getItem() == this.selectedItem) {
            stack.applyComponents(this.selectedStack.getComponentsPatch());
            stack.setCount(this.count);
        }

        this.selectedStack = stack;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int centerX = this.width / 2;
        int slotX = centerX - 95;
        int slotY = 126;

        if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
            int step = hasShiftDown() ? 10 : 1;
            int delta = (int) Math.signum(scrollY) * step;

            int maxCount = !this.selectedStack.isEmpty() ? this.selectedStack.getMaxStackSize() : 64;
            this.count = Math.max(1, Math.min(maxCount, this.count + delta));
            updateSelectedStack();
            return true;
        }

        int cols = 9;
        int totalRows = (matchingItems.size() + cols - 1) / cols;
        int maxScroll = Math.max(0, totalRows - 4);

        this.gridScrollOffset = Math.max(0, Math.min(maxScroll, this.gridScrollOffset - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int cols = 9;
        int rows = 4;
        int gridX = centerX - (cols * 18 / 2);
        int gridY = 45;

        for (int r = 0; r < rows; r++) {
            int rowIdx = r + gridScrollOffset;
            for (int c = 0; c < cols; c++) {
                int index = rowIdx * cols + c;
                if (index >= matchingItems.size()) break;

                int slotX = gridX + c * 18;
                int slotY = gridY + r * 18;

                if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                    this.selectedItem = matchingItems.get(index);
                    this.selectedStack = new ItemStack(this.selectedItem, this.count);
                    loadNbtFromStack(this.selectedStack);
                    updateSelectedStack();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 8, 0xFFFFFF);

        int cols = 9;
        int rows = 4;
        int gridX = centerX - (cols * 18 / 2);
        int gridY = 45;

        ItemStack hoveredGridStack = ItemStack.EMPTY;

        for (int r = 0; r < rows; r++) {
            int rowIdx = r + gridScrollOffset;
            for (int c = 0; c < cols; c++) {
                int index = rowIdx * cols + c;
                int slotX = gridX + c * 18;
                int slotY = gridY + r * 18;

                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF2D2D2D);
                guiGraphics.fill(slotX - 1, slotY - 1, slotX + 19, slotY + 19, 0xFF555555);
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF2D2D2D);

                if (index < matchingItems.size()) {
                    Item item = matchingItems.get(index);
                    boolean isSelected = (item == this.selectedItem);

                    if (isSelected) {
                        guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF3D5C3D);
                    }

                    ItemStack stack = new ItemStack(item);
                    guiGraphics.renderFakeItem(stack, slotX + 1, slotY + 1);

                    if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                        guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0x80FFFFFF);
                        hoveredGridStack = stack;
                    }
                }
            }
        }

        if (matchingItems.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.literal("Ничего не найдено"), centerX, gridY + 30, 0xFFFF5555);
        }

        int slotX = centerX - 95;
        int slotY = 126;

        guiGraphics.fill(centerX - 100, 120, centerX + 100, 148, 0x1AFFFFFF);
        guiGraphics.fill(slotX - 1, slotY - 1, slotX + 19, slotY + 19, 0xFF555555);
        guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF2D2D2D);

        if (!this.selectedStack.isEmpty()) {
            guiGraphics.renderFakeItem(this.selectedStack, slotX + 1, slotY + 1);
            guiGraphics.renderItemDecorations(this.font, this.selectedStack, slotX + 1, slotY + 1);
        }

        guiGraphics.drawCenteredString(this.font,
                Component.literal("Колесико на слоте — количество (" + this.count + ") | Shift = x10"),
                centerX, 152, 0xA0A0A0);

        if (!this.errorMessage.isEmpty()) {
            guiGraphics.drawWordWrap(this.font, Component.literal(this.errorMessage), centerX - 95, 106, 190, 0xFFFF5555);
        }

        if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18 && !this.selectedStack.isEmpty()) {
            guiGraphics.renderTooltip(this.font, this.selectedStack, mouseX, mouseY);
        } else if (!hoveredGridStack.isEmpty()) {
            guiGraphics.renderTooltip(this.font, hoveredGridStack, mouseX, mouseY);
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
}