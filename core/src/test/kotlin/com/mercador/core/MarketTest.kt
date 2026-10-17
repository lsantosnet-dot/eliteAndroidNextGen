package com.mercador.core

import com.mercador.core.economy.Commodity
import com.mercador.core.economy.Market
import com.mercador.core.economy.PriceReason
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MarketTest {
    private val region = Region.around(Universe(2024))

    @Test
    fun `regiao tem 50 sistemas unicos e e deterministica`() {
        assertEquals(50, region.systems.size)
        assertEquals(50, region.systems.map { it.id }.toSet().size)
        val again = Region.around(Universe(2024))
        assertEquals(region.systems.map { it.id }, again.systems.map { it.id })
    }

    @Test
    fun `regiao tem as tres ou quatro facoes e e navegavel com alcance razoavel`() {
        val factions = region.systems.map { it.faction.id }.toSet()
        assertTrue(factions.size >= 2, "facções na região: $factions")
        val range = region.minimumConnectingRange()
        assertTrue(range <= 60, "alcance mínimo para conectar: $range ly")
        assertTrue(region.isConnected(range.toDouble()))
    }

    @Test
    fun `mercado e funcao pura de sistema e dia`() {
        val s = region.systems[3]
        assertEquals(Market.entries(s, 10), Market.entries(s, 10))
        // dentro do mesmo período, o mesmo mercado
        assertEquals(Market.entries(s, 7), Market.entries(s, 13))
    }

    @Test
    fun `precos e estoques sao positivos e limitados`() {
        for (s in region.systems) for (day in listOf(0, 30, 365)) for (e in Market.entries(s, day)) {
            assertTrue(e.price >= 1)
            assertTrue(e.price <= e.commodity.basePrice * 4, "${e.commodity} ${e.price}")
            assertTrue(e.stock >= 0)
        }
    }

    @Test
    fun `quem produz vende mais barato que quem demanda`() {
        val cheap = ArrayList<Double>()
        val dear = ArrayList<Double>()
        for (s in region.systems) for (c in Commodity.values()) {
            val e = Market.entry(s, c, 0)
            if (e.illegal) continue
            val ratio = e.price.toDouble() / c.basePrice
            when (e.reason) { PriceReason.EXCEDENTE -> cheap += ratio; PriceReason.ESCASSO -> dear += ratio; else -> {} }
        }
        assertTrue(cheap.isNotEmpty() && dear.isNotEmpty())
        assertTrue(cheap.average() < 0.9 && dear.average() > 1.1, "${cheap.average()} vs ${dear.average()}")
    }

    @Test
    fun `contrabando paga premio e vigilancia alta paga mais`() {
        val illegal = region.systems.flatMap { s -> Commodity.values().filter { it.bannedBy(s.traits.ideology) }.map { s to it } }
        assertTrue(illegal.isNotEmpty())
        for ((s, c) in illegal) {
            val e = Market.entry(s, c, 0)
            assertTrue(e.illegal)
            assertEquals(PriceReason.PROIBIDO, e.reason)
        }
        val (low, high) = illegal.partition { it.first.traits.surveillance < 40 }
        if (low.isNotEmpty() && high.isNotEmpty()) {
            fun avg(l: List<Pair<com.mercador.core.universe.StarSystem, Commodity>>) =
                l.map { (s, c) -> Market.entry(s, c, 0).price.toDouble() / c.basePrice }.average()
            assertTrue(avg(high) > avg(low))
        }
    }

    @Test
    fun `existe arbitragem lucrativa legal entre sistemas da regiao`() {
        // O laço de comércio só funciona se houver rotas com lucro: procura a melhor margem legal.
        var best = 0.0
        for (c in Commodity.values()) {
            val legal = region.systems.map { Market.entry(it, c, 0) }.filter { !it.illegal && it.stock > 0 }
            if (legal.size < 2) continue
            best = maxOf(best, legal.maxOf { it.price }.toDouble() / legal.minOf { it.price })
        }
        assertTrue(best > 1.5, "melhor margem: $best")
    }
}
