package com.example.epicvanguard.init;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EpicVanguardMod.MOD_ID);

    public static final RegistryObject<EntityType<WarriorCompanionEntity>> WARRIOR_COMPANION =
            ENTITY_TYPES.register("warrior_companion",
                    () -> EntityType.Builder.<WarriorCompanionEntity>of(WarriorCompanionEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(new ResourceLocation(EpicVanguardMod.MOD_ID, "warrior_companion").toString()));
}
