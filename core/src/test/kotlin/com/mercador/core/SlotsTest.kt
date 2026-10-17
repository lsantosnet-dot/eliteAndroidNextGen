package com.mercador.core

import com.mercador.core.game.*
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class SlotsTest {
    private fun game(seed: Long): Game {
        val world = World(Region.around(Universe(seed)))
        return Game(world, world.region.systems[0], stories = false)
    }

    private fun dir() = Files.createTempDirectory("mercador-slots").toFile()

    @Test
    fun `slots novos ganham ids crescentes e nomes padrao, e a lista vem do mais recente`() {
        val d = dir(); var t = 1_000L
        val slots = SaveSlots(d) { t++ }
        assertTrue(slots.list().isEmpty()); assertNull(slots.lastUsed())
        val a = slots.create(game(1)); val b = slots.create(game(2), "Rota do Qube"); val c = slots.create(game(3))
        assertEquals(listOf(1, 2, 3), listOf(a, b, c))
        assertEquals(listOf("Jogo 3", "Rota do Qube", "Jogo 1"), slots.list().map { it.name })
        assertEquals(3, slots.lastUsed()!!.id)
        // salvar o 1 o torna o mais recente
        val g = slots.load(1); g.credits = 4321
        assertTrue(slots.save(1, "Jogo 1", g))
        assertEquals(1, slots.lastUsed()!!.id)
        assertEquals(4321, slots.list().first().credits)
        d.deleteRecursively()
    }

    @Test
    fun `cada slot guarda o seu proprio universo e jogo`() {
        val d = dir(); val slots = SaveSlots(d)
        val g1 = game(1); val g2 = game(2); g2.credits = 777
        val a = slots.create(g1); val b = slots.create(g2)
        assertEquals(g1.toSave(), slots.load(a).toSave().copy(name = "", savedAt = 0))
        assertEquals(777, slots.load(b).credits)
        assertNotEquals(slots.load(a).world.region.universe.seed, slots.load(b).world.region.universe.seed)
        d.deleteRecursively()
    }

    @Test
    fun `lista mostra dia, dinheiro e local sem carregar o jogo`() {
        val d = dir(); val slots = SaveSlots(d)
        val g = game(4); g.credits = 1234
        slots.create(g, "Teste")
        val info = slots.list().single()
        assertEquals("Teste", info.name); assertEquals(1234, info.credits); assertEquals(g.location.name, info.location); assertTrue(info.playable)
        d.deleteRecursively()
    }

    @Test
    fun `slot ilegivel aparece com o problema, nao carrega e nunca e apagado sozinho`() {
        val d = dir(); val slots = SaveSlots(d)
        val ok = slots.create(game(1))
        File(d, "slot-7.json").writeText("quebrado")
        val list = slots.list()
        assertEquals(listOf(ok, 7), list.map { it.id })       // o ilegível vai para o fim
        assertFalse(list[1].playable); assertNotNull(list[1].problem)
        assertEquals(ok, slots.lastUsed()!!.id)                // e nunca é o "mais recente"
        assertThrows(SaveException::class.java) { slots.load(7) }
        assertTrue(File(d, "slot-7.json").exists())
        assertEquals(8, slots.create(game(2)))                 // o id do ilegível não é reaproveitado
        d.deleteRecursively()
    }

    @Test
    fun `apagar remove so aquele slot`() {
        val d = dir(); val slots = SaveSlots(d)
        slots.create(game(1)); val b = slots.create(game(2)); slots.create(game(3))
        assertTrue(slots.delete(b))
        assertEquals(listOf(3, 1), slots.list().map { it.id })
        assertFalse(slots.delete(b))
        assertThrows(SaveException::class.java) { slots.load(b) }
        d.deleteRecursively()
    }

    @Test
    fun `renomear mantem o jogo e limpa o nome`() {
        val d = dir(); val slots = SaveSlots(d)
        val id = slots.create(game(1))
        val before = slots.load(id).toSave()
        slots.rename(id, "  A fuga de Qube  ")
        assertEquals("A fuga de Qube", slots.nameOf(id))
        slots.rename(id, "   ")
        assertEquals("Jogo $id", slots.nameOf(id))
        assertEquals(before, slots.load(id).toSave())
        d.deleteRecursively()
    }

    @Test
    fun `nao grava com decisao pendente`() {
        val d = dir(); val slots = SaveSlots(d)
        for (seed in 1L..40L) {
            val world = World(Region.around(Universe(seed)))
            val g = Game(world, world.region.systems[0], credits = 4_000)
            repeat(100) { if (!g.busy) { g.refuel(); g.travel(g.reachable().first()) } }
            if (!g.busy) continue
            val id = slots.create(game(seed))
            val before = File(d, "slot-$id.json").readText()
            assertFalse(slots.save(id, "x", g))
            assertEquals(before, File(d, "slot-$id.json").readText())
            d.deleteRecursively(); return
        }
        error("nenhuma decisão pendente")
    }

    @Test
    fun `save antigo sem metadados continua legivel`() {
        val g = game(1)
        val text = SaveCodec.encode(g.toSave()).lines().filterNot { Regex(""""(name|savedAt|locationName)"""").containsMatchIn(it) }.joinToString("\n")
            .replace(Regex(",\\s*\\}\\s*$"), "\n}")
        val back = SaveCodec.decode(text)
        assertEquals("", back.name)
        assertEquals(g.credits, Game.fromSave(back).credits)
    }
}
