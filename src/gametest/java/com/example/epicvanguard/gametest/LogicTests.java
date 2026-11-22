package com.example.epicvanguard.gametest;

import com.example.epicvanguard.EpicVanguardMod;
import com.example.epicvanguard.dialogue.PersonalityArchetype;
import com.example.epicvanguard.dialogue.SpeechTrigger;
import com.example.epicvanguard.entity.CompanionSavedData;
import com.example.epicvanguard.entity.CompanionSavedData.FriendlyFireMode;
import com.example.epicvanguard.entity.WarriorCompanionEntity;
import com.example.epicvanguard.event.GuideBookHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.example.epicvanguard.gametest.TestUtil.*;

/** Regras puras (XP, fogo amigo), traduções e o livro do Patchouli. */
@GameTestHolder(EpicVanguardMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class LogicTests {

    @GameTest(template = PLATFORM)
    public static void xpCurveIsSane(GameTestHelper helper) {
        checkEquals(50, WarriorCompanionEntity.getXpForNextLevel(1), "XP do nível 1 para o 2");
        for (int lvl = 2; lvl < 20; lvl++) {
            check(WarriorCompanionEntity.getXpForNextLevel(lvl) > WarriorCompanionEntity.getXpForNextLevel(lvl - 1),
                    "a XP necessária deveria crescer a cada nível (nível " + lvl + ")");
        }
        checkEquals(0, WarriorCompanionEntity.getXpForNextLevel(20), "XP no nível máximo");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void warriorLevelsUpAndCaps(GameTestHelper helper) {
        WarriorCompanionEntity warrior = warrior(helper, 4, 4);
        warrior.setWarriorLevel(1);
        float baseHealth = warrior.getMaxHealth();

        warrior.addWarriorExperience(WarriorCompanionEntity.getXpForNextLevel(1));
        checkEquals(2, warrior.getWarriorLevel(), "nível depois da XP exata para subir");
        check(warrior.getMaxHealth() > baseHealth, "a vida máxima deveria aumentar ao subir de nível");

        warrior.addWarriorExperience(1_000_000);
        checkEquals(20, warrior.getWarriorLevel(), "nível máximo");
        checkEquals(0, warrior.getWarriorExperience(), "XP guardada no nível máximo");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void friendlyFireWordsInBothLanguages(GameTestHelper helper) {
        String[][] cases = {
                {"off", "DISABLED"}, {"livre", "DISABLED"}, {"desativado", "DISABLED"}, {"qualquer coisa", "DISABLED"},
                {"melee", "MELEE_ONLY"}, {"fisico", "MELEE_ONLY"},
                {"spells", "SPELLS_ONLY"}, {"magias", "SPELLS_ONLY"},
                {"all", "ALL"}, {"total", "ALL"}, {"ON", "ALL"},
        };
        for (String[] c : cases) {
            checkEquals(FriendlyFireMode.valueOf(c[1]), FriendlyFireMode.fromString(c[0]), "/friendlyfire " + c[0]);
        }
        checkEquals(FriendlyFireMode.DISABLED, new CompanionSavedData().getGlobalFriendlyFireMode(), "fogo amigo padrão");
        helper.succeed();
    }

    private static JsonObject lang(String code) {
        String path = "/assets/" + EpicVanguardMod.MOD_ID + "/lang/" + code + ".json";
        try (InputStream in = EpicVanguardMod.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("arquivo não encontrado: " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @GameTest(template = PLATFORM)
    public static void translationsAreComplete(GameTestHelper helper) {
        JsonObject en = lang("en_us");
        JsonObject pt = lang("pt_br");

        Set<String> onlyEn = new TreeSet<>(en.keySet());
        onlyEn.removeAll(pt.keySet());
        Set<String> onlyPt = new TreeSet<>(pt.keySet());
        onlyPt.removeAll(en.keySet());
        check(onlyEn.isEmpty() && onlyPt.isEmpty(), "chaves só em inglês: " + onlyEn + " / só em português: " + onlyPt);

        // Chaves montadas em tempo de execução (não aparecem como texto fixo no código)
        List<String> dynamic = new ArrayList<>();
        for (PersonalityArchetype p : PersonalityArchetype.values()) {
            dynamic.add("epicvanguard.personality." + p.getKey());
            for (SpeechTrigger trigger : SpeechTrigger.values()) {
                for (int v = 1; v <= trigger.getVariantCount(); v++) {
                    dynamic.add("speech.epicvanguard." + p.getKey() + "." + trigger.getKey() + "." + v);
                }
            }
        }
        for (String spec : new String[]{"warrior", "berserker", "guardian", "duelist"}) {
            dynamic.add("epicvanguard.spec." + spec + ".name");
            dynamic.add("epicvanguard.spec." + spec + ".title");
        }
        for (FriendlyFireMode mode : FriendlyFireMode.values()) {
            String key = mode.name().toLowerCase(java.util.Locale.ROOT);
            dynamic.add("epicvanguard.friendly_fire." + key + ".name");
            dynamic.add("epicvanguard.friendly_fire." + key + ".formatted");
        }
        for (String mode : new String[]{"follow", "guard", "stay"}) dynamic.add("epicvanguard.mode." + mode);
        for (String k : new String[]{"name", "subtitle", "landing_text"}) dynamic.add("book.epicvanguard.guide." + k);

        List<String> missing = new ArrayList<>();
        for (String key : dynamic) if (!en.has(key)) missing.add(key);
        check(missing.isEmpty(), "chaves faltando nas traduções: " + missing);
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void guideBookOnlyWithPatchouli(GameTestHelper helper) {
        boolean patchouli = ModList.get().isLoaded("patchouli");
        ItemStack book = GuideBookHandler.createGuideBook();
        boolean recipe = helper.getLevel().getServer().getRecipeManager()
                .byKey(new ResourceLocation(EpicVanguardMod.MOD_ID, "guide_book")).isPresent();

        if (!patchouli) {
            check(book.isEmpty(), "sem o Patchouli não deveria existir livro");
            check(!recipe, "sem o Patchouli a receita do livro não deveria carregar");
            helper.succeed();
            return;
        }

        check(!book.isEmpty(), "com o Patchouli o livro deveria existir");
        checkEquals(GuideBookHandler.BOOK_ID, book.getOrCreateTag().getString("patchouli:book"), "livro do item");
        check(recipe, "a receita do livro (livro + moeda) deveria carregar");

        // Jogador novo recebe o livro ao entrar
        ServerPlayer newcomer = player(helper, false, 4, 4);
        withPlayers(() -> {
            boolean hasBook = false;
            for (ItemStack stack : newcomer.getInventory().items) {
                if (stack.hasTag() && GuideBookHandler.BOOK_ID.equals(stack.getTag().getString("patchouli:book"))) {
                    hasBook = true;
                }
            }
            check(hasBook, "o jogador deveria receber o guia no primeiro login");
        }, newcomer);
        helper.succeed();
    }
}
