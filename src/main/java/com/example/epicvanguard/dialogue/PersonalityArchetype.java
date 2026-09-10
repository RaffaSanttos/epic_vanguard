package com.example.epicvanguard.dialogue;

public enum PersonalityArchetype {
    VETERAN(0, "veteran", "Veterano"),
    BRAVE(1, "brave", "Audaz"),
    SENTINEL(2, "sentinel", "Sentinela");

    private final int id;
    private final String key;
    private final String displayName;

    PersonalityArchetype(int id, String key, String displayName) {
        this.id = id;
        this.key = key;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
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
