package com.frost.envoys.gui.menu;

import com.frost.envoys.init.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;

public class NPCMerchantMenu extends MerchantMenu {

    private final Merchant merchant;

    public NPCMerchantMenu(int containerId, Inventory playerInventory, Merchant merchant) {
        super(containerId, playerInventory, merchant);
        this.merchant = merchant;
    }

    public NPCMerchantMenu(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory);
        this.merchant = null;
    }

    public Merchant getMerchant() {
        return this.merchant;
    }

    @Override
    public MenuType<?> getType() {
        return ModMenus.NPC_MERCHANT_MENU.get();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index == 2) {
                ItemStack payment0 = this.slots.get(0).getItem().copy();
                ItemStack payment1 = this.slots.get(1).getItem().copy();

                while (true) {
                    this.refillInputSlot(0, payment0);
                    this.refillInputSlot(1, payment1);

                    if (!slot.hasItem()) {
                        break;
                    }

                    ItemStack resultStack = slot.getItem();
                    ItemStack copyStack = resultStack.copy();

                    if (!this.moveItemStackTo(resultStack, 3, 39, true)) {
                        return ItemStack.EMPTY;
                    }

                    slot.onQuickCraft(resultStack, copyStack);
                    slot.onTake(player, resultStack);
                }

                return itemstack;
            } else if (index != 0 && index != 1) {
                if (index >= 3 && index < 30) {
                    if (!this.moveItemStackTo(itemstack1, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 30 && index < 39 && !this.moveItemStackTo(itemstack1, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }
        return itemstack;
    }

    private void refillInputSlot(int inputSlotIndex, ItemStack sampleItem) {
        if (sampleItem.isEmpty()) return;

        Slot inputSlot = this.slots.get(inputSlotIndex);
        ItemStack currentInput = inputSlot.getItem();

        int targetCount = sampleItem.getCount();
        int currentCount = currentInput.isEmpty() ? 0 : currentInput.getCount();
        int needed = targetCount - currentCount;

        if (needed <= 0) return;

        for (int i = 3; i < 39; i++) {
            Slot invSlot = this.slots.get(i);
            if (invSlot.hasItem()) {
                ItemStack invStack = invSlot.getItem();

                if (isSameItem(invStack, sampleItem)) {
                    int toMove = Math.min(needed, invStack.getCount());

                    if (currentInput.isEmpty()) {
                        ItemStack newStack = invStack.copy();
                        newStack.setCount(toMove);
                        inputSlot.set(newStack);
                        currentInput = inputSlot.getItem();
                    } else {
                        currentInput.grow(toMove);
                        inputSlot.setChanged();
                    }

                    invStack.shrink(toMove);
                    if (invStack.isEmpty()) {
                        invSlot.set(ItemStack.EMPTY);
                    } else {
                        invSlot.setChanged();
                    }

                    needed -= toMove;
                    if (needed <= 0) break;
                }
            }
        }
    }

    private boolean isSameItem(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) return false;

        return ItemStack.isSameItemSameComponents(stack1, stack2); 
    }
}