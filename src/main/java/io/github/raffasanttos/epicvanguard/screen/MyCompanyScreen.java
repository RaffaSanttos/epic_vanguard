package io.github.raffasanttos.epicvanguard.screen;

import io.github.raffasanttos.epicvanguard.entity.WarriorCompanionEntity;
import io.github.raffasanttos.epicvanguard.entity.util.WarriorFormation;
import io.github.raffasanttos.epicvanguard.networking.Messages;
import io.github.raffasanttos.epicvanguard.networking.packet.CompanyEntry;
import io.github.raffasanttos.epicvanguard.networking.packet.PacketCompanyAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela "Minha Companhia" (tecla J por padrão): todos os seus companheiros, carregados ou não, com classe, nível,
 * vida, postura e onde estão. Dá para chamar um ou todos, trocar a postura e escolher a formação do modo Seguir.
 * Os dados vêm do servidor ({@code PacketCompanyList}); cada clique pede ao servidor, que responde com a lista nova.
 */
public class MyCompanyScreen extends Screen {

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 222;
    private static final int ROW_H = 30;
    private static final int LIST_TOP = 46;
    private static final int VISIBLE_ROWS = 5;

    private List<CompanyEntry> entries;
    private int formation;
    private int scroll = 0;
    private int left, top;

    public MyCompanyScreen(int formation, List<CompanyEntry> entries) {
        super(Component.translatable("epicvanguard.company.title"));
        this.formation = formation;
        this.entries = new ArrayList<>(entries);
    }

    /** Resposta nova do servidor com a tela já aberta. */
    public void update(int formation, List<CompanyEntry> entries) {
        this.formation = formation;
        this.entries = new ArrayList<>(entries);
        this.scroll = Mth.clamp(scroll, 0, maxScroll());
        this.rebuildWidgets();
    }

    private int maxScroll() {
        return Math.max(0, entries.size() - VISIBLE_ROWS);
    }

