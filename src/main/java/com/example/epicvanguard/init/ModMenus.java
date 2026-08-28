package com.example.epicvanguard.init;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.screen.HonorContractMenu;
import com.example.epicvanguard.screen.WarriorCompanionMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, EpicVanguardMod.MOD_ID);

    public static final RegistryObject<MenuType<WarriorCompanionMenu>> WARRIOR_COMPANION_MENU =
            MENUS.register("warrior_companion_menu",
                    () -> IForgeMenuType.create(WarriorCompanionMenu::new));

    public static final RegistryObject<MenuType<HonorContractMenu>> HONOR_CONTRACT_MENU =
            MENUS.register("honor_contract_menu",
                    () -> IForgeMenuType.create(HonorContractMenu::new));
}
