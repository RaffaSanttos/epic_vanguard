package io.github.raffasanttos.epicvanguard.init;

import io.github.raffasanttos.epicvanguard.EpicVanguardMod;
import io.github.raffasanttos.epicvanguard.block.VanguardPointBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EpicVanguardMod.MOD_ID);

    public static final RegistryObject<Block> VANGUARD_POINT = BLOCKS.register("vanguard_point",
            VanguardPointBlock::new);
}
