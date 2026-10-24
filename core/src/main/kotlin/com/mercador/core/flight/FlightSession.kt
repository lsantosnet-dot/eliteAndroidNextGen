package com.mercador.core.flight

import com.mercador.core.game.Failure
import com.mercador.core.game.Game
import com.mercador.core.game.Outcome
import com.mercador.core.game.Upgrade
import com.mercador.core.rng.Rng
import com.mercador.core.universe.Faction
import com.mercador.core.universe.StarSystem

/** O que um voo devolve ao jogo quando algo grande acontece. */
sealed interface FlightResult {
    /** Atracou na estação de [system]. */
    data class Docked(val system: StarSystem) : FlightResult
    /** O salto aconteceu: o jogo já está em [to] e [outcome] é a chegada (patrulha, história...). */
    data class Jumped(
        val from: StarSystem, val to: StarSystem, val ly: Int, val days: Int, val outcome: Outcome,
        /** Sistemas que trocaram de dono durante a viagem: (sistema, dono antigo, dono novo). */
        val revolts: List<Triple<StarSystem, Faction, Faction>> = emptyList(),
    ) : FlightResult
    /** O salto falhou e caímos no witchspace a caminho de [to]. */
    data class Misjumped(val from: StarSystem, val to: StarSystem) : FlightResult
    /** A nave foi destruída; [text] é o desfecho (cápsula, seguro, carga perdida). */
    data class Destroyed(val text: String) : FlightResult
}

/**
 * Um voo entre a saída de uma estação e a atracação seguinte, ligando o [FlightModel] (o Elite) ao [Game]
 * (o Mercador): decolar põe a estação atrás e o planeta à frente; o hiperespaço é pilotado — escolher o
 * destino, afastar-se da massa da estação e esperar a contagem, sob fogo se for o caso —; na chegada é
 * preciso voar até o planeta, achar a estação e atracar. No caminho há piratas, caçadores e a polícia
 * (mais perigosos onde a vigilância é baixa ou a sua ficha é suja) e, raramente, o salto falha no witchspace.
 */
class FlightSession(val game: Game, target: StarSystem? = null, seed: Long = kotlin.random.Random.nextLong()) {
    val model = FlightModel(seed)
    val controls = FlightControls()
    private val rng = Rng(seed xor 0x5EED)

    /** Destino do hiperespaço (QQ9/QQ10). */
    var target: StarSystem? = target?.takeIf { it !== game.location }

    /** Combustível já queimado num salto que falhou rumo a [witchspaceTarget]. */
    private var prepaid = 0
    /** No witchspace, o destino do salto que falhou. */
    var witchspaceTarget: StarSystem? = null
        private set
    val inWitchspace: Boolean get() = model.witchspace

    /** Abates e recompensas deste voo (vão para o livro de contas ao atracar). */
    var kills = 0
        private set
    var bounty = 0
        private set

    /** Mensagem atual do HUD e por quanto tempo (s) ainda aparece. */
    var message: String? = null
        private set
    private var messageTime = 0f

    init {
        val c = model.combat
        c.onBounty = { _, n -> bounty += n; game.collectBounty(n) }
        c.onKill = { ship ->
            kills++
            // a lei não perdoa quem derruba polícia ou inocentes no espaço dela
            if (!model.witchspace) when {
                ship.newb and Newb.COP != 0 -> game.aggression(COP_KILL_HEAT)
                ship.newb and Newb.INNOCENT != 0 || ship.newb and Newb.TRADER != 0 -> game.aggression(INNOCENT_KILL_HEAT)
            }
        }
        c.onAggression = { if (!model.witchspace) game.aggression(AGGRESSION_HEAT) }
        c.contraband = { game.evidence().values.sum() }
        c.onScoop = { game.scoopCanister(rng)?.let { "+1 t de ${it.label.lowercase()}" } }
    }

    /** TT110: decola da estação do sistema atual. A estação rearma os mísseis e o laser é o do estaleiro. */
    fun launch() {
        model.launch(systemInfo(game.location))
        equip()
        flash("${game.location.name.uppercase()} · ESTAÇÃO ATRÁS", 3f)
    }

    private fun equip() {
        val p = model.player
        p.lasers[0] = Upgrade.laserFor(game.level(Upgrade.LASER))
        p.missiles = MISSILES
        p.legalStatus = game.legalStatus() or (game.evidence().values.sum().coerceAtMost(40)) // TT110: carga ilegal suja a ficha
    }

    /** Distância em LY do hiperespaço para o destino atual (o custo, já descontado o que o witchspace queimou). */
    fun jumpCost(): Int? = target?.let { (game.fuelCost(it) - prepaidFor(it)).coerceAtLeast(0) }

    private fun prepaidFor(s: StarSystem) = if (s === witchspaceTarget) prepaid else 0

    /** Troca o destino (o mapa de bordo); o sistema atual não é destino. */
    fun selectTarget(s: StarSystem) {
        if (s === game.location) return
        target = s
        if (model.hyperspace.active) { model.hyperspace.cancel(); flash("CONTAGEM CANCELADA") }
    }

