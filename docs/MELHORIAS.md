# Lista de melhorias — Epic Vanguard

Levantamento feito em 20/11/2026 (versão 1.0.14, Forge 47.4.0 / MC 1.20.1), lendo o código-fonte.
Complementa a [AUDITORIA.md](AUDITORIA.md): itens que já estão lá aparecem aqui só quando ainda estão pendentes ou quando a correção registrada não funciona.

Nada aqui foi testado em jogo. Prioridades:

- 🔴 **Alta**: bug confirmado ou risco em multiplayer
- 🟠 **Média**: comportamento estranho para o jogador ou mecânica incompleta
- 🟢 **Baixa**: organização, desempenho ou acabamento
- ✅ **Corrigido** no código (compila, ainda não testado em jogo)

---

## 1. Bugs

### ✅ 1.1 Renomear por comando com o guerreiro descarregado não funciona — corrigido (21/11/2026)
A auditoria (seção 8) registra isso como corrigido, mas a correção não funciona:

1. `/vanguard renomear` grava o nome novo só em `CompanionInfo.name` ([VanguardCommand.java:585](../src/main/java/io/github/raffasanttos/epicvanguard/command/VanguardCommand.java)).
2. Quando o chunk carrega, `readAdditionalSaveData` chama `setWarriorName(nome antigo do NBT)`, que também chama `CompanionSavedData.updateName(...)` ([WarriorCompanionEntity.java:638](../src/main/java/io/github/raffasanttos/epicvanguard/entity/WarriorCompanionEntity.java)).
3. Quando `CompanionTeleportHandler.onEntityJoinLevel` compara os nomes, o registro já tem o nome antigo e o rename se perde.

Também por isso, uma **cópia antiga** (geração menor) sobrescreve o nome no registro antes de ser descartada.

**Correção:** separar o "aplicar nome na entidade" do "gravar no registro". Durante `readAdditionalSaveData`, só aplicar na entidade, sem chamar `updateName`. Gravar no registro só em ações do jogador (name tag, pacote de rename, comando).

### ✅ 1.2 Ataque do guerreiro ignora encantamentos e a arma nunca gasta — corrigido (21/11/2026)
`doHurtTarget` ([WarriorCompanionEntity.java:1436](../src/main/java/io/github/raffasanttos/epicvanguard/entity/WarriorCompanionEntity.java)) só chama `target.hurt(...)`. O que falta:
- Bônus de encantamento (`EnchantmentHelper.getDamageBonus`): Afiação, Julgamento, Ruína dos Artrópodes.
- Aspecto Flamejante e Repulsão (empurrão do golpe).
- `EnchantmentHelper.doPostHurtEffects` / `doPostDamageEffects` (Espinhos, efeitos de mods).
- `getMainHandItem().hurtAndBreak(...)`: a arma tem durabilidade infinita.

**Correção:** seguir a estrutura do `Mob#doHurtTarget` vanilla e aplicar os multiplicadores de especialização em cima dele. Conferir também o que acontece quando o golpe vem pelas animações do Epic Fight.

### ✅ 1.3 Guerreiro recém-recrutado fica em modo Guarda — corrigido (22/11/2026)
Agora quem é recrutado (moedas, resgate ou duelo) passa por `applyRecruitDefaults()`: postura **Seguir** e **pacífico** (Espada e Carne desligadas). Ele continua defendendo o dono e a si mesmo; o jogador liga o ataque a monstros pela tela.
- Constantes `MODE_FOLLOW/GUARD/STAY` em `WarriorCompanionEntity`; comentário de `CompanionInfo.combatMode` atualizado; chaves sem uso apagadas dos lang.
- Falta: trocar os `setCombatMode(1)` dos guerreiros **não recrutados** (casa, prisão, Posto) pelas constantes. Eles ficam de guarda de propósito.

