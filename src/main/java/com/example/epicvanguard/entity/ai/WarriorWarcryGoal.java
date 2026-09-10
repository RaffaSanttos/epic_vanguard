package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

public class WarriorWarcryGoal extends Goal {
    private final WarriorCompanionEntity warrior;
    private int roarTicks = 0;

    public WarriorWarcryGoal(WarriorCompanionEntity warrior) {
        this.warrior = warrior;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (warrior.isTalking() || warrior.isInventoryOpen() || !warrior.isAlive() || warrior.isEmergencyRetreating()) return false;
        if (warrior.getWarriorLevel() < 5) return false;
        if (warrior.getWarcryCooldown() > 0) return false;

        LivingEntity target = warrior.getTarget();
        boolean inCombat = (target != null && target.isAlive()) || warrior.getLastHurtByMob() != null;
        Player owner = warrior.getOwner();
        if (!inCombat && owner != null) {
            inCombat = (owner.getLastHurtByMob() != null && owner.distanceToSqr(warrior) < 256.0D);
        }
        if (!inCombat) return false;

        // Gatilho 1: Vida do jogador < 40%
        if (owner != null && owner.getHealth() < (owner.getMaxHealth() * 0.40F)) {
            return true;
        }
        // Gatilho 2: Vida do guerreiro < 40%
        if (warrior.getHealth() < (warrior.getMaxHealth() * 0.40F)) {
            return true;
        }
        // Gatilho 3: Aglomeração de inimigos (>= 2 monstros em 10 blocos)
        List<Monster> monsters = warrior.level().getEntitiesOfClass(Monster.class, warrior.getBoundingBox().inflate(10.0D), LivingEntity::isAlive);
        return monsters.size() >= 2;
    }

    @Override
    public void start() {
        this.roarTicks = 20;
        warrior.getNavigation().stop();
        warrior.triggerWarcry();
    }

    @Override
    public boolean canContinueToUse() {
        return this.roarTicks > 0;
    }

    @Override
    public void tick() {
        if (this.roarTicks > 0) {
            this.roarTicks--;
            warrior.getNavigation().stop();
        }
    }
}
