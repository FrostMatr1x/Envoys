package com.frost.envoys.npc.merchant;

import com.frost.envoys.TestSupport;
import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.network.NetworkTestSupport;
import com.frost.envoys.npc.NPCTrade;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.platform.TestAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NPCMerchantTest {

    @TempDir
    Path tempDir;

    private final UUID npcId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private final UUID playerId = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private TestAttachments attachments;
    private BaseNPC owner;
    private ServerLevel level;
    private Player player;

    @BeforeAll
    static void bootstrap() {
        NetworkTestSupport.bootStrap();
    }

    @BeforeEach
    void setUp() {
        attachments = new TestAttachments();
        TestSupport.installEnvironment(tempDir, attachments);
        TestSupport.resetTradeCounterState();

        owner = mock(BaseNPC.class);
        level = mock(ServerLevel.class);
        player = mock(Player.class);
        when(owner.getUUID()).thenReturn(npcId);
        when(owner.level()).thenReturn(level);
        when(level.isClientSide()).thenReturn(false);
        when(player.getUUID()).thenReturn(playerId);
    }

    @AfterEach
    void tearDown() {
        TestSupport.resetTradeCounterState();
        TestSupport.clearEnvironment();
    }

    @Test
    void buildsOffersForValidTrades() {
        NPCTrade first = trade(stone(5), dirt(2), emerald(1));
        NPCTrade second = trade(stone(3), null, emerald(2));

        NPCMerchant merchant = merchant(action(null, first, second));

        MerchantOffers offers = merchant.getOffers();
        assertEquals(2, offers.size());
        assertEquals(Items.STONE, offers.get(0).getCostA().getItem());
        assertEquals(5, offers.get(0).getCostA().getCount());
        assertEquals(Items.DIRT, offers.get(0).getCostB().getItem());
        assertEquals(2, offers.get(0).getCostB().getCount());
        assertEquals(Items.STONE, offers.get(1).getCostA().getItem());
        assertEquals(3, offers.get(1).getCostA().getCount());
        assertEquals(9999, offers.get(0).getMaxUses());
        assertEquals(9999, offers.get(1).getMaxUses());
    }

    @Test
    void skipsInvalidTrades() {
        NPCTrade valid = trade(stone(2), null, emerald(1));
        NPCTrade emptyInput = trade(ItemStack.EMPTY, null, emerald(1));
        NPCTrade nullInput = trade(null, null, emerald(1));
        NPCTrade emptyOutput = trade(stone(2), null, ItemStack.EMPTY);
        NPCTrade nullOutput = trade(stone(2), null, null);

        NPCMerchant merchant = merchant(action(null, null, emptyInput, nullInput, emptyOutput, nullOutput, valid));

        MerchantOffers offers = merchant.getOffers();
        assertEquals(1, offers.size());
        assertEquals(Items.STONE, offers.get(0).getCostA().getItem());
        assertEquals(2, offers.get(0).getCostA().getCount());
    }

    @Test
    void respectsBaseSlotsAndUnlockedSlots() {
        ActionTrade action = action(1, trade(stone(2), null, emerald(1)), trade(stone(3), null, emerald(1)));

        assertEquals(1, merchant(action).getOffers().size());

        attachments.getMerchantSlots(player).addUnlocked(npcId.toString(), 1);
        assertEquals(2, merchant(action).getOffers().size());
    }

    @Test
    void nullOrNegativeBaseSlotsMeanNoLimit() {
        ActionTrade unlimited = action(null, trade(stone(2), null, emerald(1)), trade(stone(3), null, emerald(1)));
        ActionTrade negative = action(-1, trade(stone(2), null, emerald(1)), trade(stone(3), null, emerald(1)));

        assertEquals(2, merchant(unlimited).getOffers().size());
        assertEquals(2, merchant(negative).getOffers().size());
    }

    @Test
    void appliesPriceIncreaseForCompletedTrades() {
        NPCTrade trade = trade(stone(5), null, emerald(1));
        trade.priceMultiplier = 1.5f;
        ActionTrade action = action(null, trade);
        TradeCounterStore.onTradeCompleted(npcId, playerId, 0, trade, 4);

        MerchantOffer offer = merchant(action).getOffers().get(0);
        assertEquals(7, offer.getCostA().getCount());

        MerchantOffer rebuilt = merchant(action).getOffers().get(0);
        assertEquals(7, rebuilt.getCostA().getCount());
    }

    @Test
    void overflowGoesToSecondSlotWhenEmpty() {
        NPCTrade trade = trade(stone(64), null, emerald(1));
        trade.priceMultiplier = 2.0f;
        TradeCounterStore.onTradeCompleted(npcId, playerId, 0, trade, 64);

        MerchantOffer offer = merchant(action(null, trade)).getOffers().get(0);

        assertEquals(64, offer.getCostA().getCount());
        assertEquals(Items.STONE, offer.getCostB().getItem());
        assertEquals(64, offer.getCostB().getCount());
        assertEquals(Items.EMERALD, offer.getResult().getItem());
    }

    @Test
    void overflowIsClampedWhenSecondSlotHoldsSameItem() {
        NPCTrade trade = trade(stone(64), stone(32), emerald(1));
        trade.priceMultiplier = 2.0f;
        TradeCounterStore.onTradeCompleted(npcId, playerId, 0, trade, 64);

        MerchantOffer offer = assertDoesNotThrow(() -> merchant(action(null, trade)).getOffers().get(0));

        assertEquals(64, offer.getCostA().getCount());
        assertEquals(Items.STONE, offer.getCostB().getItem());
        assertEquals(64, offer.getCostB().getCount());
    }

    @Test
    void overflowIsIgnoredWhenSecondSlotHoldsDifferentItem() {
        NPCTrade trade = trade(stone(64), dirt(10), emerald(1));
        trade.priceMultiplier = 2.0f;
        TradeCounterStore.onTradeCompleted(npcId, playerId, 0, trade, 64);

        MerchantOffer offer = merchant(action(null, trade)).getOffers().get(0);

        assertEquals(64, offer.getCostA().getCount());
        assertEquals(Items.DIRT, offer.getCostB().getItem());
        assertEquals(10, offer.getCostB().getCount());
    }

    @Test
    void outOfStockWhenRemainingUsesAreZero() {
        NPCTrade trade = trade(stone(2), null, emerald(1));
        trade.maxTrades = 1;
        TradeCounterStore.onTradeCompleted(npcId, playerId, 0, trade, 1);

        MerchantOffer offer = merchant(action(null, trade)).getOffers().get(0);

        assertTrue(offer.isOutOfStock());
    }

    @Test
    void remainingUsesControlOfferMaxUses() {
        NPCTrade unlimited = trade(stone(2), null, emerald(1));
        assertEquals(Integer.MAX_VALUE, TradeCounterStore.remainingUses(npcId, playerId, 0, unlimited));
        assertEquals(9999, merchant(action(null, unlimited)).getOffers().get(0).getMaxUses());

        NPCTrade limited = trade(stone(2), null, emerald(1));
        limited.maxTrades = 5;
        TradeCounterStore.onTradeCompleted(npcId, playerId, 0, limited, 2);
        assertEquals(3, TradeCounterStore.remainingUses(npcId, playerId, 0, limited));
        assertEquals(3, merchant(action(null, limited)).getOffers().get(0).getMaxUses());

        TradeCounterStore.onTradeCompleted(npcId, playerId, 0, limited, 3);
        assertEquals(0, TradeCounterStore.remainingUses(npcId, playerId, 0, limited));
    }

    @Test
    void recordTradeUpdatesCounterAndRebuildsOffers() {
        NPCTrade trade = trade(stone(2), null, emerald(1));
        trade.maxTrades = 5;
        NPCMerchant merchant = merchant(action(null, trade));
        merchant.setTradingPlayer(player);
        MerchantOffer offer = merchant.getOffers().get(0);

        merchant.recordTrade(offer, 1);

        assertEquals(1, TradeCounterStore.getCompletedTrades(npcId, playerId, 0, trade));
        MerchantOffers rebuilt = merchant.getOffers();
        assertNotNull(rebuilt);
        assertEquals(1, rebuilt.size());
        assertEquals(4, rebuilt.get(0).getMaxUses());
    }

    @Test
    void recordTradeIgnoresClientSideNullPlayerAndNonPositiveTimes() {
        NPCTrade trade = trade(stone(2), null, emerald(1));
        trade.maxTrades = 5;
        NPCMerchant merchant = merchant(action(null, trade));
        MerchantOffer offer = merchant.getOffers().get(0);

        when(level.isClientSide()).thenReturn(true);
        merchant.setTradingPlayer(player);
        merchant.recordTrade(offer, 1);
        assertEquals(0, TradeCounterStore.getCompletedTrades(npcId, playerId, 0, trade));

        when(level.isClientSide()).thenReturn(false);
        merchant.setTradingPlayer(null);
        merchant.recordTrade(offer, 1);
        assertEquals(0, TradeCounterStore.getCompletedTrades(npcId, playerId, 0, trade));

        merchant.setTradingPlayer(player);
        merchant.recordTrade(offer, 0);
        merchant.recordTrade(offer, -2);
        assertEquals(0, TradeCounterStore.getCompletedTrades(npcId, playerId, 0, trade));
    }

    @Test
    void getOffersIsNeverNullAndOverrideHandlesNull() {
        NPCMerchant merchant = new NPCMerchant(owner, null, player);

        assertNotNull(merchant.getOffers());
        assertTrue(merchant.getOffers().isEmpty());

        merchant.overrideOffers(null);

        assertNotNull(merchant.getOffers());
        assertTrue(merchant.getOffers().isEmpty());
    }

    private NPCMerchant merchant(ActionTrade action) {
        return new NPCMerchant(owner, action, player);
    }

    private static ActionTrade action(Integer baseSlots, NPCTrade... trades) {
        ActionTrade action = new ActionTrade("trade-1");
        action.baseSlots = baseSlots;
        action.trades.addAll(Arrays.asList(trades));
        return action;
    }

    private static NPCTrade trade(ItemStack input1, ItemStack input2, ItemStack output) {
        return new NPCTrade(input1, input2, output);
    }

    private static ItemStack stone(int count) {
        return new ItemStack(Items.STONE, count);
    }

    private static ItemStack dirt(int count) {
        return new ItemStack(Items.DIRT, count);
    }

    private static ItemStack emerald(int count) {
        return new ItemStack(Items.EMERALD, count);
    }
}
