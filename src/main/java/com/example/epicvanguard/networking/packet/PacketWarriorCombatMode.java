package com.example.epicvanguard.networking.packet;

import com.example.epicvanguard.entity.WarriorCompanionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketWarriorCombatMode {
    private final int entityId;
    private final int newMode;

    public static final int MODE_AGGRO   = 0;
    public static final int MODE_DEFENSE = 1;
    public static final int MODE_GUARD   = 2;
    public static final int MODE_STAY    = 3;

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
            if (context.getSender() == null) return;
            Entity entity = context.getSender().serverLevel().getEntity(entityId);
            if (!(entity instanceof WarriorCompanionEntity warrior)) return;

            warrior.setCombatMode(newMode);
            if (newMode == MODE_GUARD) {
                warrior.setGuardPos(warrior.blockPosition());
            }
        });
        return true;
    }
}
