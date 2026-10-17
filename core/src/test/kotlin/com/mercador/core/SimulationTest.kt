package com.mercador.core

import com.mercador.core.economy.Commodity
import com.mercador.core.game.Game
import com.mercador.core.game.Outcome
import com.mercador.core.game.Response
import com.mercador.core.universe.Region
import com.mercador.core.universe.StarSystem
import com.mercador.core.universe.Universe
import com.mercador.core.world.Side
import com.mercador.core.world.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Risco R2 do design: a economia não pode quebrar. Um mercador guloso joga 1 ano em várias galáxias. */
class SimulationTest {
    private data class Result(val profit: Int, val trips: Int, val maxDiscontent: Int, val unrestSystems: Int)

    private fun play(seed: Long, days: Int = 365): Result {
        val world = World(Region.around(Universe(seed)))
        val g = Game(world, world.region.systems[0])
        var trips = 0
        var maxD = 0
        var stuck = 0
        while (world.day < days && stuck < 5) {
            for ((c, t) in g.cargo.toMap()) g.sell(c, t)
            g.refuel()
            val plan = bestPlan(g)
            if (plan == null) { stuck++; world.advance(3); continue }
            val (c, dest, tons) = plan
            g.buy(c, tons)
            if (g.travel(dest) is Outcome.Ok) { trips++; stuck = 0 } else stuck++
            for (s in world.region.systems) maxD = maxOf(maxD, world.meters(s).discontent)
        }
        for ((c, t) in g.cargo.toMap()) g.sell(c, t)
        // invariantes
        for (s in world.region.systems) {
            val m = world.meters(s)
            for (v in listOf(m.control, m.discontent, m.supply)) assertTrue(v in 0..100, "$v")
            for (e in world.quotes(s)) {
                assertTrue(e.price in 1..e.commodity.basePrice * 6, "${s.name} ${e.commodity} ${e.price}")
                assertTrue(e.stock >= 0)
            }
        }
        return Result(g.credits - Game.START_CREDITS, trips, maxD, world.region.systems.count { world.meters(it).unrest })
    }

    private fun bestPlan(g: Game): Triple<Commodity, StarSystem, Int>? {
        var best: Triple<Commodity, StarSystem, Int>? = null
        var bestGain = 0
        for (dest in g.reachable()) for (c in Commodity.values()) {
            if (c.bannedBy(g.location.traits.ideology) || c.bannedBy(dest.traits.ideology)) continue
            val stock = g.world.quote(g.location, c).stock
            val tons = minOf(stock, g.holdFree, g.credits / g.world.unitPrice(g.location, c, 0.0, Side.BUY).coerceAtLeast(1))
            if (tons <= 0) continue
            val cost = g.world.unitPrice(g.location, c, tons.toDouble(), Side.BUY)
            val revenue = g.world.unitPrice(dest, c, tons.toDouble(), Side.SELL)
            val gain = (revenue - cost) * tons - g.fuelCost(dest) * Game.FUEL_PRICE
            if (gain > bestGain) { bestGain = gain; best = Triple(c, dest, tons) }
        }
        return best
    }

    @Test
    fun `um ano de comercio guloso e lucrativo e nao quebra a economia`() {
        val results = (1L..6L).map { play(it) }
        println("simulação 365 dias: $results")
        for (r in results) {
            assertTrue(r.trips > 10, "poucas viagens: $r")
            assertTrue(r.profit > 0, "sem lucro: $r")
            // sem inflação descontrolada: um ano não rende mais que ~150x o capital inicial
            assertTrue(r.profit < Game.START_CREDITS * 150, "lucro explosivo: $r")
        }
    }

    /** Contrabandista: leva bens proibidos no destino; suborna se puder, senão entrega-se. */
    private fun smuggle(seed: Long, days: Int = 365): Triple<Int, Int, Int> {
        val world = World(Region.around(Universe(seed)))
        val g = Game(world, world.region.systems[0])
        var chases = 0
        var stuck = 0
        while (world.day < days && stuck < 5) {
            for ((c, t) in g.cargo.toMap()) g.sell(c, t)
            if (g.credits > g.debt + 500) g.repay()
            g.takeLoan(); g.haulFreight()
            g.refuel()
            var best: Triple<Commodity, StarSystem, Int>? = null
            var bestGain = 0
            for (dest in g.reachable()) for (c in Commodity.values()) {
                if (c.bannedBy(g.location.traits.ideology)) continue
                val stock = g.world.quote(g.location, c).stock
                val tons = minOf(stock, g.holdFree, g.credits / g.world.unitPrice(g.location, c, 0.0, Side.BUY).coerceAtLeast(1))
                if (tons <= 0) continue
                val gain = (g.world.unitPrice(dest, c, tons.toDouble(), Side.SELL) - g.world.unitPrice(g.location, c, tons.toDouble(), Side.BUY)) * tons -
                    g.fuelCost(dest) * Game.FUEL_PRICE
                if (gain > bestGain) { bestGain = gain; best = Triple(c, dest, tons) }
            }
            val plan = best
            if (plan == null) { stuck++; world.advance(3); continue }
            g.buy(plan.first, plan.third)
            when (val out = g.travel(plan.second)) {
                is Outcome.Pursued -> {
                    chases++
                    val r = if (Response.SUBORNAR in g.responses()) Response.SUBORNAR else Response.ENTREGAR_SE
                    g.respond(r)
                    g.takeLoan()
                }
                is Outcome.Ok -> stuck = 0
                else -> stuck++
            }
        }
        assertTrue(stuck < 5, "travado: seed $seed credits=${g.credits} debt=${g.debt}")
        assertNull(g.pursuit)
        return Triple(g.credits - g.debt - Game.START_CREDITS, chases, g.ledger.size)
    }

    @Test
    fun `contrabandista e perseguido mas a economia segue estavel`() {
        val results = (1L..6L).map { smuggle(it) }
        println("contrabandista 365 dias (lucro, perseguições, linhas no livro): $results")
        // o bot ignora a atenção e é pego várias vezes: pode terminar no vermelho, mas nunca travado (ver "travado" em smuggle)
        for ((profit, _, _) in results) assertTrue(profit < Game.START_CREDITS * 150, "lucro explosivo: $profit")
    }
}
