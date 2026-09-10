package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class FollowOwnerGoal extends Goal {
    private final WarriorCompanionEntity warrior;
    private final double speedModifier;
    private final float stopDist;
    private final float startDist;
    private int timeToRecalcPath;

    public FollowOwnerGoal(WarriorCompanionEntity warrior, double speedModifier, float startDist, float stopDist) {
        this.warrior = warrior;
        this.speedModifier = speedModifier;
        this.startDist = startDist;
        this.stopDist = stopDist;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (warrior.isTalking()) return false;
        Player owner = warrior.getOwner();
        if (owner == null || owner.isSpectator() || !warrior.isRecruited()) return false;
        if (warrior.getCombatMode() != 0) return false; // Apenas modo 0 (Seguir)
        if (warrior.getTarget() != null && warrior.getTarget().isAlive()) return false;
        return warrior.distanceToSqr(owner) > (double) (startDist * startDist);
    }

    @Override
    public boolean canContinueToUse() {
        if (warrior.isTalking()) return false;
        Player owner = warrior.getOwner();
        if (owner == null || !warrior.isRecruited()) return false;
        if (warrior.getCombatMode() != 0) return false;
        if (warrior.getTarget() != null && warrior.getTarget().isAlive()) return false;
        return warrior.distanceToSqr(owner) > (double) (stopDist * stopDist);
    }

    @Override
    public void tick() {
        Player owner = warrior.getOwner();
        if (owner == null || warrior.getCombatMode() != 0) return;

        warrior.getLookControl().setLookAt(owner, 10.0F, (float) warrior.getMaxHeadXRot());

        // Mudança de dimensão
        if (owner.level() != warrior.level() && owner.level() instanceof ServerLevel targetLevel) {
            if (owner.onGround() && owner.getY() >= owner.level().getMinBuildHeight()) {
                warrior.teleportTo(targetLevel, owner.getX(), owner.getY(), owner.getZ(), null, warrior.getYRot(), warrior.getXRot());
                warrior.safeTeleportTo(owner);
            }
            return;
        }

        if (!owner.onGround() || owner.isFallFlying() || owner.getAbilities().flying) {
            return;
        }

        double distSq = warrior.distanceToSqr(owner);
        if (distSq >= 225.0D) { // >= 15 blocks
            warrior.safeTeleportTo(owner);
            return;
        }

        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10;
            if (!warrior.getNavigation().moveTo(owner, speedModifier)) {
                if (distSq > 64.0D) {
                    warrior.safeTeleportTo(owner);
                }
            }
        }
    }
}
