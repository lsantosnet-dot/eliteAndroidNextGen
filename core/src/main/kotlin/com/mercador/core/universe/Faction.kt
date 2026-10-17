package com.mercador.core.universe

import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive

data class Faction(
    val id: Int,
    val name: String,
    val ideology: Ideology,
    val doctrine: Doctrine,
    /** Posição do núcleo da facção, em coordenadas de setor. */
    val coreX: Int,
    val coreY: Int,
)

/** Facções de uma galáxia e a matriz de relações (-100 hostil .. 100 aliada) que nasce da semente. */
class FactionMap(val galaxySeed: Long, val factions: List<Faction>) {
    private val relations: Array<IntArray>

    init {
        val n = factions.size
        relations = Array(n) { IntArray(n) }
        for (i in 0 until n) for (j in i + 1 until n) {
            val r = Rng(derive(galaxySeed, 0xFAC7, i.toLong(), j.toLong())).range(-100, 100)
            relations[i][j] = r
            relations[j][i] = r
        }
        for (i in 0 until n) relations[i][i] = 100
    }

    fun relation(a: Int, b: Int): Int = relations[a][b]

    /** Facção cujo núcleo é o mais próximo do setor (diagrama de Voronoi). */
    fun controllerOf(sx: Int, sy: Int): Faction =
        factions.minBy { (it.coreX - sx).toLong().let { d -> d * d } + (it.coreY - sy).toLong().let { d -> d * d } }

    companion object {
        private val prefixes = listOf("Guilda", "Regime", "Sínodo", "Cartel", "Liga", "Consórcio", "Casa", "Concílio")
        private val suffixes = listOf("Mercante", "de Qube", "de Aurel", "Vermelho", "do Vazio", "de Tessa", "Ocre", "de Halm")

        fun generate(galaxySeed: Long, count: Int = 4, spread: Int = 12): FactionMap {
            val rng = Rng(derive(galaxySeed, 0xFAC))
            val used = HashSet<String>()
            val list = (0 until count).map { id ->
                var name: String
                do name = "${rng.pick(prefixes)} ${rng.pick(suffixes)}" while (!used.add(name))
                Faction(
                    id, name,
                    Ideology.values()[rng.nextInt(Ideology.values().size)],
                    Doctrine.values()[rng.nextInt(Doctrine.values().size)],
                    rng.range(-spread, spread), rng.range(-spread, spread),
                )
            }
            return FactionMap(galaxySeed, list)
        }
    }
}
