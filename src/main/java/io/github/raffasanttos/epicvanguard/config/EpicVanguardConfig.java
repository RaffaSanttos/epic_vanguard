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

    public static long registryMemoryTicks() {
        return (loaded() ? REGISTRY_MEMORY_DAYS.get() : 100) * 24000L;
    }
}
