package io.github.raffasanttos.epicvanguard.networking.packet;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/** Uma linha da tela Minha Companhia (só dados, vai do servidor para o cliente). */
public record CompanyEntry(UUID id, String name, int specialization, int level, float health, float maxHealth,
                           int mode, String dimension, int x, int y, int z, boolean loaded) {

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name, 64);
        buf.writeByte(specialization);
        buf.writeByte(level);
        buf.writeFloat(health);
        buf.writeFloat(maxHealth);
        buf.writeByte(mode);
        buf.writeUtf(dimension, 256);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeBoolean(loaded);
    }

    public static CompanyEntry read(FriendlyByteBuf buf) {
        return new CompanyEntry(buf.readUUID(), buf.readUtf(64), buf.readByte(), buf.readByte(), buf.readFloat(),
                buf.readFloat(), buf.readByte(), buf.readUtf(256), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean());
    }
}
