package com.mercador.core.universe

import kotlin.math.hypot

/**
 * Região jogável da fatia vertical: os [size] sistemas mais próximos do centro de uma galáxia.
 * Determinística: depende só da semente do universo, da galáxia e do setor central.
 */
class Region(val universe: Universe, val galaxy: Long, val systems: List<StarSystem>) {

    /** Distância em anos-luz (posições estão em décimos de ano-luz). */
    fun distanceLy(a: StarSystem, b: StarSystem): Double = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()) / 10.0

    /** Sistemas ao alcance de um salto de [rangeLy], do mais perto ao mais longe. */
    fun neighbors(from: StarSystem, rangeLy: Double): List<StarSystem> =
        systems.filter { it !== from && distanceLy(from, it) <= rangeLy }.sortedBy { distanceLy(from, it) }

    /** Verdadeiro se todos os sistemas se alcançam encadeando saltos de [rangeLy]. */
    fun isConnected(rangeLy: Double): Boolean {
        if (systems.isEmpty()) return true
        val seen = HashSet<SystemId>()
        val stack = ArrayDeque<StarSystem>().apply { add(systems[0]) }
        seen.add(systems[0].id)
        while (stack.isNotEmpty()) {
            val s = stack.removeLast()
            for (n in neighbors(s, rangeLy)) if (seen.add(n.id)) stack.add(n)
        }
        return seen.size == systems.size
    }

    /** Menor alcance de salto (em anos-luz, arredondado para cima) que conecta a região inteira. */
    fun minimumConnectingRange(): Int {
        var r = 1
        while (!isConnected(r.toDouble())) r++
        return r
    }

    companion object {
        const val DEFAULT_SIZE = 50

        fun around(universe: Universe, galaxy: Long = 0, centerSx: Int = 0, centerSy: Int = 0, size: Int = DEFAULT_SIZE): Region {
            var radius = 1
            var found: List<StarSystem>
            do {
                found = universe.nearby(galaxy, centerSx, centerSy, radius)
                radius++
            } while (found.size < size)
            val cx = centerSx * Universe.SECTOR_SIZE + Universe.SECTOR_SIZE / 2
            val cy = centerSy * Universe.SECTOR_SIZE + Universe.SECTOR_SIZE / 2
            // Desempate por id: ordem total, logo resultado estável.
            val picked = found
                .sortedWith(compareBy<StarSystem>({ hypot((it.x - cx).toDouble(), (it.y - cy).toDouble()) }, { it.id.sx }, { it.id.sy }, { it.id.index }))
                .take(size)
            return Region(universe, galaxy, picked)
        }
    }
}
