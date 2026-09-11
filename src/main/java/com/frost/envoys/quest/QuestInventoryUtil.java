package com.frost.envoys.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class QuestInventoryUtil {

    private QuestInventoryUtil() {
    }

    public static Item resolveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        ResourceLocation key = ResourceLocation.tryParse(itemId);
        if (key == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.get(key);
    }

    public static boolean hasAtLeast(Player player, Item item, int amount) {
        if (player == null || item == null || amount <= 0) {
            return false;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
                if (total >= amount) {
                    return true;
                }
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
                if (total >= amount) {
                    return true;
                }
            }
        }
        return total >= amount;
    }

    public static int count(Player player, String itemId) {
        return count(player, resolveItem(itemId));
    }

    public static int count(Player player, Item item) {
        if (player == null || item == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static int consume(Player player, String itemId, int amount) {
        return consume(player, resolveItem(itemId), amount);
    }

    public static int consume(Player player, Item item, int amount) {
        if (player == null || item == null || amount <= 0) {
            return 0;
        }

        int remaining = amount;
        remaining = consumeFrom(player.getInventory().items, item, remaining);
        remaining = consumeFrom(player.getInventory().offhand, item, remaining);

        int consumed = amount - remaining;
        if (consumed > 0) {
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        return consumed;
    }

    private static int consumeFrom(Iterable<ItemStack> stacks, Item item, int remaining) {
        if (remaining <= 0) {
            return remaining;
        }
        for (ItemStack stack : stacks) {
            if (remaining <= 0) {
                break;
            }
            if (stack.isEmpty() || stack.getItem() != item) {
                continue;
            }
            int taken = Math.min(stack.getCount(), remaining);
            stack.shrink(taken);
            remaining -= taken;
        }
        return remaining;
    }
}
