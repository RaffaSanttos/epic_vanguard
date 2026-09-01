package com.example.epicvanguard.event;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.entity.VillageWarriorSavedData;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public class VillageWarriorSpawner {

    public static final TagKey<Structure> VILLAGE_TAG =
            TagKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft", "village"));
    public static final TagKey<Structure> PILLAGER_OUTPOST_TAG =
            TagKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft", "pillager_outpost"));
    public static final ResourceKey<Structure> WARRIOR_HOUSE_KEY =
            ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(EpicVanguardMod.MOD_ID, "warrior_house"));
    public static final ResourceLocation WARRIOR_HOUSE_RL =
            new ResourceLocation(EpicVanguardMod.MOD_ID, "warrior_house");
    public static final ResourceLocation PILLAGER_OUTPOST_RL =
            new ResourceLocation("minecraft", "pillager_outpost");

    // Rastreia o tempo do último respawn/reforço de guerreiros em cada vila (72.000 ticks = 3 dias de Minecraft)
    private static final Map<BlockPos, Long> LAST_VILLAGE_SPAWN = new HashMap<>();
    private static final long VILLAGE_RESPAWN_INTERVAL = 72000L; // 3 dias in-game

    public static void tick(ServerLevel level) {
        // Executa a cada 40 ticks (~2 segundos) quando jogadores estão no Overworld
        if (level.getGameTime() % 40 != 0) return;

        var players = level.players();
        if (players.isEmpty()) return;

        VillageWarriorSavedData savedData = VillageWarriorSavedData.get(level.getServer());

        for (ServerPlayer player : players) {
            if (!player.isAlive() || player.isSpectator()) continue;

            int playerChunkX = player.getBlockX() >> 4;
            int playerChunkZ = player.getBlockZ() >> 4;

            // 1. Checa estruturas em chunks carregados ao redor do jogador
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int cx = playerChunkX + dx;
                    int cz = playerChunkZ + dz;

                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) continue;

                    Map<Structure, StructureStart> starts = chunk.getAllStarts();
                    if (starts.isEmpty()) continue;

                    for (Map.Entry<Structure, StructureStart> entry : starts.entrySet()) {
                        StructureStart start = entry.getValue();
                        if (start != null && start.isValid()) {
                            ChunkPos startPos = start.getChunkPos();
                            if (startPos.x == cx && startPos.z == cz) {
                                long key = startPos.toLong();
                                if (!savedData.hasSpawned(key)) {
                                    Holder<Structure> structureHolder = level.registryAccess()
                                            .registryOrThrow(Registries.STRUCTURE)
                                            .wrapAsHolder(entry.getKey());

                                    ResourceLocation structureId = level.registryAccess()
                                            .registryOrThrow(Registries.STRUCTURE)
                                            .getKey(entry.getKey());

                                    if (structureHolder.is(PILLAGER_OUTPOST_TAG) || (structureId != null && structureId.equals(PILLAGER_OUTPOST_RL))) {
                                        savedData.markSpawned(key);
                                        spawnPrisonerInOutpost(level, start);
                                    } else if (structureHolder.is(WARRIOR_HOUSE_KEY) || (structureId != null && structureId.equals(WARRIOR_HOUSE_RL))) {
                                        savedData.markSpawned(key);
                                        spawnWarriorInHouse(level, start);
                                    } else if (structureHolder.is(VILLAGE_TAG) || (structureId != null && structureId.getPath().contains("village"))) {
                                        savedData.markSpawned(key);
                                        spawnWarriorsInVillage(level, start);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Checa reforço de mercenários em vilas próximas se todos foram contratados (a cada 3 dias)
            checkVillageRespawn(level, player);
        }
    }

    private static void spawnWarriorsInVillage(ServerLevel level, StructureStart start) {
        BoundingBox bb = start.getBoundingBox();
        int centerX = (bb.minX() + bb.maxX()) / 2;
        int centerZ = (bb.minZ() + bb.maxZ()) / 2;
        BlockPos villageCenter = new BlockPos(centerX, (bb.minY() + bb.maxY()) / 2, centerZ);

        LAST_VILLAGE_SPAWN.put(villageCenter, level.getGameTime());

        int count = 2 + level.random.nextInt(3); // 2 a 4 guerreiros por vila
        for (int i = 0; i < count; i++) {
            BlockPos spawnPos = findSafeVillageGround(level, centerX, centerZ, bb, 2 + i * 4);
            if (spawnPos == null) {
                spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        new BlockPos(centerX + (i * 4 - 4), 0, centerZ + (i * 3 - 3)));
            }

            WarriorCompanionEntity warrior = ModEntityTypes.WARRIOR_COMPANION.get().create(level);
            if (warrior != null) {
                warrior.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                        level.random.nextFloat() * 360.0F, 0.0F);
                warrior.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null, null);
                warrior.applyEquipmentTier(WarriorCompanionEntity.rollRandomTier(level.random));
                warrior.setRecruited(false);
                warrior.setCombatMode(1);
                level.addFreshEntity(warrior);

                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D,
                        12, 0.5D, 0.5D, 0.5D, 0.05D);
                level.playSound(null, spawnPos, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
        }
    }

    private static BlockPos findSafeVillageGround(ServerLevel level, int centerX, int centerZ, BoundingBox bb, int radius) {
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (x < bb.minX() + 2 || x > bb.maxX() - 2 || z < bb.minZ() + 2 || z > bb.maxZ() - 2) continue;

                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                BlockPos below = ground.below();
                BlockState stateBelow = level.getBlockState(below);
                BlockState stateAt = level.getBlockState(ground);
                BlockState stateAbove = level.getBlockState(ground.above());

                if (stateBelow.isSolidRender(level, below) &&
                        !stateBelow.is(Blocks.LAVA) &&
                        !stateBelow.is(Blocks.WATER) &&
                        !stateBelow.is(Blocks.FIRE) &&
                        stateAt.isAir() &&
                        stateAbove.isAir()) {
                    return ground;
                }
            }
        }
        return null;
    }

    private static void checkVillageRespawn(ServerLevel level, ServerPlayer player) {
        long currentTime = level.getGameTime();

        for (Map.Entry<BlockPos, Long> entry : LAST_VILLAGE_SPAWN.entrySet()) {
            BlockPos center = entry.getKey();
            if (player.distanceToSqr(center.getX(), center.getY(), center.getZ()) <= 64.0D * 64.0D) {
                var unrecruited = level.getEntitiesOfClass(
                        WarriorCompanionEntity.class,
                        new AABB(center).inflate(48.0D),
                        w -> !w.isRecruited() && !w.isPrisoner()
                );

                if (unrecruited.isEmpty()) {
                    long lastTime = entry.getValue();
                    if (lastTime == 0L || (currentTime - lastTime) >= VILLAGE_RESPAWN_INTERVAL) {
                        entry.setValue(currentTime);
                        int count = 1 + level.random.nextInt(2);
                        for (int i = 0; i < count; i++) {
                            BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    center.offset(level.random.nextInt(8) - 4, 0, level.random.nextInt(8) - 4));
                            WarriorCompanionEntity warrior = ModEntityTypes.WARRIOR_COMPANION.get().create(level);
                            if (warrior != null) {
                                warrior.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                                        level.random.nextFloat() * 360.0F, 0.0F);
                                warrior.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null, null);
                                warrior.applyEquipmentTier(WarriorCompanionEntity.rollRandomTier(level.random));
                                warrior.setRecruited(false);
                                warrior.setCombatMode(1);
                                level.addFreshEntity(warrior);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void spawnPrisonerInOutpost(ServerLevel level, StructureStart start) {
        BoundingBox bb = start.getBoundingBox();
        int centerX = (bb.minX() + bb.maxX()) / 2;
        int centerZ = (bb.minZ() + bb.maxZ()) / 2;

        // Localiza um ponto aberto no posto avançado para a cela 4x4
        BlockPos cagePos = null;
        for (int dx = -14; dx <= 14; dx += 4) {
            for (int dz = -14; dz <= 14; dz += 4) {
                if (Math.abs(dx) < 6 && Math.abs(dz) < 6) continue;
                int x = centerX + dx;
                int z = centerZ + dz;
                if (x < bb.minX() || x > bb.maxX() || z < bb.minZ() || z > bb.maxZ()) continue;

                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                BlockPos below = ground.below();
                if (level.getBlockState(below).isSolidRender(level, below) && !level.getBlockState(below).is(Blocks.LAVA)) {
                    cagePos = ground;
                    break;
                }
            }
            if (cagePos != null) break;
        }

        if (cagePos == null) {
            cagePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(centerX + 10, 0, centerZ + 10));
        }

        // Constrói a Cela de Prisioneiro 4x4 de Carvalho Escuro
        buildPillagerPrisonerCage(level, cagePos);

        // Spawna o Prisioneiro exatamente no interior da cela (espaço 2x2)
        BlockPos insidePos = cagePos.offset(1, 1, 1);
        WarriorCompanionEntity prisoner = ModEntityTypes.WARRIOR_COMPANION.get().create(level);
        if (prisoner != null) {
            prisoner.moveTo(insidePos.getX() + 0.5D, insidePos.getY(), insidePos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            prisoner.finalizeSpawn(level, level.getCurrentDifficultyAt(insidePos), MobSpawnType.STRUCTURE, null, null);
            prisoner.applyEquipmentTier(-1); // Prisioneiro: Sem armadura, 3.0 HP, Fraqueza permanente
            prisoner.setRecruited(false);
            prisoner.setCombatMode(1);
            level.addFreshEntity(prisoner);
        }
    }

    private static void buildPillagerPrisonerCage(ServerLevel level, BlockPos origin) {
        // Base 4x4 no chão (Y = 0)
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                level.setBlock(origin.offset(x, 0, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
            }
        }

        // Paredes (Y = 1 e Y = 2) com cantos de toras e cercas de carvalho escuro
        for (int y = 1; y <= 2; y++) {
            for (int x = 0; x < 4; x++) {
                for (int z = 0; z < 4; z++) {
                    BlockPos p = origin.offset(x, y, z);
                    boolean isCorner = (x == 0 || x == 3) && (z == 0 || z == 3);
                    boolean isEdge = (x == 0 || x == 3 || z == 0 || z == 3);

                    if (isCorner) {
                        level.setBlock(p, Blocks.DARK_OAK_LOG.defaultBlockState(), 3);
                    } else if (isEdge) {
                        level.setBlock(p, Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
                    } else {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        // Teto (Y = 3)
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                level.setBlock(origin.offset(x, 3, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
            }
        }
        // Detalhe no topo (Y = 4) - Lajes 2x2
        for (int x = 1; x <= 2; x++) {
            for (int z = 1; z <= 2; z++) {
                level.setBlock(origin.offset(x, 4, z), Blocks.DARK_OAK_SLAB.defaultBlockState(), 3);
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
            warrior.applyEquipmentTier(WarriorCompanionEntity.rollRandomTier(level.random));
            warrior.setRecruited(false);
            warrior.setCombatMode(1);
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
}
