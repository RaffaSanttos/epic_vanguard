package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;

import java.util.EnumSet;

/**
 * Active Shield Defense Goal:
 * Intelligently raises and maintains shield block when:
 * 1. Projectiles (arrows, tridents, fireballs) are incoming within 12 blocks.
 * 2. Facing ranged enemies (Skeletons, Pillagers) that are aiming or in line of sight.
 * 3. As Guardian in frontline posture, absorbing projectile bursts for the team.
 */
public class ActiveShieldDefenseGoal extends Goal {

    private final WarriorCompanionEntity warrior;
    private int guardTicks = 0;
    private Entity threatEntity = null;

    public ActiveShieldDefenseGoal(WarriorCompanionEntity warrior) {
        this.warrior = warrior;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!warrior.hasShield()) return false;
        if (warrior.isTalking() || warrior.isInventoryOpen() || warrior.isDuelMode() ||
            warrior.isEmergencyRetreating() || warrior.isPrisoner()) {
            return false;
        }

        // Gatilho 1: Projétil vindo em rota de colisão
        if (warrior.isThreatenedByProjectiles(12.0D)) {
            threatEntity = warrior.getIncomingProjectileThreat(12.0D);
            return true;
        }

        // Gatilho 2: Alvo é monstro de ataque à distância mirando
        LivingEntity target = warrior.getTarget();
        if (target != null && target.isAlive() && target instanceof RangedAttackMob) {
            double distSq = warrior.distanceToSqr(target);
            if (distSq <= 196.0D && warrior.hasLineOfSight(target)) { // dentro de 14 blocos
                threatEntity = target;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (!warrior.hasShield()) return false;
        if (warrior.isEmergencyRetreating() || warrior.isPrisoner()) return false;
        return guardTicks > 0 || warrior.isThreatenedByProjectiles(8.0D);
    }

    @Override
    public void start() {
        this.guardTicks = 35; // ~1.75 segundos de guarda firme
        warrior.raiseShield();
    }

    @Override
    public void stop() {
        warrior.lowerShield();
        this.threatEntity = null;
        this.guardTicks = 0;
    }

    @Override
    public void tick() {
        if (guardTicks > 0) {
            guardTicks--;
        }

        // Atualiza a ameaça prioritária (projétil ou atirador)
        Entity currentThreat = warrior.getIncomingProjectileThreat(10.0D);
        if (currentThreat != null) {
            threatEntity = currentThreat;
        }

        if (threatEntity != null && threatEntity.isAlive()) {
            warrior.getLookControl().setLookAt(threatEntity.getX(), threatEntity.getEyeY(), threatEntity.getZ(), 45.0F, 45.0F);

            // Se a ameaça for um atirador (esqueleto/pillager), avança em marcha firme e protegida
            if (threatEntity instanceof LivingEntity livingThreat && livingThreat instanceof RangedAttackMob) {
                warrior.getNavigation().moveTo(livingThreat, 0.85D);
            }
        } else {
            LivingEntity target = warrior.getTarget();
            if (target != null && target.isAlive()) {
                warrior.getLookControl().setLookAt(target, 35.0F, 35.0F);
            }
        }

        // Garante que o escudo continue erguido
        if (!warrior.isActivelyBlocking()) {
            warrior.raiseShield();
        }
    }
}