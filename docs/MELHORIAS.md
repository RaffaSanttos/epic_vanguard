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

1. `/vanguard renomear` grava o nome novo só em `CompanionInfo.name` ([VanguardCommand.java:585](../src/main/java/com/example/epicvanguard/command/VanguardCommand.java)).
2. Quando o chunk carrega, `readAdditionalSaveData` chama `setWarriorName(nome antigo do NBT)`, que também chama `CompanionSavedData.updateName(...)` ([WarriorCompanionEntity.java:638](../src/main/java/com/example/epicvanguard/entity/WarriorCompanionEntity.java)).
3. Quando `CompanionTeleportHandler.onEntityJoinLevel` compara os nomes, o registro já tem o nome antigo e o rename se perde.

Também por isso, uma **cópia antiga** (geração menor) sobrescreve o nome no registro antes de ser descartada.

**Correção:** separar o "aplicar nome na entidade" do "gravar no registro". Durante `readAdditionalSaveData`, só aplicar na entidade, sem chamar `updateName`. Gravar no registro só em ações do jogador (name tag, pacote de rename, comando).

### ✅ 1.2 Ataque do guerreiro ignora encantamentos e a arma nunca gasta — corrigido (21/11/2026)
`doHurtTarget` ([WarriorCompanionEntity.java:1436](../src/main/java/com/example/epicvanguard/entity/WarriorCompanionEntity.java)) só chama `target.hurt(...)`. O que falta:
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

### 🟢 1.5 Drops da morte não passam pelo `LivingDropsEvent`
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

### 🟢 2.3 Pacote de abrir GUI manda um `boolean recruited` inútil
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

### 🟢 2.7 Recruta que chega pelo edital pode ser contratado por qualquer um
Quem paga 20 moedas no Posto espera 7 dias, mas o mercenário que chega é neutro: outro jogador pode contratá-lo primeiro.
**Sugestão:** reservar o recruta para quem pagou por alguns minutos (ou para sempre), mostrando o nome dele na tela de contrato.

### 🟢 2.8 Fechar a tela de contrato limpa a "tela aberta" de outro jogador
`HonorContractMenu.removed` sempre chama `setInventoryOpenPlayer(null)`, mesmo quando outro jogador também está com a tela aberta. O guerreiro volta a andar enquanto o segundo jogador ainda olha. Só zerar se for o mesmo jogador.

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
- Falta (🟢): filtro por tipo (só comida, poções e equipamento melhor).

### ✅ 3.3 Especialização não pode ser escolhida — corrigido (22/11/2026)
Um Guerreiro base (sem classe) no **nível 5** pode virar Berserker, Guardião ou Duelista pela aba **Talentos**:
- botão **⇄** alterna a árvore mostrada; **Escolher classe** e depois **Confirmar?** (dois cliques, porque é permanente);
- `PacketChooseSpecialization` (só o dono, perto) chama `WarriorProgression.chooseSpecialization`;
- a classe é o tipo de entidade (o Epic Fight registra atributos por tipo), então o guerreiro é recriado no lugar com o mesmo UUID e NBT (nível, XP, nome, inventário, personalidade) e uma geração nova no registro, para nenhuma cópia antiga voltar;
- a vida é mantida em porcentagem da vida máxima nova.

### 🟢 3.4 Guerreiro equipa melhor equipamento sozinho
Hoje ele só usa o que está nos slots 0–5. Ele poderia trocar sozinho por armadura ou arma melhor que estiver na mochila, comparando armadura/dano. Seria um botão "auto-equipar" ou um comportamento padrão.

### 🟢 3.5 Ordens rápidas sem abrir a GUI
Sugestões:
- Agachar + clicar: alternar Seguir/Parado.
- Item "Corneta de comando": ordenar a todos os companheiros próximos que ataquem o alvo apontado.

### ✅ 3.8 Guerreiros ficavam mudos depois de trocar de mundo — corrigido (22/11/2026)
Os cooldowns de fala de esquadrão ficam em mapas estáticos com o `gameTime`. No singleplayer, ao abrir um mundo com `gameTime` menor que o anterior, a diferença ficava negativa e a fala ficava bloqueada até o tempo alcançar o valor antigo (podia levar dias de jogo). Agora diferença negativa conta como cooldown vencido.

### ✅ 3.9 Matar animais e aldeões dava XP aos companheiros — corrigido (22/11/2026)
Qualquer abate do jogador dava XP (mínimo 10) aos companheiros num raio de 24 blocos, inclusive galinhas e aldeões: um farm de nível trivial. Agora só abates de monstros contam. Caçadas feitas pelo próprio guerreiro continuam dando XP.

### 🟢 3.6 Regeneração natural só considera dano de mob
A regeneração fora de combate usa `getLastHurtByMobTimestamp()`. Dano de queda, fogo e veneno não reiniciam a contagem.
**Sugestão:** guardar o tick do último dano de qualquer fonte em `hurt()`.

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

### 🟢 4.2 Conjuntos `Retired` e `Dismissed` só crescem
Num mundo antigo, o save cresce sem limite. Sugestões:
- Guardar junto o `gameTime` em que o UUID entrou e limpar depois de, por exemplo, 30 dias de jogo.
- Ou limpar quando a cópia antiga for de fato encontrada e descartada.

### 🟢 4.3 Snapshot com NBT inteiro de cada guerreiro no SavedData
Cada companheiro guarda o NBT completo (inventário incluído) no arquivo do overworld, e isso é salvo a cada 10 s quando há mudança. Com muitos companheiros, o arquivo fica pesado.
**Sugestão:** só marcar `setDirty()` quando o NBT mudar de fato. Dá para comparar um hash do inventário e do estado relevante.

