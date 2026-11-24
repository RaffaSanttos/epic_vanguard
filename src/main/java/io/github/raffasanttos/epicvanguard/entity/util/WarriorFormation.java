package io.github.raffasanttos.epicvanguard.entity.util;

import io.github.raffasanttos.epicvanguard.entity.CompanionSavedData;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Formações do modo Seguir, escolhidas pela Corneta (agachado + botão direito) ou pela tela Minha Companhia.
 * A formação é do dono (vale para todos os companheiros dele) e fica salva no {@link CompanionSavedData}.
 *
 * <ul>
 *   <li>{@link #FREE}: como sempre foi, cada um segue de perto do seu jeito;</li>
 *   <li>{@link #COLUMN}: fila indiana atrás do dono;</li>
 *   <li>{@link #LINE}: lado a lado, uma fileira atrás do dono;</li>
 *   <li>{@link #CIRCLE}: escolta em círculo em volta do dono.</li>
 * </ul>
 */
public final class WarriorFormation {

    private WarriorFormation() {}

    public static final int FREE = 0;
    public static final int COLUMN = 1;
    public static final int LINE = 2;
    public static final int CIRCLE = 3;
    public static final int COUNT = 4;

    /** Até onde um companheiro conta como "na formação" (os mais longe vêm se aproximando sem posto). */
    public static final double MEMBER_RADIUS = 24.0D;

    public static int next(int formation) {
        return (formation + 1) % COUNT;
    }

    public static int sanitize(int formation) {
        return formation < 0 || formation >= COUNT ? FREE : formation;
    }

    public static String key(int formation) {
        return switch (sanitize(formation)) {
            case COLUMN -> "column";
            case LINE -> "line";
            case CIRCLE -> "circle";
            default -> "free";
        };
    }

    public static Component name(int formation) {
        return Component.translatable("epicvanguard.formation." + key(formation));
    }

    /** Formação atual do dono (servidor). */
    public static int getFormation(WarriorCompanionEntity w) {
        if (w.getServer() == null) return FREE;
        UUID owner = w.getOwnerUUID().orElse(null);
        return owner == null ? FREE : CompanionSavedData.get(w.getServer()).getFormation(owner);
    }

    /**
     * Companheiros do mesmo dono que estão seguindo em formação, em ordem fixa (UUID): a posição de cada um
     * na lista é o lugar dele na formação.
     */
    public static List<WarriorCompanionEntity> members(Player owner) {
        List<WarriorCompanionEntity> list = owner.level().getEntitiesOfClass(WarriorCompanionEntity.class,
                owner.getBoundingBox().inflate(MEMBER_RADIUS),
                w -> w.isAlive() && w.isRecruited() && !w.isPrisoner() && !w.isPassenger()
                        && w.getCombatMode() == WarriorCompanionEntity.MODE_FOLLOW && w.isOwner(owner));
        list.sort(Comparator.comparing(WarriorCompanionEntity::getUUID));
        return list;
    }

    /**
     * Ponto do lugar {@code index} (de {@code count}) na formação, girado para onde o dono olha.
     * {@code null} na formação livre.
     */
    public static Vec3 slotPosition(int formation, int index, int count, Vec3 ownerPos, float ownerYaw) {
        double yaw = Math.toRadians(ownerYaw);
        // Frente do dono (como o vetor de visão do Minecraft) e a direita dele
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double rx = -fz, rz = fx;
        switch (sanitize(formation)) {
            case COLUMN -> {
                double back = 2.5D * (index + 1);
                return ownerPos.add(-fx * back, 0.0D, -fz * back);
            }
            case LINE -> {
                double lateral = (index - (count - 1) / 2.0D) * 2.0D;
                return ownerPos.add(-fx * 2.5D + rx * lateral, 0.0D, -fz * 2.5D + rz * lateral);
            }
            case CIRCLE -> {
                double radius = count > 6 ? 4.0D : 3.0D;
                double angle = yaw + Math.PI + (2.0D * Math.PI * index) / Math.max(1, count);
                return ownerPos.add(-Math.sin(angle) * radius, 0.0D, Math.cos(angle) * radius);
            }
            default -> {
                return null;
            }
        }
    }
}
