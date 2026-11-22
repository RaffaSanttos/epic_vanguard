package io.github.raffasanttos.epicvanguard.command;

import io.github.raffasanttos.epicvanguard.entity.CompanionSavedData;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.UUID;

public class VanguardCommand {

    private static final SuggestionProvider<CommandSourceStack> TARGET_SUGGESTIONS = (ctx, builder) -> {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            CompanionSavedData data = CompanionSavedData.get(ctx.getSource().getServer());
            List<CompanionSavedData.CompanionInfo> companions = data.getPlayerCompanions(player.getUUID());
            for (int i = 0; i < companions.size(); i++) {
                CompanionSavedData.CompanionInfo c = companions.get(i);
                builder.suggest("#" + (i + 1));
                if (c.name != null && !c.name.isEmpty()) {
                    builder.suggest(c.name.contains(" ") ? "\"" + c.name + "\"" : c.name);
                }
                if (c.companionUUID != null) {
                    builder.suggest(c.companionUUID.toString());
                }
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> FRIENDLY_FIRE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(List.of("off", "melee", "spells", "all", "livre", "fisico", "magias", "total"), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Registra comando principal em Português
        dispatcher.register(buildRootCommand("companhias"));
        dispatcher.register(buildRootCommand("companhia"));
        dispatcher.register(buildRootCommand("companions"));
        dispatcher.register(buildRootCommand("companion"));

        // Registra comando com o nome do Mod
        dispatcher.register(buildRootCommand("vanguard"));
        dispatcher.register(buildRootCommand("epicvanguard"));

        // Atalhos diretos para comando de Fogo Amigo
        dispatcher.register(Commands.literal("fogoamigo")
                .executes(context -> getFriendlyFire(context.getSource()))
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests(FRIENDLY_FIRE_SUGGESTIONS)
                        .executes(context -> setFriendlyFire(context.getSource(), StringArgumentType.getString(context, "mode"))))
        );
        dispatcher.register(Commands.literal("friendlyfire")
                .executes(context -> getFriendlyFire(context.getSource()))
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests(FRIENDLY_FIRE_SUGGESTIONS)
                        .executes(context -> setFriendlyFire(context.getSource(), StringArgumentType.getString(context, "mode"))))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRootCommand(String rootName) {
        return Commands.literal(rootName)
                .executes(context -> listCompanions(context.getSource()))
                .then(Commands.literal("listar")
                        .executes(context -> listCompanions(context.getSource())))
                .then(Commands.literal("list")
                        .executes(context -> listCompanions(context.getSource())))
                .then(Commands.literal("chamar")
                        .executes(context -> summonAllCompanions(context.getSource())))
                .then(Commands.literal("call")
                        .executes(context -> summonAllCompanions(context.getSource())))
                .then(Commands.literal("summon")
                        .executes(context -> summonAllCompanions(context.getSource())))
                .then(Commands.literal("tp")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> teleportCompanionToPlayer(context.getSource(), StringArgumentType.getString(context, "target")))))
                .then(Commands.literal("trazer")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> teleportCompanionToPlayer(context.getSource(), StringArgumentType.getString(context, "target")))))
                .then(Commands.literal("bring")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> teleportCompanionToPlayer(context.getSource(), StringArgumentType.getString(context, "target")))))
                .then(Commands.literal("ir")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> teleportPlayerToCompanion(context.getSource(), StringArgumentType.getString(context, "target")))))
                .then(Commands.literal("goto")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> teleportPlayerToCompanion(context.getSource(), StringArgumentType.getString(context, "target")))))
                .then(Commands.literal("dispensar")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> removeCompanion(context.getSource(), StringArgumentType.getString(context, "target")))))
                .then(Commands.literal("remover")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> removeCompanion(context.getSource(), StringArgumentType.getString(context, "target")))))
                .then(Commands.literal("dismiss")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> removeCompanion(context.getSource(), StringArgumentType.getString(context, "target")))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> removeCompanion(context.getSource(), StringArgumentType.getString(context, "target")))))
                .then(Commands.literal("renomear")
                        .then(Commands.argument("target", StringArgumentType.string())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> renameSingleCompanion(context.getSource(), StringArgumentType.getString(context, "target")))
                                .then(Commands.argument("new_name", StringArgumentType.greedyString())
                                        .executes(context -> renameCompanion(context.getSource(), StringArgumentType.getString(context, "target"), StringArgumentType.getString(context, "new_name"))))))
                .then(Commands.literal("rename")
                        .then(Commands.argument("target", StringArgumentType.string())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> renameSingleCompanion(context.getSource(), StringArgumentType.getString(context, "target")))
                                .then(Commands.argument("new_name", StringArgumentType.greedyString())
                                        .executes(context -> renameCompanion(context.getSource(), StringArgumentType.getString(context, "target"), StringArgumentType.getString(context, "new_name"))))))
                .then(Commands.literal("limpar")
                        .executes(context -> cleanInactiveCompanions(context.getSource())))
                .then(Commands.literal("clean")
                        .executes(context -> cleanInactiveCompanions(context.getSource())))
                .then(Commands.literal("fogoamigo")
                        .executes(context -> getFriendlyFire(context.getSource()))
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(FRIENDLY_FIRE_SUGGESTIONS)
                                .executes(context -> setFriendlyFire(context.getSource(), StringArgumentType.getString(context, "mode")))))
                .then(Commands.literal("friendlyfire")
                        .executes(context -> getFriendlyFire(context.getSource()))
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(FRIENDLY_FIRE_SUGGESTIONS)
                                .executes(context -> setFriendlyFire(context.getSource(), StringArgumentType.getString(context, "mode")))));
    }

    /**
     * Pode mexer nos companheiros de outros jogadores. Só operadores: antes qualquer jogador em criativo
     * passava, e num servidor com criativo liberado isso deixava trazer/renomear/dispensar companheiros alheios.
     */
    public static boolean hasAdminAccess(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    private static int getFriendlyFire(CommandSourceStack source) {
        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.FriendlyFireMode currentMode = data.getGlobalFriendlyFireMode();
        source.sendSuccess(() -> Component.translatable("epicvanguard.text.vanguard_command.1", currentMode.getFormatted()), false);
        source.sendSuccess(() -> Component.translatable("epicvanguard.text.vanguard_command.2"), false);
        return 1;
    }

    private static int setFriendlyFire(CommandSourceStack source, String modoStr) {
        // O modo de fogo amigo é global (vale para todos os jogadores): só operadores podem alterar
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.translatable("epicvanguard.text.vanguard_command.no_permission"));
            return 0;
        }
        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.FriendlyFireMode mode = CompanionSavedData.FriendlyFireMode.fromString(modoStr);
        data.setGlobalFriendlyFireMode(mode);
        source.sendSuccess(() -> Component.translatable("epicvanguard.text.vanguard_command.3", mode.getFormatted()), true);
        return 1;
    }

    private static int listCompanions(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("epicvanguard.text.vanguard_command.4"));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        List<CompanionSavedData.CompanionInfo> companions = data.getPlayerCompanions(player.getUUID());

        if (companions.isEmpty()) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.5"));
            return 1;
        }

        player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.6", companions.size()));

        for (int i = 0; i < companions.size(); i++) {
            CompanionSavedData.CompanionInfo info = companions.get(i);
            String dimName = formatDimName(info.dimension);

            WarriorCompanionEntity liveEntity = findLoadedCompanion(source.getServer(), info.companionUUID);
            if (liveEntity != null && !liveEntity.getWarriorName().isEmpty()) {
                info.name = liveEntity.getWarriorName();
                info.pos = liveEntity.blockPosition();
                info.dimension = liveEntity.level().dimension().location().toString();
                info.combatMode = liveEntity.getCombatMode();
                info.health = liveEntity.getHealth();
                info.maxHealth = liveEntity.getMaxHealth();
                info.level = liveEntity.getWarriorLevel();
                info.specialization = liveEntity.getSpecialization();
                data.setDirty();
            }

            String displayName = (liveEntity != null && !liveEntity.getWarriorName().isEmpty()) ? liveEntity.getWarriorName() : info.name;
            BlockPos currentPos = liveEntity != null ? liveEntity.blockPosition() : info.pos;
            String currentDim = liveEntity != null ? liveEntity.level().dimension().location().toString() : info.dimension;
            int currentMode = liveEntity != null ? liveEntity.getCombatMode() : info.combatMode;
            float currentHp = liveEntity != null ? liveEntity.getHealth() : info.health;
            float maxHp = liveEntity != null ? liveEntity.getMaxHealth() : info.maxHealth;
            int companionLvl = liveEntity != null ? liveEntity.getWarriorLevel() : Math.max(1, info.level);
            Component specTitle = liveEntity != null ? liveEntity.getFormattedSpecializationTitle() : info.getFormattedSpecializationTitle();

            Component modeName = switch (currentMode) {
                case 1 -> Component.translatable("epicvanguard.mode.guard");
                case 2 -> Component.translatable("epicvanguard.mode.stay");
                default -> Component.translatable("epicvanguard.mode.follow");
            };

            Component distText = Component.empty();
            if (currentPos != null && player.level().dimension().location().toString().equals(currentDim)) {
                double dist = Math.sqrt(player.blockPosition().distSqr(currentPos));
                distText = Component.translatable("epicvanguard.text.vanguard_command.distance", (int) dist);
            }

            String hpText = " §c " + (int) currentHp + "/" + (int) maxHp;

            int posX = currentPos != null ? currentPos.getX() : 0;
            int posY = currentPos != null ? currentPos.getY() : 0;
            int posZ = currentPos != null ? currentPos.getZ() : 0;

            MutableComponent line = Component.translatable("epicvanguard.text.vanguard_command.7", (i + 1), specTitle, displayName, companionLvl, hpText)
                    .append(Component.translatable("epicvanguard.text.vanguard_command.8", posX, posY, posZ, dimName, distText))
                    .append(Component.translatable("epicvanguard.text.vanguard_command.9", modeName));

            if (info.companionUUID != null) {
                String uuidStr = info.companionUUID.toString();

                String tpBringCmd = "/companions bring " + uuidStr;
                MutableComponent btnBring = Component.translatable("epicvanguard.text.vanguard_command.10")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpBringCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("epicvanguard.text.vanguard_command.11", displayName))));

                String tpGoCmd = "/companions goto " + uuidStr;
                MutableComponent btnGo = Component.translatable("epicvanguard.text.vanguard_command.12")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpGoCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("epicvanguard.text.vanguard_command.13", displayName))));

                String renameCmd = "/companions rename #" + (i + 1) + " ";
                MutableComponent btnRename = Component.translatable("epicvanguard.text.vanguard_command.14")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, renameCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("epicvanguard.text.vanguard_command.15", displayName))));

                String removeCmd = "/companions dismiss " + uuidStr;
                MutableComponent btnDismiss = Component.translatable("epicvanguard.text.vanguard_command.16")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, removeCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("epicvanguard.text.vanguard_command.17", displayName))));

                line.append(btnBring).append(btnGo).append(btnRename).append(btnDismiss);
            }

            player.sendSystemMessage(line);
        }

        MutableComponent footer = Component.literal("§6==================================================");
        footer.append(Component.translatable("epicvanguard.text.vanguard_command.18")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/companions call"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("epicvanguard.text.vanguard_command.19")))));

        footer.append(Component.translatable("epicvanguard.text.vanguard_command.20")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/companions clean"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("epicvanguard.text.vanguard_command.21")))));

        footer.append(Component.translatable("epicvanguard.text.vanguard_command.22")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friendlyfire"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("epicvanguard.text.vanguard_command.23")))));

        player.sendSystemMessage(footer);

        return 1;
    }

    private static CompanionSavedData.CompanionInfo findCompanionInfo(CompanionSavedData data, ServerPlayer player, String targetIdentifier, boolean isOp) {
        if (targetIdentifier == null || targetIdentifier.trim().isEmpty()) return null;
        targetIdentifier = targetIdentifier.trim().replace("\"", "");

        List<CompanionSavedData.CompanionInfo> playerCompanions = data.getPlayerCompanions(player.getUUID());

        try {
            UUID uuid = UUID.fromString(targetIdentifier);
            CompanionSavedData.CompanionInfo info = data.getCompanion(uuid);
            if (info != null) return info;
        } catch (IllegalArgumentException ignored) {}

        String numStr = targetIdentifier.startsWith("#") ? targetIdentifier.substring(1) : targetIdentifier;
        try {
            int index = Integer.parseInt(numStr) - 1;
            if (index >= 0 && index < playerCompanions.size()) {
                return playerCompanions.get(index);
            }
        } catch (NumberFormatException ignored) {}

        for (CompanionSavedData.CompanionInfo c : playerCompanions) {
            if (c.name != null && c.name.equalsIgnoreCase(targetIdentifier)) {
                return c;
            }
        }

        if (isOp) {
            for (CompanionSavedData.CompanionInfo c : data.getPlayerCompanions(null)) {
                if (c.name != null && c.name.equalsIgnoreCase(targetIdentifier)) {
                    return c;
                }
            }
        }

        return null;
    }

    private static int teleportCompanionToPlayer(CommandSourceStack source, String targetIdentifier) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("epicvanguard.text.vanguard_command.4"));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.CompanionInfo info = findCompanionInfo(data, player, targetIdentifier, source.hasPermission(2));

        if (info == null) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.24", targetIdentifier));
            return 0;
        }

        if (!player.getUUID().equals(info.ownerUUID) && !hasAdminAccess(source)) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.25"));
            return 0;
        }

        WarriorCompanionEntity entity = findLoadedCompanion(source.getServer(), info.companionUUID);
        if (entity == null) {
            entity = io.github.raffasanttos.epicvanguard.event.CompanionTeleportHandler.restoreCompanionAtPlayer(player, (ServerLevel) player.level(), info);
        }
        if (entity != null) {
            entity = entity.safeTeleportTo(player);
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.26", entity.getWarriorName()));
            return 1;
        } else {
            boolean isNear = player.level().dimension().location().toString().equals(info.dimension)
                    && info.pos != null && player.blockPosition().distSqr(info.pos) < 64 * 64;

            if (isNear) {
                String removeCmd = "/companions dismiss " + info.companionUUID;
                MutableComponent btnRemove = Component.translatable("epicvanguard.text.vanguard_command.27")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, removeCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("epicvanguard.text.vanguard_command.28"))));

                player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.29", info.name).append(btnRemove));
            } else {
                player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.30", info.name, (info.pos != null ? info.pos.getX() + ", " + info.pos.getY() + ", " + info.pos.getZ() : Component.translatable("epicvanguard.text.unknown"))));
            }
            return 1;
        }
    }

    private static int teleportPlayerToCompanion(CommandSourceStack source, String targetIdentifier) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("epicvanguard.text.vanguard_command.4"));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.CompanionInfo info = findCompanionInfo(data, player, targetIdentifier, source.hasPermission(2));

        if (info == null) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.24", targetIdentifier));
            return 0;
        }

        if (!player.getUUID().equals(info.ownerUUID) && !hasAdminAccess(source)) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.25"));
            return 0;
        }

        ServerLevel targetLevel = null;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            if (level.dimension().location().toString().equals(info.dimension)) {
                targetLevel = level;
                break;
            }
        }

        if (targetLevel == null) {
            targetLevel = player.serverLevel();
        }

        WarriorCompanionEntity entity = findLoadedCompanion(source.getServer(), info.companionUUID);
        double targetX = entity != null ? entity.getX() : (info.pos != null ? info.pos.getX() + 0.5D : player.getX());
        double targetY = entity != null ? entity.getY() : (info.pos != null ? info.pos.getY() : player.getY());
        double targetZ = entity != null ? entity.getZ() : (info.pos != null ? info.pos.getZ() + 0.5D : player.getZ());
        float yRot = player.getYRot();
        float xRot = player.getXRot();

        player.teleportTo(targetLevel, targetX, targetY, targetZ, yRot, xRot);
        String name = entity != null ? entity.getWarriorName() : info.name;
        player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.31", name));

        targetLevel.sendParticles(ParticleTypes.PORTAL, targetX, targetY + 1.0D, targetZ, 30, 0.5D, 0.5D, 0.5D, 0.1D);
        targetLevel.playSound(null, targetX, targetY, targetZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        return 1;
    }

    private static int removeCompanion(CommandSourceStack source, String targetIdentifier) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("epicvanguard.text.vanguard_command.4"));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.CompanionInfo info = findCompanionInfo(data, player, targetIdentifier, source.hasPermission(2));

        if (info == null) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.24", targetIdentifier));
            return 0;
        }

        if (!player.getUUID().equals(info.ownerUUID) && !hasAdminAccess(source)) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.25"));
            return 0;
        }

        WarriorCompanionEntity live = findLoadedCompanion(source.getServer(), info.companionUUID);
        if (live != null) {
            live.setRecruited(false);
            live.setOwnerUUID(null);
            data.unregister(info.companionUUID);
        } else {
            // Entidade em chunk descarregado: será liberada quando carregar (CompanionTeleportHandler)
            data.dismiss(info.companionUUID);
        }
        player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.32", info.name));
        return 1;
    }

    private static int cleanInactiveCompanions(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("epicvanguard.text.vanguard_command.4"));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        List<CompanionSavedData.CompanionInfo> companions = data.getPlayerCompanions(player.getUUID());

        int removed = 0;
        for (CompanionSavedData.CompanionInfo info : companions) {
            WarriorCompanionEntity live = findLoadedCompanion(source.getServer(), info.companionUUID);
            if (live == null) {
                boolean isNear = player.level().dimension().location().toString().equals(info.dimension)
                        && info.pos != null && player.blockPosition().distSqr(info.pos) < 64 * 64;
                if (isNear) {
                    data.unregister(info.companionUUID);
                    removed++;
                }
            }
        }

        if (removed > 0) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.33", removed));
        } else {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.34"));
        }
        return 1;
    }

    private static int summonAllCompanions(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("epicvanguard.text.vanguard_command.4"));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        List<CompanionSavedData.CompanionInfo> companions = data.getPlayerCompanions(player.getUUID());

        if (companions.isEmpty()) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.35"));
            return 1;
        }

        int count = 0;
        for (CompanionSavedData.CompanionInfo info : companions) {
            WarriorCompanionEntity entity = findLoadedCompanion(source.getServer(), info.companionUUID);
            if (entity == null) {
                entity = io.github.raffasanttos.epicvanguard.event.CompanionTeleportHandler.restoreCompanionAtPlayer(player, (ServerLevel) player.level(), info);
            }
            if (entity != null) {
                entity.safeTeleportTo(player);
                count++;
            }
        }

        player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.36", count, companions.size()));
        return 1;
    }

    private static WarriorCompanionEntity findLoadedCompanion(net.minecraft.server.MinecraftServer server, UUID uuid) {
        if (uuid == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e instanceof WarriorCompanionEntity warrior && warrior.isAlive()) {
                return warrior;
            }
        }
        return null;
    }

    private static int renameSingleCompanion(CommandSourceStack source, String newName) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("epicvanguard.text.vanguard_command.4"));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        List<CompanionSavedData.CompanionInfo> playerCompanions = data.getPlayerCompanions(player.getUUID());

        if (playerCompanions.isEmpty()) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.37"));
            return 0;
        }

        if (playerCompanions.size() == 1) {
            return renameCompanion(source, "#1", newName);
        }

        player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.38", newName));
        return 0;
    }

    private static int renameCompanion(CommandSourceStack source, String targetIdentifier, String newName) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("epicvanguard.text.vanguard_command.4"));
            return 0;
        }

        if (newName == null || newName.trim().isEmpty()) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.39"));
            return 0;
        }

        String cleanName = newName.replaceAll("§[0-9a-fk-or]", "").trim();
        if (cleanName.length() > 20) {
            cleanName = cleanName.substring(0, 20);
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.CompanionInfo info = findCompanionInfo(data, player, targetIdentifier, source.hasPermission(2));

        if (info == null) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.24", targetIdentifier));
            return 0;
        }

        if (!player.getUUID().equals(info.ownerUUID) && !hasAdminAccess(source)) {
            player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.25"));
            return 0;
        }

        String oldName = info.name;
        info.name = cleanName;
        data.setDirty();

        WarriorCompanionEntity live = findLoadedCompanion(source.getServer(), info.companionUUID);
        if (live != null) {
            live.setWarriorName(cleanName);
        }

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        player.sendSystemMessage(Component.translatable("epicvanguard.text.vanguard_command.40", oldName, cleanName));
        return 1;
    }

    private static String formatDimName(String dim) {
        if (dim == null) return "Overworld";
        if (dim.contains("overworld")) return "Overworld";
        if (dim.contains("the_nether")) return "Nether";
        if (dim.contains("the_end")) return "The End";
        return dim;
    }
}
