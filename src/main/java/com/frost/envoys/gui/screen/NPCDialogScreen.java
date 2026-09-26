package com.frost.envoys.gui.screen;

import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.model.ActionDialog;
import com.frost.envoys.gui.bridges.DialogGuiBridge;
import com.frost.envoys.network.payload.SelectDialogAnswerPayload;
import com.frost.envoys.util.ColorUtils;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

    private static final ResourceLocation CUSTOM_FRAME = ResourceLocation.fromNamespaceAndPath("envoys", "gui_background");

    private final Component npcName;
    private final Component dialogText;
    private final List<DialogOption> options;
    private final UUID npcUuid;
    private boolean answered = false;

    private double dialogScrollAmount = 0.0;

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
        super.init();

        int frameHeight = (int) (this.height * 0.4);

        int buttonX = 10;
        int buttonWidth = (int) (this.width * 0.6);
        int buttonHeight = 22;
        int startY = 10 + frameHeight + 15;
        int spacing = 6;

        for (int i = 0; i < options.size(); i++) {
            if (i >= 5) break;
            DialogOption option = options.get(i);

            Component optionText = ColorUtils.parse(ColorUtils.toFormattedString(option.text()));

            Button optionButton = new Button(
                buttonX, 
                startY + i * (buttonHeight + spacing), 
                buttonWidth, 
                buttonHeight, 
                optionText, 
                (btn) -> {
                    this.answered = true;
                    option.onSelect().run();
                    this.onClose();
                }, 
                (supplier) -> supplier.get()
            ) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                    if (!this.active) {
                        guiGraphics.setColor(0.5F, 0.5F, 0.5F, 1.0F); 
                    } else if (this.isHoveredOrFocused()) {
                        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F); 
                    } else {
                        guiGraphics.setColor(0.75F, 0.75F, 0.75F, 1.0F); 
                    }

                    guiGraphics.blitSprite(CUSTOM_FRAME, this.getX(), this.getY(), this.width, this.height);
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

                    int textColor = this.active ? (this.isHoveredOrFocused() ? 0xFFFFFFA0 : 0xFFFFFFFF) : 0xFFA0A0A0;

                    Component message = this.getMessage();
                    int textWidth = NPCDialogScreen.this.font.width(message);
                    int maxAllowedWidth = this.width - 12;

                    int textY = this.getY() + (this.height - 8) / 2;

                    if (textWidth <= maxAllowedWidth) {
                        guiGraphics.drawCenteredString(
                            NPCDialogScreen.this.font, 
                            message, 
                            this.getX() + this.width / 2, 
                            textY, 
                            textColor
                        );
                    } else {
                        double maxOffset = textWidth - maxAllowedWidth;
                        double time = Util.getMillis() / 1000.0;

                        double speed = 1;
                        double rawSin = Math.sin(time * speed);
                        double clamped = Math.max(-0.75, Math.min(0.75, rawSin)) / 0.75;
                        double progress = (clamped + 1.0) / 2.0;
                        
                        int offsetX = (int) (progress * maxOffset);

                        int minX = this.getX() + 6;
                        int maxX = minX + maxAllowedWidth;

                        guiGraphics.enableScissor(minX, this.getY(), maxX, this.getY() + this.height);
                        guiGraphics.drawString(
                            NPCDialogScreen.this.font, 
                            message, 
                            minX - offsetX, 
                            textY, 
                            textColor, 
                            true
                        );
                        guiGraphics.disableScissor();
                    }
                }
            };

            this.addRenderableWidget(optionButton);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int frameY = 10;
        int frameHeight = (int) (this.height * 0.4);

        if (mouseY >= frameY && mouseY <= frameY + frameHeight) {
            int textWidth = this.width - 20 - 30;
            List<FormattedCharSequence> lines = this.font.split(this.dialogText, textWidth);
            int totalTextHeight = lines.size() * 10;
            int visibleHeight = frameHeight - 35;
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

        int frameX = 10;
        int frameY = 10;
        int frameWidth = this.width - 20;
        int frameHeight = (int) (this.height * 0.4);
        int borderThickness = 6;

        guiGraphics.blitSprite(CUSTOM_FRAME, frameX, frameY, frameWidth, frameHeight);

        int textX = frameX + borderThickness + 10;
        int textY = frameY + borderThickness + 10;
        guiGraphics.drawString(this.font, this.npcName, textX, textY, 0xFFFFFF00, true);

        int dialogBoxY = textY + 15;
        int dialogBoxHeight = frameY + frameHeight - borderThickness - dialogBoxY;
        int dialogWidth = frameWidth - borderThickness * 2 - 20;

        List<FormattedCharSequence> lines = this.font.split(this.dialogText, dialogWidth);
        int totalTextHeight = lines.size() * 10;
        int maxDialogScroll = Math.max(0, totalTextHeight - dialogBoxHeight);
        this.dialogScrollAmount = Mth.clamp(this.dialogScrollAmount, 0, maxDialogScroll);

        guiGraphics.enableScissor(textX, dialogBoxY, textX + dialogWidth, dialogBoxY + dialogBoxHeight);
        for (int i = 0; i < lines.size(); i++) {
            int lineY = dialogBoxY + i * 10 - (int) this.dialogScrollAmount;
            if (lineY + 10 >= dialogBoxY && lineY <= dialogBoxY + dialogBoxHeight) {
                guiGraphics.drawString(this.font, lines.get(i), textX, lineY, 0xFFFFFFFF, true);
            }
        }
        guiGraphics.disableScissor();

        if (maxDialogScroll > 0) {
            int scrollbarX = frameX + frameWidth - borderThickness - 8;
            int scrollbarWidth = 4;
            int trackHeight = dialogBoxHeight;

            guiGraphics.fill(scrollbarX, dialogBoxY, scrollbarX + scrollbarWidth, dialogBoxY + trackHeight, 0x80000000);

            int thumbHeight = Math.max(10, (int) ((float) trackHeight / totalTextHeight * trackHeight));
            int thumbY = dialogBoxY + (int) ((dialogScrollAmount / maxDialogScroll) * (trackHeight - thumbHeight));

            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFFA0A0A0);
        }

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
}