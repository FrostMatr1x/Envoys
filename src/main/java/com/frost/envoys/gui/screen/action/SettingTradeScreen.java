package com.frost.envoys.gui.screen.action;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.npc.NPCTrade;

public class SettingTradeScreen extends Screen {

    private static final Component TITLE_TEXT = Component.literal("Редактор сделок");
    private static final Component SUBTITLE_TEXT = Component.literal("Список сделок (прокрутка колесиком):");
    private static final Component CONTROLS_INFO_1 = Component.literal("ЛКМ — меню поиска и NBT | ПКМ — очистить слот");
    private static final Component ID_LABEL = Component.literal("ID следующего действия:");
    private static final Component ADD_BUTTON_TEXT = Component.literal("Добавить");
    private static final Component BACK_BUTTON_TEXT = Component.literal("Назад");
    private static final Component SETTINGS_BUTTON_TEXT = Component.literal("Настройки");
    private static final Component ID_BOX_LABEL = Component.literal("ID");

    private static final int START_Y = 35;
    private static final int ROW_HEIGHT = 26;
    private static final int MAX_ROWS = 5;
    private static final int SLOT_SIZE = 18;

    private final ActionTrade action;
    private final Screen parentScreen;
    private final List<Trade> trades = new ArrayList<>();
    private EditBox idEditBox;
    private Button settingsButton;
    private String nextId = "";
    private int tradesScrollOffset = 0;
    private int selectedRow = -1;

    public SettingTradeScreen(Screen parentScreen, ActionTrade action) {
        super(TITLE_TEXT);
        this.parentScreen = parentScreen;
        this.action = action;
        this.nextId = action.nextActionId != null ? action.nextActionId : "";
        
        for (com.frost.envoys.npc.NPCTrade npcTrade : action.trades) {
            Trade t = new Trade();
            t.setInputA(npcTrade.input1 != null ? npcTrade.input1.copy() : ItemStack.EMPTY);
            t.setInputB(npcTrade.input2 != null ? npcTrade.input2.copy() : ItemStack.EMPTY);
            t.setResult(npcTrade.output != null ? npcTrade.output.copy() : ItemStack.EMPTY);
            t.setPriceMultiplier(npcTrade.priceMultiplier);
            t.setMaxTrades(npcTrade.maxTrades);
            t.setResetTime(npcTrade.resetTime);
            this.trades.add(t);
        }
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 100;
        int buttonHeight = 20;
        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(ADD_BUTTON_TEXT, button -> {
            this.trades.add(new Trade());
            this.tradesScrollOffset = Math.max(0, this.trades.size() - MAX_ROWS);
        }).bounds(centerX - 155, this.height - 35, buttonWidth, buttonHeight).build());