### ✅ 1.4 Recrutar por moedas sem checar o estado do guerreiro — corrigido (21/11/2026)
`PacketRecruitWarrior` não verifica `isAlive()` nem `isDuelMode()`, e não usa `PacketGuards.nearbyWarrior`.
**Correção:** usar `PacketGuards.nearbyWarrior(...)` e recusar o recrutamento durante um duelo.

### ✅ 1.5 Drops da morte não passam pelo `LivingDropsEvent` — corrigido (24/11/2026): os itens saem em `dropEquipment()` com `spawnAtLocation`, dentro da captura do Forge
`dropAllInventory` cria os `ItemEntity` direto com `addFreshEntity`. Mods de lápide e de keep-inventory não enxergam esses itens.
**Correção:** chamar `spawnAtLocation(stack)` dentro de `dropCustomDeathLoot`, que já roda com captura de drops. Tirar a segunda chamada de `dropAllInventory()` em `die()`.

---

### ✅ 1.6 Armadura do guerreiro nunca gasta — corrigido (22/11/2026)
O `LivingEntity#hurtArmor` vanilla não faz nada para mobs (só o jogador sobrescreve). A armadura do guerreiro nunca perde durabilidade.
**Correção:** sobrescrever `hurtArmor` e `hurtHelmet` em `WarriorCompanionEntity`, desgastando os slots de armadura como o `Inventory#hurtArmor` do jogador. Decidir se o desgaste deve ser igual ao do jogador ou menor.

### ✅ 1.7 Guerreiros de saves antigos ainda podem duplicar o equipamento — corrigido (22/11/2026)
A correção de duplicação zera a chance de drop dos slots de equipamento no construtor. Mas o `Mob#readAdditionalSaveData` vanilla lê `ArmorDropChances`/`HandDropChances` do NBT e sobrescreve esse zero. Guerreiros salvos antes da correção (versão 1.0.0, a publicada) voltam com a chance padrão de 8,5% por slot. Se um deles morrer para um jogador (PvP ou fogo amigo), `super.dropCustomDeathLoot` dropa uma cópia da peça, e depois `dropAllInventory` dropa o original.
**Correção:** zerar as chances de novo no fim de `readAdditionalSaveData` (ou sobrescrever `getEquipmentDropChance` para devolver 0). Dá para cobrir com um GameTest que carrega um NBT com `ArmorDropChances` antigo.

---

## 2. Segurança e multiplayer

### ✅ 2.1 Jogador em criativo é tratado como admin nos comandos — corrigido (21/11/2026)
`VanguardCommand.hasAdminAccess` aceita `player.isCreative()`. Num servidor com criativo liberado, qualquer um pode trazer, renomear, dispensar ou ir até os companheiros dos outros.
**Correção:** exigir só `hasPermission(2)`, ou criar uma opção de config `allowCreativeAdmin` (padrão `false`).

### ✅ 2.2 Canal de rede aceita qualquer versão — corrigido (21/11/2026)
Em `Messages.register`, `clientAcceptedVersions(s -> true)` e `serverAcceptedVersions(s -> true)` deixam versões diferentes do mod se conectarem. O resultado são pacotes com layout diferente e erros estranhos.
**Correção:** `PROTOCOL_VERSION::equals`, e aumentar `PROTOCOL_VERSION` sempre que algum pacote mudar.

### ✅ 2.3 Pacote de abrir GUI manda um `boolean recruited` inútil — corrigido (24/11/2026)
(O protocolo foi para "3" na 1.0.18 por causa da coleta e da escolha de classe; dá para aproveitar o próximo aumento.)
`PacketOpenWarriorGUI` já ignora o flag e usa o estado do servidor. Dá para remover o campo e economizar o byte. Isso exige aumentar a versão do protocolo (2.2).

