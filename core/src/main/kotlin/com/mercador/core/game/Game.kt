package com.mercador.core.game

import com.mercador.core.economy.Commodity
import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive
import com.mercador.core.story.StoryCard
import com.mercador.core.story.StoryEngine
import com.mercador.core.universe.Doctrine
import com.mercador.core.universe.StarSystem
import com.mercador.core.world.MeterModel
import com.mercador.core.world.Side
import com.mercador.core.world.World
import java.util.EnumMap
import kotlin.math.ceil

/** Uma linha do livro de contas: o registro permanente do que o jogador fez (seção 5). */
data class LedgerEntry(val day: Int, val systemName: String, val text: String)

enum class Failure {
    SEM_DINHEIRO, SEM_PORAO, SEM_ESTOQUE, SEM_CARGA, SEM_COMBUSTIVEL, FORA_DE_ALCANCE, QUANTIDADE_INVALIDA, TANQUE_CHEIO, PERSEGUICAO_PENDENTE, SEM_PERSEGUICAO, RESPOSTA_INDISPONIVEL, SEM_DIVIDA, NAO_ELEGIVEL, SEM_CENA, POSTURA_INDISPONIVEL, SEM_HISTORIA, ESCOLHA_INDISPONIVEL, SEM_BAR
}

sealed interface Outcome {
    data class Ok(val credits: Int) : Outcome
    data class Fail(val reason: Failure) : Outcome
    /** A viagem terminou, mas uma patrulha alcançou o jogador: resolver com [Game.respond]. */
    data class Pursued(val pursuit: Pursuit) : Outcome
    data class Resolved(val resolution: Resolution) : Outcome
    /** Uma cena de confronto começou (ou segue) e espera uma postura: [Game.act]. */
    data class Confronting(val scene: Confrontation) : Outcome
    /** Uma storylet está à espera de escolha: [Game.chooseStory]. */
    data class Story(val card: StoryCard) : Outcome
    data class StoryResolved(val text: String, val scene: Confrontation?) : Outcome
    data class SceneEnded(val scene: Confrontation, val ending: Ending, val text: String) : Outcome
}

/** Estado do mercador solitário e o laço comprar → viajar → vender. */
class Game(val world: World, start: StarSystem, var credits: Int = START_CREDITS, val stories: Boolean = true) {
    var location: StarSystem = start
        private set
    var fuel: Int = FUEL_CAPACITY
        private set
    val cargo: MutableMap<Commodity, Int> = EnumMap(Commodity::class.java)
    val factions = world.region.universe.factions(world.region.galaxy)
    val standing = Standing(factions)
    var pursuit: Pursuit? = null
        private set
    var debt: Int = 0
        private set
    var confrontation: Confrontation? = null
        private set
    private val traitExpiry = HashMap<Trait, Int?>()
    val traits: Set<Trait> get() = traitExpiry.keys
    /** Segredos usáveis como moeda, por facção sobre a qual se sabe algo. */
    private val secrets = ArrayList<Int>()
    private val favors = IntArray(factions.factions.size)
    private val retaliations = ArrayList<Retaliation>()
    private var sceneWins = 0
    private var intimidationWins = 0
    var pendingStory: StoryCard? = null
        private set
    private val _voice = ArrayList<LedgerEntry>()
    /** Voz interior: frases curtas de monólogo que reagem ao que o jogador fez (seção 8.4). */
    val voiceLog: List<LedgerEntry> get() = _voice
    private val storySeen = HashMap<String, Int>()
    val busy: Boolean get() = pursuit != null || confrontation != null || pendingStory != null

    private class Retaliation(val due: Int, val avenger: Person)
    private val _ledger = ArrayList<LedgerEntry>()
    val ledger: List<LedgerEntry> get() = _ledger

