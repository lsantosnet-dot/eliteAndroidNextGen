package com.mercador.core.game

import com.mercador.core.economy.Commodity
import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive
import com.mercador.core.universe.Doctrine

/** As posturas por rodada (seção 8.2). */
enum class Posture(val label: String, val cost: String) {
    RESISTIR("Resistir", "desgasta o seu sangue-frio"),
    INTIMIDAR("Intimidar", "pode escalar para a violência"),
    EXPOR_SEGREDO("Expor um segredo", "queima o segredo para sempre"),
    COBRAR_FAVOR("Cobrar um favor", "gasta o favor"),
    SACRIFICAR("Sacrificar algo", "metade da carga comprometedora vai ao vazio"),
    VIOLENCIA("Partir para a violência", "irreversível, sem recompensa; nunca recomendada"),
}

enum class Ending { ELE_CEDEU, VOCE_CEDEU, EMPATE, VIOLENCIA }

/**
 * Cena em rodadas (em geral 3 a 5), sem tiro. Medidores: sangue-frio (zero = o lado cede) e
 * alavancagem (o que cada lado tem contra o outro). Os resultados vêm de números do jogo e de
 * sorteio determinístico, para os testes reproduzirem qualquer cena.
 */
class Confrontation(
    val opponent: Person,
    val evidence: Map<Commodity, Int>,
    /** Cena de retaliação: não há carga em jogo, o que está em jogo é dinheiro. */
    val retaliation: Boolean,
    playerPoise: Int,
    playerLeverage: Int,
    opponentPoise: Int,
    opponentLeverage: Int,
    val maxRounds: Int,
    private val seedKey: Long,
    private val day: Int,
) {
    var round = 0
        private set
    var playerCool = playerPoise.coerceIn(1, 100)
        private set
    var opponentCool = opponentPoise.coerceIn(1, 100)
        private set
    var playerLeverage = playerLeverage
        private set
    val opponentLeverage = opponentLeverage
    var ending: Ending? = null
        private set
    var intimidations = 0
        private set
    /** Se o sacrifício foi feito, a metade da carga comprometedora que sai do porão. */
    var sacrificed: Boolean = false
        private set
    val log = ArrayList<String>()
    val doctrine: Doctrine get() = opponent.faction.doctrine
    val finished: Boolean get() = ending != null

    private val pressure: Double
        get() {
            val base = when (doctrine) { Doctrine.BRUTAL -> 14.0; Doctrine.MERCANTIL -> 8.0; Doctrine.BUROCRATICA -> 10.0; Doctrine.COBRADORA -> 11.0 }
            return (base * (1 + (opponentLeverage - playerLeverage) / 100.0)).coerceIn(4.0, 24.0)
        }

    /** Uma rodada. As regras de disponibilidade (segredo, favor, fama) ficam em [Game]. */
    fun step(posture: Posture, hasFame: Boolean) {
        check(!finished) { "cena encerrada" }
        val rng = Rng(derive(seedKey, day.toLong(), round.toLong(), posture.ordinal.toLong(), 0xC0F))
        val resistDmg = (6 + (playerLeverage - opponentLeverage) / 10).coerceIn(2, 14)
        val p = pressure
        when (posture) {
            Posture.RESISTIR -> {
                opponentCool -= resistDmg; playerCool -= (p * 0.6).toInt()
                log += "Você aguenta o olhar de ${opponent.name} sem ceder."
            }
            Posture.INTIMIDAR -> {
                val chance = (0.4 + (if (hasFame) 0.2 else 0.0) - if (doctrine == Doctrine.BRUTAL) 0.2 else 0.0).coerceIn(0.1, 0.9)
                if (rng.nextDouble() < chance) {
                    intimidations++
                    opponentCool -= 22; playerCool -= (p * 0.5).toInt()
                    log += "${opponent.name} recua um passo."
                } else {
                    playerCool -= p.toInt()
                    log += "A ameaça não colou."
                    if (rng.chance(0.3)) { ending = Ending.VIOLENCIA; log += "A cena escala para a violência."; round++; return }
                }
            }
            Posture.EXPOR_SEGREDO -> {
                opponentCool -= 35; playerCool -= (p * 0.4).toInt()
                log += "Você diz o que sabe. ${opponent.name} empalidece."
            }
            Posture.COBRAR_FAVOR -> {
                opponentCool -= 28; playerCool -= (p * 0.4).toInt()
                log += "Você lembra a ${opponent.name} o que lhe deve."
            }
            Posture.SACRIFICAR -> {
                sacrificed = true
                playerLeverage += 15; opponentCool -= 12; playerCool -= (p * 0.5).toInt()
                log += "Você abre mão de parte da carga. ${opponent.name} nota o gesto."
            }
            Posture.VIOLENCIA -> {
                ending = Ending.VIOLENCIA
                log += "Você parte para cima de ${opponent.name}."
                round++
                return
            }
        }
        round++
        ending = when {
            opponentCool <= 0 -> Ending.ELE_CEDEU
            playerCool <= 0 -> Ending.VOCE_CEDEU
            round >= maxRounds -> Ending.EMPATE
            else -> null
        }
        playerCool = playerCool.coerceAtLeast(0); opponentCool = opponentCool.coerceAtLeast(0)
    }

    companion object {
        fun rounds(surveillance: Int, seedKey: Long): Int = 3 + Rng(derive(seedKey, 0x40D)).nextInt(if (surveillance > 60) 3 else 2)
    }
}
