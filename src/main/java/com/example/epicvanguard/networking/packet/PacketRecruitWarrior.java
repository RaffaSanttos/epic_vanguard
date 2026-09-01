package com.example.epicvanguard.networking.packet;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModItems;
import com.example.epicvanguard.screen.WarriorCompanionMenu;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class PacketRecruitWarrior {
    private final int entityId;

    public PacketRecruitWarrior(int entityId) {
        this.entityId = entityId;
    }

    public PacketRecruitWarrior(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Entity entity = player.serverLevel().getEntity(entityId);
            if (!(entity instanceof WarriorCompanionEntity warrior)) return;

            if (warrior.isRecruited() || warrior.distanceTo(player) > 8.0F) return;

            int cost = warrior.getRecruitCost();
            if (cost <= 0 || consumeGoldCoins(player, cost)) {
                warrior.setOwnerUUID(player.getUUID());
                warrior.setRecruited(true);
                warrior.setCombatMode(1); // Defensivo por padrao
                if (warrior.isPrisoner()) {
                    warrior.setPrisoner(false);
                    warrior.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
                    warrior.setHealth(warrior.getMaxHealth());
                }

                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            warrior.getX(), warrior.getY() + 1.0D, warrior.getZ(),
                            20, 0.5D, 0.5D, 0.5D, 0.05D);
                    serverLevel.playSound(null, warrior.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                }

                player.sendSystemMessage(Component.literal("§6[" + warrior.getWarriorName() + "] §aPacto de honra selado! Estou às suas ordens."));

                // Abre a tela de gerenciamento do guerreiro
                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (id, inv, p) -> new WarriorCompanionMenu(id, inv, warrior.getId()),
                        Component.literal("Companheiro - " + warrior.getWarriorName())
                ), buf -> buf.writeInt(warrior.getId()));
            } else {
                player.sendSystemMessage(Component.literal("§cVocê precisa de " + cost + " Moedas de Ouro para contratar " + warrior.getWarriorName() + "."));
            }
        });
        return true;
    }

    public static int countGoldCoins(ServerPlayer player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(ModItems.GOLD_COIN.get())) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean consumeGoldCoins(ServerPlayer player, int amount) {
        if (player.isCreative()) return true;
        if (countGoldCoins(player) < amount) return false;

        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(ModItems.GOLD_COIN.get())) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
        return true;
    }
}