package com.frost.envoys.init;

import com.frost.envoys.Envoys;
import com.frost.envoys.npc.entity.BaseNPC;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> NPC_TYPES = 
        DeferredRegister.create(Registries.ENTITY_TYPE, Envoys.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BaseNPC>> BASE_NPC = 
        NPC_TYPES.register("base_npc", () -> EntityType.Builder.of(BaseNPC::new, MobCategory.CREATURE)
            .sized(0.6F, 1.8F)
            .build("base_npc")
        );
}