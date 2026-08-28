package com.example.epicvanguard.world;

import com.example.epicvanguard.entity.VillageCompanionSavedData;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class VillageCompanionSpawner {

    // 1 dia Minecraft = 24.000 ticks
    // Intervalo muito mais dinâmico e frequente:
    // Base: 12.000 ticks (~10 minutos / meio dia in-game)
    // Variacao: ate +12.000 ticks (ate meio dia de variacao)
    private static final long MIN_INTERVAL_TICKS = 12000L;
    private static final long MAX_EXTRA_TICKS   = 12000L;

    public static void tick(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return;

        // Executa verificacao a cada 100 ticks (5s) para resposta agil
        if (level.getGameTime() % 100 != 0) return;

        VillageCompanionSavedData data = VillageCompanionSavedData.get(level);
        long currentTime = level.getGameTime();

        // Inicializa mundo novo com prontidao quase imediata para o primeiro spawn
        if (data.getLastSpawnTime() == 0) {
            data.setLastSpawnTime(1L);
            data.setNextInterval(100L);
            return;
        }

        // Verifica se ja se passou o intervalo necessario
        if (currentTime - data.getLastSpawnTime() < data.getNextInterval()) {
            return;
        }

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        PoiManager poiManager = level.getPoiManager();

        for (ServerPlayer player : players) {
            if (!player.isAlive() || player.isSpectator()) continue;

            BlockPos playerPos = player.blockPosition();

            // Busca camas (PoiTypes.HOME) em um raio de 48 blocos ao redor do jogador
            List<BlockPos> beds = poiManager.getInRange(
                    holder -> holder.is(PoiTypes.HOME),
                    playerPos,
                    48,
                    PoiManager.Occupancy.ANY
            ).map(PoiRecord::getPos).toList();

            // Aceita vilarejos e bases com pelo menos 2 camas
            if (beds.size() >= 2) {
                // Vilas pequenas (2-5 camas): max 2 guerreiros disponiveis
                // Vilas maiores (6+ camas): max 3 guerreiros disponiveis
                int maxUnrecruited = beds.size() >= 6 ? 3 : 2;

                var existingWarriors = level.getEntitiesOfClass(
                        WarriorCompanionEntity.class,
                        new AABB(playerPos).inflate(36.0D),
                        w -> !w.isRecruited()
                );
                if (existingWarriors.size() >= maxUnrecruited) {
                    continue; // Ja tem guerreiros suficientes disponiveis nesta vila
                }

                // Encontra uma posicao valida e segura no chao proximo a uma das camas
                BlockPos centerBed = beds.get(level.random.nextInt(beds.size()));
                BlockPos spawnPos = findSafeSpawnPosition(level, centerBed);

                if (spawnPos != null) {
                    WarriorCompanionEntity warrior = ModEntityTypes.WARRIOR_COMPANION.get().create(level);
                    if (warrior != null) {
                        warrior.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                                level.random.nextFloat() * 360.0F, 0.0F);

                        // Garante custo balanceado em torno de 40 moedas de ouro (35 a 45)
                        warrior.setRecruitCost(35 + level.random.nextInt(11));
                        level.addFreshEntity(warrior);

                        // Efeitos de aparicao
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D,
                                15, 0.5D, 0.5D, 0.5D, 0.05D);
                        level.playSound(null, spawnPos, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);

                        // Atualiza o tempo para o proximo spawn
                        data.setLastSpawnTime(currentTime);
                        data.setNextInterval(MIN_INTERVAL_TICKS + level.random.nextInt((int) MAX_EXTRA_TICKS));
                        break;
                    }
                }
            }
        }
    }

    private static BlockPos findSafeSpawnPosition(ServerLevel level, BlockPos origin) {
        for (int attempts = 0; attempts < 15; attempts++) {
            int dx = level.random.nextInt(11) - 5; // -5 a +5
            int dz = level.random.nextInt(11) - 5;
            int dy = level.random.nextInt(5) - 2;

            BlockPos check = origin.offset(dx, dy, dz);
            BlockPos below = check.below();

            if (level.getBlockState(below).isSolidRender(level, below) &&
                !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.LAVA) &&
                !level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.FIRE) &&
                level.getBlockState(check).isAir() &&
                level.getBlockState(check.above()).isAir()) {
                return check;
            }
        }
        return null;
    }
}
