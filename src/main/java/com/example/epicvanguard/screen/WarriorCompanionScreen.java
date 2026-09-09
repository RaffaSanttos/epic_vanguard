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

import java.util.ArrayList;
import java.util.List;

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

        WarriorCompanionEntity warrior = getCompanion();
        if (warrior != null) {
            // 1. Barra de Vida (Painel Direito)
            float hp = Math.max(0.0F, warrior.getHealth());
            float maxHp = Math.max(1.0F, warrior.getMaxHealth());
            float pct = Math.min(1.0F, hp / maxHp);

            int barX = leftPos + 164;
            int barY = topPos + 28;
            int barW = 44;
            int barH = 5;

            // Fundo escuro da barra de vida
            guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF350505);

            // Preenchimento dinâmico
            int filled = (int) (pct * barW);
            if (filled > 0) {
                int fillColor = pct > 0.5F ? 0xFF2ECC71 : (pct > 0.25F ? 0xFFF39C12 : 0xFFE74C3C);
                guiGraphics.fill(barX, barY, barX + filled, barY + barH, fillColor);
            }

            // 2. Barra de Experiência e Nível (Abaixo da Mochila)
            int lvl = warrior.getWarriorLevel();
            int currentXp = warrior.getWarriorExperience();
            int nextXp = WarriorCompanionEntity.getXpForNextLevel(lvl);
            float xpPct = (lvl >= 20) ? 1.0F : (nextXp > 0 ? Math.min(1.0F, (float) currentXp / nextXp) : 0.0F);

            int xpBarX = leftPos + 61;
            int xpBarY = topPos + 85;
            int xpBarW = 88;
            int xpBarH = 5;

            // Borda externa escura com relevo
            guiGraphics.fill(xpBarX - 1, xpBarY - 1, xpBarX + xpBarW + 1, xpBarY + xpBarH + 1, 0xFF373737);
            // Fundo escuro interno
            guiGraphics.fill(xpBarX, xpBarY, xpBarX + xpBarW, xpBarY + xpBarH, 0xFF141A14);

            // Preenchimento de XP
            int fillW = (int) (xpPct * xpBarW);
            if (fillW > 0) {
                if (lvl >= 20) {
                    // Dourado reluzente para nível máximo
                    guiGraphics.fill(xpBarX, xpBarY, xpBarX + fillW, xpBarY + xpBarH, 0xFFFFD700);
                    guiGraphics.fill(xpBarX, xpBarY, xpBarX + fillW, xpBarY + 1, 0xFFFFF2A3);
                } else {
                    // Verde esmeralda de XP Minecraft com brilho superior e sombra inferior
                    guiGraphics.fill(xpBarX, xpBarY, xpBarX + fillW, xpBarY + xpBarH, 0xFF00D836);
                    guiGraphics.fill(xpBarX, xpBarY, xpBarX + fillW, xpBarY + 1, 0xFF80FF80);
                    guiGraphics.fill(xpBarX, xpBarY + xpBarH - 1, xpBarX + fillW, xpBarY + xpBarH, 0xFF009922);
                }
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

            // Indicador de Nível e Classe acima da barra de XP (centralizado sob a Mochila: x=105)
            int lvl = warrior.getWarriorLevel();
            int currentXp = warrior.getWarriorExperience();
            int nextXp = WarriorCompanionEntity.getXpForNextLevel(lvl);
            float xpPct = (lvl >= 20) ? 1.0F : (nextXp > 0 ? Math.min(1.0F, (float) currentXp / nextXp) : 0.0F);

            String title = warrior.getFormattedSpecializationTitle();
            String levelText = "§2Nv. §l" + lvl + "§r §8• " + title;
            int lvlW = this.font.width(levelText);
            int lvlX = 105 - (lvlW / 2);
            guiGraphics.drawString(this.font, levelText, lvlX, 75, 0x404040, false);

            // Texto numérico de XP abaixo da barra (alinhado à direita para não conflitar com 'Inventory')
            String xpText;
            if (lvl >= 20) {
                xpText = "§6★ Nível Máximo ★";
            } else {
                xpText = "§8" + currentXp + "/" + nextXp + " XP §2(" + (int) (xpPct * 100) + "%)";
            }
            int xpW = this.font.width(xpText);
            int xpX = 149 - xpW;
            guiGraphics.drawString(this.font, xpText, xpX, 93, 0x505050, false);
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

        // Tooltip rica ao passar o mouse sobre o nível e a barra de XP
        WarriorCompanionEntity warrior = getCompanion();
        if (warrior != null) {
            int xpAreaX = leftPos + 60;
            int xpAreaY = topPos + 74;
            if (mouseX >= xpAreaX && mouseX <= xpAreaX + 90 && mouseY >= xpAreaY && mouseY <= topPos + 102) {
                int lvl = warrior.getWarriorLevel();
                int currentXp = warrior.getWarriorExperience();
                int nextXp = WarriorCompanionEntity.getXpForNextLevel(lvl);
                float xpPct = (lvl >= 20) ? 1.0F : (nextXp > 0 ? Math.min(1.0F, (float) currentXp / nextXp) : 0.0F);

                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal("§6§l✦ Experiência de Combate ✦"));
                tooltip.add(Component.literal("§7Nível Atual: §eNv. " + lvl + " " + warrior.getFormattedSpecializationTitle()));
                if (lvl >= 20) {
                    tooltip.add(Component.literal("§6★ Nível Máximo Atingido! ★"));
                    tooltip.add(Component.literal("§7Este guerreiro atingiu o ápice de seu poder."));
                } else {
                    tooltip.add(Component.literal("§7Progresso: §a" + currentXp + " §7/ §f" + nextXp + " XP §7(" + (int) (xpPct * 100) + "%)"));
                    tooltip.add(Component.literal("§7XP Restante: §e" + (nextXp - currentXp) + " XP"));
                    tooltip.add(Component.literal("§8Derrote monstros em batalha para evoluir!"));
                }
                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
        }
    }
}
