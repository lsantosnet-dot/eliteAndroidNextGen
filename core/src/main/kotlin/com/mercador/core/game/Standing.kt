package com.mercador.core.game

import com.mercador.core.universe.Faction
import com.mercador.core.universe.FactionMap
import kotlin.math.roundToInt

/**
 * Atenção (heat) e reputação do jogador por facção (seção 6 do design).
 * Atenção: 0..100, o quanto a facção está de olho nele; esfria com o tempo.
 * Reputação: -100..100; ajudar uma facção custa a confiança das rivais (efeito de segunda ordem).
 */
class Standing(private val map: FactionMap) {
    private val heat = IntArray(map.factions.size)
    private val rep = IntArray(map.factions.size)

    fun heat(f: Faction): Int = heat[f.id]
    fun reputation(f: Faction): Int = rep[f.id]

    fun addHeat(f: Faction, delta: Int) { heat[f.id] = (heat[f.id] + delta).coerceIn(0, 100) }
    fun setHeatAtMost(f: Faction, cap: Int) { heat[f.id] = minOf(heat[f.id], cap) }

    /** Muda a reputação com [f] e propaga metade do efeito, ponderado pela relação, para as demais. */
    fun addReputation(f: Faction, delta: Int) {
        rep[f.id] = (rep[f.id] + delta).coerceIn(-100, 100)
        for (o in map.factions) if (o.id != f.id) {
            val spill = (delta * map.relation(f.id, o.id) / 200.0).roundToInt()
            rep[o.id] = (rep[o.id] + spill).coerceIn(-100, 100)
        }
    }

    internal fun heatValues(): List<Int> = heat.toList()
    internal fun reputationValues(): List<Int> = rep.toList()
    internal fun restore(heat: List<Int>, rep: List<Int>) {
        require(heat.size == this.heat.size && rep.size == this.rep.size) { "número de facções diferente do save" }
        heat.forEachIndexed { i, v -> this.heat[i] = v.coerceIn(0, 100) }
        rep.forEachIndexed { i, v -> this.rep[i] = v.coerceIn(-100, 100) }
    }

    fun cool(days: Int) { for (i in heat.indices) heat[i] = (heat[i] - days * COOL_PER_DAY).coerceAtLeast(0) }

    /** Zona proibida: o poder local já o trata como inimigo (seção 7.2). */
    fun isRestricted(f: Faction): Boolean = rep[f.id] <= RESTRICTED_BELOW

    companion object {
        const val COOL_PER_DAY = 1
        const val RESTRICTED_BELOW = -50
    }
}
