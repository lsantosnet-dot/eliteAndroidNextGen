import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import com.mercador.core.economy.Commodity
import com.mercador.core.game.*
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import com.mercador.ui.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AppFlowTest {
    private fun controller(stories: Boolean = false): GameController {
        val world = World(Region.around(Universe(2024)))
        return GameController(Game(world, world.region.systems[0], credits = 5_000, stories = stories))
    }

    @Test
    fun `viagem pelo controlador troca de lugar, volta para a estacao e limpa a selecao`() {
        val c = controller()
        c.selectTab(Tab.MAPA)
        val dest = c.game.reachable().first()
        c.selectSystem(dest)
        val v0 = c.version
        c.travel()
        assertEquals(dest, c.game.location)
        assertEquals(Tab.ESTACAO, c.tab)
        assertNull(c.selectedSystem)
        assertTrue(c.version > v0)
        assertTrue(c.notice!!.contains(dest.name))
    }

    @Test
    fun `comprar e vender pelo controlador mexem no jogo e avisam`() {
        val c = controller()
        val com = Commodity.values().first { c.game.world.quote(c.game.location, it).stock >= 5 && !it.bannedBy(c.game.location.traits.ideology) }
        c.buy(); assertTrue(c.notice!!.contains("Escolha")) // sem seleção
        c.selectCommodity(com)
        val before = c.game.credits
        c.buy()
        assertTrue(c.game.credits < before)
        assertEquals(1, c.game.cargo[com])
        assertTrue(c.notice!!.startsWith("Comprou"))
        c.cycleQuantity(); assertEquals(5, c.quantity)
        c.sell()
        assertEquals(null, c.game.cargo[com]) // vende o que há, até a quantidade
        assertTrue(c.notice!!.startsWith("Vendeu"))
        c.sell(); assertEquals("Você não carrega isso.", c.notice)
    }

    @Test
    fun `falhas viram avisos em portugues`() {
        val c = controller()
        c.selectTab(Tab.MAPA)
        c.travel(); assertEquals("Escolha um destino no mapa.", c.notice)
        c.game.credits = 0
        c.selectTab(Tab.DOSSIE)
        c.refuel()
        assertEquals("O tanque já está cheio.", c.notice)
        for (f in Failure.values()) assertTrue(GameController.describe(f).isNotBlank())
    }

    @Test
    fun `decisoes pendentes bloqueiam a navegacao ate serem resolvidas`() {
        var found: GameController? = null
        for (seed in 1L..40L) {
            val world = World(Region.around(Universe(seed)))
            val c = GameController(Game(world, world.region.systems[0], credits = 4_000))
            repeat(80) {
                if (found != null) return@repeat
                c.game.refuel()
                val d = c.game.reachable().firstOrNull() ?: return@repeat
                c.selectSystem(d); c.travel()
                if (c.game.pendingStory != null) found = c
                else if (c.game.busy) return@repeat
            }
            if (found != null) break
        }
        val c = found ?: error("nenhuma história")
        assertTrue(c.game.busy)
        val i = c.game.pendingStory!!.choices.indexOfFirst { it.available }
        c.choose(i)
        assertNotNull(c.report)
        assertNull(c.game.pendingStory)
        c.dismissReport()
        assertNull(c.report)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `toque real na barra de navegacao troca de aba`() {
        val c = controller()
        val scene = ImageComposeScene(720, 1480, Density(2f)) { AppRoot(c) }
        try {
            var t = 0L
            fun frame() { t += 16_000_000L; scene.render(t) }
            frame()
            assertEquals(Tab.ESTACAO, c.tab)
            fun tap(x: Float, y: Float) {
                scene.sendPointerEvent(PointerEventType.Press, Offset(x, y)); frame()
                scene.sendPointerEvent(PointerEventType.Release, Offset(x, y)); frame()
            }
            // a barra ocupa os 100 px de baixo, em quatro colunas iguais
            tap(90f, 1430f); assertEquals(Tab.MAPA, c.tab)
            tap(450f, 1430f); assertEquals(Tab.DOSSIE, c.tab)
            tap(630f, 1430f); assertEquals(Tab.LIVRO, c.tab)
            tap(270f, 1430f); assertEquals(Tab.ESTACAO, c.tab)
        } finally { scene.close() }
    }
}
