package com.example.epicvanguard.entity.ai;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Warrior Tactical Combat Goal:
 * Replaces linear brainless attacks with dynamic combat pacing:
 * 1. Tactical approach adapting to ranged vs melee enemies.
 * 2. Circular strafe orbit to destabilize enemy aiming and look for openings.
 * 3. Fast lunge and combo string.
 * 4. Micro-spacing / tactical disengage backstep (whiff-punish mechanics).
 * 5. Instant evasion against ignited creepers.
 */
public class WarriorTacticalCombatGoal extends Goal {

    private final WarriorCompanionEntity warrior;
    private final double speedModifier;

    private enum Phase {
        APPROACH,
        STRAFE,
        LUNGE,
        DISENGAGE
    }

    private Phase phase = Phase.APPROACH;
    private int phaseTicks = 0;
    private int strafeDir = 1;
    private int attackCooldown = 0;
    private int comboCount = 0;
    private int maxCombo = 3;
    private int pathRecalcTicks = 0;

    public WarriorTacticalCombatGoal(WarriorCompanionEntity warrior, double speedModifier) {
        this.warrior = warrior;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (warrior.isTalking() || warrior.isInventoryOpen() || warrior.isDuelMode() ||
            warrior.isEmergencyRetreating() || warrior.isLowHealth() || warrior.isPrisoner()) {
            return false;
        }

        if (warrior.isRecruited()) {
            if (warrior.getCombatMode() == 2) { // Parado
                warrior.clearCombatTarget();
                return false;
            }
            if (warrior.getCombatMode() == 1 && warrior.getGuardPos() != null) { // Guarda
                if (warrior.getTarget() != null) {
                    double distToGuard = warrior.getTarget().distanceToSqr(
                            warrior.getGuardPos().getX() + 0.5D,
                            warrior.getGuardPos().getY(),
                            warrior.getGuardPos().getZ() + 0.5D
                    );
                    if (distToGuard > 64.0D) {
                        warrior.clearCombatTarget();
                        return false;
                    }
                }
            }
        }

        LivingEntity target = warrior.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (warrior.isRecruited() && target == warrior.getOwner()) {
            warrior.setTarget(null);
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (warrior.isEmergencyRetreating() || warrior.isLowHealth() || warrior.isPrisoner()) {
            return false;
        }
        if (warrior.isRecruited() && warrior.getCombatMode() == 2) {
            warrior.clearCombatTarget();
            return false;
        }
        LivingEntity target = warrior.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.phase = Phase.APPROACH;
        this.phaseTicks = 0;
        this.attackCooldown = 0;
        this.comboCount = 0;
        this.pathRecalcTicks = 0;
        this.strafeDir = warrior.getRandom().nextBoolean() ? 1 : -1;
        this.maxCombo = getMaxComboForSpec();
    }

    @Override
    public void stop() {
        this.warrior.getNavigation().stop();
        this.warrior.lowerShield();
        this.phase = Phase.APPROACH;
        this.comboCount = 0;
    }

    private int getMaxComboForSpec() {
        int spec = warrior.getSpecialization();
        if (spec == WarriorCompanionEntity.SPEC_BERSERKER) {
            return 3 + warrior.getRandom().nextInt(2); // 3 a 4 golpes violentos
        } else if (spec == WarriorCompanionEntity.SPEC_GUARDIAN) {
            return 2; // 2 golpes firmes e volta para guarda
        } else if (spec == WarriorCompanionEntity.SPEC_DUELIST) {
            return 2 + warrior.getRandom().nextInt(2); // 2 a 3 golpes cirúrgicos
        }
        return 2 + warrior.getRandom().nextInt(2);
    }

    @Override
    public void tick() {
        LivingEntity target = warrior.getTarget();
        if (target == null || !target.isAlive()) return;

        // 1. Alerta de Sobrevivência: Creeper iniciando detonação nas proximidades
        Creeper dangerousCreeper = warrior.getImminentCreeperThreat(7.0D);
        if (dangerousCreeper != null) {
            Vec3 awayFromCreeper = warrior.position().subtract(dangerousCreeper.position()).multiply(1.0, 0.0, 1.0);
            if (awayFromCreeper.lengthSqr() < 1.0E-4D) {
                awayFromCreeper = new Vec3(-Math.sin(Math.toRadians(warrior.getYRot())), 0.0D, Math.cos(Math.toRadians(warrior.getYRot())));
            }
            warrior.applyRollImpulse(awayFromCreeper.normalize());
            this.phase = Phase.DISENGAGE;
            this.phaseTicks = 30;
            return;
        }

        // 2. Olha sempre atentamente para o oponente
        warrior.getLookControl().setLookAt(target, 35.0F, 35.0F);

        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (phaseTicks > 0) {
            phaseTicks--;
        }

        double distSq = warrior.distanceToSqr(target);
        double reachSqr = getAttackReachSqr(target);
        boolean isRangedEnemy = target instanceof RangedAttackMob;

        // 3. Máquina de Estados de Combate Tático
        switch (phase) {
            case APPROACH -> {
                if (isRangedEnemy) {
                    // Contra inimigos à distância (esqueletos, pillagers): avanço veloz em linha reta para travar corpo a corpo
                    if (--pathRecalcTicks <= 0) {
                        pathRecalcTicks = 8;
                        warrior.getNavigation().moveTo(target, speedModifier * 1.35D);
                    }
                    if (distSq <= reachSqr + 1.5D) {
                        phase = Phase.LUNGE;
                        phaseTicks = 15;
                    }
                } else {
                    // Contra inimigos corpo a corpo: aproximação ponderada
                    if (--pathRecalcTicks <= 0) {
                        pathRecalcTicks = 10;
                        warrior.getNavigation().moveTo(target, speedModifier);
                    }
                    // Ao atingir a zona de combate (raio de 3 a 4 blocos), entra em órbita de strafe
                    if (distSq <= reachSqr + 4.5D) {
                        warrior.getNavigation().stop();
                        phase = Phase.STRAFE;
                        int spec = warrior.getSpecialization();
                        // Berserker passa menos tempo circulando; Guardião e Duelista analisam mais
                        phaseTicks = (spec == WarriorCompanionEntity.SPEC_BERSERKER) ? 10 + warrior.getRandom().nextInt(10) : 20 + warrior.getRandom().nextInt(25);
                    }
                }
            }

            case STRAFE -> {
                // Manutenção de distância dinâmica (2.0 a 3.8 blocos)
                float forward = 0.0F;
                if (distSq > reachSqr + 3.0D) {
                    forward = 0.45F; // fecha um pouco o espaço
                } else if (distSq < reachSqr + 0.5D) {
                    forward = -0.50F; // recua para não tomar golpe de graça!
                }

                float strafeSpeed = (warrior.getSpecialization() == WarriorCompanionEntity.SPEC_DUELIST) ? 0.85F : 0.60F;
                warrior.getMoveControl().strafe(forward, strafeSpeed * strafeDir);

                // Se colidir com bloco na lateral, inverte a órbita
                if (warrior.horizontalCollision && warrior.getRandom().nextFloat() < 0.3F) {
                    strafeDir = -strafeDir;
                }

                // Quando o timer da órbita termina ou se o ataque já estiver pronto
                if (phaseTicks <= 0 && attackCooldown <= 0) {
                    phase = Phase.LUNGE;
                    phaseTicks = 25;
                }
            }

            case LUNGE -> {
                // Bote ofensivo: acelera direto ao alcance de ataque
                warrior.getNavigation().moveTo(target, speedModifier * 1.25D);

                if (distSq <= reachSqr && attackCooldown <= 0) {
                    // Executa o golpe
                    warrior.lowerShield();
                    warrior.swing(InteractionHand.MAIN_HAND);
                    warrior.doHurtTarget(target);
                    comboCount++;

                    int spec = warrior.getSpecialization();
                    // Berserker em fúria tem cadência de ataque mais rápida
                    int baseCd = (spec == WarriorCompanionEntity.SPEC_BERSERKER && warrior.getHealth() < warrior.getMaxHealth() * 0.40F) ? 8 : 12;
                    attackCooldown = baseCd;

                    if (comboCount >= maxCombo) {
                        // Fim do combo -> recuo tático de desengajamento
                        phase = Phase.DISENGAGE;
                        phaseTicks = (spec == WarriorCompanionEntity.SPEC_DUELIST) ? 12 : 18;

                        // Passo tático rápido para trás (backstep)
                        Vec3 away = warrior.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
                        if (away.lengthSqr() > 1.0E-4D) {
                            double stepPower = (spec == WarriorCompanionEntity.SPEC_DUELIST) ? 0.45D : 0.35D;
                            warrior.performTacticalStep(away.normalize(), stepPower);
                        }
                    }
                } else if (phaseTicks <= 0) {
                    // Se não alcançou a tempo, reseta para strafe
                    phase = Phase.STRAFE;
                    phaseTicks = 20;
                }
            }

            case DISENGAGE -> {
                // Se for Guardião e tiver escudo: ergue a guarda durante o desengajamento!
                if (warrior.getSpecialization() == WarriorCompanionEntity.SPEC_GUARDIAN && warrior.hasShield()) {
                    warrior.raiseShield();
                } else if (warrior.getSpecialization() == WarriorCompanionEntity.SPEC_DUELIST) {
                    // Duelista circula ágil para o lado oposto
                    warrior.getMoveControl().strafe(-0.2F, 0.70F * strafeDir);
                }

                if (phaseTicks <= 0) {
                    warrior.lowerShield();
                    comboCount = 0;
                    strafeDir = -strafeDir; // Inverte o sentido do próximo ataque
                    maxCombo = getMaxComboForSpec();
                    phase = (distSq > reachSqr + 5.0D) ? Phase.APPROACH : Phase.STRAFE;
                    phaseTicks = 20;
                }
            }
        }
    }

    private double getAttackReachSqr(LivingEntity target) {
        return (double)(this.warrior.getBbWidth() * 2.0F * this.warrior.getBbWidth() * 2.0F + target.getBbWidth());
    }
}