    val day: Int get() = world.day
    val faction get() = location.faction
    fun hasTrait(t: Trait) = t in traitExpiry
    fun favors(f: com.mercador.core.universe.Faction) = favors[f.id]
    fun secretsAbout(f: com.mercador.core.universe.Faction) = secrets.count { it == f.id }
    fun grantFavor(f: com.mercador.core.universe.Faction) { favors[f.id] = (favors[f.id] + 1).coerceAtMost(3) }
    fun grantSecret(f: com.mercador.core.universe.Faction) { secrets += f.id }
    fun hasFame() = traitExpiry.keys.any { it.feared }
    fun pendingRetaliations() = retaliations.size

    /** Consequências em aberto, à vista do jogador no livro (seção 10.1): quem virá e quando. */
    fun openConsequences(): List<Pair<Person, Int>> = retaliations.map { it.avenger to it.due }

    /** Concede um traço (também usado para restaurar um save). */
    fun grantTrait(t: Trait, days: Int? = null) = addTrait(t, days)

    /** Dias restantes de um traço que se dissipa; null se permanente ou ausente. */
    fun traitDaysLeft(t: Trait): Int? = traitExpiry[t]?.let { (it - day).coerceAtLeast(0) }
    fun isIllegalHere(c: Commodity) = c.bannedBy(location.traits.ideology)
    val holdUsed: Int get() = cargo.values.sum()
    val holdFree: Int get() = HOLD_CAPACITY - holdUsed

    fun fuelCost(to: StarSystem): Int = ceil(world.region.distanceLy(location, to)).toInt().coerceAtLeast(1)
    fun travelDays(to: StarSystem): Int = 1 + ceil(world.region.distanceLy(location, to) / 2).toInt()

    /** Destinos que o combustível atual alcança. */
    fun reachable(): List<StarSystem> =
        world.region.systems.filter { it !== location && fuelCost(it) <= fuel }.sortedBy { world.region.distanceLy(location, it) }

