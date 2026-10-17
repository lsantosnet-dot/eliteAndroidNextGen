package com.mercador.core.game

import com.mercador.core.rng.Rng
import com.mercador.core.universe.Doctrine
import com.mercador.core.universe.Faction

/** Traços do jogador (seção 8.4): surgem de atos, abrem e fecham opções; alguns se dissipam com o tempo. */
enum class Trait(val label: String, val permanent: Boolean) {
    CALEJADO("Calejado", true),
    FRIO("Frio", false),
    INSONE("Insone", false),
    DEVEDOR("Devedor", false),
    /** Única marca permanente que vem só da violência. */
    MARCADO("Marcado", true);

    /** Ajuste do sangue-frio inicial numa cena. */
    val poise: Int get() = when (this) { CALEJADO -> 10; FRIO -> 5; INSONE -> -10; DEVEDOR -> -5; MARCADO -> 0 }

    /** Dá fama de perigoso (libera Intimidar). */
    val feared: Boolean get() = this == FRIO || this == MARCADO || this == CALEJADO
}

/** Uma pessoa com nome e rosto: o oponente nasce do mundo e a violência cria alguém que pode retaliar. */
data class Person(val name: String, val role: String, val faction: Faction)

object People {
    private val first = listOf("Ilya", "Mara", "Tobin", "Sael", "Ruvo", "Cass", "Oren", "Dalia", "Vesk", "Nuri", "Hadda", "Pell")
    private val last = listOf("Voss", "Karimov", "Delacroix", "Ashe", "Quill", "Marek", "Tessaly", "Brandt", "Okoye", "Ferro")

    fun role(d: Doctrine): String = when (d) {
        Doctrine.BRUTAL -> "capitão da guarda"
        Doctrine.MERCANTIL -> "fiscal da guilda"
        Doctrine.BUROCRATICA -> "inspetor da alfândega"
        Doctrine.COBRADORA -> "cobrador do cartel"
    }

    fun generate(rng: Rng, faction: Faction, role: String = role(faction.doctrine)) =
        Person("${rng.pick(first)} ${rng.pick(last)}", role, faction)
}
