package com.mercador.core.game

import com.mercador.core.universe.Economy
import com.mercador.core.universe.StarSystem

/**
 * Melhorias da nave: o que o dinheiro compra além de mais carga. Cada uma tem níveis com preço crescente
 * e só se vende em certos estaleiros (alta tecnologia para as legais, corrupção para as outras), o que dá
 * motivo para ir a lugares específicos.
 */
enum class Upgrade(val label: String, val effect: String, val prices: List<Int>) {
    PORAO("Porão ampliado", "+${HOLD_STEP} t de carga por nível", listOf(1_500, 4_000, 9_000)),
    TANQUE("Tanque extra", "+${FUEL_STEP} de combustível por nível: saltos mais longos", listOf(1_200, 3_500)),
    MOTOR("Motor de salto", "viagens ${MOTOR_STEP_PCT}% mais curtas por nível", listOf(2_500, 7_000)),
    COMPARTIMENTO("Compartimento oculto", "esconde $HIDDEN_STEP t de contrabando da revista por nível", listOf(2_000, 6_000)),
    TRANSPONDER("Transponder falso", "patrulhas te abordam ${TRANSPONDER_CUT_PCT}% menos", listOf(6_000)),
    LASER("Laser de combate", "o laser de pulso vira de feixe (nível 1) e militar (nível 2)", listOf(3_000, 8_000));

    val maxLevel: Int get() = prices.size

    /** Laser do voo (byte LASER do Elite) com este nível de [LASER]: pulso, feixe ou militar. */
    companion object {
        fun laserFor(level: Int): Int = when (level) {
            0 -> com.mercador.core.flight.PlayerShip.PULSE_LASER
            1 -> com.mercador.core.flight.PlayerShip.BEAM_LASER
            else -> com.mercador.core.flight.PlayerShip.MILITARY_LASER
        }
    }

    /** Preço do próximo nível, ou null se já está no máximo. */
    fun priceFor(level: Int): Int? = prices.getOrNull(level)

    /** Estaleiros legais exigem tecnologia; o mercado negro (compartimento, transponder) exige corrupção. */
    fun soldAt(s: StarSystem): Boolean = when (this) {
        COMPARTIMENTO, TRANSPONDER -> s.traits.corruption >= BLACK_MARKET_CORRUPTION
        else -> s.traits.techLevel >= SHIPYARD_TECH || s.traits.economy == Economy.INDUSTRIAL || s.traits.economy == Economy.TECNOLOGICA
    }

    /** Onde comprar, para o jogador saber o que procurar. */
    val where: String get() = when (this) {
        COMPARTIMENTO, TRANSPONDER -> "mercado negro: sistemas com corrupção alta"
        else -> "estaleiros: sistemas industriais, tecnológicos ou de tecnologia alta"
    }
}

const val HOLD_STEP = 10
const val FUEL_STEP = 6
const val MOTOR_STEP_PCT = 25
const val HIDDEN_STEP = 4
const val TRANSPONDER_CUT_PCT = 40
const val SHIPYARD_TECH = 9
const val BLACK_MARKET_CORRUPTION = 55
