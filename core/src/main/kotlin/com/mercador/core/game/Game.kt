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
import com.mercador.core.world.NewsKind
import com.mercador.core.world.World
import com.mercador.core.world.WorldNews
import java.util.EnumMap
import kotlin.math.ceil

/** Uma linha do livro de contas: o registro permanente do que o jogador fez (seção 5). */
data class LedgerEntry(val day: Int, val systemName: String, val text: String)

/** Preços de venda (por ordinal de [Commodity]) que o jogador viu num sistema, e quando. */
class KnownMarket(val day: Int, val sell: List<Int>)

/** Estimativa de venda da carga num destino: receita, lucro sobre o custo, toneladas proibidas lá e contratos a receber. */
class SaleEstimate(val revenue: Int, val profit: Int, val bannedTons: Int, val contractPay: Int, val seenDay: Int)

enum class Failure {
    SEM_DINHEIRO, SEM_PORAO, SEM_ESTOQUE, SEM_CARGA, SEM_COMBUSTIVEL, FORA_DE_ALCANCE, QUANTIDADE_INVALIDA, TANQUE_CHEIO, PERSEGUICAO_PENDENTE, SEM_PERSEGUICAO, RESPOSTA_INDISPONIVEL, SEM_DIVIDA, NAO_ELEGIVEL, SEM_CENA, POSTURA_INDISPONIVEL, SEM_HISTORIA, ESCOLHA_INDISPONIVEL, SEM_BAR,
    SEM_CONTRATO, LIMITE_DE_CONTRATOS, CONTRATO_EM_OUTRO_LUGAR, SEM_ESTALEIRO, NIVEL_MAXIMO, SEM_CAMPANHA
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

/**
 * Estado do mercador solitário e o laço comprar → viajar → vender.
 * Com [campaign], o jogo começa com a dívida da campanha (ver [Campaign]); sem ela, é modo livre.
 */
class Game(val world: World, start: StarSystem, var credits: Int = START_CREDITS, val stories: Boolean = true, campaign: Boolean = false) {
    var location: StarSystem = start
        private set
    private val upgrades: MutableMap<Upgrade, Int> = EnumMap(Upgrade::class.java)
    fun level(u: Upgrade) = upgrades[u] ?: 0
    val holdCapacity: Int get() = HOLD_CAPACITY + level(Upgrade.PORAO) * HOLD_STEP
    val fuelCapacity: Int get() = FUEL_CAPACITY + level(Upgrade.TANQUE) * FUEL_STEP
    /** Toneladas de contrabando que o compartimento oculto esconde da revista. */
    val hiddenCapacity: Int get() = level(Upgrade.COMPARTIMENTO) * HIDDEN_STEP
    var fuel: Int = FUEL_CAPACITY
        private set
    /** Contratos aceitos e ainda em aberto. */
    private val _contracts = ArrayList<Contract>()
    val contracts: List<Contract> get() = _contracts
    /** Ofertas já aceitas (para não reaparecerem na mesma estação e período). */
    private val takenOffers = HashSet<Long>()
    /** Custo médio por tonelada do que está no porão (para estimar lucro no mapa). */
    private val avgCost: MutableMap<Commodity, Int> = EnumMap(Commodity::class.java)
    /** Preços de venda que o jogador viu em cada sistema (atracando ou por rumor) e em que dia. */
    private val known = HashMap<com.mercador.core.universe.SystemId, KnownMarket>()
    private val _news = ArrayList<LedgerEntry>()
    /** Jornal da região: crises e revoltas, aconteçam ou não por causa do jogador. */
    val news: List<LedgerEntry> get() = _news
    private val stats: MutableMap<Stat, Int> = EnumMap(Stat::class.java)
    fun stat(s: Stat) = stats[s] ?: 0
    private fun bump(s: Stat, n: Int = 1) { stats[s] = stat(s) + n }
    var campaign: Campaign? = null
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

    init {
        remember(start)
        if (campaign) startCampaign(Rng(derive(start.seedKey(), world.region.universe.seed, 0xDEB7)))
    }

    val day: Int get() = world.day
    /** Quem governa onde o jogador está (pode ter mudado numa revolta). */
    val faction get() = world.controller(location)
    val ideology get() = world.ideology(location)
    fun hasTrait(t: Trait) = t in traitExpiry
    fun favors(f: com.mercador.core.universe.Faction) = favors[f.id]
    fun secretsAbout(f: com.mercador.core.universe.Faction) = secrets.count { it == f.id }
    fun grantFavor(f: com.mercador.core.universe.Faction) { favors[f.id] = (favors[f.id] + 1).coerceAtMost(3) }
    fun grantSecret(f: com.mercador.core.universe.Faction) { secrets += f.id }
    internal fun useFavor(f: com.mercador.core.universe.Faction) { favors[f.id] = (favors[f.id] - 1).coerceAtLeast(0) }
    fun hasFame() = traitExpiry.keys.any { it.feared }
    fun pendingRetaliations() = retaliations.size

