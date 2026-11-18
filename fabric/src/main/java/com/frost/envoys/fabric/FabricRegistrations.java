package com.frost.envoys.fabric;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.gui.menu.NPCMerchantMenu;
import com.frost.envoys.init.ModEntities;
import com.frost.envoys.init.ModItems;
import com.frost.envoys.init.ModMenus;
import com.frost.envoys.npc.entity.BaseNPC;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

public final class FabricRegistrations {
    private FabricRegistrations() {
    }

    public static void register() {
        EntityType<BaseNPC> npcType = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                id("base_npc"),
                EntityType.Builder.of(BaseNPC::new, MobCategory.CREATURE)
                        .sized(0.6F, 1.8F)
                        .build(id("base_npc").toString()));
        ModEntities.BASE_NPC.set(npcType);
        FabricDefaultAttributeRegistry.register(npcType, BaseNPC.createAttributes().build());

        Item tuner = Registry.register(
                BuiltInRegistries.ITEM,
                id("npc_tuner"),
                new Item(new Item.Properties()));
        ModItems.NPC_TUNER.set(tuner);

        Item tabIcon = Registry.register(
                BuiltInRegistries.ITEM,
                id("tab_icon"),
                new Item(new Item.Properties()));
        ModItems.TAB_ICON.set(tabIcon);

        SpawnEggItem spawnEgg = Registry.register(
                BuiltInRegistries.ITEM,
                id("npc_spawn_egg"),
                new SpawnEggItem(npcType, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
        ModItems.NPC_SPAWN_EGG.set(spawnEgg);

        MenuType<NPCMerchantMenu> merchantMenu = Registry.register(
                BuiltInRegistries.MENU,
                id("npc_merchant"),
                new MenuType<NPCMerchantMenu>(NPCMerchantMenu::new, FeatureFlags.VANILLA_SET));
        ModMenus.NPC_MERCHANT_MENU.set(merchantMenu);

        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                id("envoys_tab"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.envoys"))
                        .icon(() -> ModItems.TAB_ICON.get().getDefaultInstance())
                        .displayItems((parameters, output) -> {
                            output.accept(ModItems.NPC_TUNER.get());
                            output.accept(ModItems.NPC_SPAWN_EGG.get());
                        })
                        .build());

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(ModItems.NPC_TUNER.get()));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(entries -> entries.accept(ModItems.NPC_SPAWN_EGG.get()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(EnvoysConstants.MODID, path);
    }
}
