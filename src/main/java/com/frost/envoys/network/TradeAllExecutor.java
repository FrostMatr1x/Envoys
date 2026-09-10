package com.frost.envoys.network;

import java.util.ArrayList;
import java.util.List;

import com.frost.envoys.gui.menu.NPCMerchantMenu;
import com.frost.envoys.npc.merchant.NPCMerchant;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

public final class TradeAllExecutor {

    private TradeAllExecutor() {
    }

    public static void execute(ServerPlayer player, NPCMerchantMenu menu, int shopItem) {
        MerchantOffers offers = menu.getOffers();
        if (offers.isEmpty() || shopItem < 0 || shopItem >= offers.size()) return;

        MerchantOffer offer = offers.get(shopItem);
        int remainingUses = offer.getMaxUses() - offer.getUses();
        if (remainingUses <= 0) return;

        ItemStack costA = offer.getCostA();
        ItemStack costB = offer.getCostB();
        ItemStack result = offer.getResult();

        boolean costAIsBackpack = costA.getItem() instanceof BackpackItem;
        boolean costBIsBackpack = costB.getItem() instanceof BackpackItem;
        boolean sameCost = isSameItem(costA, costB);

        int neededA = costA.getCount();
        int neededB = costB.getCount();

        Inventory inv = player.getInventory();
        List<ItemStack> stacks = new ArrayList<>(inv.items.size() + inv.offhand.size() + inv.armor.size());
        stacks.addAll(inv.items);
        stacks.addAll(inv.offhand);
        stacks.addAll(inv.armor);

        List<IItemHandler> backpacks = new ArrayList<>();

        int availA = 0;
        int availB = 0;

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            if (isSameItem(stack, costA) && (!(stack.getItem() instanceof BackpackItem) || costAIsBackpack)) {
                availA += stack.getCount();
            }
            if (isSameItem(stack, costB) && (!(stack.getItem() instanceof BackpackItem) || costBIsBackpack)) {
                availB += stack.getCount();
            }
            if (stack.getItem() instanceof BackpackItem) {
                IItemHandler handler = BackpackWrapper.fromStack(stack).getInventoryHandler();
                if (handler != null) backpacks.add(handler);
            }
        }

        for (IItemHandler handler : backpacks) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack inside = handler.getStackInSlot(slot);
                if (inside.isEmpty()) continue;
                if (isSameItem(inside, costA)) availA += inside.getCount();
                if (isSameItem(inside, costB)) availB += inside.getCount();
            }
        }

        int totalTrades;
        if (sameCost) {
            totalTrades = availA / (neededA + neededB);
        } else {
            int tradesA = costA.isEmpty() ? remainingUses : availA / neededA;
            int tradesB = costB.isEmpty() ? remainingUses : availB / neededB;
            totalTrades = Math.min(tradesA, tradesB);
        }
        totalTrades = Math.min(totalTrades, remainingUses);
        if (totalTrades <= 0) return;

        if (sameCost) {
            consume(stacks, backpacks, costA, (neededA + neededB) * totalTrades);
        } else {
            consume(stacks, backpacks, costA, neededA * totalTrades);
            consume(stacks, backpacks, costB, neededB * totalTrades);
        }

        giveResult(player, inv, backpacks, result, result.getCount() * totalTrades);

        for (int i = 0; i < totalTrades; i++) {
            offer.increaseUses();
        }
        if (menu.getMerchant() instanceof NPCMerchant npcMerchant) {
            npcMerchant.recordTrade(offer, totalTrades);
        }
        player.awardStat(Stats.TRADED_WITH_VILLAGER, totalTrades);

        menu.broadcastChanges();
        player.inventoryMenu.broadcastChanges(); // Синхронизируем инвентарь игрока
        player.sendMerchantOffers(menu.containerId, menu.getOffers(), menu.getTraderLevel(), menu.getTraderXp(), menu.showProgressBar(), menu.canRestock());
    }

    private static void consume(List<ItemStack> stacks, List<IItemHandler> backpacks, ItemStack required, int amount) {
        if (required.isEmpty() || amount <= 0) return;

        boolean targetIsBackpack = required.getItem() instanceof BackpackItem;

        for (ItemStack stack : stacks) {
            if (amount <= 0) break;
            if (!stack.isEmpty() && isSameItem(stack, required)
                    && (!(stack.getItem() instanceof BackpackItem) || targetIsBackpack)) {
                int toTake = Math.min(amount, stack.getCount());
                stack.shrink(toTake);
                amount -= toTake;
            }
        }

        for (IItemHandler handler : backpacks) {
            if (amount <= 0) break;
            for (int slot = 0; slot < handler.getSlots() && amount > 0; slot++) {
                ItemStack inside = handler.getStackInSlot(slot);
                if (!inside.isEmpty() && isSameItem(inside, required)) {
                    amount -= handler.extractItem(slot, amount, false).getCount();
                }
            }
        }
    }

    private static void giveResult(ServerPlayer player, Inventory inv, List<IItemHandler> backpacks, ItemStack resultTemplate, int totalCount) {
        if (resultTemplate.isEmpty() || totalCount <= 0) return;

        int maxStackSize = resultTemplate.getMaxStackSize();
        while (totalCount > 0) {
            int chunk = Math.min(totalCount, maxStackSize);
            ItemStack stack = resultTemplate.copyWithCount(chunk);

            for (IItemHandler handler : backpacks) {
                stack = ItemHandlerHelper.insertItemStacked(handler, stack, false);
                if (stack.isEmpty()) break;
            }

            if (!stack.isEmpty()) {
                inv.add(stack);
            }

            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }

            totalCount -= chunk;
        }
    }

    private static boolean isSameItem(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
    }
}