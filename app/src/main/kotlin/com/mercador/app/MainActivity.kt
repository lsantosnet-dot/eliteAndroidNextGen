package com.mercador.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import com.mercador.core.game.SaveSlots
import com.mercador.ui.AppRoot
import com.mercador.ui.GameController
import com.mercador.ui.Noir
import com.mercador.ui.freshGame
import com.mercador.ui.openSession
import java.io.File

/**
 * Guarda o [GameController] além da Activity: girar a tela ou trocar de app não perde o jogo.
 * Os jogos ficam na pasta interna do app (filesDir/saves), um arquivo por slot.
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    val controller: GameController

    init {
        val slots = SaveSlots(File(app.filesDir, "saves"))
        val opened = openSession(slots)
        controller = GameController(opened.game, slots = slots, activeSlot = opened.slotId, newGameFactory = { freshGame() })
        opened.warning?.let { controller.notice = it }
    }
}

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // targetSdk 35 desenha sob as barras do sistema: o padding as deixa de fora.
            Box(Modifier.fillMaxSize().background(Noir.bg).systemBarsPadding()) { AppRoot(vm.controller) }
        }
    }
}
