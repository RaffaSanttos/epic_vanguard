package com.example.epicvanguard.init;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.block.entity.VanguardPointBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EpicVanguardMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<VanguardPointBlockEntity>> VANGUARD_POINT_BE =
            BLOCK_ENTITIES.register("vanguard_point",
                    () -> BlockEntityType.Builder.of(VanguardPointBlockEntity::new, ModBlocks.VANGUARD_POINT.get()).build(null));
}
