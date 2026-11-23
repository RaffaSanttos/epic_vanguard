# Epic Vanguard: planejamento das próximas melhorias

Este documento lista o que **ainda não foi feito**. O que já entrou está em `AUDITORIA.md`:
seção 9 (config, combate à distância, árvore de talentos, desgaste, Jade/WTHIT) e seção 10
(testes automáticos, aviso de durabilidade e reparo, Arqueiro, Porta-estandarte e montarias).
Os itens 1, 4, 7 e 8 abaixo ficam como registro do plano original.

Cada item traz o objetivo, como implementar (com os arquivos que serão tocados), o que configurar
e o tamanho estimado: **P** (pequeno, até 1 dia), **M** (médio, 2 a 4 dias), **G** (grande, 1 semana ou mais).

---

## Prioridade sugerida

| # | Item | Tamanho | Por que nessa ordem |
|---|------|---------|---------------------|
| 1 | Testes automáticos (GameTest) | M | Protege tudo que vem depois; já estava planejado | **Feito (seção 10 da auditoria)**
| 2 | Ordens em grupo por tecla | M | Muito pedido em mods de companheiros; usa só o que já existe |
| 3 | Respec (redistribuir talentos) | P | Complementa a árvore recém-criada |
| 4 | Aviso de durabilidade e reparo | P | Complementa o desgaste recém-criado | **Feito (seção 10 da auditoria)**
| 5 | Morte com resgate (insígnia) | M | Diminui a frustração de perder um guerreiro de nível alto |
| 6 | Salário e moral | M | Dá uso contínuo às moedas e à taverna |
| 7 | Novas especializações (Arqueiro, Porta-estandarte) | G | Usa o combate à distância e os talentos | **Feito (seção 10 da auditoria)**
| 8 | Montarias | G | Mexe em IA, navegação e teleporte | **Feito (seção 10 da auditoria)**
| 9 | Mais estruturas por bioma | M | Conteúdo; depende de datapack/NBT |
| 10 | Epic Fight mais a fundo | G | Depende da API do Epic Fight (opcional) |

---

## 1. Testes automáticos (GameTest + runServer)

- **Objetivo:** pegar regressões sem abrir o jogo.
- **Como:** `src/test` não serve para lógica com mundo; usar `@GameTestHolder` do Forge.
  Testes iniciais: recrutar guerreiro, salvar/carregar NBT com talentos (`Perks`), quebra de item por
  desgaste, Segundo Fôlego cancela a morte e não descarta o inventário, flecha do guerreiro não fere o dono,
  migração `grimal_companions` para `epicvanguard_companions`.
- **Build:** tarefa `runGameTestServer` do ForgeGradle e um passo de CI.
- **Atenção:** `runServer` precisa aceitar a EULA (`run/eula.txt`), o que só o dono do projeto deve fazer.
- **Tamanho:** M.

## 2. Ordens em grupo por tecla

- **Objetivo:** dar ordens a todos os guerreiros próximos (ou a um grupo numerado) sem abrir a GUI de cada um.
- **Como:**
  - `client/KeyBindings` com `RegisterKeyMappingsEvent`: teclas para "Seguir", "Guardar aqui", "Parado",
    "Atacar alvo na mira" e "Recuar"; categoria `key.categories.epicvanguard`.
  - Pacote `PacketSquadOrder(order, targetEntityId, BlockPos)`: o servidor valida o dono, o raio (config) e aplica
    `setCombatMode`/`setGuardPos`/`setTarget` usando os mesmos métodos do `PacketWarriorCombatMode`.
  - Grupos: campo `SquadId` (0 a 3) no NBT e na GUI; a tecla com Shift alterna o grupo ativo.
  - Mensagem curta na action bar e uma fala do `WarriorSpeechSystem` (gatilho novo `ORDER_RECEIVED`).
- **Config:** `orders.radius` (padrão 32), `orders.requireLineOfSight`.
- **Tamanho:** M.

