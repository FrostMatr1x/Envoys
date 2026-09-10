package com.frost.envoys.init;

import com.frost.envoys.Envoys;
import com.frost.envoys.gui.menu.NPCMerchantMenu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = 
        DeferredRegister.create(BuiltInRegistries.MENU, Envoys.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<NPCMerchantMenu>> NPC_MERCHANT_MENU =
        MENUS.register("npc_merchant", () -> IMenuTypeExtension.create(
            (containerId, playerInventory, extraData) -> new NPCMerchantMenu(containerId, playerInventory)
        ));
}
