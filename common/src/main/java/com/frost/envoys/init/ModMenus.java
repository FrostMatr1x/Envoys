package com.frost.envoys.init;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.gui.menu.NPCMerchantMenu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final RegistryEntry<MenuType<NPCMerchantMenu>> NPC_MERCHANT_MENU =
            new RegistryEntry<>(ResourceLocation.fromNamespaceAndPath(EnvoysConstants.MODID, "npc_merchant"));

    private ModMenus() {
    }
}
