package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public class WarriorStrollGoal extends WaterAvoidingRandomStrollGoal {
    private final WarriorCompanionEntity warrior;

    public WarriorStrollGoal(WarriorCompanionEntity warrior, double speed) {
        super(warrior, speed);
        this.warrior = warrior;
    }

    @Override
    public boolean canUse() {
        if (warrior.isRecruited() && (warrior.getCombatMode() == 2 || warrior.getCombatMode() == 1)) {
            return false;
        }
        if (warrior.isTalking() || warrior.isDuelMode() || warrior.isInventoryOpen()) return false;
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (warrior.isRecruited() && (warrior.getCombatMode() == 2 || warrior.getCombatMode() == 1)) {
            return false;
        }
        return super.canContinueToUse();
    }
}
