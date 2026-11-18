# Auditoria técnica — Epic Vanguard

Revisão de código feita em 17/11/2026 (versão 1.0.14, Forge 47.4.0 / MC 1.20.1).
Legenda: ✅ resolvido nesta revisão · ⏳ pendente · 💡 sugestão

---

## 1. Segurança em multiplayer (pacotes C→S)

O servidor não pode confiar no cliente: um cliente modificado envia qualquer pacote, com qualquer ID de entidade.

| Pacote | Problema | Status |
|---|---|---|
| `PacketWarriorCombatMode` | Qualquer jogador mudava o modo do guerreiro de outro; aceitava qualquer número de modo | ✅ exige dono + distância ≤ 10 blocos; modo limitado a 0–2 |
| `PacketWarriorTactics` | Qualquer jogador mudava alvos (hostis/passivos) do guerreiro de outro | ✅ exige dono + distância |
| `PacketWarriorCancelAction` | Qualquer jogador cancelava o alvo do guerreiro de outro | ✅ exige dono + distância |
| `PacketOpenWarriorGUI` | Abria o **inventário** de guerreiro recrutado de outro jogador (roubo de itens); confiava no flag `recruited` do cliente | ✅ usa o estado do servidor; inventário só abre para o dono |
| `WarriorCompanionMenu.stillValid` | Não checava dono | ✅ fecha a tela se quem está com ela aberta não for o dono |
| `PacketRecruitWarrior`, `PacketRenameWarrior`, `PacketPostVanguardContract` | — | já validavam (distância/dono) |

As validações ficam em [`networking/PacketGuards.java`](../src/main/java/com/example/epicvanguard/networking/PacketGuards.java). **Todo pacote novo que mexa num guerreiro deve usar `PacketGuards.ownedWarrior(...)` ou `nearbyWarrior(...)`.**

## 2. Persistência dos companheiros (`CompanionSavedData`)

| Item | Status |
|---|---|
| Snapshot completo de NBT de **cada** guerreiro recrutado a cada 1 s (`tick % 20`) → save do mundo pesado | ✅ agora a cada 10 s; a saída de chunk/mundo (`EntityLeaveLevelEvent`) continua salvando na hora |
| **Duplicação de itens**: ao rematerializar um guerreiro a partir do snapshot, a cópia antiga continuava no chunk original. Se ela carregasse primeiro, a cópia nova era descartada e a antiga voltava com os itens antigos | ✅ campo `generation` em `CompanionInfo` + `RestoreGen` no NBT da entidade; cópia com geração menor é descartada ao entrar no mundo e nunca sobrescreve o snapshot |
| **Ressurreição**: guerreiro morto era removido do registro; uma cópia antiga num chunk descarregado podia reaparecer viva (com inventário) | ✅ `retire(uuid)` guarda o UUID dos mortos (`Retired` no save); cópias desses UUIDs são descartadas |
| `DATA_NAME = "grimal_companions"` — nome copiado do outro mod | ⏳ trocar para `epicvanguard_companions` **com migração** (ler o arquivo antigo se o novo não existir), senão os saves atuais perdem os companheiros |
| Mundos antigos: entradas sem `gen` carregam como 0, compatível | ✅ |

## 3. Permissões de comando

- ⏳ `VanguardCommand.hasAdminAccess` considera **qualquer jogador em criativo** como admin. Em servidor com criativo liberado, qualquer um usa comandos administrativos. Sugestão: exigir só `hasPermission(2)` ou criar opção de config.

## 4. Dependências e licença

- ✅ `epicfight` está como opcional no `mods.toml`. **Corrigido (18/11/2026):** `EpicFightCompat` tinha `@Mod.EventBusSubscriber` com tipos do Epic Fight nos métodos, o que derrubaria o carregamento sem o Epic Fight. Agora é registrada à mão em `EpicVanguardMod` só se `isEpicFightLoaded()`; o código de fora do pacote `compat` só chama a integração depois dessa checagem.
- ⏳ `libs/epic-fight-20.14.17-mc1.20.1-forge.jar` está versionado no git. Conferir se a licença do Epic Fight permite redistribuir o jar; o ideal é baixar via CurseMaven/Modrinth Maven no `build.gradle`:
  ```groovy
  repositories { maven { url "https://cursemaven.com" } }
  dependencies { implementation fg.deobf("curse.maven:epic-fight-mod-405076:<fileId>") }
  ```
- ⏳ Licença do mod é "All Rights Reserved" — decidir se é isso mesmo antes de publicar.

## 5. Organização do código

- ⏳ `WarriorCompanionEntity.java` tem ~1.850 linhas. Sugestão de divisão: `WarriorPersistence` (save/load/snapshot), `WarriorInteraction` (`mobInteract`, contrato, prisioneiro), `WarriorThreatSensor` (creepers/projéteis/ameaças), progressão (nível/XP/especialização).
- ⏳ Pacote `com.example.epicvanguard` — trocar por um domínio seu (ex.: `io.github.raffasanttos.epicvanguard`) antes de publicar.
- ⏳ `scratch_test_nbt.java` na raiz (ferramenta de debug que procura `tavern.nbt`, que não existe mais) — mover para uma pasta `tools/` ou apagar.
- ✅ `.gitgnore` (cópia com erro de digitação do `.gitignore`) removido; `.gitignore` agora ignora `.idea/`, `*.iml`, `.vscode/`, `*.class`.
- ⏳ `build_jar.bat` anuncia `build\libs\epicvanguard-1.0.0.jar`, mas o jar real se chama `EpicVanguard <versão> [1.20.1].jar`.
- 💡 `build/libs` acumula 14 jars antigos; rodar `gradlew clean` antes de gerar release.

