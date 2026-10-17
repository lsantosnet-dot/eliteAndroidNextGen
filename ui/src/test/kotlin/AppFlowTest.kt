import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import com.mercador.core.economy.Commodity
import com.mercador.core.game.*
import com.mercador.core.game.SaveStore
import com.mercador.core.game.SaveSlots
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

    @Test
    fun `o controlador salva sozinho a cada acao calma e o jogo volta como estava`() {
        val dir = java.nio.file.Files.createTempDirectory("mercador-ui").toFile()
        val store = SaveStore(java.io.File(dir, "save.json"))
        val world = World(Region.around(Universe(2024)))
        val c = GameController(Game(world, world.region.systems[0], credits = 5_000, stories = false), onChange = { store.autosave(it) })
        assertFalse(store.exists())
        val com = Commodity.values().first { c.game.world.quote(c.game.location, it).stock >= 3 && !it.bannedBy(c.game.location.traits.ideology) }
        c.selectCommodity(com); c.buy()
        assertTrue(store.exists())
        c.selectTab(Tab.MAPA); c.selectSystem(c.game.reachable().first()); c.travel()
        val back = Game.fromSave(store.read())
        assertEquals(c.game.toSave(), back.toSave())
        assertEquals(c.game.location.id, back.location.id)
        assertEquals(c.game.cargo.toMap(), back.cargo.toMap())
        dir.deleteRecursively()
    }
}

class SlotsFlowTest {
    private fun newWorldGame(seed: Long): Game {
        val world = World(Region.around(Universe(seed)))
        return Game(world, world.region.systems[0], stories = false)
    }

    private class Env {
        val dir = java.nio.file.Files.createTempDirectory("mercador-ui-slots").toFile()
        var t = 0L
        val slots = SaveSlots(dir) { t++ }
        var seeds = 10L
    }

    private fun env(): Pair<Env, GameController> {
        val e = Env()
        val first = newWorldGame(1)
        val id = e.slots.create(first)
        val c = GameController(first, slots = e.slots, activeSlot = id, newGameFactory = { newWorldGame(e.seeds++) })
        return e to c
    }

    @Test
    fun `novo jogo cria slot, troca de universo e limpa o estado da interface`() {
        val (e, c) = env()
        val old = c.game
        c.selectTab(Tab.MAPA); c.selectSystem(old.reachable().first())
        c.openSlots(); assertTrue(c.showSlots)
        c.newGame()
        assertNotSame(old, c.game)
        assertEquals(2, c.slotId); assertFalse(c.showSlots)
        assertEquals(Tab.ESTACAO, c.tab); assertNull(c.selectedSystem)
        assertEquals(2, e.slots.list().size)
        assertNotEquals(old.world.region.universe.seed, c.game.world.region.universe.seed)
        e.dir.deleteRecursively()
    }

    @Test
    fun `cada jogo grava no proprio slot, sem vazar para o outro`() {
        val (e, c) = env()
        c.game.credits = 111; c.selectTab(Tab.DOSSIE) // qualquer ação grava
        c.newGame()
        c.game.credits = 999; c.selectTab(Tab.LIVRO)
        assertEquals(111, e.slots.load(1).credits)
        assertEquals(999, e.slots.load(2).credits)
        c.loadSlot(1)
        assertEquals(111, c.game.credits); assertEquals(1, c.slotId)
        c.selectTab(Tab.MAPA)
        assertEquals(999, e.slots.load(2).credits) // o jogo 2 não foi tocado
        e.dir.deleteRecursively()
    }

    @Test
    fun `apagar pede confirmacao e apagar o jogo atual abre outro ou comeca um novo`() {
        val (e, c) = env()
        c.newGame() // agora há 1 e 2, atual = 2
        c.deleteSlot(1)
        assertEquals(1, c.pendingDelete); assertEquals(2, e.slots.list().size) // só pediu confirmação
        c.deleteSlot(1)
        assertNull(c.pendingDelete); assertEquals(listOf(2), e.slots.list().map { it.id })
        c.deleteSlot(2); c.deleteSlot(2) // apaga o em jogo, sem outro restante
        assertEquals(1, e.slots.list().size)
        assertEquals(c.slotId, e.slots.list().single().id)
        assertTrue(c.notice!!.contains("novo"))
        e.dir.deleteRecursively()
    }

