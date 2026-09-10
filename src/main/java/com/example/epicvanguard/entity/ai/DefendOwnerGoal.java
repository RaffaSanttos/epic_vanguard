package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class DefendOwnerGoal extends TargetGoal {
    private final WarriorCompanionEntity warrior;
    private LivingEntity ownerLastHurtBy;
    private int timestamp;

    public DefendOwnerGoal(WarriorCompanionEntity warrior) {
        super(warrior, false);
        this.warrior = warrior;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!warrior.isRecruited() || warrior.getCombatMode() == 2 || warrior.isEmergencyRetreating() || warrior.isLowHealth()) return false;
        Player owner = warrior.getOwner();
        if (owner == null) return false;

        this.ownerLastHurtBy = owner.getLastHurtByMob();
        int i = owner.getLastHurtByMobTimestamp();
        return i != this.timestamp && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT) &&
                this.ownerLastHurtBy != warrior && this.ownerLastHurtBy != owner;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.ownerLastHurtBy);
        Player owner = warrior.getOwner();
        if (owner != null) {
            this.timestamp = owner.getLastHurtByMobTimestamp();
        }

        // Guardian Passive Taunt: 70% de chance de forçar o monstro a focar no Guardião
        if (warrior.getSpecialization() == WarriorCompanionEntity.SPEC_GUARDIAN && this.ownerLastHurtBy instanceof Mob attackingMob) {
            if (warrior.getRandom().nextFloat() < 0.70F) {
                attackingMob.setTarget(warrior);
                if (warrior.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, warrior.getX(), warrior.getY() + 1.2D, warrior.getZ(),
                            6, 0.3D, 0.3D, 0.3D, 0.05D);
                }
            }
        }
        super.start();
    }
}
