package io.github.raffasanttos.epicvanguard.client;

import io.github.raffasanttos.epicvanguard.networking.packet.PacketCompanyList;
import io.github.raffasanttos.epicvanguard.screen.MyCompanyScreen;
import net.minecraft.client.Minecraft;

/** Pacotes que chegam no cliente. Só é carregada no cliente (via DistExecutor). */
public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void handleCompanyList(PacketCompanyList packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MyCompanyScreen screen) {
            screen.update(packet.getFormation(), packet.getEntries());
        } else if (packet.isOpenScreen() && mc.screen == null) {
            mc.setScreen(new MyCompanyScreen(packet.getFormation(), packet.getEntries()));
        }
    }
}
