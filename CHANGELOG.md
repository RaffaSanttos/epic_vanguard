# Changelog — Epic Vanguard

## 1.0.20 (Minecraft 1.20.1, Forge 47.x)

### English

**New**
- **Two new classes:** **Archer** (bow or crossbow, keeps a sword for when arrows run out, warcry "Arrow Rain") and **Standard-bearer** (banner in the off hand, Resistance aura for you and nearby allies, warcry "Rally"). Both spawn in villages and through the Vanguard Point.
- **Every companion has a class from the start** (Berserker, Guardian, Duelist, Archer or Standard-bearer). There is no plain Warrior and no class choice at level 5 anymore; old plain Warriors become a random class when loaded, keeping level, name and items.
- **Talents:** new Talents tab with 10 talents. Companions earn 1 talent point every 2 levels.
- **Ranged combat:** any companion with a bow or crossbow and arrows in the backpack fights at range. Their arrows never hit you or your other companions.
- **Equipment wear:** low durability warning in chat, automatic swap when an item breaks, and repair with materials from the backpack while resting at a campfire or at the Vanguard Point.
- **Mounts:** in Follow stance, companions ride your tamed, saddled horses when you ride, and teleport together with their horse.
- **Death notice:** you get a chat message when a companion dies, with the cause and where their items dropped.
- **My Company screen** (key **J**, changeable in Controls): every companion you have, even far away or in other dimensions, with class, level, health, stance and location. Call one or everyone, change stances and pick the formation from there.
- **Formations:** sneak + right-click with the War Horn cycles **Free → Column → Line → Circle**. Companions in Follow keep their place around you and turn with you; in a fight they break formation and come back after.
- **Jade / WTHIT** (optional): tooltip with level, class, owner and stance.
- New config sections: regeneration, shield and dodge chances, ranged combat, equipment wear, aura radius, mounts, spawns and contract cost.

**Changes**
- Companion screens: Backpack, Skills and Talents tabs in the same place, and all three screens have the same size.
- Fixed: right after recruiting, the Monsters button showed ON and could turn hunting on by accident.
- Item pick-up is simply **on or off** again: on, they pick up everything they walk over, including what you drop for them; off, they pick up nothing. The "useful only" mode was removed (companions that had it now pick up everything).
- Auto-equip: the Standard-bearer puts a banner in the off hand instead of a shield, and the Archer leaves the off hand empty.
- War Horn: companions in **Guard** now chase the ordered target even far from their post, then go back to it.
- War Horn: it no longer accepts other players' tamed animals, villagers, golems or armor stands, and it aims past your own companions instead of turning the order into "regroup".
- Coins in the off hand now count when hiring.
- Fixed: companions in **Guard** hesitated before attacking (aimed, took a few steps, stopped, went back). They now only pick enemies up to about 10 blocks from the post (20 with a bow) and chase them smoothly.
- Fixed: auto-equip could put a piece of **armor in the main hand** when it was empty (Epic Fight marks armor with a "not a weapon" category, which counted as a weapon). Armor no longer fits the weapon slots.
- Level 20 now really shortens the warcry cooldown by 20% (50 s → 40 s), as the skill tree promised.
- Items of a dead companion now go through Forge's drop event, so grave mods (Gravestone, Corail Tombstone) keep them.
- New config options: Berserker critical chance/multiplier, Blood Rage threshold/bonus and Guardian taunt chance. The skill tree shows the configured warcry cooldown.
- Guide book: new pages for the Archer, the Standard-bearer, mounts, wear and repair, bows and crossbows, the death notice, resting at the Vanguard Point and the Guard range.
- A newly hired or rescued companion is saved to the companion list right away (before, it took up to 10 s).
- Natural regeneration is now **off by default**: companions heal with food, potions and campfires (`companions.naturalRegen` turns it back on).
- Item pick-up now starts **off** for new companions; turn it on with the ✋ button.
- Quick order (sneak + right-click with an empty hand) now cycles **Follow → Guard → Stay**; Guard protects the spot where the companion is.
- Fixed: crash when a companion mounted a horse with Epic Fight installed.
- Fixed: health could go above the maximum.
- Fixed: long class names spilled out of their box in the companion screen.

**Notes**
- Client and server must both be on 1.0.20 (network protocol changed).

### Português

