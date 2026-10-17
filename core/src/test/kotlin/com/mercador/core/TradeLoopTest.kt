package com.mercador.core

import com.mercador.core.economy.Commodity
import com.mercador.core.game.*
import com.mercador.core.universe.Region
import com.mercador.core.universe.StarSystem
import com.mercador.core.universe.Universe
import com.mercador.core.world.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TradeLoopTest {
    private fun newGame(seed: Long = 2024): Game {
        val world = World(Region.around(Universe(seed)))
        return Game(world, world.region.systems[0], stories = false)
    }

    @Test
    fun `medidores de nascenca ficam em 0 a 100 e dependem dos tracos`() {
        val w = World(Region.around(Universe(1)))
        for (s in w.region.systems) {
            val m = w.meters(s)
            for (v in listOf(m.control, m.discontent, m.supply)) assertTrue(v in 0..100)
        }
        assertTrue(w.region.systems.map { w.meters(it).control }.toSet().size > 5)
    }

    @Test
    fun `entregar comida baixa descontentamento e sobe abastecimento, e o efeito decai`() {
        val g = newGame()
        val s = g.location
        val before = g.world.meters(s)
        g.world.recordTrade(s, Commodity.COMIDA, 40, Side.SELL)
        val after = g.world.meters(s)
        assertTrue(after.supply > before.supply)
        assertTrue(after.discontent <= before.discontent)
        g.world.advance(200)
        assertEquals(before, g.world.meters(s))
    }

    @Test
    fun `armas aumentam controle`() {
        val g = newGame()
        val s = g.location
        val before = g.world.meters(s).control
        g.world.recordTrade(s, Commodity.ARMAS, 30, Side.SELL)
        assertTrue(g.world.meters(s).control > before || before == 100)
    }

    @Test
    fun `falta de abastecimento alimenta descontentamento e encarece bens essenciais`() {
        val g = newGame()
        val s = g.location
        val d0 = g.world.meters(s).discontent
        val p0 = g.world.unitPrice(s, Commodity.COMIDA, 0.0, Side.BUY)
        repeat(10) { g.world.recordTrade(s, Commodity.COMIDA, 100, Side.BUY) } // esvazia o sistema
        assertTrue(g.world.meters(s).supply < 40)
        assertTrue(g.world.meters(s).discontent >= d0)
        assertTrue(g.world.unitPrice(s, Commodity.COMIDA, 0.0, Side.BUY) > p0)
    }

    @Test
    fun `comprar e revender no mesmo lugar nunca da lucro`() {
        for (c in Commodity.values()) {
            val g = newGame()
            val tons = minOf(g.world.quote(g.location, c).stock, 20)
            if (tons <= 0) continue
            g.credits = 100_000
            val before = g.credits
            assertTrue(g.buy(c, tons) is Outcome.Ok)
            assertTrue(g.sell(c, tons) is Outcome.Ok)
            assertTrue(g.credits < before, "$c: $before -> ${g.credits}")
        }
    }

    @Test
    fun `compra respeita dinheiro, porao e estoque`() {
        val g = newGame()
        val c = Commodity.COMIDA
        assertEquals(Outcome.Fail(Failure.QUANTIDADE_INVALIDA), g.buy(c, 0))
        assertEquals(Outcome.Fail(Failure.SEM_ESTOQUE), g.buy(c, 10_000))
        assertEquals(Outcome.Fail(Failure.SEM_CARGA), g.sell(c, 1))
        g.credits = 0
        val stocked = Commodity.values().first { g.world.quote(g.location, it).stock > 0 }
        assertEquals(Outcome.Fail(Failure.SEM_DINHEIRO), g.buy(stocked, 1))
    }

    @Test
    fun `viagem gasta combustivel, passa dias e muda de lugar`() {
        val g = newGame()
        val dest = g.reachable().first()
        val fuel0 = g.fuel; val day0 = g.day; val cost = g.fuelCost(dest)
        assertEquals(Outcome.Ok(g.credits), g.travel(dest))
        assertEquals(dest, g.location)
        assertEquals(fuel0 - cost, g.fuel)
        assertTrue(g.day > day0)
        assertEquals(Outcome.Fail(Failure.FORA_DE_ALCANCE), g.travel(dest))
        g.credits = 10_000
        assertTrue(g.refuel() is Outcome.Ok)
        assertEquals(Game.FUEL_CAPACITY, g.fuel)
        assertEquals(10_000 - cost * Game.FUEL_PRICE, g.credits)
        assertEquals(Outcome.Fail(Failure.TANQUE_CHEIO), g.refuel())
    }

    @Test
    fun `livro de contas anota entrega em racionamento e nunca apaga`() {
        val g = newGame()
        val s = g.location
        // força racionamento drenando o abastecimento
        repeat(10) { g.world.recordTrade(s, Commodity.COMIDA, 100, Side.BUY) }
        assertTrue(g.world.meters(s).rationing)
        g.cargo[Commodity.REMEDIO] = 10
        g.sell(Commodity.REMEDIO, 10)
        assertEquals(1, g.ledger.size)
        assertTrue(g.ledger[0].text.contains("racionamento"))
        g.world.advance(100)
        assertEquals(1, g.ledger.size)
    }
}
