package com.frost.envoys.gui.screen;

import java.util.List;

import com.frost.envoys.client.quest.ClientQuestTracker;
import com.frost.envoys.gui.screen.NPCTradeScreen.CustomTradeAllButton;
import com.frost.envoys.network.payload.ClientQuestEntry;
import com.frost.envoys.quest.QuestInventoryUtil;
import com.frost.envoys.quest.QuestType;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class QuestWallScreen extends Screen {

    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath("envoys", "quest_wall");
    private static final ResourceLocation SCROLLER = ResourceLocation.fromNamespaceAndPath("envoys", "scroller");
    private static final ResourceLocation SCROLLER_DISABLED = ResourceLocation.fromNamespaceAndPath("envoys", "scroller_disabled");

    protected static final ResourceLocation TRADE_BUTTON = ResourceLocation.fromNamespaceAndPath("envoys", "trade_button");
    protected static final ResourceLocation TRADE_BUTTON_DISABLED = ResourceLocation.fromNamespaceAndPath("envoys", "trade_button_disabled");

    protected static final WidgetSprites SPRITES_BUTTON = new WidgetSprites(TRADE_BUTTON, TRADE_BUTTON_DISABLED);

    private static final int PAD = 12;
    private static final int HEADER_H = 18;
    private static final int FOOTER_H = 24;
    private static final int ROW_H = 34;
    private static final int SCROLLER_H = 27;

    private double scrollOff;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public QuestWallScreen() {
        super(Component.translatable("envoys.quest_wall.title"));
    }

    @Override
    protected void init() {
        super.init();

        this.panelW = Math.min(this.width - 40, 260);
        this.panelH = Math.min(this.height - 40, 220);
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;

        this.addRenderableWidget(this.addRenderableWidget(new CustomButton(
            this.panelX + (this.panelW - 100) / 2, this.panelY + this.panelH - PAD - 18, 100, 18,
            Component.literal("Готово"),
            button -> this.onClose()
        )));

        this.scrollOff = Mth.clamp(this.scrollOff, 0, this.maxScroll());
    }

    private int listTop() {
        return this.panelY + PAD + HEADER_H;
    }

    private int listBottom() {
        return this.panelY + this.panelH - PAD - FOOTER_H;
    }

    private int listHeight() {
        return Math.max(1, this.listBottom() - this.listTop());
    }

    private int maxScroll() {
        return Math.max(0, ClientQuestTracker.get().entries().size() * ROW_H - this.listHeight());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.setColor(1F, 1F, 1F, 1F);
        guiGraphics.blitSprite(PANEL, this.panelX, this.panelY, this.panelW, this.panelH);

        guiGraphics.drawCenteredString(this.font, this.title, this.panelX + this.panelW / 2, this.panelY + PAD, 0xFFFFFF);

        List<ClientQuestEntry> entries = ClientQuestTracker.get().entries();

        if (entries.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("envoys.quest_wall.empty"),
                this.panelX + this.panelW / 2, this.panelY + this.panelH / 2, 0xA0A0A0);
        } else {
            this.renderRows(guiGraphics, entries);
            this.renderScroller(guiGraphics, entries.size());
        }

        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderRows(GuiGraphics guiGraphics, List<ClientQuestEntry> entries) {
        int innerX = this.panelX + PAD;
        int innerW = this.panelW - 2 * PAD;
        int top = this.listTop();
        int bottom = this.listBottom();

        guiGraphics.enableScissor(innerX, top, innerX + innerW, bottom);

        String pinned = ClientQuestTracker.get().pinnedQuestUuid();
        int first = (int) (this.scrollOff / ROW_H);
        for (int i = Math.max(0, first); i < entries.size(); i++) {
            int rowY = top - (int) this.scrollOff + i * ROW_H;
            if (rowY + ROW_H < top) {
                continue;
            }
            if (rowY > bottom) {
                break;
            }
            this.renderRow(guiGraphics, entries.get(i), innerX, rowY, innerW, pinned);
        }

        guiGraphics.disableScissor();
    }

    private void renderRow(GuiGraphics guiGraphics, ClientQuestEntry entry, int x, int y, int width, String pinnedUuid) {
        boolean isPinned = entry.questUuid() != null && entry.questUuid().equals(pinnedUuid);
        guiGraphics.drawString(this.font, isPinned ? "[◦]" : "[ ]", x + 2, y + 11, isPinned ? 0xFFE0B000 : 0xFF808080, false);

        String title = entry.title() == null ? "" : entry.title();
        guiGraphics.drawString(this.font, title, x + 24, y + 5, 0xFFFFFF, false);

        String npcName = entry.npcName() == null ? "" : entry.npcName();
        if (!npcName.isBlank()) {
            guiGraphics.drawString(this.font, npcName, x + 24, y + 18, 0xA0A0A0, false);
        }

        int current = this.currentProgress(entry);
        int target = Math.max(1, entry.targetProgress());
        String progress = "";

        if (!entry.completed())
            progress = Component.translatable("envoys.current_quest.progress", current, target).getString();
        else
            progress = "✔  ";

        guiGraphics.drawString(this.font, progress, x + width - this.font.width(progress) - 8, y + 11,
            entry.completed() ? 0xFF55FF55 : 0xFFFFFF, false);
    }

    private void renderScroller(GuiGraphics guiGraphics, int entryCount) {
        int visible = this.listHeight() / ROW_H;
        int scrollerX = this.panelX + this.panelW - PAD - 6;
        int scrollerY = this.listTop();

        if (entryCount > visible) {
            int track = this.listHeight() - SCROLLER_H;
            int maxScroll = this.maxScroll();
            int offset = maxScroll <= 0 ? 0 : (int) (this.scrollOff / maxScroll * track);
            guiGraphics.blitSprite(SCROLLER, scrollerX, scrollerY + offset, 6, SCROLLER_H);
        } else {
            guiGraphics.blitSprite(SCROLLER_DISABLED, scrollerX, scrollerY, 6, SCROLLER_H);
        }
    }

    private int currentProgress(ClientQuestEntry entry) {
        if (entry.type() == QuestType.ITEM) {
            Minecraft minecraft = Minecraft.getInstance();
            int count = QuestInventoryUtil.count(minecraft.player, entry.itemId());
            return Math.min(Math.max(1, entry.targetProgress()), count);
        }
        return Math.max(0, entry.currentProgress());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = this.maxScroll();
        if (maxScroll > 0) {
            this.scrollOff = Mth.clamp(this.scrollOff - scrollY * ROW_H, 0, maxScroll);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int innerX = this.panelX + PAD;
        int innerW = this.panelW - 2 * PAD;
        int top = this.listTop();
        int bottom = this.listBottom();

        if (button == 0 && mouseX >= innerX && mouseX <= innerX + innerW && mouseY >= top && mouseY < bottom) {
            List<ClientQuestEntry> entries = ClientQuestTracker.get().entries();
            int index = (int) ((mouseY - top + this.scrollOff) / ROW_H);
            if (index >= 0 && index < entries.size()) {
                String uuid = entries.get(index).questUuid();
                if (uuid != null && !uuid.isBlank()) {
                    ClientQuestTracker tracker = ClientQuestTracker.get();
                    tracker.setPinned(uuid.equals(tracker.pinnedQuestUuid()) ? null : uuid);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    class CustomButton extends Button {
        public CustomButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            Minecraft minecraft = Minecraft.getInstance();
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();

            guiGraphics.blitSprite(NPCTradeScreen.SPRITES_BUTTON.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());

            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            int color = this.getFGColor();
            this.renderString(guiGraphics, minecraft.font, color | Mth.ceil(this.alpha * 255.0F) << 24);
        }
    }
}
