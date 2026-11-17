package com.example.epicvanguard.networking.packet;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.networking.PacketGuards;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketWarriorCombatMode {
    private final int entityId;
    private final int newMode;

    public static final int MODE_FOLLOW = 0; // Seguir jogador
    public static final int MODE_GUARD  = 1; // Ficar de guarda (raio de 3 blocos)
    public static final int MODE_STAY   = 2; // Parado e imóvel

    public PacketWarriorCombatMode(int entityId, int newMode) {
        this.entityId = entityId;
        this.newMode  = newMode;
    }

    public PacketWarriorCombatMode(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.newMode  = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(newMode);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Só o dono, perto do guerreiro, pode trocar o modo; e só para modos válidos
            WarriorCompanionEntity warrior = PacketGuards.ownedWarrior(context.getSender(), entityId);
            if (warrior == null) return;
            if (newMode < MODE_FOLLOW || newMode > MODE_STAY) return;

            warrior.setCombatMode(newMode);
            if (newMode == MODE_GUARD) {
                warrior.setGuardPos(warrior.blockPosition());
            }
        });
        return true;
    }
}
