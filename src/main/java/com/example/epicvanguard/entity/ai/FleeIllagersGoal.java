package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.AbstractIllager;

public class FleeIllagersGoal extends AvoidEntityGoal<AbstractIllager> {
    private final WarriorCompanionEntity warrior;

    public FleeIllagersGoal(WarriorCompanionEntity warrior, float maxDist, double walkSpeed, double sprintSpeed) {
        super(warrior, AbstractIllager.class, maxDist, walkSpeed, sprintSpeed);
        this.warrior = warrior;
    }

    @Override
    public boolean canUse() {
        return warrior.isPrisoner() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return warrior.isPrisoner() && super.canContinueToUse();
    }
}
