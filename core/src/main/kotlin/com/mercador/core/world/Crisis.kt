package com.mercador.core.world

import com.mercador.core.economy.Commodity
import com.mercador.core.universe.SystemId

/**
 * Crises com prazo (fome, epidemia, bloqueio...): inflacionam o que falta, barateiam o que sobra e
 * empurram os medidores do sistema. As de socorro ([need] > 0) acabam antes se o jogador entregar o que falta.
 */
enum class CrisisKind(
    val label: String,
    /** O que falta: o preço sobe [mult] vezes e o estoque some. */
    val wants: Set<Commodity>,
    val mult: Double,
    /** O que sobra: o preço cai e o estoque cresce (uma chance de comprar barato). */
    val surplus: Set<Commodity> = emptySet(),
    /** Toneladas que encerram a crise antes do prazo; 0 = só o tempo resolve. */
    val relief: IntRange = 0..0,
    val control: Int = 0,
    val discontent: Int = 0,
    val supply: Int = 0,
) {
    FOME("Fome", setOf(Commodity.COMIDA, Commodity.AGUA), 2.6, relief = 18..30, discontent = 15, supply = -25),
    EPIDEMIA("Epidemia", setOf(Commodity.REMEDIO), 3.0, relief = 12..20, discontent = 10, supply = -12),
    BLOQUEIO("Bloqueio", setOf(Commodity.COMBUSTIVEL, Commodity.COMIDA, Commodity.MAQUINAS), 1.9, relief = 20..32, control = 10, supply = -20),
    GUERRA("Guerra", setOf(Commodity.ARMAS, Commodity.COMBUSTIVEL), 2.2, control = 15, discontent = 6),
    FESTIVAL("Festival", setOf(Commodity.LUXO, Commodity.RELIQUIAS, Commodity.DROGAS), 1.8, surplus = setOf(Commodity.COMIDA)),
    DESCOBERTA("Veio descoberto", setOf(Commodity.MAQUINAS, Commodity.COMIDA), 1.7, surplus = setOf(Commodity.MINERIO, Commodity.COMBUSTIVEL), supply = 5);

    val relieves: Boolean get() = relief.last > 0

    companion object { const val SURPLUS_MULT = 0.55 }
}

class Crisis(val kind: CrisisKind, val system: SystemId, val systemName: String, val start: Int, val end: Int, var need: Int) {
    fun daysLeft(day: Int) = (end - day).coerceAtLeast(0)
    fun priceMult(c: Commodity): Double = when (c) {
        in kind.wants -> kind.mult
        in kind.surplus -> CrisisKind.SURPLUS_MULT
        else -> 1.0
    }
}

/** Notícia do mundo vivido (crise, revolta): o que mudou sem o jogador ter feito nada, ou por causa dele. */
data class WorldNews(val day: Int, val system: SystemId, val systemName: String, val text: String, val kind: NewsKind)

enum class NewsKind { CRISE_COMECA, CRISE_ACABA, CRISE_SOCORRIDA, REVOLTA }