### ✅ 2.4 Criativo mexe nas moedas depositadas por outro jogador no Posto da Vanguarda — corrigido (22/11/2026)
`VanguardPointBlockEntity.canAccessCoins` aceita `player.isCreative()`. É o mesmo problema da 2.1: num servidor com criativo liberado, alguém pode tirar as moedas que outro depositou. O impacto é pequeno, porque quem está em criativo pode criar moedas.
**Correção:** aceitar só o depositante ou `hasPermissions(2)`. Criativo continua contratando de graça (`canStartContract`), mas sem usar as moedas dos outros.

---

### ✅ 2.5 Funil tirava as moedas de outro jogador do Posto da Vanguarda — corrigido (22/11/2026)
O bloco expunha o slot de moedas como `ITEM_HANDLER` em todos os lados. Um funil embaixo puxava as moedas depositadas por outro jogador, passando por cima da checagem de dono do menu. Agora o slot não é exposto a funis e canos.

### ✅ 2.6 Quebrar o Posto roubava moedas ou cancelava o edital alheio — corrigido (22/11/2026)
Qualquer um podia quebrar o bloco: as moedas de outro jogador caíam no chão e um edital pago em andamento se perdia. Agora só quem depositou, quem pagou o edital ou um operador consegue minerar o bloco (`canBreak`). Explosões ainda quebram.

### ➖ 2.7 Recruta que chega pelo edital pode ser contratado por qualquer um — mantido assim (22/11/2026)
Quem paga 20 moedas no Posto espera 7 dias, mas o mercenário que chega é neutro: outro jogador pode contratá-lo primeiro.
**Decisão:** fica **sem reserva**, de propósito. O recruta do edital é de quem chegar primeiro e pagar.

### ✅ 2.8 Fechar a tela de contrato limpa a "tela aberta" de outro jogador — corrigido (22/11/2026)
`HonorContractMenu.removed` e `WarriorCompanionMenu.removed` chamam `clearInventoryOpenPlayer(player)`, que só limpa se quem fechou for quem abriu.

---

## 3. Comportamento da IA e gameplay

### ✅ 3.1 Teleporte do modo Seguir interrompe o combate — corrigido (22/11/2026)
Antes, a partir de 13 blocos do dono o guerreiro largava o alvo e se teleportava na hora, todo tick.
Agora a regra fica em `WarriorTeleport.tickFollowTeleport`:
- dono a mais de **32 blocos**: teleporta na hora, mesmo lutando;
- dono entre **13 e 32 blocos**: teleporta só **fora de combate** e depois de **2 s seguidos** longe (como o lobo vanilla);
- lutando com o dono a menos de 32 blocos: termina a luta.

O `CompanionTeleportHandler` passou a cuidar só do que o guerreiro não resolve sozinho: outra dimensão, mais de 32 blocos ou chunk descarregado.

### ✅ 3.2 Coleta automática pega qualquer item do chão — corrigido (22/11/2026)
- Botão **✋** na tela do guerreiro liga/desliga a coleta (ligada por padrão; salvo no NBT como `PickupItems`).
- Itens jogados pelo dono (`ItemEntity#getOwner`) são ignorados.
- A varredura roda a cada 5 ticks, espalhada entre guerreiros pelo id (parte do 5.1).
- ✅ Filtro (22/11/2026): o botão ✋ agora tem 3 modos: **desligada** (cinza), **só úteis** (amarela) e **tudo** (verde). Desde a 1.0.20 começa **desligada** nos companheiros novos. "Úteis" = comida, poções de Cura/Regeneração, moedas de ouro, flechas e armadura/arma/escudo melhor que o equipado (`WarriorEquipment.isUseful`). NBT `PickupMode`; o `PickupItems` da 1.0.18 vira "tudo" (ligado) ou "desligada".

