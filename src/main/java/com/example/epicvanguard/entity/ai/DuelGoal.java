package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class DuelGoal extends MeleeAttackGoal {
    private final WarriorCompanionEntity warrior;

    public DuelGoal(WarriorCompanionEntity warrior, double speed) {
        super(warrior, speed, true);
        this.warrior = warrior;
    }

    @Override
    public boolean canUse() {
        return warrior.isDuelMode() && warrior.getDuelTarget() != null && warrior.getDuelTarget().isAlive();
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
        double attackReach = this.getAttackReachSqr(pEnemy);
        if (pDistToEnemySqr <= attackReach && this.isTimeToAttack()) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);

            if (pEnemy.getHealth() <= 4.0F) {
                pEnemy.hurt(warrior.damageSources().mobAttack(warrior), 1.0F);
            } else {
                this.mob.doHurtTarget(pEnemy);
            }
        }
    }
}
