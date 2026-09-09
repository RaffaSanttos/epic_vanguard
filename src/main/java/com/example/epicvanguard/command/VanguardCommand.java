package com.example.epicvanguard.command;

import com.example.epicvanguard.entity.CompanionSavedData;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
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
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;

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
            SharedSuggestionProvider.suggest(List.of("livre", "desativado", "melee", "fisico", "magias", "total", "ativado"), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Registra comando principal em Português
        dispatcher.register(buildRootCommand("companhias"));
        dispatcher.register(buildRootCommand("companhia"));
        dispatcher.register(buildRootCommand("companions"));
        dispatcher.register(buildRootCommand("companion"));

        // Registra comando com o nome do Mod
        dispatcher.register(buildRootCommand("vanguard"));
        dispatcher.register(buildRootCommand("epicvanguard"));

        // Compatibilidade retroativa
        dispatcher.register(buildRootCommand("grimal"));

        // Atalhos diretos para comando de Fogo Amigo
        dispatcher.register(Commands.literal("fogoamigo")
                .executes(context -> getFriendlyFire(context.getSource()))
                .then(Commands.argument("modo", StringArgumentType.word())
                        .suggests(FRIENDLY_FIRE_SUGGESTIONS)
                        .executes(context -> setFriendlyFire(context.getSource(), StringArgumentType.getString(context, "modo"))))
        );
        dispatcher.register(Commands.literal("friendlyfire")
                .executes(context -> getFriendlyFire(context.getSource()))
                .then(Commands.argument("modo", StringArgumentType.word())
                        .suggests(FRIENDLY_FIRE_SUGGESTIONS)
                        .executes(context -> setFriendlyFire(context.getSource(), StringArgumentType.getString(context, "modo"))))
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
                        .then(Commands.argument("alvo", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> teleportCompanionToPlayer(context.getSource(), StringArgumentType.getString(context, "alvo")))))
                .then(Commands.literal("trazer")
                        .then(Commands.argument("alvo", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> teleportCompanionToPlayer(context.getSource(), StringArgumentType.getString(context, "alvo")))))
                .then(Commands.literal("bring")
                        .then(Commands.argument("alvo", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> teleportCompanionToPlayer(context.getSource(), StringArgumentType.getString(context, "alvo")))))
                .then(Commands.literal("ir")
                        .then(Commands.argument("alvo", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> teleportPlayerToCompanion(context.getSource(), StringArgumentType.getString(context, "alvo")))))
                .then(Commands.literal("goto")
                        .then(Commands.argument("alvo", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> teleportPlayerToCompanion(context.getSource(), StringArgumentType.getString(context, "alvo")))))
                .then(Commands.literal("dispensar")
                        .then(Commands.argument("alvo", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> removeCompanion(context.getSource(), StringArgumentType.getString(context, "alvo")))))
                .then(Commands.literal("remover")
                        .then(Commands.argument("alvo", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> removeCompanion(context.getSource(), StringArgumentType.getString(context, "alvo")))))
                .then(Commands.literal("dismiss")
                        .then(Commands.argument("alvo", StringArgumentType.greedyString())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> removeCompanion(context.getSource(), StringArgumentType.getString(context, "alvo")))))
                .then(Commands.literal("limpar")
                        .executes(context -> cleanInactiveCompanions(context.getSource())))
                .then(Commands.literal("clean")
                        .executes(context -> cleanInactiveCompanions(context.getSource())))
                .then(Commands.literal("fogoamigo")
                        .executes(context -> getFriendlyFire(context.getSource()))
                        .then(Commands.argument("modo", StringArgumentType.word())
                                .suggests(FRIENDLY_FIRE_SUGGESTIONS)
                                .executes(context -> setFriendlyFire(context.getSource(), StringArgumentType.getString(context, "modo")))))
                .then(Commands.literal("friendlyfire")
                        .executes(context -> getFriendlyFire(context.getSource()))
                        .then(Commands.argument("modo", StringArgumentType.word())
                                .suggests(FRIENDLY_FIRE_SUGGESTIONS)
                                .executes(context -> setFriendlyFire(context.getSource(), StringArgumentType.getString(context, "modo")))));
    }

    public static boolean hasAdminAccess(CommandSourceStack source) {
        if (source.hasPermission(2)) return true;
        if (source.getEntity() instanceof ServerPlayer player && player.isCreative()) return true;
        return false;
    }

    private static int getFriendlyFire(CommandSourceStack source) {
        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.FriendlyFireMode currentMode = data.getGlobalFriendlyFireMode();
        source.sendSuccess(() -> Component.literal("§6⚔ Fogo Amigo Atual: " + currentMode.getFormatted()), false);
        source.sendSuccess(() -> Component.literal("§7Modos disponíveis: §blivre §7(sem dano) | §emelee §7(apenas golpes físicos) | §ctotal §7(todos acertam)"), false);
        return 1;
    }

    private static int setFriendlyFire(CommandSourceStack source, String modoStr) {
        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.FriendlyFireMode mode = CompanionSavedData.FriendlyFireMode.fromString(modoStr);
        data.setGlobalFriendlyFireMode(mode);
        source.sendSuccess(() -> Component.literal("§a⚔ Fogo Amigo alterado globalmente para: " + mode.getFormatted()), true);
        return 1;
    }

    private static int listCompanions(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cApenas jogadores podem executar este comando."));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        List<CompanionSavedData.CompanionInfo> companions = data.getPlayerCompanions(player.getUUID());

        if (companions.isEmpty()) {
            player.sendSystemMessage(Component.literal("§e✦ Você ainda não possui nenhuma companhia contratada no mundo."));
            return 1;
        }

        player.sendSystemMessage(Component.literal("§6============== ⚔ SUAS COMPANHIAS (" + companions.size() + ") ⚔ =============="));

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
                case 0 -> modeName = "§a Seguir";
                case 1 -> modeName = "§e Guarda";
                case 2 -> modeName = "§7 Parado";
                default -> modeName = "§a Seguir";
            }

            String distText = "";
            if (currentPos != null && player.level().dimension().location().toString().equals(currentDim)) {
                double dist = Math.sqrt(player.blockPosition().distSqr(currentPos));
                distText = " §7(§f" + (int) dist + " blocos de distância§7)";
            }

            String hpText = " §c " + (int) currentHp + "/" + (int) maxHp;

            int posX = currentPos != null ? currentPos.getX() : 0;
            int posY = currentPos != null ? currentPos.getY() : 0;
            int posZ = currentPos != null ? currentPos.getZ() : 0;

            MutableComponent line = Component.literal("§e[#" + (i + 1) + "] §b§l" + displayName + hpText + "\n")
                    .append(Component.literal("  §7 Coordenadas: §fX: " + posX + ", Y: " + posY + ", Z: " + posZ + " §7(§d" + dimName + "§7)" + distText + "\n"))
                    .append(Component.literal("  §7 Modo:" + modeName + "\n  "));

            if (info.companionUUID != null) {
                String uuidStr = info.companionUUID.toString();

                String tpBringCmd = "/companhias tp " + uuidStr;
                MutableComponent btnBring = Component.literal("§8[§a Trazer§8]")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpBringCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§aClique para teletransportar " + displayName + " até você"))));

                String tpGoCmd = "/companhias ir " + uuidStr;
                MutableComponent btnGo = Component.literal(" §8[§b Ir§8]")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpGoCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§bClique para se teletransportar até a posição de " + displayName))));

                String removeCmd = "/companhias dispensar " + uuidStr;
                MutableComponent btnDismiss = Component.literal(" §8[§c Dispensar§8]")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, removeCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§cClique para dispensar " + displayName + " e remover da lista"))));

                line.append(btnBring).append(btnGo).append(btnDismiss);
            }

            player.sendSystemMessage(line);
        }

        MutableComponent footer = Component.literal("§6==================================================");
        footer.append(Component.literal("\n§8[§a Chamar Todas as Companhias§8]")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/companhias chamar"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§aClique para teletransportar TODAS as suas companhias até você")))));

        footer.append(Component.literal(" §8[§e Limpar Inexistentes§8]")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/companhias limpar"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§eLimpa registros de companhias que não existem mais no mundo")))));

        footer.append(Component.literal(" §8[§b Fogo Amigo§8]")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fogoamigo"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§bClique para ver e configurar o fogo amigo")))));

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
            source.sendFailure(Component.literal("§cApenas jogadores podem executar este comando."));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.CompanionInfo info = findCompanionInfo(data, player, targetIdentifier, source.hasPermission(2));

        if (info == null) {
            player.sendSystemMessage(Component.literal("§cCompanhia '" + targetIdentifier + "' não encontrada na lista."));
            return 0;
        }

        if (!player.getUUID().equals(info.ownerUUID) && !hasAdminAccess(source)) {
            player.sendSystemMessage(Component.literal("§cVocê não é o dono desta companhia!"));
            return 0;
        }

        WarriorCompanionEntity entity = findOrLoadCompanion(source.getServer(), info);
        if (entity != null) {
            if (entity.level() != player.level()) {
                entity.teleportTo((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), null, entity.getYRot(), entity.getXRot());
            }
            entity.safeTeleportTo(player);
            player.sendSystemMessage(Component.literal("§a✦ " + entity.getWarriorName() + " foi teletransportado(a) até você com sucesso!"));
            return 1;
        } else {
            boolean isNear = player.level().dimension().location().toString().equals(info.dimension)
                    && info.pos != null && player.blockPosition().distSqr(info.pos) < 64 * 64;

            if (isNear) {
                String removeCmd = "/companhias dispensar " + info.companionUUID;
                MutableComponent btnRemove = Component.literal(" §c[Remover Registro]")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, removeCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§cClique para remover este guerreiro que não existe mais"))));

                player.sendSystemMessage(Component.literal("§c✦ " + info.name + " não foi encontrado(a) nesta área (pode ter sido derrotado(a) ou removido(a)).").append(btnRemove));
            } else {
                player.sendSystemMessage(Component.literal("§e✦ O chunk onde " + info.name + " está localizado (" + (info.pos != null ? info.pos.getX() + ", " + info.pos.getY() + ", " + info.pos.getZ() : "desconhecido") + ") está descarregado. Vá até próximo dele para localizá-lo!"));
            }
            return 1;
        }
    }

    private static int teleportPlayerToCompanion(CommandSourceStack source, String targetIdentifier) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cApenas jogadores podem executar este comando."));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.CompanionInfo info = findCompanionInfo(data, player, targetIdentifier, source.hasPermission(2));

        if (info == null) {
            player.sendSystemMessage(Component.literal("§cCompanhia '" + targetIdentifier + "' não encontrada na lista."));
            return 0;
        }

        if (!player.getUUID().equals(info.ownerUUID) && !hasAdminAccess(source)) {
            player.sendSystemMessage(Component.literal("§cVocê não é o dono desta companhia!"));
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
        player.sendSystemMessage(Component.literal("§a✦ Você foi teletransportado(a) até a posição de §b" + name + "§a!"));

        targetLevel.sendParticles(ParticleTypes.PORTAL, targetX, targetY + 1.0D, targetZ, 30, 0.5D, 0.5D, 0.5D, 0.1D);
        targetLevel.playSound(null, targetX, targetY, targetZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        return 1;
    }

    private static int removeCompanion(CommandSourceStack source, String targetIdentifier) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cApenas jogadores podem executar este comando."));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        CompanionSavedData.CompanionInfo info = findCompanionInfo(data, player, targetIdentifier, source.hasPermission(2));

        if (info == null) {
            player.sendSystemMessage(Component.literal("§cCompanhia '" + targetIdentifier + "' não encontrada na lista."));
            return 0;
        }

        if (!player.getUUID().equals(info.ownerUUID) && !hasAdminAccess(source)) {
            player.sendSystemMessage(Component.literal("§cVocê não é o dono desta companhia!"));
            return 0;
        }

        WarriorCompanionEntity live = findLoadedCompanion(source.getServer(), info.companionUUID);
        if (live != null) {
            live.setRecruited(false);
            live.setOwnerUUID(null);
            live.setCustomName(Component.literal("§7" + live.getWarriorName()));
        }

        data.unregister(info.companionUUID);
        player.sendSystemMessage(Component.literal("§a✦ Companhia §b" + info.name + "§a foi dispensada e removida da lista."));
        return 1;
    }

    private static int cleanInactiveCompanions(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cApenas jogadores podem executar este comando."));
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
            player.sendSystemMessage(Component.literal("§a✦ " + removed + " registro(s) de companhias inexistentes foram limpos da sua lista!"));
        } else {
            player.sendSystemMessage(Component.literal("§e✦ Nenhuma companhia inexistente encontrada perto de você para limpar."));
        }
        return 1;
    }

    private static int summonAllCompanions(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cApenas jogadores podem executar este comando."));
            return 0;
        }

        CompanionSavedData data = CompanionSavedData.get(source.getServer());
        List<CompanionSavedData.CompanionInfo> companions = data.getPlayerCompanions(player.getUUID());

        if (companions.isEmpty()) {
            player.sendSystemMessage(Component.literal("§e✦ Você não possui companhias para chamar."));
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

        player.sendSystemMessage(Component.literal("§a✦ " + count + " de " + companions.size() + " companhia(s) foram teletransportadas até você!"));
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
                    try {
                        level.getChunkSource().addRegionTicket(TicketType.FORCED, cpos, 2, cpos);
                        level.getChunk(cpos.x, cpos.z, ChunkStatus.FULL, true);

                        Entity e = level.getEntity(info.companionUUID);
                        if (e instanceof WarriorCompanionEntity warrior && warrior.isAlive()) {
                            return warrior;
                        }
                    } finally {
                        level.getChunkSource().removeRegionTicket(TicketType.FORCED, cpos, 2, cpos);
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
