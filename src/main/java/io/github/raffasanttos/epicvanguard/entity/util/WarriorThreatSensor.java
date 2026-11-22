package io.github.raffasanttos.epicvanguard.entity.util;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Detecta ameaças próximas: creepers prestes a explodir e projéteis vindo na direção do guerreiro ou do dono.
 *
 * <p>As varreduras de área são caras, e os goals pedem várias vezes por tick com raios diferentes.
 * Por isso cada guerreiro guarda a última lista de entidades num {@link Cache}: projéteis são
 * varridos a cada {@value #PROJECTILE_SCAN_INTERVAL} ticks e creepers a cada {@value #CREEPER_SCAN_INTERVAL},
 * com uma margem no raio. Na hora da consulta a posição, o movimento e o estado de cada entidade
 * são lidos de novo, então o resultado continua atual.
 */
public final class WarriorThreatSensor {

    static final int PROJECTILE_SCAN_INTERVAL = 2;
    static final int CREEPER_SCAN_INTERVAL = 5;
    /** Maior raio pedido pelos goals (12) + o que uma flecha anda em 2 ticks (~6). */
    private static final double PROJECTILE_SCAN_RADIUS = 18.0D;
    /** Maior raio pedido pelos goals (7) + margem para o creeper andar até a próxima varredura. */
    private static final double CREEPER_SCAN_RADIUS = 9.0D;

    private WarriorThreatSensor() {}

    /** Últimas varreduras de um guerreiro (um por entidade, só no servidor). */
    public static final class Cache {
        private List<Projectile> projectiles = Collections.emptyList();
        private int projectileScanTick = Integer.MIN_VALUE;
        private List<Creeper> creepers = Collections.emptyList();
        private int creeperScanTick = Integer.MIN_VALUE;
    }

    private static List<Projectile> projectiles(WarriorCompanionEntity w) {
        Cache c = w.getThreatCache();
        int now = w.tickCount;
        if (now - c.projectileScanTick >= PROJECTILE_SCAN_INTERVAL || now < c.projectileScanTick) {
            c.projectiles = w.level().getEntitiesOfClass(Projectile.class,
                    w.getBoundingBox().inflate(PROJECTILE_SCAN_RADIUS), Entity::isAlive);
            c.projectileScanTick = now;
        }
        return c.projectiles;
    }

    private static List<Creeper> creepers(WarriorCompanionEntity w) {
        Cache c = w.getThreatCache();
        int now = w.tickCount;
        if (now - c.creeperScanTick >= CREEPER_SCAN_INTERVAL || now < c.creeperScanTick) {
            c.creepers = w.level().getEntitiesOfClass(Creeper.class,
                    w.getBoundingBox().inflate(CREEPER_SCAN_RADIUS), Entity::isAlive);
            c.creeperScanTick = now;
        }
        return c.creepers;
    }

    private static boolean inBox(WarriorCompanionEntity w, Entity e, double radius) {
        return e.isAlive() && w.getBoundingBox().inflate(radius).intersects(e.getBoundingBox());
    }

    private static boolean isFlying(Projectile p) {
        return p.getDeltaMovement().lengthSqr() > 0.05D;
    }

    @Nullable
    public static Creeper imminentCreeper(WarriorCompanionEntity w, double radius) {
        for (Creeper c : creepers(w)) {
            if (inBox(w, c, radius) && (c.isIgnited() || c.getSwellDir() > 0)) {
                return c;
            }
        }
        return null;
    }

    public static boolean threatenedByProjectiles(WarriorCompanionEntity w, double radius) {
        Player owner = w.getOwner();
        boolean ownerClose = owner != null && w.distanceToSqr(owner) < 64.0D;
        for (Projectile p : projectiles(w)) {
            if (!inBox(w, p, radius) || !isFlying(p)) continue;
            Vec3 projMotion = p.getDeltaMovement().normalize();
            Vec3 toWarrior = w.position().subtract(p.position()).normalize();
            if (projMotion.dot(toWarrior) > 0.65D) {
                return true;
            }
            if (ownerClose) {
                Vec3 toOwner = owner.position().subtract(p.position()).normalize();
                if (projMotion.dot(toOwner) > 0.65D) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static Entity incomingProjectile(WarriorCompanionEntity w, double radius) {
        for (Projectile p : projectiles(w)) {
            if (!inBox(w, p, radius) || !isFlying(p)) continue;
            Vec3 projMotion = p.getDeltaMovement().normalize();
            Vec3 toWarrior = w.position().subtract(p.position()).normalize();
            if (projMotion.dot(toWarrior) > 0.65D) {
                return p;
            }
        }
        return null;
    }
}
