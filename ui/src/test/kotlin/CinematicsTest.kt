import com.mercador.core.economy.Commodity
import com.mercador.core.game.*
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import com.mercador.ui.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.imageio.ImageIO

class CinematicsTest {
    private fun game(seed: Long = 2024, living: Boolean = false, credits: Int = 5_000): Game {
        val world = World(Region.around(Universe(seed)), living)
        return Game(world, world.region.systems[0], credits = credits, stories = false)
    }

    private fun all(g: Game): List<Cinematic> {
        val f = g.factions.factions
        return listOf(Docking("Raorko", f[0], 7L), Interception("Kote", f[1], "4 t"), Revolt("Orrahal", f[2], f[3]), Install(Upgrade.PORAO, 1)) +
            Upgrade.values().map { Install(it, 1) }
    }

    @Test
    fun `cada animacao muda a cada quadro, e o mesmo quadro sempre da a mesma imagem`() {
        for (cin in all(game())) {
            fun frame(p: Float) = Preview.png { CinematicFrame(cin, p) }
            val frames = listOf(.1f, .4f, .7f, .95f).map(::frame)
            for (i in frames.indices) for (k in i + 1 until frames.size)
                assertFalse(frames[i].contentEquals(frames[k]), "${cin::class.simpleName}: quadros $i e $k iguais")
            assertArrayEquals(frame(.5f), frame(.5f), "${cin::class.simpleName} não é determinística")
        }
    }

    @Test
    fun `a atracacao termina no escuro do hangar e a interceptacao fica vermelha`() {
        val g = game()
        val dock = ImageIO.read(Preview.png { CinematicFrame(Docking("A", g.factions.factions[0], 1L), 1f) }.inputStream())
        val c = dock.getRGB(dock.width / 2, dock.height / 3)
        assertTrue((c shr 16 and 255) < 30 && (c and 255) < 30, "fim da atracação devia ser escuro")
        val alarm = ImageIO.read(Preview.png { CinematicFrame(Interception("A", g.factions.factions[1], "x"), .5625f) }.inputStream())
        val e = alarm.getRGB(4, 4)
        assertTrue((e shr 16 and 255) > (e and 255) + 15, "a borda devia estar vermelha")
    }

    @Test
    fun `viagem calma toca salto e depois atracacao, e cada fim passa para a proxima`() {
        val c = GameController(game())
        c.selectSystem(c.game.reachable().first()); c.travel()
        assertTrue(c.cinematic is Jump)
        c.finishCinematic()
        val d = c.cinematic
        assertTrue(d is Docking, "$d")
        assertEquals(c.game.location.name, (d as Docking).station)
        c.finishCinematic()
        assertNull(c.cinematic)
        c.finishCinematic() // idempotente
    }

    @Test
    fun `patrulha que alcanca o jogador toca a interceptacao em vez da atracacao`() {
        for (seed in 1L..40L) {
            val c = GameController(game(seed, credits = 20_000))
            val g = c.game
            repeat(60) {
                g.refuel()
                val dest = g.reachable().firstOrNull() ?: return@repeat
                val bad = Commodity.values().firstOrNull { it.bannedBy(g.world.ideology(dest)) && !g.isIllegalHere(it) && g.world.quote(g.location, it).stock > 0 }
                if (bad != null) g.buy(bad, minOf(g.holdFree, g.world.quote(g.location, bad).stock))
                g.standing.addHeat(g.world.controller(dest), 60)
                c.selectSystem(dest); c.travel()
                if (g.pursuit != null) {
                    c.finishCinematic()
                    val i = c.cinematic
                    assertTrue(i is Interception, "$i")
                    assertEquals(g.pursuit!!.patrol.id, (i as Interception).patrol.id)
                    return
                }
                while (c.cinematic != null) c.finishCinematic()
                while (g.busy) { g.pendingStory?.let { g.chooseStory(0) }; g.confrontation?.let { g.act(Posture.RESISTIR) }; g.pursuit?.let { g.respond(Response.ENTREGAR_SE) } }
                for (k in g.cargo.keys.toList()) g.ownTons(k).takeIf { it > 0 }?.let { g.sell(k, it) }
            }
        }
        fail<Unit>("nenhuma perseguição em 40 universos")
    }

    @Test
    fun `revolta no caminho toca entre o salto e a chegada`() {
        val c = GameController(game(2024, living = true, credits = 50_000))
        val g = c.game
        val target = g.world.region.systems.first { it !== g.location }
        val old = g.world.controller(target)
        repeat(8) {
            g.world.pushMeters(target, -60, 60, 0)
            g.refuel()
            c.selectSystem(g.reachable().first { it !== target }); c.travel()
            val seq = generateSequence(c.cinematic) { c.finishCinematic(); c.cinematic }.toList()
            while (g.busy) { g.pendingStory?.let { g.chooseStory(0) }; g.confrontation?.let { g.act(Posture.RESISTIR) }; g.pursuit?.let { g.respond(Response.ENTREGAR_SE) } }
            val r = seq.filterIsInstance<Revolt>().firstOrNull { it.system == target.name }
            if (r != null) {
                assertTrue(seq.first() is Jump)
                assertEquals(old.id, r.old.id)
                assertEquals(g.world.controller(target).id, r.new.id)
                assertTrue(seq.indexOf(r) < seq.size - 1, "a chegada vem depois da revolta")
                return
            }
        }
        fail<Unit>("a revolta não aconteceu")
    }

    @Test
    fun `comprar melhoria toca a instalacao, e falhar nao toca nada`() {
        val c = GameController(game(credits = 100_000))
        val g = c.game
        val yard = g.world.region.systems.first { Upgrade.PORAO.soldAt(it) }
        // chega ao estaleiro de verdade, saltando
        var guard = 0
        while (g.location !== yard && guard++ < 30) {
            g.refuel()
            val next = g.reachable().minBy { g.world.region.distanceLy(it, yard) }
            c.selectSystem(next); c.travel()
            while (c.cinematic != null) c.finishCinematic()
            while (g.busy) { g.pendingStory?.let { g.chooseStory(0) }; g.confrontation?.let { g.act(Posture.RESISTIR) }; g.pursuit?.let { g.respond(Response.ENTREGAR_SE) } }
        }
        assertSame(yard, g.location)
        c.buyUpgrade(Upgrade.PORAO)
        val i = c.cinematic
        assertTrue(i is Install, "$i")
        assertEquals(Upgrade.PORAO, (i as Install).upgrade); assertEquals(1, i.level)
        c.finishCinematic()
        g.credits = 0
        c.buyUpgrade(Upgrade.PORAO)
        assertNull(c.cinematic)
    }
}
