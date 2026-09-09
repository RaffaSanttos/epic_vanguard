package com.example.epicvanguard.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class BerserkerCompanionEntity extends WarriorCompanionEntity {
    public BerserkerCompanionEntity(EntityType<? extends BerserkerCompanionEntity> entityType, Level level) {
        super(entityType, level);
        this.setSpecialization(SPEC_BERSERKER);
        this.recalculateAttributes();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setSpecialization(SPEC_BERSERKER);
        this.recalculateAttributes();
    }

    @Override
    public int getSpecialization() {
        return SPEC_BERSERKER;
    }
}
