package com.frost.envoys.init;

import com.frost.envoys.Envoys;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Envoys.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENVOYS_TAB = CREATIVE_TABS.register("envoys_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.envoys"))
            .icon(() -> ModItems.TAB_ICON.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.NPC_TUNER.get());
                //output.accept(ModItems.SURVIVAL_NPC_TUNER.get());
                output.accept(ModItems.NPC_SPAWN_EGG.get());
            }).build());
    
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.NPC_TUNER);
            //event.accept(ModItems.SURVIVAL_NPC_TUNER);
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS)
        {
            event.accept(ModItems.NPC_SPAWN_EGG);
        }
    }
}