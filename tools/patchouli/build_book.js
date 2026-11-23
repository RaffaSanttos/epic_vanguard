// Gera o livro do Patchouli (Guia da Vanguarda) em inglês e português.
// Uso: node tools/patchouli/build_book.js   (a partir da raiz do mod)
// Edite o texto aqui e rode de novo: os JSON em src/main/resources são sobrescritos.
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const NS = 'epicvanguard';
const BOOK = 'guide';
const ASSETS = path.join(ROOT, 'src/main/resources/assets', NS, 'patchouli_books', BOOK);

// ── Categorias ─────────────────────────────────────────────────────────────
const categories = [
  { id: 'getting_started', icon: 'epicvanguard:gold_coin', sortnum: 0,
    en: ['Getting Started', 'Gold coins, where to find mercenaries and how to hire them.'],
    pt: ['Primeiros Passos', 'Moedas de ouro, onde achar mercenários e como contratá-los.'] },
  { id: 'commanding', icon: 'minecraft:iron_sword', sortnum: 1,
    en: ['Commanding Companions', 'The companion screen, stances, inventory and how they protect you.'],
    pt: ['Comandando Companheiros', 'A tela do companheiro, posturas, inventário e como eles te protegem.'] },
  { id: 'classes', icon: 'minecraft:experience_bottle', sortnum: 2,
    en: ['Classes & Progression', 'Levels, the three classes and their warcries.'],
    pt: ['Classes e Progressão', 'Níveis, as três classes e seus gritos de guerra.'] },
  { id: 'vanguard_point', icon: 'epicvanguard:vanguard_point', sortnum: 3,
    en: ['The Vanguard Point', 'Summon new mercenary companies to your base.'],
    pt: ['O Vanguard Point', 'Convoque novas companhias de mercenários para a sua base.'] },
  { id: 'reference', icon: 'minecraft:command_block', sortnum: 4,
    en: ['Commands & Settings', 'Chat commands, friendly fire and the server config file.'],
    pt: ['Comandos e Configurações', 'Comandos de chat, fogo amigo e o arquivo de config do servidor.'] },
];

// ── Entradas ───────────────────────────────────────────────────────────────
// Cada página: { type, ...campos } com text/title em { en, pt }.
const t = (en, pt) => ({ en, pt });

