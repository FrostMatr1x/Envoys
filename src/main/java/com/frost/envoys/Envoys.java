package com.frost.envoys;

import com.frost.envoys.action.ActionEngineManager;
import com.frost.envoys.event.ClientEvents;
import com.frost.envoys.event.CommonEvents;
import com.frost.envoys.event.ServerEvents;
import com.frost.envoys.gui.screen.NPCTradeScreen;
import com.frost.envoys.init.ModCreativeTabs;
import com.frost.envoys.init.ModEntities;
import com.frost.envoys.init.ModItems;
import com.frost.envoys.init.ModMenus;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.quest.QuestKillTracker;
import com.mojang.logging.LogUtils;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(Envoys.MODID)
public class Envoys {
    public static final String MODID = "envoys";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Envoys(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModEntities.NPC_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        modEventBus.addListener(this::registerAttributes);
        modEventBus.addListener(ModCreativeTabs::addCreative);

        modEventBus.register(CommonEvents.class);

        if (FMLEnvironment.dist.isClient()) {
            modEventBus.register(ClientEvents.class);
        }

        NeoForge.EVENT_BUS.register(ServerEvents.class);
        NeoForge.EVENT_BUS.register(QuestKillTracker.class);

        ActionEngineManager.initialize();
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BASE_NPC.get(), BaseNPC.createAttributes().build());
    }
}