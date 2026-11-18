package com.frost.envoys.neoforge;

import com.frost.envoys.EnvoysConstants;
import com.frost.envoys.gui.menu.NPCMerchantMenu;
import com.frost.envoys.init.ModEntities;
import com.frost.envoys.init.ModItems;
import com.frost.envoys.init.ModMenus;
import com.frost.envoys.npc.entity.BaseNPC;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeRegistrations {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnvoysConstants.MODID);
    private static final DeferredRegister<EntityType<?>> NPC_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, EnvoysConstants.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EnvoysConstants.MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EnvoysConstants.MODID);

    private static final DeferredItem<Item> NPC_TUNER = ITEMS.registerSimpleItem("npc_tuner", new Item.Properties());
    private static final DeferredItem<Item> TAB_ICON = ITEMS.registerSimpleItem("tab_icon", new Item.Properties());

    private static final DeferredHolder<EntityType<?>, EntityType<BaseNPC>> BASE_NPC =
            NPC_TYPES.register("base_npc", () -> EntityType.Builder.of(BaseNPC::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .build("base_npc"));

    private static final DeferredItem<SpawnEggItem> NPC_SPAWN_EGG = ITEMS.registerItem("npc_spawn_egg",
            properties -> new DeferredSpawnEggItem(ModEntities.BASE_NPC::get, 0xFFFFFF, 0xFFFFFF,
                    new Item.Properties().rarity(Rarity.EPIC)));

    private static final DeferredHolder<MenuType<?>, MenuType<NPCMerchantMenu>> NPC_MERCHANT_MENU =
            MENUS.register("npc_merchant", () -> IMenuTypeExtension.create(
                    (containerId, playerInventory, extraData) -> new NPCMerchantMenu(containerId, playerInventory)));

    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENVOYS_TAB =
            CREATIVE_TABS.register("envoys_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.envoys"))
                    .icon(() -> ModItems.TAB_ICON.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.NPC_TUNER.get());
                        output.accept(ModItems.NPC_SPAWN_EGG.get());
                    }).build());

    private NeoForgeRegistrations() {
    }

    public static void register(IEventBus bus) {
        ModItems.NPC_TUNER.bind(NPC_TUNER);
        ModItems.TAB_ICON.bind(TAB_ICON);
        ModItems.NPC_SPAWN_EGG.bind(() -> NPC_SPAWN_EGG.get());
        ModEntities.BASE_NPC.bind(BASE_NPC);
        ModMenus.NPC_MERCHANT_MENU.bind(NPC_MERCHANT_MENU);

        ITEMS.register(bus);
        NPC_TYPES.register(bus);
        MENUS.register(bus);
        CREATIVE_TABS.register(bus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BASE_NPC.get(), BaseNPC.createAttributes().build());
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.NPC_TUNER.get());
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.NPC_SPAWN_EGG.get());
        }
    }
}
