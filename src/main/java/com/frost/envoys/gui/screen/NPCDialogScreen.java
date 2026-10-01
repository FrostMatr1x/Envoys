package com.frost.envoys.gui.screen;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.model.ActionDialog;
import com.frost.envoys.gui.bridges.DialogGuiBridge;
import com.frost.envoys.client.gui.DialogLayout;
import com.frost.envoys.network.payload.SelectDialogAnswerPayload;
import com.frost.envoys.util.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NPCDialogScreen extends Screen implements DialogGuiBridge {

    private static final ResourceLocation CUSTOM_FRAME = ResourceLocation.fromNamespaceAndPath("envoys", "dialog_background");
    private static final ResourceLocation SCROLLER = ResourceLocation.fromNamespaceAndPath("envoys", "scroller");
    private static final ResourceLocation SCROLLER_DISABLED = ResourceLocation.fromNamespaceAndPath("envoys", "scroller_disabled");

    protected static final ResourceLocation BUTTON = ResourceLocation.fromNamespaceAndPath("envoys", "background");
    protected static final ResourceLocation BUTTON_DISABLED = ResourceLocation.fromNamespaceAndPath("envoys", "background_hover");

    protected static final WidgetSprites SPRITES_BUTTON = new WidgetSprites(BUTTON, BUTTON_DISABLED);

    private final Component npcName;
    private final Component dialogText;
    private final List<DialogOption> options;
    private final UUID npcUuid;
    private boolean answered = false;

    private double dialogScrollAmount = 0.0;

    private DialogLayout layout = DialogLayout.get();

    private int nameColor = 0xFFFFFF00;
    private int textColor = 0xFFFFFFFF;
    private int frameWidthCache;
    private int frameHeightCache;

    public NPCDialogScreen(Component npcName, Component dialogText, List<DialogOption> options) {
        this(npcName, dialogText, options, null);
    }

    public NPCDialogScreen(Component npcName, Component dialogText, List<DialogOption> options, UUID npcUuid) {
        super(Component.translatable("envoys.gui.dialog.title"));
        this.npcName = ColorUtils.parse(ColorUtils.toFormattedString(npcName));
        this.dialogText = ColorUtils.parse(ColorUtils.toFormattedString(dialogText));
        this.options = options;
        this.npcUuid = npcUuid;
    }

    @Override
    protected void init() {
        if (this.minecraft != null) {
            DialogLayout.reload(this.minecraft.getResourceManager());
        }
        this.layout = DialogLayout.get();

        super.init();

        DialogLayout.Frame frame = this.layout.frame;
        DialogLayout.OptionsCfg optionsCfg = this.layout.options;

        this.frameWidthCache = frame.resolveWidth(this.width, 0);
        this.frameHeightCache = frame.resolveHeight(this.height, 0);
        this.nameColor = this.layout.name.colorOrDefault();
        this.textColor = this.layout.text.colorOrDefault();

        int frameY = frame.y;
        int frameHeight = this.frameHeightCache;

        int buttonX = optionsCfg.x;
        int buttonWidth = optionsCfg.resolveWidth(this.width);
        int buttonHeight = optionsCfg.height;
        int startY = frameY + frameHeight + optionsCfg.yOffsetFromFrame;
        int spacing = optionsCfg.spacing;
        int maxVisible = Math.max(0, optionsCfg.maxVisible);

        for (int i = 0; optionsCfg.visible && i < options.size() && i < maxVisible; i++) {
            DialogOption option = options.get(i);

            Component optionText = ColorUtils.parse(ColorUtils.toFormattedString(option.text()));

            this.addRenderableWidget(new DialogOptionButton(
                buttonX,
                startY + i * (buttonHeight + spacing),
                buttonWidth,
                buttonHeight,
                optionText,
                btn -> {
                    this.answered = true;
                    option.onSelect().run();
                    this.onClose();
                }
            ));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        DialogLayout.Frame frame = this.layout.frame;
        DialogLayout.TextCfg textCfg = this.layout.text;
        int frameY = frame.y;
        int frameHeight = this.frameHeightCache;

        if (mouseY >= frameY && mouseY <= frameY + frameHeight) {
            int frameX = frame.x;
            int frameWidth = this.frameWidthCache;
            int textWidth = Math.max(1, frameWidth - frame.borderThickness * 2 - textCfg.padLeft - textCfg.padRight);
            List<FormattedCharSequence> lines = this.font.split(this.dialogText, (int) (textWidth / textCfg.safeScale()));
            int totalTextHeight = (int) (lines.size() * textCfg.lineHeight * textCfg.safeScale());
            int visibleHeight = Math.max(1, frameHeight - frame.borderThickness * 2 - textCfg.padTop - textCfg.padBottom);
            int maxScroll = Math.max(0, totalTextHeight - visibleHeight);

            this.dialogScrollAmount = Mth.clamp(this.dialogScrollAmount - scrollY * 12, 0, maxScroll);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x35101010, 0x45101010);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        DialogLayout.Frame frame = this.layout.frame;
        DialogLayout.NameCfg nameCfg = this.layout.name;
        DialogLayout.TextCfg textCfg = this.layout.text;
        DialogLayout.TextCfg.Scroller scrollerCfg = textCfg.scroller;

        int frameX = frame.x;
        int frameY = frame.y;
        int frameWidth = this.frameWidthCache;
        int frameHeight = this.frameHeightCache;
        int borderThickness = frame.borderThickness;

        guiGraphics.blitSprite(CUSTOM_FRAME, frameX, frameY, frameWidth, frameHeight);

        float nameScale = nameCfg.safeScale();
        float textScale = textCfg.safeScale();

        int nameX = frameX + borderThickness + nameCfg.padLeft;
        int nameY = frameY + borderThickness + nameCfg.padTop;
        int nameHeight = (int) (this.font.lineHeight * nameScale);

        if (nameCfg.visible) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(nameX, nameY, 0.0F);
            guiGraphics.pose().scale(nameScale, nameScale, 1.0F);
            guiGraphics.drawString(this.font, this.npcName, 0, 0, this.nameColor, nameCfg.shadow);
            guiGraphics.pose().popPose();
        }

        int dialogBoxY = nameY + nameHeight + textCfg.offsetFromName;
        int dialogBoxHeight = Math.max(1, frameY + frameHeight - borderThickness - dialogBoxY - textCfg.padBottom);
        int dialogWidth = Math.max(1, frameWidth - borderThickness * 2 - textCfg.padLeft - textCfg.padRight);
        int textX = frameX + borderThickness + textCfg.padLeft;

        List<FormattedCharSequence> lines = this.font.split(this.dialogText, (int) (dialogWidth / textScale));
        int totalTextHeight = (int) (lines.size() * textCfg.lineHeight * textScale);
        int maxDialogScroll = Math.max(0, totalTextHeight - dialogBoxHeight);
        this.dialogScrollAmount = Mth.clamp(this.dialogScrollAmount, 0, maxDialogScroll);

        guiGraphics.enableScissor(textX, dialogBoxY, textX + dialogWidth, dialogBoxY + dialogBoxHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(textX, dialogBoxY, 0.0F);
        guiGraphics.pose().scale(textScale, textScale, 1.0F);
        for (int i = 0; i < lines.size(); i++) {
            int lineY = i * textCfg.lineHeight - (int) (this.dialogScrollAmount / textScale);
            if (lineY + textCfg.lineHeight >= 0 && lineY <= (int) (dialogBoxHeight / textScale)) {
                guiGraphics.drawString(this.font, lines.get(i), 0, lineY, this.textColor, textCfg.shadow);
            }
        }
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();

        int scrollerX = frameX + frameWidth - borderThickness - scrollerCfg.offsetRight;
        int scrollerHeight = scrollerCfg.height;
        if (maxDialogScroll > 0) {
            int maxOffset = Math.max(0, dialogBoxHeight - scrollerHeight);
            int scrollerY = dialogBoxY + (int) ((dialogScrollAmount / maxDialogScroll) * maxOffset);
            guiGraphics.blitSprite(SCROLLER, scrollerX, scrollerY, scrollerCfg.width, scrollerHeight);
        } else {
            guiGraphics.blitSprite(SCROLLER_DISABLED, scrollerX, dialogBoxY, scrollerCfg.width, scrollerHeight);
        }

        RenderSystem.disableBlend();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (npcUuid != null && !answered) {
            PacketDistributor.sendToServer(new SelectDialogAnswerPayload(npcUuid, ""));
        }
        super.onClose();
    }

    public record DialogOption(Component text, Runnable onSelect) {}

    @Override
    public void open(Player player, UUID npcUuid, ActionDialog dialog, AnswerCallback callback) {
        List<DialogOption> options = new ArrayList<>();

        for (Map.Entry<String, String> entry : dialog.answers.entrySet()) {
            String answerText = entry.getKey();
            String nextActionId = entry.getValue();

            Component textComponent = ColorUtils.parse(answerText);

            Runnable onSelect = () -> {
                if (callback != null) {
                    callback.onAnswer(nextActionId); 
                }
                
                Minecraft.getInstance().setScreen(null);
            };

            options.add(new DialogOption(textComponent, onSelect));
        }

        String title = "NPC";
        NPCInteractManager manager = NPCInteractManager.byUUID(npcUuid).orElse(null);
        if (manager != null && manager.passport != null && !manager.passport.npcName.isBlank()) {
            title = manager.passport.npcName;
        } else if (dialog.NPCName != null && !dialog.NPCName.isBlank()) {
            title = dialog.NPCName;
        }

        Component npcNameComponent = ColorUtils.parse(title);
        Component dialogTextComponent = dialog.npcMessage != null ? dialog.npcMessage : Component.empty();

        Minecraft.getInstance().setScreen(new NPCDialogScreen(npcNameComponent, dialogTextComponent, options));
    }
    
    public class DialogOptionButton extends Button {

        public DialogOptionButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            float optionScale = NPCDialogScreen.this.layout.options.safeScale();

            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();

            guiGraphics.blitSprite(NPCDialogScreen.SPRITES_BUTTON.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());

            int textColor = this.active ? (this.isHoveredOrFocused() ? 0xFFFFFFA0 : 0xFFFFFFFF) : 0xFFA0A0A0;

            Component message = this.getMessage();
            int textWidth = (int) (font.width(message) * optionScale);
            int maxAllowedWidth = (int) ((this.width - 12) / optionScale);
            int textY = this.getY() + (this.height - (int) (8 * optionScale)) / 2;

            if (textWidth <= this.width - 12) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(this.getX() + this.width / 2.0F, textY, 0.0F);
                guiGraphics.pose().scale(optionScale, optionScale, 1.0F);
                guiGraphics.drawCenteredString(font, message, 0, 0, textColor);
                guiGraphics.pose().popPose();
            } else {
                double maxOffset = textWidth - (this.width - 12);
                double time = Util.getMillis() / 1000.0;

                double speed = 1.0;
                double rawSin = Math.sin(time * speed);
                double clamped = Math.max(-0.75, Math.min(0.75, rawSin)) / 0.75;
                double progress = (clamped + 1.0) / 2.0;

                int offsetX = (int) (progress * maxOffset);

                int minX = this.getX() + 6;
                int maxX = minX + maxAllowedWidth;

                guiGraphics.enableScissor(minX, this.getY(), maxX, this.getY() + this.height);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(minX - offsetX, textY, 0.0F);
                guiGraphics.pose().scale(optionScale, optionScale, 1.0F);
                guiGraphics.drawString(font, message, 0, 0, textColor, true);
                guiGraphics.pose().popPose();
                guiGraphics.disableScissor();
            }
        }
    }
}