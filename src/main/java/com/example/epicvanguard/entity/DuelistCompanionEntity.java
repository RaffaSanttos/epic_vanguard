package com.example.epicvanguard.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class DuelistCompanionEntity extends WarriorCompanionEntity {
    public DuelistCompanionEntity(EntityType<? extends DuelistCompanionEntity> entityType, Level level) {
        super(entityType, level);
        this.setSpecialization(SPEC_DUELIST);
        this.recalculateAttributes();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setSpecialization(SPEC_DUELIST);
        this.recalculateAttributes();
    }

    @Override
    public int getSpecialization() {
        return SPEC_DUELIST;
    }
}
