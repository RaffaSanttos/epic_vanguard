package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;

public class AggressiveTargetGoal extends NearestAttackableTargetGoal<Mob> {
    private final WarriorCompanionEntity warrior;

    public AggressiveTargetGoal(WarriorCompanionEntity warrior) {
        super(warrior, Mob.class, 10, true, false,
                entity -> entity instanceof Enemy && !(entity instanceof WarriorCompanionEntity));
        this.warrior = warrior;
    }

    @Override
    public boolean canUse() {
        if (!warrior.isRecruited() || !warrior.isTargetHostiles() || warrior.getCombatMode() == 2 || warrior.isEmergencyRetreating() || warrior.isLowHealth()) return false;
        return super.canUse();
    }
}
