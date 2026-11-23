package io.github.raffasanttos.epicvanguard.screen;


import net.minecraft.client.resources.language.I18n;
import io.github.raffasanttos.epicvanguard.EpicVanguardMod;
import io.github.raffasanttos.epicvanguard.block.entity.VanguardPointBlockEntity;
import io.github.raffasanttos.epicvanguard.networking.Messages;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketPostVanguardContract;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class VanguardPointScreen extends AbstractContainerScreen<VanguardPointMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(EpicVanguardMod.MOD_ID, "textures/gui/vanguard_point.png");

    private Button postButton;
    private Button accelerateButton;

    public VanguardPointScreen(VanguardPointMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 190;
        this.imageHeight = 196;
        this.inventoryLabelY = 94;
        this.inventoryLabelX = 15;
        this.titleLabelX = 15;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();

        Player player = Minecraft.getInstance().player;
        boolean isCreative = player != null && player.isCreative();

        this.postButton = Button.builder(
                Component.translatable("epicvanguard.text.vanguard_point_screen.1"),
                btn -> {
                    Messages.sendToServer(new PacketPostVanguardContract(menu.getPos(), false));
                })
                .bounds(leftPos + 30, topPos + 74, 130, 18)
                .tooltip(Tooltip.create(Component.translatable("epicvanguard.text.vanguard_point_screen.2")))
                .build();

        this.addRenderableWidget(this.postButton);

        if (isCreative) {
            this.accelerateButton = Button.builder(
                    Component.translatable("epicvanguard.text.vanguard_point_screen.3"),
                    btn -> {
                        Messages.sendToServer(new PacketPostVanguardContract(menu.getPos(), true));
                    })
                    .bounds(leftPos + 130, topPos + 22, 52, 14)
                    .tooltip(Tooltip.create(Component.translatable("epicvanguard.text.vanguard_point_screen.4")))
                    .build();

            this.addRenderableWidget(this.accelerateButton);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        Player player = Minecraft.getInstance().player;
        boolean isCreative = player != null && player.isCreative();
        boolean active = menu.isContractActive();
        int coins = menu.getStoredCoins();
        boolean canAfford = isCreative || coins >= VanguardPointBlockEntity.requiredCoins();

        if (active) {
            postButton.active = false;
            postButton.setMessage(Component.translatable("epicvanguard.text.vanguard_point_screen.5"));
            if (accelerateButton != null) {
                accelerateButton.visible = true;
            }
        } else {
            postButton.active = canAfford;
            postButton.setMessage(Component.literal(canAfford ? I18n.get("epicvanguard.ui.vanguard_point_screen.1") : I18n.get("epicvanguard.ui.vanguard_point_screen.2")));
            if (accelerateButton != null) {
                accelerateButton.visible = false;
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (menu.isContractActive()) {
            // Cobre a área do slot com uma elegante barra de progresso da marcha
            int barX = leftPos + 20;
            int barY = topPos + 54;
            int barW = 150;
            int barH = 14;

            // Borda e fundo escuro da barra de marcha
            guiGraphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF46321E);
            guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF2A1C12);

            // Preenchimento de progresso
            int remTicks = menu.getRemainingTicks();
            int totalTicks = menu.getTotalTicks();
            float progress = 1.0F - ((float) remTicks / (float) totalTicks);
            progress = Math.max(0.0F, Math.min(1.0F, progress));

            int fillW = (int) (progress * barW);
            if (fillW > 0) {
                // Gradiente dourado de marcha militar
                guiGraphics.fill(barX, barY, barX + fillW, barY + barH, 0xFFD4AF37);
                guiGraphics.fill(barX, barY, barX + fillW, barY + 2, 0xFFFFE57F);
                guiGraphics.fill(barX, barY + barH - 2, barX + fillW, barY + barH, 0xFFAA8020);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Título centralizado no topo do pergaminho
        String titleStr = I18n.get("epicvanguard.ui.vanguard_point_screen.3");
        int titleX = (this.imageWidth - this.font.width(titleStr)) / 2;
        guiGraphics.drawString(this.font, titleStr, titleX, 6, 0x8B4513, false);

        if (!menu.isContractActive()) {
            // Modo Inativo: Publicar Edital
            guiGraphics.drawString(this.font, I18n.get("epicvanguard.ui.vanguard_point_screen.4"), 15, 23, 0x333333, false);
            guiGraphics.drawString(this.font, I18n.get("epicvanguard.ui.vanguard_point_screen.5"), 15, 34, 0x333333, false);

            int coins = menu.getStoredCoins();
            String coinStatus = (coins >= VanguardPointBlockEntity.requiredCoins() ? "§2§l" : "§c§l") + coins + I18n.get("epicvanguard.ui.vanguard_point_screen.6");
            guiGraphics.drawString(this.font, I18n.get("epicvanguard.ui.vanguard_point_screen.7") + coinStatus, 15, 45, 0x333333, false);

            guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        } else {
            // Modo Ativo: Marcha das Tropas
            String marchingTitle = I18n.get("epicvanguard.ui.vanguard_point_screen.8");
            int mx = (this.imageWidth - this.font.width(marchingTitle)) / 2;
            guiGraphics.drawString(this.font, marchingTitle, mx, 23, 0x8B4513, false);

            int remTicks = menu.getRemainingTicks();
            int days = (int) Math.ceil((double) remTicks / 24000.0);
            if (days < 1 && remTicks > 0) days = 1;

            int totalSec = remTicks / 20;
            int hours = totalSec / 3600;
            int mins = (totalSec % 3600) / 60;
            int secs = totalSec % 60;
            String timeStr = (hours > 0 ? hours + "h " : "") + mins + "m " + secs + "s";

            String dayText = I18n.get("epicvanguard.ui.vanguard_point_screen.9") + days + (days == 1 ? I18n.get("epicvanguard.ui.vanguard_point_screen.10") : I18n.get("epicvanguard.ui.vanguard_point_screen.11")) + " §8(" + timeStr + ")";
            int dx = (this.imageWidth - this.font.width(dayText)) / 2;
            guiGraphics.drawString(this.font, dayText, dx, 36, 0x333333, false);

            // Texto sobre a barra de progresso
            float progress = 1.0F - ((float) remTicks / (float) menu.getTotalTicks());
            int pct = (int) (progress * 100);
            String pctStr = "§f§l" + pct + I18n.get("epicvanguard.ui.vanguard_point_screen.12");
            int px = (this.imageWidth - this.font.width(pctStr)) / 2;
            guiGraphics.drawString(this.font, pctStr, px, 57, 0xFFFFFF, true);

            guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
