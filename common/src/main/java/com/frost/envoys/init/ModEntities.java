package com.frost.envoys.init;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.npc.entity.BaseNPC;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public final class ModEntities {
    public static final RegistryEntry<EntityType<BaseNPC>> BASE_NPC =
            new RegistryEntry<>(ResourceLocation.fromNamespaceAndPath(EnvoysConstants.MODID, "base_npc"));

    private ModEntities() {
    }
}
