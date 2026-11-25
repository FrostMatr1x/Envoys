package com.frost.envoys.quest;

import com.frost.envoys.TestSupport;
import com.frost.envoys.network.NetworkTestSupport;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestInventoryUtilTest {

    private Player player;
    private Inventory inventory;
    private InventoryMenu menu;

    @BeforeAll
    static void bootstrap() {
        NetworkTestSupport.bootStrap();
    }

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        inventory = new Inventory(player);
        menu = mock(InventoryMenu.class);
        when(player.getInventory()).thenReturn(inventory);
        TestSupport.setInstanceField(player, Player.class, "inventoryMenu", menu);
    }

    @Test
    void resolveItemReturnsNullForNullOrBlankOrInvalidId() {
        assertNull(QuestInventoryUtil.resolveItem(null));
        assertNull(QuestInventoryUtil.resolveItem(""));
        assertNull(QuestInventoryUtil.resolveItem("   "));
        assertNull(QuestInventoryUtil.resolveItem("bad id"));
    }

    @Test
    void resolveItemResolvesNamespacedAndPlainIds() {
        assertSame(Items.STONE, QuestInventoryUtil.resolveItem("minecraft:stone"));
        assertSame(Items.STONE, QuestInventoryUtil.resolveItem("stone"));
    }

    @Test
    void resolveItemFallsBackToRegistryDefaultForUnknownId() {
        assertSame(Items.AIR, QuestInventoryUtil.resolveItem("minecraft:definitely_missing"));
    }

    @Test
    void countReturnsZeroForNullPlayerOrItem() {
        assertEquals(0, QuestInventoryUtil.count(null, Items.STONE));
        assertEquals(0, QuestInventoryUtil.count(player, (net.minecraft.world.item.Item) null));
    }

    @Test
    void countReturnsZeroForEmptyInventory() {
        assertEquals(0, QuestInventoryUtil.count(player, Items.STONE));
    }

    @Test
    void countSumsMainAndOffhandAndIgnoresOtherItems() {
        putMain(0, Items.STONE, 3);
        putMain(1, Items.DIRT, 5);
        putMain(2, Items.STONE, 2);
        putOffhand(0, Items.STONE, 4);

        assertEquals(9, QuestInventoryUtil.count(player, Items.STONE));
        assertEquals(5, QuestInventoryUtil.count(player, Items.DIRT));
    }

    @Test
    void countByStringIdUsesResolvedItem() {
        putMain(0, Items.STONE, 6);

        assertEquals(6, QuestInventoryUtil.count(player, "minecraft:stone"));
        assertEquals(6, QuestInventoryUtil.count(player, "stone"));
        assertEquals(0, QuestInventoryUtil.count(player, "minecraft:definitely_missing"));
    }

    @Test
    void hasAtLeastRejectsInvalidInputs() {
        putMain(0, Items.STONE, 5);

        assertFalse(QuestInventoryUtil.hasAtLeast(null, Items.STONE, 1));
        assertFalse(QuestInventoryUtil.hasAtLeast(player, null, 1));
        assertFalse(QuestInventoryUtil.hasAtLeast(player, Items.STONE, 0));
        assertFalse(QuestInventoryUtil.hasAtLeast(player, Items.STONE, -3));
    }

    @Test
    void hasAtLeastMatchesExactAmountAndReportsShortage() {
        putMain(0, Items.STONE, 5);

        assertTrue(QuestInventoryUtil.hasAtLeast(player, Items.STONE, 5));
        assertFalse(QuestInventoryUtil.hasAtLeast(player, Items.STONE, 6));
    }

    @Test
    void hasAtLeastCountsOffhandTowardsRequirement() {
        putMain(0, Items.STONE, 2);
        putOffhand(0, Items.STONE, 2);

        assertTrue(QuestInventoryUtil.hasAtLeast(player, Items.STONE, 4));
    }

    @Test
    void consumeRejectsInvalidInputs() {
        putMain(0, Items.STONE, 5);

        assertEquals(0, QuestInventoryUtil.consume(null, Items.STONE, 1));
        assertEquals(0, QuestInventoryUtil.consume(player, (net.minecraft.world.item.Item) null, 1));
        assertEquals(0, QuestInventoryUtil.consume(player, Items.STONE, 0));
        assertEquals(0, QuestInventoryUtil.consume(player, Items.STONE, -1));
        assertEquals(5, inventory.items.get(0).getCount());
        verify(menu, never()).broadcastChanges();
    }

    @Test
    void consumeTakesOnlyFromMainWhenPossible() {
        putMain(0, Items.STONE, 5);
        putOffhand(0, Items.STONE, 3);

        assertEquals(3, QuestInventoryUtil.consume(player, Items.STONE, 3));

        assertEquals(2, inventory.items.get(0).getCount());
        assertEquals(3, inventory.offhand.get(0).getCount());
        verify(menu, times(1)).broadcastChanges();
    }

    @Test
    void consumeSpillsIntoOffhandAfterMainIsEmpty() {
        putMain(0, Items.STONE, 2);
        putOffhand(0, Items.STONE, 5);

        assertEquals(4, QuestInventoryUtil.consume(player, Items.STONE, 4));

        assertTrue(inventory.items.get(0).isEmpty());
        assertEquals(3, inventory.offhand.get(0).getCount());
        verify(menu, times(1)).broadcastChanges();
    }

    @Test
    void consumeReturnsActuallyTakenAmountWhenInventoryIsShort() {
        putMain(0, Items.STONE, 2);
        putOffhand(0, Items.STONE, 1);

        assertEquals(3, QuestInventoryUtil.consume(player, Items.STONE, 10));

        assertTrue(inventory.items.get(0).isEmpty());
        assertTrue(inventory.offhand.get(0).isEmpty());
        verify(menu, times(1)).broadcastChanges();
    }

    @Test
    void consumeSkipsOtherItemsAndEmptyStacks() {
        putMain(0, Items.DIRT, 5);
        putMain(1, Items.STONE, 2);

        assertEquals(2, QuestInventoryUtil.consume(player, Items.STONE, 2));

        assertEquals(5, inventory.items.get(0).getCount());
        assertTrue(inventory.items.get(1).isEmpty());
    }

    @Test
    void consumeDoesNotBroadcastWhenNothingWasTaken() {
        putMain(0, Items.DIRT, 5);

        assertEquals(0, QuestInventoryUtil.consume(player, Items.STONE, 2));

        verify(menu, never()).broadcastChanges();
    }

    @Test
    void consumeByStringIdUsesResolvedItem() {
        putMain(0, Items.STONE, 4);

        assertEquals(2, QuestInventoryUtil.consume(player, "minecraft:stone", 2));
        assertEquals(2, inventory.items.get(0).getCount());

        assertEquals(0, QuestInventoryUtil.consume(player, "minecraft:definitely_missing", 2));
    }

    private void putMain(int slot, net.minecraft.world.item.Item item, int count) {
        inventory.items.set(slot, new ItemStack(item, count));
    }

    private void putOffhand(int slot, net.minecraft.world.item.Item item, int count) {
        inventory.offhand.set(slot, new ItemStack(item, count));
    }
}
