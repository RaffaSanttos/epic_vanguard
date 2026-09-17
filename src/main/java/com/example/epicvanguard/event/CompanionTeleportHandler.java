package com.example.epicvanguard.event;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.entity.CompanionSavedData;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o teleporte e acompanhamento contínuo de companheiros em Modo 0 (Seguir).
 * Garante que:
 * 1. Se o jogador estiver no chão e as companhias estiverem em modo Seguir (0),
 *    elas se teleportarão para ele onde quer que estejam (mesmo em chunks descarregados fora de colônias).
 * 2. Se a entidade estiver em um chunk descarregado (ex: na natureza ao voltar para uma colônia com Minecolonies),
 *    ela é materializada e recuperada imediatamente ao lado do jogador através do snapshot persistente de NBT.
 * 3. Prevenção total de duplicações: caso um chunk antigo na natureza seja carregado posteriormente,
 *    a cópia antiga é descartada imediatamente pelo EntityJoinLevelEvent.
 * 4. Companhias em Modo 1 (Guarda) e Modo 2 (Parado) permanecem estritamente em suas posições.
 * 5. Totalmente agnóstico: não cria nenhuma dependência externa.
 */
@Mod.EventBusSubscriber(modid = EpicVanguardMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CompanionTeleportHandler {

    private record PlayerPosRecord(ResourceLocation dimension, double x, double y, double z) {}

    private static final Map<UUID, PlayerPosRecord> LAST_POSITIONS = new ConcurrentHashMap<>();

    /**
     * Verifica se o jogador está com os pés no chão firme ou sobre blocos de suporte
     * (não caindo no void, não planando de Elytra, nem voando no ar).
     */
    public static boolean isPlayerSafelyGrounded(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return false;
        if (player.isFallFlying() || player.getAbilities().flying) return false;
        if (player.onGround()) return true;

        BlockPos pos = player.blockPosition();
        BlockPos below = pos.below();
        ServerLevel level = player.serverLevel();

        return level.getBlockState(below).isSolid()
                || level.getBlockState(pos).isSolid()
                || !level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!player.isAlive() || player.isSpectator()) return;

        UUID playerUUID = player.getUUID();
        ResourceLocation currentDim = player.serverLevel().dimension().location();
        double curX = player.getX();
        double curY = player.getY();
        double curZ = player.getZ();

        PlayerPosRecord last = LAST_POSITIONS.get(playerUUID);

        if (last != null) {
            boolean dimChanged = !last.dimension.equals(currentDim);
            double distSq = (curX - last.x) * (curX - last.x)
                    + (curY - last.y) * (curY - last.y)
                    + (curZ - last.z) * (curZ - last.z);

            // Teleporte detectado: mudança de dimensão ou salto rápido (> 12 blocos em 1 tick)
            if (dimChanged || distSq > 144.0D) {
                if (isPlayerSafelyGrounded(player)) {
                    rescueFollowingCompanions(player);
                }
            }
        }

        // Verificação contínua a cada 20 ticks (1 segundo):
        // Se o jogador estiver no chão firme, garante que qualquer companheiro em modo Seguir (0)
        // que tenha ficado para trás (chunk descarregado ou > 24 blocos) venha para o jogador!
        if (player.tickCount % 20 == 0) {
            if (isPlayerSafelyGrounded(player)) {
                rescueFollowingCompanions(player);
            }
        }

        LAST_POSITIONS.put(playerUUID, new PlayerPosRecord(currentDim, curX, curY, curZ));
    }

    @SubscribeEvent
    public static void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isPlayerSafelyGrounded(player)) {
            rescueFollowingCompanions(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            rescueFollowingCompanions(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            rescueFollowingCompanions(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_POSITIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        // Ao descarregar chunk ou sair do mundo, salva o estado atualizado imediatamente
        if (event.getEntity() instanceof WarriorCompanionEntity warrior && !event.getLevel().isClientSide()) {
            if (warrior.isAlive() && warrior.isRecruited()) {
                warrior.updateSavedData();
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof WarriorCompanionEntity warrior && !event.getLevel().isClientSide()) {
            MinecraftServer server = event.getLevel().getServer();
            if (server == null) return;

            UUID uuid = warrior.getUUID();
            // Prevenção de duplicação: se já existe outra instância viva com este UUID no servidor
            // (ex: o chunk antigo na natureza acordou depois de o guerreiro já ter sido trazido para o jogador)
            for (ServerLevel level : server.getAllLevels()) {
                Entity existing = level.getEntity(uuid);
                if (existing instanceof WarriorCompanionEntity other && other != warrior && other.isAlive()) {
                    warrior.discard();
                    event.setCanceled(true);
                    return;
                }
            }

            if (warrior.isRecruited() && warrior.isAlive()) {
                warrior.updateSavedData();
            }
        }
    }

    /**
     * Resgata todas as companhias do jogador que estiverem no Modo 0 (Seguir).
     * Funciona tanto com entidades carregadas na memória quanto com entidades em chunks descarregados.
     */
    public static void rescueFollowingCompanions(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        CompanionSavedData data = CompanionSavedData.get(server);
        List<CompanionSavedData.CompanionInfo> companions = data.getPlayerCompanions(player.getUUID());
        if (companions.isEmpty()) return;

        ServerLevel targetLevel = player.serverLevel();
        int rescuedCount = 0;

        for (CompanionSavedData.CompanionInfo info : companions) {
            // Apenas modo 0 (Seguir)! Modo 1 (Guarda) e Modo 2 (Parado) permanecem onde estão!
            if (info.combatMode != 0) continue;

            WarriorCompanionEntity warrior = findLoadedCompanion(server, info.companionUUID);
            if (warrior != null) {
                // Caso 1: A entidade está atualmente carregada em algum chunk/dimensão
                // SÓ teletransporta se estiver em outra dimensão ou se a distância ultrapassar 13 blocos (> 169.0D)!
                if (warrior.level() == targetLevel && warrior.distanceToSqr(player) <= 169.0D) {
                    continue; // Está a 13 blocos ou menos: segue a pé normalmente!
                }

                warrior.stopRiding();

                if (warrior.level() != targetLevel) {
                    warrior.teleportTo(targetLevel, player.getX(), player.getY(), player.getZ(), null, warrior.getYRot(), warrior.getXRot());
                }

                warrior.safeTeleportTo(player);
                warrior.setTarget(null);
                warrior.getNavigation().stop();
                warrior.setDeltaMovement(0.0D, 0.0D, 0.0D);
                warrior.updateSavedData();

                targetLevel.sendParticles(ParticleTypes.PORTAL,
                        warrior.getX(), warrior.getY() + 0.5D, warrior.getZ(),
                        20, 0.4D, 0.6D, 0.4D, 0.1D);
                rescuedCount++;
            } else {
                // Caso 2: A entidade está em um chunk descarregado (ex: na natureza longe da colônia)!
                // Se a última posição conhecida na mesma dimensão for <= 13 blocos, não precisa materializar
                if (info.pos != null && targetLevel.dimension().location().toString().equals(info.dimension)) {
                    if (player.blockPosition().distSqr(info.pos) <= 169.0D) {
                        continue;
                    }
                }

                // Materializa a companhia diretamente no jogador a partir do banco de dados persistente!
                WarriorCompanionEntity restored = restoreCompanionAtPlayer(player, targetLevel, info);
                if (restored != null) {
                    rescuedCount++;
                }
            }
        }

        if (rescuedCount > 0) {
            targetLevel.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.0F);
        }
    }

    /**
     * Alias para compatibilidade com chamadas existentes.
     */
    public static void teleportFollowingCompanions(ServerPlayer player) {
        rescueFollowingCompanions(player);
    }

    /**
     * Materializa um companheiro que estava em chunk descarregado na posição do jogador,
     * restaurando todos os seus atributos, inventário, armas, vida e nível.
     */
    public static WarriorCompanionEntity restoreCompanionAtPlayer(ServerPlayer player, ServerLevel targetLevel, CompanionSavedData.CompanionInfo info) {
        if (info == null || info.companionUUID == null) return null;

        EntityType<? extends WarriorCompanionEntity> type = getEntityTypeForSpecialization(info.specialization);
        WarriorCompanionEntity companion = type.create(targetLevel);
        if (companion == null) return null;

        if (info.entityNbt != null && !info.entityNbt.isEmpty()) {
            try {
                companion.load(info.entityNbt);
            } catch (Exception e) {
                EpicVanguardMod.LOGGER.error("Falha ao carregar NBT do companheiro: " + info.companionUUID, e);
            }
        } else {
            // Fallback para dados legados sem snapshot de NBT
            companion.setWarriorName(info.name);
            companion.setWarriorLevel(info.level);
            companion.setSpecialization(info.specialization);
            companion.setHealth(Math.max(5.0F, info.health));
            int tier = (info.level >= 3) ? 2 : (info.level == 2 ? 1 : 0);
            companion.applyEquipmentTier(tier);
        }

        // Garante os identificadores persistentes e modo seguir
        companion.setUUID(info.companionUUID);
        companion.setOwnerUUID(player.getUUID());
        companion.setRecruited(true);
        companion.setCombatMode(0); // Modo 0 (Seguir)

        // Posiciona no jogador antes de adicionar ao mundo para registrar na section correta
        companion.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        companion.resetFallDistance();
        companion.fallDistance = 0.0F;
        companion.setDeltaMovement(0.0D, 0.0D, 0.0D);

        targetLevel.addFreshEntity(companion);
        companion.safeTeleportTo(player);
        companion.setTarget(null);
        companion.getNavigation().stop();
        companion.updateSavedData();

        targetLevel.sendParticles(ParticleTypes.PORTAL,
                companion.getX(), companion.getY() + 0.5D, companion.getZ(),
                20, 0.4D, 0.6D, 0.4D, 0.1D);

        return companion;
    }

    public static EntityType<? extends WarriorCompanionEntity> getEntityTypeForSpecialization(int specialization) {
        return switch (specialization) {
            case 1 -> ModEntityTypes.BERSERKER_COMPANION.get();
            case 2 -> ModEntityTypes.GUARDIAN_COMPANION.get();
            case 3 -> ModEntityTypes.DUELIST_COMPANION.get();
            default -> ModEntityTypes.WARRIOR_COMPANION.get();
        };
    }

    public static WarriorCompanionEntity findLoadedCompanion(MinecraftServer server, UUID uuid) {
        if (uuid == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e instanceof WarriorCompanionEntity warrior && warrior.isAlive()) {
                return warrior;
            }
        }
        return null;
    }

    public static WarriorCompanionEntity findOrLoadCompanion(MinecraftServer server, CompanionSavedData.CompanionInfo info) {
        if (info == null || info.companionUUID == null) return null;

        WarriorCompanionEntity live = findLoadedCompanion(server, info.companionUUID);
        if (live != null) return live;

        return null;
    }
}
