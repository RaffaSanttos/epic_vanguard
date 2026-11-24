package io.github.raffasanttos.epicvanguard.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> cliente: a lista da tela Minha Companhia (abre a tela ou atualiza a que está aberta). */
public class PacketCompanyList {
    /** Limite de linhas por pacote (ninguém tem tantos companheiros; só protege o pacote). */
    private static final int MAX_ENTRIES = 256;

    private final int formation;
    private final boolean openScreen;
    private final List<CompanyEntry> entries;

    public PacketCompanyList(int formation, boolean openScreen, List<CompanyEntry> entries) {
        this.formation = formation;
        this.openScreen = openScreen;
        this.entries = entries.size() > MAX_ENTRIES ? entries.subList(0, MAX_ENTRIES) : entries;
    }

    public PacketCompanyList(FriendlyByteBuf buf) {
        this.formation = buf.readByte();
        this.openScreen = buf.readBoolean();
        int n = Math.min(buf.readVarInt(), MAX_ENTRIES);
        List<CompanyEntry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(CompanyEntry.read(buf));
        this.entries = list;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeByte(formation);
        buf.writeBoolean(openScreen);
        buf.writeVarInt(entries.size());
        for (CompanyEntry e : entries) e.write(buf);
    }

    public int getFormation() { return formation; }
    public boolean isOpenScreen() { return openScreen; }
    public List<CompanyEntry> getEntries() { return entries; }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> io.github.raffasanttos.epicvanguard.client.ClientPacketHandler.handleCompanyList(this)));
        context.setPacketHandled(true);
        return true;
    }
}
