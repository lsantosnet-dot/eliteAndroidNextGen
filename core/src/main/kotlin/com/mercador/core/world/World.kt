package com.mercador.core.world

import com.mercador.core.economy.Commodity
import com.mercador.core.economy.Market
import com.mercador.core.economy.MarketEntry
import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive
import com.mercador.core.universe.Faction
import com.mercador.core.universe.Ideology
import com.mercador.core.universe.Region
import com.mercador.core.universe.StarSystem
import com.mercador.core.universe.SystemId
import kotlin.math.abs
import kotlin.math.roundToInt

enum class Side { BUY, SELL }

/**
 * O mundo vivido: só as diferenças sobre o mundo-fórmula (design, seção 3.2).
 * Medidores = baseline gerado + delta; preços = [Market] + efeito do volume que o jogador movimentou.
 * Os deltas decaem com o tempo, então o mundo se recompõe sozinho.
 *
 * Com [living], o mundo também se mexe sozinho: crises com prazo surgem e acabam, e um sistema que passa dias
 * sob pressão alta (ver [Meters.pressure]) se revolta e troca de dono (com isso mudam a cor no mapa e as leis). Os testes de unidade
 * desligam isso para isolar o que medem.
 */
class World(val region: Region, val living: Boolean = false) {
    var day: Int = 0
        private set

    private val meterDeltas = HashMap<SystemId, DoubleArray>()
    private val volumes = HashMap<Pair<SystemId, Commodity>, Double>()
    val factions = region.universe.factions(region.galaxy)
    /** Sistemas que mudaram de dono numa revolta: id do sistema → id da facção. */
    private val controllers = HashMap<SystemId, Int>()
    private val unrestDays = HashMap<SystemId, Int>()
    private val lastRevolt = HashMap<SystemId, Int>()
    private val _crises = ArrayList<Crisis>()
    val crises: List<Crisis> get() = _crises
    private val pendingNews = ArrayList<WorldNews>()

    /** Quem governa o sistema hoje: o gerado, ou quem tomou o poder numa revolta. */
    fun controller(s: StarSystem): Faction = controllers[s.id]?.let { id -> factions.factions[id] } ?: s.faction

    /** A ideologia (e portanto as leis) é de quem governa agora. */
    fun ideology(s: StarSystem): Ideology = controller(s).ideology
    fun banned(s: StarSystem, c: Commodity): Boolean = c.bannedBy(ideology(s))

    fun crisisAt(s: StarSystem): Crisis? = _crises.firstOrNull { it.system == s.id }

    /** Notícias acumuladas desde a última chamada (o jogo as passa para o jornal dele). */
    fun drainNews(): List<WorldNews> = pendingNews.toList().also { pendingNews.clear() }

    fun meters(s: StarSystem): Meters {
        val base = MeterModel.baseline(s.traits, s.seedKey())
        val d = meterDeltas[s.id]
        val control = clamp(base[0] + (d?.get(0) ?: 0.0))
        val supply = clamp(base[2] + (d?.get(2) ?: 0.0))
        val discontent = clamp(base[1] + (d?.get(1) ?: 0.0) + MeterModel.scarcityPressure(supply))
        return Meters(control.roundToInt(), discontent.roundToInt(), supply.roundToInt())
    }

    /** Toneladas líquidas que o jogador tirou deste mercado (negativo = entregou mais do que levou). */
    fun volume(s: StarSystem, c: Commodity): Double = volumes[s.id to c] ?: 0.0

    /** Mercado exibido ao jogador: preços e estoques já com abastecimento e volume. */
    fun quote(s: StarSystem, c: Commodity): MarketEntry {
        val e = Market.entry(s, c, day, ideology(s))
        val vol = volume(s, c)
        val cr = crisisAt(s)
        val stock = when {
            cr == null -> e.stock.toDouble()
            c in cr.kind.wants -> e.stock * 0.3
            c in cr.kind.surplus -> e.stock + 20.0
            else -> e.stock.toDouble()
        }
        return e.copy(price = unitPrice(s, c, 0.0, Side.BUY).coerceAtLeast(1), stock = (stock - vol).toInt().coerceAtLeast(0))
    }

    fun quotes(s: StarSystem): List<MarketEntry> = Commodity.values().map { quote(s, it) }

