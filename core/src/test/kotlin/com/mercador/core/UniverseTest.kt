package com.mercador.core

import com.mercador.core.economy.Commodity
import com.mercador.core.economy.Laws
import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive
import com.mercador.core.universe.Ideology
import com.mercador.core.universe.Universe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UniverseTest {
    @Test
    fun `mesma semente gera o mesmo setor, em qualquer ordem de acesso`() {
        val a = Universe(42)
        val b = Universe(42)
        val far = a.sector(7_000_000, 123, -456).map { it.name to it.traits }
        b.sector(0, 0, 0) // acesso anterior não influencia
        assertEquals(far, b.sector(7_000_000, 123, -456).map { it.name to it.traits })
    }

    @Test
    fun `sementes diferentes geram universos diferentes`() {
        val a = Universe(1).nearby(0, 0, 0, 3).map { it.name }
        val b = Universe(2).nearby(0, 0, 0, 3).map { it.name }
        assertNotEquals(a, b)
    }

    @Test
    fun `sistemas ficam dentro do seu setor e com traços válidos`() {
        val u = Universe(99)
        for (s in u.nearby(0, 0, 0, 5)) {
            assertEquals(Math.floorDiv(s.x, Universe.SECTOR_SIZE), s.id.sx)
            assertEquals(Math.floorDiv(s.y, Universe.SECTOR_SIZE), s.id.sy)
            val t = s.traits
            assertTrue(t.techLevel in 1..15)
            for (v in listOf(t.corruption, t.surveillance, t.stability)) assertTrue(v in 0..100)
            assertEquals(s.faction.ideology, t.ideology)
            assertSame(s, u.system(s.id)?.let { s }) // id resolve de volta
            assertEquals(s.name, u.system(s.id)!!.name)
        }
    }

    @Test
    fun `matriz de relacoes e simetrica e limitada`() {
        val f = Universe(5).factions(0)
        for (a in f.factions) for (b in f.factions) {
            assertEquals(f.relation(a.id, b.id), f.relation(b.id, a.id))
            assertTrue(f.relation(a.id, b.id) in -100..100)
        }
    }

    @Test
    fun `nomes de facao sao unicos e cada sistema pertence a uma facao`() {
        val f = Universe(5).factions(3)
        assertEquals(f.factions.size, f.factions.map { it.name }.toSet().size)
        assertTrue(f.controllerOf(0, 0) in f.factions)
    }

    @Test
    fun `leis por ideologia fabricam contrabando`() {
        assertTrue(Commodity.DROGAS.bannedBy(Ideology.AUTORITARIA))
        assertFalse(Commodity.COMIDA.bannedBy(Ideology.AUTORITARIA))
        for (i in Ideology.values()) assertTrue(Laws.banned(i).isNotEmpty())
    }

    @Test
    fun `derive e Rng sao deterministicos e distribuem bem`() {
        assertEquals(derive(1, 2, 3), derive(1, 2, 3))
        assertNotEquals(derive(1, 2, 3), derive(1, 3, 2))
        val r = Rng(7)
        val buckets = IntArray(10)
        repeat(10_000) { buckets[r.nextInt(10)]++ }
        assertTrue(buckets.all { it in 800..1200 })
    }
}
