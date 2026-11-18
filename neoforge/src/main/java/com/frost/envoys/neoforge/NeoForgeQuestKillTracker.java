package com.frost.envoys.neoforge;

import com.frost.envoys.quest.QuestKillHandler;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class NeoForgeQuestKillTracker {
    private NeoForgeQuestKillTracker() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        QuestKillHandler.handle(event.getEntity(), event.getSource());
    }
}
