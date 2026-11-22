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

## O que é testado

| Arquivo | Testes |
|---|---|
| `CombatTests` | Afiação aumenta o dano; arma gasta durabilidade; arma quebrada sai do inventário; Aspecto Flamejante põe fogo |
| `PersistenceTests` | Rename com o guerreiro descarregado sobrevive ao recarregar; cópia antiga é descartada; morto não volta; dispensado enquanto descarregado é liberado |
| `SecurityTests` | Pacotes de quem não é dono são recusados; só o dono mantém o inventário aberto; criativo sem OP não renomeia guerreiro alheio; cobrança de moedas; resgate de prisioneiro com comida |
| `FixesTests` | Save antigo não duplica equipamento; armadura gasta ao levar dano; criativo não mexe nas moedas dos outros no Posto; migração do save `grimal_companions` |
| `LogicTests` | Curva de XP; subir de nível e limite 20; palavras do `/friendlyfire` em inglês e português; chaves iguais em `en_us`/`pt_br`; livro e receita só com o Patchouli |

## Arquivos

- `src/gametest/java/.../gametest/` — os testes e `TestUtil` (jogador falso, recrutar, salvar/carregar NBT).
- `src/gametest/resources/data/epicvanguard/structures/gametest/platform.nbt` — plataforma 9x5x9.
  Para recriar: `node tools/gametest/make_platform.js`.
- Os testes ficam fora do jar publicado (`gradlew build` só empacota `src/main`).

## O que ainda precisa de teste manual

Visual das telas e do livro, sensação da IA em combate, animações do Epic Fight e multiplayer real com latência.
