package io.github.raffasanttos.epicvanguard.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Estruturas que já geraram seu guerreiro (casa do guerreiro, prisioneiro do posto de saqueadores).
 * A chave é a dimensão + o chunk de início da estrutura, para que duas dimensões com uma estrutura
 * no mesmo chunk não se bloqueiem.
 *
 * <p>Até a 1.0.18 só o chunk era salvo ({@code SpawnedVillages}); esses chunks são lidos como do overworld.
 */
public class VillageWarriorSavedData extends SavedData {
    private static final String DATA_NAME = "epicvanguard_village_warriors";
    private static final String LEGACY_KEY = "SpawnedVillages";
    private static final String KEY = "SpawnedByDimension";

    private final Map<String, Set<Long>> spawnedByDimension = new HashMap<>();

    public static VillageWarriorSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(VillageWarriorSavedData::load, VillageWarriorSavedData::new, DATA_NAME);
    }

    private static String dimensionId(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }

    public boolean hasSpawned(ResourceKey<Level> dimension, long chunkPosLong) {
        Set<Long> set = spawnedByDimension.get(dimensionId(dimension));
        return set != null && set.contains(chunkPosLong);
    }

    public void markSpawned(ResourceKey<Level> dimension, long chunkPosLong) {
        if (spawnedByDimension.computeIfAbsent(dimensionId(dimension), k -> new HashSet<>()).add(chunkPosLong)) {
            setDirty();
        }
    }

    public static VillageWarriorSavedData load(CompoundTag tag) {
        VillageWarriorSavedData data = new VillageWarriorSavedData();
        if (tag.contains(KEY, Tag.TAG_COMPOUND)) {
            CompoundTag dims = tag.getCompound(KEY);
            for (String dim : dims.getAllKeys()) {
                Set<Long> set = data.spawnedByDimension.computeIfAbsent(dim, k -> new HashSet<>());
                for (long l : dims.getLongArray(dim)) set.add(l);
            }
        }
        if (tag.contains(LEGACY_KEY, Tag.TAG_LONG_ARRAY)) {
            Set<Long> overworld = data.spawnedByDimension.computeIfAbsent(dimensionId(Level.OVERWORLD), k -> new HashSet<>());
            for (long l : tag.getLongArray(LEGACY_KEY)) overworld.add(l);
            data.setDirty(); // grava no formato novo
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag dims = new CompoundTag();
        for (Map.Entry<String, Set<Long>> e : spawnedByDimension.entrySet()) {
            dims.putLongArray(e.getKey(), e.getValue().stream().mapToLong(Long::longValue).toArray());
        }
        tag.put(KEY, dims);
        return tag;
    }
}
