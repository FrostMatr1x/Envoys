package com.frost.envoys.gui.screen;

import com.frost.envoys.network.payload.TradeAllPayload;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.PacketDistributor;

public class NPCTradeScreen extends AbstractContainerScreen<MerchantMenu> {

    private static final float ITEM_SCALE = 0.9F;

    private static final ResourceLocation TRADE_GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath("envoys", "textures/gui/sprites/trade_menu.png");
    private static final ResourceLocation TRADE_ARROW = ResourceLocation.fromNamespaceAndPath("envoys", "textures/gui/sprites/trade_arrow.png");

    private static final ResourceLocation SCROLLER = ResourceLocation.fromNamespaceAndPath("envoys", "scroller");
    private static final ResourceLocation SCROLLER_DISABLED = ResourceLocation.fromNamespaceAndPath("envoys", "scroller_disabled");

    protected static final ResourceLocation TRADE_BUTTON = ResourceLocation.fromNamespaceAndPath("envoys", "trade_button");
    protected static final ResourceLocation TRADE_BUTTON_DISABLED = ResourceLocation.fromNamespaceAndPath("envoys", "trade_button_disabled");

    protected static final WidgetSprites SPRITES_BUTTON = new WidgetSprites(TRADE_BUTTON, TRADE_BUTTON_DISABLED);

    private final CustomTradeOfferButton[] customTradeButtons = new CustomTradeOfferButton[7];

    private Button tradeAllButton;

    private int scrollOff = 0;
    private int shopItem = 0;
    private boolean isDragging = false;

    public NPCTradeScreen(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 276;
        this.imageHeight = 166;
        this.inventoryLabelX = 107;
    }

    private void postButtonClick(int selectedTrade) {
        this.shopItem = selectedTrade;
        this.menu.setSelectionHint(selectedTrade);
        this.menu.tryMoveItems(selectedTrade);
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().send(new ServerboundSelectTradePacket(selectedTrade));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        int startY = j + 16 + 2;

        for (int l = 0; l < 7; ++l) {
            int buttonIndex = l;
            CustomTradeOfferButton button = new CustomTradeOfferButton(i + 5, startY, buttonIndex, (b) -> {
                if (b instanceof CustomTradeOfferButton customBtn) {
                    int selectedTrade = customBtn.getIndex() + this.scrollOff;
                    if (selectedTrade >= 0 && selectedTrade < this.menu.getOffers().size()) {
                        this.postButtonClick(selectedTrade);
                    }
                }
            });

            this.customTradeButtons[l] = this.addRenderableWidget(button);
            startY += 20;
        }
        
        this.tradeAllButton = this.addRenderableWidget(new CustomTradeAllButton(
                i + 113, j + 12, 80, 16,
                Component.translatable("envoys.gui.trade.trade_all"),
                b -> this.tradeAll()
        ));
    }

    private void tradeAll() {
        MerchantOffers offers = this.menu.getOffers();
        if (offers.isEmpty() || this.shopItem < 0 || this.shopItem >= offers.size()) return;
        PacketDistributor.sendToServer(new TradeAllPayload(this.shopItem));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TRADE_GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 512, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        MerchantOffers offers = this.menu.getOffers();
        if (!offers.isEmpty()) {
            int i = (this.width - this.imageWidth) / 2;
            int j = (this.height - this.imageHeight) / 2;

            renderCustomScroller(guiGraphics, i, j, offers);

            int startY = j + 16 + 2;

            for (int l = 0; l < 7; ++l) {
                int offerIndex = l + this.scrollOff;
                CustomTradeOfferButton button = this.customTradeButtons[l];

                if (button != null) {
                    if (offerIndex < offers.size()) {
                        button.visible = true;
                        MerchantOffer offer = offers.get(offerIndex);

                        ItemStack costA = offer.getCostA();
                        ItemStack costB = offer.getCostB();
                        ItemStack result = offer.getResult();

                        int itemY = startY + (int) ((20.0F - (16.0F * ITEM_SCALE)) / 2.0F);
                        int buttonX = i + 5;

                        renderScaledItem(guiGraphics, costA, buttonX + 5, itemY, ITEM_SCALE);

                        if (!costB.isEmpty()) {
                            renderScaledItem(guiGraphics, costB, buttonX + 35, itemY, ITEM_SCALE);
                        }

                        int arrowX = buttonX + 52;
                        int arrowY = startY + 5;

                        guiGraphics.blit(TRADE_ARROW, arrowX, arrowY, 0, 0, 10, 9, 10, 9);

                        renderScaledItem(guiGraphics, result, buttonX + 68, itemY, ITEM_SCALE);
                    } else {
                        button.visible = false;
                    }
                }
                startY += 20;
            }

            for (CustomTradeOfferButton button : this.customTradeButtons) {
                if (button != null && button.isHoveredOrFocused() && button.visible) {
                    button.renderToolTip(guiGraphics, mouseX, mouseY);
                }
            }
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderScaledItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y, float scale) {
        if (stack.isEmpty()) return;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 100.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);

        guiGraphics.renderFakeItem(stack, 0, 0);
        guiGraphics.renderItemDecorations(this.font, stack, 0, 0);

        guiGraphics.pose().popPose();
    }

