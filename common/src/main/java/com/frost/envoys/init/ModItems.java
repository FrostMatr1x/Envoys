package com.frost.envoys.init;

import com.frost.envoys.EnvoysConstants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final RegistryEntry<Item> NPC_TUNER =
            new RegistryEntry<>(ResourceLocation.fromNamespaceAndPath(EnvoysConstants.MODID, "npc_tuner"));
    public static final RegistryEntry<Item> TAB_ICON =
            new RegistryEntry<>(ResourceLocation.fromNamespaceAndPath(EnvoysConstants.MODID, "tab_icon"));
    public static final RegistryEntry<Item> NPC_SPAWN_EGG =
            new RegistryEntry<>(ResourceLocation.fromNamespaceAndPath(EnvoysConstants.MODID, "npc_spawn_egg"));

    private ModItems() {
    }
}
