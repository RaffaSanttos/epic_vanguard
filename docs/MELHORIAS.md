# Lista de melhorias — Epic Vanguard

Levantamento feito em 20/11/2026 (versão 1.0.14, Forge 47.4.0 / MC 1.20.1), lendo o código-fonte.
Complementa a [AUDITORIA.md](AUDITORIA.md): itens que já estão lá aparecem aqui só quando ainda estão pendentes ou quando a correção registrada não funciona.

Nada aqui foi testado em jogo. Prioridades:

- 🔴 **Alta**: bug confirmado ou risco em multiplayer
- 🟠 **Média**: comportamento estranho para o jogador ou mecânica incompleta
- 🟢 **Baixa**: organização, desempenho ou acabamento

---

## 1. Bugs

### 🔴 1.1 Renomear por comando com o guerreiro descarregado não funciona
A auditoria (seção 8) registra isso como corrigido, mas a correção não funciona:

1. `/vanguard renomear` grava o nome novo só em `CompanionInfo.name` ([VanguardCommand.java:585](../src/main/java/com/example/epicvanguard/command/VanguardCommand.java)).
2. Quando o chunk carrega, `readAdditionalSaveData` chama `setWarriorName(nome antigo do NBT)`, que também chama `CompanionSavedData.updateName(...)` ([WarriorCompanionEntity.java:638](../src/main/java/com/example/epicvanguard/entity/WarriorCompanionEntity.java)).
3. Quando `CompanionTeleportHandler.onEntityJoinLevel` compara os nomes, o registro já tem o nome antigo e o rename se perde.

Também por isso, uma **cópia antiga** (geração menor) sobrescreve o nome no registro antes de ser descartada.

**Correção:** separar o "aplicar nome na entidade" do "gravar no registro". Durante `readAdditionalSaveData`, só aplicar na entidade, sem chamar `updateName`. Gravar no registro só em ações do jogador (name tag, pacote de rename, comando).

### 🔴 1.2 Ataque do guerreiro ignora encantamentos e a arma nunca gasta
`doHurtTarget` ([WarriorCompanionEntity.java:1436](../src/main/java/com/example/epicvanguard/entity/WarriorCompanionEntity.java)) só chama `target.hurt(...)`. O que falta:
- Bônus de encantamento (`EnchantmentHelper.getDamageBonus`): Afiação, Julgamento, Ruína dos Artrópodes.
- Aspecto Flamejante e Repulsão (empurrão do golpe).
- `EnchantmentHelper.doPostHurtEffects` / `doPostDamageEffects` (Espinhos, efeitos de mods).
- `getMainHandItem().hurtAndBreak(...)`: a arma tem durabilidade infinita.

**Correção:** seguir a estrutura do `Mob#doHurtTarget` vanilla e aplicar os multiplicadores de especialização em cima dele. Conferir também o que acontece quando o golpe vem pelas animações do Epic Fight.

### 🟠 1.3 Guerreiro recém-recrutado fica em modo Guarda
`PacketRecruitWarrior`, o resgate de prisioneiro e `VillageWarriorSpawner` chamam `setCombatMode(1)` com o comentário "Defensivo". Mas o modo 1 é **Guarda**: o guerreiro fica parado onde foi contratado.
- Decidir o modo padrão ao recrutar (provavelmente 0 = Seguir).
- Trocar os números mágicos 0/1/2 por um `enum CombatMode { FOLLOW, GUARD, STAY }`.
- Atualizar o comentário de `CompanionInfo.combatMode`, que ainda descreve 4 modos antigos.
- Apagar as chaves sem uso `gui.epicvanguard.warrior.mode.aggressive/defensive/guard/stay` dos lang.

### 🟠 1.4 Recrutar por moedas sem checar o estado do guerreiro
`PacketRecruitWarrior` não verifica `isAlive()` nem `isDuelMode()`, e não usa `PacketGuards.nearbyWarrior`.
**Correção:** usar `PacketGuards.nearbyWarrior(...)` e recusar o recrutamento durante um duelo.

### 🟢 1.5 Drops da morte não passam pelo `LivingDropsEvent`
`dropAllInventory` cria os `ItemEntity` direto com `addFreshEntity`. Mods de lápide e de keep-inventory não enxergam esses itens.
**Correção:** chamar `spawnAtLocation(stack)` dentro de `dropCustomDeathLoot`, que já roda com captura de drops. Tirar a segunda chamada de `dropAllInventory()` em `die()`.

