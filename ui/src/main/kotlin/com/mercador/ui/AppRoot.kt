package com.mercador.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mercador.core.story.InnerVoice

/**
 * Raiz da navegação. Decisões pendentes (relatório, perseguição, confronto, história) tomam a tela
 * inteira e só liberam a navegação por abas quando resolvidas, como no design: sem pausa.
 */
@Composable
fun AppRoot(c: GameController) {
    c.version // assina as mudanças: cada ação do controlador recompõe a raiz
    val g = c.game
    val report = c.report
    val card = g.pendingStory
    when {
        c.jump != null -> HyperspaceJump(c.jump!!, c::finishJump)
        c.showSlots -> SlotsScreen(c.slotList(), c.slotId, c.pendingDelete, c.notice, c::loadSlot, c::deleteSlot, c::newGame, c::closeSlots)
        report != null -> ReportScreen(report, c::dismissReport)
        !g.busy && g.epilogue() != null -> EpilogueScreen(g.epilogue()!!, c::dismissEpilogue)
        g.pursuit != null -> PursuitScreen(g, c::respond)
        g.confrontation != null -> ConfrontationScreen(g, g.confrontation!!, InnerVoice.line(g), c::act)
        card != null -> StoryScreen(card, InnerVoice.line(g), c::choose)
        else -> when (c.tab) {
            Tab.MAPA -> MapScreen(
                g, c.selectedSystem ?: g.location, c::selectSystem, c::selectTab, c::travel, c.notice,
            )
            Tab.ESTACAO -> StationScreen(
                g, c::selectTab,
                StationUi(c.stationTab, c.selectedCommodity, c.quantity, c.notice, c.rumor,
                    c::selectStationTab, c::selectCommodity, c::cycleQuantity, c::buy, c::sell, c::buyRumor,
                    c::acceptContract, c::deliverContract, c::abandonContract, c::buyUpgrade),
            )
            Tab.DOSSIE -> DossierScreen(g, c.notice, c::selectTab, c::refuel, c::takeLoan, c::haulFreight, c::repay, c::openSlots, c::payCreditor)
            Tab.LIVRO -> LedgerScreen(g, InnerVoice.line(g), c::selectTab)
        }
    }
}
