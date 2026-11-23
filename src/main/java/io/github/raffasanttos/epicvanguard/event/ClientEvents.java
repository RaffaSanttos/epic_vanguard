package io.github.raffasanttos.epicvanguard.event;

import io.github.raffasanttos.epicvanguard.EpicVanguardMod;
import io.github.raffasanttos.epicvanguard.client.renderer.WarriorCompanionRenderer;
import io.github.raffasanttos.epicvanguard.init.ModEntityTypes;
import io.github.raffasanttos.epicvanguard.init.ModMenus;
import io.github.raffasanttos.epicvanguard.screen.HonorContractScreen;
import io.github.raffasanttos.epicvanguard.screen.VanguardPointScreen;
import io.github.raffasanttos.epicvanguard.screen.WarriorCompanionScreen;
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
            MenuScreens.register(ModMenus.VANGUARD_POINT_MENU.get(), VanguardPointScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.WARRIOR_COMPANION.get(), WarriorCompanionRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.BERSERKER_COMPANION.get(), WarriorCompanionRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.GUARDIAN_COMPANION.get(), WarriorCompanionRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DUELIST_COMPANION.get(), WarriorCompanionRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.ARCHER_COMPANION.get(), WarriorCompanionRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.STANDARD_BEARER_COMPANION.get(), WarriorCompanionRenderer::new);
    }
}
