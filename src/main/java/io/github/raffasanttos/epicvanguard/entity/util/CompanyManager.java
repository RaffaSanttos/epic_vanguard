package io.github.raffasanttos.epicvanguard.entity.util;

import io.github.raffasanttos.epicvanguard.entity.CompanionSavedData;
import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.event.CompanionTeleportHandler;
import io.github.raffasanttos.epicvanguard.networking.Messages;
import io.github.raffasanttos.epicvanguard.networking.packet.CompanyEntry;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketCompanyAction;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketCompanyList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lado do servidor da tela Minha Companhia: monta a lista dos companheiros do jogador (carregados ou não)
 * e executa os pedidos da tela (chamar, trocar postura, trocar formação). Tudo é conferido aqui:
 * só o dono mexe nos próprios companheiros.
 */
public final class CompanyManager {

    private CompanyManager() {}

    /** Anti-spam: um pedido que mexe em entidades a cada 5 ticks por jogador. */
    private static final int ACTION_COOLDOWN_TICKS = 5;
    private static final Map<UUID, Long> LAST_ACTION = new ConcurrentHashMap<>();

    public static void handleAction(ServerPlayer player, int action, @Nullable UUID companionId, int value) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (action != PacketCompanyAction.OPEN) {
            long now = server.getTickCount();
            Long last = LAST_ACTION.get(player.getUUID());
            if (last != null && now - last < ACTION_COOLDOWN_TICKS && now >= last) return;
            LAST_ACTION.put(player.getUUID(), now);
        }

        CompanionSavedData data = CompanionSavedData.get(server);
        switch (action) {
            case PacketCompanyAction.CALL -> call(player, data, ownInfo(player, data, companionId));
            case PacketCompanyAction.CALL_ALL -> {
                int count = 0;
                List<CompanionSavedData.CompanionInfo> all = data.getPlayerCompanions(player.getUUID());
                for (CompanionSavedData.CompanionInfo info : all) {
                    if (bring(player, info) != null) count++;
                }
                player.displayClientMessage(Component.translatable("epicvanguard.company.called_all", count, all.size()), true);
            }
            case PacketCompanyAction.SET_MODE -> setMode(player, data, ownInfo(player, data, companionId), value);
            case PacketCompanyAction.SET_FORMATION -> {
                int formation = WarriorFormation.sanitize(value);
                data.setFormation(player.getUUID(), formation);
                player.displayClientMessage(Component.translatable("epicvanguard.text.horn.formation", WarriorFormation.name(formation)), true);
            }
            default -> {}
        }
        sendList(player, action == PacketCompanyAction.OPEN);
    }

    /** Info do registro, só se o companheiro for do jogador. */
    @Nullable
    private static CompanionSavedData.CompanionInfo ownInfo(ServerPlayer player, CompanionSavedData data, @Nullable UUID id) {
        CompanionSavedData.CompanionInfo info = data.getCompanion(id);
        if (info == null || !player.getUUID().equals(info.ownerUUID)) return null;
        return info;
    }

    @Nullable
    private static WarriorCompanionEntity bring(ServerPlayer player, CompanionSavedData.CompanionInfo info) {
        WarriorCompanionEntity entity = CompanionTeleportHandler.findLoadedCompanion(player.getServer(), info.companionUUID);
        if (entity == null) {
            entity = CompanionTeleportHandler.restoreCompanionAtPlayer(player, (ServerLevel) player.level(), info);
        }
        return entity != null ? entity.safeTeleportTo(player) : null;
    }

    private static void call(ServerPlayer player, CompanionSavedData data, @Nullable CompanionSavedData.CompanionInfo info) {
        if (info == null) return;
        WarriorCompanionEntity entity = bring(player, info);
        if (entity != null) {
            player.displayClientMessage(Component.translatable("epicvanguard.text.vanguard_command.26", entity.getWarriorName()), true);
        } else {
            BlockPos p = info.pos;
            player.displayClientMessage(Component.translatable("epicvanguard.company.call_failed", info.name,
                    p != null ? p.getX() + " " + p.getY() + " " + p.getZ() : "?"), true);
        }
    }

    private static void setMode(ServerPlayer player, CompanionSavedData data, @Nullable CompanionSavedData.CompanionInfo info, int mode) {
        if (info == null || mode < WarriorCompanionEntity.MODE_FOLLOW || mode > WarriorCompanionEntity.MODE_STAY) return;
        WarriorCompanionEntity entity = CompanionTeleportHandler.findLoadedCompanion(player.getServer(), info.companionUUID);
        if (entity == null || !entity.isOwner(player)) {
            // Descarregado: a postura fica salva na entidade, que só pode ser mudada com ela carregada
            player.displayClientMessage(Component.translatable("epicvanguard.company.not_loaded", info.name), true);
            return;
        }
        entity.setCombatMode(mode); // Guarda marca o lugar onde ele está agora
    }

    /** Monta a lista (dados ao vivo para quem está carregado, o registro para os outros) e manda ao jogador. */
    public static void sendList(ServerPlayer player, boolean openScreen) {
        Messages.sendToPlayer(new PacketCompanyList(
                CompanionSavedData.get(player.getServer()).getFormation(player.getUUID()), openScreen, buildEntries(player)), player);
    }

    public static List<CompanyEntry> buildEntries(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        List<CompanyEntry> list = new ArrayList<>();
        if (server == null) return list;
        for (CompanionSavedData.CompanionInfo info : CompanionSavedData.get(server).getPlayerCompanions(player.getUUID())) {
            WarriorCompanionEntity live = CompanionTeleportHandler.findLoadedCompanion(server, info.companionUUID);
            if (live != null) {
                BlockPos p = live.blockPosition();
                String name = live.getWarriorName().isEmpty() ? info.name : live.getWarriorName();
                list.add(new CompanyEntry(info.companionUUID, safe(name), live.getSpecialization(), live.getWarriorLevel(),
                        live.getHealth(), live.getMaxHealth(), live.getCombatMode(),
                        live.level().dimension().location().toString(), p.getX(), p.getY(), p.getZ(), true));
            } else {
                BlockPos p = info.pos != null ? info.pos : BlockPos.ZERO;
                list.add(new CompanyEntry(info.companionUUID, safe(info.name), info.specialization, Math.max(1, info.level),
                        info.health, info.maxHealth, info.combatMode, info.dimension == null ? "" : info.dimension,
                        p.getX(), p.getY(), p.getZ(), false));
            }
        }
        list.sort(Comparator.comparing(CompanyEntry::name, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private static String safe(@Nullable String name) {
        if (name == null || name.isEmpty()) return "?";
        return name.length() > 64 ? name.substring(0, 64) : name;
    }

    /** Limpa o anti-spam de quem saiu (chamado no logout). */
    public static void forget(UUID player) {
        LAST_ACTION.remove(player);
    }
}
