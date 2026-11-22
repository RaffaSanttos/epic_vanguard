package io.github.raffasanttos.epicvanguard.networking.packet;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.networking.PacketGuards;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketWarriorCancelAction {
    private final int entityId;

    public PacketWarriorCancelAction(int entityId) {
        this.entityId = entityId;
    }

    public PacketWarriorCancelAction(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            WarriorCompanionEntity warrior = PacketGuards.ownedWarrior(context.getSender(), entityId);
            if (warrior != null) {
                warrior.clearCombatTarget();
            }
        });
        return true;
    }
}