    /**
     * Preço unitário de uma transação de [tons]: usa o preço no meio do deslocamento de volume,
     * então comprar e revender no mesmo lugar nunca dá lucro (venda ainda paga [SELL_SPREAD] a menos).
     */
    fun unitPrice(s: StarSystem, c: Commodity, tons: Double, side: Side): Int {
        val base = Market.entry(s, c, day, ideology(s)).price.toDouble() * (crisisAt(s)?.priceMult(c) ?: 1.0)
        val supply = meters(s).supply
        val scarcity = if (c in MeterModel.essential) 1.0 + (50 - supply).coerceAtLeast(0) / 100.0 else 1.0
        val shift = if (side == Side.BUY) tons / 2 else -tons / 2
        val volMult = (1.0 + (volume(s, c) + shift) * VOLUME_IMPACT).coerceIn(0.6, 1.5)
        val spread = if (side == Side.SELL) SELL_SPREAD else 1.0
        return (base * scarcity * volMult * spread).roundToInt().coerceAtLeast(1)
    }

    /** Registra a transação: move o volume e empurra os medidores. */
    fun recordTrade(s: StarSystem, c: Commodity, tons: Int, side: Side) {
        val key = s.id to c
        volumes[key] = volume(s, c) + if (side == Side.BUY) tons else -tons
        val d = meterDeltas.getOrPut(s.id) { DoubleArray(3) }
        val p = MeterModel.push(c)
        val k = tons / 10.0
        if (side == Side.SELL) {
            d[0] += p.control * k; d[1] += p.discontent * k; d[2] += p.supply * k
        } else {
            d[2] -= p.supply * k * MeterModel.REMOVAL_FACTOR
        }
        d[0] = d[0].coerceIn(-MAX_DELTA, MAX_DELTA); d[1] = d[1].coerceIn(-MAX_DELTA, MAX_DELTA); d[2] = d[2].coerceIn(-MAX_DELTA, MAX_DELTA)
    }

    /** Empurra os medidores diretamente (eventos de história, não comércio). */
    fun pushMeters(s: StarSystem, control: Int, discontent: Int, supply: Int) {
        val d = meterDeltas.getOrPut(s.id) { DoubleArray(3) }
        d[0] = (d[0] + control).coerceIn(-MAX_DELTA, MAX_DELTA)
        d[1] = (d[1] + discontent).coerceIn(-MAX_DELTA, MAX_DELTA)
        d[2] = (d[2] + supply).coerceIn(-MAX_DELTA, MAX_DELTA)
    }

    /**
     * Entrega de [tons] de [c] num sistema em crise de socorro: abate o que falta e, se zerar, a crise acaba antes
     * do prazo. Devolve a crise encerrada (para o jogo dar o crédito ao jogador), ou null.
     */
    fun relieve(s: StarSystem, c: Commodity, tons: Int): Crisis? {
        val cr = crisisAt(s) ?: return null
        if (!cr.kind.relieves || c !in cr.kind.wants || cr.need <= 0) return null
        cr.need -= tons
        if (cr.need > 0) return null
        _crises.remove(cr)
        pushMeters(s, 0, -15, 20)
        pendingNews += WorldNews(day, s.id, s.name, "${cr.kind.label} em ${s.name} acabou antes do prazo: chegou o que faltava.", NewsKind.CRISE_SOCORRIDA)
        return cr
    }

    /** Diferenças do mundo vivido, para o salvamento. */
    internal fun exportDeltas(): Pair<Map<SystemId, DoubleArray>, Map<Pair<SystemId, Commodity>, Double>> =
        meterDeltas.mapValues { it.value.copyOf() } to HashMap(volumes)

    internal fun exportLiving() = LivingState(HashMap(controllers), HashMap(unrestDays), HashMap(lastRevolt), _crises.toList())

    internal fun restore(day: Int, deltas: Map<SystemId, DoubleArray>, vols: Map<Pair<SystemId, Commodity>, Double>, living: LivingState? = null) {
        this.day = day
        meterDeltas.clear(); deltas.forEach { (k, v) -> meterDeltas[k] = v.copyOf() }
        volumes.clear(); volumes.putAll(vols)
        controllers.clear(); unrestDays.clear(); lastRevolt.clear(); _crises.clear()
        if (living != null) {
            controllers.putAll(living.controllers); unrestDays.putAll(living.unrestDays); lastRevolt.putAll(living.lastRevolt)
            _crises += living.crises
        }
    }

