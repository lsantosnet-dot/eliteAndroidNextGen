# Mercador — "O Mercador Cúmplice"

Sucessor de [eliteAndroid](https://github.com/lsantosnet-dot/eliteAndroid): um jogo de comércio e intriga política
em Android nativo (Kotlin), sem combate de tiro. O design completo está em
[`docs/DESIGN-MERCADOR.md`](docs/DESIGN-MERCADOR.md).

**Decisão D1 (design, seção 12):** projeto novo. Reaproveita apenas ideias do original (núcleo em Kotlin puro
testável sem Android SDK, universo por semente, economia por mercadoria). Voo 3D, combate e wireframe ficam no repositório antigo.

## Estrutura

```
core/   Kotlin/JVM puro, testável sem Android SDK
  rng/       SplitMix64: mix64, derive (árvore de sementes por hash), Rng
  universe/  Universe (galáxia → setor → sistema, acesso direto), traços, facções e relações
  universe/Region  região jogável de 50 sistemas, distâncias, vizinhos e conectividade
  world/     Meters/MeterModel (controle, descontentamento, abastecimento) e World (deltas do mundo vivido, preços com volume)
  game/      Save (GameSave, SaveCodec, SaveStore: salvamento só do mundo vivido), Slots (vários jogos salvos), Game (comprar, vender, viajar, livro de contas, empréstimo/frete de socorro), Standing (atenção e reputação por facção), Pursuit (respostas à perseguição), Confrontation (cena em rodadas), Traits (traços e pessoas), Bar (rumores e segredos à venda)
  story/     Storylet/StoryEngine (eventos com condições e lacunas), StoryContent + StoryContentLote2 + StoryContentLote3 (62 storylets), InnerVoice (voz interior)
  economy/   Commodity (12 mercadorias), Laws (ideologia → contrabando) e Market (preço/estoque por sistema e dia)
app/    App Android (Activity + ViewModel que guarda o jogo; reaproveita o código de ui/ sem as classes só de PC). Só entra no build com Android SDK
ui/     Interface Compose (dossiê noir): tema, telas (Mapa, Estação com mercado/dossiê/bar, animação de hiperespaço no salto, Dossiê, Livro, Perseguição, Confronto, História, Relatório), GameController (estado e ações) e AppRoot (navegação); Preview renderiza PNGs sem janela
docs/   Design e protótipo visual (docs/mercador)
```

Ainda falta a edição de nome dos jogos salvos na interface e testar o app num aparelho/emulador (o APK compila, mas nunca foi executado).

## Roteiro (seção 11 do design)

1. ✅ **Dados e gerador** em Kotlin puro, com testes — universo, sistemas, facções, mercadorias, preços por sistema e região de 50 sistemas
2. ✅ Laço de comércio + medidores (controle, descontentamento, abastecimento) — com simulação de 1 ano (risco R2)
3. ✅ Leis, atenção e perseguição por decisões (sem o confronto, que é a etapa 4)
4. ✅ Confronto por rodadas, traços e livro de contas
5. ⬜ Storylets e voz interior
6. ✅ Polimento visual "dossiê noir": tema e as 4 telas das maquetes em Compose, renderizadas de verdade em [`docs/ui`](docs/ui)

## Build

### Android Studio

Abra a pasta do repositório (`File ▸ Open`), deixe o Gradle sincronizar (o Studio cria o `local.properties` com o SDK) e rode a configuração **app** num emulador ou aparelho com Android 8.0+ (API 26). Pela linha de comando: `./gradlew :app:assembleDebug` (APK em `app/build/outputs/apk/debug`). Os jogos ficam em `filesDir/saves` do app.

### Linha de comando

Requisitos: JDK 17+.

```bash
./gradlew :core:test                 # lógica do jogo
./gradlew :ui:test                   # renderiza as 4 telas sem janela e confere que não saem em branco
./gradlew :ui:renderScreens           # gera os PNGs das telas em docs/ui
./gradlew :ui:runApp                 # abre o jogo numa janela (precisa de display); salva sozinho em ~/.mercador/saves (um arquivo por jogo; Dossiê → Jogos salvos)
```

O `:ui` usa Compose Multiplatform (desktop/JVM) e baixa `androidx.*` do Google Maven (`dl.google.com`).

![Mapa](docs/ui/01-mapa.png) ![Estação](docs/ui/02-estacao.png) ![Confronto](docs/ui/03-confronto.png) ![Livro](docs/ui/04-livro.png)
