package com.example.epicvanguard.dialogue;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

public class WarriorSpeechSystem {

    public static final int GLOBAL_COOLDOWN_TICKS = 160; // 8 segundos entre quaisquer falas

    public static boolean isBossOrElite(LivingEntity entity) {
        if (entity == null) return false;
        if (entity instanceof WitherBoss
                || entity instanceof EnderDragon
                || entity instanceof Warden
                || entity instanceof ElderGuardian
                || entity instanceof Ravager
                || entity instanceof Evoker) {
            return true;
        }
        return entity.getMaxHealth() >= 50.0F;
    }

    public static boolean trySpeak(WarriorCompanionEntity warrior, SpeechTrigger trigger) {
        if (warrior == null || warrior.level().isClientSide() || !warrior.isAlive()) {
            return false;
        }

        if (!warrior.isRecruited() && trigger != SpeechTrigger.FRIENDLY_FIRE) {
            return false;
        }

        if (warrior.getGlobalSpeechCooldown() > 0) {
            return false;
        }

        if (warrior.getTriggerSpeechCooldown(trigger) > 0) {
            return false;
        }

        if (warrior.getRandom().nextFloat() > trigger.getTriggerChance()) {
            return false;
        }

        // Aplica cooldowns
        warrior.setGlobalSpeechCooldown(GLOBAL_COOLDOWN_TICKS);
        warrior.setTriggerSpeechCooldown(trigger, trigger.getCooldownTicks());

        // Seleciona variante aleatória
        PersonalityArchetype personality = warrior.getPersonality();
        int variant = 1 + warrior.getRandom().nextInt(Math.max(1, trigger.getVariantCount()));
        String translationKey = "speech.epicvanguard." + personality.getKey() + "." + trigger.getKey() + "." + variant;

        MutableComponent fullMsg = Component.literal("§6[" + warrior.getWarriorName() + "] §7\"§f")
                .append(Component.translatable(translationKey))
                .append(Component.literal("§7\""));

        Player owner = warrior.getOwner();
        if (owner != null) {
            owner.sendSystemMessage(fullMsg);
        }

        // Também envia para jogadores próximos no raio de 16 blocos
        for (Player nearbyPlayer : warrior.level().getEntitiesOfClass(Player.class, warrior.getBoundingBox().inflate(16.0D))) {
            if (nearbyPlayer != owner) {
                nearbyPlayer.sendSystemMessage(fullMsg);
            }
        }

        return true;
    }

    public static void onSpotTarget(WarriorCompanionEntity warrior, LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        if (isBossOrElite(target)) {
            trySpeak(warrior, SpeechTrigger.SPOT_BOSS);
        } else {
            trySpeak(warrior, SpeechTrigger.SPOT_ENEMY);
        }
    }

    public static void onKillTarget(WarriorCompanionEntity warrior, LivingEntity victim) {
        if (victim == null) return;
        if (isBossOrElite(victim)) {
            trySpeak(warrior, SpeechTrigger.KILL_BOSS);
        } else {
            trySpeak(warrior, SpeechTrigger.KILL_ENEMY);
        }
    }

    public static void onCriticalRetreat(WarriorCompanionEntity warrior) {
        trySpeak(warrior, SpeechTrigger.CRITICAL_RETREAT);
    }

    public static void onHealed(WarriorCompanionEntity warrior) {
        trySpeak(warrior, SpeechTrigger.HEALED);
    }

    public static void onFriendlyFire(WarriorCompanionEntity warrior, Player player) {
        if (warrior.isOwner(player)) {
            trySpeak(warrior, SpeechTrigger.FRIENDLY_FIRE);
        }
    }

    public static void onCampfireRest(WarriorCompanionEntity warrior) {
        trySpeak(warrior, SpeechTrigger.CAMPFIRE_REST);
    }

    public static void onBaseWelcome(WarriorCompanionEntity warrior) {
        trySpeak(warrior, SpeechTrigger.BASE_WELCOME);
    }
}
