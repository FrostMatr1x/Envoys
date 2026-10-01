package com.frost.envoys.gui.screen;

import com.frost.envoys.client.gui.TradeLayout;
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
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class NPCTradeScreen extends AbstractContainerScreen<MerchantMenu> {

    private static final ResourceLocation TRADE_GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath("envoys", "textures/gui/sprites/trade_menu.png");
    private static final ResourceLocation TRADE_ARROW = ResourceLocation.fromNamespaceAndPath("envoys", "textures/gui/sprites/trade_arrow.png");

    private static final ResourceLocation SCROLLER = ResourceLocation.fromNamespaceAndPath("envoys", "scroller");
    private static final ResourceLocation SCROLLER_DISABLED = ResourceLocation.fromNamespaceAndPath("envoys", "scroller_disabled");

    protected static final ResourceLocation MERCH_BUTTON = ResourceLocation.fromNamespaceAndPath("envoys", "merch");
    protected static final ResourceLocation MERCH_BUTTON_DISABLED = ResourceLocation.fromNamespaceAndPath("envoys", "merch_hover");

    protected static final WidgetSprites MERCH_SPRITES_BUTTON = new WidgetSprites(MERCH_BUTTON, MERCH_BUTTON_DISABLED);

    protected static final ResourceLocation TRADE_ALL = ResourceLocation.fromNamespaceAndPath("envoys", "trade_arrow_disable");
    protected static final ResourceLocation TRADE_ALL_HOVER = ResourceLocation.fromNamespaceAndPath("envoys", "trade_arrow");

    protected static final WidgetSprites TRADE_ALL_SPRITES = new WidgetSprites(TRADE_ALL, TRADE_ALL_HOVER);

    private final List<CustomTradeOfferButton> customTradeButtons = new ArrayList<>();

    private Button tradeAllButton;

    private int scrollOff = 0;
    private int shopItem = 0;
    private boolean isDragging = false;

    private TradeLayout layout = TradeLayout.get();

    public NPCTradeScreen(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        applyLayout(TradeLayout.get());
    }

    private void applyLayout(TradeLayout layout) {
        this.imageWidth = layout.image.width;
        this.imageHeight = layout.image.height;
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
        if (this.minecraft != null) {
            TradeLayout.reload(this.minecraft.getResourceManager());
        }
        this.layout = TradeLayout.get();
        applyLayout(this.layout);

        super.init();
        this.clearWidgets();
        this.customTradeButtons.clear();

        TradeLayout.ListCfg list = this.layout.list;
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        int startY = j + list.y;

        int rows = list.safeRows();
        for (int l = 0; l < rows; ++l) {
            final int buttonIndex = l;
            CustomTradeOfferButton button = new CustomTradeOfferButton(
                    i + list.x, startY, list.offerWidth, list.offerHeight, buttonIndex,
                    (b) -> {
                        if (b instanceof CustomTradeOfferButton customBtn) {
                            int selectedTrade = customBtn.getIndex() + this.scrollOff;
                            if (selectedTrade >= 0 && selectedTrade < this.menu.getOffers().size()) {
                                this.postButtonClick(selectedTrade);
                            }
                        }
                    });

            this.customTradeButtons.add(this.addRenderableWidget(button));
            startY += list.rowStep;
        }

        TradeLayout.ButtonCfg all = this.layout.tradeAll;
        if (all.visible) {
            this.tradeAllButton = this.addRenderableWidget(new CustomTradeAllArrowButton(
                    i + all.x, j + all.y, all.width, all.height,
                    b -> this.tradeAll()
            ));
        } else {
            this.tradeAllButton = null;
        }
    }

    private void tradeAll() {
        if (this.tradeAllButton == null) return;
        MerchantOffers offers = this.menu.getOffers();
        if (offers.isEmpty() || this.shopItem < 0 || this.shopItem >= offers.size()) return;
        PacketDistributor.sendToServer(new TradeAllPayload(this.shopItem));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        TradeLayout.LabelsCfg labels = this.layout.labels;
        if (labels.titleVisible) {
            guiGraphics.drawString(this.font, this.title, labels.titleX, labels.titleY, 0x404040, false);
        }
        if (labels.inventoryVisible) {
            guiGraphics.drawString(this.font, this.playerInventoryTitle, labels.inventoryX, labels.inventoryY, 0x404040, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TRADE_GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight,
                this.layout.image.texWidth, this.layout.image.texHeight);

        RenderSystem.disableBlend();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        MerchantOffers offers = this.menu.getOffers();
        if (!offers.isEmpty()) {
            int i = (this.width - this.imageWidth) / 2;
            int j = (this.height - this.imageHeight) / 2;

            renderCustomScroller(guiGraphics, i, j, offers);

            TradeLayout.ListCfg list = this.layout.list;
            TradeLayout.RowCfg row = this.layout.row;
            int startY = j + list.y;

            for (int l = 0; l < this.customTradeButtons.size(); ++l) {
                int offerIndex = l + this.scrollOff;
                CustomTradeOfferButton button = this.customTradeButtons.get(l);

                if (button != null) {
                    if (offerIndex < offers.size()) {
                        button.visible = true;
                        MerchantOffer offer = offers.get(offerIndex);

                        ItemStack costA = offer.getCostA();
                        ItemStack costB = offer.getCostB();
                        ItemStack result = offer.getResult();

                        int itemY = startY + (int) ((list.offerHeight - (16.0F * row.itemScale)) / 2.0F);
                        int buttonX = i + list.x;

                        renderScaledItem(guiGraphics, costA, buttonX + row.in1, itemY, row.scaleFor(0));

                        if (!costB.isEmpty()) {
                            renderScaledItem(guiGraphics, costB, buttonX + row.in2, itemY, row.scaleFor(1));
                        }

                        int arrowY = startY + (list.offerHeight - row.arrowHeight) / 2;

                        guiGraphics.blit(TRADE_ARROW, buttonX + row.arrow, arrowY, 0, 0,
                                row.arrowWidth, row.arrowHeight, row.arrowWidth, row.arrowHeight);

                        renderScaledItem(guiGraphics, result, buttonX + row.out, itemY, row.scaleFor(2));
                    } else {
                        button.visible = false;
                    }
                }
                startY += list.rowStep;
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
        TradeLayout.ScrollerCfg scroller = this.layout.scroller;
        int maxScroll = merchantOffers.size() - this.layout.list.safeRows();
        int scrollerX = posX + scroller.x;
        int scrollerY = posY + scroller.y;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (maxScroll > 0) {
            int trackLength = scroller.trackHeight - scroller.height;
            int offsetY = (int) ((float) this.scrollOff / (float) maxScroll * trackLength);

            guiGraphics.blitSprite(SCROLLER, scrollerX, scrollerY + offsetY, scroller.width, scroller.height);
        } else {
            guiGraphics.blitSprite(SCROLLER_DISABLED, scrollerX, scrollerY, scroller.width, scroller.height);
        }

        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = this.menu.getOffers().size() - this.layout.list.safeRows();
        if (maxScroll > 0) {
            this.scrollOff = Mth.clamp((int) ((double) this.scrollOff - scrollY), 0, maxScroll);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.isDragging = false;
        TradeLayout.ScrollerCfg scroller = this.layout.scroller;
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        if (this.menu.getOffers().size() > this.layout.list.safeRows()
                && mouseX > (double) (i + scroller.x) && mouseX < (double) (i + scroller.x + scroller.width)
                && mouseY > (double) (j + scroller.y) && mouseY <= (double) (j + scroller.y + scroller.trackHeight)) {
            this.isDragging = true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int maxScroll = this.menu.getOffers().size() - this.layout.list.safeRows();
        if (this.isDragging && maxScroll > 0) {
            TradeLayout.ScrollerCfg scroller = this.layout.scroller;
            int top = (this.height - this.imageHeight) / 2 + scroller.y;
            int bottom = top + scroller.trackHeight;
            float f = ((float) mouseY - (float) top - (scroller.height / 2.0F))
                    / ((float) (bottom - top) - scroller.height);
            f = f * (float) maxScroll + 0.5F;
            this.scrollOff = Mth.clamp((int) f, 0, maxScroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    class CustomTradeAllArrowButton extends Button {
        public CustomTradeAllArrowButton(int x, int y, int width, int height, Button.OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();

            guiGraphics.blitSprite(TRADE_ALL_SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());

            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    class CustomTradeOfferButton extends Button {

        private final int index;

        public CustomTradeOfferButton(int x, int y, int width, int height, int index, Button.OnPress onPress) {
            super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
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
            guiGraphics.blitSprite(MERCH_SPRITES_BUTTON.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            int i = this.getFGColor();
            this.renderString(guiGraphics, minecraft.font, i | Mth.ceil(this.alpha * 255.0F) << 24);
        }

        public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            MerchantOffers offers = NPCTradeScreen.this.menu.getOffers();
            int offerIndex = this.index + NPCTradeScreen.this.scrollOff;

            if (this.isHovered && offerIndex < offers.size()) {
                MerchantOffer offer = offers.get(offerIndex);
                TradeLayout.RowCfg row = NPCTradeScreen.this.layout.row;

                if (mouseX < this.getX() + row.in2) {
                    guiGraphics.renderTooltip(NPCTradeScreen.this.font, offer.getCostA(), mouseX, mouseY);
                } else if (mouseX < this.getX() + row.arrow) {
                    ItemStack costB = offer.getCostB();
                    if (!costB.isEmpty()) {
                        guiGraphics.renderTooltip(NPCTradeScreen.this.font, costB, mouseX, mouseY);
                    }
                } else {
                    guiGraphics.renderTooltip(NPCTradeScreen.this.font, offer.getResult(), mouseX, mouseY);
                }
            }
        }
    }
}
