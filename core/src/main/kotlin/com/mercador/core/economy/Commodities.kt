package com.mercador.core.economy

import com.mercador.core.universe.Economy
import com.mercador.core.universe.Ideology

/**
 * Mercadorias da fatia vertical (~12). Cada uma tem preço base (créditos por tonelada),
 * as economias que a produzem e as que a demandam.
 */
enum class Commodity(
    val label: String,
    val basePrice: Int,
    val producedBy: Set<Economy>,
    val demandedBy: Set<Economy>,
) {
    COMIDA("Comida", 20, setOf(Economy.AGRICOLA), setOf(Economy.INDUSTRIAL, Economy.MINERADORA)),
    AGUA("Água", 15, setOf(Economy.AGRICOLA, Economy.PORTUARIA), setOf(Economy.MINERADORA)),
    REMEDIO("Remédio", 90, setOf(Economy.TECNOLOGICA), setOf(Economy.AGRICOLA, Economy.PORTUARIA)),
    COMBUSTIVEL("Combustível", 35, setOf(Economy.MINERADORA), setOf(Economy.PORTUARIA, Economy.COMERCIAL)),
    MINERIO("Minério", 30, setOf(Economy.MINERADORA), setOf(Economy.INDUSTRIAL)),
    MAQUINAS("Máquinas", 120, setOf(Economy.INDUSTRIAL), setOf(Economy.AGRICOLA, Economy.MINERADORA)),
    ARMAS("Armas", 200, setOf(Economy.INDUSTRIAL), setOf(Economy.PORTUARIA, Economy.COMERCIAL)),
    ELETRONICOS("Eletrônicos", 150, setOf(Economy.TECNOLOGICA), setOf(Economy.COMERCIAL, Economy.INDUSTRIAL)),
    LUXO("Bens de luxo", 260, setOf(Economy.COMERCIAL), setOf(Economy.TECNOLOGICA, Economy.PORTUARIA)),
    DROGAS("Narcóticos", 340, setOf(Economy.PORTUARIA), setOf(Economy.COMERCIAL, Economy.INDUSTRIAL)),
    LIVROS("Livros e dados", 60, setOf(Economy.TECNOLOGICA), setOf(Economy.AGRICOLA, Economy.MINERADORA)),
    RELIQUIAS("Relíquias", 420, setOf(Economy.COMERCIAL), setOf(Economy.TECNOLOGICA));

    /** Mercadorias que a ideologia da facção proíbe: é isso que fabrica o contrabando (seção 3.3). */
    fun bannedBy(ideology: Ideology): Boolean = this in Laws.banned(ideology)
}

object Laws {
    private val table: Map<Ideology, Set<Commodity>> = mapOf(
        Ideology.MERCANTIL to setOf(Commodity.RELIQUIAS),
        Ideology.AUTORITARIA to setOf(Commodity.LIVROS, Commodity.DROGAS, Commodity.RELIQUIAS),
        Ideology.RELIGIOSA to setOf(Commodity.DROGAS, Commodity.LUXO, Commodity.ELETRONICOS),
        Ideology.CLANICA to setOf(Commodity.ARMAS, Commodity.LIVROS),
    )

    fun banned(ideology: Ideology): Set<Commodity> = table.getValue(ideology)
}
