package com.mercador.core.world

import com.mercador.core.economy.Commodity
import com.mercador.core.economy.Market
import com.mercador.core.economy.MarketEntry
import com.mercador.core.universe.Region
import com.mercador.core.universe.StarSystem
import com.mercador.core.universe.SystemId
import kotlin.math.abs
import kotlin.math.roundToInt

enum class Side { BUY, SELL }

/**
 * O mundo vivido: só as diferenças sobre o mundo-fórmula (design, seção 3.2).
 * Medidores = baseline gerado + delta; preços = [Market] + efeito do volume que o jogador movimentou.
 * Os deltas decaem com o tempo, então o mundo se recompõe sozinho.
 */
class World(val region: Region) {
    var day: Int = 0
        private set

    private val meterDeltas = HashMap<SystemId, DoubleArray>()
    private val volumes = HashMap<Pair<SystemId, Commodity>, Double>()

    fun meters(s: StarSystem): Meters {
        val base = MeterModel.baseline(s.traits)
        val d = meterDeltas[s.id]
        val control = clamp(base[0] + (d?.get(0) ?: 0.0))
        val supply = clamp(base[2] + (d?.get(2) ?: 0.0))
        val discontent = clamp(base[1] + (d?.get(1) ?: 0.0) + MeterModel.scarcityPressure(supply))
        return Meters(control.roundToInt(), discontent.roundToInt(), supply.roundToInt())
    }

    /** Toneladas líquidas que o jogador tirou deste mercado (negativo = entregou mais do que levou). */
    fun volume(s: StarSystem, c: Commodity): Double = volumes[s.id to c] ?: 0.0

    /** Mercado exibido ao jogador: preços e estoques já com abastecimento e volume. */
    fun quote(s: StarSystem, c: Commodity): MarketEntry {
        val e = Market.entry(s, c, day)
        val vol = volume(s, c)
        return e.copy(price = unitPrice(s, c, 0.0, Side.BUY).coerceAtLeast(1), stock = (e.stock - vol).toInt().coerceAtLeast(0))
    }

    fun quotes(s: StarSystem): List<MarketEntry> = Commodity.values().map { quote(s, it) }

    /**
     * Preço unitário de uma transação de [tons]: usa o preço no meio do deslocamento de volume,
     * então comprar e revender no mesmo lugar nunca dá lucro (venda ainda paga [SELL_SPREAD] a menos).
     */
    fun unitPrice(s: StarSystem, c: Commodity, tons: Double, side: Side): Int {
        val base = Market.entry(s, c, day).price.toDouble()
        val supply = meters(s).supply
        val scarcity = if (c in MeterModel.essential) 1.0 + (50 - supply).coerceAtLeast(0) / 100.0 else 1.0
        val shift = if (side == Side.BUY) tons / 2 else -tons / 2
        val volMult = (1.0 + (volume(s, c) + shift) * VOLUME_IMPACT).coerceIn(0.6, 1.5)
        val spread = if (side == Side.SELL) SELL_SPREAD else 1.0
        return (base * scarcity * volMult * spread).roundToInt().coerceAtLeast(1)
    }

    /** Registra a transação: move o volume e empurra os medidores. */
    fun recordTrade(s: StarSystem, c: Commodity, tons: Int, side: Side) {
        val key = s.id to c
        volumes[key] = volume(s, c) + if (side == Side.BUY) tons else -tons
        val d = meterDeltas.getOrPut(s.id) { DoubleArray(3) }
        val p = MeterModel.push(c)
        val k = tons / 10.0
        if (side == Side.SELL) {
            d[0] += p.control * k; d[1] += p.discontent * k; d[2] += p.supply * k
        } else {
            d[2] -= p.supply * k * MeterModel.REMOVAL_FACTOR
        }
        d[0] = d[0].coerceIn(-MAX_DELTA, MAX_DELTA); d[1] = d[1].coerceIn(-MAX_DELTA, MAX_DELTA); d[2] = d[2].coerceIn(-MAX_DELTA, MAX_DELTA)
    }

    /** Empurra os medidores diretamente (eventos de história, não comércio). */
    fun pushMeters(s: StarSystem, control: Int, discontent: Int, supply: Int) {
        val d = meterDeltas.getOrPut(s.id) { DoubleArray(3) }
        d[0] = (d[0] + control).coerceIn(-MAX_DELTA, MAX_DELTA)
        d[1] = (d[1] + discontent).coerceIn(-MAX_DELTA, MAX_DELTA)
        d[2] = (d[2] + supply).coerceIn(-MAX_DELTA, MAX_DELTA)
    }

    /** Diferenças do mundo vivido, para o salvamento. */
    internal fun exportDeltas(): Pair<Map<SystemId, DoubleArray>, Map<Pair<SystemId, Commodity>, Double>> =
        meterDeltas.mapValues { it.value.copyOf() } to HashMap(volumes)

    internal fun restore(day: Int, deltas: Map<SystemId, DoubleArray>, vols: Map<Pair<SystemId, Commodity>, Double>) {
        this.day = day
        meterDeltas.clear(); deltas.forEach { (k, v) -> meterDeltas[k] = v.copyOf() }
        volumes.clear(); volumes.putAll(vols)
    }

    fun advance(days: Int) {
        repeat(days) {
            day++
            meterDeltas.values.forEach { d -> for (i in d.indices) d[i] *= METER_DECAY }
            meterDeltas.values.removeAll { d -> d.all { abs(it) < 0.05 } }
            val it = volumes.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                e.setValue(e.value * VOLUME_DECAY)
                if (abs(e.value) < 0.05) it.remove()
            }
        }
    }

    private fun clamp(v: Double) = v.coerceIn(0.0, 100.0)

    companion object {
        const val VOLUME_IMPACT = 0.01     // +1% de preço por tonelada tirada
        const val SELL_SPREAD = 0.95
        const val METER_DECAY = 0.97       // meia-vida de ~23 dias
        const val VOLUME_DECAY = 0.94      // meia-vida de ~11 dias
        const val MAX_DELTA = 60.0
    }
}