    /** Consequências em aberto, à vista do jogador no livro (seção 10.1): quem virá e quando. */
    fun openConsequences(): List<Pair<Person, Int>> = retaliations.map { it.avenger to it.due }

    /** Concede um traço (também usado para restaurar um save). */
    fun grantTrait(t: Trait, days: Int? = null) = addTrait(t, days)

    /** Dias restantes de um traço que se dissipa; null se permanente ou ausente. */
    fun traitDaysLeft(t: Trait): Int? = traitExpiry[t]?.let { (it - day).coerceAtLeast(0) }
    fun isIllegalHere(c: Commodity) = world.banned(location, c)
    val holdUsed: Int get() = cargo.values.sum()
    val holdFree: Int get() = holdCapacity - holdUsed

    /** Toneladas de [c] presas a contratos de carga lacrada (não podem ser vendidas). */
    fun lockedTons(c: Commodity): Int = _contracts.filter { it.kind == ContractKind.LACRADA && it.commodity == c }.sumOf { it.tons }
    /** Carga do jogador sem a lacrada: o que ele pode vender. */
    fun ownTons(c: Commodity): Int = ((cargo[c] ?: 0) - lockedTons(c)).coerceAtLeast(0)
    val sealedTons: Int get() = _contracts.filter { it.kind == ContractKind.LACRADA }.sumOf { it.tons }

    fun fuelCost(to: StarSystem): Int = ceil(world.region.distanceLy(location, to)).toInt().coerceAtLeast(1)
    fun travelDays(to: StarSystem): Int {
        val base = 1 + ceil(world.region.distanceLy(location, to) / 2).toInt()
        return ceil(base * (1 - level(Upgrade.MOTOR) * MOTOR_STEP_PCT / 100.0)).toInt().coerceAtLeast(1)
    }

    /** Chance de a patrulha abordar ao chegar em [s] com a carga de agora (o mapa, o bar e a chegada usam a mesma). */
    fun encounterChance(s: StarSystem): Double {
        val f = world.controller(s)
        val p = PursuitRules.encounterChance(standing.heat(f), s.traits.surveillance, evidenceAt(s).isNotEmpty(), standing.isRestricted(f))
        return if (level(Upgrade.TRANSPONDER) > 0) p * (1 - TRANSPONDER_CUT_PCT / 100.0) else p
    }

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
        val own = ownTons(c)
        avgCost[c] = ((avgCost[c] ?: 0).toLong() * own + cost).div(own + tons).toInt()
        cargo[c] = (cargo[c] ?: 0) + tons
        world.recordTrade(location, c, tons, Side.BUY)
        heatFromIllegalTrade(c, tons)
        return Outcome.Ok(credits)
    }

