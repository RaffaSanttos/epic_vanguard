package io.github.raffasanttos.epicvanguard.networking.packet;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.networking.PacketGuards;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Botões de tática da tela do guerreiro: atacar monstros, caçar animais e coleta de itens (0 desligada, 1 úteis, 2 tudo). */
public class PacketWarriorTactics {
    private final int entityId;
    private final boolean targetHostiles;
    private final boolean targetPassives;
    private final int pickupMode;

    public PacketWarriorTactics(int entityId, boolean targetHostiles, boolean targetPassives, int pickupMode) {
        this.entityId = entityId;
        this.targetHostiles = targetHostiles;
        this.targetPassives = targetPassives;
        this.pickupMode = pickupMode;
    }

    public PacketWarriorTactics(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.targetHostiles = buf.readBoolean();
        this.targetPassives = buf.readBoolean();
        this.pickupMode = buf.readByte();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(targetHostiles);
        buf.writeBoolean(targetPassives);
        buf.writeByte(pickupMode);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            WarriorCompanionEntity warrior = PacketGuards.ownedWarrior(context.getSender(), entityId);
            if (warrior == null) return;

            warrior.setTargetHostiles(targetHostiles);
            warrior.setTargetPassives(targetPassives);
            warrior.setPickupMode(pickupMode); // setPickupMode limita a 0..2

            // Se o alvo atual não for mais compatível com as regras ativas, limpa o alvo
            if (warrior.getTarget() != null) {
                if (!targetHostiles && warrior.getTarget() instanceof net.minecraft.world.entity.monster.Enemy) {
                    warrior.clearCombatTarget();
                } else if (!targetPassives && warrior.getTarget() instanceof net.minecraft.world.entity.animal.Animal) {
                    warrior.clearCombatTarget();
                }
            }
        });
        return true;
    }
}
