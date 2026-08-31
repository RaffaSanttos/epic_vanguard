package com.example.epicvanguard.event;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.entity.VillageWarriorSavedData;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = EpicVanguardMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VillageWarriorSpawner {

    public static final TagKey<Structure> VILLAGE_TAG =
            TagKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft", "village"));
    public static final ResourceKey<Structure> WARRIOR_HOUSE_KEY =
            ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(EpicVanguardMod.MOD_ID, "warrior_house"));
    public static final ResourceLocation WARRIOR_HOUSE_RL =
            new ResourceLocation(EpicVanguardMod.MOD_ID, "warrior_house");

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkAccess chunk = event.getChunk();
        Map<Structure, StructureStart> starts = chunk.getAllStarts();
        if (starts.isEmpty()) return;

        VillageWarriorSavedData savedData = VillageWarriorSavedData.get(serverLevel.getServer());

        for (Map.Entry<Structure, StructureStart> entry : starts.entrySet()) {
            StructureStart start = entry.getValue();
            if (start != null && start.isValid()) {
                Holder<Structure> structureHolder = serverLevel.registryAccess()
                        .registryOrThrow(Registries.STRUCTURE)
                        .wrapAsHolder(entry.getKey());

                ResourceLocation structureId = serverLevel.registryAccess()
                        .registryOrThrow(Registries.STRUCTURE)
                        .getKey(entry.getKey());

                ChunkPos startPos = start.getChunkPos();
                if (startPos.equals(chunk.getPos())) {
                    long key = startPos.toLong();
                    if (!savedData.hasSpawned(key)) {
                        if (structureHolder.is(VILLAGE_TAG)) {
                            savedData.markSpawned(key);
                            spawnWarriorsInVillage(serverLevel, start);
                        } else if (structureHolder.is(WARRIOR_HOUSE_KEY) || (structureId != null && structureId.equals(WARRIOR_HOUSE_RL))) {
                            savedData.markSpawned(key);
                            spawnWarriorInHouse(serverLevel, start);
                        }
                    }
                }
            }
        }
    }

    private static void spawnWarriorInHouse(ServerLevel level, StructureStart start) {
        BoundingBox bb = start.getBoundingBox();
        int centerX = (bb.minX() + bb.maxX()) / 2;
        int centerZ = (bb.minZ() + bb.maxZ()) / 2;

        BlockPos spawnPos = findSafeHouseSpawnPos(level, bb, centerX, centerZ);
        if (spawnPos == null) {
            spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(centerX, 0, centerZ));
        }

        WarriorCompanionEntity warrior = ModEntityTypes.WARRIOR_COMPANION.get().create(level);
        if (warrior != null) {
            warrior.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            warrior.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null, null);
            warrior.setRecruited(false);
            warrior.setCombatMode(1); // Defensivo por padrão
            level.addFreshEntity(warrior);
        }
    }

    private static BlockPos findSafeHouseSpawnPos(ServerLevel level, BoundingBox bb, int centerX, int centerZ) {
        for (int r = 0; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    if (x < bb.minX() || x > bb.maxX() || z < bb.minZ() || z > bb.maxZ()) continue;

                    for (int y = bb.minY(); y <= bb.maxY(); y++) {
                        BlockPos check = new BlockPos(x, y, z);
                        BlockPos below = check.below();
                        BlockState stateBelow = level.getBlockState(below);
                        BlockState stateCheck = level.getBlockState(check);
                        BlockState stateAbove = level.getBlockState(check.above());

                        if (stateBelow.isSolidRender(level, below) &&
                                !stateBelow.is(Blocks.LAVA) &&
                                !stateBelow.is(Blocks.FIRE) &&
                                stateCheck.isAir() &&
                                stateAbove.isAir()) {
                            return check;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static void spawnWarriorsInVillage(ServerLevel level, StructureStart start) {
        int centerX = (start.getBoundingBox().minX() + start.getBoundingBox().maxX()) / 2;
        int centerZ = (start.getBoundingBox().minZ() + start.getBoundingBox().maxZ()) / 2;

        // Cada vila terá garantidamente entre 2 e 4 guerreiros
        int count = 2 + level.random.nextInt(3);

        for (int i = 0; i < count; i++) {
            int offsetX = (level.random.nextInt(9) - 4) * 3;
            int offsetZ = (level.random.nextInt(9) - 4) * 3;
            int targetX = centerX + offsetX;
            int targetZ = centerZ + offsetZ;

            BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(targetX, 0, targetZ));

            WarriorCompanionEntity warrior = ModEntityTypes.WARRIOR_COMPANION.get().create(level);
            if (warrior != null) {
                warrior.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                        level.random.nextFloat() * 360.0F, 0.0F);
                warrior.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null, null);
                warrior.setRecruited(false);
                warrior.setCombatMode(1); // Defensivo por padrão
                level.addFreshEntity(warrior);
            }
        }
    }
}
