package com.example.epicvanguard.event;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.client.renderer.WarriorCompanionRenderer;
import com.example.epicvanguard.init.ModEntityTypes;
import com.example.epicvanguard.init.ModMenus;
import com.example.epicvanguard.screen.HonorContractScreen;
import com.example.epicvanguard.screen.WarriorCompanionScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = EpicVanguardMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.WARRIOR_COMPANION_MENU.get(), WarriorCompanionScreen::new);
            MenuScreens.register(ModMenus.HONOR_CONTRACT_MENU.get(), HonorContractScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.WARRIOR_COMPANION.get(), WarriorCompanionRenderer::new);
    }
}
