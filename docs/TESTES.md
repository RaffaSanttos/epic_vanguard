# Testes automatizados (GameTest)

Os testes sobem um servidor Minecraft sem janela, criam uma plataforma de pedra,
colocam guerreiros, zumbis e jogadores falsos, verificam o resultado e desligam.
Não precisa abrir o jogo.

## Rodar

```powershell
$env:JAVA_HOME = "$env:APPDATA\.minecraft\runtime\java-runtime-gamma\windows\java-runtime-gamma"
.\gradlew.bat runGameTestServer
```

No fim do log aparece `All 22 required tests passed :)`, ou a lista dos que falharam
com a mensagem de cada um. O Gradle termina com erro se algum teste falhar.

Para testar sem as dependências opcionais (garante que o mod carrega sozinho):

```powershell
.\gradlew.bat runGameTestServer -PnoEpicFight -PnoPatchouli
```

Na primeira vez o Gradle baixa o Epic Fight pelo CurseMaven (não precisa mais de jar em `libs/`).

## O que é testado

| Arquivo | Testes |
|---|---|
| `CombatTests` | Afiação aumenta o dano; arma gasta durabilidade; arma quebrada sai do inventário; Aspecto Flamejante põe fogo |
| `PersistenceTests` | Rename com o guerreiro descarregado sobrevive ao recarregar; cópia antiga é descartada; morto não volta; dispensado enquanto descarregado é liberado |
| `SecurityTests` | Pacotes de quem não é dono são recusados; só o dono mantém o inventário aberto; criativo sem OP não renomeia guerreiro alheio; cobrança de moedas; resgate de prisioneiro com comida |
| `FixesTests` | Save antigo não duplica equipamento; armadura gasta ao levar dano; criativo não mexe nas moedas dos outros no Posto; migração do save `grimal_companions` |
| `FeatureTests` | Talentos aplicam e persistem no NBT; requisitos de nível/pré-requisito/pontos; Segundo Fôlego cancela a morte sem esvaziar a mochila; arma quebrada trocada pela melhor da mochila; aviso de durabilidade baixa uma vez; reparo em descanso com material da mochila; flecha do guerreiro não fere aliado; Arqueiro começa com arco e flechas e troca para a espada sem flechas; aura do Porta-estandarte só para aliados; montaria só no cavalo domado e selado do dono |
| `LogicTests` | Curva de XP; subir de nível e limite 20; palavras do `/friendlyfire` em inglês e português; chaves iguais em `en_us`/`pt_br`; livro e receita só com o Patchouli |

## Arquivos

- `src/gametest/java/.../gametest/` — os testes e `TestUtil` (jogador falso, recrutar, salvar/carregar NBT).
- `src/gametest/resources/data/epicvanguard/structures/gametest/platform.nbt` — plataforma 9x5x9.
  Para recriar: `node tools/gametest/make_platform.js`.
- Os testes ficam fora do jar publicado (`gradlew build` só empacota `src/main`).

## O que ainda precisa de teste manual

### Checklist dos recursos novos (porte de 23/11/2026)

- [ ] Config `config/epicvanguard-common.toml`: seções novas aparecem (companions, combat, ranged, equipment, specializations, mounts, spawning) e mudar valores tem efeito.
- [ ] Aba **Talentos** na árvore: pontos (1 a cada 2 níveis), clique desbloqueia, tooltip com requisito; Vitalidade aumenta a vida.
- [ ] Não existe mais guerreiro sem classe: todo guerreiro nasce com uma das 5 classes; um Guerreiro base antigo (save antigo, ovo antigo, `/summon`) vira uma classe sorteada ao carregar, mantendo nível, nome e itens. Não há mais escolha de classe no nível 5.
- [ ] Mochila, Habilidades e Talentos: as 3 abas aparecem nas duas telas, no mesmo lugar, e a tela não muda de tamanho ao trocar de aba.
- [ ] Logo depois de contratar, o botão de monstros (⚔) aparece **desligado** e continua desligado ao mexer nos outros botões.
- [ ] Com a coleta (✋) ligada, ele pega também os itens que você joga.
- [ ] Com a coleta (✋) desligada, ele não pega nada, nem o que você joga. O botão só alterna cinza/verde.
- [ ] Corneta com um companheiro de Guarda: mire num monstro longe do posto; ele vai até lá, mata e volta para o posto.
- [ ] Corneta com um companheiro na frente da mira: a ordem de ataque vai para o monstro atrás dele.
- [ ] Corneta mirando num aldeão, golem, suporte de armadura ou pet de outro jogador: não vira alvo.
- [ ] Arqueiro com escudo na mochila: não equipa o escudo.
- [ ] Contratar com as moedas só na mão secundária.
- [ ] Guarda: com um zumbi a ~6 blocos do posto ele vai direto atacar, sem parar no meio; com um zumbi a ~20 blocos ele não sai do posto.
- [ ] Auto-equipar com a mão principal vazia e armadura na mochila: a armadura não vai para a mão.
- [ ] Livro: páginas do Arqueiro, Porta-estandarte, Montarias, Desgaste e Reparo, Arco e Besta e Descanso aparecem e o texto cabe na página.
- [ ] Tecla J abre a Minha Companhia com todos os companheiros (inclusive um deixado de Guarda longe, que aparece como "longe").
- [ ] Na Minha Companhia: Chamar, Chamar todos, trocar postura (carregado) e o aviso ao tentar trocar a postura de um companheiro longe.
- [ ] Corneta agachado: troca a formação (mensagem na barra). Coluna, Linha e Círculo: andar e virar; eles acompanham sem ficar presos.
- [ ] Em formação, entrar numa luta: eles saem da formação para lutar e voltam depois.
- [ ] Arqueiro (ovo no criativo): começa com arco e flechas, atira de longe, saca a espada sem flechas, grito "Chuva de Flechas".
- [ ] Porta-estandarte: estandarte na mão secundária, aura de Resistência em você e aliados, grito "Reagrupar".
- [ ] Qualquer guerreiro com arco/besta + flechas na mochila luta à distância; flechas não acertam você nem aliados.
- [ ] Desgaste: aviso de durabilidade baixa no chat, troca automática quando quebra, reparo sentado na fogueira ou no Posto da Vanguarda com lingotes na mochila.
- [ ] Montarias: em modo Seguir, monte num cavalo; o guerreiro monta num cavalo seu, domado e selado, e desmonta para lutar corpo a corpo.
- [ ] Jade e/ou WTHIT instalados: tooltip com nível, classe, dono, modo e pontos.
- [ ] Posto da Vanguarda: o custo do edital segue `contractCoins`.

Visual das telas e do livro, sensação da IA em combate, animações do Epic Fight e multiplayer real com latência.
