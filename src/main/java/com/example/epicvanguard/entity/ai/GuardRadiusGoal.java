package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class GuardRadiusGoal extends Goal {
    private final WarriorCompanionEntity warrior;
    private final double speedModifier;

    public GuardRadiusGoal(WarriorCompanionEntity warrior, double speedModifier) {
        this.warrior = warrior;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (warrior.isTalking()) return false;
        return warrior.isRecruited() && warrior.getCombatMode() == 1 && warrior.getGuardPos() != null;
    }

    @Override
    public void tick() {
        BlockPos gPos = warrior.getGuardPos();
        if (gPos == null) return;

        double distSq = warrior.distanceToSqr(gPos.getX() + 0.5D, gPos.getY(), gPos.getZ() + 0.5D);
        if (distSq > 36.0D) { // > 6 blocks from guard point
            warrior.getNavigation().moveTo(gPos.getX() + 0.5D, gPos.getY(), gPos.getZ() + 0.5D, speedModifier);
        }
    }
}
