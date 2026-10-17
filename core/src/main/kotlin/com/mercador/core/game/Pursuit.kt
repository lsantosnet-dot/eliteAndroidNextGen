package com.mercador.core.game

import com.mercador.core.economy.Commodity
import com.mercador.core.universe.Doctrine
import com.mercador.core.universe.Faction
import com.mercador.core.universe.StarSystem

/** As respostas à perseguição (seção 7.1). "Confrontar" entra na etapa 4. */
enum class Response(val label: String) {
    SUBORNAR("Subornar"),
    JOGAR_FORA("Jogar a carga fora"),
    BLEFAR("Blefar"),
    FUGIR("Fugir para a base"),
    ENTREGAR_SE("Entregar-se e negociar"),
    CONFRONTAR("Confrontar"),
}

/** Patrulha que alcançou o jogador carregando [evidence] (toneladas de contrabando por mercadoria). */
class Pursuit(
    val system: StarSystem,
    val patrol: Faction,
    val evidence: Map<Commodity, Int>,
) {
    val evidenceTons: Int get() = evidence.values.sum()
    val evidenceValue: Int get() = evidence.entries.sumOf { (c, t) -> c.basePrice * t }
    val doctrine: Doctrine get() = patrol.doctrine
}

data class Resolution(
    val response: Response,
    val success: Boolean,
    val lostTons: Int,
    val paid: Int,
    val text: String,
)

/** Chances e custos, todos funções dos traços do sistema e da doutrina da facção. */
object PursuitRules {
    fun bribeChance(p: Pursuit): Double {
        val mod = when (p.doctrine) {
            Doctrine.BRUTAL -> -0.2; Doctrine.MERCANTIL -> 0.2; Doctrine.BUROCRATICA -> -0.1; Doctrine.COBRADORA -> 0.1
        }
        return (0.15 + p.system.traits.corruption / 100.0 * 0.7 + mod).coerceIn(0.05, 0.95)
    }

    fun bribeCost(p: Pursuit): Int = ((100 + p.evidenceValue * 0.5) * (1 + p.system.traits.surveillance / 100.0)).toInt()

    fun bluffChance(p: Pursuit, reputation: Int): Double {
        val mod = when (p.doctrine) {
            Doctrine.BUROCRATICA -> -0.2; Doctrine.MERCANTIL -> 0.1; Doctrine.BRUTAL -> -0.1; Doctrine.COBRADORA -> 0.0
        }
        return (0.35 + reputation / 300.0 - p.system.traits.surveillance / 250.0 + mod).coerceIn(0.05, 0.9)
    }

    fun fleeChance(p: Pursuit): Double =
        (0.6 - p.system.traits.surveillance / 200.0 - if (p.doctrine == Doctrine.BRUTAL) 0.15 else 0.0).coerceIn(0.05, 0.9)

    fun fine(p: Pursuit): Int {
        val mult = when (p.doctrine) {
            Doctrine.BRUTAL -> 2.0; Doctrine.MERCANTIL -> 0.5; Doctrine.BUROCRATICA -> 1.0; Doctrine.COBRADORA -> 1.5
        }
        return ((50 + p.evidenceValue * 0.4) * mult).toInt()
    }

    fun surrenderFine(p: Pursuit): Int = (fine(p) * 0.6).toInt()

    /** Chance de ser abordado ao chegar a um sistema. */
    fun encounterChance(heat: Int, surveillance: Int, carryingIllegal: Boolean, restricted: Boolean): Double {
        var p = 0.05 + heat / 200.0
        if (carryingIllegal) p += surveillance / 200.0
        if (restricted) p += 0.3
        return p.coerceIn(0.0, 0.9)
    }

    const val FLEE_FUEL = 3
}
