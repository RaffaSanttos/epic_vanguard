package io.github.raffasanttos.epicvanguard.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Arqueiro: arco/besta, prefere 8 a 18 blocos, puxa e acerta mais forte; grito "Chuva de Flechas". */
public class ArcherCompanionEntity extends WarriorCompanionEntity {
    public ArcherCompanionEntity(EntityType<? extends ArcherCompanionEntity> entityType, Level level) {
        super(entityType, level);
        this.setSpecialization(SPEC_ARCHER);
        this.recalculateAttributes();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setSpecialization(SPEC_ARCHER);
        this.recalculateAttributes();
    }

    @Override
    public int getSpecialization() {
        return SPEC_ARCHER;
    }
}
