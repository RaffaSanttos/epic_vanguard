package io.github.raffasanttos.epicvanguard.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Porta-estandarte: com estandarte na mão secundária dá Resistência I aos aliados próximos; grito "Reagrupar". */
public class StandardBearerCompanionEntity extends WarriorCompanionEntity {
    public StandardBearerCompanionEntity(EntityType<? extends StandardBearerCompanionEntity> entityType, Level level) {
        super(entityType, level);
        this.setSpecialization(SPEC_STANDARD_BEARER);
        this.recalculateAttributes();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setSpecialization(SPEC_STANDARD_BEARER);
        this.recalculateAttributes();
    }

    @Override
    public int getSpecialization() {
        return SPEC_STANDARD_BEARER;
    }
}
