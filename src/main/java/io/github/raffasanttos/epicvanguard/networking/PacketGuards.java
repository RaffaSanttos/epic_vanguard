package io.github.raffasanttos.epicvanguard.networking;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * Validações compartilhadas pelos pacotes C→S.
 * O servidor nunca deve confiar no cliente: todo pacote que mexe num guerreiro
 * precisa confirmar que ele existe, está perto e pertence a quem enviou.
 */
public final class PacketGuards {

    /** Distância máxima (em blocos) para interagir com um guerreiro via GUI. */
    public static final double MAX_INTERACT_DISTANCE = 10.0D;

    private PacketGuards() {}

    /** Retorna o guerreiro se ele existir, estiver vivo e ao alcance; senão {@code null}. */
    @Nullable
    public static WarriorCompanionEntity nearbyWarrior(@Nullable ServerPlayer player, int entityId) {
        if (player == null) return null;
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!(entity instanceof WarriorCompanionEntity warrior) || !warrior.isAlive()) return null;
        if (warrior.distanceToSqr(player) > MAX_INTERACT_DISTANCE * MAX_INTERACT_DISTANCE) return null;
        return warrior;
    }

    /** Como {@link #nearbyWarrior}, mas exige também que o jogador seja o dono do guerreiro recrutado. */
    @Nullable
    public static WarriorCompanionEntity ownedWarrior(@Nullable ServerPlayer player, int entityId) {
        WarriorCompanionEntity warrior = nearbyWarrior(player, entityId);
        if (warrior == null || !warrior.isRecruited() || !warrior.isOwner(player)) return null;
        return warrior;
    }
}
