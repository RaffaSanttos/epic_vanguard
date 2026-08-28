package com.example.epicvanguard.screen;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.networking.Messages;
import com.example.epicvanguard.networking.packet.PacketWarriorCancelAction;
import com.example.epicvanguard.networking.packet.PacketWarriorCombatMode;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
            "§c⚔ Ataque", "§9🛡 Defesa", "§e🏠 Guarda", "§7⏹ Parado"
    };

    private int currentMode = 0;
    private Button modeButton;

    public WarriorCompanionScreen(WarriorCompanionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 194;
        this.imageHeight = 188;
        this.inventoryLabelY = 94;
        this.inventoryLabelX = 16;
        this.titleLabelX = 12;
        this.titleLabelY = 8;

        if (Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(menu.getEntityId());
            if (entity instanceof WarriorCompanionEntity warrior) {
                this.currentMode = warrior.getCombatMode();
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        modeButton = Button.builder(
                Component.literal(MODE_NAMES[currentMode % 4]),
                btn -> {
                    currentMode = (currentMode + 1) % 4;
                    btn.setMessage(Component.literal(MODE_NAMES[currentMode]));
                    Messages.sendToServer(new PacketWarriorCombatMode(menu.getEntityId(), currentMode));
                })
                .bounds(leftPos + 83, topPos + 76, 52, 16)
                .build();

        Button cancelButton = Button.builder(
                Component.literal("§c🛑 Parar"),
                btn -> {
                    Messages.sendToServer(new PacketWarriorCancelAction(menu.getEntityId()));
                })
                .bounds(leftPos + 137, topPos + 76, 36, 16)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Cessar-fogo: Interrompe o combate imediatamente")))
                .build();

        this.addRenderableWidget(modeButton);
        this.addRenderableWidget(cancelButton);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "Equip.", 12, 8, 0x404040, false);
        guiGraphics.drawString(this.font, "Mochila", 82, 8, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
