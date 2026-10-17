package com.mercador.core.game

import com.mercador.core.economy.Commodity
import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive
import com.mercador.core.universe.Faction
import com.mercador.core.universe.StarSystem
import com.mercador.core.world.Side

/** Informação é mercadoria (seção 4): rumores comprados no bar valem lucro. */
enum class RumorKind(val label: String) {
    ONDE_VENDER("Onde vender melhor"),
    RISCO_DA_ROTA("Risco de abordagem num destino"),
    SEGREDO("Um segredo sobre uma facção"),
}

class BarOffer(val kind: RumorKind, val price: Int, val about: Faction? = null)

/** O que o jogador compra no bar: texto verdadeiro calculado do mundo, nunca inventado. */
object Bar {
    fun offers(g: Game): List<BarOffer> {
        val rng = Rng(derive(g.location.seedKey(), g.day.toLong(), 0xBA2))
        val others = g.factions.factions.filter { it.id != g.faction.id }
        return listOf(
            BarOffer(RumorKind.ONDE_VENDER, 40),
            BarOffer(RumorKind.RISCO_DA_ROTA, 30),
            BarOffer(RumorKind.SEGREDO, 150 + g.location.traits.corruption, rng.pick(others)),
        )
    }

    /** Melhor destino ao alcance para vender o que o jogador leva (ou, vazio, o melhor lucro por tonelada). */
    fun bestSale(g: Game): Pair<Commodity, StarSystem>? {
        var best: Pair<Commodity, StarSystem>? = null
        var bestGain = 0
        for (dest in g.reachable()) for (c in Commodity.values()) {
            if (c.bannedBy(dest.traits.ideology) || c.bannedBy(g.location.traits.ideology)) continue
            val gain = g.world.unitPrice(dest, c, 1.0, Side.SELL) - g.world.unitPrice(g.location, c, 1.0, Side.BUY)
            if (gain > bestGain) { bestGain = gain; best = c to dest }
        }
        return best
    }
}
