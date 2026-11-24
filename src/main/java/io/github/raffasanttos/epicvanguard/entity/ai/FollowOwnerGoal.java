package io.github.raffasanttos.epicvanguard.entity.ai;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorFormation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Modo Seguir: anda até o dono. Com uma formação escolhida (Corneta ou tela Minha Companhia),
 * anda até o seu lugar na formação em vez de ir direto ao dono.
 */
public class FollowOwnerGoal extends Goal {
    private final WarriorCompanionEntity warrior;
    private final double speedModifier;
    private final float stopDist;
    private final float startDist;
    private int timeToRecalcPath;

    /** Lugar na formação, recalculado a cada segundo (a lista de membros muda pouco). */
    private int formation = WarriorFormation.FREE;
    private int slotIndex = 0;
    private int slotCount = 1;
    private int slotRefresh = 0;

    /** Distância do lugar na formação que já conta como "cheguei" / "preciso ir". */
    private static final double SLOT_ARRIVED_SQR = 0.8D * 0.8D;
    private static final double SLOT_START_SQR = 1.6D * 1.6D;

    public FollowOwnerGoal(WarriorCompanionEntity warrior, double speedModifier, float startDist, float stopDist) {
        this.warrior = warrior;
        this.speedModifier = speedModifier;
        this.startDist = startDist;
        this.stopDist = stopDist;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private boolean basicChecks(Player owner) {
        if (warrior.isTalking()) return false;
        if (owner == null || !warrior.isRecruited()) return false;
        if (warrior.getCombatMode() != 0) return false; // Apenas modo 0 (Seguir)
        return !warrior.isFighting(); // Se estiver lutando, só o teleporte o trará de volta
    }

    @Override
    public boolean canUse() {
        Player owner = warrior.getOwner();
        if (!basicChecks(owner) || owner.isSpectator()) return false;
        if (owner.level() != warrior.level()) return true;
        refreshSlot(owner, false);
        Vec3 slot = currentSlot(owner);
        if (slot != null) {
            return warrior.distanceToSqr(slot) > SLOT_START_SQR;
        }
        return warrior.distanceToSqr(owner) > (double) (startDist * startDist);
    }

    @Override
    public boolean canContinueToUse() {
        Player owner = warrior.getOwner();
        if (!basicChecks(owner)) return false;
        if (owner.level() != warrior.level()) return true;
        Vec3 slot = currentSlot(owner);
        if (slot != null) {
            return warrior.distanceToSqr(slot) > SLOT_ARRIVED_SQR;
        }
        return warrior.distanceToSqr(owner) > (double) (stopDist * stopDist);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.warrior.getNavigation().stop();
    }

    /** Atualiza formação e lugar a cada 20 ticks (ou na hora, se {@code force}). */
    private void refreshSlot(Player owner, boolean force) {
        if (!force && --slotRefresh > 0) return;
        slotRefresh = 20;
        formation = WarriorFormation.getFormation(warrior);
        if (formation == WarriorFormation.FREE || warrior.isPassenger()) {
            slotIndex = 0;
            slotCount = 1;
            return;
        }
        List<WarriorCompanionEntity> members = WarriorFormation.members(owner);
        int idx = members.indexOf(warrior);
        slotIndex = Math.max(0, idx);
        slotCount = Math.max(1, members.size());
    }

    /** Lugar na formação agora (segue o dono e para onde ele olha), ou null se livre/montado/longe. */
    private Vec3 currentSlot(Player owner) {
        if (formation == WarriorFormation.FREE || warrior.isPassenger() || owner.isPassenger()) return null;
        if (warrior.distanceToSqr(owner) > WarriorFormation.MEMBER_RADIUS * WarriorFormation.MEMBER_RADIUS) return null;
        return WarriorFormation.slotPosition(formation, slotIndex, slotCount, owner.position(), owner.getYRot());
    }

    @Override
    public void tick() {
        Player owner = warrior.getOwner();
        if (owner == null || warrior.getCombatMode() != 0) return;

        // Mudança de dimensão
        if (owner.level() != warrior.level() && owner.level() instanceof ServerLevel) {
            if (owner.getY() >= owner.level().getMinBuildHeight() && !owner.isFallFlying() && !owner.getAbilities().flying) {
                // A entidade é recriada na outra dimensão (esta é removida): safeTeleportTo cuida disso
                warrior.safeTeleportTo(owner);
            }
            return;
        }

        if (owner.isFallFlying() || owner.getAbilities().flying) {
            warrior.getLookControl().setLookAt(owner, 10.0F, (float) warrior.getMaxHeadXRot());
            return;
        }

        double distSq = warrior.distanceToSqr(owner);
        if (distSq >= 169.0D) { // >= 13 blocks (13^2 = 169)
            warrior.safeTeleportTo(owner);
            return;
        }

        refreshSlot(owner, false);
        Vec3 slot = currentSlot(owner);

        if (slot != null) {
            // Em formação: olha para onde o dono olha quando já está no lugar, senão para o dono
            if (warrior.distanceToSqr(slot) < 4.0D) {
                Vec3 ahead = owner.position().add(owner.getLookAngle().multiply(1.0D, 0.0D, 1.0D).scale(8.0D));
                warrior.getLookControl().setLookAt(ahead.x, owner.getEyeY(), ahead.z);
            } else {
                warrior.getLookControl().setLookAt(owner, 10.0F, (float) warrior.getMaxHeadXRot());
            }
            if (--timeToRecalcPath <= 0) {
                timeToRecalcPath = 5; // o dono anda: o lugar muda o tempo todo
                double speed = warrior.distanceToSqr(slot) > 36.0D ? speedModifier * 1.2D : speedModifier;
                if (!warrior.getNavigation().moveTo(slot.x, slot.y, slot.z, speed)) {
                    // Lugar inalcançável (parede, água...): vai até o dono como na formação livre
                    warrior.getNavigation().moveTo(owner, speedModifier);
                }
            }
            return;
        }

        warrior.getLookControl().setLookAt(owner, 10.0F, (float) warrior.getMaxHeadXRot());
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10;
            if (!warrior.getNavigation().moveTo(owner, speedModifier)) {
                if (distSq >= 169.0D) { // Apenas se ultrapassar 13 blocos de distância
                    warrior.safeTeleportTo(owner);
                }
            }
        }
    }

    /** A formação mudou (Corneta/tela): o próximo tick recalcula o lugar na hora. */
    public void invalidateSlot() {
        this.slotRefresh = 0;
    }
}