**Novidades**
- **Duas classes novas:** **Arqueiro** (arco ou besta, guarda uma espada para quando acabam as flechas, grito "Chuva de Flechas") e **Porta-estandarte** (estandarte na mão secundária, aura de Resistência em você e nos aliados próximos, grito "Reagrupar"). As duas aparecem nas vilas e pelo Posto da Vanguarda.
- **Todo companheiro já nasce com classe** (Berserker, Guardião, Duelista, Arqueiro ou Porta-estandarte). Não existe mais Guerreiro sem classe nem escolha de classe no nível 5; Guerreiros antigos viram uma classe sorteada ao carregar, mantendo nível, nome e itens.
- **Talentos:** aba nova de Talentos com 10 talentos. O companheiro ganha 1 ponto a cada 2 níveis.
- **Combate à distância:** qualquer companheiro com arco ou besta e flechas na mochila luta de longe. As flechas nunca acertam você nem seus outros companheiros.
- **Desgaste de equipamento:** aviso de durabilidade baixa no chat, troca automática quando um item quebra e reparo com materiais da mochila ao descansar numa fogueira ou no Posto da Vanguarda.
- **Montarias:** na postura Seguir, os companheiros montam nos seus cavalos domados e selados quando você monta, e são teleportados junto com o cavalo.
- **Aviso de morte:** você recebe uma mensagem no chat quando um companheiro morre, com a causa e o lugar onde os itens caíram.
- **Tela Minha Companhia** (tecla **J**, muda em Controles): todos os seus companheiros, mesmo longe ou em outra dimensão, com classe, nível, vida, postura e onde estão. Dá para chamar um ou todos, trocar a postura e escolher a formação por ali.
- **Formações:** agachado + botão direito com a Corneta alterna **Livre → Coluna → Linha → Círculo**. Os companheiros em Seguir mantêm o lugar em volta de você e giram junto; numa luta saem da formação e voltam depois.
- **Jade / WTHIT** (opcional): tooltip com nível, classe, dono e postura.
- Seções novas na config: regeneração, chances de escudo e esquiva, combate à distância, desgaste, raio da aura, montarias, spawns e custo do edital.

**Mudanças**
- Telas do companheiro: abas Mochila, Habilidades e Talentos no mesmo lugar, e as três telas têm o mesmo tamanho.
- Corrigido: logo depois de contratar, o botão de Monstros aparecia ligado e podia ligar a caça sem querer.
- A coleta de itens voltou a ser só **ligada ou desligada**: ligada, pega tudo por onde passa, inclusive o que você joga para ele; desligada, não pega nada. O modo "só úteis" saiu (quem estava nele passa a pegar tudo).
- Auto-equipar: o Porta-estandarte coloca um estandarte na mão secundária em vez de escudo, e o Arqueiro deixa a mão secundária vazia.
- Corneta: quem está de **Guarda** agora persegue o alvo mandado mesmo longe do posto, e depois volta para ele.
- Corneta: não aceita mais animais domesticados de outros jogadores, aldeões, golens nem suportes de armadura, e mira através dos seus companheiros em vez de virar "reagrupar".
- Moedas na mão secundária agora contam na contratação.
- Corrigido: companheiros de **Guarda** hesitavam antes de atacar (miravam, davam uns passos, paravam, voltavam). Agora só escolhem inimigos a até uns 10 blocos do posto (20 com arco) e vão até eles sem travar.
- Corrigido: o auto-equipar podia colocar uma **armadura na mão principal** quando ela estava vazia (o Epic Fight marca armadura com a categoria "não é arma", que contava como arma). Armadura não entra mais nos slots de arma.
- O nível 20 agora reduz de verdade a recarga do grito em 20% (50 s → 40 s), como a árvore prometia.
- Os itens de um companheiro morto agora passam pelo evento de drops do Forge, então mods de lápide (Gravestone, Corail Tombstone) guardam esses itens.
- Opções novas na config: chance e multiplicador do crítico do Berserker, limite e bônus da Fúria Sangrenta e chance de provocação do Guardião. A árvore de habilidades mostra a recarga do grito configurada.
- Livro guia: páginas novas do Arqueiro, do Porta-estandarte, montarias, desgaste e reparo, arco e besta, aviso de morte, descanso no Vanguard Point e alcance da Guarda.
- Companheiro recém-contratado ou resgatado entra na lista de companheiros na hora (antes levava até 10 s).
- A regeneração natural agora vem **desligada por padrão**: os companheiros se curam com comida, poções e fogueira (`companions.naturalRegen` liga de novo).
- A coleta de itens agora começa **desligada** nos companheiros novos; ligue no botão ✋.
- A ordem rápida (agachado + botão direito de mão vazia) agora alterna **Seguir → Guarda → Parado**; a Guarda protege o lugar onde o companheiro está.
- Corrigido: crash quando um companheiro montava num cavalo com o Epic Fight instalado.
- Corrigido: a vida podia passar do máximo.
- Corrigido: nomes de classe longos saíam da caixa na tela do companheiro.