### ✅ 3.3 Especialização não pode ser escolhida — corrigido (22/11/2026)
Um Guerreiro base (sem classe) no **nível 5** pode virar Berserker, Guardião ou Duelista pela aba **Talentos**:
- botão **⇄** alterna a árvore mostrada; **Escolher classe** e depois **Confirmar?** (dois cliques, porque é permanente);
- `PacketChooseSpecialization` (só o dono, perto) chama `WarriorProgression.chooseSpecialization`;
- a classe é o tipo de entidade (o Epic Fight registra atributos por tipo), então o guerreiro é recriado no lugar com o mesmo UUID e NBT (nível, XP, nome, inventário, personalidade) e uma geração nova no registro, para nenhuma cópia antiga voltar;
- a vida é mantida em porcentagem da vida máxima nova.

### ✅ 3.4 Guerreiro equipa melhor equipamento sozinho — feito (22/11/2026)
`WarriorEquipment.tickAutoEquip` (a cada 2 s, fora de combate, sem a tela aberta) troca pela melhor peça da mochila:
- armadura: armadura + resistência/2 + Proteção/2;
- arma principal: dano + Afiação; só arma corpo a corpo troca por corpo a corpo (arco, besta e tridente ficam);
- escudo: só se a mão secundária estiver vazia;
- ignora itens com menos de 10% de durabilidade; a peça antiga volta para a mochila.
É o comportamento padrão, sem botão: para ele não usar algo, não deixe na mochila.

### ✅ 3.5 Ordens rápidas sem abrir a GUI — feito (22/11/2026)
- **Agachado + clique de mão vazia** no próprio companheiro: alterna Seguir → Guarda → Parado (mensagem na action bar). Agachar + clique com arma fica livre para o duelo (3.7).
- **Corneta da Vanguarda** (`vanguard_horn`, 4 cobres + 1 moeda): mirando numa criatura, todos os seus companheiros num raio de 32 blocos atacam (mesmo pacíficos; quem está em Parado não sai do lugar); mirando no nada, todos param de lutar. Não aceita jogadores, companheiros nem seus pets como alvo. Recarga de 2 s. Usa a textura da corneta de cabra vanilla.

### ✅ 3.8 Guerreiros ficavam mudos depois de trocar de mundo — corrigido (22/11/2026)
Os cooldowns de fala de esquadrão ficam em mapas estáticos com o `gameTime`. No singleplayer, ao abrir um mundo com `gameTime` menor que o anterior, a diferença ficava negativa e a fala ficava bloqueada até o tempo alcançar o valor antigo (podia levar dias de jogo). Agora diferença negativa conta como cooldown vencido.

### ✅ 3.9 Matar animais e aldeões dava XP aos companheiros — corrigido (22/11/2026)
Qualquer abate do jogador dava XP (mínimo 10) aos companheiros num raio de 24 blocos, inclusive galinhas e aldeões: um farm de nível trivial. Agora só abates de monstros contam. Caçadas feitas pelo próprio guerreiro continuam dando XP.

### ✅ 3.6 Regeneração natural só considera dano de mob — corrigido (22/11/2026)
`hurt()` guarda `lastDamageTick` para qualquer dano que entrou de fato; a regeneração espera 5 s a partir dele.

### 🟠 3.7 Duelo nunca é ativado (a desenvolver)
Toda a lógica de duelo existe (`DUEL_MODE`, recrutar quem vence o duelo em `hurt()`, exceções em fogo amigo e IA), mas nada chama `setDuelMode(true)`. É uma mecânica morta. Decisão (22/11/2026): **manter e desenvolver mais para frente**. O livro do Patchouli não menciona o duelo até lá.

