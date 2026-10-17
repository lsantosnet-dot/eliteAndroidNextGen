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
        return files
    }
}

fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "docs/ui")
    Preview.renderAll(out).forEach { println("gerado: ${it.path} (${it.length() / 1024} KB)") }
}
