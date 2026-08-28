package com.example.epicvanguard.command;

import com.example.epicvanguard.entity.CompanionSavedData;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.List;
import java.util.UUID;

public class VanguardCommand {

        public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vanguard")
            .executes(context -> listCompanions(context.getSource()))
            .then(Commands.literal("listar").executes(context -> listCompanions(context.getSource())))
            .then(Commands.literal("chamar").requires(VanguardCommand::hasAdminAccess).executes(context -> summonAllCompanions(context.getSource())))
            .then(Commands.literal("tp").requires(VanguardCommand::hasAdminAccess).then(Commands.argument("alvo", StringArgumentType.string()).executes(context -> teleportCompanionToPlayer(context.getSource(), StringArgumentType.getString(context, "alvo")))))
            .then(Commands.literal("ir").requires(VanguardCommand::hasAdminAccess).then(Commands.argument("alvo", StringArgumentType.string()).executes(context -> teleportPlayerToCompanion(context.getSource(), StringArgumentType.getString(context, "alvo")))))
            .then(Commands.literal("remover").then(Commands.argument("alvo", StringArgumentType.string()).executes(context -> removeCompanion(context.getSource(), StringArgumentType.getString(context, "alvo")))))
            .then(Commands.literal("dispensar").then(Commands.argument("alvo", StringArgumentType.string()).executes(context -> removeCompanion(context.getSource(), StringArgumentType.getString(context, "alvo")))))
            .then(Commands.literal("limpar").executes(context -> cleanInactiveCompanions(context.getSource())))
            .then(Commands.literal("fogoamigo")
                .executes(context -> getFriendlyFire(context.getSource()))
                .then(Commands.argument("modo", StringArgumentType.word())
                    .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(List.of("livre", "melee", "total"), builder))
                    .executes(context -> setFriendlyFire(context.getSource(), StringArgumentType.getString(context, "modo")))))
        );

        dispatcher.register(Commands.literal("companhias")
            .executes(context -> listCompanions(context.getSource()))
            .then(Commands.literal("listar").executes(context -> listCompanions(context.getSource())))
            .then(Commands.literal("chamar").requires(VanguardCommand::hasAdminAccess).executes(context -> summonAllCompanions(context.getSource())))
            .then(Commands.literal("tp").requires(VanguardCommand::hasAdminAccess).then(Commands.argument("alvo", StringArgumentType.string()).executes(context -> teleportCompanionToPlayer(context.getSource(), StringArgumentType.getString(context, "alvo")))))
            .then(Commands.literal("ir").requires(VanguardCommand::hasAdminAccess).then(Commands.argument("alvo", StringArgumentType.string()).executes(context -> teleportPlayerToCompanion(context.getSource(), StringArgumentType.getString(context, "alvo")))))
            .then(Commands.literal("remover").then(Commands.argument("alvo", StringArgumentType.string()).executes(context -> removeCompanion(context.getSource(), StringArgumentType.getString(context, "alvo")))))
            .then(Commands.literal("dispensar").then(Commands.argument("alvo", StringArgumentType.string()).executes(context -> removeCompanion(context.getSource(), StringArgumentType.getString(context, "alvo")))))
            .then(Commands.literal("limpar").executes(context -> cleanInactiveCompanions(context.getSource())))
        );
    }

    private static int getFriendlyFire(CommandSourceStack source) {
        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.FriendlyFireMode currentMode = data.getGlobalFriendlyFireMode();
        source.sendSuccess(() -> Component.literal("§6⚔ Fogo Amigo Atual: " + currentMode.getFormatted()), false);
        source.sendSuccess(() -> Component.literal("§7Modos: §blivre §7(sem dano) | §emelee §7(apenas golpes fisicos) | §ctotal §7(todos acertam)"), false);
        return 1;
    }

    private static int setFriendlyFire(CommandSourceStack source, String modoStr) {
        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.FriendlyFireMode mode = CompanionSavedData.FriendlyFireMode.fromString(modoStr);
        data.setGlobalFriendlyFireMode(mode);
        source.sendSuccess(() -> Component.literal("§a⚔ Fogo Amigo alterado globalmente para: " + mode.getFormatted()), true);
        return 1;
    }

    public static boolean hasAdminAccess(CommandSourceStack source) {
        if (source.hasPermission(2)) return true;
        if (source.getEntity() instanceof ServerPlayer player && player.isCreative()) return true;
        return false;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildSubcommand() {
        return Commands.literal("companhias")
            .executes(context -> listCompanions(context.getSource()))
            .then(Commands.literal("listar")
                .executes(context -> listCompanions(context.getSource()))
            )
            .then(Commands.literal("chamar")
                .requires(VanguardCommand::hasAdminAccess)
                .executes(context -> summonAllCompanions(context.getSource()))
            )
            .then(Commands.literal("tp")
                .requires(VanguardCommand::hasAdminAccess)
                .then(Commands.argument("alvo", StringArgumentType.string())
                    .executes(context -> teleportCompanionToPlayer(
                            context.getSource(),
                            StringArgumentType.getString(context, "alvo")
                    ))
                )
            )
            .then(Commands.literal("ir")
                .requires(VanguardCommand::hasAdminAccess)
                .then(Commands.argument("alvo", StringArgumentType.string())
                    .executes(context -> teleportPlayerToCompanion(
                            context.getSource(),
                            StringArgumentType.getString(context, "alvo")
                    ))
                )
            )
            .then(Commands.literal("goto")
                .requires(VanguardCommand::hasAdminAccess)
                .then(Commands.argument("alvo", StringArgumentType.string())
                    .executes(context -> teleportPlayerToCompanion(
                            context.getSource(),
                            StringArgumentType.getString(context, "alvo")
                    ))
                )
            )
            .then(Commands.literal("remover")
                .then(Commands.argument("alvo", StringArgumentType.string())
                    .executes(context -> removeCompanion(
                            context.getSource(),
                            StringArgumentType.getString(context, "alvo")
                    ))
                )
            )
            .then(Commands.literal("dispensar")
                .then(Commands.argument("alvo", StringArgumentType.string())
                    .executes(context -> removeCompanion(
                            context.getSource(),
                            StringArgumentType.getString(context, "alvo")
                    ))
                )
            )
            .then(Commands.literal("limpar")
                .executes(context -> cleanInactiveCompanions(context.getSource()))
            );
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildDirectCommand() {
        return Commands.literal("companhias")
            .executes(context -> listCompanions(context.getSource()))
            .then(Commands.literal("listar")
                .executes(context -> listCompanions(context.getSource()))
            )
            .then(Commands.literal("chamar")
                .requires(VanguardCommand::hasAdminAccess)
                .executes(context -> summonAllCompanions(context.getSource()))
            )
            .then(Commands.literal("tp")
                .requires(VanguardCommand::hasAdminAccess)
                .then(Commands.argument("alvo", StringArgumentType.string())
                    .executes(context -> teleportCompanionToPlayer(
                            context.getSource(),
                            StringArgumentType.getString(context, "alvo")
                    ))
                )
            )
            .then(Commands.literal("ir")
                .requires(VanguardCommand::hasAdminAccess)
                .then(Commands.argument("alvo", StringArgumentType.string())
                    .executes(context -> teleportPlayerToCompanion(
                            context.getSource(),
                            StringArgumentType.getString(context, "alvo")
                    ))
                )
            )
            .then(Commands.literal("goto")
                .requires(VanguardCommand::hasAdminAccess)
                .then(Commands.argument("alvo", StringArgumentType.string())
                    .executes(context -> teleportPlayerToCompanion(
                            context.getSource(),
                            StringArgumentType.getString(context, "alvo")
                    ))
                )
            )
            .then(Commands.literal("remover")
                .then(Commands.argument("alvo", StringArgumentType.string())
                    .executes(context -> removeCompanion(
                            context.getSource(),
                            StringArgumentType.getString(context, "alvo")
                    ))
                )
            )
            .then(Commands.literal("dispensar")
                .then(Commands.argument("alvo", StringArgumentType.string())
                    .executes(context -> removeCompanion(
                            context.getSource(),
                            StringArgumentType.getString(context, "alvo")
                    ))
                )
            )
            .then(Commands.literal("limpar")
                .executes(context -> cleanInactiveCompanions(context.getSource()))
            );
    }



    private static int listCompanions(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cApenas jogadores podem executar este comando."));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        List<CompanionSavedData.CompanionInfo> companions = data.getPlayerCompanions(player.getUUID());

        if (companions.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7e+ Voce ainda nao possui nenhum companheiro contratado no mundo."));
            return 1;
        }

        boolean isAdmin = hasAdminAccess(source);

        player.sendSystemMessage(Component.literal("\u00a76============== + SUAS COMPANHIAS (" + companions.size() + ") + =============="));

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
                data.setDirty();
            }

            String displayName = (liveEntity != null && !liveEntity.getWarriorName().isEmpty()) ? liveEntity.getWarriorName() : info.name;
            BlockPos currentPos = liveEntity != null ? liveEntity.blockPosition() : info.pos;
            String currentDim = liveEntity != null ? liveEntity.level().dimension().location().toString() : info.dimension;
            int currentMode = liveEntity != null ? liveEntity.getCombatMode() : info.combatMode;
            float currentHp = liveEntity != null ? liveEntity.getHealth() : info.health;
            float maxHp = liveEntity != null ? liveEntity.getMaxHealth() : info.maxHealth;

            String modeName;
            switch (currentMode) {
                case 0 -> modeName = "\u00a7c Agressivo";
                case 1 -> modeName = "\u00a7e Defensivo";
                case 2 -> modeName = "\u00a7b Guarda";
                case 3 -> modeName = "\u00a77 Ficar / Base";
                default -> modeName = "\u00a7fDesconhecido";
            }

            String distText = "";
            if (player.level().dimension().location().toString().equals(currentDim) && currentPos != null) {
                double dist = Math.sqrt(player.blockPosition().distSqr(currentPos));
                distText = " \u00a77(\u00a7f" + (int) dist + " blocos de distancia\u00a77)";
            }

            String hpText = " \u00a7c " + (int) currentHp + "/" + (int) maxHp;

            MutableComponent line = Component.literal("\u00a7e[#" + (i + 1) + "] \u00a7b\u00a7l" + displayName + hpText + "\n")
                    .append(Component.literal("  \u00a77 Coordenadas: \u00a7fX: " + currentPos.getX() + ", Y: " + currentPos.getY() + ", Z: " + currentPos.getZ() + " \u00a77(\u00a7d" + dimName + "\u00a77)" + distText + "\n"))
                    .append(Component.literal("  \u00a77 Modo: " + modeName + "\n  "));

            if (info.companionUUID != null) {
                String uuidStr = info.companionUUID.toString();

                if (isAdmin) {
                    String tpBringCmd = "/grimal companhias tp " + uuidStr;
                    MutableComponent btnBring = Component.literal("\u00a78[\u00a7a Trazer\u00a78]")
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpBringCmd))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("\u00a7a[Admin] Clique para teletransportar " + displayName + " ate sua posicao"))));

                    String tpGoCmd = "/grimal companhias ir " + uuidStr;
                    MutableComponent btnGo = Component.literal(" \u00a78[\u00a7b Ir\u00a78]")
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpGoCmd))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("\u00a7b[Admin] Clique para se teletransportar ate a posicao de " + displayName))));

                    line.append(btnBring).append(btnGo);
                }

                String removeCmd = "/grimal companhias remover " + uuidStr;
                MutableComponent btnDismiss = Component.literal(" \u00a78[\u00a7c Dispensar\u00a78]")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, removeCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("\u00a7cClique para dispensar " + displayName + " e remover da lista"))));

                line.append(btnDismiss);
            }

            player.sendSystemMessage(line);
        }

        MutableComponent footer = Component.literal("\u00a76==================================================");
        if (isAdmin) {
            footer.append(Component.literal("\n\u00a78[\u00a7a Chamar Todas as Companhias\u00a78]")
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/grimal companhias chamar"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("\u00a7a[Admin] Clique para teletransportar TODAS as suas companhias ate voce")))));
        }

        footer.append(Component.literal(" \u00a78[\u00a7e Limpar Inexistentes\u00a78]")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/grimal companhias limpar"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("\u00a7eLimpa registros de companhias que nao existem mais no mundo")))));

        footer.append(Component.literal(" \u00a78[\u00a7b Fogo Amigo\u00a78]")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/grimal fogoamigo"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("\u00a7bClique para configurar o fogo amigo com suas companhias")))));

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
            source.sendFailure(Component.literal("\u00a7cApenas jogadores podem executar este comando."));
            return 0;
        }

        if (!hasAdminAccess(source)) {
            player.sendSystemMessage(Component.literal("\u00a7cVoce precisa de privilegio de Operador (OP) ou Modo Criativo para teletransportar companhias."));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.CompanionInfo info = findCompanionInfo(data, player, targetIdentifier, source.hasPermission(2));

        if (info == null) {
            player.sendSystemMessage(Component.literal("\u00a7cCompanhia '" + targetIdentifier + "' nao encontrada na lista."));
            return 0;
        }

        if (!player.getUUID().equals(info.ownerUUID) && !source.hasPermission(2)) {
            player.sendSystemMessage(Component.literal("\u00a7cVoce nao e o dono desta companhia!"));
            return 0;
        }

        WarriorCompanionEntity entity = findOrLoadCompanion(source.getServer(), info);
        if (entity != null) {
            if (entity.level() != player.level()) {
                entity.teleportTo((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), null, entity.getYRot(), entity.getXRot());
            }
            entity.safeTeleportTo(player);
            player.sendSystemMessage(Component.literal("\u00a7a+ " + entity.getWarriorName() + " foi teletransportado(a) ate voce com sucesso!"));
            return 1;
        } else {
            boolean isNear = player.level().dimension().location().toString().equals(info.dimension)
                    && info.pos != null && player.blockPosition().distSqr(info.pos) < 64 * 64;

            if (isNear) {
                String removeCmd = "/grimal companhias remover " + info.companionUUID;
                MutableComponent btnRemove = Component.literal(" \u00a7c[ Remover Registro]")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, removeCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("\u00a7cClique para remover este guerreiro que nao existe mais no mundo"))));

                player.sendSystemMessage(Component.literal("\u00a7c+ " + info.name + " nao foi encontrado(a) nesta area (pode ter sido derrotado(a) ou removido(a)).").append(btnRemove));
            } else {
                player.sendSystemMessage(Component.literal("\u00a7e+ O chunk onde " + info.name + " esta localizado (" + info.pos.getX() + ", " + info.pos.getY() + ", " + info.pos.getZ() + ") esta descarregado. Va ate proximo dele para localiza-lo!"));
            }
            return 1;
        }
    }

    private static int teleportPlayerToCompanion(CommandSourceStack source, String targetIdentifier) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cApenas jogadores podem executar este comando."));
            return 0;
        }

        if (!hasAdminAccess(source)) {
            player.sendSystemMessage(Component.literal("\u00a7cVoce precisa de privilegio de Operador (OP) ou Modo Criativo para teletransportar ate companhias."));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.CompanionInfo info = findCompanionInfo(data, player, targetIdentifier, source.hasPermission(2));

        if (info == null) {
            player.sendSystemMessage(Component.literal("\u00a7cCompanhia '" + targetIdentifier + "' nao encontrada na lista."));
            return 0;
        }

        if (!player.getUUID().equals(info.ownerUUID) && !source.hasPermission(2)) {
            player.sendSystemMessage(Component.literal("\u00a7cVoce nao e o dono desta companhia!"));
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

        WarriorCompanionEntity entity = findOrLoadCompanion(source.getServer(), info);
        double targetX = entity != null ? entity.getX() : (info.pos != null ? info.pos.getX() + 0.5D : player.getX());
        double targetY = entity != null ? entity.getY() : (info.pos != null ? info.pos.getY() : player.getY());
        double targetZ = entity != null ? entity.getZ() : (info.pos != null ? info.pos.getZ() + 0.5D : player.getZ());
        float yRot = player.getYRot();
        float xRot = player.getXRot();

        player.teleportTo(targetLevel, targetX, targetY, targetZ, yRot, xRot);
        String name = entity != null ? entity.getWarriorName() : info.name;
        player.sendSystemMessage(Component.literal("\u00a7a+ Voce foi teletransportado(a) ate a posicao de \u00a7b" + name + "\u00a7a!"));

        targetLevel.sendParticles(ParticleTypes.PORTAL, targetX, targetY + 1.0D, targetZ, 30, 0.5D, 0.5D, 0.5D, 0.1D);
        targetLevel.playSound(null, targetX, targetY, targetZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        return 1;
    }

    private static int removeCompanion(CommandSourceStack source, String targetIdentifier) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cApenas jogadores podem executar este comando."));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.CompanionInfo info = findCompanionInfo(data, player, targetIdentifier, source.hasPermission(2));

        if (info == null) {
            player.sendSystemMessage(Component.literal("\u00a7cCompanhia '" + targetIdentifier + "' nao encontrada na lista."));
            return 0;
        }

        if (!player.getUUID().equals(info.ownerUUID) && !source.hasPermission(2)) {
            player.sendSystemMessage(Component.literal("\u00a7cVoce nao e o dono desta companhia!"));
            return 0;
        }

        WarriorCompanionEntity live = findLoadedCompanion(source.getServer(), info.companionUUID);
        if (live != null) {
            live.setRecruited(false);
            live.setOwnerUUID(null);
            live.setCustomName(Component.literal("\u00a77" + live.getWarriorName()));
        }

        data.unregister(info.companionUUID);
        player.sendSystemMessage(Component.literal("\u00a7a+ Companhia \u00a7b" + info.name + "\u00a7a foi dispensada e removida da lista."));
        return 1;
    }

    private static int cleanInactiveCompanions(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cApenas jogadores podem executar este comando."));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        List<CompanionSavedData.CompanionInfo> companions = data.getPlayerCompanions(player.getUUID());

        int removed = 0;
        for (CompanionSavedData.CompanionInfo info : companions) {
            WarriorCompanionEntity live = findOrLoadCompanion(source.getServer(), info);
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
            player.sendSystemMessage(Component.literal("\u00a7a+ " + removed + " registro(s) de companhias inexistentes foram limpos da sua lista!"));
        } else {
            player.sendSystemMessage(Component.literal("\u00a7e+ Nenhuma companhia inexistente encontrada perto de voce para limpar."));
        }
        return 1;
    }

    private static int summonAllCompanions(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cApenas jogadores podem executar este comando."));
            return 0;
        }

        if (!hasAdminAccess(source)) {
            player.sendSystemMessage(Component.literal("\u00a7cVoce precisa de privilegio de Operador (OP) ou Modo Criativo para chamar companhias."));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        List<CompanionSavedData.CompanionInfo> companions = data.getPlayerCompanions(player.getUUID());

        if (companions.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7e+ Voce nao possui companhias para chamar."));
            return 1;
        }

        int count = 0;
        for (CompanionSavedData.CompanionInfo info : companions) {
            WarriorCompanionEntity entity = findOrLoadCompanion(source.getServer(), info);
            if (entity != null) {
                if (entity.level() != player.level()) {
                    entity.teleportTo((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), null, entity.getYRot(), entity.getXRot());
                }
                entity.safeTeleportTo(player);
                count++;
            }
        }

        player.sendSystemMessage(Component.literal("\u00a7a+ " + count + " de " + companions.size() + " companhia(s) foram teletransportadas ate voce!"));
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

    private static WarriorCompanionEntity findOrLoadCompanion(net.minecraft.server.MinecraftServer server, CompanionSavedData.CompanionInfo info) {
        WarriorCompanionEntity live = findLoadedCompanion(server, info.companionUUID);
        if (live != null) return live;

        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(info.dimension)) {
                if (info.pos != null) {
                    ChunkPos cpos = new ChunkPos(info.pos);
                    level.getChunkSource().addRegionTicket(TicketType.FORCED, cpos, 2, cpos);
                    level.getChunk(cpos.x, cpos.z, ChunkStatus.FULL, true);

                    Entity e = level.getEntity(info.companionUUID);
                    if (e instanceof WarriorCompanionEntity warrior && warrior.isAlive()) {
                        return warrior;
                    }
                }
            }
        }
        return null;
    }

    private static String formatDimName(String dim) {
        if (dim == null) return "Overworld";
        if (dim.contains("overworld")) return "Overworld";
        if (dim.contains("the_nether")) return "Nether";
        if (dim.contains("the_end")) return "The End";
        return dim;
    }
}