---

## 2. Segurança e multiplayer

### 🔴 2.1 Jogador em criativo é tratado como admin nos comandos
`VanguardCommand.hasAdminAccess` aceita `player.isCreative()`. Num servidor com criativo liberado, qualquer um pode trazer, renomear, dispensar ou ir até os companheiros dos outros.
**Correção:** exigir só `hasPermission(2)`, ou criar uma opção de config `allowCreativeAdmin` (padrão `false`).

### 🟠 2.2 Canal de rede aceita qualquer versão
Em `Messages.register`, `clientAcceptedVersions(s -> true)` e `serverAcceptedVersions(s -> true)` deixam versões diferentes do mod se conectarem. O resultado são pacotes com layout diferente e erros estranhos.
**Correção:** `PROTOCOL_VERSION::equals`, e aumentar `PROTOCOL_VERSION` sempre que algum pacote mudar.

### 🟢 2.3 Pacote de abrir GUI manda um `boolean recruited` inútil
`PacketOpenWarriorGUI` já ignora o flag e usa o estado do servidor. Dá para remover o campo e economizar o byte. Isso exige aumentar a versão do protocolo (2.2).

---

## 3. Comportamento da IA e gameplay

### 🟠 3.1 Teleporte do modo Seguir interrompe o combate
Em `tick()`, a partir de 13 blocos do dono o guerreiro limpa o alvo e se teleporta, verificando a cada tick. Isso tira o guerreiro de qualquer perseguição.
**Sugestões:**
- Não teleportar enquanto `isFighting()` e o dono estiver a menos de ~24 blocos.
- Teleportar só depois de N ticks seguidos longe, como o lobo vanilla.
- Unificar essa lógica com `CompanionTeleportHandler.rescueFollowingCompanions`, que faz a mesma coisa por outro caminho.

### 🟠 3.2 Coleta automática pega qualquer item do chão
`pickUpGroundItems` recolhe tudo num raio de 1 bloco, inclusive o loot que o dono quer pegar.
**Sugestões:**
- Botão na GUI para ligar ou desligar a coleta.
- Ignorar itens jogados pelo dono (`ItemEntity#getOwner` / `thrower`).
- Filtrar: só comida, poções e equipamento melhor que o atual.

### 🟠 3.3 Especialização não pode ser escolhida
Nenhum pacote chama `setSpecialization`. A classe vem só do tipo de entidade em que o guerreiro nasceu, e a árvore de habilidades ([WarriorSkillTreeScreen.java](../src/main/java/com/example/epicvanguard/screen/WarriorSkillTreeScreen.java)) só mostra informação.
**Sugestão:** permitir que um Guerreiro base escolha a classe ao chegar num nível (ex.: 5). Para isso:
- um pacote `PacketChooseSpecialization` com `PacketGuards.ownedWarrior`;
- uma troca de entidade (Guerreiro → Berserker/Guardião/Duelista) mantendo UUID, NBT e geração, usando o mesmo caminho do `moveToLevel`.

### 🟢 3.4 Guerreiro equipa melhor equipamento sozinho
Hoje ele só usa o que está nos slots 0–5. Ele poderia trocar sozinho por armadura ou arma melhor que estiver na mochila, comparando armadura/dano. Seria um botão "auto-equipar" ou um comportamento padrão.

### 🟢 3.5 Ordens rápidas sem abrir a GUI
Sugestões:
- Agachar + clicar: alternar Seguir/Parado.
- Item "Corneta de comando": ordenar a todos os companheiros próximos que ataquem o alvo apontado.

### 🟢 3.6 Regeneração natural só considera dano de mob
A regeneração fora de combate usa `getLastHurtByMobTimestamp()`. Dano de queda, fogo e veneno não reiniciam a contagem.
**Sugestão:** guardar o tick do último dano de qualquer fonte em `hurt()`.

---

## 4. Persistência

### 🟠 4.1 `DATA_NAME = "grimal_companions"` (pendente da auditoria)
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
Em `tick()`, `pickUpGroundItems` faz `getEntitiesOfClass(ItemEntity)` todo tick para cada guerreiro recrutado. As checagens de projétil e creeper também varrem a área com frequência.
**Sugestão:** rodar a cada 5–10 ticks, com o `tickCount` deslocado pelo `getId()` para espalhar a carga entre guerreiros.