    /**
     * "H": começa (ou cancela) a contagem do hiperespaço. Recusa sem destino, sem combustível, longe demais ou
     * ainda na massa da estação. Retorna a mensagem mostrada.
     */
    fun toggleHyperspace(): String {
        if (model.hyperspace.active) { model.hyperspace.cancel(); return flash("CONTAGEM CANCELADA") }
        val t = target ?: return flash("ESCOLHA UM DESTINO")
        when (game.jumpCheck(t, prepaidFor(t))) {
            null -> Unit
            Failure.SEM_COMBUSTIVEL -> return flash("COMBUSTÍVEL INSUFICIENTE")
            Failure.FORA_DE_ALCANCE -> return flash("FORA DE ALCANCE")
            else -> return flash("AGORA NÃO")
        }
        model.hyperspaceBlock()?.let { return flash(it) }
        model.hyperspace.start()
        return flash("HIPERESPAÇO: ${t.name.uppercase()}")
    }

    /** "J": salto curto dentro do sistema. */
    fun inSystemJump(): String = flash(model.inSystemJump() ?: "SALTO")

    /** "C": computador de atracação. Retorna o resultado se atracou. */
    fun dockingComputer(): FlightResult? {
        model.dockingComputerBlock()?.let { flash(it); return null }
        return finishDocked()
    }

    fun missile(): String? = model.combat.missileButton()?.let { flash(it) }

    /** Vista dianteira / traseira (as laterais do original ficam no teclado). */
    fun cycleView(next: View? = null) {
        model.view = next ?: if (model.view == View.FRONT) View.REAR else View.FRONT
    }

    /** Um quadro de [delta] s. Retorna algo só quando o voo termina ou o salto acontece. */
    fun update(delta: Float): FlightResult? {
        messageTime -= delta
        val e = model.update(delta, controls)
        if (messageTime <= 0f) model.combat.messages.removeFirstOrNull()?.let { flash(it) }
        return when (e) {
            FlightEvent.DOCKED -> finishDocked()
            FlightEvent.CRASHED -> FlightResult.Destroyed(game.shipDestroyed())
            FlightEvent.HYPERSPACE -> jump()
            FlightEvent.BUMPED -> { flash("BATIDA"); null }
            FlightEvent.STATION_APPEARED, FlightEvent.NONE -> null
        }
    }

    private fun finishDocked(): FlightResult {
        game.logFlight(kills, bounty)
        return FlightResult.Docked(game.location)
    }

    /**
     * TT18: o salto. Em [MISJUMP_IN_256]/256 dos saltos (fora do witchspace) ele falha no meio e caímos entre
     * as estrelas com os Thargoids; senão o jogo viaja (combustível, dias, chegada) e o voo recomeça no destino.
     */
    private fun jump(): FlightResult? {
        val t = target ?: return null
        val from = game.location
        if (!model.witchspace && rng.nextInt(256) < MISJUMP_IN_256) {
            prepaid = game.misjump(t)
            witchspaceTarget = t
            model.enterWitchspace()
            flash("WITCHSPACE! THARGOIDS", 4f)
            return FlightResult.Misjumped(from, t)
        }
        val ly = game.fuelCost(t)
        val days = game.travelDays(t)
        val rulers = game.world.region.systems.associateWith { game.world.controller(it) }
        val outcome = game.travel(t, prepaidFor(t))
        if (outcome is Outcome.Fail) { flash("O SALTO NÃO ACONTECEU"); return null }
        prepaid = 0; witchspaceTarget = null; target = null
        model.arrive(systemInfo(game.location))
        model.player.legalStatus = maxOf(model.player.legalStatus, game.legalStatus())
        flash(game.location.name.uppercase(), 4f)
        val revolts = rulers.mapNotNull { (s, old) -> game.world.controller(s).takeIf { it.id != old.id }?.let { Triple(s, old, it) } }
        return FlightResult.Jumped(from, game.location, ly, days, outcome, revolts)
    }

    /** Mostra [text] no HUD por [seconds]. */
    fun flash(text: String, seconds: Float = 2.2f): String {
        message = text; messageTime = seconds
        return text
    }

    companion object {
        const val MISSILES = 3
        /** Chance de o salto falhar (o original: 3/256; aqui um pouco mais, para a experiência existir). */
        const val MISJUMP_IN_256 = 8
        const val COP_KILL_HEAT = 40
        const val INNOCENT_KILL_HEAT = 15
        const val AGGRESSION_HEAT = 10

        /** O que o Elite chama de governo vem da vigilância: pouca vigilância é anarquia, muita é corporativo. */
        fun systemInfo(s: StarSystem) = FlightSystem(
            government = (s.traits.surveillance * 8 / 101).coerceIn(0, 7),
            techLevel = s.traits.techLevel,
            seed = s.seedKey(),
        )
    }
}