**Observações**
- Cliente e servidor precisam estar os dois na 1.0.20 (o protocolo de rede mudou).

---

## 1.0.19 (Minecraft 1.20.1, Forge 47.x)

### English

**New**
- **Vanguard War Horn** (4 copper ingots + 1 gold coin): aim at a creature and blow it, and all your companions within 32 blocks attack it, even if peaceful. Aim at nothing and they stop fighting and regroup.
- **Quick order:** sneak + right-click your companion with an empty hand to switch between Follow and Stay without opening the screen.
- **Auto-equip:** companions put on better armor, a stronger melee weapon or a shield from their backpack on their own (out of combat). The old piece goes back to the backpack. Bows and crossbows are never swapped.
- **Item pick-up modes** (✋ button): off, **useful only** (food, healing potions, coins, arrows and better gear; new default) or everything.
- **Config file** `config/epicvanguard-common.toml`: recruit cost, XP gain, war cry cooldown, Vanguard Point request time, follow teleport distances and how long the save remembers dead/dismissed companions. Defaults are the same as before.
- Guide book: new "Server Settings" page.

**Changes**
- Natural regeneration now waits 5 seconds after any damage, including falls and fire (before, only damage from mobs counted).
- Closing a companion's screen no longer lets them walk off while another player still has it open.
- Less work per tick: companions share one scan for incoming arrows and creepers instead of several, only update their equipment slots when the gear actually changes, and the follow rescue finds loaded companions faster.
- The companion save file is only rewritten when something changed, and old dead/dismissed entries are cleaned up after 100 in-game days (configurable).
- Warrior houses and pillager outposts are now tracked per dimension (old worlds are converted automatically).
- English texts reviewed: "Vanguard Point" everywhere, smoother dialogue lines.

**Notes**
- Client and server must both be on 1.0.19 (network protocol changed).
- Companions from 1.0.18 keep their pick-up setting (on = everything, off = off).
- The Java package changed (`io.github.raffasanttos.epicvanguard`). The mod id is still `epicvanguard`, so worlds and items are not affected.

### Português

**Novidades**
- **Corneta da Vanguarda** (4 barras de cobre + 1 moeda de ouro): mire numa criatura e toque, e todos os seus companheiros num raio de 32 blocos atacam, mesmo pacíficos. Mirando no nada, eles param de lutar e se reagrupam.
- **Ordem rápida:** agachado + botão direito no companheiro de mão vazia alterna entre Seguir e Parado sem abrir a tela.
- **Auto-equipar:** os companheiros vestem sozinhos armadura melhor, uma arma corpo a corpo mais forte ou um escudo que estiverem na mochila (fora de combate). A peça antiga volta para a mochila. Arcos e bestas nunca são trocados.
- **Modos de coleta** (botão ✋): desligada, **só úteis** (comida, poções de cura, moedas, flechas e equipamento melhor; novo padrão) ou tudo.
- **Arquivo de config** `config/epicvanguard-common.toml`: custo de contratação, ganho de XP, recarga do grito de guerra, tempo do edital do Posto, distâncias do teleporte de seguir e por quanto tempo o save lembra companheiros mortos/dispensados. Os padrões são os mesmos de antes.
- Livro guia: página nova "Configurações do Servidor".

**Mudanças**
- A regeneração natural agora espera 5 segundos depois de qualquer dano, inclusive queda e fogo (antes só contava dano de mobs).
- Fechar a tela de um companheiro não faz mais ele sair andando enquanto outro jogador ainda está com ela aberta.
- Menos trabalho por tick: os companheiros fazem uma varredura só para flechas e creepers em vez de várias, só atualizam os slots de equipamento quando o equipamento muda de fato, e o resgate do modo Seguir acha companheiros carregados mais rápido.
- O arquivo de companheiros só é regravado quando algo mudou, e registros antigos de mortos/dispensados são limpos depois de 100 dias de jogo (configurável).
- Casas de guerreiro e postos de saqueadores agora são marcados por dimensão (mundos antigos são convertidos sozinhos).
- Textos em inglês revisados: "Vanguard Point" em todo lugar e falas mais naturais.

**Observações**
- Cliente e servidor precisam estar os dois na 1.0.19 (o protocolo de rede mudou).
- Companheiros da 1.0.18 mantêm a coleta como estava (ligada = tudo, desligada = desligada).
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
