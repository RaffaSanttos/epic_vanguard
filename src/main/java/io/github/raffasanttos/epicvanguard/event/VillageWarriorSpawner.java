package io.github.raffasanttos.epicvanguard.event;

import io.github.raffasanttos.epicvanguard.EpicVanguardMod;
import io.github.raffasanttos.epicvanguard.entity.VillageWarriorSavedData;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

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
                                if (!savedData.hasSpawned(level.dimension(), key)) {
                                    Holder<Structure> structureHolder = level.registryAccess()
                                            .registryOrThrow(Registries.STRUCTURE)
                                            .wrapAsHolder(entry.getKey());

                                    ResourceLocation structureId = level.registryAccess()
                                            .registryOrThrow(Registries.STRUCTURE)
                                            .getKey(entry.getKey());

                                    if (structureHolder.is(PILLAGER_OUTPOST_TAG) || (structureId != null && structureId.equals(PILLAGER_OUTPOST_RL))) {
                                        savedData.markSpawned(level.dimension(), key);
                                        spawnPrisonerInOutpost(level, start);
                                    } else if (structureHolder.is(WARRIOR_HOUSE_KEY) || (structureId != null && structureId.equals(WARRIOR_HOUSE_RL))) {
                                        savedData.markSpawned(level.dimension(), key);
                                        spawnWarriorInHouse(level, start);
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    public static BlockPos findDirectPointSpawnPos(ServerLevel level, BlockPos poiPos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = poiPos.offset(dx, 0, dz);
                BlockPos below = p.below();
                if (level.getBlockState(below).isSolidRender(level, below) &&
                        !level.getBlockState(below).is(Blocks.LAVA) &&
                        !level.getBlockState(below).is(Blocks.WATER) &&
                        level.getBlockState(p).isAir() &&
                        level.getBlockState(p.above()).isAir()) {
                    return p;
                }
            }
        }
        BlockPos above = poiPos.above();
        if (level.getBlockState(above).isAir() && level.getBlockState(above.above()).isAir()) {
            return above;
        }
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, poiPos);
    }

    public static WarriorCompanionEntity createRandomSpecializedCompanion(ServerLevel level) {
        int roll = level.random.nextInt(3);
        if (roll == 0) {
            return ModEntityTypes.BERSERKER_COMPANION.get().create(level);
        } else if (roll == 1) {
            return ModEntityTypes.GUARDIAN_COMPANION.get().create(level);
        } else {
            return ModEntityTypes.DUELIST_COMPANION.get().create(level);
        }
    }

    private static void spawnTavernMercenary(ServerLevel level, BlockPos spawnPos, ServerPlayer player, BlockPos poiPos, boolean playerIsNear) {
        WarriorCompanionEntity warrior = createRandomSpecializedCompanion(level);
        if (warrior != null) {
            warrior.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            warrior.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null, null);
            warrior.applyEquipmentTier(WarriorCompanionEntity.rollRandomTier(level.random));
            warrior.setRecruited(false);
            warrior.setCombatMode(1);
            level.addFreshEntity(warrior);

            if (playerIsNear) {
                // 🎆 Fogos de artifício no céu onde o guerreiro apareceu
                launchArrivalFirework(level, spawnPos);

                // 📯 Efeitos sonoros de aviso e celebração
                level.playSound(null, player.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).get(), SoundSource.NEUTRAL, 1.2F, 1.0F);
                level.playSound(null, spawnPos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 2.0F, 1.0F);

                // 💬 Notificação para o jogador
                player.sendSystemMessage(Component.translatable("epicvanguard.text.village_warrior_spawner.1"));
            } else {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D,
                        15, 0.5D, 0.5D, 0.5D, 0.05D);
            }
        }
    }

    public static void launchArrivalFirework(ServerLevel level, BlockPos spawnPos) {
        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag tag = fireworkStack.getOrCreateTagElement("Fireworks");
        tag.putByte("Flight", (byte) 2);
        ListTag explosions = new ListTag();
        CompoundTag explosion = new CompoundTag();
        explosion.putByte("Type", (byte) 1); // Large ball
        explosion.putIntArray("Colors", new int[] { 0xFFD700, 0xFFA500, 0xE6C229 }); // Ouro, Laranja, Dourado
        explosion.putByte("Flicker", (byte) 1);
        explosion.putByte("Trail", (byte) 1);
        explosions.add(explosion);
        tag.put("Explosions", explosions);

        FireworkRocketEntity firework = new FireworkRocketEntity(level, spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D, fireworkStack);
        level.addFreshEntity(firework);
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
        WarriorCompanionEntity prisoner = createRandomSpecializedCompanion(level);
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

        WarriorCompanionEntity warrior = createRandomSpecializedCompanion(level);
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