## 3. Respec (redistribuir talentos)

- **Como:** botão "Redistribuir" na aba Talentos; pacote `PacketResetPerks`; custo em peças de ouro do inventário
  do jogador (config `perks.respecCost`, padrão 10 × nível / 5). Zera `Perks` e recalcula atributos
  (vida atual limitada à nova vida máxima).
- **Alternativa:** item consumível "Tomo do Esquecimento" (receita com livro + ouro).
- **Tamanho:** P.

## 4. Aviso de durabilidade e reparo

- **Aviso:** quando um item equipado passar de 90% de desgaste, fala do guerreiro (gatilho `GEAR_WORN`)
  e ícone vermelho no slot da GUI; evento único por item (marca no NBT do stack).
- **Reparo:**
  - Na taverna (Ponto Vanguarda): o guerreiro gasta materiais da mochila (lingote, couro, tábuas) e repara
    25% por material, igual à bigorna, sem custo de XP.
  - Talento futuro "Ferreiro de Campo": repara 1 ponto por minuto fora de combate.
- **Troca automática:** se a arma quebrar e houver outra na mochila, equipar a melhor (reusar a lógica de
  `WarriorInventory` que já compara armas).
- **Tamanho:** P.

## 5. Morte com resgate (insígnia + Ponto Vanguarda)

- **Objetivo:** perder um guerreiro deixa de ser definitivo, mas tem custo.
- **Como:**
  - Ao morrer, o guerreiro deixa uma **Insígnia do Guerreiro** (item novo) com NBT completo
    (`saveWithoutId`, nome, nível, talentos, inventário opcional conforme config). O inventário continua
    caindo no chão por padrão.
  - No Ponto Vanguarda: colocar a insígnia + moedas (config `revive.cost`, padrão 15 + 2 × nível)
    inicia um "resgate" de 1 dia de jogo (reusar o temporizador do contrato).
  - Ao terminar, o guerreiro volta com o NBT salvo e perde parte do XP do nível (config `revive.xpPenalty`).
  - `CompanionSavedData`: novo estado `Fallen` (diferente de `Retired`), para `/vanguard list` mostrar
    "Caído: resgatável".
- **Riscos:** duplicação (insígnia copiada no criativo): guardar o UUID e recusar se o guerreiro já existir.
- **Tamanho:** M.

## 6. Salário e moral

- **Objetivo:** manter um exército grande ter custo e dar personalidade.
- **Moral (0 a 100, sincronizada):**
  - Sobe com vitórias, descanso em fogueira/taverna, comida boa, falas do dono (interação).
  - Cai com fome, ferimentos graves, aliado morto por perto, salário atrasado, fogo amigo.
  - Efeitos: moral alta dá +5% de dano e falas positivas; moral baixa reduz velocidade de ataque e,
    abaixo de 10, o guerreiro pode desertar (vira não recrutado e vai embora) se a config permitir.
- **Salário:** a cada N dias (config `wages.intervalDays`, padrão 7) o guerreiro pede
  `wages.coinsPerLevel` × nível peças de ouro. Pagamento pela GUI (aba nova) ou automático a partir de um baú
  "Cofre da Vanguarda" ligado ao Ponto Vanguarda.
- **Config:** tudo desligável (`wages.enabled=false` por padrão para não mudar mundos existentes).
- **Tamanho:** M.

## 7. Novas especializações

### Arqueiro
- Entidade `ArcherCompanionEntity` (como Berserker/Guardião/Duelista), `SPEC_ARCHER = 4`.
- Passivas: +30% de dano à distância, puxada 20% mais rápida, prefere distância maior (8 a 18 blocos),
  recua mais. Usa o `WarriorRangedCombatGoal` com parâmetros por especialização.
- Grito: "Chuva de Flechas" (flechas caem numa área 5×5 em volta do alvo).
- Talentos de especialização: SPEC_T1 "Flecha Perfurante" (Piercing I grátis), SPEC_T2 "Tiro Duplo".
- Nova skin e texturas; entrada nos lang de talentos (`epicvanguard.perk.spec_t1.archer.*`).

