package com.mercador.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mercador.core.game.Game
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World

/** Janela de desenvolvimento no PC, no formato retrato de celular (360 × 740 dp). */
fun main() = application {
    val world = World(Region.around(Universe(2024)))
    val controller = GameController(Game(world, world.region.systems[0]))
    Window(
        onCloseRequest = ::exitApplication, title = "Mercador",
        state = rememberWindowState(size = DpSize(Preview.WIDTH_DP.dp, Preview.HEIGHT_DP.dp)),
    ) { AppRoot(controller) }
}
