package com.mercador.core.universe

import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive

/** Identifica um sistema sem precisar gerar nenhum outro: galáxia → setor (x, y) → índice. */
data class SystemId(val galaxy: Long, val sx: Int, val sy: Int, val index: Int)

class StarSystem(
    val id: SystemId,
    val name: String,
    /** Posição absoluta em décimos de ano-luz. */
    val x: Int,
    val y: Int,
    val traits: SystemTraits,
    val faction: Faction,
) {
    /** Chave estável para derivar valores por sistema (mercado, eventos) sem depender de ordem. */
    fun seedKey(): Long = derive(id.galaxy, id.sx.toLong(), id.sy.toLong(), id.index.toLong())
}

/**
 * Universo por hash: qualquer galáxia/setor é gerado direto da semente, sem gerar os anteriores
 * (design, seção 3.1). Nada aqui é salvo; o save guarda só os deltas do mundo vivido.
 */
class Universe(val seed: Long) {
    private val factionMaps = HashMap<Long, FactionMap>()

    fun galaxySeed(galaxy: Long): Long = derive(seed, galaxy)

    fun factions(galaxy: Long): FactionMap =
        factionMaps.getOrPut(galaxy) { FactionMap.generate(galaxySeed(galaxy)) }

    /** Sistemas do setor (sx, sy), em ordem de índice. Mesmo resultado para a mesma semente, sempre. */
    fun sector(galaxy: Long, sx: Int, sy: Int): List<StarSystem> {
        val factionMap = factions(galaxy)
        val sectorSeed = derive(galaxySeed(galaxy), sx.toLong(), sy.toLong())
        val count = Rng(sectorSeed).range(0, MAX_PER_SECTOR)
        val faction = factionMap.controllerOf(sx, sy)
        return (0 until count).map { i ->
            val sysSeed = derive(sectorSeed, i.toLong())
            val rng = Rng(sysSeed)
            val x = sx * SECTOR_SIZE + rng.nextInt(SECTOR_SIZE)
            val y = sy * SECTOR_SIZE + rng.nextInt(SECTOR_SIZE)
            val traits = SystemGenerator.traits(rng, faction.ideology)
            StarSystem(SystemId(galaxy, sx, sy, i), SystemGenerator.name(rng), x, y, traits, faction)
        }
    }

    fun system(id: SystemId): StarSystem? = sector(id.galaxy, id.sx, id.sy).getOrNull(id.index)

    /** Sistemas dentro de [radius] setores de (sx, sy): geração preguiçosa só do que está perto. */
    fun nearby(galaxy: Long, sx: Int, sy: Int, radius: Int): List<StarSystem> =
        (sx - radius..sx + radius).flatMap { x -> (sy - radius..sy + radius).flatMap { y -> sector(galaxy, x, y) } }

    companion object {
        const val SECTOR_SIZE = 64
        const val MAX_PER_SECTOR = 3
    }
}

internal object SystemGenerator {
    private val syl = listOf("qu", "be", "ra", "au", "rel", "te", "ssa", "hal", "m", "or", "di", "so", "la", "ve", "ri", "zen", "ta", "ko")

    fun name(rng: Rng): String {
        val parts = rng.range(2, 3)
        val s = (0 until parts).joinToString("") { rng.pick(syl) }
        return s.replaceFirstChar { it.uppercase() }
    }

    fun traits(rng: Rng, factionIdeology: Ideology): SystemTraits {
        val economy = Economy.values()[rng.nextInt(Economy.values().size)]
        val tech = (rng.range(1, 10) + if (economy == Economy.TECNOLOGICA) 4 else 0).coerceAtMost(15)
        // Regras encadeadas: ideologia autoritária → mais vigilância e menos corrupção, e vice-versa.
        val authoritarian = factionIdeology == Ideology.AUTORITARIA
        val mercantile = factionIdeology == Ideology.MERCANTIL
        val surveillance = (rng.range(10, 70) + if (authoritarian) 25 else 0).coerceIn(0, 100)
        val corruption = (rng.range(10, 70) + if (mercantile) 20 else 0 - if (authoritarian) 20 else 0).coerceIn(0, 100)
        val stability = (100 - (corruption / 2) + rng.range(-20, 20) + if (authoritarian) 10 else 0).coerceIn(0, 100)
        val rare = if (rng.chance(0.2)) RareResource.values()[rng.range(1, RareResource.values().size - 1)] else RareResource.NENHUM
        return SystemTraits(
            economy = economy,
            techLevel = tech,
            population = rng.range(1, 900),
            productivity = rng.range(10, 100),
            corruption = corruption,
            surveillance = surveillance,
            stability = stability,
            ideology = factionIdeology,
            cultureIndex = rng.range(0, 99),
            rareResource = rare,
        )
    }
}