### 🟢 5.2 `syncEquipmentWithInventory()` todo tick
Chama `setItemSlot` 6 vezes por tick. Daria para marcar o inventário como "sujo" em `setChanged()` e sincronizar só quando mudar.

### 🟢 5.3 Resgate de companheiros a cada segundo por jogador
`rescueFollowingCompanions` roda a cada 20 ticks e varre todos os níveis para cada companheiro. Com poucos companheiros não pesa; com muitos jogadores e companheiros, convém um cache UUID → entidade atualizado no join/leave.

---

## 6. Organização do código

- 🟠 **Dividir `WarriorCompanionEntity`** (~1.930 linhas), como a auditoria sugere:
  - `WarriorPersistence` (save/load/snapshot)
  - `WarriorInteraction` (`mobInteract`, contrato, prisioneiro)
  - `WarriorThreatSensor` (creeper/projéteis)
  - `WarriorProgression` (nível/XP/especialização/grito de guerra)
- 🟢 **Constantes de balanceamento num lugar só.** Custos de recrutamento, curva de XP, cooldowns, raios e porcentagens estão espalhados como números mágicos. Melhor ainda: um `ForgeConfigSpec` (`epicvanguard-common.toml`), para donos de servidor ajustarem sem recompilar.
- 🟢 **Imports duplicados** em `WarriorCompanionEntity` (`ShieldItem` duas vezes) e indentação quebrada em `isSpellOrMagicDamage`.
- 🟢 **Código morto:**
  - `NEXT_TAVERN_SPAWN` / `MIN_TAVERN_INTERVAL` / `checkTavernRespawn` comentado em `VillageWarriorSpawner`
  - `findOrLoadCompanion`, que hoje é igual a `findLoadedCompanion`
  - `teleportFollowingCompanions` (alias)
  - `consumeHealingItem(ItemStack, int)`, que ignora o `ItemStack`
- 🟢 **Pacote `com.example.epicvanguard`** → trocar por um domínio seu antes de publicar (pendente da auditoria).
- 🟢 **Registro de pacotes:** `Messages` mistura imports e nomes totalmente qualificados. Padronizar com imports.

---

## 7. Build e projeto

- 🟠 **Jar do Epic Fight versionado em `libs/`**: trocar por CurseMaven ou Modrinth Maven (pendente da auditoria; ver a licença do Epic Fight).
- 🟢 `build_jar.bat` anuncia `build\libs\epicvanguard-1.0.0.jar`, mas o jar real é `EpicVanguard <versão> [1.20.1].jar`.
- 🟢 Apagar `scratch_test_nbt.java` da raiz, ou mover para `tools/`.
- 🟢 Mudar `forge.logging.console.level` de `debug` para `info` nas runs, para deixar o log legível.
- 🟢 CI no GitHub Actions rodando `./gradlew build` a cada push.

---

## 8. Testes e documentação

- 🟠 **GameTests** (plano da auditoria), começando pelos bugs desta lista:
  - rename com o guerreiro descarregado mantém o nome novo (1.1);
  - golpe com espada encantada causa mais dano e gasta durabilidade (1.2);
  - jogador que não é dono não comanda nem abre o inventário;
  - cópia de geração menor é descartada; UUID em `Retired` não reaparece.
- 🟢 **Testes unitários de lógica pura:** `getXpForNextLevel`, `rollRandomTier`, `FriendlyFireMode.fromString`.
- 🟢 **README.md:** o que o mod faz, como recrutar (moedas, duelo, prisioneiro), modos de combate, classes, comandos `/vanguard` e `/fogoamigo`, dependências, como compilar.
- 🟢 **Revisar as traduções em inglês dentro do jogo** (pendente da auditoria).

---

## Ordem sugerida

1. 1.1 (rename), 1.2 (dano/durabilidade), 2.1 (admin em criativo), 2.2 (versão de rede)
2. 1.3 (modo padrão ao recrutar + enum de modos), 1.4, 3.1 (teleporte em combate)
3. 4.1 (migração do `DATA_NAME`) e config com `ForgeConfigSpec`
4. Divisão da `WarriorCompanionEntity`, GameTests e README
5. Novas mecânicas: escolha de especialização (3.3), auto-equipar (3.4), ordens rápidas (3.5)
