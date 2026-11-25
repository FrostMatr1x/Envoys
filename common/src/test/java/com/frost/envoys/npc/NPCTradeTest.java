package com.frost.envoys.npc;

import com.frost.envoys.network.NetworkTestSupport;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NPCTradeTest {

    @BeforeAll
    static void bootstrap() {
        NetworkTestSupport.bootStrap();
    }

    @Test
    void emptyRequirementMatchesOnlyEmptyPlayerStack() {
        assertTrue(NPCTrade.isStackMatching(null, null));
        assertTrue(NPCTrade.isStackMatching(ItemStack.EMPTY, null));
        assertTrue(NPCTrade.isStackMatching(null, ItemStack.EMPTY));
        assertTrue(NPCTrade.isStackMatching(ItemStack.EMPTY, ItemStack.EMPTY));
        assertFalse(NPCTrade.isStackMatching(new ItemStack(Items.STONE), null));
        assertFalse(NPCTrade.isStackMatching(new ItemStack(Items.STONE), ItemStack.EMPTY));
    }

    @Test
    void nonEmptyRequirementDoesNotMatchEmptyPlayerStack() {
        assertFalse(NPCTrade.isStackMatching(null, new ItemStack(Items.STONE, 1)));
        assertFalse(NPCTrade.isStackMatching(ItemStack.EMPTY, new ItemStack(Items.STONE, 1)));
    }

    @Test
    void countAndItemMustMatchRequirement() {
        ItemStack required = new ItemStack(Items.STONE, 5);

        assertFalse(NPCTrade.isStackMatching(new ItemStack(Items.STONE, 4), required));
        assertTrue(NPCTrade.isStackMatching(new ItemStack(Items.STONE, 5), required));
        assertTrue(NPCTrade.isStackMatching(new ItemStack(Items.STONE, 9), required));
        assertFalse(NPCTrade.isStackMatching(new ItemStack(Items.DIRT, 5), required));
    }

    @Test
    void componentPredicateRequiresMatchingDataComponents() {
        ItemStack required = new ItemStack(Items.STONE, 1);
        required.set(DataComponents.DAMAGE, 10);

        assertFalse(NPCTrade.isStackMatching(new ItemStack(Items.STONE, 1), required));

        ItemStack damaged = new ItemStack(Items.STONE, 1);
        damaged.set(DataComponents.DAMAGE, 10);
        assertTrue(NPCTrade.isStackMatching(damaged, required));
    }

    @Test
    void plainItemWithoutCustomComponentsMatchesAnyDamageValue() {
        ItemStack required = new ItemStack(Items.STONE, 1);
        ItemStack damaged = new ItemStack(Items.STONE, 1);
        damaged.set(DataComponents.DAMAGE, 3);

        assertTrue(NPCTrade.isStackMatching(damaged, required));
    }

    @Test
    void getCustomComponentsReturnsEmptyForNullOrEmptyStack() {
        assertTrue(NPCTrade.getCustomComponents(null).isEmpty());
        assertTrue(NPCTrade.getCustomComponents(ItemStack.EMPTY).isEmpty());
    }

    @Test
    void getCustomComponentsReturnsEmptyForVanillaStack() {
        DataComponentMap components = NPCTrade.getCustomComponents(new ItemStack(Items.STONE));

        assertTrue(components.isEmpty());
    }

    @Test
    void getCustomComponentsDetectsChangedComponents() {
        ItemStack stack = new ItemStack(Items.STONE);
        stack.set(DataComponents.DAMAGE, 7);

        DataComponentMap components = NPCTrade.getCustomComponents(stack);

        assertFalse(components.isEmpty());
        assertEquals(7, components.get(DataComponents.DAMAGE));
    }

    @Test
    void matchesAcceptsEitherInputOrder() {
        NPCTrade trade = new NPCTrade(
                new ItemStack(Items.STONE, 5),
                new ItemStack(Items.DIRT, 2),
                new ItemStack(Items.EMERALD, 1));

        assertTrue(trade.matches(new ItemStack(Items.STONE, 5), new ItemStack(Items.DIRT, 2)));
        assertTrue(trade.matches(new ItemStack(Items.DIRT, 2), new ItemStack(Items.STONE, 5)));
    }

    @Test
    void matchesRejectsWrongItemOrCount() {
        NPCTrade trade = new NPCTrade(
                new ItemStack(Items.STONE, 5),
                new ItemStack(Items.DIRT, 2),
                new ItemStack(Items.EMERALD, 1));

        assertFalse(trade.matches(new ItemStack(Items.STONE, 4), new ItemStack(Items.DIRT, 2)));
        assertFalse(trade.matches(new ItemStack(Items.STONE, 5), new ItemStack(Items.DIRT, 1)));
        assertFalse(trade.matches(new ItemStack(Items.GOLD_INGOT, 5), new ItemStack(Items.DIRT, 2)));
    }

    @Test
    void matchesTreatsEmptySecondInputAsNotRequired() {
        NPCTrade trade = new NPCTrade(
                new ItemStack(Items.STONE, 5),
                ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, 1));

        assertTrue(trade.matches(new ItemStack(Items.STONE, 5), ItemStack.EMPTY));
        assertTrue(trade.matches(new ItemStack(Items.STONE, 5), null));
        assertFalse(trade.matches(new ItemStack(Items.STONE, 5), new ItemStack(Items.DIRT, 1)));
    }

    @Test
    void getScaledInputReturnsEmptyForNullOrEmpty() {
        NPCTrade trade = new NPCTrade();

        assertEquals(ItemStack.EMPTY, trade.getScaledInput(null));
        assertEquals(ItemStack.EMPTY, trade.getScaledInput(ItemStack.EMPTY));
    }

    @Test
    void getScaledInputReturnsIndependentCopy() {
        NPCTrade trade = new NPCTrade();
        ItemStack input = new ItemStack(Items.STONE, 7);

        ItemStack scaled = trade.getScaledInput(input);

        assertEquals(7, scaled.getCount());
        assertEquals(Items.STONE, scaled.getItem());
        assertNotSame(input, scaled);

        scaled.setCount(1);
        assertEquals(7, input.getCount());
    }

    @Test
    void constructorSetsInputsOutputsAndDefaults() {
        ItemStack input1 = new ItemStack(Items.STONE, 4);
        ItemStack input2 = new ItemStack(Items.DIRT, 1);
        ItemStack output = new ItemStack(Items.EMERALD, 2);

        NPCTrade trade = new NPCTrade(input1, input2, output);

        assertEquals(input1, trade.input1);
        assertEquals(input2, trade.input2);
        assertEquals(output, trade.output);
        assertEquals(1.0f, trade.priceMultiplier);
        assertEquals(0, trade.demand);
        assertEquals(-1, trade.maxTrades);
        assertEquals(-1, trade.resetTime);
    }
}
