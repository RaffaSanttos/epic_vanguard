package com.example.epicvanguard.init;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.item.GoldCoinItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EpicVanguardMod.MOD_ID);

    public static final RegistryObject<Item> GOLD_COIN = ITEMS.register("gold_coin",
            () -> new GoldCoinItem(new Item.Properties()));

    public static final RegistryObject<Item> WARRIOR_SPAWN_EGG = ITEMS.register("warrior_companion_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.WARRIOR_COMPANION, 0x8B0000, 0xD4AF37,
                    new Item.Properties()));
}