        this.settingsButton = Button.builder(SETTINGS_BUTTON_TEXT, button -> {
            if (this.selectedRow >= 0 && this.selectedRow < this.trades.size()) {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new TradeOptionScreen(this, this.trades.get(this.selectedRow)));
                }
            }
        }).bounds(centerX - 50, this.height - 35, buttonWidth, buttonHeight).build();
        this.settingsButton.active = false;
        this.addRenderableWidget(this.settingsButton);

        this.addRenderableWidget(Button.builder(BACK_BUTTON_TEXT, button -> {
            this.saveData();
            this.onClose();
        }).bounds(centerX + 55, this.height - 35, buttonWidth, buttonHeight).build());

        this.idEditBox = new EditBox(this.font, centerX + 40, this.height - 65, 70, 20, ID_BOX_LABEL);
        this.idEditBox.setValue(this.nextId);
        this.idEditBox.setTooltip(Tooltip.create(Component.literal("ID действия, которое выполнится далее. Пусто — конец цепочки. Формат: id_N")));
        this.idEditBox.setResponder(text -> this.nextId = text);
        this.addRenderableWidget(this.idEditBox);
    }

    private void saveData() {
        this.action.nextActionId = this.nextId.trim();
        this.action.trades.clear();
        for (Trade t : this.trades) {
            NPCTrade newTrade = new NPCTrade(
                t.getInputA().copy(), 
                t.getInputB().copy(), 
                t.getResult().copy()
            );
            newTrade.priceMultiplier = t.getPriceMultiplier();
            newTrade.maxTrades = t.getMaxTrades();
            newTrade.resetTime = t.getResetTime();
            this.action.trades.add(newTrade);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.settingsButton != null) {
            this.settingsButton.active = (this.selectedRow >= 0 && this.selectedRow < this.trades.size());
        }

        int centerX = this.width / 2;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 10, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, SUBTITLE_TEXT, centerX, 22, 0xA0A0A0);

        ItemStack hoveredStack = ItemStack.EMPTY;

        for (int i = 0; i < MAX_ROWS; i++) {
            int index = i + tradesScrollOffset;
            if (index >= trades.size()) break;

            Trade trade = trades.get(index);
            int y = START_Y + i * ROW_HEIGHT;
            boolean isSelected = (index == selectedRow);

            // Расширяем фон строки, чтобы вместить множитель, кнопку удаления и стрелки перестановки
            guiGraphics.fill(centerX - 75, y - 2, centerX + 116, y + 20, isSelected ? 0x33FFFFFF : 0x1AFFFFFF);

            int x1 = getInputAX(centerX);
            boolean h1 = isHovering(mouseX, mouseY, x1, y, SLOT_SIZE, SLOT_SIZE);
            renderSlot(guiGraphics, x1, y, trade.getInputA(), h1);
            if (h1 && !trade.getInputA().isEmpty()) hoveredStack = trade.getInputA();

            guiGraphics.drawString(this.font, "+", centerX - 36, y + 5, 0xFFFFFF);

            int x2 = getInputBX(centerX);
            boolean h2 = isHovering(mouseX, mouseY, x2, y, SLOT_SIZE, SLOT_SIZE);
            renderSlot(guiGraphics, x2, y, trade.getInputB(), h2);
            if (h2 && !trade.getInputB().isEmpty()) hoveredStack = trade.getInputB();

            guiGraphics.drawString(this.font, "->", centerX + 1, y + 5, 0xFFFFFF);

            int x3 = getResultX(centerX);
            boolean h3 = isHovering(mouseX, mouseY, x3, y, SLOT_SIZE, SLOT_SIZE);
            renderSlot(guiGraphics, x3, y, trade.getResult(), h3);
            if (h3 && !trade.getResult().isEmpty()) hoveredStack = trade.getResult();

            int xDel = getDeleteX(centerX);
            boolean isDelHovered = isHovering(mouseX, mouseY, xDel, y + 3, 12, 12);
            guiGraphics.drawString(this.font, "X", xDel, y + 5, isDelHovered ? 0xFFFF5555 : 0xFFAA0000);

            int xUp = getUpX(centerX);
            boolean isUpHovered = isHovering(mouseX, mouseY, xUp, y + 3, 12, 12);
            guiGraphics.drawString(this.font, "▲", xUp, y + 5, isUpHovered ? 0xFFFFFF55 : 0xFFFFFFFF);

            int xDown = getDownX(centerX);
            boolean isDownHovered = isHovering(mouseX, mouseY, xDown, y + 3, 12, 12);
            guiGraphics.drawString(this.font, "▼", xDown, y + 5, isDownHovered ? 0xFFFFFF55 : 0xFFFFFFFF);
        }

        int infoY = START_Y + MAX_ROWS * ROW_HEIGHT + 2;
        guiGraphics.drawCenteredString(this.font, CONTROLS_INFO_1, centerX, infoY, 0x808080);
        guiGraphics.drawString(this.font, ID_LABEL, centerX - 110, this.height - 60, 0xA0A0A0);

        if (!hoveredStack.isEmpty()) {
            guiGraphics.renderTooltip(this.font, hoveredStack, mouseX, mouseY);
        }
    }

    private void renderSlot(GuiGraphics guiGraphics, int x, int y, ItemStack stack, boolean isHovered) {
        guiGraphics.fill(x - 1, y - 1, x + 19, y + 19, 0xFF555555);
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF2D2D2D);

        if (!stack.isEmpty()) {
            guiGraphics.renderFakeItem(stack, x + 1, y + 1);
            guiGraphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
        }
        if (isHovered) {
            guiGraphics.fill(x, y, x + 18, y + 18, 0x80FFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int signum = (int) Math.signum(scrollY);
        if (signum == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int centerX = this.width / 2;

        for (int i = 0; i < MAX_ROWS; i++) {
            int index = i + tradesScrollOffset;
            if (index >= trades.size()) break;

            int y = START_Y + i * ROW_HEIGHT;
            int xMult = getMultX(centerX);

            if (isHovering(mouseX, mouseY, xMult, y, 24, SLOT_SIZE)) {
                Trade trade = trades.get(index);
                float step = hasShiftDown() ? 1.0f : 0.1f;

                trade.setPriceMultiplier(trade.getPriceMultiplier() + (signum * step));

                return true;
            }
        }

        int maxScroll = Math.max(0, trades.size() - MAX_ROWS);
        if (maxScroll > 0) {
            this.tradesScrollOffset = Math.max(0, Math.min(maxScroll, this.tradesScrollOffset - signum));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;

        for (int i = 0; i < MAX_ROWS; i++) {
            int index = i + tradesScrollOffset;
            if (index >= trades.size()) break;

            Trade trade = trades.get(index);
            int y = START_Y + i * ROW_HEIGHT;

            if (handleSlotClick(mouseX, mouseY, getInputAX(centerX), y, button, trade.getInputA(), trade::setInputA)) return true;
            if (handleSlotClick(mouseX, mouseY, getInputBX(centerX), y, button, trade.getInputB(), trade::setInputB)) return true;
            if (handleSlotClick(mouseX, mouseY, getResultX(centerX), y, button, trade.getResult(), trade::setResult)) return true;

            int xDel = getDeleteX(centerX);
            if (isHovering(mouseX, mouseY, xDel, y + 3, 12, 12)) {
                trades.remove(index);

                int maxScroll = Math.max(0, trades.size() - MAX_ROWS);
                if (this.tradesScrollOffset > maxScroll) {
                    this.tradesScrollOffset = maxScroll;
                }
                if (this.selectedRow >= trades.size()) {
                    this.selectedRow = trades.isEmpty() ? -1 : trades.size() - 1;
                }
                return true;
            }

            if (button == 0) {
                if (isHovering(mouseX, mouseY, getUpX(centerX), y + 3, 12, 12)) {
                    moveTrade(index, -1);
                    return true;
                }
                if (isHovering(mouseX, mouseY, getDownX(centerX), y + 3, 12, 12)) {
                    moveTrade(index, 1);
                    return true;
                }

                if (isHovering(mouseX, mouseY, centerX - 75, y - 2, 191, 22)) {
                    this.selectedRow = index;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void moveTrade(int from, int direction) {
        int to = from + direction;
        if (to < 0 || to >= trades.size()) return;

        Collections.swap(trades, from, to);

        if (this.selectedRow == from) {
            this.selectedRow = to;
        } else if (this.selectedRow == to) {
            this.selectedRow = from;
        }

        int maxScroll = Math.max(0, trades.size() - MAX_ROWS);
        if (to < tradesScrollOffset) {
            this.tradesScrollOffset = Math.max(0, Math.min(maxScroll, to));
        } else if (to >= tradesScrollOffset + MAX_ROWS) {
            this.tradesScrollOffset = Math.max(0, Math.min(maxScroll, to - MAX_ROWS + 1));
        }
    }

    private boolean handleSlotClick(double mouseX, double mouseY, int slotX, int slotY, int button, ItemStack currentStack, java.util.function.Consumer<ItemStack> itemSetter) {
        if (isHovering(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE)) {
            if (button == 1) {
                itemSetter.accept(ItemStack.EMPTY);
            } else {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new ItemSelectorScreen(this, currentStack, itemSetter));
                }
            }
            return true;
        }
        return false;
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

    private int getInputAX(int centerX) { return centerX - 60; }
    private int getInputBX(int centerX) { return centerX - 25; }
    private int getResultX(int centerX) { return centerX + 18; }
    private int getMultX(int centerX) { return centerX + 42; }
    private int getDeleteX(int centerX) { return centerX + 72; }
    private int getUpX(int centerX) { return centerX + 88; }
    private int getDownX(int centerX) { return centerX + 102; }

    private boolean isHovering(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static class Trade {
        private ItemStack inputA = ItemStack.EMPTY;
        private ItemStack inputB = ItemStack.EMPTY;
        private ItemStack result = ItemStack.EMPTY;
        private float priceMultiplier = 1.0f;
        private int maxTrades = -1;
        private int resetTime = -1;

        public Trade() {}

        public ItemStack getInputA() { return inputA; }
        public void setInputA(ItemStack inputA) { this.inputA = nullCheck(inputA); }

        public ItemStack getInputB() { return inputB; }
        public void setInputB(ItemStack inputB) { this.inputB = nullCheck(inputB); }

        public ItemStack getResult() { return result; }
        public void setResult(ItemStack result) { this.result = nullCheck(result); }

        public float getPriceMultiplier() { return priceMultiplier; }
        public void setPriceMultiplier(float priceMultiplier) { 
            // Ограничиваем множитель от 0.1 до 10.0 и округляем до десятых во избежание ошибок float
            this.priceMultiplier = Math.max(0.1f, Math.min(10.0f, Math.round(priceMultiplier * 10.0f) / 10.0f)); 
        }

        public int getMaxTrades() { return maxTrades; }
        public void setMaxTrades(int maxTrades) { this.maxTrades = maxTrades; }

        public int getResetTime() { return resetTime; }
        public void setResetTime(int resetTime) { this.resetTime = resetTime; }

        private ItemStack nullCheck(ItemStack item) {
            if (item == null || item.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (item.getCount() <= 0) {
                item.setCount(1);
            }
            return item;
        } 
    }
}