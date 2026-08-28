package com.example.epicvanguard;

import com.example.epicvanguard.init.ModCreativeModeTabs;
import com.example.epicvanguard.init.ModEntityTypes;
import com.example.epicvanguard.init.ModItems;
import com.example.epicvanguard.init.ModMenus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(EpicVanguardMod.MOD_ID)
public class EpicVanguardMod {
    public static final String MOD_ID = "epicvanguard";
    public static final Logger LOGGER = LogManager.getLogger();

    public EpicVanguardMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Epic Vanguard inicializado com sucesso!");
    }
}
