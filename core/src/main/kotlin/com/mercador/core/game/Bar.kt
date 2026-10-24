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
    CRISE("O que vem por aí"),
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
            BarOffer(RumorKind.CRISE, 60),
            BarOffer(RumorKind.SEGREDO, 150 + g.location.traits.corruption, rng.pick(others)),
        )
    }

    /** Melhor destino ao alcance para vender o que o jogador leva (ou, vazio, o melhor lucro por tonelada). */
    fun bestSale(g: Game): Pair<Commodity, StarSystem>? {
        var best: Pair<Commodity, StarSystem>? = null
        var bestGain = 0
        for (dest in g.reachable()) for (c in Commodity.values()) {
            if (g.world.banned(dest, c) || g.isIllegalHere(c)) continue
            val gain = g.world.unitPrice(dest, c, 1.0, Side.SELL) - g.world.unitPrice(g.location, c, 1.0, Side.BUY)
            if (gain > bestGain) { bestGain = gain; best = c to dest }
        }
        return best
    }

    /**
     * Saber de uma crise antes dos outros é lucro: primeiro o que o sorteio do mundo já marcou para os próximos
     * dias (com o mundo vivo), senão a crise ativa mais perto. Sempre verdade, nunca inventado.
     */
    fun crisisRumor(g: Game): String {
        val w = g.world
        if (!w.living) return "Por aqui, nada muda. É o que todos dizem, e é verdade."
        w.upcomingCrises(CRISIS_LOOKAHEAD).firstOrNull()?.let { (day, sk) ->
            val (s, k) = sk
            val what = k.wants.joinToString(" e ") { it.label.lowercase() }
            return "Um piloto bêbado jura: ${k.label.lowercase()} em ${s.name} em ${day - g.day} dia(s). Vai faltar $what."
        }
        val cr = w.crises.minByOrNull { c -> w.region.systems.first { it.id == c.system }.let { w.region.distanceLy(g.location, it) } }
            ?: return "Nenhuma crise à vista. Um mau momento para quem vive delas."
        val what = cr.kind.wants.joinToString(" e ") { it.label.lowercase() }
        return "${cr.kind.label} em ${cr.systemName}: falta $what, por mais ${cr.daysLeft(g.day)} dia(s)." +
            if (cr.need > 0) " Com ${cr.need} t entregues, acaba antes." else ""
    }

    const val CRISIS_LOOKAHEAD = 6
}
