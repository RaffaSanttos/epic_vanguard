package com.example.epicvanguard.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public class VillageWarriorSavedData extends SavedData {
    private static final String DATA_NAME = "epicvanguard_village_warriors";

    private final Set<Long> spawnedVillageChunks = new HashSet<>();

    public static VillageWarriorSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(VillageWarriorSavedData::load, VillageWarriorSavedData::new, DATA_NAME);
    }

    public boolean hasSpawned(long chunkPosLong) {
        return spawnedVillageChunks.contains(chunkPosLong);
    }

    public void markSpawned(long chunkPosLong) {
        spawnedVillageChunks.add(chunkPosLong);
        setDirty();
    }

    public static VillageWarriorSavedData load(CompoundTag tag) {
        VillageWarriorSavedData data = new VillageWarriorSavedData();
        if (tag.contains("SpawnedVillages", 12)) {
            long[] array = tag.getLongArray("SpawnedVillages");
            for (long l : array) {
                data.spawnedVillageChunks.add(l);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        long[] array = spawnedVillageChunks.stream().mapToLong(Long::longValue).toArray();
        tag.putLongArray("SpawnedVillages", array);
        return tag;
    }
}
