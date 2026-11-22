package com.example.epicvanguard.entity.util;

import com.example.epicvanguard.entity.CompanionSavedData;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.event.CompanionTeleportHandler;
import com.example.epicvanguard.init.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;

/**
 * Progressão do guerreiro: escolha de especialização.
 * <p>
 * A classe (Berserker, Guardião, Duelista) é o próprio tipo de entidade, porque o Epic Fight registra atributos
 * por tipo. Por isso trocar de classe recria o guerreiro no mesmo lugar, com o mesmo UUID, NBT (inventário,
 * nível, XP, nome, personalidade) e uma geração nova no registro, para que nenhuma cópia antiga volte.
 */
public final class WarriorProgression {

    /** Nível a partir do qual um Guerreiro base pode escolher a classe. */
    public static final int SPEC_CHOICE_LEVEL = 5;

    private WarriorProgression() {}

    /** Guerreiro base (sem classe), recrutado e no nível mínimo: pode escolher a especialização. */
    public static boolean canChooseSpecialization(WarriorCompanionEntity w) {
        return w.getType() == ModEntityTypes.WARRIOR_COMPANION.get()
                && w.getSpecialization() == WarriorCompanionEntity.SPEC_NONE
                && w.isRecruited()
                && w.getWarriorLevel() >= SPEC_CHOICE_LEVEL;
    }

    /**
     * Troca a classe do guerreiro. Só no servidor. Devolve a entidade nova, ou {@code null} se não pôde trocar.
     * A entidade antiga é removida e não deve mais ser usada.
     */
    @Nullable
    public static WarriorCompanionEntity chooseSpecialization(WarriorCompanionEntity w, ServerPlayer owner, int spec) {
        if (!(w.level() instanceof ServerLevel level) || owner.getServer() == null) return null;
        if (!canChooseSpecialization(w) || !w.isOwner(owner)) return null;
        if (spec != WarriorCompanionEntity.SPEC_BERSERKER && spec != WarriorCompanionEntity.SPEC_GUARDIAN
                && spec != WarriorCompanionEntity.SPEC_DUELIST) return null;

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
        promoted.setHealth(Math.max(1.0F, promoted.getMaxHealth() * healthPct));

        // A nova é a cópia oficial: a antiga vira "stale" e qualquer snapshot dela é ignorado
        CompanionSavedData data = CompanionSavedData.get(owner.getServer());
        CompanionSavedData.CompanionInfo info = data.getCompanion(w.getUUID());
        if (info != null) {
            promoted.setRestoreGeneration(data.bumpGeneration(info));
        }

        w.discard();
        level.addFreshEntity(promoted);
        promoted.syncEquipmentWithInventory();
        promoted.updateSavedData();

        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, promoted.getX(), promoted.getY() + 1.0D, promoted.getZ(),
                40, 0.5D, 0.8D, 0.5D, 0.25D);
        level.playSound(null, promoted.getX(), promoted.getY(), promoted.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2F, 0.8F);
        owner.sendSystemMessage(Component.translatable("epicvanguard.text.specialization.chosen",
                promoted.getWarriorName(), promoted.getFormattedSpecializationTitle()));
        return promoted;
    }
}