    fun advance(days: Int) {
        repeat(days) {
            day++
            meterDeltas.values.forEach { d -> for (i in d.indices) d[i] *= METER_DECAY }
            meterDeltas.values.removeAll { d -> d.all { abs(it) < 0.05 } }
            val it = volumes.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                e.setValue(e.value * VOLUME_DECAY)
                if (abs(e.value) < 0.05) it.remove()
            }
            if (living) liveOneDay()
        }
    }

    private fun liveOneDay() {
        // crises que venceram
        val gone = _crises.filter { it.end <= day }
        for (cr in gone) {
            _crises.remove(cr)
            pendingNews += WorldNews(day, cr.system, cr.systemName, "${cr.kind.label} em ${cr.systemName} terminou.", NewsKind.CRISE_ACABA)
        }
        // crise nova
        if (_crises.size < MAX_CRISES) spawnRoll(day)?.let { (s, kind, rng) -> startCrisis(s, kind, rng) }
        // revoltas: dias seguidos de pressão alta derrubam quem governa
        for (s in region.systems) {
            if (meters(s).revolting) unrestDays[s.id] = (unrestDays[s.id] ?: 0) + 1 else unrestDays.remove(s.id)
            val since = lastRevolt[s.id]?.let { day - it } ?: Int.MAX_VALUE
            if ((unrestDays[s.id] ?: 0) >= REVOLT_AFTER_DAYS && since >= REVOLT_COOLDOWN) revolt(s)
        }
    }

    /** Sorteio da crise do dia (sem olhar o limite de crises ativas): base do rumor sobre o que vem por aí. */
    private fun spawnRoll(d: Int): Triple<StarSystem, CrisisKind, Rng>? {
        val rng = Rng(derive(region.universe.seed, region.galaxy, d.toLong(), 0xC215E))
        if (!rng.chance(CRISIS_CHANCE)) return null
        val s = rng.pick(region.systems)
        if (crisisAt(s) != null) return null
        val kind = CrisisKind.values()[rng.nextInt(CrisisKind.values().size)]
        return Triple(s, kind, rng)
    }

    /** Crises que o sorteio já marcou para os próximos [days] dias (o que se ouve no bar). */
    fun upcomingCrises(days: Int): List<Pair<Int, Pair<StarSystem, CrisisKind>>> =
        (day + 1..day + days).mapNotNull { d -> spawnRoll(d)?.let { (s, k, _) -> d to (s to k) } }

    private fun startCrisis(s: StarSystem, kind: CrisisKind, rng: Rng) {
        val need = if (kind.relieves) rng.range(kind.relief.first, kind.relief.last) else 0
        val cr = Crisis(kind, s.id, s.name, day, day + rng.range(CRISIS_MIN_DAYS, CRISIS_MAX_DAYS), need)
        _crises += cr
        pushMeters(s, kind.control, kind.discontent, kind.supply)
        val what = kind.wants.joinToString(", ") { it.label.lowercase() }
        pendingNews += WorldNews(day, s.id, s.name, "${kind.label} em ${s.name}: falta $what.", NewsKind.CRISE_COMECA)
    }

    /** O poder cai: assume a facção que mais odeia a antiga (quem financiou a revolta), e a pressão se alivia. */
    private fun revolt(s: StarSystem) {
        val old = controller(s)
        val next = factions.factions.filter { it.id != old.id }.minBy { factions.relation(old.id, it.id) }
        if (next.id == s.faction.id) controllers.remove(s.id) else controllers[s.id] = next.id
        lastRevolt[s.id] = day
        unrestDays.remove(s.id)
        pushMeters(s, 10, -45, 0) // o governo novo aperta o controle e a pressão cai
        pendingNews += WorldNews(day, s.id, s.name, "Revolta em ${s.name}: ${old.name} caiu e ${next.name} assumiu. As leis mudaram.", NewsKind.REVOLTA)
    }

    private fun clamp(v: Double) = v.coerceIn(0.0, 100.0)

    companion object {
        const val VOLUME_IMPACT = 0.01     // +1% de preço por tonelada tirada
        const val SELL_SPREAD = 0.95
        const val METER_DECAY = 0.97       // meia-vida de ~23 dias
        const val VOLUME_DECAY = 0.94      // meia-vida de ~11 dias
        const val MAX_DELTA = 60.0
        const val MAX_CRISES = 5
        const val CRISIS_CHANCE = 0.16
        const val CRISIS_MIN_DAYS = 14
        const val CRISIS_MAX_DAYS = 28
        const val REVOLT_AFTER_DAYS = 4
        const val REVOLT_COOLDOWN = 90
    }
}

/** O que o mundo vivo guarda além dos medidores e volumes (para o salvamento). */
internal class LivingState(
    val controllers: Map<SystemId, Int>,
    val unrestDays: Map<SystemId, Int>,
    val lastRevolt: Map<SystemId, Int>,
    val crises: List<Crisis>,
) {
}
