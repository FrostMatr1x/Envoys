package com.frost.envoys.init;

import com.frost.envoys.Envoys;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Envoys.MODID);

    public static final DeferredItem<Item> NPC_TUNER = ITEMS.registerSimpleItem("npc_tuner", new Item.Properties());
    public static final DeferredItem<Item> SURVIVAL_NPC_TUNER = ITEMS.registerSimpleItem("survival_npc_tuner", new Item.Properties());
    public static final DeferredItem<Item> TAB_ICON = ITEMS.registerSimpleItem("tab_icon", new Item.Properties());
    
    public static final DeferredItem<SpawnEggItem> NPC_SPAWN_EGG = ITEMS.registerItem("npc_spawn_egg",
        properties -> new DeferredSpawnEggItem(ModEntities.BASE_NPC::get, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
}
