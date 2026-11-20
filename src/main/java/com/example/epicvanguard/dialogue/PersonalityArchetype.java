package com.example.epicvanguard.dialogue;

public enum PersonalityArchetype {
    VETERAN(0, "veteran"),
    BRAVE(1, "brave"),
    SENTINEL(2, "sentinel");

    private final int id;
    private final String key;

    PersonalityArchetype(int id, String key) {
        this.id = id;
        this.key = key;
    }

    public int getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    /** Nome traduzível (lang: epicvanguard.personality.<key>). */
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("epicvanguard.personality." + key);
    }

    public static PersonalityArchetype byId(int id) {
        for (PersonalityArchetype type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return VETERAN;
    }
}
