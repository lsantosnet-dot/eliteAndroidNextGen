package com.mercador.core

import com.mercador.core.economy.Commodity
import com.mercador.core.game.*
import com.mercador.core.universe.Region
import com.mercador.core.universe.StarSystem
import com.mercador.core.universe.Universe
import com.mercador.core.world.Side
import com.mercador.core.world.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * A campanha precisa ser vencível por quem joga bem e não trivial para quem só compra e vende sem pensar.
 * Um mercador guloso, que melhora a nave e paga o agiota, joga a campanha inteira com mundo vivo e histórias.
 */
class CampaignSimTest {
    class Run(val paidOff: Boolean, val day: Int, val credits: Int, val paid: Int, val total: Int, val missed: Int, val crises: Int, val revolts: Int, val stories: Int)

    /** Resolve qualquer decisão pendente do jeito mais barato que um jogador apressado escolheria. */
    private fun settle(g: Game, o: Outcome, counter: IntArray) {
        var out = o
        var guard = 0
        while (guard++ < 20) {
            out = when {
                g.pendingStory != null -> { counter[0]++; g.chooseStory(g.pendingStory!!.choices.indexOfLast { it.available }) }
                g.pursuit != null -> g.respond(if (Response.SUBORNAR in g.responses()) Response.SUBORNAR else Response.ENTREGAR_SE)
                g.confrontation != null -> g.act(Posture.RESISTIR)
                else -> return
            }
        }
        fail<Unit>("decisão sem fim: $out")
    }

    private fun plan(g: Game): Triple<Commodity, StarSystem, Int>? {
        var best: Triple<Commodity, StarSystem, Int>? = null
        var bestGain = 0
        val budget = g.credits - 200
        for (dest in g.reachable()) for (c in Commodity.values()) {
            if (g.isIllegalHere(c) || g.world.banned(dest, c)) continue
            val stock = g.world.quote(g.location, c).stock
            val tons = minOf(stock, g.holdFree, budget / g.world.unitPrice(g.location, c, 0.0, Side.BUY).coerceAtLeast(1))
            if (tons <= 0) continue
            val gain = (g.world.unitPrice(dest, c, tons.toDouble(), Side.SELL) - g.world.unitPrice(g.location, c, tons.toDouble(), Side.BUY)) * tons -
                g.fuelCost(dest) * Game.FUEL_PRICE
            if (gain > bestGain) { bestGain = gain; best = Triple(c, dest, tons) }
        }
        return best
    }

    fun play(seed: Long): Run {
        val world = World(Region.around(Universe(seed)), living = true)
        val g = Game(world, world.region.systems[0], campaign = true)
        val c = g.campaign!!
        val counter = IntArray(1)
        var stuck = 0
        while (!c.over && stuck < 30) {
            for (k in g.cargo.keys.toList()) g.ownTons(k).takeIf { it > 0 }?.let { g.sell(k, it) }
            g.refuel()
            // melhora a nave quando sobra dinheiro, mas só depois de pagar a parcela que está perto
            val next = c.next()
            val reserve = 1_500 + (next?.second ?: 0)
            for (u in listOf(Upgrade.PORAO, Upgrade.MOTOR, Upgrade.TANQUE)) {
                val price = u.priceFor(g.level(u)) ?: continue
                if (u.soldAt(g.location) && g.credits - price > reserve) g.buyUpgrade(u)
            }
            if (next != null && (next.second > 0 && next.first - g.day <= 30)) g.payCreditor(minOf(next.second, g.credits - 800))
            if (g.credits > 8_000) g.payCreditor(g.credits - 6_000)
            val p = plan(g)
            val dest = p?.second ?: g.reachable().firstOrNull()
            if (p != null) g.buy(p.first, p.third)
            if (dest == null) { stuck++; g.haulFreight(); continue }
            val o = g.travel(dest)
            if (o is Outcome.Fail) { stuck++; g.haulFreight(); continue }
            stuck = 0
            settle(g, o, counter)
            if (g.day > c.deadline + 5) break
        }
        val news = g.news
        return Run(c.outcome == CampaignOutcome.QUITADA, g.day, g.credits, c.paid, c.total, c.missed,
            news.count { it.text.contains(": falta") }, news.count { it.text.startsWith("Revolta") }, counter[0])
    }

    @Test
    fun `a campanha termina, e um bom mercador consegue quitar a divida na maioria dos universos`() {
        val runs = (1L..8L).map { it to play(it) }
        for ((s, r) in runs) println("campanha seed $s: quitou=${r.paidOff} dia=${r.day} pago=${r.paid}/${r.total} perdidas=${r.missed} cr=${r.credits} crises=${r.crises} revoltas=${r.revolts} histórias=${r.stories}")
        val won = runs.count { it.second.paidOff }
        assertTrue(won >= 5, "a campanha está difícil demais: $won/8")
        // não trivial: ninguém quita na metade do prazo
        assertTrue(runs.none { it.second.paidOff && it.second.day < Campaign.DEADLINE / 3 }, "fácil demais")
        // o mundo se mexe: crises acontecem, e histórias aparecem com frequência
        assertTrue(runs.all { it.second.crises >= 5 }, "poucas crises")
        assertTrue(runs.all { it.second.stories >= 20 }, "poucas histórias")
    }
}
