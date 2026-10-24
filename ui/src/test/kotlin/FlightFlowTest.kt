import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import com.mercador.core.flight.FlightResult
import com.mercador.core.flight.FlightSession
import com.mercador.core.flight.PlayerShip
import com.mercador.core.game.Game
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import com.mercador.ui.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FlightFlowTest {
    private fun controller(): GameController {
        val world = World(Region.around(Universe(2024)))
        return GameController(Game(world, world.region.systems[0], credits = 5_000, stories = false))
    }

    /** Pilota até algo grande acontecer e entrega ao controlador, como a tela faz. */
    private fun GameController.fly(s: FlightSession, seconds: Float): FlightResult? {
        var t = 0f
        while (t < seconds) {
            s.update(1f / 60f)?.let { onFlight(it); return it }
            t += 1f / 60f
        }
        return null
    }

    @Test
    fun `decolar pelo mapa abre o voo com o destino escolhido, e o salto pilotado toca a animacao e chega`() {
        for (seed in 0 until 10) {
            val c = controller()
            val from = c.game.location
            val dest = c.game.reachable().first()
            c.selectTab(Tab.MAPA)
            c.launchTo(dest)
            val s = c.flight!!
            assertSame(dest, s.target)
            s.controls.targetSpeed = PlayerShip.MAX_SPEED
            assertNull(c.fly(s, 25f))
            s.toggleHyperspace()
            val r = c.fly(s, 15f)
            if (r !is FlightResult.Jumped) continue // o salto falhou (witchspace): tenta outra decolagem
            assertSame(dest, c.game.location)
            val j = c.jump!!
            assertEquals(from.name, j.from); assertEquals(dest.name, j.to)
            assertNotNull(c.flight, "depois do salto o voo continua, agora no destino")
            while (c.cinematic != null) c.finishCinematic()
            // chega à estação de lá e atraca (com o computador, para o teste não depender de pilotagem)
            // traz o planeta para perto: a estação surge no próximo ciclo do loop principal
            if (c.flight!!.model.station == null) c.flight!!.model.planet!!.position.set(0f, 0f, 30_000f)
            var docked: FlightResult? = null
            repeat(3600) {
                if (docked == null && c.flight != null) {
                    c.flight!!.update(1f / 60f)?.let { c.onFlight(it); docked = it }
                    if (docked == null && c.flight?.model?.station != null) {
                        c.flight!!.model.station!!.position.set(0f, 0f, 3_000f)
                        c.flight!!.dockingComputer()?.let { c.onFlight(it); docked = it }
                    }
                }
            }
            assertTrue(docked is FlightResult.Docked, "$docked")
            assertNull(c.flight)
            assertTrue(c.cinematic is Docking)
            assertEquals(Tab.ESTACAO, c.tab)
            assertTrue(c.notice!!.contains(dest.name))
            return
        }
        fail<Unit>("nenhum salto completo")
    }

    @Test
    fun `nave destruida termina o voo com um relatorio e volta a estacao`() {
        val c = controller()
        c.launch()
        val s = c.flight!!
        val st = s.model.station!!
        st.position.set(0f, 0f, 700f); st.rollCounter = 0f
        st.setOrientation(com.mercador.core.flight.Vec3(0f, 0f, -1f), com.mercador.core.flight.Vec3(0f, 1f, 0f))
        s.controls.targetSpeed = PlayerShip.MAX_SPEED
        val r = c.fly(s, 10f)
        assertTrue(r is FlightResult.Destroyed)
        assertNull(c.flight)
        assertEquals("Nave destruída", c.report!!.kicker)
        assertEquals(Tab.ESTACAO, c.tab)
    }

    @Test
    fun `nao troca de jogo em pleno voo`() {
        val c = controller()
        c.launch()
        c.openSlots()
        assertFalse(c.showSlots)
        assertEquals("Atraque antes de trocar de jogo.", c.notice)
    }

    @Test
    fun `destino sem combustivel nao decola`() {
        val c = controller()
        val far = c.game.world.region.systems.filter { it !== c.game.location }.maxBy { c.game.fuelCost(it) }
        if (c.game.fuelCost(far) <= c.game.fuel) return
        c.launchTo(far)
        assertNull(c.flight)
        assertNotNull(c.notice)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `a raiz mostra o voo, o relogio move a nave e o gatilho dispara o laser`() {
        val c = controller()
        val scene = ImageComposeScene(720, 1480, Density(2f)) { AppRoot(c) }
        try {
            var t = 0L
            fun frame(ms: Long) { t += ms * 1_000_000L; scene.render(t) }
            frame(16)
            c.launch()
            frame(16); frame(16)
            val s = c.flight!!
            val z0 = s.model.station!!.position.z
            repeat(30) { frame(16) }
            assertTrue(s.model.station!!.position.z < z0, "a estação não ficou para trás")
            // o gatilho fica no canto inferior direito; segurar dispara
            val fire = Offset(720f - 20f - 96f, 1480f - 12f - 150f)
            scene.sendPointerEvent(PointerEventType.Press, fire); frame(16); frame(16)
            assertTrue(s.controls.fire, "o gatilho não ligou o laser")
            assertTrue(s.model.player.laserTemp > 0f)
            scene.sendPointerEvent(PointerEventType.Release, fire); frame(16)
            assertFalse(s.controls.fire)
        } finally { scene.close() }
    }
}
