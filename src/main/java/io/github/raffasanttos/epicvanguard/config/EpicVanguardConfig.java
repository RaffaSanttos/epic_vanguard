package io.github.raffasanttos.epicvanguard.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Config comum ({@code config/epicvanguard-common.toml}). Vale para o servidor (e para o singleplayer).
 *
 * <p>Os getters devolvem o padrão se a config ainda não carregou (ex.: algo chamado muito cedo),
 * em vez de lançar exceção.
 */
public final class EpicVanguardConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.DoubleValue RECRUIT_COST_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue XP_MULTIPLIER;
    private static final ForgeConfigSpec.IntValue PLAYER_KILL_XP_RADIUS;
    private static final ForgeConfigSpec.IntValue WARCRY_COOLDOWN_SECONDS;
    private static final ForgeConfigSpec.IntValue CONTRACT_DAYS;
    private static final ForgeConfigSpec.DoubleValue FOLLOW_TELEPORT_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue COMBAT_LEASH_DISTANCE;
    private static final ForgeConfigSpec.IntValue FOLLOW_TELEPORT_DELAY_TICKS;
    private static final ForgeConfigSpec.IntValue REGISTRY_MEMORY_DAYS;
    private static final ForgeConfigSpec.IntValue CONTRACT_COINS;
    private static final ForgeConfigSpec.BooleanValue NATURAL_REGEN;
    private static final ForgeConfigSpec.IntValue REGEN_INTERVAL_TICKS;
    private static final ForgeConfigSpec.DoubleValue SHIELD_BLOCK_CHANCE;
    private static final ForgeConfigSpec.DoubleValue SHIELD_BLOCK_REDUCTION;
    private static final ForgeConfigSpec.DoubleValue DUELIST_DODGE_CHANCE;
    private static final ForgeConfigSpec.BooleanValue RANGED_COMBAT;
    private static final ForgeConfigSpec.BooleanValue INFINITE_ARROWS;
    private static final ForgeConfigSpec.DoubleValue RANGED_DAMAGE_MULTIPLIER;
    private static final ForgeConfigSpec.BooleanValue EQUIPMENT_WEAR;
    private static final ForgeConfigSpec.DoubleValue WEAR_MULTIPLIER;
    private static final ForgeConfigSpec.BooleanValue WARN_ON_BREAK;
    private static final ForgeConfigSpec.DoubleValue LOW_DURABILITY_WARNING;
    private static final ForgeConfigSpec.BooleanValue FIELD_REPAIR;
    private static final ForgeConfigSpec.BooleanValue AUTO_SWAP_BROKEN;
    private static final ForgeConfigSpec.IntValue BANNER_AURA_RADIUS;
    private static final ForgeConfigSpec.BooleanValue MOUNTS;
    private static final ForgeConfigSpec.IntValue MOUNT_SEARCH_RADIUS;
    private static final ForgeConfigSpec.BooleanValue OUTPOST_PRISONERS;
    private static final ForgeConfigSpec.BooleanValue WARRIOR_HOUSE_SPAWNS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Recruiting and progression").push("progression");
        RECRUIT_COST_MULTIPLIER = b
                .comment("Multiplier for the coin cost to recruit a mercenary (1.0 = 20-30 / 40-50 / 60-80 coins by gear tier).")
                .defineInRange("recruitCostMultiplier", 1.0D, 0.0D, 10.0D);
        XP_MULTIPLIER = b
                .comment("Multiplier for the XP companions get from kills and assists.")
                .defineInRange("xpMultiplier", 1.0D, 0.0D, 10.0D);
        PLAYER_KILL_XP_RADIUS = b
                .comment("When the owner kills a monster, own companions within this many blocks share the XP.")
                .defineInRange("playerKillXpRadius", 24, 0, 128);
        WARCRY_COOLDOWN_SECONDS = b
                .comment("Cooldown of the war cry, in seconds.")
                .defineInRange("warcryCooldownSeconds", 50, 1, 3600);
        b.pop();

        b.comment("Vanguard Point").push("vanguardPoint");
        CONTRACT_DAYS = b
                .comment("How long a posted contract lasts, in in-game days (1 day = 20 minutes). Applies to new contracts.")
                .defineInRange("contractDays", 7, 1, 100);
        CONTRACT_COINS = b
                .comment("Gold coins needed to post a contract.")
                .defineInRange("contractCoins", 20, 1, 64);
        b.pop();

        b.comment("Companions").push("companions");
        NATURAL_REGEN = b
                .comment("Companions slowly regenerate health out of combat. Off by default: they heal with food, potions and campfires.")
                .define("naturalRegen", false);
        REGEN_INTERVAL_TICKS = b
                .comment("Ticks between each point of natural regeneration (20 = 1 second).")
                .defineInRange("regenIntervalTicks", 60, 5, 1200);
        b.pop();

        b.comment("Combat").push("combat");
        SHIELD_BLOCK_CHANCE = b
                .comment("Chance (0-1) that a companion with a shield parries a hit from an entity.")
                .defineInRange("shieldBlockChance", 0.70D, 0.0D, 1.0D);
        SHIELD_BLOCK_REDUCTION = b
                .comment("Damage reduction when the shield parries (0.8 = 80%).")
                .defineInRange("shieldBlockReduction", 0.80D, 0.0D, 1.0D);
        DUELIST_DODGE_CHANCE = b
                .comment("Chance (0-1) that a Duelist dodges a hit completely.")
                .defineInRange("duelistDodgeChance", 0.18D, 0.0D, 1.0D);
        b.pop();

        b.comment("Ranged combat (bows and crossbows)").push("ranged");
        RANGED_COMBAT = b
                .comment("Companions with a bow or crossbow in the main hand and arrows in the backpack fight at range.")
                .define("enableRangedCombat", true);
        INFINITE_ARROWS = b
                .comment("Companions do not need arrows in the backpack.")
                .define("infiniteArrows", false);
        RANGED_DAMAGE_MULTIPLIER = b
                .comment("Multiplier for arrow damage shot by companions.")
                .defineInRange("rangedDamageMultiplier", 1.0D, 0.0D, 10.0D);
        b.pop();

        b.comment("Equipment durability").push("equipment");
        EQUIPMENT_WEAR = b
                .comment("Armor, weapons and shields of companions lose durability.")
                .define("equipmentWear", true);
        WEAR_MULTIPLIER = b
                .comment("Multiplier for durability loss (1.0 = same as a player).")
                .defineInRange("wearMultiplier", 1.0D, 0.0D, 10.0D);
        WARN_ON_BREAK = b
                .comment("Tell the owner when a companion's item breaks or is swapped.")
                .define("warnOnBreak", true);
        LOW_DURABILITY_WARNING = b
                .comment("Warn the owner once when an equipped item drops below this fraction of durability (0 = off).")
                .defineInRange("lowDurabilityWarning", 0.10D, 0.0D, 0.5D);
        FIELD_REPAIR = b
                .comment("Resting companions (sitting at a campfire or tavern) repair their gear with materials from the backpack.")
                .define("fieldRepair", true);
        AUTO_SWAP_BROKEN = b
                .comment("When an item breaks, equip a replacement from the backpack.")
                .define("autoSwapBroken", true);
        b.pop();

        b.comment("Specializations").push("specializations");
        BANNER_AURA_RADIUS = b
                .comment("Standard-bearer aura radius in blocks.")
                .defineInRange("bannerAuraRadius", 8, 2, 32);
        b.pop();

        b.comment("Mounts").push("mounts");
        MOUNTS = b
                .comment("Companions in Follow stance mount the owner's tamed, saddled horses when the owner rides.")
                .define("enableMounts", true);
        MOUNT_SEARCH_RADIUS = b
                .comment("How far a companion looks for a free horse.")
                .defineInRange("mountSearchRadius", 16, 4, 48);
        b.pop();

        b.comment("World generation").push("spawning");
        OUTPOST_PRISONERS = b
                .comment("Pillager outposts get a caged prisoner warrior.")
                .define("outpostPrisoners", true);
        WARRIOR_HOUSE_SPAWNS = b
                .comment("Warrior houses get a mercenary.")
                .define("warriorHouseSpawns", true);
        b.pop();

        b.comment("Follow stance").push("follow");
        FOLLOW_TELEPORT_DISTANCE = b
                .comment("Blocks behind the owner, out of combat, before a following companion teleports.")
                .defineInRange("followTeleportDistance", 13.0D, 4.0D, 64.0D);
        COMBAT_LEASH_DISTANCE = b
                .comment("Blocks from the owner at which a companion teleports even while fighting.",
                        "Must be bigger than followTeleportDistance.")
                .defineInRange("combatLeashDistance", 32.0D, 8.0D, 128.0D);
        FOLLOW_TELEPORT_DELAY_TICKS = b
                .comment("Ticks (20 = 1 second) the companion has to stay behind before teleporting.")
                .defineInRange("followTeleportDelayTicks", 40, 0, 1200);
        b.pop();

        b.comment("Companion registry (save file)").push("registry");
        REGISTRY_MEMORY_DAYS = b
                .comment("In-game days the registry remembers dead and dismissed companions, so old copies in",
                        "unloaded chunks are removed when those chunks load. Lower values keep the save smaller;",
                        "an old copy found after this is no longer recognized.")
                .defineInRange("registryMemoryDays", 100, 1, 10000);
        b.pop();

        SPEC = b.build();
    }

    private EpicVanguardConfig() {}

    private static boolean loaded() {
        return SPEC.isLoaded();
    }

    public static double recruitCostMultiplier() {
        return loaded() ? RECRUIT_COST_MULTIPLIER.get() : 1.0D;
    }

    public static double xpMultiplier() {
        return loaded() ? XP_MULTIPLIER.get() : 1.0D;
    }

    public static double playerKillXpRadius() {
        return loaded() ? PLAYER_KILL_XP_RADIUS.get() : 24;
    }

    public static int warcryCooldownTicks() {
        return (loaded() ? WARCRY_COOLDOWN_SECONDS.get() : 50) * 20;
    }

    public static int contractTicks() {
        return (loaded() ? CONTRACT_DAYS.get() : 7) * 24000;
    }

    public static double followTeleportDistance() {
        return loaded() ? FOLLOW_TELEPORT_DISTANCE.get() : 13.0D;
    }

    public static double combatLeashDistance() {
        double leash = loaded() ? COMBAT_LEASH_DISTANCE.get() : 32.0D;
        return Math.max(leash, followTeleportDistance() + 1.0D);
    }

    public static int followTeleportDelayTicks() {
        return loaded() ? FOLLOW_TELEPORT_DELAY_TICKS.get() : 40;
    }

    public static int contractCoins() { return loaded() ? CONTRACT_COINS.get() : 20; }
    public static boolean naturalRegen() { return loaded() ? NATURAL_REGEN.get() : false; }
    public static int regenIntervalTicks() { return loaded() ? REGEN_INTERVAL_TICKS.get() : 60; }
    public static float shieldBlockChance() { return loaded() ? SHIELD_BLOCK_CHANCE.get().floatValue() : 0.70F; }
    public static float shieldBlockReduction() { return loaded() ? SHIELD_BLOCK_REDUCTION.get().floatValue() : 0.80F; }
    public static float duelistDodgeChance() { return loaded() ? DUELIST_DODGE_CHANCE.get().floatValue() : 0.18F; }
    public static boolean rangedCombat() { return loaded() ? RANGED_COMBAT.get() : true; }
    public static boolean infiniteArrows() { return loaded() ? INFINITE_ARROWS.get() : false; }
    public static double rangedDamageMultiplier() { return loaded() ? RANGED_DAMAGE_MULTIPLIER.get() : 1.0D; }
    public static boolean equipmentWear() { return loaded() ? EQUIPMENT_WEAR.get() : true; }
    public static double wearMultiplier() { return loaded() ? WEAR_MULTIPLIER.get() : 1.0D; }
    public static boolean warnOnBreak() { return loaded() ? WARN_ON_BREAK.get() : true; }
    public static double lowDurabilityWarning() { return loaded() ? LOW_DURABILITY_WARNING.get() : 0.10D; }
    public static boolean fieldRepair() { return loaded() ? FIELD_REPAIR.get() : true; }
    public static boolean autoSwapBroken() { return loaded() ? AUTO_SWAP_BROKEN.get() : true; }
    public static int bannerAuraRadius() { return loaded() ? BANNER_AURA_RADIUS.get() : 8; }
    public static boolean mounts() { return loaded() ? MOUNTS.get() : true; }
    public static int mountSearchRadius() { return loaded() ? MOUNT_SEARCH_RADIUS.get() : 16; }
    public static boolean outpostPrisoners() { return loaded() ? OUTPOST_PRISONERS.get() : true; }
    public static boolean warriorHouseSpawns() { return loaded() ? WARRIOR_HOUSE_SPAWNS.get() : true; }

    public static long registryMemoryTicks() {
        return (loaded() ? REGISTRY_MEMORY_DAYS.get() : 100) * 24000L;
    }
}
