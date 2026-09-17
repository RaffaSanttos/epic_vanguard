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
import net.minecraft.world.entity.raid.Raider;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarriorSpeechSystem {

    public static final int GLOBAL_COOLDOWN_TICKS = 200; // 10 segundos individuais

    // Cooldown compartilhado de esquadrão: garante que apenas 1 companheiro fale pelo grupo
    private static final Map<UUID, Long> SQUAD_LAST_SPEAK_TIME = new ConcurrentHashMap<>();
    private static final Map<String, Long> SQUAD_TRIGGER_LAST_TIME = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> UNOWNED_LAST_SPEAK_TIME = new ConcurrentHashMap<>();

    public static final long SQUAD_GLOBAL_COOLDOWN_TICKS = 240L; // 12 segundos entre quaisquer falas no mesmo esquadrão
    public static final long SQUAD_TRIGGER_COOLDOWN_TICKS = 600L; // 30 segundos para o mesmo tipo de fala no esquadrão

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
        if (entity instanceof Raider raider && raider.hasActiveRaid()) {
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

        // 1. Cooldowns individuais do guerreiro
        if (warrior.getGlobalSpeechCooldown() > 0) {
            return false;
        }

        if (warrior.getTriggerSpeechCooldown(trigger) > 0) {
            return false;
        }

        // 2. Cooldown Compartilhado de Esquadrão (Apenas 1 guerreiro fala pelo grupo)
        long currentTick = warrior.level().getGameTime();
        UUID ownerUUID = warrior.getOwnerUUID().orElse(null);

        if (ownerUUID != null) {
            Long lastSquadSpeak = SQUAD_LAST_SPEAK_TIME.get(ownerUUID);
            if (lastSquadSpeak != null && (currentTick - lastSquadSpeak) < SQUAD_GLOBAL_COOLDOWN_TICKS) {
                return false;
            }

            String triggerKey = ownerUUID + ":" + trigger.name();
            Long lastTriggerSpeak = SQUAD_TRIGGER_LAST_TIME.get(triggerKey);
            if (lastTriggerSpeak != null && (currentTick - lastTriggerSpeak) < SQUAD_TRIGGER_COOLDOWN_TICKS) {
                return false;
            }
        } else {
            int chunkHash = (warrior.blockPosition().getX() >> 4) ^ (warrior.blockPosition().getZ() >> 4);
            Long lastUnowned = UNOWNED_LAST_SPEAK_TIME.get(chunkHash);
            if (lastUnowned != null && (currentTick - lastUnowned) < 300L) {
                return false;
            }
        }

        // 3. Probabilidade do gatilho
        if (warrior.getRandom().nextFloat() > trigger.getTriggerChance()) {
            return false;
        }

        // Aplica cooldowns individuais
        warrior.setGlobalSpeechCooldown(GLOBAL_COOLDOWN_TICKS);
        warrior.setTriggerSpeechCooldown(trigger, trigger.getCooldownTicks());

        // Aplica cooldowns de esquadrão
        if (ownerUUID != null) {
            SQUAD_LAST_SPEAK_TIME.put(ownerUUID, currentTick);
            SQUAD_TRIGGER_LAST_TIME.put(ownerUUID + ":" + trigger.name(), currentTick);
        } else {
            int chunkHash = (warrior.blockPosition().getX() >> 4) ^ (warrior.blockPosition().getZ() >> 4);
            UNOWNED_LAST_SPEAK_TIME.put(chunkHash, currentTick);
        }

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
        // Inimigos fracos comuns (zumbis, esqueletos, aranhas, creepers) NÃO geram spam no chat.
        // Apenas Bosses, Elites e Invasões disparam o alerta tático de aproximação!
        if (isBossOrElite(target)) {
            trySpeak(warrior, SpeechTrigger.SPOT_BOSS);
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
