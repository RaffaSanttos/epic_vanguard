package com.example.epicvanguard.screen;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.networking.Messages;
import com.example.epicvanguard.networking.packet.PacketWarriorCancelAction;
import com.example.epicvanguard.networking.packet.PacketWarriorCombatMode;
import com.example.epicvanguard.networking.packet.PacketWarriorTactics;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;

public class WarriorCompanionScreen extends AbstractContainerScreen<WarriorCompanionMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(EpicVanguardMod.MOD_ID, "textures/gui/warrior_hud.png");

    private static final String[] MODE_NAMES = {
            "§a🚶 Seguir", "§e🏠 Guarda", "§7⏹ Parado"
    };

    private int currentMode = 0;
    private boolean targetHostiles = true;
    private boolean targetPassives = false;

    private Button modeButton;
    private Button cancelButton;
    private Button hostilesButton;
    private Button passivesButton;

    public WarriorCompanionScreen(WarriorCompanionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 224;
        this.imageHeight = 192;
        this.inventoryLabelY = 98;
        this.inventoryLabelX = 31;
        this.titleLabelX = 12;
        this.titleLabelY = 8;

        if (Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(menu.getEntityId());
            if (entity instanceof WarriorCompanionEntity warrior) {
                this.currentMode = warrior.getCombatMode() % 3;
                this.targetHostiles = warrior.isTargetHostiles();
                this.targetPassives = warrior.isTargetPassives();
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        // Painel Direito: Botão 1 - Modo de Postura (Y = 37, 46x16)
        modeButton = Button.builder(
                Component.literal(MODE_NAMES[currentMode % 3]),
                btn -> {
                    currentMode = (currentMode + 1) % 3;
                    btn.setMessage(Component.literal(MODE_NAMES[currentMode]));
                    Messages.sendToServer(new PacketWarriorCombatMode(menu.getEntityId(), currentMode));
                })
                .bounds(leftPos + 163, topPos + 37, 46, 16)
                .tooltip(Tooltip.create(Component.literal("Postura: Clique para alternar entre Seguir, Guarda e Parado")))
                .build();

        // Painel Direito: Botão 2 - Cessar-fogo / Cancelar Alvo (Y = 55, 46x16)
        cancelButton = Button.builder(
                Component.literal("§c🛑 Parar"),
                btn -> {
                    Messages.sendToServer(new PacketWarriorCancelAction(menu.getEntityId()));
                })
                .bounds(leftPos + 163, topPos + 55, 46, 16)
                .tooltip(Tooltip.create(Component.literal("Cessar-fogo: Interrompe o combate e limpa o alvo imediatamente")))
                .build();

        // Painel Direito: Botão 3 - Ícone Monstros Hostis (Y = 73, 22x18)
        hostilesButton = Button.builder(
                Component.literal(targetHostiles ? "§c⚔" : "§8⚔"),
                btn -> {
                    targetHostiles = !targetHostiles;
                    btn.setMessage(Component.literal(targetHostiles ? "§c⚔" : "§8⚔"));
                    Messages.sendToServer(new PacketWarriorTactics(menu.getEntityId(), targetHostiles, targetPassives));
                })
                .bounds(leftPos + 163, topPos + 73, 22, 18)
                .tooltip(Tooltip.create(Component.literal("Monstros Hostis: ON / OFF (Atacar zumbis, esqueletos, etc.)")))
                .build();

        // Painel Direito: Botão 4 - Ícone Caça de Comida / Passivos (Y = 73, 22x18)
        passivesButton = Button.builder(
                Component.literal(targetPassives ? "§6🍖" : "§8🍖"),
                btn -> {
                    targetPassives = !targetPassives;
                    btn.setMessage(Component.literal(targetPassives ? "§6🍖" : "§8🍖"));
                    Messages.sendToServer(new PacketWarriorTactics(menu.getEntityId(), targetHostiles, targetPassives));
                })
                .bounds(leftPos + 187, topPos + 73, 22, 18)
                .tooltip(Tooltip.create(Component.literal("Caçar Comida: ON / OFF (Abater animais adultos para alimento)")))
                .build();

        this.addRenderableWidget(modeButton);
        this.addRenderableWidget(cancelButton);
        this.addRenderableWidget(hostilesButton);
        this.addRenderableWidget(passivesButton);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Renderiza a barra de vida da companhia no painel direito
        WarriorCompanionEntity warrior = getCompanion();
        if (warrior != null) {
            float hp = Math.max(0.0F, warrior.getHealth());
            float maxHp = Math.max(1.0F, warrior.getMaxHealth());
            float pct = Math.min(1.0F, hp / maxHp);

            int barX = leftPos + 164;
            int barY = topPos + 28;
            int barW = 44;
            int barH = 5;

            // Fundo escuro da barra
            guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF350505);

            // Preenchimento com cor dinâmica (verde -> laranja -> vermelho)
            int filled = (int) (pct * barW);
            if (filled > 0) {
                int fillColor = pct > 0.5F ? 0xFF2ECC71 : (pct > 0.25F ? 0xFFF39C12 : 0xFFE74C3C);
                guiGraphics.fill(barX, barY, barX + filled, barY + barH, fillColor);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Títulos das seções
        guiGraphics.drawString(this.font, "Equip.", 12, 8, 0x404040, false);
        guiGraphics.drawString(this.font, "Mochila", 60, 8, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // Painel Direito: Vida Total da Companhia
        guiGraphics.drawString(this.font, "§4❤ §8Vida", 168, 7, 0x404040, false);
        WarriorCompanionEntity warrior = getCompanion();
        if (warrior != null) {
            int curHp = (int) Math.ceil(warrior.getHealth());
            int maxHp = (int) warrior.getMaxHealth();
            String hpStr = curHp + "/" + maxHp;
            int textW = this.font.width(hpStr);
            guiGraphics.drawString(this.font, hpStr, 186 - (textW / 2), 18, 0x202020, false);
        }
    }

    private WarriorCompanionEntity getCompanion() {
        if (Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(menu.getEntityId());
            if (entity instanceof WarriorCompanionEntity warrior) {
                return warrior;
            }
        }
        return null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
