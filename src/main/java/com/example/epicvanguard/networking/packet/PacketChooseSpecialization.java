package com.example.epicvanguard.networking.packet;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.entity.util.WarriorProgression;
import com.example.epicvanguard.networking.PacketGuards;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Dono escolhe a classe de um Guerreiro base que chegou ao nível {@link WarriorProgression#SPEC_CHOICE_LEVEL}. */
public class PacketChooseSpecialization {
    private final int entityId;
    private final int specialization;

    public PacketChooseSpecialization(int entityId, int specialization) {
        this.entityId = entityId;
        this.specialization = specialization;
    }

    public PacketChooseSpecialization(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.specialization = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVarInt(specialization);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            // Só o dono, perto do guerreiro
            WarriorCompanionEntity warrior = PacketGuards.ownedWarrior(player, entityId);
            if (warrior == null || player == null) return;
            if (!WarriorProgression.canChooseSpecialization(warrior)) {
                player.sendSystemMessage(Component.translatable("epicvanguard.text.specialization.not_ready",
                        WarriorProgression.SPEC_CHOICE_LEVEL));
                return;
            }
            WarriorProgression.chooseSpecialization(warrior, player, specialization);
        });
        return true;
    }
}
