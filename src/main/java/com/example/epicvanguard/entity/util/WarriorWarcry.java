package com.example.epicvanguard.entity.util;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

/** Grito de guerra de cada classe (efeitos, partículas e sons). Separado da WarriorCompanionEntity. */
public final class WarriorWarcry {

    private WarriorWarcry() {}

    /** Dispara o grito de guerra da classe do guerreiro e inicia a recarga (50 s). */
    public static void trigger(WarriorCompanionEntity w) {
        if (w.level().isClientSide) return;
        w.setWarcryCooldown(1000); // 50 segundos de recarga

        switch (w.getSpecialization()) {
            case WarriorCompanionEntity.SPEC_BERSERKER -> executeBerserkerWarcry(w);
            case WarriorCompanionEntity.SPEC_GUARDIAN  -> executeGuardianWarcry(w);
            case WarriorCompanionEntity.SPEC_DUELIST   -> executeDuelistWarcry(w);
            default             -> executeBaseWarcry(w);
        }
    }

    private static void executeBerserkerWarcry(WarriorCompanionEntity w) {
        if (!(w.level() instanceof ServerLevel serverLevel)) return;
        w.level().playSound(null, w.getX(), w.getY(), w.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.5F, 1.2F);
        spawnParticleRing(w, serverLevel, ParticleTypes.FLAME, 3.0D, 24);
        spawnParticleRing(w, serverLevel, ParticleTypes.ANGRY_VILLAGER, 2.0D, 12);

        // 1. Monstros em 10 blocos ganham Fraqueza I por 10s (200 ticks)
        List<Monster> enemies = serverLevel.getEntitiesOfClass(Monster.class, w.getBoundingBox().inflate(10.0D), LivingEntity::isAlive);
        for (Monster enemy : enemies) {
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, true));
        }

        // 2. Berserker ganha Força II por 10s
        w.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, true));

        // 3. Jogador e equipe ganham Força I por 10s
        Player owner = w.getOwner();
        if (owner != null && owner.distanceToSqr(w) <= 256.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, false, true));
            owner.displayClientMessage(
                    Component.translatable("epicvanguard.text.warrior_companion_entity.2", w.getWarriorName()),
                    false);
        }
    }

    private static void executeGuardianWarcry(WarriorCompanionEntity w) {
        if (!(w.level() instanceof ServerLevel serverLevel)) return;
        w.level().playSound(null, w.getX(), w.getY(), w.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2F, 0.8F);
        w.level().playSound(null, w.getX(), w.getY(), w.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.3F, 0.7F);
        spawnParticleRing(w, serverLevel, ParticleTypes.FLASH, 4.0D, 16);
        spawnParticleRing(w, serverLevel, ParticleTypes.ENCHANTED_HIT, 3.0D, 32);

        // 1. Taunt absoluto (100%) em monstros num raio de 12 blocos
        List<Monster> enemies = serverLevel.getEntitiesOfClass(Monster.class, w.getBoundingBox().inflate(12.0D), LivingEntity::isAlive);
        for (Monster enemy : enemies) {
            enemy.setTarget(w);
        }

        // 2. Guardião ganha Resistência II por 15s (300 ticks)
        w.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1, false, true));

        // 3. Jogador e equipe ganham Força I e Resistência I por 15s
        Player owner = w.getOwner();
        if (owner != null && owner.distanceToSqr(w) <= 324.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0, false, true));
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0, false, true));
            owner.displayClientMessage(
                    Component.translatable("epicvanguard.text.warrior_companion_entity.3", w.getWarriorName()),
                    false);
        }
    }

    private static void executeDuelistWarcry(WarriorCompanionEntity w) {
        if (!(w.level() instanceof ServerLevel serverLevel)) return;
        w.level().playSound(null, w.getX(), w.getY(), w.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5F, 0.5F);
        w.level().playSound(null, w.getX(), w.getY(), w.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.8F);
        spawnParticleRing(w, serverLevel, ParticleTypes.SWEEP_ATTACK, 2.5D, 16);
        spawnParticleRing(w, serverLevel, ParticleTypes.CLOUD, 3.5D, 24);

        // 1. Desestabiliza inimigos em 6 blocos com quebra de postura breve (Lentidão IV / atordoamento por 1.5s)
        List<Monster> enemies = serverLevel.getEntitiesOfClass(Monster.class, w.getBoundingBox().inflate(6.0D), LivingEntity::isAlive);
        for (Monster enemy : enemies) {
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3, false, true));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, true));
        }

        // 2. Jogador e equipe ganham Força I e Velocidade II (+40%) por 12s (240 ticks)
        w.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 1, false, true));
        w.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 240, 0, false, true));

        Player owner = w.getOwner();
        if (owner != null && owner.distanceToSqr(w) <= 256.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 1, false, true));
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 240, 0, false, true));
            owner.displayClientMessage(
                    Component.translatable("epicvanguard.text.warrior_companion_entity.4", w.getWarriorName()),
                    false);
        }
    }

    private static void executeBaseWarcry(WarriorCompanionEntity w) {
        if (!(w.level() instanceof ServerLevel serverLevel)) return;
        w.level().playSound(null, w.getX(), w.getY(), w.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.0F);
        spawnParticleRing(w, serverLevel, ParticleTypes.CRIT, 2.5D, 16);

        w.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0, false, true));
        Player owner = w.getOwner();
        if (owner != null && owner.distanceToSqr(w) <= 256.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0, false, true));
            owner.displayClientMessage(
                    Component.translatable("epicvanguard.text.warrior_companion_entity.5", w.getWarriorName()),
                    false);
        }
    }

    private static void spawnParticleRing(WarriorCompanionEntity w, ServerLevel serverLevel, ParticleOptions particle, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            double px = w.getX() + Math.cos(angle) * radius;
            double pz = w.getZ() + Math.sin(angle) * radius;
            serverLevel.sendParticles(particle, px, w.getY() + 0.2D, pz, 1, 0.0D, 0.1D, 0.0D, 0.02D);
        }
    }
}