    @Test
    fun `nao troca de jogo com decisao pendente e slot ilegivel nao carrega`() {
        val (e, c) = env()
        var busy: GameController? = null
        for (seed in 1L..40L) {
            val world = World(Region.around(Universe(seed)))
            val g = Game(world, world.region.systems[0], credits = 4_000)
            val cc = GameController(g, slots = e.slots, activeSlot = e.slots.create(g), newGameFactory = { newWorldGame(99) })
            repeat(100) { if (!g.busy) { g.refuel(); g.travel(g.reachable().first()) } }
            if (g.busy) { busy = cc; break }
        }
        val b = busy ?: error("nenhuma decisão pendente")
        val n = e.slots.list().size
        b.newGame(); assertEquals(n, e.slots.list().size)
        assertTrue(b.notice!!.contains("Resolva"))
        b.openSlots(); assertFalse(b.showSlots)

        java.io.File(e.dir, "slot-50.json").writeText("quebrado")
        c.loadSlot(50)
        assertEquals(1, c.slotId) // continua no jogo em que estava
        assertTrue(c.notice!!.contains("ilegível"))
        e.dir.deleteRecursively()
    }

    @Test
    fun `a lista de jogos renderiza com ativo, apagar e slot ilegivel`() {
        val (e, c) = env()
        c.newGame()
        java.io.File(e.dir, "slot-9.json").writeText("quebrado")
        c.openSlots(); c.deleteSlot(1)
        val png = Preview.png { AppRoot(c) }
        assertTrue(png.size > 5_000)
        e.dir.deleteRecursively()
    }
}

class HyperspaceTest {
    private fun controller(): GameController {
        val world = World(Region.around(Universe(2024)))
        return GameController(Game(world, world.region.systems[0], credits = 5_000, stories = false))
    }

    @Test
    fun `saltar liga a animacao com origem, destino e custo, e o jogo ja esta no destino`() {
        val c = controller()
        val from = c.game.location
        val dest = c.game.reachable().first()
        val ly = c.game.fuelCost(dest); val days = c.game.travelDays(dest)
        c.selectSystem(dest); c.travel()
        val j = c.jump!!
        assertEquals(from.name, j.from); assertEquals(dest.name, j.to); assertEquals(ly, j.ly); assertEquals(days, j.days)
        assertEquals(dest, c.game.location)
        val v = c.version
        c.finishJump()
        assertNull(c.jump); assertTrue(c.version > v)
        c.finishJump() // idempotente
    }

    @Test
    fun `viagem que falha nao toca a animacao`() {
        val c = controller()
        c.travel()
        assertNull(c.jump)
        c.game.credits = 0
        val far = c.game.world.region.systems.maxByOrNull { c.game.world.region.distanceLy(c.game.location, it) }!!
        c.selectSystem(far)
        if (c.game.fuelCost(far) > c.game.fuel) { c.travel(); assertNull(c.jump) }
    }

    @Test
    fun `a animacao muda a cada quadro e termina em clarao`() {
        val j = Jump("A", "B", 3, 2)
        fun frame(p: Float): ByteArray = Preview.png { HyperspaceScreen(j, p) }
        val frames = listOf(0f, .25f, .5f, .75f).map(::frame)
        for (i in frames.indices) for (k in i + 1 until frames.size) assertFalse(frames[i].contentEquals(frames[k]), "quadros $i e $k iguais")
        // fim: tela quase toda branca
        val img = javax.imageio.ImageIO.read(frame(1f).inputStream())
        val c = img.getRGB(img.width / 2, img.height / 2)
        assertTrue((c shr 16 and 255) > 240 && (c shr 8 and 255) > 240)
        // início: fundo escuro (só estrelas e a nave)
        val start = javax.imageio.ImageIO.read(frame(0f).inputStream())
        assertTrue(start.getRGB(5, 5) and 0xFF < 40)
        // sempre a mesma animação: o mesmo quadro duas vezes dá a mesma imagem
        assertArrayEquals(frame(.4f), frame(.4f))
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Test
    fun `a raiz mostra a animacao, avanca com o relogio e depois libera a chegada`() {
        val c = controller()
        val scene = androidx.compose.ui.ImageComposeScene(720, 1480, androidx.compose.ui.unit.Density(2f)) { AppRoot(c) }
        try {
            var t = 0L
            fun frame(ms: Long) { t += ms * 1_000_000L; scene.render(t) }
            frame(16)
            c.selectSystem(c.game.reachable().first()); c.travel()
            assertNotNull(c.jump)
            frame(16); frame(16)
            assertNotNull(c.jump, "terminou cedo demais")
            frame(1000); assertNotNull(c.jump)
            frame(1000); assertNotNull(c.jump)
            repeat(4) { frame(400) }
            assertNull(c.jump, "a animação não terminou depois de ${HYPERSPACE_MS} ms")
            assertEquals(Tab.ESTACAO, c.tab)
            // pular com um toque
            c.selectSystem(c.game.reachable().first()); c.travel()
            frame(16); frame(16)
            assertNotNull(c.jump)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, androidx.compose.ui.geometry.Offset(300f, 600f)); frame(16)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, androidx.compose.ui.geometry.Offset(300f, 600f)); frame(16)
            assertNull(c.jump, "o toque não pulou a animação")
        } finally { scene.close() }
    }
}
