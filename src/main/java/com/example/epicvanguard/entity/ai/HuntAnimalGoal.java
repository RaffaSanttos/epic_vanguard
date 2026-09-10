package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;

public class HuntAnimalGoal extends NearestAttackableTargetGoal<Animal> {
    private final WarriorCompanionEntity warrior;

    public HuntAnimalGoal(WarriorCompanionEntity warrior) {
        super(warrior, Animal.class, 10, true, false,
                animal -> animal != null && !animal.isBaby() &&
                        !(animal instanceof TamableAnimal tamable && tamable.isTame()) &&
                        (animal instanceof net.minecraft.world.entity.animal.Cow ||
                         animal instanceof net.minecraft.world.entity.animal.Pig ||
                         animal instanceof net.minecraft.world.entity.animal.Sheep ||
                         animal instanceof net.minecraft.world.entity.animal.Chicken ||
                         animal instanceof net.minecraft.world.entity.animal.Rabbit ||
                         animal instanceof net.minecraft.world.entity.animal.goat.Goat));
        this.warrior = warrior;
    }

    @Override
    public boolean canUse() {
        if (!warrior.isRecruited() || !warrior.isTargetPassives() || warrior.getCombatMode() == 2 || warrior.isEmergencyRetreating() || warrior.isLowHealth()) return false;
        return super.canUse();
    }
}
