package io.github.raffasanttos.epicvanguard.entity.util;

import io.github.raffasanttos.epicvanguard.entity.CompanionSavedData;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.event.CompanionTeleportHandler;
import io.github.raffasanttos.epicvanguard.init.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;

/**
 * Classe do guerreiro.
 * <p>
 * Todo guerreiro já nasce com uma classe (Berserker, Guardião, Duelista, Arqueiro ou Porta-estandarte). A classe é
 * o próprio tipo de entidade, porque o Epic Fight registra atributos por tipo. Um Guerreiro base (ovo antigo,
 * /summon ou save antigo) é recriado no mesmo lugar como uma classe sorteada, com o mesmo UUID e NBT (inventário,
 * nível, XP, nome, personalidade) e uma geração nova no registro, para que nenhuma cópia antiga volte.
 */
public final class WarriorProgression {

    private WarriorProgression() {}

    /** Guerreiro base, sem classe, que deve virar uma classe sorteada. Guerreiros sem IA (testes) ficam como estão. */
    public static boolean needsClass(WarriorCompanionEntity w) {
        return w.getType() == ModEntityTypes.WARRIOR_COMPANION.get() && !w.isNoAi();
    }

    /** Troca um Guerreiro base por uma classe sorteada. Só no servidor. Devolve a entidade nova, ou {@code null}. */
    @Nullable
    public static WarriorCompanionEntity assignRandomClass(WarriorCompanionEntity w) {
        int spec = WarriorCompanionEntity.SPEC_BERSERKER + w.getRandom().nextInt(WarriorCompanionEntity.SPEC_MAX);
        return convertToClass(w, spec);
    }

    /**
     * Recria o guerreiro como a classe pedida. Só no servidor. Devolve a entidade nova, ou {@code null} se não pôde.
     * A entidade antiga é removida e não deve mais ser usada.
     */
    @Nullable
    public static WarriorCompanionEntity convertToClass(WarriorCompanionEntity w, int spec) {
        if (!(w.level() instanceof ServerLevel level) || w.isRemoved()) return null;
        if (spec < WarriorCompanionEntity.SPEC_BERSERKER || spec > WarriorCompanionEntity.SPEC_MAX) return null;

        EntityType<? extends WarriorCompanionEntity> type = CompanionTeleportHandler.getEntityTypeForSpecialization(spec);
        WarriorCompanionEntity promoted = type.create(level);
        if (promoted == null) return null;

        float healthPct = w.getHealth() / Math.max(1.0F, w.getMaxHealth());
        CompoundTag tag = new CompoundTag();
        w.saveWithoutId(tag);
        tag.putInt("Specialization", spec);
        promoted.load(tag); // mesmo UUID, inventário, nível, nome; o construtor da subclasse fixa a classe
        promoted.moveTo(w.getX(), w.getY(), w.getZ(), w.getYRot(), w.getXRot());
        promoted.setYHeadRot(w.getYHeadRot());
        // Quem ainda não foi contratado recebe o equipamento da classe (arco do Arqueiro, estandarte...)
        if (!promoted.isRecruited() && !promoted.isPrisoner()) {
            promoted.applySpecializationLoadout(Math.max(0, promoted.getEquipmentTier()));
        }
        promoted.recalculateAttributes();
        promoted.setHealth(Math.max(1.0F, promoted.getMaxHealth() * healthPct));

        // A nova é a cópia oficial: a antiga vira "stale" e qualquer snapshot dela é ignorado
        if (level.getServer() != null && w.isRecruited()) {
            CompanionSavedData data = CompanionSavedData.get(level.getServer());
            CompanionSavedData.CompanionInfo info = data.getCompanion(w.getUUID());
            if (info != null) {
                promoted.setRestoreGeneration(data.bumpGeneration(info));
            }
        }

        w.discard();
        level.addFreshEntity(promoted);
        promoted.syncEquipmentWithInventory();
        if (promoted.isRecruited()) {
            promoted.updateSavedData();
        }
        return promoted;
    }
}
