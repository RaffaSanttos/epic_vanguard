# Changelog — Epic Vanguard

## 1.0.19 (Minecraft 1.20.1, Forge 47.x) — not released yet

### English

**New**
- **Config file** `config/epicvanguard-common.toml`: recruit cost, XP gain, war cry cooldown, Vanguard Point request time, follow teleport distances and how long the save remembers dead/dismissed companions. Defaults are the same as before.
- Guide book: new "Server Settings" page.

**Changes**
- Less work per tick: companions share one scan for incoming arrows and creepers instead of several, only update their equipment slots when the gear actually changes, and the follow rescue finds loaded companions faster.
- The companion save file is only rewritten when something changed, and old dead/dismissed entries are cleaned up after 100 in-game days (configurable).
- Warrior houses and pillager outposts are now tracked per dimension (old worlds are converted automatically).
- English texts reviewed: "Vanguard Point" everywhere, smoother dialogue lines.

**Notes**
- The Java package changed (`io.github.raffasanttos.epicvanguard`). The mod id is still `epicvanguard`, so worlds and items are not affected.

### Português

**Novidades**
- **Arquivo de config** `config/epicvanguard-common.toml`: custo de contratação, ganho de XP, recarga do grito de guerra, tempo do edital do Posto, distâncias do teleporte de seguir e por quanto tempo o save lembra companheiros mortos/dispensados. Os padrões são os mesmos de antes.
- Livro guia: página nova "Configurações do Servidor".

**Mudanças**
- Menos trabalho por tick: os companheiros fazem uma varredura só para flechas e creepers em vez de várias, só atualizam os slots de equipamento quando o equipamento muda de fato, e o resgate do modo Seguir acha companheiros carregados mais rápido.
- O arquivo de companheiros só é regravado quando algo mudou, e registros antigos de mortos/dispensados são limpos depois de 100 dias de jogo (configurável).
- Casas de guerreiro e postos de saqueadores agora são marcados por dimensão (mundos antigos são convertidos sozinhos).
- Textos em inglês revisados: "Vanguard Point" em todo lugar e falas mais naturais.

**Observações**
- O pacote Java mudou (`io.github.raffasanttos.epicvanguard`). O mod id continua `epicvanguard`, então mundos e itens não são afetados.

---

## 1.0.18 (Minecraft 1.20.1, Forge 47.x)

### English

**New**
- **Choose a class:** a plain Warrior that reaches **level 5** can become a Berserker, Guardian or Duelist. Open the Talents tab, press ⇄ to preview each class tree, then "Choose class" and "Confirm?". It's permanent; level, XP, name and gear are kept.
- **Pick-up button (✋)** in the companion screen: turn automatic item pick-up on or off. Companions also never pick up items you dropped yourself anymore.

**Changes**
- Follow stance no longer drags companions out of a fight. They teleport to you only after being 13+ blocks behind for 2 seconds while not fighting, or right away if you're more than 32 blocks away.
- Guide book (Patchouli) updated: tactic buttons, the new follow rule, item pick-up and choosing a class.

**Notes**
- Client and server must both be on 1.0.18 (network protocol changed).

### Português

**Novidades**
- **Escolha de classe:** um Guerreiro comum que chega ao **nível 5** pode virar Berserker, Guardião ou Duelista. Abra a aba Talentos, aperte ⇄ para ver a árvore de cada classe, depois "Escolher classe" e "Confirmar?". É permanente; nível, XP, nome e equipamento continuam.
- **Botão de coleta (✋)** na tela do companheiro: liga ou desliga pegar itens do chão. E os companheiros não pegam mais os itens que você mesmo jogou.

**Mudanças**
- A postura Seguir não tira mais o companheiro do meio da luta. Ele se teleporta até você só depois de ficar 13+ blocos para trás por 2 segundos fora de combate, ou na hora se você estiver a mais de 32 blocos.
- Livro guia (Patchouli) atualizado: botões de tática, a nova regra de seguir, coleta de itens e escolha de classe.

**Observações**
- Cliente e servidor precisam estar os dois na 1.0.18 (o protocolo de rede mudou).

---

## 1.0.17 (Minecraft 1.20.1, Forge 47.x)

### English

**Changes**
- Newly recruited companions (coins, prisoner rescue) now start in **Follow** stance and **peaceful**: they don't hunt monsters or animals until you turn it on in their screen (Sword / Meat buttons). They still defend you and themselves.

**Fixes**
- Companions from older worlds could duplicate their armor and weapons when killed by a player. Fixed for old and new saves.
- Companion armor now loses durability when hit, like the player's.
- Vanguard Point: hoppers can no longer pull coins deposited by another player.
- Vanguard Point: only the player who deposited the coins, the player who paid for the contract, or an operator can break the block. Nobody can steal coins or cancel someone else's contract by breaking it.
- Vanguard Point: creative players who aren't operators can no longer take coins deposited by others.
- Companions no longer go silent after switching worlds in singleplayer.
- Killing animals or villagers no longer gives XP to nearby companions (easy level farm). Monster kills still do.
- The companion registry save file was renamed to `epicvanguard_companions.dat`. Existing worlds are migrated automatically; the old `grimal_companions.dat` is kept as a backup.
- Guide book (Patchouli) updated with the new defaults.

### Português

**Mudanças**
- Companheiros recém-recrutados (moedas, resgate de prisioneiro) agora começam na postura **Seguir** e **pacíficos**: não caçam monstros nem animais até você ligar isso na tela deles (botões Espada / Carne). Continuam defendendo você e a si mesmos.

**Correções**
- Companheiros de mundos antigos podiam duplicar armadura e armas ao morrer para um jogador. Corrigido para saves antigos e novos.
- A armadura dos companheiros agora gasta ao levar dano, como a do jogador.
- Posto da Vanguarda: funis não puxam mais as moedas depositadas por outro jogador.
- Posto da Vanguarda: só quem depositou as moedas, quem pagou o edital ou um operador consegue quebrar o bloco. Ninguém rouba moedas nem cancela o edital de outro quebrando o bloco.
- Posto da Vanguarda: jogadores em criativo que não são operadores não pegam mais as moedas dos outros.
- Companheiros não ficam mais mudos depois de trocar de mundo no singleplayer.
- Matar animais ou aldeões não dá mais XP aos companheiros por perto (farm fácil de nível). Abates de monstros continuam dando.
- O arquivo do registro de companheiros passou a se chamar `epicvanguard_companions.dat`. Mundos existentes são migrados sozinhos; o antigo `grimal_companions.dat` fica como backup.
- Livro guia (Patchouli) atualizado com os novos padrões.

---

## 1.0.16

- In-game guide book (requires Patchouli, optional), in English and Portuguese. Given once on first login and craftable with a book + gold coin.
- Renaming a companion by command while it's unloaded now sticks.
- Companion attacks apply weapon enchantments (Sharpness, Smite, Bane, Knockback, Fire Aspect) and wear the weapon down.
- Creative players are no longer treated as admins by the commands; only operators manage other players' companions.
- Client and server must run the same mod version (network protocol check).
- Recruiting with coins requires the companion to be alive, nearby and not dueling.

## 1.0.15

- The whole mod follows the game language: English or Portuguese (Brazil). Players had been seeing Portuguese text, which was mistaken for Spanish.
- Epic Fight is truly optional (no crash without it).
- Death no longer duplicates equipment; dismissing an unloaded companion works; cross-dimension teleport fixes.
- Vanguard Point coins belong to the player who deposited them; `/friendlyfire <mode>` requires operator.
