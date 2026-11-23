package io.github.raffasanttos.epicvanguard.entity.util;

import io.github.raffasanttos.epicvanguard.config.EpicVanguardConfig;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.UUID;

/**
 * Montarias: guerreiros no modo Seguir montam cavalos (ou burros/mulas) domados e selados
 * do próprio dono quando o dono está montado, e desmontam para lutar corpo a corpo.
 * O Arqueiro continua montado e atira de cima do cavalo.
 */
public final class WarriorMountHelper {

    private WarriorMountHelper() {}

    /** Cavalo domado e selado, do mesmo dono do guerreiro, sem ninguém em cima. */
    public static boolean isSuitableMount(WarriorCompanionEntity warrior, AbstractHorse horse) {
        if (!horse.isAlive() || !horse.isTamed() || !horse.isSaddled() || horse.isVehicle() || horse.isBaby()) return false;
        UUID owner = warrior.getOwnerUUID().orElse(null);
        return owner != null && owner.equals(horse.getOwnerUUID());
    }

    /** O guerreiro deveria estar montado agora? */
    public static boolean shouldRide(WarriorCompanionEntity warrior) {
        if (!EpicVanguardConfig.mounts() || !warrior.isRecruited() || warrior.isPrisoner() || warrior.getCombatMode() != 0) return false;
        if (warrior.isEmergencyRetreating() || warrior.isDuelMode()) return false;
        Player owner = warrior.getOwner();
        if (owner == null || owner.level() != warrior.level() || !(owner.getVehicle() instanceof AbstractHorse)) return false;
        boolean fighting = warrior.getTarget() != null && warrior.getTarget().isAlive();
        // Em combate só fica montado quem luta à distância
        return !fighting || WarriorRangedHelper.canFightAtRange(warrior);
    }

    /** Montaria preferida (a última usada) ou o cavalo adequado mais próximo. */
    @Nullable
    public static AbstractHorse findMount(WarriorCompanionEntity warrior) {
        double radius = EpicVanguardConfig.mountSearchRadius();
        UUID preferred = warrior.getMountUUID();
        if (preferred != null && warrior.level() instanceof ServerLevel sl) {
            Entity e = sl.getEntity(preferred);
            if (e instanceof AbstractHorse h && isSuitableMount(warrior, h) && h.distanceToSqr(warrior) <= radius * radius) {
                return h;
            }
        }
        return warrior.level().getEntitiesOfClass(AbstractHorse.class, warrior.getBoundingBox().inflate(radius),
                        h -> isSuitableMount(warrior, h))
                .stream()
                .min(Comparator.comparingDouble(h -> h.distanceToSqr(warrior)))
                .orElse(null);
    }

    /**
     * Pede para montar. A montada acontece no próximo tick da entidade, fora dos goals: ao montar/desmontar o
     * Epic Fight refaz os goals do mob, e fazer isso de dentro de um goal derruba o jogo
     * (ConcurrentModificationException no GoalSelector).
     */
    public static void mount(WarriorCompanionEntity warrior, AbstractHorse horse) {
        warrior.getNavigation().stop();
        warrior.setPendingMount(horse);
    }

    /** Executa a montada pedida por {@link #mount}. Chamado no tick da entidade, antes da IA. */
    public static void applyPendingMount(WarriorCompanionEntity warrior, AbstractHorse horse) {
        if (warrior.isPassenger() || !isSuitableMount(warrior, horse) || !shouldRide(warrior)
                || horse.distanceToSqr(warrior) > 4.0D * 4.0D) return;
        ride(warrior, horse);
    }

    /** Monta na hora. Não chamar de dentro de um goal (ver {@link #mount}). */
    public static boolean ride(WarriorCompanionEntity warrior, AbstractHorse horse) {
        if (warrior.startRiding(horse, true)) {
            warrior.setMountUUID(horse.getUUID());
            return true;
        }
        return false;
    }

    /** Chamado todo tick no servidor: guia o cavalo atrás do dono ou desmonta. */
    public static void tickMounted(WarriorCompanionEntity warrior) {
        if (!(warrior.getVehicle() instanceof AbstractHorse horse)) return;
        if (!shouldRide(warrior)) {
            warrior.stopRiding();
            horse.getNavigation().stop();
            return;
        }
        Player owner = warrior.getOwner();
        if (owner == null) return;
        double distSq = horse.distanceToSqr(owner);
        if (distSq > 7.0D * 7.0D) {
            if (warrior.tickCount % 10 == 0 || horse.getNavigation().isDone()) {
                horse.getNavigation().moveTo(owner, distSq > 16.0D * 16.0D ? 2.2D : 1.6D);
            }
        } else if (distSq < 4.0D * 4.0D) {
            horse.getNavigation().stop();
        }
    }
}
