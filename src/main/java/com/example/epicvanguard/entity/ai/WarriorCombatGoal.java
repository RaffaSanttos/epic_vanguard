package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;

public class WarriorCombatGoal extends MeleeAttackGoal {
    private final WarriorCompanionEntity warrior;
    private int comboCount = 0;
    private int maxCombo = 3;

    public WarriorCombatGoal(WarriorCompanionEntity pMob, double pSpeedModifier, boolean pFollowingTargetEvenIfNotSeen) {
        super(pMob, pSpeedModifier, pFollowingTargetEvenIfNotSeen);
        this.warrior = pMob;
    }

    @Override
    public boolean canUse() {
        if (warrior.isTalking() || warrior.isInventoryOpen() || warrior.isInStaminaRegen() || warrior.isDuelMode() || warrior.isEmergencyRetreating() || warrior.isLowHealth()) return false;
        if (warrior.isPrisoner()) return false;

        if (!warrior.isRecruited()) {
            if (warrior.getLastHurtByMob() == null && warrior.getTarget() == null) {
                return false;
            }
        } else {
            if (warrior.getCombatMode() == 2) { // 2 = Parado
                warrior.clearCombatTarget();
                return false;
            }
            if (warrior.getCombatMode() == 1 && warrior.getGuardPos() != null) { // 1 = Guarda
                if (warrior.getTarget() != null) {
                    double distToGuard = warrior.getTarget().distanceToSqr(
                            warrior.getGuardPos().getX() + 0.5D,
                            warrior.getGuardPos().getY(),
                            warrior.getGuardPos().getZ() + 0.5D
                    );
                    if (distToGuard > 64.0D) { // > 8 blocos do ponto de guarda
                        warrior.clearCombatTarget();
                        return false;
                    }
                }
            }
            if (warrior.getTarget() != null && warrior.getTarget() == warrior.getOwner()) {
                warrior.setTarget(null);
                return false;
            }
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (warrior.isEmergencyRetreating() || warrior.isLowHealth()) return false;
        if (warrior.isPrisoner()) return false;
        if (warrior.isRecruited()) {
            if (warrior.getCombatMode() == 2) {
                warrior.clearCombatTarget();
                return false;
            }
            if (warrior.getCombatMode() == 1 && warrior.getGuardPos() != null) {
                if (warrior.getTarget() != null) {
                    double distToGuard = warrior.getTarget().distanceToSqr(
                            warrior.getGuardPos().getX() + 0.5D,
                            warrior.getGuardPos().getY(),
                            warrior.getGuardPos().getZ() + 0.5D
                    );
                    if (distToGuard > 64.0D) {
                        warrior.clearCombatTarget();
                        return false;
                    }
                }
            }
        }
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        comboCount = 0;
        maxCombo = 2 + warrior.getRandom().nextInt(3); // 2 to 4 hits
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
        if (warrior.isRecruited() && pEnemy == warrior.getOwner()) {
            warrior.setTarget(null);
            return;
        }
        double attackReach = this.getAttackReachSqr(pEnemy);
        if (pDistToEnemySqr <= attackReach && this.isTimeToAttack()) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(pEnemy);
            comboCount++;

            if (comboCount >= maxCombo) {
                warrior.setInStaminaRegen(true);
                warrior.setStaminaRegenCooldown(40); // 2 seconds

                Vec3 dir = warrior.position().subtract(pEnemy.position()).normalize().scale(0.6D);
                warrior.setDeltaMovement(dir.x, 0.25D, dir.z);
                comboCount = 0;
            }
        }
    }
}
