package com.example.epicvanguard.gametest;

import com.example.epicvanguard.entity.CompanionSavedData;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModEntityTypes;
import com.example.epicvanguard.inventory.WarriorInventory;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Utilitários compartilhados pelos GameTests do Epic Vanguard. */
final class TestUtil {

    /** Template: plataforma de pedra 9x9 (gerada por tools/gametest/make_platform.js). */
    static final String PLATFORM = "gametest/platform";

    private TestUtil() {}

    static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    static <T> void checkEquals(T expected, T actual, String what) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new GameTestAssertException(what + ": esperado <" + expected + ">, veio <" + actual + ">");
        }
    }

    /** Guerreiro sem IA (não anda nem ataca sozinho), em coordenadas relativas ao teste. */
    static WarriorCompanionEntity warrior(GameTestHelper helper, int x, int z) {
        return helper.spawnWithNoFreeWill(ModEntityTypes.WARRIOR_COMPANION.get(), x, 1, z);
    }

    static Zombie zombie(GameTestHelper helper, int x, int z) {
        return helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE, x, 1, z);
    }

    /** Coloca a arma no slot principal do inventário do guerreiro (como o jogador faria pela tela). */
    static void giveWeapon(WarriorCompanionEntity warrior, ItemStack weapon) {
        warrior.getWarriorInventory().setItem(WarriorInventory.SLOT_WEAPON_MAIN, weapon);
        warrior.syncEquipmentWithInventory();
    }

    /** Recruta o guerreiro para um dono e registra no CompanionSavedData. Modo Parado, para não seguir ninguém. */
    static void recruit(WarriorCompanionEntity warrior, UUID owner) {
        warrior.setOwnerUUID(owner);
        warrior.setRecruited(true);
        warrior.setCombatMode(2);
        warrior.updateSavedData();
    }

    static CompanionSavedData data(GameTestHelper helper) {
        return CompanionSavedData.get(helper.getLevel().getServer());
    }

    /** NBT completo da entidade, como fica salvo no chunk. */
    static CompoundTag saveEntity(WarriorCompanionEntity warrior) {
        CompoundTag tag = new CompoundTag();
        warrior.saveWithoutId(tag);
        return tag;
    }

    /** Recria a entidade a partir do NBT, como o jogo faz ao carregar um chunk (ainda sem adicionar ao mundo). */
    static WarriorCompanionEntity loadCopy(GameTestHelper helper, CompoundTag tag) {
        WarriorCompanionEntity copy = ModEntityTypes.WARRIOR_COMPANION.get().create(helper.getLevel());
        if (copy == null) throw new GameTestAssertException("não conseguiu criar a entidade");
        copy.load(tag);
        return copy;
    }

    /**
     * Jogador falso conectado ao servidor. O do vanilla ({@code makeMockServerPlayerInLevel}) é sempre criativo;
     * este deixa escolher. Precisa ser removido com {@link #remove(ServerPlayer)} no fim do teste.
     */
    static ServerPlayer player(GameTestHelper helper, boolean creative, int x, int z) {
        ServerLevel level = helper.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "ev-test-" + (creative ? "c" : "s"));
        ServerPlayer player = new ServerPlayer(level.getServer(), level, profile) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return creative; }
        };
        // O Forge mexe no pipeline da conexão ao logar: precisa de um canal (o do vanilla não tem e quebra)
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player);
        Vec3 pos = helper.absoluteVec(new Vec3(x + 0.5D, 1.0D, z + 0.5D));
        player.teleportTo(level, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    static void remove(ServerPlayer player) {
        if (player != null && player.getServer() != null) {
            player.getServer().getPlayerList().remove(player);
        }
    }

    /** Roda o corpo do teste e sempre remove os jogadores falsos, passando ou falhando. */
    static void withPlayers(Runnable body, ServerPlayer... players) {
        try {
            body.run();
        } finally {
            for (ServerPlayer p : players) remove(p);
        }
    }
}
