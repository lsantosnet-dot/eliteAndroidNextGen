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
  game/      Game: comprar, vender, reabastecer, viajar e o livro de contas
  economy/   Commodity (12 mercadorias), Laws (ideologia → contrabando) e Market (preço/estoque por sistema e dia)
docs/   Design e protótipo visual (docs/mercador)
```

`:app` (Jetpack Compose + Room para os deltas do mundo vivido) entra na etapa 2.

## Roteiro (seção 11 do design)

1. ✅ **Dados e gerador** em Kotlin puro, com testes — universo, sistemas, facções, mercadorias, preços por sistema e região de 50 sistemas
2. ✅ Laço de comércio + medidores (controle, descontentamento, abastecimento) — com simulação de 1 ano (risco R2)
3. ⬜ Leis, atenção e perseguição por decisões
4. ⬜ Confronto por rodadas, traços e livro de contas
5. ⬜ Storylets e voz interior
6. ⬜ Polimento visual "dossiê noir"

## Build

Requisitos: JDK 17+.

```bash
./gradlew :core:test
```
