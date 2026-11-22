package com.example.epicvanguard.entity.util;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/** Detecta ameaças próximas: creepers prestes a explodir e projéteis vindo na direção do guerreiro ou do dono. */
public final class WarriorThreatSensor {

    private WarriorThreatSensor() {}

    @Nullable
    public static Creeper imminentCreeper(WarriorCompanionEntity w, double radius) {
        List<Creeper> creepers = w.level().getEntitiesOfClass(
                Creeper.class,
                w.getBoundingBox().inflate(radius),
                c -> c.isAlive() && (c.isIgnited() || c.getSwellDir() > 0)
        );
        return creepers.isEmpty() ? null : creepers.get(0);
    }

    public static boolean threatenedByProjectiles(WarriorCompanionEntity w, double radius) {
        List<Projectile> projectiles = w.level().getEntitiesOfClass(
                Projectile.class,
                w.getBoundingBox().inflate(radius),
                p -> p.isAlive() && p.getDeltaMovement().lengthSqr() > 0.05D
        );
        for (Projectile p : projectiles) {
            Vec3 projMotion = p.getDeltaMovement().normalize();
            Vec3 toWarrior = w.position().subtract(p.position()).normalize();
            if (projMotion.dot(toWarrior) > 0.65D) {
                return true;
            }
            Player owner = w.getOwner();
            if (owner != null && w.distanceToSqr(owner) < 64.0D) {
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
        List<Projectile> projectiles = w.level().getEntitiesOfClass(
                Projectile.class,
                w.getBoundingBox().inflate(radius),
                p -> p.isAlive() && p.getDeltaMovement().lengthSqr() > 0.05D
        );
        for (Projectile p : projectiles) {
            Vec3 projMotion = p.getDeltaMovement().normalize();
            Vec3 toWarrior = w.position().subtract(p.position()).normalize();
            if (projMotion.dot(toWarrior) > 0.65D) {
                return p;
            }
        }
        return null;
    }
}