    fun sell(c: Commodity, tons: Int): Outcome {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE)
        if (tons <= 0) return Outcome.Fail(Failure.QUANTIDADE_INVALIDA)
        val have = cargo[c] ?: 0
        if (tons > ownTons(c)) return Outcome.Fail(Failure.SEM_CARGA)
        val before = world.meters(location)
        credits += world.unitPrice(location, c, tons.toDouble(), Side.SELL) * tons
        if (have == tons) cargo.remove(c) else cargo[c] = have - tons
        world.recordTrade(location, c, tons, Side.SELL)
        recordConsequence(c, tons, before)
        heatFromIllegalTrade(c, tons)
        relieveCrisis(c, tons)
        return Outcome.Ok(credits)
    }

    fun refuel(): Outcome {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE)
        val missing = fuelCapacity - fuel
        if (missing == 0) return Outcome.Fail(Failure.TANQUE_CHEIO)
        val cost = missing * FUEL_PRICE
        if (cost > credits) return Outcome.Fail(Failure.SEM_DINHEIRO)
        credits -= cost
        fuel = fuelCapacity
        return Outcome.Ok(credits)
    }

    /**
     * Salta para [to]: gasta combustível, passa os dias da viagem e chega (patrulha, histórias).
     * [prepaid] é o combustível já queimado num salto que falhou (witchspace) a caminho do mesmo destino.
     */
    fun travel(to: StarSystem, prepaid: Int = 0): Outcome {
        val check = jumpCheck(to, prepaid)
        if (check != null) return Outcome.Fail(check)
        fuel -= (fuelCost(to) - prepaid).coerceAtLeast(0)
        val days = travelDays(to)
        elapse(days)
        location = to
        remember(location)
        val arrived = arrive()
        if (!stories || arrived !is Outcome.Ok) return arrived
        val card = StoryEngine.draw(this) ?: return arrived
        storySeen[card.storylet.id] = day
        pendingStory = card
        return Outcome.Story(card)
    }

    /** Por que o salto para [to] não pode acontecer agora, ou null se pode. */
    fun jumpCheck(to: StarSystem, prepaid: Int = 0): Failure? {
        if (busy) return Failure.PERSEGUICAO_PENDENTE
        if (to === location || to !in world.region.systems) return Failure.FORA_DE_ALCANCE
        val cost = fuelCost(to)
        if (cost > fuelCapacity) return Failure.FORA_DE_ALCANCE
        if (cost - prepaid > fuel) return Failure.SEM_COMBUSTIVEL
        return null
    }

    /**
     * O salto para [to] falhou no meio (witchspace): queima metade do combustível da viagem, que conta como já
     * pago quando o jogador saltar de novo para o mesmo destino ([travel] com `prepaid`). Retorna o combustível queimado.
     */
    fun misjump(to: StarSystem): Int {
        if (jumpCheck(to) != null) return 0
        val burned = (fuelCost(to) + 1) / 2
        fuel -= burned
        note("O salto para ${to.name} falhou no meio do caminho: witchspace, e não estávamos sozinhos.")
        return burned
    }

    /** Recompensa por uma nave abatida em voo (piratas e foragidos valem dinheiro). */
    fun collectBounty(amount: Int) { if (amount > 0) credits += amount }

    /** Um contêiner recolhido no espaço: 1 t de algo, se couber no porão. Retorna o que veio, ou null. */
    fun scoopCanister(rng: Rng): Commodity? {
        if (holdFree <= 0) return null
        val c = Commodity.values()[rng.nextInt(Commodity.values().size)]
        val own = ownTons(c)
        avgCost[c] = ((avgCost[c] ?: 0).toLong() * own).div(own + 1).toInt()
        cargo[c] = (cargo[c] ?: 0) + 1
        return c
    }

    /** O jogador atirou em quem não devia (polícia, inocentes, a estação): a lei de quem manda aqui anota. */
    fun aggression(heat: Int) { standing.addHeat(faction, heat) }

    /** Status legal do voo (FIST do Elite, 0..255): ≥ 40 os caçadores de recompensa atacam. Vem da atenção da facção local. */
    fun legalStatus(): Int = (standing.heat(faction) * 3 / 5 + if (standing.isRestricted(faction)) 20 else 0).coerceIn(0, 255)

    /** Resumo de um voo, anotado no livro ao atracar. */
    fun logFlight(kills: Int, bounty: Int) {
        if (kills > 0) note("Em voo: $kills nave${if (kills > 1) "s" else ""} abatida${if (kills > 1) "s" else ""}" + if (bounty > 0) ", $bounty cr de recompensa." else ".")
    }

    /**
     * A nave foi destruída em voo. A cápsula de fuga leva o jogador de volta à estação de onde ele está; a
     * seguradora entrega outra nave igual (com as melhorias), cobra a franquia e a carga se perde
     * (a carga lacrada quebra o contrato). Retorna o texto do desfecho.
     */
    fun shipDestroyed(): String {
        val lost = cargo.values.sum()
        cargo.clear(); avgCost.clear()
        checkSealed()
        val fee = minOf(credits, maxOf(INSURANCE_MIN, credits / 10))
        credits -= fee
        note("Nave destruída em combate. A cápsula de fuga voltou a ${location.name}; a seguradora cobrou $fee cr" + if (lost > 0) " e $lost t de carga se perderam." else ".")
        return "A cápsula de fuga dispara um segundo antes da explosão e te deixa, tonto, no hangar de ${location.name}. " +
            "A seguradora entrega outra nave igual e cobra a franquia: ${fee} cr." + if (lost > 0) " As $lost t de carga viraram destroços." else ""
    }

    /** Escolhe uma opção da storylet pendente; o efeito e o texto do desfecho vêm da própria storylet. */
    fun chooseStory(index: Int): Outcome {
        val card = pendingStory ?: return Outcome.Fail(Failure.SEM_HISTORIA)
        if (index !in card.choices.indices || !card.choices[index].available) return Outcome.Fail(Failure.ESCOLHA_INDISPONIVEL)
        pendingStory = null
        val text = StoryEngine.resolve(this, card, index)
        checkSealed()
        return Outcome.StoryResolved(text, confrontation)
    }

    /** Provas que a lei do sistema atual enxerga no porão (o compartimento oculto esconde parte). */
    fun evidence(): Map<Commodity, Int> = evidenceAt(location)

    /** Provas que a lei de [s] enxergaria: o compartimento esconde primeiro o contrabando mais caro. */
    fun evidenceAt(s: StarSystem): Map<Commodity, Int> {
        var hide = hiddenCapacity
        val out = EnumMap<Commodity, Int>(Commodity::class.java)
        for ((c, t) in cargo.entries.filter { world.banned(s, it.key) }.sortedByDescending { it.key.basePrice }) {
            val h = minOf(hide, t); hide -= h
            if (t - h > 0) out[c] = t - h
        }
        return out
    }

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
        val p = encounterChance(location)
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
                    val refuge = world.region.systems.filter { world.controller(it).id != f.id }
                        .minByOrNull { world.region.distanceLy(location, it) } ?: location
                    elapse(1)
                    location = refuge
                    remember(location)
                    note("Fugiu da patrulha de ${f.name} e se refugiou em ${refuge.name}.")
                    Resolution(r, true, 0, 0, "Escapou para ${refuge.name}, em território de ${world.controller(refuge).name}.")
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
        checkSealed()
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
        elapse(FREIGHT_DAYS)
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

    internal fun adjustFuel(d: Int) { fuel = (fuel + d).coerceIn(0, fuelCapacity) }

    internal fun addVoice(text: String) { _voice += LedgerEntry(day, location.name, text) }

    /** Passa tempo fora de uma viagem: esfria a atenção e dissipa traços. */
    internal fun passDays(n: Int) = elapse(n)

    /**
     * Todo passar do tempo vem por aqui: o mundo anda, a atenção esfria, traços se dissipam, as notícias do
     * mundo vivo entram no jornal, contratos vencem e os marcos da campanha são cobrados.
     */
    private fun elapse(n: Int) {
        world.advance(n)
        standing.cool(n)
        expireTraits()
        for (w in world.drainNews()) onNews(w)
        expireContracts()
        checkCampaign()
    }

    private fun onNews(w: WorldNews) {
        _news += LedgerEntry(w.day, w.systemName, w.text)
        if (w.kind == NewsKind.REVOLTA && _ledger.any { it.systemName == w.systemName && it.text.contains("última gota") && it.day >= w.day - 30 }) {
            bump(Stat.REVOLTAS_PROVOCADAS)
            note("A revolta em ${w.systemName} começou com um carregamento seu.")
        }
    }

    /** Coloca o jogador em [s] sem viajar (testes e ferramentas de depuração; o jogo nunca chama). */
    internal fun teleport(s: StarSystem) { location = s; remember(s) }

    private fun remember(s: StarSystem) {
        known[s.id] = KnownMarket(day, Commodity.values().map { world.unitPrice(s, it, 1.0, Side.SELL) })
    }

    /** Preços de venda vistos em [s] (atual, se o jogador está lá); null se nunca viu. */
    fun knownMarket(s: StarSystem): KnownMarket? =
        if (s === location) KnownMarket(day, Commodity.values().map { world.unitPrice(s, it, 1.0, Side.SELL) }) else known[s.id]

    /**
     * Quanto renderia vender a carga própria em [s], pelos preços que o jogador conhece de lá, e o lucro
     * sobre o custo médio. Mercadoria proibida lá fica de fora (e é contada à parte). Null se nunca viu os preços.
     */
    fun estimateSale(s: StarSystem): SaleEstimate? {
        val k = knownMarket(s) ?: return null
        var revenue = 0; var cost = 0; var banned = 0
        for (c in cargo.keys) {
            val t = ownTons(c)
            if (t <= 0) continue
            if (world.banned(s, c)) { banned += t; continue }
            revenue += k.sell[c.ordinal] * t
            cost += (avgCost[c] ?: 0) * t
        }
        val pay = _contracts.filter { it.destination == s.id }.sumOf { it.reward }
        return SaleEstimate(revenue, revenue - cost, banned, pay, k.day)
    }

    /** Melhor compra daqui para vender em [s] pelos preços conhecidos de lá: mercadoria e ganho por tonelada. */
    fun bestRun(s: StarSystem): Pair<Commodity, Int>? {
        val k = knownMarket(s) ?: return null
        if (s === location) return null
        return Commodity.values().filter { !isIllegalHere(it) && !world.banned(s, it) && world.quote(location, it).stock > 0 }
            .map { it to k.sell[it.ordinal] - world.unitPrice(location, it, 1.0, Side.BUY) }
            .filter { it.second > 0 }.maxByOrNull { it.second }
    }

    // ---- contratos ----

    internal fun contractTaken(id: Long) = id in takenOffers

    fun offers(): List<Contract> = Contracts.offers(this)

    fun acceptContract(c: Contract): Outcome {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE)
        if (_contracts.size >= Contracts.MAX_ACTIVE) return Outcome.Fail(Failure.LIMITE_DE_CONTRATOS)
        if (c.id in takenOffers || offers().none { it.id == c.id }) return Outcome.Fail(Failure.SEM_CONTRATO)
        if (c.kind == ContractKind.LACRADA) {
            if (c.tons > holdFree) return Outcome.Fail(Failure.SEM_PORAO)
            cargo[c.commodity] = (cargo[c.commodity] ?: 0) + c.tons
        }
        takenOffers += c.id
        _contracts += c
        note("Aceitou ${c.kind.label.lowercase()}: ${c.tons} t de ${c.cargoLabel} para ${c.destName} até o dia ${c.deadline}, por ${c.reward} cr.")
        return Outcome.Ok(credits)
    }

    /** O contrato pode ser entregue aqui e agora? */
    fun canDeliver(c: Contract): Boolean = c.destination == location.id &&
        (if (c.kind == ContractKind.LACRADA) (cargo[c.commodity] ?: 0) >= c.tons else ownTons(c.commodity) >= c.tons)

    fun deliverContract(id: Long): Pair<Outcome, String?> {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE) to null
        val c = _contracts.firstOrNull { it.id == id } ?: return Outcome.Fail(Failure.SEM_CONTRATO) to null
        if (c.destination != location.id) return Outcome.Fail(Failure.CONTRATO_EM_OUTRO_LUGAR) to null
        if (!canDeliver(c)) return Outcome.Fail(Failure.SEM_CARGA) to null
        _contracts.remove(c)
        val left = (cargo[c.commodity] ?: 0) - c.tons
        if (left <= 0) cargo.remove(c.commodity) else cargo[c.commodity] = left
        credits += c.reward
        val client = factions.factions[c.client]
        standing.addReputation(client, 4)
        bump(Stat.CONTRATOS_CUMPRIDOS)
        val text = when (c.kind) {
            ContractKind.ENTREGA -> {
                world.recordTrade(location, c.commodity, c.tons, Side.SELL)
                "Entregue. ${c.clientName} manda os ${c.reward} cr sem atraso."
            }
            ContractKind.SOCORRO -> {
                val before = world.meters(location)
                world.recordTrade(location, c.commodity, c.tons, Side.SELL)
                recordConsequence(c.commodity, c.tons, before)
                standing.addReputation(faction, 3)
                relieveCrisis(c.commodity, c.tons)
                "As caixas descem e somem na fila antes de você assinar o recibo. ${c.reward} cr, e gente que vai lembrar."
            }
            ContractKind.LACRADA -> {
                bump(Stat.LACRADAS_ENTREGUES)
                revealSealed(c)
            }
        }
        note("Cumpriu o contrato com ${c.clientName}: ${c.tons} t de ${c.cargoLabel} em ${location.name}, ${c.reward} cr.")
        checkCampaign()
        return Outcome.Ok(credits) to text
    }

    /** No fim da carga lacrada, a verdade: o que era e o que fez no destino. */
    private fun revealSealed(c: Contract): String {
        val (line, pushC, pushD) = when (c.commodity) {
            Commodity.ARMAS -> Triple("Eram armas. Em ${location.name}, alguém vai usá-las para manter outro alguém quieto.", 8, 2)
            Commodity.DROGAS -> Triple("Era droga, pura. ${location.name} vai dormir melhor esta semana, e acordar pior.", -3, 4)
            Commodity.LIVROS -> Triple("Eram livros e panfletos. Em ${location.name}, o poder vai descobrir de onde vieram.", -5, 5)
            else -> Triple("Eram relíquias saqueadas de algum templo que já não existe.", 0, 1)
        }
        world.pushMeters(location, pushC, pushD, 0)
        note(line)
        return "${c.clientName} paga ${c.reward} cr e abre uma caixa na sua frente. $line"
    }

    fun abandonContract(id: Long): Outcome {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE)
        val c = _contracts.firstOrNull { it.id == id } ?: return Outcome.Fail(Failure.SEM_CONTRATO)
        failContract(c, "Desistiu do contrato com ${c.clientName}.")
        return Outcome.Ok(credits)
    }

    private fun failContract(c: Contract, why: String) {
        _contracts.remove(c)
        if (c.kind == ContractKind.LACRADA) {
            // o cliente manda buscar o que sobrou da carga dele
            val left = (cargo[c.commodity] ?: 0) - c.tons
            if (left <= 0) cargo.remove(c.commodity) else cargo[c.commodity] = left
        }
        standing.addReputation(factions.factions[c.client], if (c.kind == ContractKind.LACRADA) -10 else -5)
        bump(Stat.CONTRATOS_FALHOS)
        note(why)
    }

    private fun expireContracts() {
        for (c in _contracts.filter { it.deadline < day }) failContract(c, "O prazo do contrato com ${c.clientName} venceu: ${c.tons} t de ${c.cargoLabel} que não chegaram a ${c.destName}.")
    }

    /** Carga lacrada confiscada, jogada fora ou perdida numa história quebra o contrato. */
    private fun checkSealed() {
        val byGood = _contracts.filter { it.kind == ContractKind.LACRADA }.groupBy { it.commodity }
        for ((good, list) in byGood) {
            var have = cargo[good] ?: 0
            for (c in list) {
                if (have >= c.tons) { have -= c.tons; continue }
                _contracts.remove(c)
                standing.addReputation(factions.factions[c.client], -10)
                bump(Stat.CONTRATOS_FALHOS)
                note("A carga lacrada de ${c.clientName} se perdeu. Quem a mandou não vai esquecer.")
            }
        }
    }

    private fun relieveCrisis(c: Commodity, tons: Int) {
        val cr = world.relieve(location, c, tons) ?: return
        bump(Stat.CRISES_SOCORRIDAS)
        standing.addReputation(faction, 6)
        grantFavor(faction)
        note("${cr.kind.label} em ${location.name} acabou com a sua entrega. ${faction.name} fica te devendo.")
        for (w in world.drainNews()) onNews(w)
    }

    // ---- estaleiro ----

    /** Melhorias à venda aqui (e o preço do próximo nível de cada). */
    fun shipyard(): List<Pair<Upgrade, Int?>> = Upgrade.values().filter { it.soldAt(location) }.map { it to it.priceFor(level(it)) }

    fun buyUpgrade(u: Upgrade): Outcome {
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE)
        if (!u.soldAt(location)) return Outcome.Fail(Failure.SEM_ESTALEIRO)
        val price = u.priceFor(level(u)) ?: return Outcome.Fail(Failure.NIVEL_MAXIMO)
        if (price > credits) return Outcome.Fail(Failure.SEM_DINHEIRO)
        credits -= price
        upgrades[u] = level(u) + 1
        bump(Stat.MELHORIAS)
        note("Instalou ${u.label.lowercase()} (nível ${level(u)}) por $price cr.")
        return Outcome.Ok(credits)
    }

    // ---- campanha ----

    private fun startCampaign(rng: Rng) {
        val fs = factions.factions
        val f = fs.firstOrNull { it.doctrine == Doctrine.COBRADORA } ?: rng.pick(fs)
        val creditor = People.generate(rng, f, "agiota")
        campaign = Campaign(creditor, Campaign.DEBT)
        note("Você deve ${Campaign.DEBT} cr a ${creditor.name}, agiota de ${f.name}. Prazo: dia ${Campaign.DEADLINE}. Ele cobra em parcelas.")
    }

    /** Paga ao agiota tudo o que der (ou [amount], se menor). */
    fun payCreditor(amount: Int = Int.MAX_VALUE): Outcome {
        val c = campaign ?: return Outcome.Fail(Failure.SEM_CAMPANHA)
        if (c.over) return Outcome.Fail(Failure.SEM_CAMPANHA)
        if (busy) return Outcome.Fail(Failure.PERSEGUICAO_PENDENTE)
        val pay = minOf(amount, credits, c.remaining)
        if (pay <= 0) return Outcome.Fail(Failure.SEM_DINHEIRO)
        credits -= pay; c.paid += pay
        note("Pagou $pay cr a ${c.creditor.name}. Faltam ${c.remaining}.")
        if (c.remaining == 0) {
            c.outcome = CampaignOutcome.QUITADA
            note("Quitou a dívida com ${c.creditor.name}.")
        }
        return Outcome.Ok(credits)
    }

    /** Confere os marcos vencidos: parcela perdida engorda a dívida e manda um cobrador. */
    private fun checkCampaign() {
        val c = campaign ?: return
        while (!c.over && c.checked < Campaign.MILESTONE_AT.size && day >= c.milestone(c.checked).first) {
            val (_, share) = c.milestone(c.checked)
            val due = Math.ceil(c.total * share).toInt()
            val last = c.checked == Campaign.MILESTONE_AT.size - 1
            c.checked++
            if (c.paid >= due) { if (!last) note("${c.creditor.name} recebeu a parcela. Por enquanto, silêncio."); continue }
            c.missed++
            if (last) {
                c.outcome = CampaignOutcome.CALOTE
                standing.addReputation(c.creditor.faction, -30)
                note("O prazo de ${c.creditor.name} venceu e a dívida não foi paga.")
                continue
            }
            val fine = (c.remaining * Campaign.PENALTY).toInt()
            c.total += fine
            val agent = People.generate(Rng(derive(location.seedKey(), day.toLong(), 0xC0B)), c.creditor.faction, "cobrador de ${c.creditor.name}")
            retaliations += Retaliation(day, agent)
            note("Perdeu a parcela de ${c.creditor.name}: a dívida cresceu $fine cr e ${agent.name} vem cobrar pessoalmente.")
        }
    }

    /** Melhoria dada por uma história (sem passar pelo estaleiro). */
    internal fun grantUpgrade(u: Upgrade) { if (level(u) < u.maxLevel) upgrades[u] = level(u) + 1 }

    /** Muda o total da dívida da campanha (histórias de agiota); nunca abaixo do que já foi pago. */
    internal fun adjustDebt(n: Int) { campaign?.takeIf { !it.over }?.let { it.total = maxOf(it.paid, it.total + n) } }

    /** O epílogo da campanha, enquanto o jogador ainda não o leu. */
    fun epilogue(): Epilogue? = campaign?.takeIf { it.over && !it.epilogueSeen }?.let { Epilogues.compose(this) }

    fun dismissEpilogue() { campaign?.epilogueSeen = true }

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
                remember(d)
                "No bar dizem que o mercado de ${c.label.lowercase()} paga bem em ${d.name}. Os preços de lá vão para o seu mapa."
            } ?: "Ninguém sabe de nada que valha o salto agora."
            RumorKind.RISCO_DA_ROTA -> {
                val d = reachable().maxByOrNull { standing.heat(world.controller(it)) + it.traits.surveillance }
                if (d == null) "Sem combustível, a rota não importa." else {
                    "Em ${d.name}, a patrulha de ${world.controller(d).name} te abordaria com ${(encounterChance(d) * 100).toInt()}% de chance."
                }
            }
            RumorKind.CRISE -> Bar.crisisRumor(this)
            RumorKind.SEGREDO -> { grantSecret(offer.about!!); "Você sai do bar sabendo algo sobre ${offer.about.name} que não devia." }
        }
        note("Pagou ${offer.price} cr por informação no bar.")
        return Outcome.Ok(credits) to text
    }

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
        val ending = s.ending ?: return Outcome.Confronting(s).also { checkSealed() }
        return finish(s, ending).also { checkSealed() }
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
            bump(Stat.ENTREGAS_RACIONAMENTO)
            note("Entregou $tons t de ${c.label} durante o racionamento.")
            standing.addReputation(faction, Math.max(1, tons / 5))
            if (tons >= 10) grantFavor(faction)
        }
        if (c == Commodity.ARMAS && tons >= 5 && before.discontent >= 60) {
            bump(Stat.ARMAS_A_TIRANOS)
            note("Vendeu $tons t de armas a um poder que o povo já não suporta.")
        }
        if (!before.nearRevolt && world.meters(location).nearRevolt) {
            note("Seu carregamento de ${c.label} foi a última gota: o sistema está à beira da revolta.")
        }
    }

    internal fun note(text: String) { _ledger += LedgerEntry(day, location.name, text) }

    /** Pode salvar agora? Com uma decisão pendente, o último momento calmo é que vale. */
    val canSave: Boolean get() = !busy

    /** Foto do mundo vivido (ver [GameSave]). Decisões pendentes ficam de fora. */
    fun toSave(): GameSave {
        val (deltas, vols) = world.exportDeltas()
        val living = world.exportLiving()
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
            locationName = location.name,
            living = world.living,
            controllers = living.controllers.entries.sortedWith(compareBy({ it.key.sx }, { it.key.sy }, { it.key.index })).map { SysCountSave(ref(it.key), it.value) },
            unrestDays = living.unrestDays.entries.sortedWith(compareBy({ it.key.sx }, { it.key.sy }, { it.key.index })).map { SysCountSave(ref(it.key), it.value) },
            lastRevolt = living.lastRevolt.entries.sortedWith(compareBy({ it.key.sx }, { it.key.sy }, { it.key.index })).map { SysCountSave(ref(it.key), it.value) },
            crises = living.crises.map { CrisisSave(it.kind.name, ref(it.system), it.systemName, it.start, it.end, it.need) },
            upgrades = upgrades.entries.sortedBy { it.key.ordinal }.associate { it.key.name to it.value },
            contracts = _contracts.map { ContractSave(it.id, it.kind.name, it.commodity.name, it.tons, ref(it.destination), it.destName, it.deadline, it.reward, it.client, it.clientName) },
            takenOffers = takenOffers.sorted(),
            avgCost = avgCost.entries.sortedBy { it.key.ordinal }.associate { it.key.name to it.value },
            known = known.entries.sortedWith(compareBy({ it.key.sx }, { it.key.sy }, { it.key.index })).map { KnownSave(ref(it.key), it.value.day, it.value.sell) },
            news = _news.map { LedgerSave(it.day, it.systemName, it.text) },
            stats = stats.entries.sortedBy { it.key.ordinal }.associate { it.key.name to it.value },
            campaign = campaign?.let { c ->
                CampaignSave(PersonSave(c.creditor.name, c.creditor.role, c.creditor.faction.id), c.total, c.paid, c.deadline, c.checked, c.missed, c.outcome?.name, c.epilogueSeen)
            },
        )
    }

    companion object {
        /** Reconstrói o jogo: o mundo gerado vem da semente, o vivido vem do [save]. */
        fun fromSave(save: GameSave): Game {
            val universe = com.mercador.core.universe.Universe(save.universeSeed)
            fun sys(r: SysRef) = universe.system(com.mercador.core.universe.SystemId(save.galaxy, r.sx, r.sy, r.index))
                ?: throw SaveException("Sistema ${r.sx},${r.sy}#${r.index} não existe neste universo.")
            val systems = save.region.map(::sys)
            val world = World(com.mercador.core.universe.Region(universe, save.galaxy, systems), save.living)
            fun id(r: SysRef) = com.mercador.core.universe.SystemId(save.galaxy, r.sx, r.sy, r.index)
            fun <T : Enum<T>> named(cls: Class<T>, name: String): T =
                cls.enumConstants.firstOrNull { it.name == name } ?: throw SaveException("Valor desconhecido no save: $name")
            val nFactions = world.factions.factions.size
            if (save.controllers.any { it.value !in 0 until nFactions }) throw SaveException("Facção desconhecida no save.")
            world.restore(
                save.day,
                save.meterDeltas.associate { id(it.system) to doubleArrayOf(it.control, it.discontent, it.supply) },
                save.volumes.associate { (id(it.system) to named(Commodity::class.java, it.commodity)) to it.tons },
                com.mercador.core.world.LivingState(
                    save.controllers.associate { id(it.system) to it.value },
                    save.unrestDays.associate { id(it.system) to it.value },
                    save.lastRevolt.associate { id(it.system) to it.value },
                    save.crises.map { com.mercador.core.world.Crisis(named(com.mercador.core.world.CrisisKind::class.java, it.kind), id(it.system), it.systemName, it.start, it.end, it.need) },
                ),
            )
            // o local atual tem de ser a MESMA instância da lista da região: o jogo compara sistemas por identidade
            val here = world.region.systems.firstOrNull { it.id == id(save.location) }
                ?: throw SaveException("A localização do save não pertence à região.")
            val g = Game(world, here, save.credits, save.stories)
            g.known.clear()
            save.upgrades.forEach { (k, v) -> val u = named(Upgrade::class.java, k); g.upgrades[u] = v.coerceIn(0, u.maxLevel) }
            g.fuel = save.fuel.coerceIn(0, g.fuelCapacity)
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
            for (c in save.contracts) {
                if (c.client !in 0 until nFactions) throw SaveException("Facção desconhecida no save.")
                g._contracts += Contract(c.id, named(ContractKind::class.java, c.kind), named(Commodity::class.java, c.commodity), c.tons, id(c.destination), c.destName, c.deadline, c.reward, c.client, c.clientName)
            }
            g.takenOffers += save.takenOffers
            save.avgCost.forEach { (k, v) -> g.avgCost[named(Commodity::class.java, k)] = v }
            for (k in save.known) {
                if (k.sell.size != Commodity.values().size) throw SaveException("Preços conhecidos inválidos no save.")
                g.known[id(k.system)] = KnownMarket(k.day, k.sell)
            }
            if (g.known.isEmpty()) g.remember(here)
            save.news.forEach { g._news += LedgerEntry(it.day, it.system, it.text) }
            save.stats.forEach { (k, v) -> g.stats[named(Stat::class.java, k)] = v }
            save.campaign?.let { c ->
                val f = g.factions.factions.firstOrNull { it.id == c.creditor.faction } ?: throw SaveException("Facção desconhecida no save.")
                g.campaign = Campaign(Person(c.creditor.name, c.creditor.role, f), c.total, c.paid, c.deadline, c.checked, c.missed,
                    c.outcome?.let { named(CampaignOutcome::class.java, it) }, c.epilogueSeen)
            }
            return g
        }

        const val START_CREDITS = 1000
        const val INSURANCE_MIN = 150
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
