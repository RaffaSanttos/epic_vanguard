package com.example.epicvanguard.dialogue;

public enum SpeechTrigger {
    SPOT_ENEMY("spot_enemy", 3, 600, 0.05F),
    SPOT_BOSS("spot_boss", 3, 600, 1.00F),
    KILL_ENEMY("kill_enemy", 3, 600, 0.10F),
    KILL_BOSS("kill_boss", 3, 600, 1.00F),
    CRITICAL_RETREAT("retreat", 3, 300, 1.00F),
    HEALED("healed", 3, 400, 0.60F),
    FRIENDLY_FIRE("friendly_fire", 3, 160, 0.70F),
    CAMPFIRE_REST("campfire", 3, 800, 0.40F),
    BASE_WELCOME("welcome", 3, 800, 0.85F);

    private final String key;
    private final int variantCount;
    private final int cooldownTicks;
    private final float triggerChance;

    SpeechTrigger(String key, int variantCount, int cooldownTicks, float triggerChance) {
        this.key = key;
        this.variantCount = variantCount;
        this.cooldownTicks = cooldownTicks;
        this.triggerChance = triggerChance;
    }

    public String getKey() {
        return key;
    }

    public int getVariantCount() {
        return variantCount;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public float getTriggerChance() {
        return triggerChance;
    }
}
