package com.frost.envoys.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class QuestKillHandler {
    private QuestKillHandler() {
    }

    public static void handle(LivingEntity dead, DamageSource source) {
        if (dead == null || dead.level().isClientSide()) {
            return;
        }

        Entity killer = source.getEntity();
        if (!(killer instanceof ServerPlayer player)) {
            return;
        }

        if (!QuestIndex.hasKillQuests()) {
            return;
        }

        ResourceLocation killedType = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        if (killedType == null) {
            return;
        }

        for (QuestDefinition quest : QuestIndex.killCandidates(killedType.toString())) {
            PlayerQuestManager.addKill(player, quest);
        }
    }
}
