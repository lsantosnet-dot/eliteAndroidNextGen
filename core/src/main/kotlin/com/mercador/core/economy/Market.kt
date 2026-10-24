package com.mercador.core.economy

import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive
import com.mercador.core.universe.Ideology
import com.mercador.core.universe.StarSystem

enum class Trend { SOBE, ESTAVEL, CAI }

/** Motivo mostrado ao jogador ao lado do preço ("escasso aqui", "excedente"...). */
enum class PriceReason(val label: String) {
    EXCEDENTE("excedente local"), ESCASSO("escasso aqui"), PROIBIDO("proibido: risco"), NORMAL("")
}

data class MarketEntry(
    val commodity: Commodity,
    val price: Int,
    /** Toneladas disponíveis para compra. */
    val stock: Int,
    /** Proibida pela lei local: dá lucro, mas atrai atenção. */
    val illegal: Boolean,
    val trend: Trend,
    val reason: PriceReason,
)

/**
 * Preços de um sistema num dado dia. Função pura de (semente do sistema, mercadoria, dia):
 * mesma entrada, mesmo mercado, e nada precisa ser salvo. O mundo vivido (entregas, embargos)
 * entra depois como deltas sobre este valor (etapa 2).
 */
object Market {
    /** Preços mudam a cada [PERIOD_DAYS] dias. */
    const val PERIOD_DAYS = 7

    fun entries(system: StarSystem, day: Int): List<MarketEntry> = Commodity.values().map { entry(system, it, day) }

    /** [ideology] é a de quem governa hoje (muda numa revolta); por padrão, a gerada. */
    fun entry(system: StarSystem, c: Commodity, day: Int, ideology: Ideology = system.traits.ideology): MarketEntry {
        val period = Math.floorDiv(day, PERIOD_DAYS)
        val now = multiplier(system, c, period, ideology)
        val before = multiplier(system, c, period - 1, ideology)
        val t = system.traits
        val sysKey = system.seedKey()

        val produced = t.economy in c.producedBy
        val demanded = t.economy in c.demandedBy
        val illegal = c.bannedBy(ideology)

        val price = (c.basePrice * now).toInt().coerceAtLeast(1)
        val prodScale = 0.3 + t.productivity / 100.0
        val baseStock = when {
            produced -> 40 * prodScale
            demanded -> 6.0
            else -> 15.0
        }
        val stock = (baseStock * (0.6 + Rng(derive(sysKey, c.ordinal.toLong(), period.toLong(), STOCK)).nextDouble() * 0.8))
            .toInt().coerceAtLeast(if (produced) 3 else 0)

        val trend = when {
            now > before * 1.04 -> Trend.SOBE
            now < before * 0.96 -> Trend.CAI
            else -> Trend.ESTAVEL
        }
        val reason = when {
            illegal -> PriceReason.PROIBIDO
            produced -> PriceReason.EXCEDENTE
            demanded -> PriceReason.ESCASSO
            else -> PriceReason.NORMAL
        }
        return MarketEntry(c, price, if (illegal) stock / 2 else stock, illegal, trend, reason)
    }

    /** Fator sobre o preço base: estrutura do sistema × ruído estável por sistema × ruído por período. */
    private fun multiplier(system: StarSystem, c: Commodity, period: Int, ideology: Ideology): Double {
        val t = system.traits
        val sysKey = system.seedKey()
        var m = 1.0
        if (t.economy in c.producedBy) m *= 0.85 - 0.30 * t.productivity / 100.0      // 0.55..0.85
        if (t.economy in c.demandedBy) m *= 1.20 + 0.25 * (100 - t.stability) / 100.0 // 1.20..1.45
        if (c.bannedBy(ideology)) m *= 1.0 + 0.6 * t.surveillance / 100.0 + 0.2 // prêmio de risco: 1.2..1.8
        // Sistemas de alta tecnologia pagam mais por bens sofisticados.
        if (c.basePrice >= 100) m *= 1.0 + (t.techLevel - 8) * 0.01
        val fixed = 0.95 + Rng(derive(sysKey, c.ordinal.toLong(), FIXED)).nextDouble() * 0.10
        val drift = 0.90 + Rng(derive(sysKey, c.ordinal.toLong(), period.toLong(), DRIFT)).nextDouble() * 0.20
        return m * fixed * drift
    }

    private const val FIXED = 0x51L
    private const val DRIFT = 0xD21L
    private const val STOCK = 0x570CL
}
