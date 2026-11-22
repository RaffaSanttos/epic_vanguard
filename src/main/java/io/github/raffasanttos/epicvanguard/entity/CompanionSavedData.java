package io.github.raffasanttos.epicvanguard.entity;

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
    public static final String DATA_NAME = "epicvanguard_companions";
    /** Nome usado até a 1.0.16; migrado em {@link #migrateLegacy}. */
    public static final String LEGACY_DATA_NAME = "grimal_companions";

    public enum FriendlyFireMode {
        DISABLED("disabled"),
        SPELLS_ONLY("spells_only"),
        MELEE_ONLY("melee_only"),
        ALL("all");

        private final String key;

        FriendlyFireMode(String key) {
            this.key = key;
        }

        /** Nome curto traduzível (lang: epicvanguard.friendly_fire.<modo>.name). */
        public net.minecraft.network.chat.Component getName() {
            return net.minecraft.network.chat.Component.translatable("epicvanguard.friendly_fire." + key + ".name");
        }

        /** Descrição colorida traduzível (lang: epicvanguard.friendly_fire.<modo>.formatted). */
        public net.minecraft.network.chat.Component getFormatted() {
            return net.minecraft.network.chat.Component.translatable("epicvanguard.friendly_fire." + key + ".formatted");
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
        public int combatMode; // WarriorCompanionEntity.MODE_FOLLOW (0), MODE_GUARD (1), MODE_STAY (2)
        public float health;
        public float maxHealth;
        public int level;
        public int specialization;
        public CompoundTag entityNbt;
        /**
         * Geração da cópia "oficial" do companheiro. É incrementada toda vez que ele é
         * rematerializado a partir do snapshot; cópias antigas (geração menor) que acordarem
         * em chunks descarregados são descartadas, evitando duplicação de itens.
         */
        public int generation;

        public CompanionInfo(UUID companionUUID, UUID ownerUUID, String name, String dimension, BlockPos pos, int combatMode, float health, float maxHealth, int level, int specialization) {
            this.companionUUID = companionUUID;
            this.ownerUUID = ownerUUID;
            this.name = name != null && !name.isEmpty() ? name : WarriorCompanionEntity.DEFAULT_NAME;
            this.dimension = dimension;
            this.pos = pos;
            this.combatMode = combatMode;
            this.health = health;
            this.maxHealth = maxHealth;
            this.level = Math.max(1, level);
            this.specialization = specialization;
            this.entityNbt = null;
        }

        public CompanionInfo(UUID companionUUID, UUID ownerUUID, String name, String dimension, BlockPos pos, int combatMode, float health, float maxHealth) {
            this(companionUUID, ownerUUID, name, dimension, pos, combatMode, health, maxHealth, 1, 0);
        }

        public net.minecraft.network.chat.Component getFormattedSpecializationTitle() {
            return WarriorCompanionEntity.specializationTitle(specialization);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            if (companionUUID != null) tag.putUUID("companion", companionUUID);
            if (ownerUUID != null) tag.putUUID("owner", ownerUUID);
            tag.putString("name", name != null ? name : WarriorCompanionEntity.DEFAULT_NAME);
            tag.putString("dim", dimension != null ? dimension : "minecraft:overworld");
            tag.putLong("pos", pos != null ? pos.asLong() : 0L);
            tag.putInt("mode", combatMode);
            tag.putFloat("health", health);
            tag.putFloat("maxHealth", maxHealth);
            tag.putInt("level", level);
            tag.putInt("spec", specialization);
            tag.putInt("gen", generation);
            if (entityNbt != null) {
                tag.put("entityNbt", entityNbt);
            }
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
            int level = tag.contains("level") ? tag.getInt("level") : 1;
            int spec = tag.contains("spec") ? tag.getInt("spec") : 0;
            CompanionInfo info = new CompanionInfo(companionUUID, ownerUUID, name, dim, pos, mode, health, maxHealth, level, spec);
            info.generation = tag.getInt("gen");
            if (tag.contains("entityNbt")) {
                info.entityNbt = tag.getCompound("entityNbt");
            }
            return info;
        }
    }

    private final Map<UUID, CompanionInfo> companions = new ConcurrentHashMap<>();
    /** Companheiros que morreram: cópias antigas deles em chunks descarregados não podem "ressuscitar". */
    private final Map<UUID, Long> retired = new ConcurrentHashMap<>();
    /**
     * Companheiros dispensados enquanto estavam em chunk descarregado: quando a entidade carregar,
     * ela ainda está marcada como recrutada no NBT e precisa ser liberada (senão voltaria ao registro).
     */
    private final Map<UUID, Long> dismissed = new ConcurrentHashMap<>();
    // Os mapas acima guardam o tempo de jogo (overworld) em que o UUID entrou; 0 = ainda sem data
    // (entrada de save antigo ou registrada antes da primeira manutenção). Ver pruneForgotten.
    private FriendlyFireMode globalFriendlyFireMode = FriendlyFireMode.DISABLED;

    public static CompanionSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(CompanionSavedData::load, () -> migrateLegacy(server), DATA_NAME);
    }

    /**
     * Mundo sem o arquivo novo: lê o do nome antigo ({@value #LEGACY_DATA_NAME}), se existir, e marca para salvar
     * com o nome novo. O arquivo antigo fica no disco como backup. Lido direto do arquivo (e não pelo
     * DimensionDataStorage) para não ficar em cache com os dois nomes.
     */
    public static CompanionSavedData migrateLegacy(MinecraftServer server) {
        java.nio.file.Path legacy = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("data").resolve(LEGACY_DATA_NAME + ".dat");
        if (!java.nio.file.Files.isRegularFile(legacy)) return new CompanionSavedData();
        try {
            CompoundTag root = net.minecraft.nbt.NbtIo.readCompressed(legacy.toFile());
            CompanionSavedData data = load(root.getCompound("data"));
            data.setDirty();
            org.slf4j.LoggerFactory.getLogger(CompanionSavedData.class).info(
                    "Migrated {} companions from {}.dat to {}.dat", data.companions.size(), LEGACY_DATA_NAME, DATA_NAME);
            return data;
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(CompanionSavedData.class).error(
                    "Could not read legacy companion data {}.dat; starting empty", LEGACY_DATA_NAME, e);
            return new CompanionSavedData();
        }
    }

    public void registerOrUpdate(UUID companionUUID, UUID ownerUUID, String name, String dimension, BlockPos pos, int mode, float health, float maxHealth, int level, int specialization, CompoundTag fullNbt) {
        if (companionUUID == null) return;
        // Foi recrutado de novo: não está mais dispensado
        CompanionInfo info = companions.get(companionUUID);
        boolean changed = dismissed.remove(companionUUID) != null;
        if (info == null) {
            info = new CompanionInfo(companionUUID, ownerUUID, name, dimension, pos, mode, health, maxHealth, level, specialization);
            companions.put(companionUUID, info);
            changed = true;
        } else {
            String newName = (name != null && !name.isEmpty()) ? name : info.name;
            if (!Objects.equals(info.ownerUUID, ownerUUID) || !Objects.equals(info.name, newName)
                    || !Objects.equals(info.dimension, dimension) || !Objects.equals(info.pos, pos)
                    || info.combatMode != mode || info.health != health || info.maxHealth != maxHealth
                    || info.level != level || info.specialization != specialization) {
                changed = true;
            }
            info.ownerUUID = ownerUUID;
            info.name = newName;
            info.dimension = dimension;
            info.pos = pos;
            info.combatMode = mode;
            info.health = health;
            info.maxHealth = maxHealth;
            info.level = level;
            info.specialization = specialization;
        }
        // Guerreiro parado (ex.: modo Parado num posto): o snapshot sai igual e o arquivo não precisa ser regravado
        if (fullNbt != null && !fullNbt.equals(info.entityNbt)) {
            info.entityNbt = fullNbt.copy();
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    public void registerOrUpdate(UUID companionUUID, UUID ownerUUID, String name, String dimension, BlockPos pos, int mode, float health, float maxHealth, int level, int specialization) {
        registerOrUpdate(companionUUID, ownerUUID, name, dimension, pos, mode, health, maxHealth, level, specialization, null);
    }

    public void registerOrUpdate(UUID companionUUID, UUID ownerUUID, String name, String dimension, BlockPos pos, int mode, float health, float maxHealth) {
        registerOrUpdate(companionUUID, ownerUUID, name, dimension, pos, mode, health, maxHealth, 1, 0, null);
    }

    public void updateName(UUID companionUUID, String name) {
        CompanionInfo info = companions.get(companionUUID);
        if (info != null && name != null && !name.isEmpty() && !name.equals(info.name)) {
            info.name = name;
            setDirty();
        }
    }

    public void updateLevelAndSpecialization(UUID companionUUID, int level, int specialization) {
        CompanionInfo info = companions.get(companionUUID);
        if (info != null && (info.level != level || info.specialization != specialization)) {
            info.level = level;
            info.specialization = specialization;
            setDirty();
        }
    }

    public void updatePosition(UUID companionUUID, String dimension, BlockPos pos) {
        CompanionInfo info = companions.get(companionUUID);
        if (info != null && (!Objects.equals(info.dimension, dimension) || !Objects.equals(info.pos, pos))) {
            info.dimension = dimension;
            info.pos = pos;
            setDirty();
        }
    }

    public void updateHealth(UUID companionUUID, float health, float maxHealth) {
        CompanionInfo info = companions.get(companionUUID);
        if (info != null && (info.health != health || info.maxHealth != maxHealth)) {
            info.health = health;
            info.maxHealth = maxHealth;
            setDirty();
        }
    }

    public void updateCombatMode(UUID companionUUID, int mode) {
        CompanionInfo info = companions.get(companionUUID);
        if (info != null && info.combatMode != mode) {
            info.combatMode = mode;
            setDirty();
        }
    }

    public void unregister(UUID companionUUID) {
        if (companionUUID == null) return;
        companions.remove(companionUUID);
        setDirty();
    }

    /** Remove o companheiro do registro e marca para liberar a entidade quando ela carregar. */
    public void dismiss(UUID companionUUID) {
        if (companionUUID == null) return;
        companions.remove(companionUUID);
        dismissed.put(companionUUID, 0L);
        setDirty();
    }

    public boolean isDismissed(UUID companionUUID) {
        return companionUUID != null && dismissed.containsKey(companionUUID);
    }

    /** A entidade dispensada já foi liberada: não precisa mais ser lembrada. */
    public void clearDismissed(UUID companionUUID) {
        if (companionUUID != null && dismissed.remove(companionUUID) != null) {
            setDirty();
        }
    }

    /** Remove o companheiro e marca o UUID como morto, para descartar cópias antigas que acordarem depois. */
    public void retire(UUID companionUUID) {
        if (companionUUID == null) return;
        companions.remove(companionUUID);
        retired.put(companionUUID, 0L);
        setDirty();
    }

    /**
     * Esquece mortos e dispensados registrados há mais de {@code memoryTicks}. Entradas sem data
     * recebem {@code gameTime} agora (contam a partir daqui). Chamado a cada minuto pelo
     * CompanionTeleportHandler. Devolve quantas entradas saíram.
     */
    public int pruneForgotten(long gameTime, long memoryTicks) {
        int removed = pruneMap(retired, gameTime, memoryTicks) + pruneMap(dismissed, gameTime, memoryTicks);
        return removed;
    }

    private int pruneMap(Map<UUID, Long> map, long gameTime, long memoryTicks) {
        int removed = 0;
        boolean changed = false;
        for (Iterator<Map.Entry<UUID, Long>> it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> e = it.next();
            long since = e.getValue();
            if (since <= 0L || since > gameTime) {
                e.setValue(gameTime);
                changed = true;
            } else if (gameTime - since > memoryTicks) {
                it.remove();
                removed++;
                changed = true;
            }
        }
        if (changed) setDirty();
        return removed;
    }

    public boolean isRetired(UUID companionUUID) {
        return companionUUID != null && retired.containsKey(companionUUID);
    }

    /** Marca uma nova cópia oficial do companheiro e devolve a geração dela. */
    public int bumpGeneration(CompanionInfo info) {
        info.generation++;
        setDirty();
        return info.generation;
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
            if (info.companionUUID != null) {
                data.companions.put(info.companionUUID, info);
            }
        }

        loadUuidTimes(tag, "Retired", data.retired);
        loadUuidTimes(tag, "Dismissed", data.dismissed);

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

    /**
     * Lê uma lista de UUIDs com data. Até a 1.0.18 a lista era só de UUIDs (int array); esses entram sem data
     * e passam a contar na primeira manutenção.
     */
    private static void loadUuidTimes(CompoundTag tag, String key, Map<UUID, Long> into) {
        if (!tag.contains(key, Tag.TAG_LIST)) return;
        ListTag list = (ListTag) tag.get(key);
        for (int i = 0; i < list.size(); i++) {
            Tag entry = list.get(i);
            if (entry instanceof CompoundTag c && c.hasUUID("id")) {
                into.put(c.getUUID("id"), c.getLong("since"));
            } else if (entry.getId() == Tag.TAG_INT_ARRAY) {
                into.put(net.minecraft.nbt.NbtUtils.loadUUID(entry), 0L);
            }
        }
    }

    private static ListTag saveUuidTimes(Map<UUID, Long> map) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Long> e : map.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            c.putLong("since", e.getValue());
            list.add(c);
        }
        return list;
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        ListTag list = new ListTag();
        for (CompanionInfo info : companions.values()) {
            list.add(info.save());
        }
        pCompoundTag.put("Companions", list);
        pCompoundTag.put("Retired", saveUuidTimes(retired));
        pCompoundTag.put("Dismissed", saveUuidTimes(dismissed));
        pCompoundTag.putString("GlobalFriendlyFire", getGlobalFriendlyFireMode().name());

        return pCompoundTag;
    }
}
