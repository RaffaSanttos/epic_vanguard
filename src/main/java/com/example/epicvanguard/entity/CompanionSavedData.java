package com.example.epicvanguard.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CompanionSavedData extends SavedData {
    private static final String DATA_NAME = "grimal_companions";

    public enum FriendlyFireMode {
        DISABLED("Totalmente Livre de Fogo Amigo", "§a🛡 Livre de Fogo Amigo §7(Nenhum dano)"),
        SPELLS_ONLY("Apenas Magias", "§b✦ Apenas Magias §7(Magias acertam / Golpes bloqueados)"),
        MELEE_ONLY("Apenas Golpes Físicos / Epic Fight", "§e⚔ Apenas Golpes Físicos / Epic Fight §7(Golpes acertam / Magias bloqueadas)"),
        ALL("Totalmente Habilitado", "§c⚡ Totalmente Habilitado §7(Magias e Golpes acertam)");

        private final String name;
        private final String formatted;

        FriendlyFireMode(String name, String formatted) {
            this.name = name;
            this.formatted = formatted;
        }

        public String getName() {
            return name;
        }

        public String getFormatted() {
            return formatted;
        }

        public static FriendlyFireMode fromString(String input) {
            if (input == null) return DISABLED;
            String clean = input.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
            return switch (clean) {
                case "spells", "magias", "magia", "spell", "magico" -> SPELLS_ONLY;
                case "melee", "epicfight", "fisico", "golpes", "golpe", "ataques", "ataque", "corpoacorpo" -> MELEE_ONLY;
                case "all", "total", "on", "ativado", "habilitado", "sim", "full" -> ALL;
                default -> DISABLED; // "disabled", "off", "desativado", "livre", "none"
            };
        }
    }

    public static class CompanionInfo {
        public UUID companionUUID;
        public UUID ownerUUID;
        public String name;
        public String dimension;
        public BlockPos pos;
        public int combatMode; // 0: Agressivo, 1: Defensivo, 2: Guarda, 3: Ficar
        public float health;
        public float maxHealth;

        public CompanionInfo(UUID companionUUID, UUID ownerUUID, String name, String dimension, BlockPos pos, int combatMode, float health, float maxHealth) {
            this.companionUUID = companionUUID;
            this.ownerUUID = ownerUUID;
            this.name = name != null && !name.isEmpty() ? name : "Guerreiro";
            this.dimension = dimension;
            this.pos = pos;
            this.combatMode = combatMode;
            this.health = health;
            this.maxHealth = maxHealth;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            if (companionUUID != null) tag.putUUID("companion", companionUUID);
            if (ownerUUID != null) tag.putUUID("owner", ownerUUID);
            tag.putString("name", name != null ? name : "Guerreiro");
            tag.putString("dim", dimension != null ? dimension : "minecraft:overworld");
            tag.putLong("pos", pos != null ? pos.asLong() : 0L);
            tag.putInt("mode", combatMode);
            tag.putFloat("health", health);
            tag.putFloat("maxHealth", maxHealth);
            return tag;
        }

        public static CompanionInfo load(CompoundTag tag) {
            UUID companionUUID = tag.hasUUID("companion") ? tag.getUUID("companion") : null;
            UUID ownerUUID = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
            String name = tag.getString("name");
            String dim = tag.getString("dim");
            BlockPos pos = BlockPos.of(tag.getLong("pos"));
            int mode = tag.getInt("mode");
            float health = tag.getFloat("health");
            float maxHealth = tag.contains("maxHealth") ? tag.getFloat("maxHealth") : 20.0F;
            return new CompanionInfo(companionUUID, ownerUUID, name, dim, pos, mode, health, maxHealth);
        }
    }

    private final Map<UUID, CompanionInfo> companions = new ConcurrentHashMap<>();
    private FriendlyFireMode globalFriendlyFireMode = FriendlyFireMode.DISABLED;

    public static CompanionSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(CompanionSavedData::load, CompanionSavedData::new, DATA_NAME);
    }

    public void registerOrUpdate(UUID companionUUID, UUID ownerUUID, String name, String dimension, BlockPos pos, int mode, float health, float maxHealth) {
        if (companionUUID == null) return;
        companions.put(companionUUID, new CompanionInfo(companionUUID, ownerUUID, name, dimension, pos, mode, health, maxHealth));
        setDirty();
    }

    public void updatePosition(UUID companionUUID, String dimension, BlockPos pos) {
        CompanionInfo info = companions.get(companionUUID);
        if (info != null) {
            info.dimension = dimension;
            info.pos = pos;
            setDirty();
        }
    }

    public void updateHealth(UUID companionUUID, float health, float maxHealth) {
        CompanionInfo info = companions.get(companionUUID);
        if (info != null) {
            info.health = health;
            info.maxHealth = maxHealth;
            setDirty();
        }
    }

    public void updateCombatMode(UUID companionUUID, int mode) {
        CompanionInfo info = companions.get(companionUUID);
        if (info != null) {
            info.combatMode = mode;
            setDirty();
        }
    }

    public void unregister(UUID companionUUID) {
        if (companionUUID == null) return;
        companions.remove(companionUUID);
        setDirty();
    }

    public List<CompanionInfo> getPlayerCompanions(UUID owner) {
        List<CompanionInfo> list = new ArrayList<>();
        for (CompanionInfo info : companions.values()) {
            if (owner == null || owner.equals(info.ownerUUID)) {
                list.add(info);
            }
        }
        return list;
    }

    public CompanionInfo getCompanion(UUID companionUUID) {
        if (companionUUID == null) return null;
        return companions.get(companionUUID);
    }

    public FriendlyFireMode getGlobalFriendlyFireMode() {
        return globalFriendlyFireMode != null ? globalFriendlyFireMode : FriendlyFireMode.DISABLED;
    }

    public void setGlobalFriendlyFireMode(FriendlyFireMode mode) {
        if (mode == null) return;
        this.globalFriendlyFireMode = mode;
        setDirty();
    }

    public FriendlyFireMode getFriendlyFireMode(UUID ownerUUID) {
        return getGlobalFriendlyFireMode();
    }

    public void setFriendlyFireMode(UUID ownerUUID, FriendlyFireMode mode) {
        setGlobalFriendlyFireMode(mode);
    }

    public static CompanionSavedData load(CompoundTag tag) {
        CompanionSavedData data = new CompanionSavedData();
        ListTag list = tag.getList("Companions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag compTag = list.getCompound(i);
            CompanionInfo info = CompanionInfo.load(compTag);
            data.companions.put(info.companionUUID, info);
        }

        if (tag.contains("GlobalFriendlyFire", Tag.TAG_STRING)) {
            try {
                data.globalFriendlyFireMode = FriendlyFireMode.valueOf(tag.getString("GlobalFriendlyFire"));
            } catch (Exception ignored) {}
        } else if (tag.contains("FriendlyFire", Tag.TAG_COMPOUND)) {
            CompoundTag ffTag = tag.getCompound("FriendlyFire");
            for (String key : ffTag.getAllKeys()) {
                try {
                    String modeName = ffTag.getString(key);
                    data.globalFriendlyFireMode = FriendlyFireMode.valueOf(modeName);
                    break;
                } catch (Exception ignored) {}
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        ListTag list = new ListTag();
        for (CompanionInfo info : companions.values()) {
            list.add(info.save());
        }
        pCompoundTag.put("Companions", list);
        pCompoundTag.putString("GlobalFriendlyFire", getGlobalFriendlyFireMode().name());

        return pCompoundTag;
    }
}