    @Override
    protected void init() {
        left = (this.width - PANEL_W) / 2;
        top = (this.height - PANEL_H) / 2;

        addRenderableWidget(Button.builder(
                        Component.translatable("epicvanguard.company.formation", WarriorFormation.name(formation)),
                        b -> send(PacketCompanyAction.SET_FORMATION, null, WarriorFormation.next(formation)))
                .bounds(left + 8, top + 20, 150, 18)
                .tooltip(Tooltip.create(Component.translatable("epicvanguard.company.formation.tooltip")))
                .build());

        Button callAll = Button.builder(Component.translatable("epicvanguard.company.call_all"),
                        b -> send(PacketCompanyAction.CALL_ALL, null, 0))
                .bounds(left + PANEL_W - 108, top + 20, 100, 18)
                .tooltip(Tooltip.create(Component.translatable("epicvanguard.company.call_all.tooltip")))
                .build();
        callAll.active = !entries.isEmpty();
        addRenderableWidget(callAll);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int i = scroll + row;
            if (i >= entries.size()) break;
            CompanyEntry e = entries.get(i);
            int y = top + LIST_TOP + row * ROW_H + 5;

            addRenderableWidget(Button.builder(Component.translatable("epicvanguard.company.call"),
                            b -> send(PacketCompanyAction.CALL, e.id(), 0))
                    .bounds(left + PANEL_W - 122, y, 50, 18)
                    .tooltip(Tooltip.create(Component.translatable("epicvanguard.company.call.tooltip")))
                    .build());

            Button mode = Button.builder(modeName(e.mode()),
                            b -> send(PacketCompanyAction.SET_MODE, e.id(), (e.mode() + 1) % 3))
                    .bounds(left + PANEL_W - 70, y, 62, 18)
                    .tooltip(Tooltip.create(Component.translatable(e.loaded()
                            ? "epicvanguard.company.mode.tooltip" : "epicvanguard.company.mode.unloaded")))
                    .build();
            mode.active = e.loaded();
            addRenderableWidget(mode);
        }
    }

    private void send(int action, java.util.UUID id, int value) {
        Messages.sendToServer(new PacketCompanyAction(action, id, value));
    }

    private static Component modeName(int mode) {
        return Component.translatable(switch (mode) {
            case WarriorCompanionEntity.MODE_GUARD -> "epicvanguard.mode.guard";
            case WarriorCompanionEntity.MODE_STAY -> "epicvanguard.mode.stay";
            default -> "epicvanguard.mode.follow";
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int next = Mth.clamp(scroll - (int) Math.signum(delta), 0, maxScroll());
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        // Painel no estilo do inventário vanilla
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFFC6C6C6);
        g.fill(left, top, left + PANEL_W - 1, top + 1, 0xFFFFFFFF);
        g.fill(left, top, left + 1, top + PANEL_H - 1, 0xFFFFFFFF);
        g.fill(left + PANEL_W - 1, top + 1, left + PANEL_W, top + PANEL_H, 0xFF555555);
        g.fill(left + 1, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFF555555);

        g.drawString(font, Component.translatable("epicvanguard.company.header", entries.size()), left + 8, top + 7, 0x404040, false);

        // Área da lista
        int listX = left + 6, listY = top + LIST_TOP, listW = PANEL_W - 12, listH = VISIBLE_ROWS * ROW_H;
        g.fill(listX, listY, listX + listW, listY + listH, 0xFF8B8B8B);

        if (entries.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("epicvanguard.company.empty"), left + PANEL_W / 2, listY + listH / 2 - 4, 0xFFFFFF);
        }

        Player me = Minecraft.getInstance().player;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int i = scroll + row;
            if (i >= entries.size()) break;
            CompanyEntry e = entries.get(i);
            int y = listY + row * ROW_H;
            g.fill(listX + 1, y + 1, listX + listW - 1, y + ROW_H - 1, row % 2 == 0 ? 0xFF6F6F6F : 0xFF666666);

            // Linha 1: nome, classe e nível
            Component line1 = Component.literal("§f§l" + e.name() + " §r")
                    .append(WarriorCompanionEntity.specializationTitle(e.specialization()))
                    .append(Component.literal(" §7" + Component.translatable("epicvanguard.company.level", e.level()).getString()));
            drawClipped(g, line1, listX + 5, y + 4, PANEL_W - 140);

            // Linha 2: barra de vida + onde está
            int barX = listX + 5, barY = y + 17, barW = 50;
            float frac = e.maxHealth() > 0 ? Mth.clamp(e.health() / e.maxHealth(), 0.0F, 1.0F) : 0.0F;
            g.fill(barX, barY, barX + barW, barY + 5, 0xFF2A0000);
            g.fill(barX, barY, barX + Math.round(barW * frac), barY + 5, frac > 0.5F ? 0xFF3FBF3F : frac > 0.25F ? 0xFFE0C030 : 0xFFD03030);
            String hp = (int) Math.ceil(e.health()) + "/" + (int) Math.ceil(e.maxHealth());
            g.drawString(font, hp, barX + barW + 4, y + 16, 0xFFFFFF, false);
            drawClipped(g, where(me, e), barX + barW + 4 + font.width(hp) + 6, y + 16, PANEL_W - 140 - barW - font.width(hp) - 10);
        }

        // Barra de rolagem
        if (maxScroll() > 0) {
            int trackX = listX + listW - 3;
            int thumbH = Math.max(10, listH * VISIBLE_ROWS / entries.size());
            int thumbY = listY + (listH - thumbH) * scroll / maxScroll();
            g.fill(trackX, listY, trackX + 2, listY + listH, 0xFF555555);
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xFFDDDDDD);
        }

        g.drawString(font, Component.translatable("epicvanguard.company.hint"), left + 8, top + PANEL_H - 12, 0x404040, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Onde ele está: distância (mesma dimensão) ou dimensão + coordenadas; "longe" se não estiver carregado. */
    private Component where(Player me, CompanyEntry e) {
        String coords = e.x() + " " + e.y() + " " + e.z();
        Component base;
        if (me != null && me.level().dimension().location().toString().equals(e.dimension())) {
            int dist = (int) Math.sqrt(me.blockPosition().distSqr(new net.minecraft.core.BlockPos(e.x(), e.y(), e.z())));
            base = Component.translatable("epicvanguard.company.distance", dist, coords);
        } else {
            base = Component.translatable("epicvanguard.company.elsewhere", dimensionName(e.dimension()), coords);
        }
        if (!e.loaded()) {
            base = Component.empty().append(base).append(Component.translatable("epicvanguard.company.unloaded"));
        }
        return base;
    }

    private static Component dimensionName(String dim) {
        return switch (dim) {
            case "minecraft:overworld" -> Component.translatable("epicvanguard.company.dim.overworld");
            case "minecraft:the_nether" -> Component.translatable("epicvanguard.company.dim.nether");
            case "minecraft:the_end" -> Component.translatable("epicvanguard.company.dim.end");
            default -> Component.literal(dim.contains(":") ? dim.substring(dim.indexOf(':') + 1) : dim);
        };
    }

    /** Escreve cortando com "..." se não couber. */
    private void drawClipped(GuiGraphics g, Component text, int x, int y, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            g.drawString(font, text, x, y, 0xFFFFFF, false);
            return;
        }
        String s = font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - font.width("...")));
        g.drawString(font, s + "...", x, y, 0xFFFFFF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
