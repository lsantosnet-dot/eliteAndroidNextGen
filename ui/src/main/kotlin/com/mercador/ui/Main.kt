package com.mercador.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mercador.core.game.Game
import com.mercador.core.game.SaveCodec
import com.mercador.core.game.SaveException
import com.mercador.core.game.SaveSlots
import java.io.File

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
    var legacyWarning: String? = null
    if (legacy.isFile && slots.list().isEmpty()) {
        try {
            slots.create(Game.fromSave(SaveCodec.decode(legacy.readText())), "Jogo anterior")
            legacy.renameTo(File(home, "save.json.migrado"))
        } catch (e: SaveException) { legacyWarning = "O save antigo estava ilegível e foi deixado em ${legacy.name}." }
    }

    val opened = openSession(slots, startNew = "--new" in args)
    application {
        val controller = GameController(opened.game, slots = slots, activeSlot = opened.slotId, newGameFactory = { freshGame() })
        (opened.warning ?: legacyWarning)?.let { controller.notice = it }
        Window(
            onCloseRequest = ::exitApplication, title = "Mercador",
            state = rememberWindowState(size = DpSize(Preview.WIDTH_DP.dp, Preview.HEIGHT_DP.dp)),
        ) { AppRoot(controller) }
    }
}
