package com.mercador.ui

import com.mercador.core.game.Game
import com.mercador.core.game.SaveException
import com.mercador.core.game.SaveSlots
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import kotlin.random.Random

/** Um jogo novo num universo próprio (semente sorteada), no sistema inicial da região. */
fun freshGame(seed: Long = Random.nextLong()): Game {
    val world = World(Region.around(Universe(seed)))
    return Game(world, world.region.systems[0])
}

/** O jogo aberto na partida do app: de qual slot veio e um aviso, se algo deu errado ao abrir. */
class Opened(val game: Game, val slotId: Int, val warning: String?)

/**
 * Abre o jogo mais recente dos [slots] ou começa um novo (num slot novo). Compartilhado pelo app
 * de PC e pelo Android, para os dois abrirem do mesmo jeito. Um slot que não carrega nunca é apagado.
 */
fun openSession(slots: SaveSlots, startNew: Boolean = false, factory: () -> Game = { freshGame() }): Opened {
    var warning: String? = null
    if (!startNew) slots.lastUsed()?.let {
        try { return Opened(slots.load(it.id), it.id, null) } catch (e: SaveException) { warning = e.message }
    }
    val g = factory()
    return Opened(g, slots.create(g), warning)
}
