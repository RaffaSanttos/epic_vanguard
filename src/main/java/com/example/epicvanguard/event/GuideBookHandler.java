package com.example.epicvanguard.event;

import com.example.epicvanguard.EpicVanguardMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Entrega o Guia de Campo da Vanguarda (livro do Patchouli) uma única vez, no primeiro login do jogador no mundo.
 * O Patchouli é opcional: sem ele nada acontece. Não usa nenhuma classe do Patchouli, só o ID do item,
 * então esta classe carrega normalmente quando ele não está instalado.
 */
@Mod.EventBusSubscriber(modid = EpicVanguardMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GuideBookHandler {

    public static final String BOOK_ID = EpicVanguardMod.MOD_ID + ":guide";
    private static final String GIVEN_TAG = EpicVanguardMod.MOD_ID + ":guide_given";
    private static final ResourceLocation PATCHOULI_BOOK_ITEM = new ResourceLocation("patchouli", "guide_book");

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModList.get().isLoaded("patchouli")) return;

        // Dados persistentes do Forge: sobrevivem à morte do jogador
        CompoundTag persistent = player.getPersistentData();
        CompoundTag modData = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        if (modData.getBoolean(GIVEN_TAG)) return;

        ItemStack book = createGuideBook();
        if (book.isEmpty()) return;

        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        modData.putBoolean(GIVEN_TAG, true);
        persistent.put(Player.PERSISTED_NBT_TAG, modData);
        player.sendSystemMessage(Component.translatable("epicvanguard.text.guide_book.given"));
    }

    /** O livro do guia, ou vazio se o Patchouli não estiver instalado. */
    public static ItemStack createGuideBook() {
        Item item = ForgeRegistries.ITEMS.getValue(PATCHOULI_BOOK_ITEM);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString("patchouli:book", BOOK_ID);
        return stack;
    }
}
