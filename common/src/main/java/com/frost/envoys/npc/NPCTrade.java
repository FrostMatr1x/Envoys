package com.frost.envoys.npc;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.ItemStack;

public class NPCTrade {
    public ItemStack input1;
    public ItemStack input2;
    public ItemStack output;
    public float priceMultiplier = 1.0f;
    public int demand = 0;
    public int maxTrades = -1;
    public int resetTime = -1;

    public NPCTrade() {}

    public NPCTrade(ItemStack input1, ItemStack input2, ItemStack output) {
        this.input1 = input1;
        this.input2 = input2;
        this.output = output;
    }

    public ItemStack getScaledInput(ItemStack input) {
        if (input == null || input.isEmpty()) return ItemStack.EMPTY;
        return input.copy();
    }

    public boolean matches(ItemStack a, ItemStack b) {
        return matchesOrdered(a, b) || matchesOrdered(b, a);
    }

    private boolean matchesOrdered(ItemStack a, ItemStack b) {
        return isStackMatching(a, input1) && isStackMatching(b, input2);
    }

    public static boolean isStackMatching(ItemStack playerStack, ItemStack requiredStack) {
        boolean reqEmpty = requiredStack == null || requiredStack.isEmpty();
        boolean playerEmpty = playerStack == null || playerStack.isEmpty();
        if (reqEmpty) {
            return playerEmpty;
        }
        if (playerEmpty) {
            return false;
        }
        if (playerStack.getCount() < requiredStack.getCount()) {
            return false;
        }
        if (!playerStack.is(requiredStack.getItem())) {
            return false;
        }
        DataComponentMap requiredComponents = getCustomComponents(requiredStack);
        if (!requiredComponents.isEmpty()) {
            return DataComponentPredicate.allOf(requiredComponents).test(playerStack);
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DataComponentMap getCustomComponents(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return DataComponentMap.EMPTY;
        }
        DataComponentPatch patch = stack.getComponentsPatch();
        DataComponentMap addedFromPatch = patch.split().added();
        if (!addedFromPatch.isEmpty()) {
            return addedFromPatch;
        }
        DataComponentMap prototype = stack.getItem().components();
        DataComponentMap current = stack.getComponents();
        DataComponentMap.Builder builder = DataComponentMap.builder();
        for (TypedDataComponent<?> component : current) {
            DataComponentType<?> type = component.type();
            Object value = component.value();
            Object defaultValue = prototype.get(type);
            if (defaultValue == null || !defaultValue.equals(value)) {
                builder.set((DataComponentType) type, value);
            }
        }
        return builder.build();
    }
}