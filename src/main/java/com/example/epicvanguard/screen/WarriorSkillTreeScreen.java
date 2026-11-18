package com.example.epicvanguard.screen;


import net.minecraft.client.resources.language.I18n;
import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.entity.BerserkerCompanionEntity;
import com.example.epicvanguard.entity.DuelistCompanionEntity;
import com.example.epicvanguard.entity.GuardianCompanionEntity;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.networking.Messages;
import com.example.epicvanguard.networking.packet.PacketOpenWarriorGUI;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class WarriorSkillTreeScreen extends Screen {
    private static final ResourceLocation PARCHMENT =
            new ResourceLocation(EpicVanguardMod.MOD_ID, "textures/gui/skill_tree_bg.png");

    private final int entityId;
    private int selectedSpec = 1; // 1 = Berserker, 2 = Guardião, 3 = Duelista
    private int leftPos;
    private int topPos;
    private final int imageWidth = 256;
    private final int imageHeight = 230;

    private Button tabMochilaButton;
    private Button tabHabilidadesButton;

    public static class SkillNode {
        public final int level;
        public final String title;
        public final String summary;
        public final List<String> details;

        public SkillNode(int level, String title, String summary, List<String> details) {
            this.level = level;
            this.title = title;
            this.summary = summary;
            this.details = details;
        }
    }

    public WarriorSkillTreeScreen(int entityId) {
        super(Component.translatable("epicvanguard.text.warrior_skill_tree_screen.1"));
        this.entityId = entityId;

        if (Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity instanceof BerserkerCompanionEntity) {
                this.selectedSpec = 1;
            } else if (entity instanceof GuardianCompanionEntity) {
                this.selectedSpec = 2;
            } else if (entity instanceof DuelistCompanionEntity) {
                this.selectedSpec = 3;
            } else if (entity instanceof WarriorCompanionEntity warrior) {
                int spec = warrior.getSpecialization();
                this.selectedSpec = (spec >= 1 && spec <= 3) ? spec : 1;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        // Abas superiores de navegação
        this.tabMochilaButton = Button.builder(
                Component.translatable("epicvanguard.text.warrior_skill_tree_screen.2"),
                btn -> {
                    Messages.sendToServer(new PacketOpenWarriorGUI(entityId, true));
                })
                .bounds(leftPos + 8, topPos - 18, 70, 18)
                .tooltip(Tooltip.create(Component.translatable("epicvanguard.text.warrior_skill_tree_screen.3")))
                .build();

        this.tabHabilidadesButton = Button.builder(
                Component.translatable("epicvanguard.text.warrior_skill_tree_screen.4"),
                btn -> {})
                .bounds(leftPos + 80, topPos - 18, 90, 18)
                .build();
        this.tabHabilidadesButton.active = false; // Aba atualmente ativa, sem tooltip obstrutiva

        this.addRenderableWidget(tabMochilaButton);
        this.addRenderableWidget(tabHabilidadesButton);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private WarriorCompanionEntity getCompanion() {
        if (Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity instanceof WarriorCompanionEntity warrior) {
                return warrior;
            }
        }
        return null;
    }

    private List<SkillNode> getNodesForSpec(int spec) {
        List<SkillNode> list = new ArrayList<>();
        // Nó 1: Nv. 1
        list.add(new SkillNode(
                1,
                I18n.get("epicvanguard.ui.warrior_skill_tree_screen.1"),
                I18n.get("epicvanguard.ui.warrior_skill_tree_screen.2"),
                List.of(
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.3"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.4"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.5"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.6"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.7")
                )
        ));

        // Nó 2: Nv. 3
        list.add(new SkillNode(
                3,
                I18n.get("epicvanguard.ui.warrior_skill_tree_screen.8"),
                I18n.get("epicvanguard.ui.warrior_skill_tree_screen.9"),
                List.of(
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.10"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.11"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.12"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.13")
                )
        ));

        // Nó 3: Nv. 5
        if (spec == 1) {
            list.add(new SkillNode(
                    5,
                    I18n.get("epicvanguard.ui.warrior_skill_tree_screen.14"),
                    I18n.get("epicvanguard.ui.warrior_skill_tree_screen.15"),
                    List.of(
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.16"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.17"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.18"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.19"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.20"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.21")
                    )
            ));
        } else if (spec == 2) {
            list.add(new SkillNode(
                    5,
                    I18n.get("epicvanguard.ui.warrior_skill_tree_screen.22"),
                    I18n.get("epicvanguard.ui.warrior_skill_tree_screen.23"),
                    List.of(
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.24"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.25"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.26"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.27"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.28"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.29"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.30")
                    )
            ));
        } else {
            list.add(new SkillNode(
                    5,
                    I18n.get("epicvanguard.ui.warrior_skill_tree_screen.31"),
                    I18n.get("epicvanguard.ui.warrior_skill_tree_screen.32"),
                    List.of(
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.33"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.34"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.35"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.36"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.37"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.38")
                    )
            ));
        }

        // Nó 4: Nv. 10
        if (spec == 1) {
            list.add(new SkillNode(
                    10,
                    I18n.get("epicvanguard.ui.warrior_skill_tree_screen.39"),
                    I18n.get("epicvanguard.ui.warrior_skill_tree_screen.40"),
                    List.of(
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.41"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.42")
                    )
            ));
        } else if (spec == 2) {
            list.add(new SkillNode(
                    10,
                    I18n.get("epicvanguard.ui.warrior_skill_tree_screen.43"),
                    I18n.get("epicvanguard.ui.warrior_skill_tree_screen.44"),
                    List.of(
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.45"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.46")
                    )
            ));
        } else {
            list.add(new SkillNode(
                    10,
                    I18n.get("epicvanguard.ui.warrior_skill_tree_screen.47"),
                    I18n.get("epicvanguard.ui.warrior_skill_tree_screen.48"),
                    List.of(
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.49"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.50")
                    )
            ));
        }

        // Nó 5: Nv. 20
        list.add(new SkillNode(
                20,
                I18n.get("epicvanguard.ui.warrior_skill_tree_screen.51"),
                I18n.get("epicvanguard.ui.warrior_skill_tree_screen.52"),
                List.of(
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.53"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.54"),
                        I18n.get("epicvanguard.ui.warrior_skill_tree_screen.55")
                )
        ));

        return list;
    }

    private void drawClampedString(GuiGraphics guiGraphics, String text, int x, int y, int maxW, int color) {
        if (this.font.width(text) > maxW) {
            String trimmed = this.font.plainSubstrByWidth(text, Math.max(10, maxW - 8)) + "...";
            guiGraphics.drawString(this.font, trimmed, x, y, color, false);
        } else {
            guiGraphics.drawString(this.font, text, x, y, color, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);

        // Fundo pergaminho medieval completo (256x230)
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, PARCHMENT);
        guiGraphics.blit(PARCHMENT, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        WarriorCompanionEntity warrior = getCompanion();
        int companionLevel = warrior != null ? warrior.getWarriorLevel() : 1;
        String companionName = warrior != null ? warrior.getWarriorName() : WarriorCompanionEntity.DEFAULT_NAME;
        String companionTitle = warrior != null ? warrior.getFormattedSpecializationTitle().getString() : I18n.get("epicvanguard.ui.warrior_skill_tree_screen.56");
        int currentXp = warrior != null ? warrior.getWarriorExperience() : 0;
        int nextXp = WarriorCompanionEntity.getXpForNextLevel(companionLevel);
        float xpPct = (companionLevel >= 20) ? 1.0F : (nextXp > 0 ? Math.min(1.0F, (float) currentXp / nextXp) : 0.0F);

        // 1. Cabeçalho
        String headerTitle = switch (selectedSpec) {
            case 1 -> I18n.get("epicvanguard.ui.warrior_skill_tree_screen.101");
            case 2 -> I18n.get("epicvanguard.ui.warrior_skill_tree_screen.102");
            case 3 -> I18n.get("epicvanguard.ui.warrior_skill_tree_screen.103");
            default -> I18n.get("epicvanguard.ui.warrior_skill_tree_screen.57");
        };
        drawClampedString(guiGraphics, headerTitle, leftPos + 12, topPos + 7, 232, 0x222222);

        String subHeader = I18n.get("epicvanguard.ui.warrior_skill_tree_screen.58") + companionName + I18n.get("epicvanguard.ui.warrior_skill_tree_screen.59") + companionLevel + " §8(" + companionTitle + "§8)";
        drawClampedString(guiGraphics, subHeader, leftPos + 12, topPos + 18, 232, 0x333333);

        // 2. Painel de Atributos em Combate
        int panelX = leftPos + 10;
        int panelY = topPos + 29;
        int panelW = imageWidth - 20;
        int panelH = 43;

        // Fundo do painel escuro com borda bronze esculpida
        guiGraphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xE01E1610);
        guiGraphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY, 0xFF5A4025);
        guiGraphics.fill(panelX - 1, panelY + panelH, panelX + panelW + 1, panelY + panelH + 1, 0xFF5A4025);
        guiGraphics.fill(panelX - 1, panelY, panelX, panelY + panelH, 0xFF5A4025);
        guiGraphics.fill(panelX + panelW, panelY, panelX + panelW + 1, panelY + panelH, 0xFF5A4025);

        if (warrior != null) {
            float hp = warrior.getHealth();
            float maxHp = warrior.getMaxHealth();
            float dmg = warrior.getCalculatedAttackDamage();
            float armor = (float) warrior.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            double speed = warrior.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);

            // Linha 1: 4 Atributos principais com ícones coloridos
            String hpStr = "§4❤ §f" + String.format("%.1f", hp) + "/" + String.format("%.1f", maxHp);
            String dmgStr = "§c⚔ §f" + String.format("%.1f", dmg);
            String armStr = "§9🛡 §f" + String.format("%.1f", armor);
            String spdStr = "§e⚡ §f" + (selectedSpec == 3 ? "0.32" : String.format("%.2f", speed));

            guiGraphics.drawString(this.font, hpStr, panelX + 6, panelY + 4, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, dmgStr, panelX + 70, panelY + 4, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, armStr, panelX + 125, panelY + 4, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, spdStr, panelX + 175, panelY + 4, 0xFFFFFF, false);

            // Linha 2: Buffs e Passivas da Classe (Conciso e sem vazamentos)
            String specBuff = switch (selectedSpec) {
                case 1 -> I18n.get("epicvanguard.ui.warrior_skill_tree_screen.104");
                case 2 -> I18n.get("epicvanguard.ui.warrior_skill_tree_screen.105");
                case 3 -> I18n.get("epicvanguard.ui.warrior_skill_tree_screen.106");
                default -> I18n.get("epicvanguard.ui.warrior_skill_tree_screen.60");
            };
            drawClampedString(guiGraphics, specBuff, panelX + 6, panelY + 16, panelW - 12, 0xFFFFFF);

            // Linha 3: Barra de XP dentro do painel
            int xpX = panelX + 6;
            int xpY = panelY + 28;
            int xpW = panelW - 12;
            int xpH = 4;

            guiGraphics.fill(xpX - 1, xpY - 1, xpX + xpW + 1, xpY + xpH + 1, 0xFF443322);
            guiGraphics.fill(xpX, xpY, xpX + xpW, xpY + xpH, 0xFF141A14);
            int xpFill = (int) (xpPct * xpW);
            if (xpFill > 0) {
                int xpColor = (companionLevel >= 20) ? 0xFFFFD700 : 0xFF00D836;
                guiGraphics.fill(xpX, xpY, xpX + xpFill, xpY + xpH, xpColor);
            }
            String xpLabel = (companionLevel >= 20) ? I18n.get("epicvanguard.ui.warrior_skill_tree_screen.61") : I18n.get("epicvanguard.ui.warrior_skill_tree_screen.62") + currentXp + " §7/ §f" + nextXp + " §8(" + (int)(xpPct * 100) + "%)";
            drawClampedString(guiGraphics, xpLabel, xpX + 2, xpY + 5, xpW - 4, 0xCCCCCC);
        }

        // 3. Árvore de Habilidades (5 Cards)
        List<SkillNode> nodes = getNodesForSpec(selectedSpec);
        int nodeY = topPos + 75;
        int nodeH = 24;
        int nodeSpacing = 28;

        SkillNode hoveredNode = null;

        for (int i = 0; i < nodes.size(); i++) {
            SkillNode node = nodes.get(i);
            int currentY = nodeY + i * nodeSpacing;
            boolean isUnlocked = (companionLevel >= node.level);

            // Cores do nó
            int borderColor = isUnlocked ? 0xFF2ECC71 : 0xFF555555;
            int bgColor = isUnlocked ? 0xE61E2E1E : 0xE6242424;
            int cardX = leftPos + 10;
            int cardW = imageWidth - 20;

            // Linha conectora vertical com o nó anterior
            if (i > 0) {
                int lineX = cardX + 10;
                int lineY = currentY - 4;
                int lineH = 4;
                guiGraphics.fill(lineX, lineY, lineX + 2, lineY + lineH, borderColor);
            }

            // Fundo e borda do card
            guiGraphics.fill(cardX - 1, currentY - 1, cardX + cardW + 1, currentY + nodeH + 1, borderColor);
            guiGraphics.fill(cardX, currentY, cardX + cardW, currentY + nodeH, bgColor);

            // Ícone / Indicador de status
            String statusIcon = isUnlocked ? "§a✔" : "§c🔒";
            guiGraphics.drawString(this.font, statusIcon, cardX + 5, currentY + 4, 0xFFFFFF, false);

            // Nível e Título (Clamped para nunca vazar)
            String reqStr = isUnlocked ? I18n.get("epicvanguard.ui.warrior_skill_tree_screen.63") + node.level + "]" : I18n.get("epicvanguard.ui.warrior_skill_tree_screen.64") + node.level + "]";
            String titleStr = reqStr + " " + (isUnlocked ? "§f§l" : "§7§l") + node.title;
            drawClampedString(guiGraphics, titleStr, cardX + 18, currentY + 3, cardW - 22, 0xFFFFFF);

            // Resumo da habilidade (Clamped para nunca vazar)
            drawClampedString(guiGraphics, "§7" + node.summary, cardX + 18, currentY + 13, cardW - 22, 0xCCCCCC);

            // Hover check
            if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY && mouseY <= currentY + nodeH) {
                hoveredNode = node;
            }
        }

        super.render(guiGraphics, mouseX, mouseY, delta);

        // Tooltip rica ao passar o mouse sobre o nó
        if (hoveredNode != null) {
            boolean isUnlocked = (companionLevel >= hoveredNode.level);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§6§l✦ " + hoveredNode.title + " ✦"));
            tooltip.add(Component.literal(isUnlocked ? I18n.get("epicvanguard.ui.warrior_skill_tree_screen.65") : I18n.get("epicvanguard.ui.warrior_skill_tree_screen.66") + hoveredNode.level + ")"));
            tooltip.add(Component.literal(""));
            for (String detail : hoveredNode.details) {
                tooltip.add(Component.literal(detail));
            }
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
