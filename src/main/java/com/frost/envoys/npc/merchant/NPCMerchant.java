package com.frost.envoys.npc.merchant;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import javax.annotation.Nullable;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.gui.bridges.TradeGuiBridge;
import com.frost.envoys.gui.menu.NPCMerchantMenu;
import com.frost.envoys.npc.NPCTrade;
import com.frost.envoys.npc.entity.BaseNPC;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class NPCMerchant implements Merchant, TradeGuiBridge {
    private final BaseNPC owner;
    private ActionTrade currentAction;

    @Nullable
    private Player tradingPlayer;
    private MerchantOffers offers;

    public NPCMerchant(BaseNPC owner, ActionTrade action, Player player) {
        this.owner = owner;
        this.currentAction = action;
        this.offers = buildOffers(action, player);
    }

    public void openTradingScreen(Player player, Component displayName, int level, @Nullable Runnable onClose) {
        this.setTradingPlayer(player);
        if (this.offers == null) {
            this.offers = new MerchantOffers();
        }
        OptionalInt containerId = player.openMenu(new SimpleMenuProvider(
            (id, inventory, p) -> new NPCMerchantMenu(id, inventory, this) {
                @Override
                public void removed(Player p) {
                    super.removed(p);
                    NPCMerchant.this.setTradingPlayer(null);
                    if (!p.level().isClientSide() && onClose != null) {
                        onClose.run();
                    }
                }
            },
            displayName
        ));
        if (containerId.isPresent() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendMerchantOffers(
                containerId.getAsInt(),
                this.getOffers(),
                level,
                this.getVillagerXp(),
                this.showProgressBar(),
                false
            );
        } else {
            this.setTradingPlayer(null);
        }
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = new MerchantOffers();
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(@Nullable MerchantOffers offers) {
        this.offers = offers == null ? new MerchantOffers() : offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();

        this.recordTrade(offer, 1);

        if (this.tradingPlayer instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu instanceof NPCMerchantMenu menu) {
            serverPlayer.sendMerchantOffers(
                menu.containerId,
                this.getOffers(),
                0,
                this.getVillagerXp(),
                this.showProgressBar(),
                false
            );
        }

        this.playTradeSound();
    }

    public void updateOfferPrice(MerchantOffer offer) {
        if (this.tradingPlayer == null || this.currentAction == null) return;

        int sourceIndex = resolveOfferSourceIndex(offer);
        if (sourceIndex < 0 || sourceIndex >= this.currentAction.trades.size()) {
            return;
        }

        NPCTrade trade = this.currentAction.trades.get(sourceIndex);

        int completedTrades = TradeCounterStore.getCompletedTrades(
            this.owner.getUUID(), 
            this.tradingPlayer.getUUID(), 
            sourceIndex, 
            trade
        );

        float priceIncreasePerTrade = Math.max(0f, trade.priceMultiplier - 1.0f);
        int extraCost = (int) (completedTrades * priceIncreasePerTrade);

        offer.setSpecialPriceDiff(extraCost);
    }

    public void recordTrade(MerchantOffer offer, int times) {
        if (this.owner.level().isClientSide()) {
            return;
        }
        Player player = this.tradingPlayer;
        if (player == null || times <= 0) {
            return;
        }
        int sourceIndex = resolveOfferSourceIndex(offer);
        if (sourceIndex < 0 || this.currentAction == null || sourceIndex >= this.currentAction.trades.size()) {
            return;
        }
        NPCTrade trade = this.currentAction.trades.get(sourceIndex);
        TradeCounterStore.onTradeCompleted(this.owner.getUUID(), player.getUUID(), sourceIndex, trade, times);

        updateOfferPrice(offer);
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    private void playTradeSound() {
        this.owner.playSound(getNotifyTradeSound(), this.owner.getSoundVolume(), this.owner.getVoicePitch());
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.NOTE_BLOCK_BELL.value();
    }

    @Override
    public boolean isClientSide() {
        return this.owner.level().isClientSide();
    }

    @Override
    public void open(Player player, UUID npcUuid, ActionTrade action, Runnable onClose) {
        this.currentAction = action;
        this.offers = buildOffers(action, player);
        openTradingScreen(player, Component.literal("Торговля"), 0, onClose);
    }

    private static boolean isValidTrade(NPCTrade trade) {
        return trade != null
            && trade.input1 != null && !trade.input1.isEmpty()
            && trade.output != null && !trade.output.isEmpty();
    }

    private MerchantOffers buildOffers(ActionTrade action, Player player) {
        MerchantOffers built = new MerchantOffers();
        if (action == null || action.trades == null || action.trades.isEmpty()) {
            return built;
        }

        UUID npcUuid = this.owner.getUUID();
        UUID playerUuid = player.getUUID();
        List<NPCTrade> trades = action.trades;

        for (int i = 0; i < trades.size(); i++) {
            NPCTrade trade = trades.get(i);
            if (!isValidTrade(trade)) {
                continue;
            }

            ItemStack in1 = trade.input1;
            ItemStack in2 = trade.input2;
            ItemStack out = trade.output;

            ItemStack scaledIn1 = trade.getScaledInput(in1);
            DataComponentMap customComponents1 = NPCTrade.getCustomComponents(in1);
            DataComponentPredicate predicate1 = customComponents1.isEmpty()
                ? DataComponentPredicate.EMPTY
                : DataComponentPredicate.allOf(customComponents1);

            ItemCost costA = new ItemCost(
                in1.getItemHolder(),
                scaledIn1.getCount(),
                predicate1,
                scaledIn1
            );

            Optional<ItemCost> costB = Optional.empty();
            if (in2 != null && !in2.isEmpty()) {
                ItemStack scaledIn2 = trade.getScaledInput(in2);
                DataComponentMap customComponents2 = NPCTrade.getCustomComponents(in2);
                DataComponentPredicate predicate2 = customComponents2.isEmpty()
                    ? DataComponentPredicate.EMPTY
                    : DataComponentPredicate.allOf(customComponents2);

                costB = Optional.of(new ItemCost(
                    in2.getItemHolder(),
                    scaledIn2.getCount(),
                    predicate2,
                    scaledIn2
                ));
            }

            ItemStack result = out.copy();

            int maxUses = 9999;
            boolean outOfStock = false;
            if (trade.maxTrades > 0) {
                int remaining = TradeCounterStore.remainingUses(npcUuid, playerUuid, i, trade);
                if (remaining <= 0) {
                    outOfStock = true;
                } else {
                    maxUses = remaining;
                }
            }

            int completedTrades = TradeCounterStore.getCompletedTrades(npcUuid, playerUuid, i, trade);
            float priceIncreasePerTrade = Math.max(0f, trade.priceMultiplier - 1.0f);
            int initialExtraCost = (int) (completedTrades * priceIncreasePerTrade);

            MerchantOffer offer = new MerchantOffer(
                costA,
                costB,
                result,
                maxUses,
                0,
                trade.priceMultiplier
            );

            offer.setSpecialPriceDiff(initialExtraCost);
            if (outOfStock) {
                offer.setToOutOfStock();
            }
            built.add(offer);
        }
        return built;
    }

    private int resolveOfferSourceIndex(MerchantOffer offer) {
        if (this.currentAction == null || this.offers == null) {
            return -1;
        }
        int position = this.offers.indexOf(offer);
        if (position < 0) {
            return -1;
        }
        int displayPosition = 0;
        List<NPCTrade> trades = this.currentAction.trades;
        for (int i = 0; i < trades.size(); i++) {
            if (!isValidTrade(trades.get(i))) {
                continue;
            }
            if (displayPosition == position) {
                return i;
            }
            displayPosition++;
        }
        return -1;
    }
}