    private void renderCustomScroller(GuiGraphics guiGraphics, int posX, int posY, MerchantOffers merchantOffers) {
        int maxScroll = merchantOffers.size() - 7;
        int scrollerX = posX + 94;
        int scrollerY = posY + 18;

        if (maxScroll > 0) {
            int scrollBarLength = 139 - 27;
            int offsetY = (int) ((float) this.scrollOff / (float) maxScroll * scrollBarLength);

            guiGraphics.blitSprite(SCROLLER, scrollerX, scrollerY + offsetY, 6, 27);
        } else {
            guiGraphics.blitSprite(SCROLLER_DISABLED, scrollerX, scrollerY, 6, 27);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = this.menu.getOffers().size() - 7;
        if (maxScroll > 0) {
            this.scrollOff = Mth.clamp((int) ((double) this.scrollOff - scrollY), 0, maxScroll);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.isDragging = false;
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        if (this.menu.getOffers().size() > 7 
                && mouseX > (double)(i + 94) && mouseX < (double)(i + 94 + 6) 
                && mouseY > (double)(j + 18) && mouseY <= (double)(j + 18 + 139)) {
            this.isDragging = true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int maxScroll = this.menu.getOffers().size() - 7;
        if (this.isDragging && maxScroll > 0) {
            int j = (this.height - this.imageHeight) / 2 + 18;
            int k = j + 139;
            float f = ((float) mouseY - (float) j - 13.5F) / ((float) (k - j) - 27.0F);
            f = f * (float) maxScroll + 0.5F;
            this.scrollOff = Mth.clamp((int) f, 0, maxScroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    class CustomTradeAllButton extends Button {
        public CustomTradeAllButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
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

    class CustomTradeOfferButton extends Button {

        private final int index;

        public CustomTradeOfferButton(int x, int y, int index, Button.OnPress onPress) {
            super(x, y, 88, 20, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
            this.index = index;
        }

        public int getIndex() {
            return this.index;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            Minecraft minecraft = Minecraft.getInstance();
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            guiGraphics.blitSprite(SPRITES_BUTTON.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            int i = this.getFGColor();
            this.renderString(guiGraphics, minecraft.font, i | Mth.ceil(this.alpha * 255.0F) << 24);
        }

        public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            MerchantOffers offers = NPCTradeScreen.this.menu.getOffers();
            int offerIndex = this.index + NPCTradeScreen.this.scrollOff;

            if (this.isHovered && offerIndex < offers.size()) {
                MerchantOffer offer = offers.get(offerIndex);

                if (mouseX < this.getX() + 25) {
                    guiGraphics.renderTooltip(NPCTradeScreen.this.font, offer.getCostA(), mouseX, mouseY);
                } else if (mouseX < this.getX() + 55 && mouseX > this.getX() + 28) {
                    ItemStack costB = offer.getCostB();
                    if (!costB.isEmpty()) {
                        guiGraphics.renderTooltip(NPCTradeScreen.this.font, costB, mouseX, mouseY);
                    }
                } else if (mouseX > this.getX() + 60) {
                    guiGraphics.renderTooltip(NPCTradeScreen.this.font, offer.getResult(), mouseX, mouseY);
                }
            }
        }
    }
}