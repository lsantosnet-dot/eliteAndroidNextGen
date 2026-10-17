package com.mercador.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mercador.core.game.Game
import com.mercador.core.game.SaveException
import com.mercador.core.game.SaveSlots
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import java.io.File
import kotlin.random.Random

/** Um jogo novo num universo próprio (semente sorteada), no sistema inicial da região. */
fun freshGame(seed: Long = Random.nextLong()): Game {
    val world = World(Region.around(Universe(seed)))
    return Game(world, world.region.systems[0])
}

/**
 * Janela de desenvolvimento no PC, no formato retrato de celular (360 × 740 dp).
 * Os jogos salvam sozinhos em ~/.mercador/saves (um arquivo por slot).
 * Argumentos: --new (começa um jogo novo ao abrir), --dir <pasta>.
 */
fun main(args: Array<String>) {
    val home = File(System.getProperty("user.home"), ".mercador")
    val dir = args.indexOf("--dir").takeIf { it >= 0 }?.let { args.getOrNull(it + 1) }?.let(::File) ?: File(home, "saves")
    val slots = SaveSlots(dir)

    // Save único da versão anterior (~/.mercador/save.json) vira o primeiro slot.
    val legacy = File(home, "save.json")
    var warning: String? = null
    if (legacy.isFile && slots.list().isEmpty()) {
        try {
            slots.create(Game.fromSave(com.mercador.core.game.SaveCodec.decode(legacy.readText())), "Jogo anterior")
            legacy.renameTo(File(home, "save.json.migrado"))
        } catch (e: SaveException) { warning = "O save antigo estava ilegível e foi deixado em ${legacy.name}." }
    }

    var game: Game? = null
    var id: Int? = null
    if ("--new" !in args) slots.lastUsed()?.let {
        try { game = slots.load(it.id); id = it.id } catch (e: SaveException) { warning = e.message }
    }
    if (game == null) { game = freshGame(); id = slots.create(game) }

    application {
        val controller = GameController(game!!, slots = slots, activeSlot = id, newGameFactory = { freshGame() })
        warning?.let { controller.notice = it }
        Window(
            onCloseRequest = ::exitApplication, title = "Mercador",
            state = rememberWindowState(size = DpSize(Preview.WIDTH_DP.dp, Preview.HEIGHT_DP.dp)),
        ) { AppRoot(controller) }
    }
}