### 🟢 4.4 `VillageWarriorSavedData` não separa dimensões
A chave é só `ChunkPos.toLong()`, mas `VillageWarriorSpawner.tick` roda em todas as dimensões.
**Sugestão:** incluir a dimensão na chave, ou rodar o spawner só no overworld (o comentário já diz isso).

---

## 5. Desempenho

### 🟢 5.1 Varreduras de entidades por tick
~~`pickUpGroundItems` faz `getEntitiesOfClass(ItemEntity)` todo tick~~ (agora a cada 5 ticks). As checagens de projétil e creeper ainda varrem a área com frequência.
**Sugestão:** rodar a cada 5–10 ticks, com o `tickCount` deslocado pelo `getId()` para espalhar a carga entre guerreiros.

### 🟢 5.2 `syncEquipmentWithInventory()` todo tick
Chama `setItemSlot` 6 vezes por tick. Daria para marcar o inventário como "sujo" em `setChanged()` e sincronizar só quando mudar.

### 🟢 5.3 Resgate de companheiros a cada segundo por jogador
`rescueFollowingCompanions` roda a cada 20 ticks e varre todos os níveis para cada companheiro. Com poucos companheiros não pesa; com muitos jogadores e companheiros, convém um cache UUID → entidade atualizado no join/leave.

---

## 6. Organização do código

- ✅ **Dividir `WarriorCompanionEntity`** — feito (22/11/2026): de ~2.030 para ~1.680 linhas. Partes novas em `entity/util` (ver [ARQUITETURA.md](ARQUITETURA.md)):
  - `WarriorInteraction` (clique do jogador: resgate, contrato, name tag, abrir tela; coleta de itens)
  - `WarriorTeleport` (teleporte seguro e a regra de seguir o dono)
  - `WarriorThreatSensor` (creeper e projéteis)
  - `WarriorWarcry` (gritos de guerra)
  - `WarriorProgression` (escolha de classe)
  - 🟢 Falta: `WarriorPersistence` (save/load/snapshot) e a parte de XP/nível.
- 🟢 **Constantes de balanceamento num lugar só.** Custos de recrutamento, curva de XP, cooldowns, raios e porcentagens estão espalhados como números mágicos. Melhor ainda: um `ForgeConfigSpec` (`epicvanguard-common.toml`), para donos de servidor ajustarem sem recompilar.
- 🟢 ~~Import duplicado de `ShieldItem`~~ (removido). Falta a indentação quebrada em `isSpellOrMagicDamage`.
- 🟢 **Código morto:**
  - `NEXT_TAVERN_SPAWN` / `MIN_TAVERN_INTERVAL` / `checkTavernRespawn` comentado em `VillageWarriorSpawner`
  - `findOrLoadCompanion`, que hoje é igual a `findLoadedCompanion`
  - ~~`teleportFollowingCompanions` (alias)~~ (removido)
  - ~~`consumeHealingItem(ItemStack, int)`~~ (removido)
  - ~~`VillageCompanionSavedData`~~ (removido)
- 🟢 **Pacote `com.example.epicvanguard`** → trocar por um domínio seu antes de publicar (pendente da auditoria).
- 🟢 **Registro de pacotes:** `Messages` mistura imports e nomes totalmente qualificados. Padronizar com imports.

---

## 7. Build e projeto

- ✅ **Jar do Epic Fight versionado em `libs/`** — corrigido (22/11/2026): o Gradle baixa pelo CurseMaven (`curse.maven:epic-fight-mod-405076:8049910`, Epic Fight 20.14.17). O jar saiu do repositório.
- 🟢 `build_jar.bat` anuncia `build\libs\epicvanguard-1.0.0.jar`, mas o jar real é `EpicVanguard <versão> [1.20.1].jar`.
- 🟢 Apagar `scratch_test_nbt.java` da raiz, ou mover para `tools/`.
- 🟢 Mudar `forge.logging.console.level` de `debug` para `info` nas runs, para deixar o log legível.
- 🟢 CI no GitHub Actions rodando `./gradlew build` e `./gradlew runGameTestServer` a cada push. Agora dá: o Epic Fight vem do CurseMaven.

---

## 8. Testes e documentação

- ✅ **GameTests:** 22 testes em `src/gametest` (combate, persistência, segurança multiplayer, XP, fogo amigo, traduções, livro). Como rodar em [TESTES.md](TESTES.md).
- 🟢 **Mais testes:** `rollRandomTier`, IA seguindo/guardando, teleporte, duelo (quando 3.7 for feito).
- ✅ **Guia dentro do jogo:** livro do Patchouli (opcional) em inglês e português, gerado por `tools/patchouli/build_book.js`.
- 🟢 **README.md:** o que o mod faz, como recrutar (moedas, duelo, prisioneiro), modos de combate, classes, comandos `/vanguard` e `/fogoamigo`, dependências, como compilar.
- 🟢 **Revisar as traduções em inglês dentro do jogo** (pendente da auditoria).

---

## Ordem sugerida

1. 1.1 (rename), 1.2 (dano/durabilidade), 2.1 (admin em criativo), 2.2 (versão de rede)
2. 3.1 (teleporte em combate), 2.7 (reserva do recruta do edital)
3. 4.1 (migração do `DATA_NAME`) e config com `ForgeConfigSpec`
4. Divisão da `WarriorCompanionEntity`, GameTests e README
5. Novas mecânicas: escolha de especialização (3.3), auto-equipar (3.4), ordens rápidas (3.5)
