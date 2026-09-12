package com.frost.envoys.client.overlay;

import java.util.List;

import com.frost.envoys.client.quest.ClientQuestTracker;
import com.frost.envoys.network.payload.ClientQuestEntry;
import com.frost.envoys.quest.QuestInventoryUtil;
import com.frost.envoys.quest.QuestType;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CurrentQuestOverlay implements LayeredDraw.Layer {

    private static final ResourceLocation PANEL =
        ResourceLocation.fromNamespaceAndPath("envoys", "current_quest");

    private static final int MIN_W = 48;
    private static final int MAX_W = 144;
    private static final int MIN_H = 33;

    private static final int MARGIN = 10;
    private static final float ALPHA = 0.8F;

    private static final int TEXT_DARK = 0xFF3A2A18;
    private static final int TEXT_MUTED = 0xFF6A5A48;
    private static final int TEXT_DONE = 0xFF2A7A2A;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }

        ClientQuestEntry entry = ClientQuestTracker.get().activeForHud();
        if (entry == null) {
            return;
        }

        String title = entry.title() == null ? "" : entry.title();
        String npcName = entry.npcName() == null ? "" : entry.npcName();
        boolean hasNpc = !npcName.isBlank();
        boolean hasItem = entry.type() == QuestType.ITEM;

        int current = currentProgress(minecraft, entry);
        int target = Math.max(1, entry.targetProgress());
        Component progress = Component.translatable("envoys.current_quest.progress", current, target);

        int titleRawWidth = minecraft.font.width(title);
        int npcWidth = hasNpc ? minecraft.font.width(npcName) : 0;
        int progressWidth = minecraft.font.width(progress) + (hasItem ? 24 : 0);

        int maxContentWidth = Math.max(titleRawWidth, Math.max(npcWidth, progressWidth));
        int desiredW = maxContentWidth + 28;

        int maxScreenW = Math.max(MIN_W, guiGraphics.guiWidth() - (MARGIN * 2));
        int effectiveMaxW = Math.min(MAX_W, maxScreenW);
        int w = Math.max(MIN_W, Math.min(desiredW, effectiveMaxW));

        int textWidth = w - 28;

        List<FormattedCharSequence> titleLines = minecraft.font.split(Component.literal(title), textWidth);

        int contentHeight = 14 + (titleLines.size() * 10);
        if (hasNpc) {
            contentHeight += 12;
        }

        int bottomSectionHeight = 26;
        int rawH = Math.max(MIN_H, contentHeight + bottomSectionHeight);
        int step = Math.max(1, (MIN_H + 1) / 2);
        int h = ((rawH + step - 1) / step) * step;

        int x = guiGraphics.guiWidth() - w - MARGIN;
        int y = MARGIN;

        guiGraphics.setColor(1F, 1F, 1F, ALPHA);
        RenderSystem.enableBlend();
        guiGraphics.blitSprite(PANEL, x, y, w, h);
        guiGraphics.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();

        int textX = x + 14;
        int lineY = y + 20;

        for (FormattedCharSequence line : titleLines) {
            guiGraphics.drawString(minecraft.font, line, textX, lineY, TEXT_DARK, false);
            lineY += 10;
        }

        if (hasNpc) {
            guiGraphics.drawString(minecraft.font, npcName, textX, lineY + 2, TEXT_MUTED, false);
        }

        guiGraphics.drawString(minecraft.font, progress, textX, y + h - 16, entry.completed() ? TEXT_DONE : TEXT_DARK, false);

        if (hasItem) {
            Item item = QuestInventoryUtil.resolveItem(entry.itemId());
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                int itemX = x + w - 32;
                int itemY = y + h - 20;
                guiGraphics.renderFakeItem(stack, itemX, itemY);
                guiGraphics.renderItemDecorations(minecraft.font, stack, itemX, itemY);
            }
        }
    }

    private int currentProgress(Minecraft minecraft, ClientQuestEntry entry) {
        if (entry.type() == QuestType.ITEM) {
            int count = QuestInventoryUtil.count(minecraft.player, entry.itemId());
            return Math.min(Math.max(1, entry.targetProgress()), count);
        }
        return Math.max(0, entry.currentProgress());
    }
}