**Ideias para desenvolver:**
- **Como desafiar:** agachar + clicar com uma arma num mercenário não contratado abre uma tela de desafio (aceitar/recusar, regras). Alternativa: um item "Luva de Desafio" jogado aos pés dele.
- **Quem aceita:** mercenários orgulhosos (personalidade) aceitam sempre; outros só se o jogador tiver nível/equipamento parecido. Recusa com fala própria.
- **Arena:** círculo de partículas de ~8 blocos ao redor do ponto do desafio. Sair do círculo = derrota. Ninguém de fora pode interferir (outros mobs e jogadores não acertam os duelistas; companheiros do jogador ficam parados).
- **Regras:** até 1 HP (ninguém morre). Escolher no desafio: só corpo a corpo, sem poções, sem escudo.
- **Recompensa:** vencer recruta de graça, ou com desconto, e o guerreiro começa com mais lealdade/XP. Perder custa moedas (aposta) e ele não aceita novo desafio por um dia de jogo.
- **Duelo amistoso com o próprio companheiro:** treino que dá XP aos dois, sem risco. Bom para subir de nível fora de combate.
- **Epic Fight:** usar a postura de duelo (guarda alta, esquivas) e um som/animação de saudação no início.
- **Torneio no Posto da Vanguarda:** chaveamento de 3 mercenários seguidos; o campeão vira um recruta raro (tier alto ou especialização garantida).
- **Testes:** GameTest para o duelo terminar em 1 HP, para o recrutamento do vencedor e para interferência externa ser bloqueada.

---

## 4. Persistência

### ✅ 4.1 `DATA_NAME = "grimal_companions"` — corrigido (22/11/2026): agora `epicvanguard_companions`, com migração automática
Trocar para `epicvanguard_companions` **com migração**: se o arquivo novo não existir, ler o antigo e marcar `setDirty()`.

### ✅ 4.2 Conjuntos `Retired` e `Dismissed` só crescem — corrigido (22/11/2026)
Cada UUID agora guarda o `gameTime` em que entrou. A cada minuto `CompanionSavedData.pruneForgotten` apaga os que passaram de `registryMemoryDays` (config, padrão **100 dias de jogo**). Entradas de saves antigos (sem data) começam a contar na primeira manutenção.
Não dá para limpar "quando a cópia antiga for descartada": cada resgate de chunk descarregado pode deixar uma cópia para trás, então o UUID precisa ficar marcado por um tempo. Uma cópia achada depois do prazo já não é reconhecida.

### ✅ 4.3 Snapshot com NBT inteiro de cada guerreiro no SavedData — corrigido (22/11/2026)
`registerOrUpdate` e os `update*` só chamam `setDirty()` (e só copiam o NBT) quando algo mudou de fato. Um guerreiro parado (modo Parado no posto, fogueira) não regrava o arquivo a cada 10 s. Quem anda continua mudando o snapshot (posição), o que é esperado.

### ✅ 4.4 `VillageWarriorSavedData` não separa dimensões — corrigido (22/11/2026)
A chave agora é dimensão + chunk (`SpawnedByDimension` no NBT). A lista antiga (`SpawnedVillages`) é lida como overworld e regravada no formato novo.

---

## 5. Desempenho

### ✅ 5.1 Varreduras de entidades por tick — corrigido (22/11/2026)
- Coleta de itens: a cada 5 ticks, espalhada pelo id.
- Projéteis e creepers (`WarriorThreatSensor`): cada guerreiro guarda a última lista num cache. Projéteis são varridos a cada 2 ticks num raio de 18 (12 pedidos + o que a flecha anda em 2 ticks) e creepers a cada 5 ticks num raio de 9. Os goals pedem várias vezes por tick com raios diferentes; agora todos usam a mesma lista, filtrando posição, movimento e pavio na hora.

### ✅ 5.2 `syncEquipmentWithInventory()` todo tick — corrigido (22/11/2026)
Continua rodando todo tick (é barato), mas só chama `setItemSlot` quando o slot aponta para outro `ItemStack`. O slot guarda a mesma instância do inventário, então durabilidade e encantamentos já ficam iguais sem copiar nada.

### ✅ 5.3 Resgate de companheiros a cada segundo por jogador — corrigido (22/11/2026)
`findLoadedCompanion(server, info, nívelDoJogador)` procura primeiro na dimensão onde o registro viu o companheiro por último e na do jogador; só depois nas outras. Quase sempre acha na primeira. Um cache UUID → entidade ficou de fora: teria de ser mantido em join/leave/troca de dimensão e o ganho é pequeno.

