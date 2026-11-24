package io.github.raffasanttos.epicvanguard.entity.ai;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorRangedHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Combate à distância com arco ou besta na mão principal:
 * mantém de 6 a 14 blocos do alvo, circula de lado, recua se o inimigo encostar,
 * puxa o arco e atira (gastando flechas da mochila, a não ser com Infinidade ou a config infiniteArrows).
 * Não atira se o dono estiver na linha de tiro.
 */
public class WarriorRangedCombatGoal extends Goal {

    private double minRangeSqr() { return WarriorRangedHelper.isArcher(warrior) ? 8.0D * 8.0D : 6.0D * 6.0D; }
    private double maxRangeSqr() { return WarriorRangedHelper.isArcher(warrior) ? 18.0D * 18.0D : 14.0D * 14.0D; }
    private double shootRangeSqr() { return WarriorRangedHelper.isArcher(warrior) ? 22.0D * 22.0D : 18.0D * 18.0D; }

    private final WarriorCompanionEntity warrior;
    private final double speedModifier;
    private int drawTicks = 0;
    private int shotCooldown = 0;
    private int strafeTicks = 0;
    private int strafeDir = 1;
    private int seeTime = 0;
    private int pathRecalcTicks = 0;

    public WarriorRangedCombatGoal(WarriorCompanionEntity warrior, double speedModifier) {
        this.warrior = warrior;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!WarriorRangedHelper.canFightAtRange(warrior)) return false;
        if (warrior.isTalking() || warrior.isInventoryOpen() || warrior.isDuelMode()
                || warrior.shouldAvoidCombat() || warrior.isPrisoner()) {
            return false;
        }
        if (warrior.isRecruited() && warrior.getCombatMode() == 2) return false;
        LivingEntity target = warrior.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (warrior.isRecruited() && target == warrior.getOwner()) {
            warrior.setTarget(null);
            return false;
        }
        // No modo Guarda não persegue alvo que se afastou do posto (mesma regra da escolha de alvo)
        if (!warrior.canEngageFromGuard(target, 2.0D)) {
            warrior.clearCombatTarget();
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.drawTicks = 0;
        this.seeTime = 0;
        this.strafeTicks = 0;
        this.warrior.setAggressive(true);
    }

    @Override
    public void stop() {
        this.warrior.setAggressive(false);
        this.seeTime = 0;
        this.drawTicks = 0;
        if (warrior.isUsingItem() && WarriorRangedHelper.isRangedWeapon(warrior.getUseItem())) {
            warrior.stopUsingItem();
        }
        if (!warrior.isPassenger()) warrior.getNavigation().stop();
        warrior.syncEquipmentWithInventory();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = warrior.getTarget();
        if (target == null) return;

        double distSq = warrior.distanceToSqr(target);
        boolean canSee = warrior.getSensing().hasLineOfSight(target);
        seeTime = canSee ? seeTime + 1 : 0;
        if (shotCooldown > 0) shotCooldown--;

        warrior.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // ── Movimento: aproxima, mantém distância ou recua (montado, quem anda é o cavalo) ──
        if (warrior.isPassenger()) {
            // nada: WarriorMountHelper guia o cavalo atrás do dono
        } else if (!canSee || distSq > maxRangeSqr()) {
            if (--pathRecalcTicks <= 0) {
                pathRecalcTicks = 10;
                warrior.getNavigation().moveTo(target, speedModifier);
            }
        } else {
            warrior.getNavigation().stop();
            if (++strafeTicks >= 40) {
                strafeTicks = 0;
                if (warrior.getRandom().nextFloat() < 0.4F) strafeDir = -strafeDir;
            }
            float forward = distSq < minRangeSqr() ? -0.6F : (distSq > maxRangeSqr() * 0.8D ? 0.3F : 0.0F);
            warrior.getMoveControl().strafe(forward, 0.45F * strafeDir);

            // Inimigo encostou: passo tático para trás para reabrir distância
            if (distSq < 3.0D * 3.0D && warrior.getDodgeCooldown() <= 0) {
                Vec3 away = warrior.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
                if (away.lengthSqr() > 1.0E-4D) {
                    warrior.performTacticalStep(away.normalize(), 0.45D);
                    warrior.setDodgeCooldown(40);
                }
            }
        }

        // ── Tiro ──
        boolean inRange = distSq <= shootRangeSqr();
        boolean ownerInLine = isOwnerInLineOfFire(target);
        if (!canSee || !inRange || ownerInLine || shotCooldown > 0) {
            // Perdeu a visão ou o alcance: abaixa o arco (com o dono na frente, segura puxado esperando)
            if (warrior.isUsingItem() && (!canSee || !inRange)) {
                warrior.stopUsingItem();
                drawTicks = 0;
            }
            return;
        }

        if (!warrior.isUsingItem()) {
            warrior.startUsingItem(InteractionHand.MAIN_HAND);
            drawTicks = 0;
            return;
        }

        drawTicks++;
        int drawTime = WarriorRangedHelper.getDrawTicks(warrior);
        if (drawTicks >= drawTime && seeTime >= 5) {
            float power = Math.min(1.0F, drawTicks / 20.0F);
            WarriorRangedHelper.shootAt(warrior, target, power);
            warrior.stopUsingItem();
            drawTicks = 0;
            shotCooldown = warrior.getMainHandItem().getItem() instanceof CrossbowItem ? 20 : 10;
        }
    }

    /** O dono está perto do segmento entre o guerreiro e o alvo (raio de 1,2 bloco)? */
    private boolean isOwnerInLineOfFire(LivingEntity target) {
        Player owner = warrior.getOwner();
        if (owner == null || owner.level() != warrior.level()) return false;
        Vec3 from = warrior.getEyePosition();
        Vec3 to = target.getEyePosition();
        Vec3 seg = to.subtract(from);
        double lenSq = seg.lengthSqr();
        if (lenSq < 1.0E-4D) return false;
        Vec3 ownerPos = owner.position().add(0.0D, owner.getBbHeight() * 0.5D, 0.0D);
        double t = Math.max(0.0D, Math.min(1.0D, ownerPos.subtract(from).dot(seg) / lenSq));
        Vec3 closest = from.add(seg.scale(t));
        return closest.distanceToSqr(ownerPos) < 1.2D * 1.2D;
    }
}
