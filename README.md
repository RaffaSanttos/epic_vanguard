# Epic Vanguard

Mercenary companions for **Minecraft 1.20.1 (Forge 47.x)**. Hire warriors with gold coins, gear them up and lead them into battle. They level up, pick a class with its own war cry, eat and drink potions on their own and follow you through teleports and dimensions.

*Português mais abaixo.*

## Features

- **Hiring:** find mercenaries in warrior houses inside villages, rescue prisoners from pillager outposts, or post a request at a **Vanguard Point** block and a company marches to your base.
- **Gold Coins** are the currency. Mercenaries cost more the better their starting gear (Recruit, Veteran, Elite).
- **Stances:** Follow, Guard (protects an area) and Stay. New recruits start in Follow and peaceful; turn monster hunting (sword) and food hunting (meat) on in their screen. Sneak + right-click with an empty hand cycles Follow → Guard → Stay.
- **Gear:** they can pick up items (✋ button: off by default / useful / everything; what you drop for them is always picked up), put on better armor and weapons from their backpack, warn you when gear is about to break and repair it at a campfire.
- **Ranged combat and mounts:** companions with a bow fight at range, and in Follow they ride your horses with you.
- **Vanguard War Horn:** order every companion nearby to attack what you aim at, or to stop and regroup.
- **Classes:** Berserker, Guardian, Duelist, Archer and Standard-bearer, each with its own skills and war cry. Every companion has a class from the start; talent points (1 every 2 levels) unlock talents.
- **Survival:** companions do not regenerate on their own (configurable); they eat food and drink healing potions from their backpack, retreat when badly hurt and rest by campfires.
- **Protection:** they block arrows, dodge creepers and step in when you are about to die.
- **Guide book** in English and Portuguese (needs Patchouli, optional).
- **Epic Fight** (optional): companions fight with its animations.

## Commands

| Command | What it does |
|---|---|
| `/companions` or `/vanguard` | List your companions with clickable actions |
| `/companions call` | Teleport all your companions to you |
| `/companions bring <target>` / `goto <target>` | Bring one to you / go to it |
| `/companions rename <target> <name>` | Rename |
| `/companions dismiss <target>` | Release a companion |
| `/companions clean` | Remove records of companions that no longer exist |
| `/friendlyfire [off\|melee\|spells\|all]` | Show or (operators) set friendly fire for the server |

`<target>` is the list number (`#1`), the name or the UUID. Portuguese aliases work too (`/vanguard chamar`, `/fogoamigo`). Operators can manage other players' companions.

## Server config

`config/epicvanguard-common.toml` (created on first launch):

| Key | Default | Meaning |
|---|---|---|
| `progression.recruitCostMultiplier` | 1.0 | Multiplier for hiring cost |
| `progression.xpMultiplier` | 1.0 | Multiplier for XP from kills and assists |
| `progression.playerKillXpRadius` | 24 | Companions this close share XP when the owner kills a monster |
| `progression.warcryCooldownSeconds` | 50 | War cry cooldown |
| `vanguardPoint.contractDays` | 7 | In-game days until a requested company arrives |
| `follow.followTeleportDistance` | 13 | Blocks behind (out of combat) before teleporting to the owner |
| `follow.combatLeashDistance` | 32 | Blocks away at which they teleport even while fighting |
| `follow.followTeleportDelayTicks` | 40 | Ticks they must stay behind first |
| `registry.registryMemoryDays` | 100 | In-game days the save remembers dead/dismissed companions |
| `companions.naturalRegen` | false | Slow health regeneration out of combat |
| `companions.regenIntervalTicks` | 60 | Ticks per point of natural regeneration |

The file has more sections (shield and dodge chances, ranged combat, equipment wear, aura, mounts, spawns, contract cost); each key has a comment.

## Requirements

- Minecraft 1.20.1, Forge 47.x
- Optional: [Epic Fight](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod) 20.x, [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli) (guide book)
- Client and server must run the same Epic Vanguard version.

## Building

Java 17 is required. Epic Fight is downloaded through CurseMaven, so no extra files are needed.

