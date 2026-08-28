package com.example.epicvanguard.networking.packet;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
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
            if (context.getSender() == null) return;
            Entity entity = context.getSender().serverLevel().getEntity(entityId);
            if (entity instanceof WarriorCompanionEntity warrior) {
                warrior.setTarget(null);
                warrior.getNavigation().stop();
            }
        });
        return true;
    }
}
