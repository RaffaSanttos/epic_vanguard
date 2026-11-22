# Arquitetura — Epic Vanguard

Mapa do código para quem for mexer no mod. Pacote base: `io.github.raffasanttos.epicvanguard` (em `src/main/java`).

## Entidade do guerreiro

`entity/WarriorCompanionEntity.java` é o mercenário. Guarda o estado (dados sincronizados, NBT, inventário) e o `tick()`. A lógica maior fica em classes estáticas em `entity/util`, que recebem o guerreiro como parâmetro:

| Classe | O que faz |
|---|---|
| `WarriorInteraction` | Clique do jogador: resgate de prisioneiro, tela de contrato, name tag, abrir a tela do dono. Coleta de itens do chão (botão ✋, ignora o que o dono jogou). |
| `WarriorTeleport` | Teleporte seguro (chão firme, sem lava, sem sufocar) e a regra do modo Seguir: 13 blocos + 2 s fora de combate, ou 32 blocos sempre (valores da config). |
| `WarriorThreatSensor` | Creeper prestes a explodir e projéteis vindo na direção do guerreiro ou do dono. Guarda a última varredura num cache por guerreiro (projéteis a cada 2 ticks, creepers a cada 5). |
| `WarriorWarcry` | Grito de guerra de cada classe (efeitos, partículas, sons). |
| `WarriorProgression` | Escolha de classe no nível 5 (recria a entidade com o tipo da classe). |
| `WarriorCombatHelper` | Fogo amigo, esquiva do Duelista, bloqueio com escudo. |
| `WarriorHealingHelper` | Escolher e consumir comida e poções. |

Classes: `BerserkerCompanionEntity`, `GuardianCompanionEntity` e `DuelistCompanionEntity` só fixam a especialização. A classe é o tipo de entidade porque o Epic Fight registra atributos (impacto, peso, stamina) por tipo.

Posturas: `MODE_FOLLOW` (0), `MODE_GUARD` (1), `MODE_STAY` (2). Quem é recrutado passa por `applyRecruitDefaults()`: Seguir e pacífico.

IA: `entity/ai` tem os goals (combate tático, seguir, guarda, comer, fugir, gritos, fogueira, patrulha).

## Registro e persistência

- `CompanionSavedData` (arquivo `epicvanguard_companions.dat` no overworld): registro de todos os companheiros recrutados, com snapshot do NBT, geração (contra cópias antigas), `Retired` (mortos) e `Dismissed` (dispensados descarregados). Migra o antigo `grimal_companions.dat` sozinho.
  - Só marca o arquivo para salvar quando algo mudou.
  - `Retired`/`Dismissed` guardam quando o UUID entrou; `pruneForgotten` (a cada minuto, pelo `CompanionTeleportHandler`) apaga os mais velhos que `registryMemoryDays`.
- `event/CompanionTeleportHandler`: resgata companheiros no modo Seguir que ficaram em outra dimensão, longe demais ou em chunk descarregado; descarta cópias antigas ao carregar.
- `VillageWarriorSavedData`: estruturas que já geraram guerreiros (casa, prisão do posto de saqueadores), por dimensão + chunk.

## Rede

`networking/Messages.java` registra os pacotes. Protocolo atual: **"3"** (cliente e servidor precisam da mesma versão; aumente ao mudar qualquer pacote).

| Pacote | Quem pode | O que faz |
|---|---|---|
| `PacketWarriorCombatMode` | dono, perto | troca a postura |
| `PacketWarriorTactics` | dono, perto | Espada (monstros), Carne (animais), Mão (coleta) |
| `PacketWarriorCancelAction` | dono, perto | encerra a luta |
| `PacketOpenWarriorGUI` | perto (inventário só o dono) | abre a tela |
| `PacketRecruitWarrior` | perto | contrata com moedas |
| `PacketRenameWarrior` | dono | renomeia |
| `PacketChooseSpecialization` | dono, perto | escolhe a classe (nível 5) |
| `PacketPostVanguardContract` | perto do bloco | edital do Posto da Vanguarda |

`PacketGuards` concentra as checagens de "perto" (10 blocos) e "dono".

## Telas

`screen/`: `WarriorCompanionScreen` (mochila e táticas), `WarriorSkillTreeScreen` (talentos e escolha de classe), `HonorContractScreen` (contratar), `VanguardPointScreen` (edital).

## Posto da Vanguarda

`block/VanguardPointBlock` + `block/entity/VanguardPointBlockEntity`: slot de moedas (do depositante, sem funis), edital de 7 dias que traz um mercenário. Só quem depositou, quem pagou ou um operador quebra o bloco.

## Config

`config/EpicVanguardConfig.java` define `config/epicvanguard-common.toml` (ForgeConfigSpec COMMON, registrado no construtor do mod). Os getters devolvem o padrão se a config ainda não carregou.

| Seção | Chave | Padrão | Onde é usada |
|---|---|---|---|
| progression | `recruitCostMultiplier` | 1.0 | custo sorteado em `WarriorCompanionEntity` |
| progression | `xpMultiplier` | 1.0 | `VanguardCombatEvents` |
| progression | `playerKillXpRadius` | 24 | XP dividido quando o dono mata |
| progression | `warcryCooldownSeconds` | 50 | `WarriorWarcry` |
| vanguardPoint | `contractDays` | 7 | novos editais em `VanguardPointBlockEntity` |
| follow | `followTeleportDistance` | 13 | `WarriorTeleport` |
| follow | `combatLeashDistance` | 32 | `WarriorTeleport`, `CompanionTeleportHandler` |
| follow | `followTeleportDelayTicks` | 40 | `WarriorTeleport` |
| registry | `registryMemoryDays` | 100 | `CompanionSavedData.pruneForgotten` |

## Integrações opcionais

- **Epic Fight** (`compat/epicfight`): só é registrado se o mod estiver instalado. No build vem pelo CurseMaven (`curse.maven:epic-fight-mod-405076:8049910`).
- **Patchouli**: livro em `src/main/resources/.../patchouli_books/guide/{en_us,pt_br}`, gerado por `node tools/patchouli/build_book.js`. Não edite os JSON à mão; edite o script e rode de novo.

## Traduções

`assets/epicvanguard/lang/en_us.json` e `pt_br.json` têm as mesmas chaves (há um GameTest que confere).

## Testes

GameTests em `src/gametest` (ver [TESTES.md](TESTES.md)). Ficam fora do jar publicado.