```
./gradlew build
```

The jar goes to `build/libs/EpicVanguard <version> [1.20.1].jar`. On Windows, `build_jar.bat` does the same and uses the Java bundled with the Minecraft launcher if `JAVA_HOME` is not set.

- `./gradlew runClient` starts a dev client (`-PnoEpicFight` / `-PnoPatchouli` run without them).
- `./gradlew runGameTestServer` runs the automated tests in `src/gametest` (see [docs/TESTES.md](docs/TESTES.md)).
- The guide book is generated: edit `tools/patchouli/build_book.js` and run `node tools/patchouli/build_book.js`.

Developer docs (Portuguese): [code map](docs/ARQUITETURA.md), [improvement list](docs/MELHORIAS.md), [changelog](CHANGELOG.md).

## License

All Rights Reserved.

---

# Epic Vanguard (Português)

Companheiros mercenários para **Minecraft 1.20.1 (Forge 47.x)**. Contrate guerreiros com moedas de ouro, equipe e leve para a batalha. Eles sobem de nível, escolhem uma classe com grito de guerra próprio, comem e bebem poções sozinhos e te seguem por teleportes e dimensões.

## O que tem

- **Contratação:** mercenários nas casas de guerreiro das vilas, prisioneiros nos postos de saqueadores para resgatar, ou um edital no bloco **Vanguard Point** (Posto da Vanguarda) para uma companhia marchar até a sua base.
- **Moedas de Ouro** são a moeda. Quanto melhor o equipamento inicial (Recruta, Veterano, Elite), mais caro.
- **Posturas:** Seguir, Guarda (protege uma área) e Parado. Recrutas novos começam em Seguir e pacíficos; ligue a caça a monstros (espada) e a animais (carne) na tela deles. Agachado + botão direito de mão vazia alterna Seguir → Guarda → Parado.
- **Equipamento:** podem pegar itens do chão (botão ✋: desligada por padrão / úteis / tudo; o que você joga para eles é sempre pego), vestem sozinhos armadura e armas melhores da mochila, avisam quando algo vai quebrar e consertam na fogueira.
- **Combate à distância e montarias:** quem tem arco luta de longe, e no modo Seguir eles montam nos seus cavalos com você.
- **Corneta da Vanguarda:** manda todos os companheiros por perto atacarem o que você mira, ou pararem e se reagruparem.
- **Classes:** Berserker, Guardião, Duelista, Arqueiro e Porta-estandarte, cada uma com habilidades e grito de guerra. Todo companheiro já nasce com classe; pontos de talento (1 a cada 2 níveis) liberam talentos.
- **Sobrevivência:** não se regeneram sozinhos (configurável); comem e bebem poções da mochila, recuam quando muito feridos e descansam em fogueiras.
- **Proteção:** bloqueiam flechas, fogem de creepers e intervêm quando você está para morrer.
- **Livro guia** em inglês e português (precisa do Patchouli, opcional).
- **Epic Fight** (opcional): os companheiros lutam com as animações dele.

## Comandos

`/vanguard` (ou `/companions`) lista seus companheiros com botões. Subcomandos: `chamar`, `trazer <alvo>`, `ir <alvo>`, `renomear <alvo> <nome>`, `dispensar <alvo>`, `limpar`. `/fogoamigo [livre|fisico|magias|total]` mostra ou (operadores) muda o fogo amigo. Os nomes em inglês também funcionam.

## Config do servidor

`config/epicvanguard-common.toml`: custo de contratação, XP, recarga do grito, dias do edital, distâncias do modo Seguir, memória do registro, regeneração natural (desligada por padrão) e mais seções de combate, desgaste, montarias e spawns. A tabela acima (em inglês) lista as chaves e os padrões.

## Compilar

Precisa de Java 17. O Epic Fight vem pelo CurseMaven. `./gradlew build` (ou `build_jar.bat` no Windows) gera `build/libs/EpicVanguard <versão> [1.20.1].jar`. Documentação para quem mexe no código: [docs/ARQUITETURA.md](docs/ARQUITETURA.md).
