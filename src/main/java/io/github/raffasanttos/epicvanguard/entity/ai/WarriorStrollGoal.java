package io.github.raffasanttos.epicvanguard.entity.ai;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
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
        // Seguindo em formação: fica no lugar em vez de passear
        if (warrior.isRecruited() && warrior.getCombatMode() == 0
                && io.github.raffasanttos.epicvanguard.entity.util.WarriorFormation.getFormation(warrior)
                   != io.github.raffasanttos.epicvanguard.entity.util.WarriorFormation.FREE) {
            return false;
        }
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
