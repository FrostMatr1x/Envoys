package com.frost.envoys.quest;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class QuestKillTracker {

    private QuestKillTracker() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead == null || dead.level().isClientSide()) {
            return;
        }

        Entity killer = event.getSource().getEntity();
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

        List<QuestDefinition> candidates = QuestIndex.killCandidates(killedType.toString());
        for (QuestDefinition quest : candidates) {
            PlayerQuestManager.addKill(player, quest);
        }
    }
}