---

## 6. Organização do código

- ✅ **Dividir `WarriorCompanionEntity`** — feito (22/11/2026): de ~2.030 para ~1.680 linhas. Partes novas em `entity/util` (ver [ARQUITETURA.md](ARQUITETURA.md)):
  - `WarriorInteraction` (clique do jogador: resgate, contrato, name tag, abrir tela; coleta de itens)
  - `WarriorTeleport` (teleporte seguro e a regra de seguir o dono)
  - `WarriorThreatSensor` (creeper e projéteis)
  - `WarriorWarcry` (gritos de guerra)
  - `WarriorProgression` (escolha de classe)
  - 🟢 Falta: `WarriorPersistence` (save/load/snapshot) e a parte de XP/nível.
- ✅ **Config `epicvanguard-common.toml`** — feito (22/11/2026): `config/EpicVanguardConfig` (ForgeConfigSpec COMMON) com custo de contratação, XP, raio do XP compartilhado, recarga do grito, duração do edital, distâncias/tempo do Seguir e memória do registro. Ver [ARQUITETURA.md](ARQUITETURA.md#config).
  - ✅ Porcentagens das classes na config (crítico e fúria do Berserker, provocação do Guardião; a esquiva já estava) e a recarga do grito nos textos da árvore (24/11/2026). A curva de XP ficou de fora: o `xpMultiplier` já cobre, e a tela do cliente calcula o XP necessário com a própria config.
  - ✅ Nível 20 reduz a recarga do grito em 20% (50 s → 40 s) (24/11/2026).
- ✅ Indentação de `isSpellOrMagicDamage` arrumada (22/11/2026).
- ✅ **Código morto** — removido (22/11/2026):
  - ~~`NEXT_TAVERN_SPAWN` / `MIN_TAVERN_INTERVAL` / `checkTavernRespawn` / `findImmersiveSpawnPos`~~ em `VillageWarriorSpawner` (e os imports que só eles usavam)
  - ~~`findOrLoadCompanion`~~ (os comandos chamam `findLoadedCompanion`)
  - ~~`teleportFollowingCompanions` (alias)~~ (removido)
  - ~~`consumeHealingItem(ItemStack, int)`~~ (removido)
  - ~~`VillageCompanionSavedData`~~ (removido)
- ✅ **Pacote** — trocado (22/11/2026): `com.example.epicvanguard` → `io.github.raffasanttos.epicvanguard` (main e gametest; `mod_group_id` no `gradle.properties`). O mod id `epicvanguard` não mudou, então mundos e configs continuam valendo.
- ✅ **Registro de pacotes:** `Messages` só com imports (22/11/2026).

---

## 7. Build e projeto

- ✅ **Jar do Epic Fight versionado em `libs/`** — corrigido (22/11/2026): o Gradle baixa pelo CurseMaven (`curse.maven:epic-fight-mod-405076:8049910`, Epic Fight 20.14.17). O jar saiu do repositório.
- ✅ `build_jar.bat` (22/11/2026): lista o jar gerado de verdade (`EpicVanguard <versão> [1.20.1].jar`) e o `PATH` do Java do launcher agora funciona (antes a variável era expandida antes de ser definida).
- ✅ `scratch_test_nbt.java` movido para `tools/dev/InspectStructureNbt.java.txt` (22/11/2026) (ferramenta de inspeção de NBT de estruturas; não compila com o mod).
- ✅ `forge.logging.console.level` em `info` (22/11/2026).
- ✅ CI no GitHub Actions (`.github/workflows/build.yml`): `build` + `runGameTestServer` a cada push, com o jar como artefato (24/11/2026).

---

## 8. Testes e documentação

- ✅ **GameTests:** 22 testes em `src/gametest` (combate, persistência, segurança multiplayer, XP, fogo amigo, traduções, livro). Como rodar em [TESTES.md](TESTES.md).
- 🟢 **Mais testes:** `rollRandomTier`, IA seguindo/guardando, teleporte, duelo (quando 3.7 for feito).
- ✅ **Guia dentro do jogo:** livro do Patchouli (opcional) em inglês e português, gerado por `tools/patchouli/build_book.js`.
- ✅ **README.md** (22/11/2026): o que o mod faz, recrutamento, posturas, classes, comandos, config, dependências e como compilar (inglês e português).
- ✅ **Traduções em inglês revisadas** (22/11/2026): "Vanguard Post" → "Vanguard Point" nos títulos, "Elite (Iron/Iron)", frases das falas mais naturais, maiúsculas padronizadas nos talentos. O aviso do edital mostra os dias da config.

---

## 9. Revisão depois da 1.0.19 (22/11/2026)

Achados de uma nova leitura do código. A 1.0.19 já foi publicada com eles; os 🟠 valem entrar na 1.0.20.

### ✅ 9.1 Corneta pode mandar atacar os animais de outro jogador — corrigido (24/11/2026): recusa pets de qualquer dono, cavalos domados, aldeões, golens de ferro/neve e suportes de armadura
`VanguardHornItem.isValidOrderTarget` só protege os pets **do próprio dono**. Em multiplayer dá para mandar os companheiros matarem o lobo, gato ou cavalo domado de outro jogador.
**Correção:** recusar qualquer `OwnableEntity` com dono e `AbstractHorse` domado (e talvez aldeões, para evitar clique errado).

### ✅ 9.2 Corneta "erra" quando um companheiro está na frente — corrigido (24/11/2026): o raio atravessa alvos inválidos
O raio pega a primeira criatura na mira. Se for um dos seus companheiros (comum: eles andam na sua frente), o alvo é inválido e a corneta vira "reagrupar" em vez de "atacar".
**Correção:** o filtro do raio deve ignorar seus companheiros e os alvos inválidos, e seguir até a próxima criatura.

### ✅ 9.3 Livro diz que a poção deixa o frasco vazio — corrigido (o livro já diz que o frasco é descartado)
A página "Comida e Cura" diz "deixando o frasco vazio", mas `WarriorHealingHelper` descarta o frasco (e a tigela dos ensopados). Corrigir o texto do livro.

### ✅ 9.4 Recrutar com moedas só registra o companheiro depois de até 10 s — corrigido (24/11/2026)
`PacketRecruitWarrior` não chama `updateSavedData()`. O `/companions` não mostra o recruta nesse intervalo, e se o servidor cair antes ele não entra no registro (o resgate do modo Seguir não o acha). Chamar `updateSavedData()` ao recrutar (moedas e prisioneiro).

### ✅ 9.5 Moedas na mão secundária não contam — corrigido (24/11/2026)
`countGoldCoins`/`consumeGoldCoins` só olham `getInventory().items`. Moedas no offhand ficam de fora do contrato.

### ✅ 9.6 Pacote de abrir GUI ainda manda o `boolean` inútil (2.3) — corrigido antes do upload da 1.0.20 (protocolo continua "6")
O protocolo foi para "6" na 1.0.20 (merge com o trabalho da nuvem). Se o campo sair antes do upload da 1.0.20, não precisa de outro aumento depois.

### 🟢 9.7 Auto-equipar: casos a conferir no jogo
- Com uma arma de duas mãos do Epic Fight (montante, lança), ele ainda coloca escudo na mão secundária se ela estiver vazia. Conferir se atrapalha a animação; se sim, não equipar escudo com arma de duas mãos.
- Armaduras de mod com habilidade especial e pouca armadura são trocadas por outras com mais armadura (comportamento esperado, mas vale citar no livro).
- ~~No modo "só úteis", ele pega várias cópias da mesma peça melhor~~: o modo "só úteis" saiu (24/11/2026); a coleta é só ligada (tudo) ou desligada (nada).
- ✅ O Arqueiro não recebe mais escudo na mão secundária (24/11/2026).

### 🟢 9.8 Mapas de cooldown de fala nunca são limpos
`WarriorSpeechSystem` guarda cooldowns em mapas estáticos (por dono, por gatilho e por chunk) que só crescem durante a sessão do servidor. Pequeno, mas dá para limpar entradas velhas de vez em quando.

### ✅ 9.9 Contagem da corneta — corrigido (24/11/2026): quem está de Guarda agora vai até o alvo da corneta (ignora o raio do posto enquanto a ordem vale) e volta ao posto depois
A mensagem "N companheiros avançam" conta quem está em Guarda, mas eles largam o alvo se ele estiver a mais de 8 blocos do ponto de guarda. Contar só quem vai de fato, ou deixar Guarda fora da ordem de ataque.

### ✅ 9.10 GameTests para o que entrou na 1.0.19 — feito (24/11/2026): `Release120Tests` (auto-equipar, coleta, corneta, regra da Guarda, drops da morte, nível 20)
Auto-equipar (troca e devolve a peça antiga), filtro "só úteis", `pruneForgotten`, migração do `VillageWarriorSavedData` por dimensão, `setDirty` só com mudança, e a corneta (alvo válido / inválido).

---

## Ordem sugerida

1. 1.1 (rename), 1.2 (dano/durabilidade), 2.1 (admin em criativo), 2.2 (versão de rede)
2. 3.1 (teleporte em combate), 2.7 (reserva do recruta do edital)
3. 4.1 (migração do `DATA_NAME`) e config com `ForgeConfigSpec`
4. Divisão da `WarriorCompanionEntity`, GameTests e README
5. Novas mecânicas: escolha de especialização (3.3), auto-equipar (3.4), ordens rápidas (3.5)

---

## 10. Revisão de 24/11/2026

### ✅ 10.1 Guarda hesitava antes de atacar — corrigido
A escolha de alvo (`AggressiveTargetGoal`, alcance de 32 blocos) achava monstros longe do posto, e o combate largava qualquer alvo a mais de 8 blocos do posto. O guerreiro mirava, andava, largava, voltava ao posto e mirava de novo. Agora as duas partes usam `canEngageFromGuard` (10 blocos corpo a corpo, 20 com arco, +2 de folga para quem já luta, +6 contra quem atacou o guarda; ordem da Corneta sem limite). Também: a órbita (strafe) volta a perseguir quando o alvo se afasta e o bote recalcula o caminho a cada 4 ticks.

### ✅ 10.2 Armadura ia para a mão principal — corrigido
`EpicFightCompat.isEpicFightWeapon` aceitava qualquer item com capacidade do Epic Fight, e armadura tem categoria `NOT_WEAPON`. Com a mão vazia, o auto-equipar colocava o capacete velho na mão. Agora `NOT_WEAPON`/`FIST` não contam e `ArmorItem` nunca é arma.

### ✅ 10.3 Tela Minha Companhia e formações — feito (24/11/2026)
- Tela `MyCompanyScreen` (tecla J): lista do `CompanionSavedData` com dados ao vivo para quem está carregado. Ações em `CompanyManager` (servidor confere o dono; anti-spam de 5 ticks): chamar, chamar todos, postura (só carregado), formação. Pacotes `PacketCompanyAction` (C→S) e `PacketCompanyList` (S→C); protocolo "7".
- Formações (`WarriorFormation`): Livre, Coluna, Linha, Círculo, por dono, salvas no `CompanionSavedData`. O `FollowOwnerGoal` anda até o lugar (ordem por UUID entre os companheiros em Seguir a até 24 blocos) e o passeio à toa fica desligado em formação.
- 🟢 Ideias para depois: ícone/cabeça do companheiro na linha, filtro por dimensão, botão "ir até ele" (como `/companions goto`) para operadores.