    fun buy(c: Commodity, tons: Int): Outcome {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE)
        if (tons <= 0) return Outcome.Fail(Failure.QUANTIDADE_INVALIDA)
        if (tons > world.quote(location, c).stock) return Outcome.Fail(Failure.SEM_ESTOQUE)
        if (tons > holdFree) return Outcome.Fail(Failure.SEM_PORAO)
        val cost = world.unitPrice(location, c, tons.toDouble(), Side.BUY) * tons
        if (cost > credits) return Outcome.Fail(Failure.SEM_DINHEIRO)
        credits -= cost
        cargo[c] = (cargo[c] ?: 0) + tons
        world.recordTrade(location, c, tons, Side.BUY)
        heatFromIllegalTrade(c, tons)
        return Outcome.Ok(credits)
    }

    fun sell(c: Commodity, tons: Int): Outcome {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE)
        if (tons <= 0) return Outcome.Fail(Failure.QUANTIDADE_INVALIDA)
        val have = cargo[c] ?: 0
        if (tons > have) return Outcome.Fail(Failure.SEM_CARGA)
        val before = world.meters(location)
        credits += world.unitPrice(location, c, tons.toDouble(), Side.SELL) * tons
        if (have == tons) cargo.remove(c) else cargo[c] = have - tons
        world.recordTrade(location, c, tons, Side.SELL)
        recordConsequence(c, tons, before)
        heatFromIllegalTrade(c, tons)
        return Outcome.Ok(credits)
    }

    fun refuel(): Outcome {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE)
        val missing = FUEL_CAPACITY - fuel
        if (missing == 0) return Outcome.Fail(Failure.TANQUE_CHEIO)
        val cost = missing * FUEL_PRICE
        if (cost > credits) return Outcome.Fail(Failure.SEM_DINHEIRO)
        credits -= cost
        fuel = FUEL_CAPACITY
        return Outcome.Ok(credits)
    }

    fun travel(to: StarSystem): Outcome {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE)
        if (to === location || to !in world.region.systems) return Outcome.Fail(Failure.FORA_DE_ALCANCE)
        val cost = fuelCost(to)
        if (cost > FUEL_CAPACITY) return Outcome.Fail(Failure.FORA_DE_ALCANCE)
        if (cost > fuel) return Outcome.Fail(Failure.SEM_COMBUSTIVEL)
        fuel -= cost
        val days = travelDays(to)
        world.advance(days)
        standing.cool(days)
        expireTraits()
        location = to
        val arrived = arrive()
        if (!stories || arrived !is Outcome.Ok) return arrived
        val card = StoryEngine.draw(this) ?: return arrived
        storySeen[card.storylet.id] = day
        pendingStory = card
        return Outcome.Story(card)
    }

    /** Escolhe uma opção da storylet pendente; o efeito e o texto do desfecho vêm da própria storylet. */
    fun chooseStory(index: Int): Outcome {
        val card = pendingStory ?: return Outcome.Fail(Failure.SEM_HISTORIA)
        if (index !in card.choices.indices || !card.choices[index].available) return Outcome.Fail(Failure.ESCOLHA_INDISPONIVEL)
        pendingStory = null
        val text = StoryEngine.resolve(this, card, index)
        return Outcome.StoryResolved(text, confrontation)
    }

    /** Provas que a lei do sistema atual enxerga no porão. */
    fun evidence(): Map<Commodity, Int> = cargo.filter { (c, _) -> isIllegalHere(c) }

    /**
     * Ao chegar: o sorteio é função do sistema e do dia (determinístico e testável).
     * Sem provas, a patrulha só revista; com provas, abre uma perseguição.
     */
    private fun arrive(): Outcome {
        retaliations.firstOrNull { it.due <= day }?.let { r ->
            retaliations.remove(r)
            return startScene(r.avenger, emptyMap(), retaliation = true)
        }
        val f = faction
        val restricted = standing.isRestricted(f)
        if (restricted) standing.addHeat(f, 10)
        val evidence = evidence()
        val p = PursuitRules.encounterChance(standing.heat(f), location.traits.surveillance, evidence.isNotEmpty(), restricted)
        if (Rng(derive(location.seedKey(), day.toLong(), 0xCA5E)).nextDouble() >= p) return Outcome.Ok(credits)
        if (evidence.isEmpty()) {
            standing.addHeat(f, -10)
            note("Revistado pela patrulha de ${f.name}: nada encontrado.")
            return Outcome.Ok(credits)
        }
        pursuit = Pursuit(location, f, evidence)
        return Outcome.Pursued(pursuit!!)
    }

    fun responses(): List<Response> {
        val p = pursuit ?: return emptyList()
        return Response.values().filter { r ->
            when (r) {
                Response.SUBORNAR -> credits >= PursuitRules.bribeCost(p)
                Response.FUGIR -> fuel >= PursuitRules.FLEE_FUEL
                else -> true
            }
        }
    }

    /** Resolve a perseguição pendente por uma decisão com custo. */
    fun respond(r: Response): Outcome {
        val p = pursuit ?: return Outcome.Fail(Failure.SEM_PERSEGUICAO)
        if (r !in responses()) return Outcome.Fail(Failure.RESPOSTA_INDISPONIVEL)
        val f = p.patrol
        val roll = Rng(derive(location.seedKey(), day.toLong(), r.ordinal.toLong(), 0xB1FF)).nextDouble()
        val res = when (r) {
            Response.SUBORNAR -> {
                val cost = PursuitRules.bribeCost(p)
                if (roll < PursuitRules.bribeChance(p)) {
                    credits -= cost
                    standing.addHeat(f, -10); standing.addReputation(f, -3)
                    note("Subornou a patrulha de ${f.name} por $cost cr.")
                    Resolution(r, true, 0, cost, "Aceitaram o dinheiro e olharam para o outro lado.")
                } else {
                    val lost = cost / 2
                    credits -= lost
                    caught(p, r, "Recusaram o suborno e ficaram com $lost cr.", extraPaid = lost)
                }
            }
            Response.JOGAR_FORA -> {
                confiscate(p)
                standing.addHeat(f, -10)
                note("Jogou ${p.evidenceTons} t de carga no vazio para sumir com a prova.")
                Resolution(r, true, p.evidenceTons, 0, "A prova sumiu. O lucro também.")
            }
            Response.BLEFAR -> {
                if (roll < PursuitRules.bluffChance(p, standing.reputation(f))) {
                    note("Blefou diante da patrulha de ${f.name} e passou.")
                    Resolution(r, true, 0, 0, "Acreditaram em você. Desta vez.")
                } else {
                    standing.addReputation(f, -5)
                    caught(p, r, "O blefe caiu.")
                }
            }
            Response.FUGIR -> {
                fuel -= PursuitRules.FLEE_FUEL
                if (roll < PursuitRules.fleeChance(p)) {
                    standing.addHeat(f, 15); standing.addReputation(f, -3)
                    val refuge = world.region.systems.filter { it.faction.id != f.id }
                        .minByOrNull { world.region.distanceLy(location, it) } ?: location
                    world.advance(1); standing.cool(1)
                    location = refuge
                    note("Fugiu da patrulha de ${f.name} e se refugiou em ${refuge.name}.")
                    Resolution(r, true, 0, 0, "Escapou para ${refuge.name}, em território de ${refuge.faction.name}.")
                } else {
                    standing.addHeat(f, 15)
                    caught(p, r, "A fuga falhou.")
                }
            }
            Response.CONFRONTAR -> {
                pursuit = null
                val person = People.generate(Rng(derive(location.seedKey(), day.toLong(), 0x9E0)), f)
                return startScene(person, p.evidence, retaliation = false)
            }
            Response.ENTREGAR_SE -> {
                val fine = minOf(PursuitRules.surrenderFine(p), credits / 2)
                credits -= fine
                confiscate(p)
                standing.setHeatAtMost(f, 10); standing.addReputation(f, -3)
                note("Entregou-se à patrulha de ${f.name}: ${p.evidenceTons} t confiscadas e multa de $fine cr.")
                Resolution(r, false, p.evidenceTons, fine, "Negociou a rendição: perdeu a carga e pagou a multa.")
            }
        }
        pursuit = null
        return Outcome.Resolved(res)
    }

    /** Rede de segurança contra o beco sem saída: falido e de porão vazio, o mercador pode se endividar (traço Devedor). */
    fun takeLoan(): Outcome {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE)
        if (debt > 0 || credits >= LOAN_THRESHOLD || holdUsed > 0) return Outcome.Fail(Failure.NAO_ELEGIVEL)
        credits += LOAN_AMOUNT
        addTrait(Trait.DEVEDOR, null)
        debt = LOAN_AMOUNT + LOAN_AMOUNT / 5
        note("Pediu $LOAN_AMOUNT cr emprestados; deve $debt.")
        return Outcome.Ok(credits)
    }

    /** Segunda rede: frete miúdo que gasta tempo e não dá lucro de verdade. Garante que toda ruína é recuperável. */
    fun haulFreight(): Outcome {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE)
        if (credits >= FREIGHT_THRESHOLD || holdUsed > 0) return Outcome.Fail(Failure.NAO_ELEGIVEL)
        credits += FREIGHT_PAY
        world.advance(FREIGHT_DAYS)
        standing.cool(FREIGHT_DAYS)
        expireTraits()
        note("Carregou frete miúdo por $FREIGHT_DAYS dias para pagar o combustível.")
        return Outcome.Ok(credits)
    }

    fun repay(): Outcome {
        if (debt == 0) return Outcome.Fail(Failure.SEM_DIVIDA)
        val pay = minOf(credits, debt)
        credits -= pay; debt -= pay
        if (debt == 0) { note("Quitou a dívida."); traitExpiry.remove(Trait.DEVEDOR) }
        return Outcome.Ok(credits)
    }

    internal fun addTrait(t: Trait, days: Int?) {
        traitExpiry[t] = if (t.permanent) null else days?.let { day + it }
    }

    internal fun removeTrait(t: Trait) { traitExpiry.remove(t) }

    internal fun adjustFuel(d: Int) { fuel = (fuel + d).coerceIn(0, FUEL_CAPACITY) }

    internal fun addVoice(text: String) { _voice += LedgerEntry(day, location.name, text) }

    /** Passa tempo fora de uma viagem: esfria a atenção e dissipa traços. */
    internal fun passDays(n: Int) { world.advance(n); standing.cool(n); expireTraits() }

    internal fun lastSeen(id: String): Int? = storySeen[id]

    private fun expireTraits() {
        traitExpiry.entries.removeAll { (t, until) -> until != null && until <= day && t != Trait.DEVEDOR }
    }

    internal fun startScene(person: Person, evidence: Map<Commodity, Int>, retaliation: Boolean): Outcome {
        val f = person.faction
        val value = evidence.entries.sumOf { (c, t) -> c.basePrice * t }
        val poise = 60 + traits.sumOf { it.poise }
        val playerLev = 20 + standing.reputation(f) / 5 + favors[f.id] * 10 + secrets.count { it == f.id } * 15 + cargoValue() / 100
        val oppPoise = 45 + when (f.doctrine) { Doctrine.BRUTAL -> 15; Doctrine.BUROCRATICA -> 5; Doctrine.COBRADORA -> 10; Doctrine.MERCANTIL -> 0 }
        val oppLev = 30 + location.traits.surveillance / 2 + value / 100 + if (retaliation) 20 else 0
        val key = derive(location.seedKey(), person.name.hashCode().toLong())
        val scene = Confrontation(person, evidence, retaliation, poise, playerLev, oppPoise, oppLev,
            Confrontation.rounds(location.traits.surveillance, key), key, day)
        confrontation = scene
        return Outcome.Confronting(scene)
    }

    private fun cargoValue() = cargo.entries.sumOf { (c, t) -> c.basePrice * t }

    /** Compra um rumor no bar (só atracado e livre de pendências). Devolve o texto verdadeiro. */
    fun buyRumor(offer: BarOffer): Pair<Outcome, String?> {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE) to null
        if (credits < offer.price) return Outcome.Fail(Failure.SEM_DINHEIRO) to null
        credits -= offer.price
        val text = when (offer.kind) {
            RumorKind.ONDE_VENDER -> Bar.bestSale(this)?.let { (c, d) ->
                "No bar dizem que o mercado de ${c.label.lowercase()} paga bem em ${d.name}."
            } ?: "Ninguém sabe de nada que valha o salto agora."
            RumorKind.RISCO_DA_ROTA -> {
                val d = reachable().maxByOrNull { standing.heat(it.faction) + it.traits.surveillance }
                if (d == null) "Sem combustível, a rota não importa." else {
                    val p = PursuitRules.encounterChance(standing.heat(d.faction), d.traits.surveillance, evidenceFor(d).isNotEmpty(), standing.isRestricted(d.faction))
                    "Em ${d.name}, a patrulha de ${d.faction.name} te abordaria com ${(p * 100).toInt()}% de chance."
                }
            }
            RumorKind.SEGREDO -> { grantSecret(offer.about!!); "Você sai do bar sabendo algo sobre ${offer.about.name} que não devia." }
        }
        note("Pagou ${offer.price} cr por informação no bar.")
        return Outcome.Ok(credits) to text
    }

    private fun evidenceFor(d: StarSystem) = cargo.filter { (c, _) -> c.bannedBy(d.traits.ideology) }

    /** Posturas disponíveis agora (seção 8.2). */
    fun postures(): List<Posture> {
        val s = confrontation ?: return emptyList()
        val f = s.opponent.faction
        return Posture.values().filter {
            when (it) {
                Posture.INTIMIDAR -> hasFame()
                Posture.EXPOR_SEGREDO -> secretsAbout(f) > 0
                Posture.COBRAR_FAVOR -> favors[f.id] > 0
                Posture.SACRIFICAR -> !s.retaliation && s.evidence.isNotEmpty() && !s.sacrificed
                else -> true
            }
        }
    }

    /** Joga uma rodada da cena. Encerra a cena e aplica as consequências quando ela termina. */
    fun act(posture: Posture): Outcome {
        val s = confrontation ?: return Outcome.Fail(Failure.SEM_CENA)
        if (posture !in postures()) return Outcome.Fail(Failure.POSTURA_INDISPONIVEL)
        val f = s.opponent.faction
        when (posture) {
            Posture.EXPOR_SEGREDO -> secrets.remove(f.id)
            Posture.COBRAR_FAVOR -> favors[f.id]--
            Posture.SACRIFICAR -> dropHalfEvidence(s)
            else -> {}
        }
        s.step(posture, hasFame())
        val ending = s.ending ?: return Outcome.Confronting(s)
        return finish(s, ending)
    }

    private fun dropHalfEvidence(s: Confrontation) {
        for ((c, t) in s.evidence) {
            val drop = (t + 1) / 2
            val left = (cargo[c] ?: 0) - drop
            if (left <= 0) cargo.remove(c) else cargo[c] = left
        }
    }

    /** Toda cena vira uma linha do livro de contas que nunca é apagada (seção 8.4). */
    private fun finish(s: Confrontation, ending: Ending): Outcome {
        confrontation = null
        val f = s.opponent.faction
        val who = "${s.opponent.name}, ${s.opponent.role} de ${f.name}"
        val text: String
        when (ending) {
            Ending.ELE_CEDEU -> {
                standing.addHeat(f, -15); standing.addReputation(f, -1)
                sceneWins++
                intimidationWins += s.intimidations
                if (sceneWins >= 3) addTrait(Trait.CALEJADO, null)
                if (intimidationWins >= 2) addTrait(Trait.FRIO, 120)
                text = "Diante de $who, foi o outro lado que cedeu."
            }
            Ending.VOCE_CEDEU, Ending.EMPATE -> {
                val left = remainingEvidence(s)
                val lostEvidence = left.values.sum()
                val fine = if (s.retaliation) credits / 4 else {
                    val p = Pursuit(location, f, left)
                    if (ending == Ending.VOCE_CEDEU) PursuitRules.fine(p) else PursuitRules.surrenderFine(p)
                }.coerceAtMost(credits / 2)
                credits -= fine
                confiscate(Pursuit(location, f, left))
                standing.setHeatAtMost(f, 20); standing.addReputation(f, -3)
                if (ending == Ending.VOCE_CEDEU) addTrait(Trait.INSONE, 60)
                text = (if (ending == Ending.VOCE_CEDEU) "Você cedeu a $who" else "Sem vencedor diante de $who") +
                    ": perdeu ${lostEvidence} t de carga e pagou $fine cr."
            }
            Ending.VIOLENCIA -> {
                standing.addHeat(f, 40); standing.addReputation(f, -30)
                addTrait(Trait.MARCADO, null); addTrait(Trait.INSONE, 90)
                val avenger = People.generate(Rng(derive(location.seedKey(), day.toLong(), 0xE7E)), f, "parente de ${s.opponent.name}")
                retaliations += Retaliation(day + Rng(derive(location.seedKey(), day.toLong(), 0xD1E)).range(30, 90), avenger)
                text = "Você partiu para a violência contra $who. ${avenger.name} não vai esquecer."
            }
        }
        note(text)
        return Outcome.SceneEnded(s, ending, text)
    }

    private fun remainingEvidence(s: Confrontation): Map<Commodity, Int> =
        s.evidence.mapValues { (_, t) -> if (s.sacrificed) t - (t + 1) / 2 else t }.filterValues { it > 0 }

    private fun confiscate(p: Pursuit) {
        for ((c, t) in p.evidence) {
            val left = (cargo[c] ?: 0) - t
            if (left <= 0) cargo.remove(c) else cargo[c] = left
        }
    }

    private fun caught(p: Pursuit, r: Response, why: String, extraPaid: Int = 0): Resolution {
        val fine = minOf(PursuitRules.fine(p), credits / 2)
        credits -= fine
        confiscate(p)
        standing.setHeatAtMost(p.patrol, 20); standing.addReputation(p.patrol, -5)
        note("Apanhado pela patrulha de ${p.patrol.name}: ${p.evidenceTons} t confiscadas e multa de $fine cr.")
        return Resolution(r, false, p.evidenceTons, fine + extraPaid, "$why Perdeu a carga e pagou $fine cr de multa.")
    }

    /** Negociar bens proibidos chama a atenção do poder local, menos onde há corrupção. */
    private fun heatFromIllegalTrade(c: Commodity, tons: Int) {
        val f = faction
        if (isIllegalHere(c)) {
            val gain = Math.ceil(tons * (0.5 + location.traits.surveillance / 100.0) * (1 - location.traits.corruption / 200.0)).toInt()
            standing.addHeat(f, gain)
            standing.addReputation(f, -Math.max(1, tons / 3))
        }
    }

    /** O livro anota o que importa: entregas em racionamento, armas num povo faminto, e a revolta. */
    private fun recordConsequence(c: Commodity, tons: Int, before: com.mercador.core.world.Meters) {
        val name = location.name
        if (c in MeterModel.essential && before.rationing && tons >= 5) {
            note("Entregou $tons t de ${c.label} durante o racionamento.")
            standing.addReputation(faction, Math.max(1, tons / 5))
            if (tons >= 10) grantFavor(faction)
        }
        if (c == Commodity.ARMAS && tons >= 5 && before.discontent >= 60) {
            note("Vendeu $tons t de armas a um poder que o povo já não suporta.")
        }
        if (!before.unrest && world.meters(location).unrest) {
            note("Seu carregamento de ${c.label} foi a última gota: o sistema está à beira da revolta.")
        }
    }

    internal fun note(text: String) { _ledger += LedgerEntry(day, location.name, text) }

    /** Pode salvar agora? Com uma decisão pendente, o último momento calmo é que vale. */
    val canSave: Boolean get() = !busy

    /** Foto do mundo vivido (ver [GameSave]). Decisões pendentes ficam de fora. */
    fun toSave(): GameSave {
        val (deltas, vols) = world.exportDeltas()
        fun ref(s: StarSystem) = SysRef(s.id.sx, s.id.sy, s.id.index)
        fun ref(id: com.mercador.core.universe.SystemId) = SysRef(id.sx, id.sy, id.index)
        return GameSave(
            universeSeed = world.region.universe.seed, galaxy = world.region.galaxy,
            region = world.region.systems.map(::ref), stories = stories, day = world.day,
            meterDeltas = deltas.entries.sortedWith(compareBy({ it.key.sx }, { it.key.sy }, { it.key.index }))
                .map { (k, d) -> MeterDeltaSave(ref(k), d[0], d[1], d[2]) },
            volumes = vols.entries.sortedWith(compareBy({ it.key.first.sx }, { it.key.first.sy }, { it.key.first.index }, { it.key.second.ordinal }))
                .map { (k, v) -> VolumeSave(ref(k.first), k.second.name, v) },
            credits = credits, fuel = fuel, debt = debt, location = ref(location),
            cargo = cargo.entries.sortedBy { it.key.ordinal }.associate { it.key.name to it.value },
            ledger = _ledger.map { LedgerSave(it.day, it.systemName, it.text) },
            voice = _voice.map { LedgerSave(it.day, it.systemName, it.text) },
            traits = traitExpiry.entries.sortedBy { it.key.ordinal }.map { TraitSave(it.key.name, it.value) },
            secrets = secrets.toList(), favors = favors.toList(),
            heat = standing.heatValues(), reputation = standing.reputationValues(),
            retaliations = retaliations.map { RetaliationSave(it.due, PersonSave(it.avenger.name, it.avenger.role, it.avenger.faction.id)) },
            sceneWins = sceneWins, intimidationWins = intimidationWins,
            storySeen = storySeen.toSortedMap(),
        )
    }

    companion object {
        /** Reconstrói o jogo: o mundo gerado vem da semente, o vivido vem do [save]. */
        fun fromSave(save: GameSave): Game {
            val universe = com.mercador.core.universe.Universe(save.universeSeed)
            fun sys(r: SysRef) = universe.system(com.mercador.core.universe.SystemId(save.galaxy, r.sx, r.sy, r.index))
                ?: throw SaveException("Sistema ${r.sx},${r.sy}#${r.index} não existe neste universo.")
            val systems = save.region.map(::sys)
            val world = World(com.mercador.core.universe.Region(universe, save.galaxy, systems))
            fun id(r: SysRef) = com.mercador.core.universe.SystemId(save.galaxy, r.sx, r.sy, r.index)
            fun <T : Enum<T>> named(cls: Class<T>, name: String): T =
                cls.enumConstants.firstOrNull { it.name == name } ?: throw SaveException("Valor desconhecido no save: $name")
            world.restore(
                save.day,
                save.meterDeltas.associate { id(it.system) to doubleArrayOf(it.control, it.discontent, it.supply) },
                save.volumes.associate { (id(it.system) to named(Commodity::class.java, it.commodity)) to it.tons },
            )
            // o local atual tem de ser a MESMA instância da lista da região: o jogo compara sistemas por identidade
            val here = world.region.systems.firstOrNull { it.id == id(save.location) }
                ?: throw SaveException("A localização do save não pertence à região.")
            val g = Game(world, here, save.credits, save.stories)
            g.fuel = save.fuel.coerceIn(0, FUEL_CAPACITY)
            g.debt = save.debt
            save.cargo.forEach { (k, v) -> g.cargo[named(Commodity::class.java, k)] = v }
            save.ledger.forEach { g._ledger += LedgerEntry(it.day, it.system, it.text) }
            save.voice.forEach { g._voice += LedgerEntry(it.day, it.system, it.text) }
            save.traits.forEach { g.traitExpiry[named(Trait::class.java, it.trait)] = it.until }
            g.secrets += save.secrets
            if (save.favors.size != g.favors.size) throw SaveException("Número de facções diferente do save.")
            save.favors.forEachIndexed { i, v -> g.favors[i] = v }
            try { g.standing.restore(save.heat, save.reputation) } catch (e: IllegalArgumentException) { throw SaveException(e.message ?: "save inválido", e) }
            for (r in save.retaliations) {
                val f = g.factions.factions.firstOrNull { it.id == r.avenger.faction } ?: throw SaveException("Facção desconhecida no save.")
                g.retaliations += Retaliation(r.due, Person(r.avenger.name, r.avenger.role, f))
            }
            g.sceneWins = save.sceneWins; g.intimidationWins = save.intimidationWins
            g.storySeen.putAll(save.storySeen)
            return g
        }

        const val START_CREDITS = 1000
        const val HOLD_CAPACITY = 20
        const val FUEL_CAPACITY = 20
        const val FUEL_PRICE = 10
        const val LOAN_AMOUNT = 300
        const val FREIGHT_PAY = 150
        const val FREIGHT_THRESHOLD = 250
        const val FREIGHT_DAYS = 5
        const val LOAN_THRESHOLD = 100
    }
}
