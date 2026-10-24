package com.mercador.core.game

import com.mercador.core.economy.Commodity
import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive
import com.mercador.core.universe.Faction
import com.mercador.core.universe.StarSystem
import com.mercador.core.universe.SystemId
import kotlin.math.ceil

/** Contratos da estação: metas curtas com prazo, pagamento e, às vezes, um dilema. */
enum class ContractKind(val label: String) {
    /** Leve [Contract.tons] de uma mercadoria (comprada por você) ao destino. */
    ENTREGA("Entrega"),
    /** "Sem perguntas": a carga vem lacrada e ocupa o porão; só no fim você descobre o que levou. */
    LACRADA("Carga lacrada"),
    /** Leve o que falta a um sistema em crise; paga menos, mas encurta a crise e rende favor. */
    SOCORRO("Socorro"),
    /**
     * Uma carta lacrada de um governador para a facção rival (vem de uma história, não da estação): não ocupa o
     * porão, paga na entrega e, se a alfândega achar numa revista ou o prazo vencer, o governador não perdoa.
     */
    CARTA("Carta lacrada"),
}

class Contract(
    val id: Long,
    val kind: ContractKind,
    val commodity: Commodity,
    val tons: Int,
    val destination: SystemId,
    val destName: String,
    val deadline: Int,
    val reward: Int,
    /** Facção que paga (e que se lembra de um calote). */
    val client: Int,
    val clientName: String,
) {
    /** O que o jogador vê: a carga lacrada nunca diz o que é. */
    val cargoLabel: String get() = when (kind) {
        ContractKind.LACRADA -> "carga lacrada"
        ContractKind.CARTA -> "carta lacrada"
        else -> commodity.label.lowercase()
    }
    /** O que se leva, para os textos: "5 t de comida", ou "uma carta lacrada". */
    val what: String get() = if (kind == ContractKind.CARTA) "uma carta lacrada" else "$tons t de $cargoLabel"
    fun daysLeft(day: Int) = deadline - day
}

object Contracts {
    /** As ofertas mudam a cada [PERIOD_DAYS] dias em cada estação. */
    const val PERIOD_DAYS = 4
    const val MAX_ACTIVE = 3
    const val OFFERS = 3
    private val sealedGoods = listOf(Commodity.ARMAS, Commodity.DROGAS, Commodity.RELIQUIAS, Commodity.LIVROS)

    /** Ofertas desta estação hoje, sem as já aceitas. Determinístico por sistema e período. */
    fun offers(g: Game): List<Contract> {
        val here = g.location
        val period = Math.floorDiv(g.day, PERIOD_DAYS)
        val range = g.fuelCapacity * 1.6
        val dests = g.world.region.systems.filter { it !== here && g.world.region.distanceLy(here, it) <= range }
        if (dests.isEmpty()) return emptyList()
        val list = ArrayList<Contract>()
        for (i in 0 until OFFERS) {
            val key = derive(here.seedKey(), period.toLong(), i.toLong(), 0xC07)
            if (g.contractTaken(key)) continue
            make(g, Rng(key), key, dests)?.let { list += it }
        }
        return list
    }

    private fun make(g: Game, rng: Rng, id: Long, dests: List<StarSystem>): Contract? {
        val here = g.location
        val w = g.world
        // Socorro primeiro: uma crise ao alcance vira oferta com boa chance.
        val crises = dests.mapNotNull { d -> w.crisisAt(d)?.takeIf { it.kind.relieves && it.need > 0 }?.let { d to it } }
        val roll = rng.nextDouble()
        val client = w.controller(here)
        val person = People.generate(rng, client, "despachante").name
        if (crises.isNotEmpty() && roll < 0.4) {
            val (d, cr) = rng.pick(crises)
            val c = rng.pick(cr.kind.wants.filter { !w.banned(here, it) && !w.banned(d, it) }.ifEmpty { return null })
            val tons = minOf(cr.need, rng.range(6, 12))
            val reward = (tons * c.basePrice * 1.2 + w.region.distanceLy(here, d) * 15).toInt()
            return Contract(id, ContractKind.SOCORRO, c, tons, d.id, d.name, g.day + eta(g, d) + rng.range(5, 8), reward, w.controller(d).id, person)
        }
        val d = rng.pick(dests)
        val dist = w.region.distanceLy(here, d)
        if (roll < 0.7) {
            val wanted = Commodity.values().filter { d.traits.economy in it.demandedBy && !w.banned(d, it) && !w.banned(here, it) }
            val c = if (wanted.isNotEmpty()) rng.pick(wanted) else Commodity.values().filter { !w.banned(d, it) && !w.banned(here, it) }.let { if (it.isEmpty()) return null else rng.pick(it) }
            val tons = rng.range(4, 10)
            val reward = (tons * c.basePrice * 1.6 + dist * 20).toInt()
            return Contract(id, ContractKind.ENTREGA, c, tons, d.id, d.name, g.day + eta(g, d) + rng.range(5, 9), reward, client.id, person)
        }
        // Lacrada: paga muito porque alguém, em algum lugar, chamaria isso de crime.
        val c = rng.pick(sealedGoods)
        val tons = rng.range(3, 7)
        val reward = (250 + tons * c.basePrice * 0.9 + dist * 25).toInt()
        val shady = People.generate(rng, client, "intermediário").name
        return Contract(id, ContractKind.LACRADA, c, tons, d.id, d.name, g.day + eta(g, d) + rng.range(2, 4), reward, client.id, shady)
    }

    /**
     * A carta do governador [governor] (facção [client]) para [rival]: entregue no sistema de [rival] mais perto
     * daqui. Null se [rival] não governa nenhum sistema da região.
     */
    fun letter(g: Game, governor: String, client: Faction, rival: Faction): Contract? {
        val w = g.world
        val d = w.region.systems.filter { it !== g.location && w.controller(it).id == rival.id }
            .minByOrNull { w.region.distanceLy(g.location, it) } ?: return null
        val dist = w.region.distanceLy(g.location, d)
        val id = derive(g.location.seedKey(), g.day.toLong(), 0xCA27A)
        return Contract(id, ContractKind.CARTA, Commodity.LIVROS, 0, d.id, d.name, g.day + eta(g, d) + LETTER_SLACK_DAYS,
            (LETTER_BASE + dist * 30).toInt(), client.id, governor)
    }

    const val LETTER_BASE = 250
    const val LETTER_SLACK_DAYS = 6
    /** Chance de a revista da patrulha achar a carta (o compartimento oculto a esconde sempre). */
    const val LETTER_FOUND = 0.5

    /** Dias até o destino se fosse direto (o prazo dá folga em cima disso). */
    private fun eta(g: Game, d: StarSystem): Int = 1 + ceil(g.world.region.distanceLy(g.location, d) / 2).toInt()
}