## 6. Tradução / textos

- ✅ **Corrigido (relato de usuário do CurseForge: "o mod está todo em espanhol").** O `en_us.json` só cobria nomes e falas; chat, telas e comandos eram português fixo. Agora:
  - 110 mensagens usam `Component.translatable` (`epicvanguard.text.*`, `.spec.*`, `.mode.*`, `.friendly_fire.*`).
  - 109 textos das telas usam `I18n.get`.
  - `en_us.json`/`pt_br.json` com as mesmas 332 chaves; `mod_description` em inglês.
- ⏳ Restam em português: mensagens de log, exceções internas e as palavras aceitas como argumento de comando (`ativado`/`desativado`). Também aceitas: `off`/`spells`/`all`.
- ⏳ Revisar as traduções em inglês dentro do jogo; foram feitas por IA e não foram testadas em jogo.

## 7. Testes e documentação

- ⏳ Não há testes. Começar por lógica pura: custo de recrutamento, curva de XP/nível, `FriendlyFireMode.fromString`.
- ⏳ **Plano registrado (17/11/2026), a fazer depois:**
  1. **Testes automáticos com GameTest do Forge** (`@GameTestHolder` + `./gradlew runGameTestServer`). Rodam dentro de um mundo de verdade, sem abrir o jogo, e podem entrar no CI.
  2. **Smoke test com `./gradlew runServer`**: subir um servidor dedicado e ler o log procurando erro de carregamento, registro ou classe de cliente carregada no servidor.
  - Casos GameTest prioritários: jogador que não é dono não comanda nem abre o inventário do guerreiro (`PacketGuards`); cópia antiga (geração menor) é descartada ao carregar; guerreiro morto (`Retired`) não reaparece; toda chave `epicvanguard.*` usada no código existe nos dois idiomas.
- ⏳ Não há README. Sugestão: o que o mod faz, como recrutar, modos de combate, comandos `/vanguard`, dependências, como compilar.
- 💡 Avisos de compilação `ResourceLocation(String,String)` deprecado: normal na 1.20.1; só precisa trocar por `ResourceLocation.fromNamespaceAndPath` ao migrar para 1.21.

## 8. Segunda revisão (18/11/2026)

| Problema | Correção |
|---|---|
| `/fogoamigo <modo>` sem permissão: qualquer jogador mudava o modo global | Exige operador (`hasPermission(2)`) |
| Duplicação na morte: o `Mob` dropava o equipamento (8,5% por slot ao morrer para jogador) e depois o inventário inteiro | Chance de drop dos slots de equipamento zerada no construtor |
| Vanguard Point quebrado não dropava nada | `loot_tables/blocks/vanguard_point.json` |
| Dispensar companheiro em chunk descarregado não valia: ele voltava ao registro ao carregar | `CompanionSavedData.dismiss` + conjunto `Dismissed` no save; a entidade é liberada ao entrar no mundo |
| Renomear por comando com o guerreiro descarregado: o nome antigo voltava | Ao entrar no mundo, o nome do registro prevalece |
| `findOrLoadCompanion` carregava chunk na hora (lag) e quase nunca achava a entidade (carregamento assíncrono) | Só procura entidades carregadas; senão usa o snapshot (a geração descarta a cópia antiga) |
| Vida baixa sem comida: guerreiro ficava passivo para sempre (não regenera) | `shouldAvoidCombat()` só evita combate se houver consumível; regeneração lenta fora de combate (1 HP / 3 s) |
| Escudo reduzia queda, lava, fogo, veneno; Duelista esquivava de queda e lava | Só vale para dano com entidade causadora e que não ignora escudo |
| Teleporte entre dimensões continuava usando a entidade removida e tentava teleportar duas vezes | `moveToLevel` devolve a entidade nova; `safeTeleportTo` devolve quem está ao lado do alvo |
| Recuo de emergência lia a ameaça depois de limpar o alvo | Ameaça lida antes |
| Duelo recrutava qualquer jogador que desse o golpe; `DuelGoal` não era usado | Só o adversário do duelo recruta; `DuelGoal` removido |
| Moedas do Vanguard Point eram de todos (retirar, usar, troco para o jogador mais próximo) | Dono do depósito (`DepositorUUID`): só ele retira, completa, usa no edital e recebe o troco |
| Nome padrão "Guerreiro" no jogo em inglês | `WarriorCompanionEntity.DEFAULT_NAME = "Warrior"`; "Guerreiro" antigo ainda é reconhecido |
| `/grimal` conflitava com o comando do outro mod | Removido |
| Reflection de XP a cada morte de qualquer mob | `Method` cacheado e XP só calculado quando um guerreiro/jogador está envolvido |

Verificado: `gradlew compileJava` sem erros. **Não testado em jogo**, inclusive o carregamento sem o Epic Fight.

---

## Como verificar as correções

1. `gradlew compileJava` — compila sem erros (verificado em 17/11/2026).
2. Teste manual em LAN/servidor com 2 jogadores (**ainda não feito**):
   - Jogador B tenta abrir/comandar o guerreiro do jogador A → nada acontece.
   - Recrutar um guerreiro, afastar-se até o chunk descarregar, teleportar (resgate) → só uma cópia existe; voltar ao local original → a cópia antiga some sem dropar itens.
   - Guerreiro morre, voltar ao chunk onde havia cópia antiga → ela não reaparece.