const entries = [
  // ═══ Primeiros passos ═══
  { category: 'getting_started', id: 'introduction', icon: 'epicvanguard:warrior_companion_spawn_egg', sortnum: 0,
    name: t('Welcome, Commander', 'Bem-vindo, Comandante'),
    pages: [
      { type: 'text', text: t(
        'Epic Vanguard adds $(thing)mercenary companions$(): warriors you hire with $(item)Gold Coins$(), equip, and lead into battle.$(br2)They level up, belong to a $(thing)class$() with its own warcry, eat and drink potions on their own and follow you through teleports and dimensions.',
        'O Epic Vanguard adiciona $(thing)companheiros mercenários$(): guerreiros que você contrata com $(item)Moedas de Ouro$(), equipa e leva para a batalha.$(br2)Eles sobem de nível, pertencem a uma $(thing)classe$() com grito de guerra próprio, comem e bebem poções sozinhos e te seguem por teleportes e dimensões.') },
      { type: 'entity', entity: 'epicvanguard:guardian_companion', scale: 0.9, text: t(
        'With $(item)Epic Fight$() installed, companions fight with its animations. Without it, they use regular Minecraft combat.',
        'Com o $(item)Epic Fight$() instalado, os companheiros lutam com as animações dele. Sem ele, usam o combate normal do Minecraft.') },
    ] },
  { category: 'getting_started', id: 'gold_coins', icon: 'epicvanguard:gold_coin', sortnum: 1,
    name: t('Gold Coins', 'Moedas de Ouro'),
    pages: [
      { type: 'spotlight', item: 'epicvanguard:gold_coin', text: t(
        'Mercenaries only work for $(item)Gold Coins$(). You need them to hire a companion and to post a request at a $(l:epicvanguard:vanguard_point/vanguard_point)Vanguard Point$().',
        'Mercenários só trabalham por $(item)Moedas de Ouro$(). Você precisa delas para contratar um companheiro e para publicar um edital no $(l:epicvanguard:vanguard_point/vanguard_point)Vanguard Point$().') },
      { type: 'crafting', recipe: 'epicvanguard:gold_coin', recipe2: 'epicvanguard:gold_nuggets_from_coin', text: t(
        'Two gold ingots make one coin. A coin can be melted back into 18 gold nuggets.',
        'Dois lingotes de ouro fazem uma moeda. Uma moeda pode ser desfeita em 18 pepitas de ouro.') },
    ] },
  { category: 'getting_started', id: 'finding_mercenaries', icon: 'minecraft:compass', sortnum: 2,
    name: t('Finding Mercenaries', 'Onde Achar Mercenários'),
    pages: [
      { type: 'text', title: t('Warrior Houses', 'Casas de Guerreiro'), text: t(
        'Small $(thing)warrior houses$() are scattered across the Overworld. When you get close to one for the first time, a mercenary is waiting there to be hired.$(br2)Every mercenary belongs to one of the classes: $(l:epicvanguard:classes/berserker)Berserker$(), $(l:epicvanguard:classes/guardian)Guardian$() or $(l:epicvanguard:classes/duelist)Duelist$().',
        'Pequenas $(thing)casas de guerreiro$() estão espalhadas pelo Overworld. Na primeira vez que você chega perto de uma, há um mercenário esperando para ser contratado.$(br2)Todo mercenário pertence a uma das classes: $(l:epicvanguard:classes/berserker)Berserker$(), $(l:epicvanguard:classes/guardian)Guardião$() ou $(l:epicvanguard:classes/duelist)Duelista$().') },
      { type: 'text', title: t('Prisoners of War', 'Prisioneiros de Guerra'), text: t(
        'Next to $(thing)Pillager Outposts$() you may find a wooden cage holding a $(thing)prisoner$(): weak, unarmed and wounded.$(br2)Rescue them for free: see $(l:epicvanguard:getting_started/recruiting)Hiring a Companion$().',
        'Perto dos $(thing)Postos de Saqueadores$() pode haver uma cela de madeira com um $(thing)prisioneiro$(): fraco, desarmado e ferido.$(br2)Você pode resgatá-lo de graça: veja $(l:epicvanguard:getting_started/recruiting)Contratando um Companheiro$().') },
      { type: 'text', title: t('Calling a Company', 'Convocando uma Companhia'), text: t(
        'You can also build a $(l:epicvanguard:vanguard_point/vanguard_point)Vanguard Point$() at your base and pay to have a new mercenary march to you.',
        'Você também pode construir um $(l:epicvanguard:vanguard_point/vanguard_point)Vanguard Point$() na sua base e pagar para um novo mercenário marchar até você.') },
    ] },
  { category: 'getting_started', id: 'recruiting', icon: 'minecraft:writable_book', sortnum: 3,
    name: t('Hiring a Companion', 'Contratando um Companheiro'),
    pages: [
      { type: 'text', title: t('Contract of Honor', 'Contrato de Honra'), text: t(
        '$(thing)Right-click$() a mercenary to open the $(thing)Contract of Honor$(). It shows their name, rank and price.$(br2)If you carry enough $(item)Gold Coins$(), press $(thing)Seal Pact$(). The coins are taken from your inventory and the mercenary swears loyalty to you.',
        'Clique com o $(thing)botão direito$() num mercenário para abrir o $(thing)Contrato de Honra$(). Ele mostra o nome, a patente e o preço.$(br2)Se você tiver $(item)Moedas de Ouro$() suficientes, aperte $(thing)Selar Pacto$(). As moedas saem do seu inventário e o mercenário jura lealdade a você.') },
      { type: 'text', title: t('Ranks and Prices', 'Patentes e Preços'), text: t(
        'The price depends on the gear they come with:$(br)$(li)$(thing)Recruit$(): leather and wood, level 1, 20 to 30 coins.$(li)$(thing)Veteran$(): chainmail, stone and shield, level 2, 40 to 50 coins.$(li)$(thing)Elite$(): iron and shield, level 3, 60 to 80 coins.$(br2)Their starting gear is yours to keep and upgrade.',
        'O preço depende do equipamento com que ele vem:$(br)$(li)$(thing)Recruta$(): couro e madeira, nível 1, 20 a 30 moedas.$(li)$(thing)Veterano$(): cota de malha, pedra e escudo, nível 2, 40 a 50 moedas.$(li)$(thing)Elite$(): ferro e escudo, nível 3, 60 a 80 moedas.$(br2)O equipamento inicial passa a ser seu para melhorar.') },
      { type: 'text', title: t('Rescuing a Prisoner', 'Resgatando um Prisioneiro'), text: t(
        'A prisoner joins you for free. $(thing)Right-click$() them holding $(item)food$(), a $(item)potion$() or a $(item)sword or axe$().$(br2)They are healed, lose their weakness and swear loyalty. A weapon you hand over goes straight to their main hand.',
        'Um prisioneiro se junta a você de graça. Clique nele com o $(thing)botão direito$() segurando $(item)comida$(), uma $(item)poção$() ou uma $(item)espada ou machado$().$(br2)Ele é curado, perde a fraqueza e jura lealdade. A arma que você entregar vai direto para a mão principal dele.') },
    ] },

  // ═══ Comandando ═══
  { category: 'commanding', id: 'companion_screen', icon: 'minecraft:chest', sortnum: 0,
    name: t('The Companion Screen', 'A Tela do Companheiro'),
    pages: [
      { type: 'text', text: t(
        '$(thing)Right-click$() your companion to open their screen (owner only).$(br)On the right side:$(li)$(thing)Stance$(): cycles between $(l:epicvanguard:commanding/stances)Follow, Guard and Stay$().$(li)$(thing)Stop$(): ends the current fight and clears the target.$(li)Three $(thing)tactic buttons$(), explained on the next page.',
        'Clique com o $(thing)botão direito$() no seu companheiro para abrir a tela dele (só o dono).$(br)No lado direito:$(li)$(thing)Postura$(): alterna entre $(l:epicvanguard:commanding/stances)Seguir, Guarda e Parado$().$(li)$(thing)Parar$(): encerra a luta atual e limpa o alvo.$(li)Três $(thing)botões de tática$(), explicados na próxima página.') },
      { type: 'text', title: t('Tactics', 'Táticas'), text: t(
        'Lit buttons are on, grey ones are off.$(li)$(thing)Sword$(): attack hostile monsters. Off for new recruits.$(li)$(thing)Meat$(): hunt adult animals for food.$(li)$(thing)Hand$(): item pick-up. Click to cycle: off (grey), useful only (yellow) and everything (green). New recruits start with it off. Items you drop for them are always picked up.',
        'Botão aceso está ligado, cinza está desligado.$(li)$(thing)Espada$(): atacar monstros hostis. Desligado ao recrutar.$(li)$(thing)Carne$(): caçar animais adultos para comida.$(li)$(thing)Mão$(): coleta de itens. Clique para trocar: desligada (cinza), só úteis (amarela) e tudo (verde). Recrutas novos começam com ela desligada. O que você joga para ele é sempre pego.') },
      { type: 'text', title: t('Tabs and Name', 'Abas e Nome'), text: t(
        'The $(thing)Backpack$() tab shows their gear and health; the $(thing)Skills$() tab shows their level, XP and class skills; the $(thing)Talents$() tab is where you spend talent points.$(br2)To rename a companion, press the $(thing)pencil$(), use a named $(item)Name Tag$() on them, or the $(l:epicvanguard:reference/commands)rename command$().',
        'A aba $(thing)Mochila$() mostra o equipamento e a vida; a aba $(thing)Habilidades$() mostra o nível, o XP e as habilidades da classe; na aba $(thing)Talentos$() você gasta os pontos de talento.$(br2)Para renomear um companheiro, aperte o $(thing)lápis$(), use uma $(item)Etiqueta$() com nome nele, ou o $(l:epicvanguard:reference/commands)comando de renomear$().') },
    ] },
  { category: 'commanding', id: 'stances', icon: 'minecraft:shield', sortnum: 1,
    name: t('Stances', 'Posturas'),
    pages: [
      { type: 'text', text: t(
        '$(thing)Follow$() (default): walks with you and fights what threatens you. More than 13 blocks behind for 2 seconds, they teleport to your side, even across dimensions or from unloaded chunks. In a fight they finish it, unless you get over 32 blocks away.',
        '$(thing)Seguir$() (padrão): anda com você e luta contra o que te ameaça. Mais de 13 blocos para trás por 2 segundos, se teleporta para o seu lado, até entre dimensões ou de chunks descarregados. Numa luta, termina a luta, a menos que você passe de 32 blocos.') },
      { type: 'text', text: t(
        '$(thing)Guard$(): defends the spot where you set it, within about 6 blocks.$(br2)$(thing)Stay$(): stands still and chases nothing, but defends themselves.$(br2)In $(thing)Guard$() or $(thing)Stay$() they never teleport to you. Use the $(l:epicvanguard:reference/commands)call command$() to bring them back.',
        '$(thing)Guarda$(): defende o ponto escolhido, a uns 6 blocos dele.$(br2)$(thing)Parado$(): fica imóvel e não persegue nada, mas se defende.$(br2)Em $(thing)Guarda$() ou $(thing)Parado$() eles nunca se teleportam até você. Use o $(l:epicvanguard:reference/commands)comando de chamar$() para trazê-los de volta.') },
      { type: 'text', title: t('Quick Orders', 'Ordens Rápidas'), text: t(
        '$(thing)Sneak + right-click$() your companion with an empty hand to cycle $(thing)Follow$() → $(thing)Guard$() → $(thing)Stay$() without opening the screen. Guard protects the spot where they are at that moment.$(br2)To command everyone at once, use the $(l:epicvanguard:commanding/war_horn)Vanguard War Horn$().',
        '$(thing)Agachado + botão direito$() no companheiro, de mão vazia, alterna $(thing)Seguir$() → $(thing)Guarda$() → $(thing)Parado$() sem abrir a tela. A Guarda protege o lugar onde ele está naquele momento.$(br2)Para comandar todos de uma vez, use a $(l:epicvanguard:commanding/war_horn)Corneta da Vanguarda$().') },
    ] },
  { category: 'commanding', id: 'inventory', icon: 'minecraft:bundle', sortnum: 2,
    name: t('Gear and Backpack', 'Equipamento e Mochila'),
    pages: [
      { type: 'text', text: t(
        'Every companion has $(thing)4 armor slots$(), a $(thing)main weapon$() slot, an $(thing)off-hand$() slot for a shield or second weapon, and a $(thing)15-slot backpack$().$(br2)Swords, axes, Epic Fight weapons and most modded weapons fit the weapon slots. Shift-click sends armor and weapons to the right slot.',
        'Todo companheiro tem $(thing)4 slots de armadura$(), um slot de $(thing)arma principal$(), um slot de $(thing)mão secundária$() para escudo ou segunda arma, e uma $(thing)mochila de 15 slots$().$(br2)Espadas, machados, armas do Epic Fight e a maioria das armas de outros mods cabem nos slots de arma. Shift + clique manda armadura e armas para o slot certo.') },
      { type: 'text', text: t(
        'Weapon enchantments work: $(thing)Sharpness$(), $(thing)Smite$(), $(thing)Knockback$(), $(thing)Fire Aspect$() and others. Weapons lose durability like yours and break when worn out.$(br2)Companions $(thing)pick up items$() they walk over, including what you drop for them. See the $(thing)Hand$() button.',
        'Os encantamentos da arma funcionam: $(thing)Afiação$(), $(thing)Julgamento$(), $(thing)Repulsão$(), $(thing)Aspecto Flamejante$() e outros. As armas gastam como as suas e quebram quando acabam.$(br2)Companheiros $(thing)pegam itens$() do chão ao passar por cima, inclusive o que você joga para eles. Veja o botão $(thing)Mão$().') },
      { type: 'text', title: t('Auto-Equip', 'Auto-Equipar'), text: t(
        'Put better gear in the $(thing)backpack$() and the companion wears it on their own after a moment out of combat: armor by protection, melee weapon by damage, a shield if the off-hand is empty. The old piece goes back to the backpack.$(br2)They never swap out a bow or crossbow, and skip nearly broken items.',
        'Coloque equipamento melhor na $(thing)mochila$() e o companheiro veste sozinho depois de um tempo fora de combate: armadura pela proteção, arma corpo a corpo pelo dano, escudo se a mão secundária estiver vazia. A peça antiga volta para a mochila.$(br2)Ele nunca troca um arco ou besta e ignora itens quase quebrados.') },
      { type: 'text', title: t('If They Fall', 'Se Ele Cair'), text: t(
        'When a companion dies, everything they carried drops on the ground. The companion is gone for good, so keep them fed and well armored.',
        'Quando um companheiro morre, tudo o que ele carregava cai no chão. Ele não volta, então mantenha-o alimentado e bem protegido.') },
    ] },
  { category: 'commanding', id: 'survival', icon: 'minecraft:golden_apple', sortnum: 3,
    name: t('Food and Healing', 'Comida e Cura'),
    pages: [
      { type: 'text', text: t(
        'Companions take care of themselves with what is in their $(thing)backpack$():$(li)Out of combat and hurt, they eat $(item)food$() until healed.$(li)They drink $(item)Healing$() and $(item)Regeneration$() potions; the empty bottle is discarded.$(li)In an emergency they prefer the strongest items, like $(item)golden apples$().',
        'Os companheiros se cuidam com o que está na $(thing)mochila$():$(li)Fora de combate e feridos, comem $(item)comida$() até se curarem.$(li)Bebem poções de $(item)Cura$() e $(item)Regeneração$(); o frasco vazio é descartado.$(li)Numa emergência preferem os itens mais fortes, como $(item)maçãs douradas$().') },
      { type: 'text', title: t('Retreat', 'Recuo'), text: t(
        'When their health gets low and they carry something to heal with, they $(thing)roll away$(), retreat and eat before returning to the fight.$(br2)Without any food they keep fighting. They do not regenerate on their own: carry food and potions for them. (Servers can turn natural regeneration on in the config.)',
        'Quando a vida fica baixa e eles têm algo para se curar, eles $(thing)rolam para trás$(), recuam e comem antes de voltar à luta.$(br2)Sem nenhuma comida, continuam lutando. Eles não se regeneram sozinhos: leve comida e poções para eles. (O servidor pode ligar a regeneração natural na config.)') },
    ] },
  { category: 'commanding', id: 'protection', icon: 'minecraft:totem_of_undying', sortnum: 4,
    name: t('Protecting You', 'Protegendo Você'),
    pages: [
      { type: 'text', text: t(
        'Companions in $(thing)Follow$() or $(thing)Guard$() attack whatever hurts you.$(br2)$(thing)Heroic Intervention$(): if your health drops to 3 hearts or less, a nearby companion rushes to the creature attacking you.$(br2)Companions holding a $(item)shield$() raise it against arrows and incoming blows, and everyone tries to get away from creepers about to explode.',
        'Companheiros em $(thing)Seguir$() ou $(thing)Guarda$() atacam o que te machuca.$(br2)$(thing)Intervenção Heroica$(): se sua vida cair para 3 corações ou menos, um companheiro próximo corre até a criatura que te ataca.$(br2)Companheiros com $(item)escudo$() o levantam contra flechas e golpes, e todos tentam fugir de creepers prestes a explodir.') },
      { type: 'text', title: t('Friendly Fire', 'Fogo Amigo'), text: t(
        'By default your attacks never hurt your own companions, and companions of the same owner never hit each other. An operator can change this: see $(l:epicvanguard:reference/friendly_fire)Friendly Fire$().',
        'Por padrão seus ataques nunca ferem seus companheiros, e companheiros do mesmo dono nunca se acertam. Um operador pode mudar isso: veja $(l:epicvanguard:reference/friendly_fire)Fogo Amigo$().') },
    ] },
  { category: 'commanding', id: 'war_horn', icon: 'epicvanguard:vanguard_horn', sortnum: 5,
    name: t('Vanguard War Horn', 'Corneta da Vanguarda'),
    pages: [
      { type: 'text', text: t(
        'Blow the horn ($(thing)right-click$()) to give an order to all your companions within 32 blocks:$(li)$(thing)aiming at a creature$(): they all attack it, even if peaceful;$(li)$(thing)aiming at nothing$(): they stop fighting and regroup.$(br2)Companions in $(thing)Stay$() hold their position. Players, companions and your own pets can\'t be targeted.',
        'Toque a corneta ($(thing)botão direito$()) para dar uma ordem a todos os seus companheiros num raio de 32 blocos:$(li)$(thing)mirando numa criatura$(): todos atacam, mesmo pacíficos;$(li)$(thing)mirando no nada$(): param de lutar e se reagrupam.$(br2)Quem está em $(thing)Parado$() segura a posição. Jogadores, companheiros e seus animais não podem ser alvo.') },
      { type: 'crafting', recipe: 'epicvanguard:vanguard_horn', text: t(
        'Four copper ingots and a gold coin.', 'Quatro barras de cobre e uma moeda de ouro.') },
    ] },

  // ═══ Classes ═══
  { category: 'classes', id: 'leveling', icon: 'minecraft:experience_bottle', sortnum: 0,
    name: t('Levels and Experience', 'Níveis e Experiência'),
    pages: [
      { type: 'text', text: t(
        'Companions gain XP by fighting:$(li)Killing a monster themselves gives the most.$(li)Helping with a kill gives some.$(li)When $(thing)you$() kill a monster, companions within 24 blocks also learn.$(br2)The maximum level is $(thing)20$(). Each level-up heals them by half their health.',
        'Os companheiros ganham XP lutando:$(li)Matar um monstro eles mesmos dá mais.$(li)Ajudar numa morte dá um pouco.$(li)Quando $(thing)você$() mata um monstro, companheiros a até 24 blocos também aprendem.$(br2)O nível máximo é $(thing)20$(). Cada nível novo cura metade da vida.') },
      { type: 'text', title: t('Per Level', 'Por Nível'), text: t(
        'Every level adds:$(li)+1.5 max health$(li)+0.25 base damage$(li)+0.5 natural armor$(li)a little armor toughness and speed$(br2)From $(thing)level 5$() they unlock their class $(l:epicvanguard:classes/warcries)warcry$().',
        'Cada nível adiciona:$(li)+1,5 de vida máxima$(li)+0,25 de dano base$(li)+0,5 de armadura natural$(li)um pouco de resistência de armadura e velocidade$(br2)A partir do $(thing)nível 5$() eles liberam o $(l:epicvanguard:classes/warcries)grito de guerra$() da classe.') },
      { type: 'text', title: t('Classes', 'Classes'), text: t(
        'Every companion already has a class when you meet them: $(l:epicvanguard:classes/berserker)Berserker$(), $(l:epicvanguard:classes/guardian)Guardian$(), $(l:epicvanguard:classes/duelist)Duelist$(), Archer or Standard-bearer. The class never changes.$(br2)Level, XP and gear grow with them; talent points (1 every 2 levels) go in the $(thing)Talents$() tab.',
        'Todo companheiro já tem uma classe quando você o encontra: $(l:epicvanguard:classes/berserker)Berserker$(), $(l:epicvanguard:classes/guardian)Guardião$(), $(l:epicvanguard:classes/duelist)Duelista$(), Arqueiro ou Porta-estandarte. A classe não muda.$(br2)Nível, XP e equipamento evoluem com ele; os pontos de talento (1 a cada 2 níveis) vão na aba $(thing)Talentos$().') },
    ] },
  { category: 'classes', id: 'berserker', icon: 'epicvanguard:berserker_companion_spawn_egg', sortnum: 1,
    name: t('Berserker', 'Berserker'),
    pages: [
      { type: 'entity', entity: 'epicvanguard:berserker_companion', scale: 0.9, name: t('Berserker', 'Berserker'), text: t(
        'All-out offense. Hits harder than the other classes and fights until 20% health before retreating.',
        'Ataque total. Bate mais forte que as outras classes e luta até 20% da vida antes de recuar.') },
      { type: 'text', text: t(
        '$(li)Deals 90% of the weapon damage (other classes deal 75%).$(li)$(thing)Critical hits$(): 20% chance to deal 1.5x damage.$(li)$(thing)Fury$(): below 40% health, deals 20% more damage.$(br2)$(thing)Warcry$(): Fierce Blood Cry. See $(l:epicvanguard:classes/warcries)Warcries$().',
        '$(li)Causa 90% do dano da arma (as outras classes causam 75%).$(li)$(thing)Críticos$(): 20% de chance de causar 1,5x de dano.$(li)$(thing)Fúria$(): abaixo de 40% da vida, causa 20% mais dano.$(br2)$(thing)Grito de guerra$(): Grito de Sangue Feroz. Veja $(l:epicvanguard:classes/warcries)Gritos de Guerra$().') },
    ] },
  { category: 'classes', id: 'guardian', icon: 'epicvanguard:guardian_companion_spawn_egg', sortnum: 2,
    name: t('Guardian', 'Guardião'),
    pages: [
      { type: 'entity', entity: 'epicvanguard:guardian_companion', scale: 0.9, name: t('Guardian', 'Guardião'), text: t(
        'A living wall. Takes the hits so you do not have to. Give them a shield.',
        'Uma muralha viva. Aguenta os golpes por você. Dê um escudo a ele.') },
      { type: 'text', text: t(
        '$(li)+30% max health.$(li)+6 natural armor and +2 armor toughness.$(li)40% knockback resistance.$(li)Pairs best with a shield, which stops attacks from creatures (not falls, fire or lava).$(br2)$(thing)Warcry$(): Bastion\'s Roar, which pulls every nearby monster onto the Guardian.',
        '$(li)+30% de vida máxima.$(li)+6 de armadura natural e +2 de resistência de armadura.$(li)40% de resistência a empurrão.$(li)Combina com um escudo, que para ataques de criaturas (não queda, fogo ou lava).$(br2)$(thing)Grito de guerra$(): Rugido do Bastião, que puxa todos os monstros próximos para o Guardião.') },
    ] },
  { category: 'classes', id: 'duelist', icon: 'epicvanguard:duelist_companion_spawn_egg', sortnum: 3,
    name: t('Duelist', 'Duelista'),
    pages: [
      { type: 'entity', entity: 'epicvanguard:duelist_companion', scale: 0.9, name: t('Duelist', 'Duelista'), text: t(
        'Fast and slippery. Dodges blows instead of blocking them.',
        'Rápido e escorregadio. Desvia dos golpes em vez de bloqueá-los.') },
      { type: 'text', text: t(
        '$(li)+15% movement speed.$(li)$(thing)Perfect Dodge$(): 18% chance to avoid all damage from an attack or projectile.$(li)Can dodge-roll twice as often as other classes.$(br2)$(thing)Warcry$(): Storm Cry, which staggers nearby enemies and speeds up you and the Duelist.',
        '$(li)+15% de velocidade.$(li)$(thing)Esquiva Perfeita$(): 18% de chance de evitar todo o dano de um ataque ou projétil.$(li)Pode rolar para esquivar duas vezes mais que as outras classes.$(br2)$(thing)Grito de guerra$(): Grito da Tempestade, que desestabiliza inimigos próximos e acelera você e o Duelista.') },
    ] },
  { category: 'classes', id: 'warcries', icon: 'minecraft:goat_horn', sortnum: 4,
    name: t('Warcries', 'Gritos de Guerra'),
    pages: [
      { type: 'text', text: t(
        'From $(thing)level 5$(), companions shout a warcry in tough fights: when you or they drop below 40% health, or when 2 or more monsters are within 10 blocks. It recharges in $(thing)50 seconds$(). Buffs reach you within ~16 blocks.$(br2)$(thing)Berserker$(): nearby monsters get Weakness for 10 s. The Berserker gets Strength II and you get Strength I.',
        'A partir do $(thing)nível 5$(), os companheiros dão um grito de guerra em lutas difíceis: quando você ou eles ficam abaixo de 40% da vida, ou quando há 2 ou mais monstros a até 10 blocos. Ele recarrega em $(thing)50 segundos$(). Os bônus te alcançam a até ~16 blocos.$(br2)$(thing)Berserker$(): monstros próximos recebem Fraqueza por 10 s. O Berserker ganha Força II e você ganha Força I.') },
      { type: 'text', text: t(
        '$(thing)Guardian$(): every monster within 12 blocks targets the Guardian. The Guardian gets Resistance II, and you get Strength I and Resistance I for 15 s.$(br2)$(thing)Duelist$(): enemies within 6 blocks are slowed and weakened for a moment. You and the Duelist get Speed II and Strength I for 12 s.',
        '$(thing)Guardião$(): todo monstro a até 12 blocos passa a mirar no Guardião. Ele ganha Resistência II, e você ganha Força I e Resistência I por 15 s.$(br2)$(thing)Duelista$(): inimigos a até 6 blocos ficam lentos e fracos por um instante. Você e o Duelista ganham Velocidade II e Força I por 12 s.') },
    ] },

  // ═══ Vanguard Point ═══
  { category: 'vanguard_point', id: 'vanguard_point', icon: 'epicvanguard:vanguard_point', sortnum: 0,
    name: t('Posting a Request', 'Publicando um Edital'),
    pages: [
      { type: 'crafting', recipe: 'epicvanguard:vanguard_point_shaped', text: t(
        'The $(item)Vanguard Point$() is a notice board where you call new mercenaries to your base.',
        'O $(item)Vanguard Point$() é um mural onde você chama novos mercenários para a sua base.') },
      { type: 'text', text: t(
        '$(thing)Right-click$() it, put $(item)20 Gold Coins$() in the slot and press $(thing)Post Request$(). Only the player who deposited the coins can use or take them back, and any extra coins are returned.$(br2)A company starts marching and arrives in $(thing)7 in-game days$(). Sleeping through the night counts too.',
        'Clique com o $(thing)botão direito$(), coloque $(item)20 Moedas de Ouro$() no slot e aperte $(thing)Publicar Edital$(). Só quem depositou as moedas pode usá-las ou retirá-las, e as moedas a mais são devolvidas.$(br2)Uma companhia começa a marchar e chega em $(thing)7 dias do jogo$(). Dormir à noite também conta.') },
      { type: 'text', title: t('Arrival', 'Chegada'), text: t(
        'Fireworks and horns announce the arrival. A new $(thing)Berserker, Guardian or Duelist$() appears next to the Vanguard Point.$(br2)The 20 coins only pay for the march. To keep the mercenary, you still need to $(l:epicvanguard:getting_started/recruiting)hire them$() normally.$(br2)The march only advances while its chunk is loaded.',
        'Fogos de artifício e cornetas anunciam a chegada. Um novo $(thing)Berserker, Guardião ou Duelista$() aparece ao lado do Vanguard Point.$(br2)As 20 moedas só pagam a marcha. Para ficar com o mercenário, você ainda precisa $(l:epicvanguard:getting_started/recruiting)contratá-lo$() normalmente.$(br2)A marcha só avança com o chunk carregado.') },
    ] },
  { category: 'vanguard_point', id: 'base_life', icon: 'minecraft:campfire', sortnum: 1,
    name: t('Life at the Base', 'Vida na Base'),
    pages: [
      { type: 'text', text: t(
        'Your companions in $(thing)Guard$() or $(thing)Stay$() near a $(item)Vanguard Point$() patrol around it when there is nothing to fight.$(br2)Near a lit $(item)campfire$(), resting companions sit down around the fire.$(br2)Mercenaries who are not hired yet hang around the closest Vanguard Point.',
        'Seus companheiros em $(thing)Guarda$() ou $(thing)Parado$() perto de um $(item)Vanguard Point$() patrulham em volta dele quando não há nada para enfrentar.$(br2)Perto de uma $(item)fogueira$() acesa, companheiros descansando sentam em volta do fogo.$(br2)Mercenários ainda não contratados ficam pelo Vanguard Point mais próximo.') },
    ] },

  // ═══ Referência ═══
  { category: 'reference', id: 'commands', icon: 'minecraft:command_block', sortnum: 0,
    name: t('Commands', 'Comandos'),
    pages: [
      { type: 'text', text: t(
        '$(thing)/companions$(): lists your companions with clickable buttons.$(li)$(thing)call$(): brings every companion to you.$(li)$(thing)bring <target>$(): brings one companion.$(li)$(thing)goto <target>$(): teleports you to one.$(li)$(thing)rename <target> <name>$()$(li)$(thing)dismiss <target>$(): releases a companion.$(li)$(thing)clean$(): removes missing entries.',
        '$(thing)/companhias$(): lista seus companheiros com botões clicáveis.$(li)$(thing)chamar$(): traz todos até você.$(li)$(thing)trazer <alvo>$(): traz um companheiro.$(li)$(thing)ir <alvo>$(): te teleporta até um.$(li)$(thing)renomear <alvo> <nome>$()$(li)$(thing)dispensar <alvo>$(): libera um companheiro.$(li)$(thing)limpar$(): remove registros perdidos.') },
      { type: 'text', text: t(
        'The $(thing)<target>$() can be the number from the list (like $(thing)#1$()), the name or the ID.$(br2)$(thing)/vanguard$() and $(thing)/companion$() work the same way, and the Portuguese names ($(thing)/companhias chamar$() and others) also work.$(br2)Only operators can use these commands on companions of other players.',
        'O $(thing)<alvo>$() pode ser o número da lista (como $(thing)#1$()), o nome ou o ID.$(br2)$(thing)/vanguard$() e $(thing)/companions$() funcionam igual, e os nomes em inglês ($(thing)/companions call$() e outros) também.$(br2)Só operadores podem usar esses comandos nos companheiros de outros jogadores.') },
    ] },
  { category: 'reference', id: 'friendly_fire', icon: 'minecraft:blaze_powder', sortnum: 1,
    name: t('Friendly Fire', 'Fogo Amigo'),
    pages: [
      { type: 'text', text: t(
        '$(thing)/friendlyfire$() shows the current mode. Operators can change it for the whole server:$(li)$(thing)off$(): your attacks never hurt your companions (default).$(li)$(thing)melee$(): only physical hits and Epic Fight attacks hurt.$(li)$(thing)spells$(): only spells and magic hurt.$(li)$(thing)all$(): everything hurts.',
        '$(thing)/fogoamigo$() mostra o modo atual. Operadores podem mudá-lo para o servidor inteiro:$(li)$(thing)livre$(): seus ataques nunca ferem seus companheiros (padrão).$(li)$(thing)fisico$(): só golpes físicos e ataques do Epic Fight ferem.$(li)$(thing)magias$(): só magias ferem.$(li)$(thing)total$(): tudo fere.') },
    ] },
  { category: 'reference', id: 'server_config', icon: 'minecraft:comparator', sortnum: 2,
    name: t('Server Settings', 'Configurações do Servidor'),
    pages: [
      { type: 'text', text: t(
        'The file $(thing)config/epicvanguard-common.toml$() lets the server owner change:$(li)recruit cost and XP gain;$(li)warcry cooldown;$(li)how long a Vanguard Point request takes;$(li)the follow teleport distances.$(br2)Values in this book are the defaults.',
        'O arquivo $(thing)config/epicvanguard-common.toml$() deixa o dono do servidor mudar:$(li)custo de contratação e ganho de XP;$(li)recarga do grito de guerra;$(li)quanto tempo leva um edital do Vanguard Point;$(li)as distâncias do teleporte de seguir.$(br2)Os valores deste livro são os padrões.') },
    ] },
];

