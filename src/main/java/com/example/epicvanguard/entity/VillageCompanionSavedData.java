package com.example.epicvanguard.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
public class VillageCompanionSavedData extends SavedData {
    private static final String DATA_NAME = "grimal_village_spawner";
    public static final long DEFAULT_INTERVAL = 12000L; // ~10 minutos / meio dia Minecraft
    private long lastSpawnTime = 0;
    private long nextInterval = DEFAULT_INTERVAL;

    public static VillageCompanionSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                VillageCompanionSavedData::load,
                VillageCompanionSavedData::new,
                DATA_NAME
        );
    }

    public static VillageCompanionSavedData load(CompoundTag tag) {
        VillageCompanionSavedData data = new VillageCompanionSavedData();
        data.lastSpawnTime = tag.getLong("LastSpawnTime");
        long loadedInterval = tag.contains("NextInterval") ? tag.getLong("NextInterval") : DEFAULT_INTERVAL;
        if (loadedInterval > 24000L) {
            loadedInterval = DEFAULT_INTERVAL;
        }
        data.nextInterval = loadedInterval;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("LastSpawnTime", lastSpawnTime);
        tag.putLong("NextInterval", nextInterval);
        return tag;
    }

    public long getLastSpawnTime() {
        return lastSpawnTime;
    }

    public void setLastSpawnTime(long time) {
        this.lastSpawnTime = time;
        this.setDirty();
    }

    public long getNextInterval() {
        return nextInterval;
    }

    public void setNextInterval(long interval) {
        this.nextInterval = interval;
        this.setDirty();
    }
}
