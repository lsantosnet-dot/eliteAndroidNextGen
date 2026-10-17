package com.mercador.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mercador.core.game.Game
import com.mercador.core.game.SaveStore
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import java.io.File

/**
 * Janela de desenvolvimento no PC, no formato retrato de celular (360 × 740 dp).
 * O jogo salva sozinho em ~/.mercador/save.json a cada ação em momento calmo.
 * Argumentos: --new (ignora o save existente), --save <arquivo>.
 */
fun main(args: Array<String>) {
    val path = args.indexOf("--save").takeIf { it >= 0 }?.let { args.getOrNull(it + 1) }
        ?: File(System.getProperty("user.home"), ".mercador/save.json").path
    val store = SaveStore(File(path))
    if ("--new" in args) File(path).delete()
    val loaded = store.loadOrNew {
        val world = World(Region.around(Universe(2024)))
        Game(world, world.region.systems[0])
    }
    application {
        val controller = GameController(loaded.game) { store.autosave(it) }
        loaded.warning?.let { controller.notice = it }
        Window(
            onCloseRequest = ::exitApplication, title = "Mercador",
            state = rememberWindowState(size = DpSize(Preview.WIDTH_DP.dp, Preview.HEIGHT_DP.dp)),
        ) { AppRoot(controller) }
    }
}
