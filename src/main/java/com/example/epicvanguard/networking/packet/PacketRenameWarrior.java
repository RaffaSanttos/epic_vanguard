package com.example.epicvanguard.networking.packet;

import com.example.epicvanguard.entity.CompanionSavedData;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketRenameWarrior {
    private final int entityId;
    private final String newName;

    public PacketRenameWarrior(int entityId, String newName) {
        this.entityId = entityId;
        this.newName = newName != null ? newName : "";
    }

    public PacketRenameWarrior(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.newName = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(newName, 64);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Entity entity = player.serverLevel().getEntity(entityId);
            if (!(entity instanceof WarriorCompanionEntity warrior)) return;

            if (!warrior.isOwner(player)) {
                player.sendSystemMessage(Component.literal("§cVocê só pode renomear guerreiros que juraram lealdade a você!"));
                return;
            }

            String clean = newName.replaceAll("§[0-9a-fk-or]", "").trim();
            if (clean.isEmpty()) {
                clean = "Guerreiro";
            }
            if (clean.length() > 20) {
                clean = clean.substring(0, 20);
            }

            warrior.setWarriorName(clean);

            if (player.getServer() != null) {
                CompanionSavedData.get(player.getServer()).updateName(warrior.getUUID(), clean);
            }

            player.serverLevel().playSound(null, warrior.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
            player.sendSystemMessage(Component.literal("§6✦ [Vanguarda] §aO companheiro agora se chama §e§l" + clean + "§a!"));
        });
        return true;
    }
}
