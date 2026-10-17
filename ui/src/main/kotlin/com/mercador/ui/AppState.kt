package com.mercador.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mercador.core.economy.Commodity
import com.mercador.core.game.*
import com.mercador.core.game.SaveSlots
import com.mercador.core.universe.StarSystem
import com.mercador.core.world.Side

enum class StationTab(val label: String) { MERCADO("Mercado"), SISTEMA("Dossiê"), BAR("Bar") }

/** Mensagem de desfecho mostrada em tela cheia (resultado de uma decisão) até o jogador continuar. */
class Report(val kicker: String, val text: String)

/**
 * Estado do app: um [Game] mais o que é só da interface (aba, seleção, avisos). Toda ação passa
 * por aqui, que chama o :core e avisa a UI com [version]. Não depende de Android nem de janela.
 */
class GameController(
    initial: Game,
    private val onChange: (Game) -> Unit = {},
    /** Vários jogos salvos. Sem isto (testes, previews) o controlador joga um único jogo sem slots. */
    private val slots: SaveSlots? = null,
    private val activeSlot: Int? = null,
    /** Fábrica de jogos novos (cada um com seu universo). */
    private val newGameFactory: () -> Game = { error("sem fábrica de jogo novo") },
) {
    var game: Game by mutableStateOf(initial)
        private set
    /** Slot em que o jogo atual grava sozinho; null sem slots. */
    var slotId: Int? by mutableStateOf(activeSlot)
        private set
    private var slotName: String = activeSlot?.let { slots?.nameOf(it) } ?: ""
    var showSlots by mutableStateOf(false)
    /** Slot que espera a segunda confirmação para ser apagado. */
    var pendingDelete by mutableStateOf<Int?>(null)
    /** Muda a cada ação: composables que o leem recomponham e releem o [game]. */
    var version by mutableIntStateOf(0)
        private set
    var tab by mutableStateOf(Tab.ESTACAO)
    var stationTab by mutableStateOf(StationTab.MERCADO)
    var selectedSystem by mutableStateOf<StarSystem?>(null)
    var selectedCommodity by mutableStateOf<Commodity?>(null)
    var quantity by mutableIntStateOf(1)
    var notice by mutableStateOf<String?>(null)
    var rumor by mutableStateOf<String?>(null)
    var report by mutableStateOf<Report?>(null)

    private fun touch() {
        version++
        onChange(game)
        slots?.let { sl -> slotId?.let { id -> sl.save(id, slotName, game) } }
    }

    /** Lista atual de jogos salvos (relê a pasta a cada chamada; é pequena). */
    fun slotList(): List<SlotInfo> = slots?.list() ?: emptyList()

    private fun resetUi() {
        tab = Tab.ESTACAO; stationTab = StationTab.MERCADO; selectedSystem = null; selectedCommodity = null
        quantity = 1; report = null; rumor = null; pendingDelete = null
    }

    private fun swap(g: Game, id: Int?, msg: String) {
        game = g; slotId = id; slotName = id?.let { slots?.nameOf(it) } ?: ""; resetUi(); showSlots = false; notice = msg
        version++; onChange(game)
    }

    private fun canSwitch(): Boolean {
        if (game.busy) { notice = "Resolva o que está acontecendo antes de trocar de jogo."; version++; return false }
        return true
    }

    fun openSlots() { if (canSwitch()) { showSlots = true; pendingDelete = null; version++ } }
    fun closeSlots() { showSlots = false; pendingDelete = null; version++ }

    /** Começa um jogo novo num slot novo; o jogo atual continua guardado no slot dele. */
    fun newGame() {
        val sl = slots ?: return
        if (!canSwitch()) return
        touch() // garante que o jogo atual está gravado antes de sair dele
        val g = newGameFactory()
        val id = sl.create(g)
        swap(g, id, "Jogo novo começou. O anterior continua guardado.")
    }

    fun loadSlot(id: Int) {
        val sl = slots ?: return
        if (!canSwitch()) return
        if (id == slotId) return closeSlots()
        touch()
        try { swap(sl.load(id), id, "Jogo carregado.") }
        catch (e: SaveException) { notice = e.message; version++ }
    }

    /** Duas etapas: o primeiro toque pede confirmação, o segundo apaga. */
    fun deleteSlot(id: Int) {
        val sl = slots ?: return
        if (pendingDelete != id) { pendingDelete = id; version++; return }
        pendingDelete = null
        sl.delete(id)
        if (id == slotId) {
            // apagou o jogo em andamento: abre o mais recente que sobrou, ou começa um novo
            val next = sl.lastUsed()
            if (next != null) swap(sl.load(next.id), next.id, "Jogo apagado. Abrindo \"${next.name}\".")
            else { val g = newGameFactory(); swap(g, sl.create(g), "Jogo apagado. Começando um novo.") }
        } else version++
    }

    fun selectTab(t: Tab) { tab = t; notice = null; touch() }
    fun selectStationTab(t: StationTab) { stationTab = t; notice = null; touch() }
    fun selectSystem(s: StarSystem) { selectedSystem = s; touch() }
    fun selectCommodity(c: Commodity) { selectedCommodity = c; touch() }
    fun cycleQuantity() { quantity = when (quantity) { 1 -> 5; 5 -> 10; else -> 1 }; touch() }
    fun dismissReport() { report = null; touch() }

    fun buy() {
        val c = selectedCommodity ?: return fail("Escolha uma mercadoria.")
        val unit = game.world.unitPrice(game.location, c, quantity.toDouble(), Side.BUY)
        when (val o = game.buy(c, quantity)) {
            is Outcome.Fail -> fail(describe(o.reason))
            else -> { notice = "Comprou $quantity t de ${c.label.lowercase()} por ${credits(unit * quantity)}."; touch() }
        }
    }

    fun sell() {
        val c = selectedCommodity ?: return fail("Escolha uma mercadoria.")
        val n = minOf(quantity, game.cargo[c] ?: 0)
        if (n <= 0) return fail(describe(Failure.SEM_CARGA))
        val unit = game.world.unitPrice(game.location, c, n.toDouble(), Side.SELL)
        when (val o = game.sell(c, n)) {
            is Outcome.Fail -> fail(describe(o.reason))
            else -> { notice = "Vendeu $n t de ${c.label.lowercase()} por ${credits(unit * n)}."; touch() }
        }
    }

    fun refuel() = simple(game.refuel(), "Tanque cheio.")
    fun takeLoan() = simple(game.takeLoan(), "Empréstimo concedido. A dívida cresce.")
    fun haulFreight() = simple(game.haulFreight(), "Frete cumprido: cinco dias a mais e algumas moedas.")
    fun repay() = simple(game.repay(), "Pagou o que pôde da dívida.")

    private fun simple(o: Outcome, ok: String) {
        if (o is Outcome.Fail) fail(describe(o.reason)) else { notice = ok; touch() }
    }

    fun travel() {
        val to = selectedSystem ?: return fail("Escolha um destino no mapa.")
        when (val o = game.travel(to)) {
            is Outcome.Fail -> fail(describe(o.reason))
            is Outcome.Ok -> { notice = "Chegou a ${to.name}."; selectedSystem = null; tab = Tab.ESTACAO; stationTab = StationTab.MERCADO; touch() }
            else -> { selectedSystem = null; tab = Tab.ESTACAO; touch() } // perseguição ou história: a tela troca sozinha
        }
    }

    fun respond(r: Response) {
        when (val o = game.respond(r)) {
            is Outcome.Fail -> fail(describe(o.reason))
            is Outcome.Resolved -> { report = Report(r.label, o.resolution.text); touch() }
            else -> touch()
        }
    }

    fun act(p: Posture) {
        when (val o = game.act(p)) {
            is Outcome.Fail -> fail(describe(o.reason))
            is Outcome.SceneEnded -> { report = Report("O confronto termina", o.text); touch() }
            else -> touch()
        }
    }

    fun choose(i: Int) {
        when (val o = game.chooseStory(i)) {
            is Outcome.Fail -> fail(describe(o.reason))
            is Outcome.StoryResolved -> { report = Report("E então…", o.text); touch() }
            else -> touch()
        }
    }

    fun buyRumor(offer: BarOffer) {
        val (o, text) = game.buyRumor(offer)
        if (o is Outcome.Fail) fail(describe(o.reason)) else { rumor = text; touch() }
    }

    private fun fail(text: String) { notice = text; touch() }

    companion object {
        fun describe(f: Failure): String = when (f) {
            Failure.SEM_DINHEIRO -> "Dinheiro insuficiente."
            Failure.SEM_PORAO -> "O porão não comporta isso."
            Failure.SEM_ESTOQUE -> "Não há tanto estoque."
            Failure.SEM_CARGA -> "Você não carrega isso."
            Failure.SEM_COMBUSTIVEL -> "Combustível insuficiente para esse salto."
            Failure.FORA_DE_ALCANCE -> "Destino fora de alcance."
            Failure.QUANTIDADE_INVALIDA -> "Quantidade inválida."
            Failure.TANQUE_CHEIO -> "O tanque já está cheio."
            Failure.PERSEGUICAO_PENDENTE -> "Resolva o que está acontecendo primeiro."
            Failure.SEM_PERSEGUICAO, Failure.SEM_CENA, Failure.SEM_HISTORIA -> "Nada a resolver agora."
            Failure.RESPOSTA_INDISPONIVEL, Failure.POSTURA_INDISPONIVEL, Failure.ESCOLHA_INDISPONIVEL -> "Essa opção não está disponível."
            Failure.SEM_DIVIDA -> "Você não deve nada."
            Failure.NAO_ELEGIVEL -> "Você ainda tem como se virar sozinho."
            Failure.SEM_BAR -> "Não há bar aqui."
        }
    }
}
