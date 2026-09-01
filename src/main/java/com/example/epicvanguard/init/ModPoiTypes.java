package com.example.epicvanguard.init;

import com.example.epicvanguard.EpicVanguardMod;
import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModPoiTypes {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(ForgeRegistries.POI_TYPES, EpicVanguardMod.MOD_ID);

    public static final RegistryObject<PoiType> VANGUARD_POI = POI_TYPES.register("vanguard_point",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.VANGUARD_POINT.get().getStateDefinition().getPossibleStates()), 2, 1));
}
