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

    public static final RegistryObject<Item> VANGUARD_POINT = ITEMS.register("vanguard_point",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.VANGUARD_POINT.get(), new Item.Properties()));

    public static final RegistryObject<Item> WARRIOR_SPAWN_EGG = ITEMS.register("warrior_companion_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.WARRIOR_COMPANION, 0x8B0000, 0xD4AF37,
                    new Item.Properties()));

    public static final RegistryObject<Item> BERSERKER_SPAWN_EGG = ITEMS.register("berserker_companion_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.BERSERKER_COMPANION, 0x8A0303, 0x222222,
                    new Item.Properties()));

    public static final RegistryObject<Item> GUARDIAN_SPAWN_EGG = ITEMS.register("guardian_companion_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.GUARDIAN_COMPANION, 0x1F4E79, 0xD9D9D9,
                    new Item.Properties()));

    public static final RegistryObject<Item> DUELIST_SPAWN_EGG = ITEMS.register("duelist_companion_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.DUELIST_COMPANION, 0x1E824C, 0xF4D03F,
                    new Item.Properties()));
}
