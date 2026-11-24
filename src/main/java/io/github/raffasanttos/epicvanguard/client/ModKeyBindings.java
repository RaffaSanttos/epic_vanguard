package io.github.raffasanttos.epicvanguard.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.raffasanttos.epicvanguard.EpicVanguardMod;
import io.github.raffasanttos.epicvanguard.networking.Messages;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketCompanyAction;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Teclas do mod (mudáveis em Opções > Controles > Epic Vanguard). */
public final class ModKeyBindings {

    private ModKeyBindings() {}

    public static final KeyMapping OPEN_COMPANY = new KeyMapping(
            "key.epicvanguard.my_company",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.epicvanguard");

    @Mod.EventBusSubscriber(modid = EpicVanguardMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(OPEN_COMPANY);
        }
    }

    @Mod.EventBusSubscriber(modid = EpicVanguardMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Handler {
        private Handler() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_COMPANY.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    // O servidor responde com a lista e a tela abre (ClientPacketHandler)
                    Messages.sendToServer(new PacketCompanyAction(PacketCompanyAction.OPEN, null, 0));
                }
            }
        }
    }
}
