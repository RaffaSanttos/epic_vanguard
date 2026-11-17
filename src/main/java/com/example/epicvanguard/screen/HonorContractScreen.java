package com.example.epicvanguard.screen;


import net.minecraft.client.resources.language.I18n;
import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.init.ModItems;
import com.example.epicvanguard.networking.Messages;
import com.example.epicvanguard.networking.packet.PacketRecruitWarrior;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HonorContractScreen extends AbstractContainerScreen<HonorContractMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(EpicVanguardMod.MOD_ID, "textures/gui/honor_contract.png");

    private Button recruitButton;
    private Button closeButton;

    public HonorContractScreen(HonorContractMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 190;
        this.imageHeight = 170;
    }

    @Override
    protected void init() {
        super.init();

        Player player = Minecraft.getInstance().player;
        int coins = player != null ? countPlayerCoins(player) : 0;
        boolean isCreative = player != null && player.isCreative();

        int cost = 40;
        if (Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(menu.getEntityId());
            if (entity instanceof WarriorCompanionEntity warrior) {
                cost = warrior.getRecruitCost();
            }
        }

        boolean canAfford = isCreative || coins >= cost;

        this.recruitButton = Button.builder(
                Component.literal(canAfford ? I18n.get("epicvanguard.ui.honor_contract_screen.1") : I18n.get("epicvanguard.ui.honor_contract_screen.2")),
                btn -> {
                    Messages.sendToServer(new PacketRecruitWarrior(menu.getEntityId()));
                    this.onClose();
                })
                .bounds(leftPos + 18, topPos + 134, 88, 20)
                .build();
        this.recruitButton.active = canAfford;

        this.closeButton = Button.builder(
                Component.translatable("epicvanguard.text.honor_contract_screen.1"),
                btn -> this.onClose())
                .bounds(leftPos + 112, topPos + 134, 60, 20)
                .build();

        this.addRenderableWidget(this.recruitButton);
        this.addRenderableWidget(this.closeButton);
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
        if (Minecraft.getInstance().level == null) return;

        Entity entity = Minecraft.getInstance().level.getEntity(menu.getEntityId());
        if (!(entity instanceof WarriorCompanionEntity warrior)) return;

        Player player = Minecraft.getInstance().player;
        int coins = player != null ? countPlayerCoins(player) : 0;
        boolean isCreative = player != null && player.isCreative();
        int cost = warrior.getRecruitCost();
        boolean canAfford = isCreative || coins >= cost;

        // Titulo centralizado
        String titleStr = I18n.get("epicvanguard.ui.honor_contract_screen.3");
        int titleX = (this.imageWidth - this.font.width(titleStr)) / 2;
        guiGraphics.drawString(this.font, titleStr, titleX, 12, 0x8B4513, false);

        // Informacoes do mercenario com contraste perfeito no pergaminho
        guiGraphics.drawString(this.font, I18n.get("epicvanguard.ui.honor_contract_screen.4") + warrior.getWarriorName(), 18, 30, 0x333333, false);

        int tier = warrior.getEquipmentTier();
        String tierName = switch (tier) {
            case 0 -> I18n.get("epicvanguard.ui.honor_contract_screen.101");
            case 1 -> I18n.get("epicvanguard.ui.honor_contract_screen.102");
            case 2 -> I18n.get("epicvanguard.ui.honor_contract_screen.103");
            case -1 -> I18n.get("epicvanguard.ui.honor_contract_screen.104");
            default -> I18n.get("epicvanguard.ui.honor_contract_screen.5");
        };
        guiGraphics.drawString(this.font, I18n.get("epicvanguard.ui.honor_contract_screen.6") + tierName, 18, 44, 0x333333, false);
        guiGraphics.drawString(this.font, I18n.get("epicvanguard.ui.honor_contract_screen.7") + cost + I18n.get("epicvanguard.ui.honor_contract_screen.8"), 18, 58, 0x333333, false);

        String coinStr = isCreative ? I18n.get("epicvanguard.ui.honor_contract_screen.9") : (canAfford ? "§2§l" : "§c§l") + coins + " §8/ §6§l" + cost + " 🪙";
        guiGraphics.drawString(this.font, I18n.get("epicvanguard.ui.honor_contract_screen.10") + coinStr, 18, 72, 0x333333, false);

        // Mensagem descritiva com quebra de linha automatica dentro das margens do pergaminho
        String desc = canAfford
                ? I18n.get("epicvanguard.ui.honor_contract_screen.11")
                : I18n.get("epicvanguard.ui.honor_contract_screen.12") + (cost - coins) + I18n.get("epicvanguard.ui.honor_contract_screen.13");
        guiGraphics.drawWordWrap(this.font, FormattedText.of(desc), 18, 90, 154, 0x444444);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static int countPlayerCoins(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(ModItems.GOLD_COIN.get())) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
