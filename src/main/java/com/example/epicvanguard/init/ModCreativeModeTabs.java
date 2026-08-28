package com.example.epicvanguard.init;

import com.example.epicvanguard.EpicVanguardMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EpicVanguardMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> VANGUARD_TAB = CREATIVE_MODE_TABS.register("vanguard_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.GOLD_COIN.get()))
                    .title(Component.translatable("itemGroup.epicvanguard.vanguard_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.GOLD_COIN.get());
                        output.accept(ModItems.WARRIOR_SPAWN_EGG.get());
                    })
                    .build());
}
