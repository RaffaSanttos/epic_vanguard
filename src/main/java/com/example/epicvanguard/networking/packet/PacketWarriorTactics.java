package com.example.epicvanguard.networking.packet;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketWarriorTactics {
    private final int entityId;
    private final boolean targetHostiles;
    private final boolean targetPassives;

    public PacketWarriorTactics(int entityId, boolean targetHostiles, boolean targetPassives) {
        this.entityId = entityId;
        this.targetHostiles = targetHostiles;
        this.targetPassives = targetPassives;
    }

    public PacketWarriorTactics(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.targetHostiles = buf.readBoolean();
        this.targetPassives = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(targetHostiles);
        buf.writeBoolean(targetPassives);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() == null) return;
            Entity entity = context.getSender().serverLevel().getEntity(entityId);
            if (!(entity instanceof WarriorCompanionEntity warrior)) return;

            warrior.setTargetHostiles(targetHostiles);
            warrior.setTargetPassives(targetPassives);

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
