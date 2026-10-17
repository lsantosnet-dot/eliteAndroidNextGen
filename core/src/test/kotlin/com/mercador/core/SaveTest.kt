package com.mercador.core

import com.mercador.core.economy.Commodity
import com.mercador.core.game.*
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class SaveTest {
    private fun newGame(seed: Long): Game {
        val world = World(Region.around(Universe(seed)))
        return Game(world, world.region.systems[0], credits = 6_000)
    }

    /** Um jogador roteirizado e determinístico: comercia, leva contrabando, foge, confronta e escolhe histórias. */
    private fun play(g: Game, steps: Int) {
        repeat(steps) { i ->
            when {
                g.pursuit != null -> g.respond(if (i % 3 == 0) Response.CONFRONTAR else Response.ENTREGAR_SE)
                g.confrontation != null -> g.act(if (g.day % 5 == 0) Posture.VIOLENCIA else Posture.RESISTIR)
                g.pendingStory != null -> g.chooseStory(g.pendingStory!!.choices.indexOfFirst { it.available })
                else -> {
                    g.credits = maxOf(g.credits, 800)
                    g.refuel()
                    val dest = g.reachable().let { it[(i * 7 + g.day) % it.size] }
                    val c = Commodity.values().filter { g.world.quote(g.location, it).stock > 0 }.let { it[(i + g.day) % it.size] }
                    g.buy(c, minOf(g.holdFree, g.world.quote(g.location, c).stock, 4))
                    g.travel(dest)
                    for ((k, t) in g.cargo.toMap()) if (i % 2 == 0 && !g.busy) g.sell(k, t)
                }
            }
        }
        // termina num momento calmo
        var guard = 0
        while (g.busy && guard++ < 20) when {
            g.pursuit != null -> g.respond(Response.JOGAR_FORA)
            g.confrontation != null -> g.act(Posture.RESISTIR)
            g.pendingStory != null -> g.chooseStory(g.pendingStory!!.choices.indexOfFirst { it.available })
        }
    }

    private fun roundTrip(g: Game): Game = Game.fromSave(SaveCodec.decode(SaveCodec.encode(g.toSave())))

    @Test
    fun `ida e volta preserva tudo, em varias partidas`() {
        for (seed in 1L..8L) {
            val g = newGame(seed)
            play(g, 120)
            assertTrue(g.canSave, "seed $seed terminou ocupado")
            val back = roundTrip(g)
            assertEquals(g.toSave(), back.toSave(), "seed $seed")
            assertEquals(g.day, back.day); assertEquals(g.credits, back.credits)
            assertEquals(g.location.id, back.location.id)
            assertEquals(g.ledger, back.ledger); assertEquals(g.traits, back.traits)
        }
    }

    @Test
    fun `o local restaurado e a mesma instancia da regiao, para as comparacoes por identidade do jogo`() {
        val g = newGame(4); play(g, 30)
        val back = roundTrip(g)
        assertTrue(back.world.region.systems.any { it === back.location })
        assertFalse(back.reachable().any { it === back.location })
        assertEquals(g.reachable().map { it.id }, back.reachable().map { it.id })
    }

    @Test
    fun `o roteiro gera livro, e traco, segredo e favor entram no save mesmo sem depender do acaso`() {
        for (seed in 1L..4L) {
            val g = newGame(seed); play(g, 120)
            assertTrue(g.ledger.isNotEmpty(), "livro vazio na seed $seed")
            val f = g.factions.factions
            g.grantTrait(Trait.CALEJADO); g.grantTrait(Trait.FRIO, 45)
            g.grantSecret(f[0]); g.grantSecret(f[0]); g.grantFavor(f[1])
            val secrets = g.secretsAbout(f[0]); val favors = g.favors(f[1])
            assertTrue(secrets >= 2)
            val back = roundTrip(g)
            assertEquals(g.toSave(), back.toSave())
            assertTrue(back.hasTrait(Trait.CALEJADO)); assertEquals(g.traitDaysLeft(Trait.FRIO), back.traitDaysLeft(Trait.FRIO))
            assertEquals(secrets, back.secretsAbout(f[0])); assertEquals(favors, back.favors(f[1]))
        }
    }

    @Test
    fun `retaliacao a caminho sobrevive ao save com o mesmo prazo e a mesma pessoa`() {
        for (seed in 1L..40L) {
            val g = newGame(seed)
            var chased = false
            repeat(150) {
                if (chased) return@repeat
                g.refuel()
                val dest = g.reachable().firstOrNull() ?: return@repeat
                val c = Commodity.values().firstOrNull { it.bannedBy(dest.traits.ideology) && !it.bannedBy(g.location.traits.ideology) && g.world.quote(g.location, it).stock > 0 }
                if (c != null) g.buy(c, minOf(g.holdFree, g.world.quote(g.location, c).stock, 6))
                if (g.travel(dest) is Outcome.Pursued) chased = true else g.standing.addHeat(dest.faction, 25)
            }
            if (!chased) continue
            g.respond(Response.CONFRONTAR); g.act(Posture.VIOLENCIA)
            if (g.pendingRetaliations() == 0) continue
            val back = roundTrip(g)
            assertEquals(g.openConsequences().map { it.first.name to it.second }, back.openConsequences().map { it.first.name to it.second })
            assertTrue(back.hasTrait(Trait.MARCADO))
            return
        }
        error("nenhuma retaliação gerada")
    }

    @Test
    fun `continuar o jogo restaurado e identico a continuar o original`() {
        for (seed in 1L..5L) {
            val a = newGame(seed); play(a, 60)
            val b = roundTrip(a)
            play(a, 80); play(b, 80)
            assertEquals(a.toSave(), b.toSave(), "seed $seed divergiu depois de restaurar")
        }
    }

    @Test
    fun `decisao pendente nao e salva e recarregar refaz o mesmo encontro`() {
        for (seed in 1L..40L) {
            val g = newGame(seed)
            var calm: GameSave? = null
            var day0 = 0
            repeat(100) {
                if (g.busy) return@repeat
                g.refuel()
                val dest = g.reachable().firstOrNull() ?: return@repeat
                calm = g.toSave(); day0 = g.day
                g.travel(dest)
            }
            if (!g.busy || calm == null) continue
            assertFalse(g.canSave)
            // o save feito no último momento calmo restaura um jogo livre
            val back = Game.fromSave(calm!!)
            assertFalse(back.busy)
            assertEquals(day0, back.day)
            // refazer a mesma viagem a partir do save dá a mesma decisão pendente
            val dest = back.reachable().first()
            back.travel(dest)
            assertEquals(g.pursuit != null, back.pursuit != null)
            assertEquals(g.pendingStory?.storylet?.id, back.pendingStory?.storylet?.id)
            assertEquals(g.confrontation != null, back.confrontation != null)
            return
        }
        error("nenhuma decisão pendente encontrada")
    }

    @Test
    fun `save corrompido ou de outra versao da erro claro, nunca um jogo quebrado`() {
        val good = SaveCodec.encode(newGame(1).toSave())
        for (bad in listOf("", "não é json", "{}", good.substring(0, good.length / 2))) {
            assertThrows(SaveException::class.java) { SaveCodec.decode(bad) }
        }
        val e = assertThrows(SaveException::class.java) { SaveCodec.decode(good.replace("\"version\": 1", "\"version\": 99")) }
        assertTrue(e.message!!.contains("99"))
        val g = newGame(1)
        assertThrows(SaveException::class.java) { Game.fromSave(g.toSave().copy(cargo = mapOf("OURO_DE_TOLO" to 3))) }
        assertThrows(SaveException::class.java) { Game.fromSave(g.toSave().copy(location = SysRef(999, 999, 5))) }
        assertThrows(SaveException::class.java) { Game.fromSave(g.toSave().copy(location = SysRef(0, 0, 0), region = g.toSave().region.drop(1))) }
        assertThrows(SaveException::class.java) { Game.fromSave(g.toSave().copy(favors = listOf(1))) }
        assertThrows(SaveException::class.java) { Game.fromSave(g.toSave().copy(heat = listOf(1, 2))) }
    }

    @Test
    fun `arquivo grava de forma atomica, le de volta e sobrescreve`() {
        val dir = Files.createTempDirectory("mercador-save").toFile()
        val file = File(dir, "sub/save.json")
        val store = SaveStore(file)
        assertFalse(store.exists())
        val g = newGame(3); play(g, 40)
        store.write(g.toSave())
        assertTrue(store.exists())
        assertFalse(File(file.path + ".tmp").exists(), "sobrou arquivo temporário")
        assertEquals(g.toSave(), store.read())
        play(g, 20)
        store.write(g.toSave())
        assertEquals(g.toSave(), store.read())
        file.writeText("lixo")
        assertThrows(SaveException::class.java) { store.read() }
        dir.deleteRecursively()
    }

    @Test
    fun `save e pequeno e legivel`() {
        val g = newGame(2); play(g, 200)
        val text = SaveCodec.encode(g.toSave())
        assertTrue(text.length < 150_000, "save de ${text.length} bytes")
        assertTrue(text.contains("\"credits\""))
        assertFalse(text.contains("Zenqum") && text.length > 10_000_000)
    }

    @Test
    fun `abrir sem save comeca um jogo novo, com save restaura, e save ilegivel vai para bak`() {
        val dir = Files.createTempDirectory("mercador-load").toFile()
        val file = File(dir, "save.json")
        val store = SaveStore(file)
        val fresh = { newGame(5) }
        val first = store.loadOrNew(fresh)
        assertNull(first.warning)
        play(first.game, 50)
        assertTrue(store.autosave(first.game))
        val again = store.loadOrNew(fresh)
        assertNull(again.warning)
        assertEquals(first.game.toSave(), again.game.toSave())

        file.writeText("{ quebrado")
        val broken = store.loadOrNew(fresh)
        assertNotNull(broken.warning)
        assertEquals(0, broken.game.day)
        assertFalse(file.exists())
        assertEquals("{ quebrado", File(file.path + ".bak").readText(), "o save ilegível precisa ser preservado")
        dir.deleteRecursively()
    }

    @Test
    fun `autosave so grava em momento calmo e nunca perde o ultimo save bom`() {
        val dir = Files.createTempDirectory("mercador-auto").toFile()
        val store = SaveStore(File(dir, "save.json"))
        for (seed in 1L..40L) {
            val g = newGame(seed)
            repeat(100) {
                if (g.busy) return@repeat
                g.refuel(); g.travel(g.reachable().first())
            }
            if (!g.busy) continue
            val before = SaveCodec.encode(newGame(seed).toSave())
            File(dir, "save.json").writeText(before)
            assertFalse(store.autosave(g), "gravou com decisão pendente")
            assertEquals(before, File(dir, "save.json").readText())
            dir.deleteRecursively()
            return
        }
        error("nenhuma decisão pendente encontrada")
    }
}