### Porta-estandarte
- `SPEC_BANNER = 5`. Carrega um estandarte (estandarte vanilla no slot da mão secundária, renderizado nas costas).
- Aura: aliados em 8 blocos ganham Resistência I e regeneração lenta; não causa muito dano.
- Grito: "Reagrupar" (puxa aliados feridos para perto e dá Absorção).
- Talentos: alcance da aura, aura de velocidade.

- **Arquivos:** `ModEntityTypes`, `WarriorCompanionEntity.SPEC_*`, `getFormattedSpecializationTitle`, a promoção
  de especialização (GUI e pacote), `WarriorPerk.keyBase` (novos ids), renderer (estandarte nas costas), lang.
- **Tamanho:** G.

## 8. Montarias

- **Objetivo:** guerreiros acompanham o dono a cavalo.
- **Como:**
  - Se o dono montar, guerreiros com um cavalo domado e selado por perto (ou no "estábulo" do guerreiro,
    referência por UUID no NBT) montam também.
  - Goal novo `MountUpGoal`: procura o cavalo, anda até ele e chama `startRiding`.
  - Combate montado: só ataque corpo a corpo com alcance maior, ou arco para o Arqueiro.
  - Teleporte: `CompanionTeleportHandler` precisa levar a montaria junto (ou desmontar antes).
- **Riscos:** navegação de cavalo controlado por mob é instável; testar bastante em terreno acidentado.
- **Tamanho:** G.

## 9. Mais estruturas por bioma

- **Hoje:** casa do guerreiro (planície) e prisioneiros em postos de saqueadores.
- **Novas:**
  - Acampamento de mercenários (taiga/floresta): 2 ou 3 guerreiros não recrutados, fogueira e Ponto Vanguarda.
  - Torre de vigia no deserto (Duelista).
  - Fortim na neve (Guardião).
  - Ruína no pântano com prisioneiro de bruxa.
- **Como:** estruturas jigsaw em `data/epicvanguard/worldgen/structure` + `structure_set` + tag de bioma
  `has_structure/*`; o `VillageWarriorSpawner` ganha uma entrada por estrutura (ou trocar por um spawn
  dentro do próprio NBT da estrutura).
- **Config:** uma chave por estrutura em `spawning` (as duas atuais já existem: `outpostPrisoners`, `warriorHouseSpawns`).
- **Tamanho:** M (sem contar o tempo de construir as estruturas).

## 10. Epic Fight mais a fundo

- **Hoje:** o guerreiro usa o "patch" de humanoide do Epic Fight para animações e usa armas do Epic Fight.
- **Próximo passo:**
  - Movesets por especialização (Berserker com combos pesados, Duelista com esquiva animada do Epic Fight
    no lugar do passo tático).
  - Usar a stamina do Epic Fight no lugar da stamina interna quando ele estiver instalado.
  - Habilidades de arma (skills) do Epic Fight disparadas pelo grito de guerra.
- **Regra:** todo acesso continua atrás de `EpicVanguardMod.isEpicFightLoaded()` e dentro de `compat/epicfight`.
- **Tamanho:** G.

---

## Outras ideias menores

- **HUD do esquadrão:** barra de vida compacta dos guerreiros no canto da tela (a textura `warrior_hud.png` já existe).
- **Comandos de admin:** `/vanguard perks <guerreiro> reset|give`, `/vanguard level <guerreiro> <n>`.
- **Integração com The One Probe:** mesmas linhas de `WarriorTooltipLines`.
- **Compatibilidade com JEI:** página explicando contrato, insígnia e talentos.
- **Acessibilidade:** opção para desligar as falas no chat (config de cliente) ou mandá-las para a action bar.
- **Balanceamento:** planilha com vida/dano/armadura por nível e especialização, para revisar depois dos talentos.