// ── Geração ────────────────────────────────────────────────────────────────
function writeJson(file, obj) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(obj, null, 2) + '\n', 'utf8');
}

const LANGS = { en: 'en_us', pt: 'pt_br' };
fs.rmSync(ASSETS, { recursive: true, force: true });

for (const [k, folder] of Object.entries(LANGS)) {
  for (const c of categories) {
    writeJson(path.join(ASSETS, folder, 'categories', c.id + '.json'), {
      name: c[k][0], description: c[k][1], icon: c.icon, sortnum: c.sortnum,
    });
  }
  for (const e of entries) {
    const pages = e.pages.map(p => {
      const out = { type: 'patchouli:' + p.type };
      for (const [key, val] of Object.entries(p)) {
        if (key === 'type') continue;
        out[key] = (val && typeof val === 'object' && 'en' in val) ? val[k] : val;
      }
      return out;
    });
    writeJson(path.join(ASSETS, folder, 'entries', e.category, e.id + '.json'), {
      name: e.name[k], icon: e.icon, category: `${NS}:${e.category}`, sortnum: e.sortnum, pages,
    });
  }
}

// Confere se todos os links $(l:ns:categoria/entrada) apontam para entradas que existem
const ids = new Set(entries.map(e => `${NS}:${e.category}/${e.id}`));
let broken = 0;
for (const e of entries) for (const p of e.pages) for (const v of Object.values(p)) {
  if (!v || typeof v !== 'object' || !('en' in v)) continue;
  for (const s of [v.en, v.pt]) for (const m of s.matchAll(/\$\(l:([^)]+)\)/g)) {
    if (!ids.has(m[1].split('#')[0])) { console.error('Link quebrado em', e.id, '->', m[1]); broken++; }
  }
}
console.log(`${categories.length} categorias, ${entries.length} entradas por idioma, ${broken} links quebrados`);
process.exit(broken ? 1 : 0);
