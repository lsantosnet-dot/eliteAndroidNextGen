package com.mercador.core.world

import com.mercador.core.economy.Commodity
import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive
import com.mercador.core.universe.Ideology
import com.mercador.core.universe.SystemTraits

/** Os três medidores de um sistema (seção 5 do design), de 0 a 100. */
data class Meters(val control: Int, val discontent: Int, val supply: Int) {
    /** Descontentamento no limite: a revolta é um evento da etapa 3 em diante. */
    val unrest: Boolean get() = discontent >= UNREST_THRESHOLD

    /** Racionamento: abastecimento tão baixo que as entregas de bens essenciais pesam no livro. */
    val rationing: Boolean get() = supply < RATIONING_THRESHOLD

    companion object {
        const val UNREST_THRESHOLD = 90
        const val RATIONING_THRESHOLD = 35
    }
}

/** Quanto uma tonelada entregue empurra cada medidor, em pontos por 10 t. */
data class Push(val control: Double = 0.0, val discontent: Double = 0.0, val supply: Double = 0.0)

object MeterModel {
    /** Medidores de nascença: saem dos traços do sistema (parte "gerada", não é salva). */
    fun baseline(t: SystemTraits, systemKey: Long): DoubleArray {
        val control = t.surveillance * 0.6 + (if (t.ideology == Ideology.AUTORITARIA) 25 else 0) + 10
        var supply = 25 + t.productivity * 0.5 + t.techLevel
        var discontent = (100 - t.stability) * 0.6 + control * 0.2
        // Parte dos sistemas nasce em crise ou tensão, senão o mundo seria calmo demais: sem racionamento nem
        // descontentamento, as histórias e os ledgers que dependem deles nunca apareceriam. Sorteio por sistema,
        // numa trilha própria, então os traços gerados continuam os mesmos.
        val roll = Rng(derive(systemKey, 0xC215)).nextDouble()
        if (roll < CRISIS_SHARE) { supply -= 30 + 10 * Rng(derive(systemKey, 0xC216)).nextDouble(); discontent += 22 }
        else if (roll < CRISIS_SHARE + TENSION_SHARE) { discontent += 14; supply -= 8 }
        return doubleArrayOf(control.coerceIn(0.0, 100.0), discontent.coerceIn(0.0, 100.0), supply.coerceIn(0.0, 100.0))
    }

    /** Fração de sistemas que nascem em crise (racionamento) e em tensão (descontentamento alto). */
    const val CRISIS_SHARE = 0.15
    const val TENSION_SHARE = 0.15

    /** Comida baixa o descontentamento, armas aumentam o controle, remédio ganha lealdade. */
    fun push(c: Commodity): Push = when (c) {
        Commodity.COMIDA -> Push(discontent = -3.0, supply = 4.0)
        Commodity.AGUA -> Push(discontent = -2.0, supply = 3.0)
        Commodity.REMEDIO -> Push(discontent = -3.0, supply = 4.0)
        Commodity.COMBUSTIVEL -> Push(supply = 3.0)
        Commodity.ARMAS -> Push(control = 4.0, discontent = 1.0, supply = 2.0)
        Commodity.DROGAS -> Push(control = -2.0, discontent = 1.0)
        Commodity.LUXO -> Push(control = 1.0, discontent = 2.0)
        Commodity.LIVROS -> Push(control = -2.0, discontent = 2.0)
        Commodity.MAQUINAS, Commodity.ELETRONICOS, Commodity.MINERIO -> Push(supply = 1.0)
        Commodity.RELIQUIAS -> Push()
    }

    /** Bens cujo preço sobe quando o abastecimento do sistema cai. */
    val essential: Set<Commodity> = setOf(Commodity.COMIDA, Commodity.AGUA, Commodity.REMEDIO, Commodity.COMBUSTIVEL)

    /** Comprar do sistema só tira abastecimento (metade do efeito de entregar); não mexe nos outros medidores. */
    const val REMOVAL_FACTOR = 0.5

    /** Falta de abastecimento alimenta o descontentamento. */
    fun scarcityPressure(supply: Double): Double = (40 - supply).coerceAtLeast(0.0) * 0.5
}
