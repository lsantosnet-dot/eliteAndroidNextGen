package com.mercador.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.mercador.core.economy.Commodity
import com.mercador.core.game.*
import com.mercador.core.story.InnerVoice
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.Side
import com.mercador.core.world.World
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/** Renderiza uma tela para PNG sem janela nem dispositivo: o que a documentação e os testes usam. */
object Preview {
    const val WIDTH_DP = 360
    const val HEIGHT_DP = 740
    const val DENSITY = 2f

    @OptIn(ExperimentalComposeUiApi::class)
    fun png(content: @Composable () -> Unit): ByteArray {
        val scene = ImageComposeScene(
            width = (WIDTH_DP * DENSITY).toInt(), height = (HEIGHT_DP * DENSITY).toInt(),
            density = Density(DENSITY), content = content,
        )
        try {
            scene.render(0L)
            return scene.render(16_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
        } finally { scene.close() }
    }

    /** Joga de verdade até haver uma perseguição com contrabando e devolve esse jogo; null se nenhuma semente der. */
    private fun hunted(): Game? {
        for (seed in 1L..40L) {
            val world = World(Region.around(Universe(seed)))
            val g = Game(world, world.region.systems[0], credits = 12_480, stories = false)
            repeat(120) {
                g.refuel()
                val dest = g.reachable().firstOrNull() ?: return@repeat
                val c = Commodity.values().firstOrNull { it.bannedBy(dest.traits.ideology) && !it.bannedBy(g.location.traits.ideology) &&
                    g.world.quote(g.location, it).stock > 0 }
                if (c != null) g.buy(c, minOf(g.holdFree, g.world.quote(g.location, c).stock, 6))
                if (g.travel(dest) is Outcome.Pursued) return g
                for ((k, t) in g.cargo.toMap()) g.sell(k, t)
                g.standing.addHeat(dest.faction, 25)
            }
        }
        return null
    }

    /** Joga de verdade até uma storylet aparecer (histórias ligadas); null se nenhuma semente der. */
    private fun storied(): Game? {
        for (seed in 1L..60L) {
            val world = World(Region.around(Universe(seed)))
            val g = Game(world, world.region.systems[0], credits = 4_200)
            repeat(80) {
                g.refuel()
                val d = g.reachable().firstOrNull() ?: return@repeat
                when (g.travel(d)) {
                    is Outcome.Story -> return g
                    is Outcome.Ok -> {}
                    else -> return@repeat
                }
            }
        }
        return null
    }

    /**
     * Joga uma campanha com mundo vivo até haver crise no jornal, um contrato aceito e uma melhoria instalada,
     * atracado num sistema com estaleiro. Determinístico (semente fixa).
     */
    private fun livingDemo(): Game {
        for (seed in 1L..40L) {
            val world = World(Region.around(Universe(seed)), living = true)
            val g = Game(world, world.region.systems[0], credits = 6_000, stories = false, campaign = true)
            repeat(40) {
                g.refuel()
                if (g.contracts.isEmpty()) g.offers().firstOrNull { it.kind != ContractKind.LACRADA }?.let { g.acceptContract(it) }
                g.shipyard().firstOrNull { it.second != null && it.second!! < g.credits - 2_000 }?.let { g.buyUpgrade(it.first) }
                val ready = world.crises.isNotEmpty() && g.contracts.isNotEmpty() && Upgrade.values().any { g.level(it) > 0 } && g.shipyard().isNotEmpty()
                if (ready) return g
                val dest = g.reachable().let { it[(g.day + seed.toInt()) % it.size] }
                if (g.travel(dest) !is Outcome.Ok) return@repeat
            }
        }
        error("nenhuma campanha de demonstração encontrada")
    }

    /** As quatro telas das maquetes (docs/mercador) sobre um jogo real, uma linha do tempo só. */
    fun renderAll(outDir: File): List<File> {
        outDir.mkdirs()
        val g = hunted() ?: error("nenhuma perseguição encontrada para a demonstração")
        val files = ArrayList<File>()
        fun save(name: String, content: @Composable () -> Unit) { files += File(outDir, name).also { it.writeBytes(png(content)) } }

        // 1 e 2: mapa e estação, com a perseguição ainda pendente à vista (mercado só lê o estado)
        val here = g.location
        val dest = g.reachable().firstOrNull() ?: g.world.region.systems.first { it !== here }
        save("01-mapa.png") { MapScreen(g, dest) }
        save("02-estacao.png") { StationScreen(g) }

        // 5: a perseguição, tal como o jogador a vê pela raiz do app
        val ctl = GameController(g)
        save("05-perseguicao.png") { AppRoot(ctl) }

        // 3: confronto, com segredo, favor e uma rodada já jogada
        val s = (g.respond(Response.CONFRONTAR) as Outcome.Confronting).scene
        g.grantFavor(s.opponent.faction)
        g.grantSecret(s.opponent.faction)
        g.grantTrait(Trait.FRIO, 90)
        g.act(Posture.RESISTIR)
        val scene = g.confrontation ?: s
        save("03-confronto.png") { ConfrontationScreen(g, scene, InnerVoice.line(g)) }
        if (g.confrontation != null) g.act(Posture.VIOLENCIA)

        // 4: livro de contas, depois da violência (marca permanente e consequência em aberto)
        g.grantTrait(Trait.CALEJADO)
        g.grantTrait(Trait.DEVEDOR)
        g.grantTrait(Trait.FRIO, 25)
        save("04-livro.png") { LedgerScreen(g, InnerVoice.line(g)) }

        // 6 a 9: história, desfecho, dossiê e bar, pela raiz do app (navegação e estado reais)
        val sg = storied() ?: error("nenhuma storylet encontrada para a demonstração")
        val sc = GameController(sg)
        save("06-historia.png") { AppRoot(sc) }
        val pick = sg.pendingStory!!.choices.indexOfFirst { it.available }
        sc.choose(pick)
        save("07-relatorio.png") { AppRoot(sc) }
        sc.dismissReport()
        if (sg.confrontation == null) {
            sc.selectTab(Tab.DOSSIE)
            save("08-dossie.png") { AppRoot(sc) }
            sc.selectTab(Tab.ESTACAO); sc.selectStationTab(StationTab.BAR)
            sc.buyRumor(Bar.offers(sg).first { it.kind == RumorKind.ONDE_VENDER })
            save("09-bar.png") { AppRoot(sc) }
        }

        // 10: jogos salvos, com um jogo em andamento, outro mais antigo, um ilegível e a confirmação de apagar
        val dir = java.nio.file.Files.createTempDirectory("mercador-preview").toFile()
        var t = 1_760_000_000_000L
        val slots = com.mercador.core.game.SaveSlots(dir) { t += 3_600_000L * 5; t }
        val first = freshGame(2024)
        val id1 = slots.create(first, "Rota do Qube")
        val second = freshGame(77).also { it.credits = 18_240 }
        val id2 = slots.create(second, "A fuga")
        java.io.File(dir, "slot-3.json").writeText("{ quebrado")
        val menu = GameController(second, slots = slots, activeSlot = id2, newGameFactory = { freshGame() })
        menu.openSlots(); menu.deleteSlot(id1)
        save("10-jogos.png") { AppRoot(menu) }
        dir.deleteRecursively()

        // 11: o hiperespaço, no meio do salto
        save("11-hiperespaco.png") { HyperspaceScreen(Jump("Zenqum", "Kote", 4, 3), .62f) }

        // 12 a 16: o mundo vivo e a campanha, num jogo de verdade (crises no mapa, contratos, estaleiro, dívida, jornal)
        val lg = livingDemo()
        val lc = GameController(lg)
        lc.selectTab(Tab.MAPA)
        lc.selectSystem(lg.contracts.firstOrNull()?.let { k -> lg.world.region.systems.first { it.id == k.destination } } ?: lg.reachable().first())
        save("12-mapa-vivo.png") { AppRoot(lc) }
        lc.selectTab(Tab.ESTACAO); lc.selectStationTab(StationTab.CONTRATOS)
        save("13-contratos.png") { AppRoot(lc) }
        lc.selectStationTab(StationTab.ESTALEIRO)
        save("14-estaleiro.png") { AppRoot(lc) }
        lc.selectTab(Tab.DOSSIE)
        save("15-divida.png") { AppRoot(lc) }
        lg.credits = 60_000; lc.payCreditor()
        save("16-epilogo.png") { AppRoot(lc) }
        return files
    }
}

fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "docs/ui")
    Preview.renderAll(out).forEach { println("gerado: ${it.path} (${it.length() / 1024} KB)") }
}
