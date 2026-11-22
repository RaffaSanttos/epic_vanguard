package com.example.epicvanguard.entity.util;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Teleporte seguro do guerreiro (chão firme, sem lava nem sufocamento) e a regra de quando seguir o dono teleportando. */
public final class WarriorTeleport {

    private WarriorTeleport() {}

    /** A partir desta distância (13 blocos) o guerreiro no modo Seguir pode se teleportar até o dono. */
    public static final double FOLLOW_TELEPORT_DISTANCE = 13.0D;
    /** Em combate ele só é puxado se o dono passar desta distância (32 blocos). */
    public static final double COMBAT_LEASH_DISTANCE = 32.0D;
    /** Tempo seguido longe do dono, fora de combate, antes de teleportar (2 s), como o lobo vanilla. */
    public static final int FOLLOW_TELEPORT_DELAY_TICKS = 40;

    /**
     * Chamado a cada tick no servidor. No modo Seguir, decide se o guerreiro deve se teleportar até o dono:
     * <ul>
     *   <li>dono a mais de 32 blocos: teleporta na hora, mesmo lutando;</li>
     *   <li>dono entre 13 e 32 blocos: teleporta só fora de combate e depois de 2 s seguidos longe;</li>
     *   <li>lutando com o dono a menos de 32 blocos: continua a luta (antes ele largava o alvo no meio).</li>
     * </ul>
     * Entre dimensões e chunks descarregados quem resgata é o {@code CompanionTeleportHandler}.
     */
    public static void tickFollowTeleport(WarriorCompanionEntity w) {
        if (!w.isRecruited() || w.getCombatMode() != WarriorCompanionEntity.MODE_FOLLOW) {
            w.setFarFromOwnerTicks(0);
            return;
        }
        Player owner = w.getOwner();
        if (owner == null || owner.isSpectator() || owner.level() != w.level()) {
            w.setFarFromOwnerTicks(0);
            return;
        }
        double distSq = w.distanceToSqr(owner);
        boolean teleport = false;
        if (distSq >= COMBAT_LEASH_DISTANCE * COMBAT_LEASH_DISTANCE) {
            teleport = true;
        } else if (distSq >= FOLLOW_TELEPORT_DISTANCE * FOLLOW_TELEPORT_DISTANCE && !w.isFighting()) {
            w.setFarFromOwnerTicks(w.getFarFromOwnerTicks() + 1);
            teleport = w.getFarFromOwnerTicks() >= FOLLOW_TELEPORT_DELAY_TICKS;
        } else {
            w.setFarFromOwnerTicks(0);
        }
        if (teleport) {
            w.setFarFromOwnerTicks(0);
            w.setTarget(null);
            safeTeleportTo(w, owner);
        }
    }

    /**
     * Teleporta com segurança para perto do alvo (inclusive entre dimensões).
     * @return a entidade que está agora ao lado do alvo (é uma nova se mudou de dimensão)
     */
    public static WarriorCompanionEntity safeTeleportTo(WarriorCompanionEntity w, Entity target) {
        if (target == null) return w;

        // Outra dimensão: transfere e continua o teleporte seguro na entidade nova
        if (w.level() != target.level() && target.level() instanceof ServerLevel targetLevel) {
            WarriorCompanionEntity moved = w.moveToLevel(targetLevel, target.getX(), target.getY(), target.getZ());
            return moved == w ? w : safeTeleportTo(moved, target);
        }

        // 1. Proteção contra o Void: se o alvo estiver no void ou abaixo da altura mínima, NÃO teletransporta
        if (target.getY() < target.level().getMinBuildHeight()) {
            return w;
        }

        // 2. Proteção contra Voo / Elytra:
        // Se o jogador estiver voando ativamente ou planando com Elytra, aguarda
        if (target instanceof Player player) {
            if (player.isFallFlying() || player.getAbilities().flying) {
                return w;
            }
        }

        BlockPos targetPos = target.blockPosition();

        // 3. Procura blocos sólidos seguros ao redor do alvo (raio horizontal de 3 blocos, dy entre +2 e -2)
        for (int dy = 2; dy >= -2; dy--) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue;
                    BlockPos candidate = targetPos.offset(dx, dy, dz);
                    if (isSafeTeleportBlock(w, candidate)) {
                        doTeleport(w, candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                        return w;
                    }
                }
            }
        }

        // 4. Fallback: se o bloco onde o alvo está pisando for seguro
        if (isSafeTeleportBlock(w, targetPos)) {
            doTeleport(w, target.getX(), target.getY(), target.getZ());
            return w;
        }

        // 5. Fallback secundário: se o jogador estiver em bloco especial (ex: Waystone, slab),
        // desde que não esteja no void/lava/fogo, teletransporta diretamente para a posição do jogador
        if (target.getY() >= target.level().getMinBuildHeight() && !target.isInLava()) {
            doTeleport(w, target.getX(), target.getY(), target.getZ());
        }
        return w;
    }

    public static void safeTeleportTo(WarriorCompanionEntity w, BlockPos targetPos) {
        if (targetPos == null) return;
        if (targetPos.getY() < w.level().getMinBuildHeight()) return;

        for (int dy = 2; dy >= -2; dy--) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos candidate = targetPos.offset(dx, dy, dz);
                    if (isSafeTeleportBlock(w, candidate)) {
                        doTeleport(w, candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                        return;
                    }
                }
            }
        }
        if (isSafeTeleportBlock(w, targetPos)) {
            doTeleport(w, targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D);
        }
    }

    public static boolean isSafeTeleportBlock(WarriorCompanionEntity w, BlockPos pos) {
        if (pos.getY() < w.level().getMinBuildHeight()) return false;

        BlockState below = w.level().getBlockState(pos.below());
        BlockState at = w.level().getBlockState(pos);
        BlockState above = w.level().getBlockState(pos.above());

        // O chão abaixo deve ser sólido ou ter colisão resistente para pisar (suporta blocos de mods, waystones, slabs, etc.)
        if (!below.isSolid() && !below.isFaceSturdy(w.level(), pos.below(), net.minecraft.core.Direction.UP) && below.getCollisionShape(w.level(), pos.below()).isEmpty()) {
            return false;
        }

        if (below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.CACTUS) || below.is(Blocks.FIRE) || below.isAir()) {
            return false;
        }

        // Os blocos onde o corpo/cabeça vão ficar não podem ser sufocantes nem perigosos
        if (at.isSuffocating(w.level(), pos) || above.isSuffocating(w.level(), pos.above())) {
            return false;
        }

        if (at.is(Blocks.LAVA) || at.is(Blocks.FIRE) || above.is(Blocks.LAVA) || above.is(Blocks.FIRE)) {
            return false;
        }

        return true;
    }

    private static void doTeleport(WarriorCompanionEntity w, double x, double y, double z) {
        w.teleportTo(x, y, z);
        w.resetFallDistance();
        w.fallDistance = 0.0F; // Reseta inércia e distância de queda acumulada antes do teleporte
        w.setDeltaMovement(Vec3.ZERO);
        w.hasImpulse = true;
        w.getNavigation().stop();
        if (w.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, w.getX(), w.getY() + 0.5D, w.getZ(),
                    15, 0.3D, 0.5D, 0.3D, 0.1D);
        }
    }
}
