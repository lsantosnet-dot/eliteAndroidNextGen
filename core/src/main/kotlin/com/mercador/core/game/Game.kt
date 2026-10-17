package com.mercador.core.game

import com.mercador.core.economy.Commodity
import com.mercador.core.universe.StarSystem
import com.mercador.core.world.MeterModel
import com.mercador.core.world.Side
import com.mercador.core.world.World
import java.util.EnumMap
import kotlin.math.ceil

/** Uma linha do livro de contas: o registro permanente do que o jogador fez (seção 5). */
data class LedgerEntry(val day: Int, val systemName: String, val text: String)

enum class Failure {
    SEM_DINHEIRO, SEM_PORAO, SEM_ESTOQUE, SEM_CARGA, SEM_COMBUSTIVEL, FORA_DE_ALCANCE, QUANTIDADE_INVALIDA, TANQUE_CHEIO
}

sealed interface Outcome {
    data class Ok(val credits: Int) : Outcome
    data class Fail(val reason: Failure) : Outcome
}

/** Estado do mercador solitário e o laço comprar → viajar → vender. */
class Game(val world: World, start: StarSystem, var credits: Int = START_CREDITS) {
    var location: StarSystem = start
        private set
    var fuel: Int = FUEL_CAPACITY
        private set
    val cargo: MutableMap<Commodity, Int> = EnumMap(Commodity::class.java)
    private val _ledger = ArrayList<LedgerEntry>()
    val ledger: List<LedgerEntry> get() = _ledger

    val day: Int get() = world.day
    val holdUsed: Int get() = cargo.values.sum()
    val holdFree: Int get() = HOLD_CAPACITY - holdUsed

    fun fuelCost(to: StarSystem): Int = ceil(world.region.distanceLy(location, to)).toInt().coerceAtLeast(1)
    fun travelDays(to: StarSystem): Int = 1 + ceil(world.region.distanceLy(location, to) / 2).toInt()

    /** Destinos que o combustível atual alcança. */
    fun reachable(): List<StarSystem> =
        world.region.systems.filter { it !== location && fuelCost(it) <= fuel }.sortedBy { world.region.distanceLy(location, it) }

    fun buy(c: Commodity, tons: Int): Outcome {
        if (tons <= 0) return Outcome.Fail(Failure.QUANTIDADE_INVALIDA)
        if (tons > world.quote(location, c).stock) return Outcome.Fail(Failure.SEM_ESTOQUE)
        if (tons > holdFree) return Outcome.Fail(Failure.SEM_PORAO)
        val cost = world.unitPrice(location, c, tons.toDouble(), Side.BUY) * tons
        if (cost > credits) return Outcome.Fail(Failure.SEM_DINHEIRO)
        credits -= cost
        cargo[c] = (cargo[c] ?: 0) + tons
        world.recordTrade(location, c, tons, Side.BUY)
        return Outcome.Ok(credits)
    }

    fun sell(c: Commodity, tons: Int): Outcome {
        if (tons <= 0) return Outcome.Fail(Failure.QUANTIDADE_INVALIDA)
        val have = cargo[c] ?: 0
        if (tons > have) return Outcome.Fail(Failure.SEM_CARGA)
        val before = world.meters(location)
        credits += world.unitPrice(location, c, tons.toDouble(), Side.SELL) * tons
        if (have == tons) cargo.remove(c) else cargo[c] = have - tons
        world.recordTrade(location, c, tons, Side.SELL)
        recordConsequence(c, tons, before)
        return Outcome.Ok(credits)
    }

    fun refuel(): Outcome {
        val missing = FUEL_CAPACITY - fuel
        if (missing == 0) return Outcome.Fail(Failure.TANQUE_CHEIO)
        val cost = missing * FUEL_PRICE
        if (cost > credits) return Outcome.Fail(Failure.SEM_DINHEIRO)
        credits -= cost
        fuel = FUEL_CAPACITY
        return Outcome.Ok(credits)
    }

    fun travel(to: StarSystem): Outcome {
        if (to === location || to !in world.region.systems) return Outcome.Fail(Failure.FORA_DE_ALCANCE)
        val cost = fuelCost(to)
        if (cost > FUEL_CAPACITY) return Outcome.Fail(Failure.FORA_DE_ALCANCE)
        if (cost > fuel) return Outcome.Fail(Failure.SEM_COMBUSTIVEL)
        fuel -= cost
        world.advance(travelDays(to))
        location = to
        return Outcome.Ok(credits)
    }

    /** O livro anota o que importa: entregas em racionamento, armas num povo faminto, e a revolta. */
    private fun recordConsequence(c: Commodity, tons: Int, before: com.mercador.core.world.Meters) {
        val name = location.name
        if (c in MeterModel.essential && before.rationing && tons >= 5) {
            note("Entregou $tons t de ${c.label} durante o racionamento.")
        }
        if (c == Commodity.ARMAS && tons >= 5 && before.discontent >= 60) {
            note("Vendeu $tons t de armas a um poder que o povo já não suporta.")
        }
        if (!before.unrest && world.meters(location).unrest) {
            note("Seu carregamento de ${c.label} foi a última gota: o sistema está à beira da revolta.")
        }
    }

    private fun note(text: String) { _ledger += LedgerEntry(day, location.name, text) }

    companion object {
        const val START_CREDITS = 1000
        const val HOLD_CAPACITY = 20
        const val FUEL_CAPACITY = 20
        const val FUEL_PRICE = 10
    }
}
