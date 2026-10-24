package com.mercador.core

import com.mercador.core.economy.Commodity
import com.mercador.core.game.*
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.CrisisKind
import com.mercador.core.world.Side
import com.mercador.core.world.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** O mundo vivo (crises e revoltas), a nave, os contratos, o mapa útil e a campanha. */
class LivingWorldTest {
    private fun game(seed: Long = 2024, living: Boolean = true, campaign: Boolean = false, credits: Int = 5_000): Game {
        val world = World(Region.around(Universe(seed)), living)
        return Game(world, world.region.systems[0], credits, stories = false, campaign = campaign)
    }

    // ---- crises ----

    @Test
    fun `crises surgem, encarecem o que falta, entram no jornal e acabam no prazo`() {
        val g = game()
        val w = g.world
        var seen: com.mercador.core.world.Crisis? = null
        while (seen == null && g.day < 200) { g.passDays(1); seen = w.crises.firstOrNull() }
        assertNotNull(seen, "nenhuma crise em 200 dias")
        val cr = seen!!
        val sys = w.region.systems.first { it.id == cr.system }
        val c = cr.kind.wants.first()
        val calm = World(w.region).also { it.advance(w.day) } // o mesmo mundo, no mesmo dia, parado
        assertTrue(w.unitPrice(sys, c, 0.0, Side.BUY) > calm.unitPrice(sys, c, 0.0, Side.BUY) * 1.4, "preço não subiu")
        assertTrue(g.news.any { it.text.startsWith(cr.kind.label) && it.systemName == sys.name })
        g.passDays(cr.end - g.day + 1)
        assertTrue(w.crises.none { it === cr })
        assertTrue(g.news.any { it.text.contains("terminou") || it.text.contains("acabou") })
    }

    @Test
    fun `mundo parado nunca tem crise nem revolta`() {
        val g = game(living = false)
        g.passDays(300)
        assertTrue(g.world.crises.isEmpty())
        assertTrue(g.news.isEmpty())
        assertTrue(g.world.region.systems.all { g.world.controller(it) == it.faction })
    }

    @Test
    fun `entregar o que falta encerra a crise antes do prazo, com favor e reputacao`() {
        for (seed in 1L..30L) {
            val g = game(seed)
            val w = g.world
            while (g.day < 150 && w.crises.none { it.kind.relieves }) g.passDays(1)
            val cr = w.crises.firstOrNull { it.kind.relieves } ?: continue
            val sys = w.region.systems.first { it.id == cr.system }
            val c = cr.kind.wants.firstOrNull { !w.banned(sys, it) } ?: continue
            g.teleport(sys)
            val f = g.faction
            val rep0 = g.standing.reputation(f); val fav0 = g.favors(f)
            g.cargo[c] = cr.need
            assertTrue(g.sell(c, cr.need) is Outcome.Ok)
            assertNull(w.crisisAt(sys), "a crise devia ter acabado")
            assertEquals(1, g.stat(Stat.CRISES_SOCORRIDAS))
            assertTrue(g.standing.reputation(f) > rep0)
            assertEquals(fav0 + 1, g.favors(f))
            assertTrue(g.ledger.any { it.text.contains("acabou com a sua entrega") })
            return
        }
        fail<Unit>("nenhuma crise de socorro encontrada")
    }

    @Test
    fun `o bar sabe da proxima crise antes de ela comecar, e acerta`() {
        val g = game()
        repeat(200) {
            val up = g.world.upcomingCrises(Bar.CRISIS_LOOKAHEAD).firstOrNull()
            if (up != null && g.world.crises.size < World.MAX_CRISES - 1) {
                val (day, sk) = up
                val text = Bar.crisisRumor(g)
                assertTrue(text.contains(sk.first.name), text)
                g.passDays(day - g.day)
                assertEquals(sk.second, g.world.crisisAt(sk.first)?.kind)
                return
            }
            g.passDays(1)
        }
        fail<Unit>("o rumor nunca previu nada")
    }

    // ---- revoltas ----

    @Test
    fun `pressao alta por dias derruba o governo, muda a cor e as leis, e vira noticia`() {
        val g = game()
        val w = g.world
        val s = w.region.systems.first { it !== g.location }
        val old = w.controller(s)
        var days = 0
        while (w.controller(s) == old && days++ < 10) { w.pushMeters(s, -60, 60, 0); g.passDays(1) }
        val now = w.controller(s)
        assertNotEquals(old.id, now.id)
        assertEquals(now.ideology, w.ideology(s))
        for (c in Commodity.values()) {
            assertEquals(c.bannedBy(now.ideology), w.banned(s, c))
            assertEquals(c.bannedBy(now.ideology), w.quote(s, c).illegal)
        }
        assertTrue(g.news.any { it.text.startsWith("Revolta em ${s.name}") })
        assertTrue(w.meters(s).pressure < com.mercador.core.world.Meters.REVOLT_PRESSURE, "a pressão devia cair")
    }

    @Test
    fun `livros derrubam e armas seguram`() {
        val w = World(Region.around(Universe(5)))
        val s = w.region.systems[3]
        val p0 = w.meters(s).pressure
        w.recordTrade(s, Commodity.LIVROS, 40, Side.SELL)
        val pBooks = w.meters(s).pressure
        assertTrue(pBooks > p0)
        w.recordTrade(s, Commodity.ARMAS, 60, Side.SELL)
        assertTrue(w.meters(s).pressure < pBooks)
    }

    // ---- nave ----

    @Test
    fun `melhorias so no estaleiro certo, com nivel maximo e efeito real`() {
        val g = game(credits = 100_000)
        val w = g.world
        val yard = w.region.systems.first { Upgrade.PORAO.soldAt(it) }
        val noYard = w.region.systems.firstOrNull { !Upgrade.PORAO.soldAt(it) }
        if (noYard != null) { g.teleport(noYard); assertEquals(Outcome.Fail(Failure.SEM_ESTALEIRO), g.buyUpgrade(Upgrade.PORAO)) }
        g.teleport(yard)
        val far = w.region.systems.maxBy { w.region.distanceLy(yard, it) }
        val days0 = g.travelDays(far)
        repeat(Upgrade.PORAO.maxLevel) { assertTrue(g.buyUpgrade(Upgrade.PORAO) is Outcome.Ok) }
        assertEquals(Outcome.Fail(Failure.NIVEL_MAXIMO), g.buyUpgrade(Upgrade.PORAO))
        assertEquals(Game.HOLD_CAPACITY + Upgrade.PORAO.maxLevel * HOLD_STEP, g.holdCapacity)
        g.buyUpgrade(Upgrade.TANQUE); g.refuel()
        assertEquals(Game.FUEL_CAPACITY + FUEL_STEP, g.fuelCapacity); assertEquals(g.fuelCapacity, g.fuel)
        g.buyUpgrade(Upgrade.MOTOR)
        assertTrue(g.travelDays(far) < days0 || days0 == 1)
        g.credits = 0
        assertEquals(Outcome.Fail(Failure.SEM_DINHEIRO), g.buyUpgrade(Upgrade.MOTOR))
    }

    @Test
    fun `o compartimento esconde o contrabando mais caro e o transponder corta a abordagem`() {
        val (g, black) = (1L..40L).asSequence().map { game(it, credits = 100_000) }.mapNotNull { g ->
            g.world.region.systems.firstOrNull { Upgrade.COMPARTIMENTO.soldAt(it) && Commodity.values().count { c -> g.world.banned(it, c) } >= 2 }?.let { g to it }
        }.first()
        val w = g.world
        g.teleport(black)
        val banned = Commodity.values().filter { w.banned(black, it) }.sortedByDescending { it.basePrice }
        g.cargo[banned[0]] = 3; g.cargo[banned[1]] = 3
        assertEquals(6, g.evidence().values.sum())
        g.buyUpgrade(Upgrade.COMPARTIMENTO)
        val ev = g.evidence()
        assertEquals(6 - HIDDEN_STEP, ev.values.sum())
        assertNull(ev[banned[0]], "o mais caro devia sumir primeiro")
        val p0 = g.encounterChance(black)
        g.buyUpgrade(Upgrade.TRANSPONDER)
        assertTrue(g.encounterChance(black) < p0 || p0 == 0.0)
    }

    // ---- contratos ----

    private fun findOffer(g: Game, kind: ContractKind): Contract {
        repeat(400) {
            g.offers().firstOrNull { it.kind == kind }?.let { return it }
            g.passDays(1)
            if (it % 20 == 19) g.teleport(g.world.region.systems[(it / 20) % g.world.region.systems.size])
        }
        error("nenhuma oferta de $kind")
    }

    @Test
    fun `ofertas sao deterministicas e somem quando aceitas`() {
        val a = game(); val b = game()
        assertEquals(a.offers().map { it.id }, b.offers().map { it.id })
        val k = a.offers().firstOrNull { it.kind != ContractKind.LACRADA || it.tons <= a.holdFree } ?: return
        assertTrue(a.acceptContract(k) is Outcome.Ok)
        assertTrue(a.offers().none { it.id == k.id })
        assertEquals(Outcome.Fail(Failure.SEM_CONTRATO), a.acceptContract(k))
    }

    @Test
    fun `entrega cumprida paga o premio e move o mercado do destino`() {
        val g = game(living = false)
        val k = findOffer(g, ContractKind.ENTREGA)
        assertTrue(g.acceptContract(k) is Outcome.Ok)
        val dest = g.world.region.systems.first { it.id == k.destination }
        g.teleport(dest)
        assertEquals(Outcome.Fail(Failure.SEM_CARGA), g.deliverContract(k.id).first)
        g.cargo[k.commodity] = k.tons
        val c0 = g.credits
        val (o, text) = g.deliverContract(k.id)
        assertTrue(o is Outcome.Ok, "$o")
        assertEquals(c0 + k.reward, g.credits)
        assertNotNull(text)
        assertTrue(g.contracts.isEmpty())
        assertEquals(1, g.stat(Stat.CONTRATOS_CUMPRIDOS))
        assertTrue(g.world.volume(dest, k.commodity) < 0)
    }

    @Test
    fun `carga lacrada ocupa o porao, nao se vende e revela o que era na entrega`() {
        val g = game(living = false)
        val k = findOffer(g, ContractKind.LACRADA)
        val free0 = g.holdFree
        assertTrue(g.acceptContract(k) is Outcome.Ok)
        assertEquals(free0 - k.tons, g.holdFree)
        assertEquals(k.tons, g.sealedTons)
        assertEquals(0, g.ownTons(k.commodity))
        assertEquals(Outcome.Fail(Failure.SEM_CARGA), g.sell(k.commodity, 1))
        g.teleport(g.world.region.systems.first { it.id == k.destination })
        val (o, text) = g.deliverContract(k.id)
        assertTrue(o is Outcome.Ok)
        assertTrue(text!!.contains("Eram") || text.contains("Era"), text)
        assertEquals(0, g.sealedTons)
        assertEquals(1, g.stat(Stat.LACRADAS_ENTREGUES))
    }

    @Test
    fun `contrato vencido falha, cobra reputacao e o cliente leva a carga lacrada`() {
        val g = game(living = false)
        val k = findOffer(g, ContractKind.LACRADA)
        g.acceptContract(k)
        val client = g.factions.factions[k.client]
        val rep0 = g.standing.reputation(client)
        g.passDays(k.deadline - g.day + 1)
        assertTrue(g.contracts.isEmpty())
        assertEquals(0, g.cargo[k.commodity] ?: 0)
        assertTrue(g.standing.reputation(client) < rep0)
        assertEquals(1, g.stat(Stat.CONTRATOS_FALHOS))
    }

    @Test
    fun `no maximo tres contratos ao mesmo tempo`() {
        val g = game(living = false, credits = 50_000)
        // no mesmo dia, de estação em estação, até juntar três; a próxima oferta é recusada
        for (s in g.world.region.systems) {
            g.teleport(s)
            for (k in g.offers().filter { it.kind != ContractKind.LACRADA }) {
                if (g.contracts.size < Contracts.MAX_ACTIVE) assertTrue(g.acceptContract(k) is Outcome.Ok)
                else { assertEquals(Outcome.Fail(Failure.LIMITE_DE_CONTRATOS), g.acceptContract(k)); return }
            }
        }
        fail<Unit>("não juntei três contratos")
    }

    // ---- mapa útil ----

    @Test
    fun `o mapa estima a venda so onde o jogador viu os precos`() {
        val g = game(living = false)
        val here = g.location
        val other = g.reachable().first()
        assertNull(g.estimateSale(other))
        val c = Commodity.values().first { !g.isIllegalHere(it) && !g.world.banned(other, it) && g.world.quote(here, it).stock >= 2 }
        g.buy(c, 2)
        g.teleport(other); g.teleport(here) // visitar registra os preços
        val est = g.estimateSale(other)!!
        assertEquals(g.knownMarket(other)!!.sell[c.ordinal] * 2, est.revenue)
        assertEquals(0, est.bannedTons)
        assertNotNull(g.estimateSale(here))
    }

    // ---- campanha ----

    @Test
    fun `a campanha comeca com a divida, cobra parcelas e manda o cobrador`() {
        val g = game(living = false, campaign = true, credits = 0)
        val c = g.campaign!!
        assertEquals(Campaign.DEBT, c.total)
        assertTrue(g.ledger.first().text.contains(c.creditor.name))
        val (day1, due1) = c.next()!!
        assertTrue(due1 > 0)
        g.passDays(day1 - g.day)
        assertEquals(1, c.missed)
        assertTrue(c.total > Campaign.DEBT)
        assertEquals(1, g.pendingRetaliations())
        // a próxima chegada traz o cobrador
        val o = g.travel(g.reachable().first())
        assertTrue(o is Outcome.Confronting, "$o")
    }

    @Test
    fun `quitar a divida termina a campanha com epilogo, e depois segue em modo livre`() {
        val g = game(living = false, campaign = true, credits = 100_000)
        assertNull(g.epilogue())
        assertTrue(g.payCreditor(1_000) is Outcome.Ok)
        assertEquals(1_000, g.campaign!!.paid)
        g.payCreditor()
        assertEquals(CampaignOutcome.QUITADA, g.campaign!!.outcome)
        val e = g.epilogue()!!
        assertEquals("Quitado", e.title)
        assertTrue(e.paragraphs.size >= 3)
        assertTrue(e.paragraphs.none { it.contains("{") })
        g.dismissEpilogue()
        assertNull(g.epilogue())
        assertEquals(Outcome.Fail(Failure.SEM_CAMPANHA), g.payCreditor())
    }

    @Test
    fun `sem pagar ate o prazo, calote`() {
        val g = game(living = false, campaign = true, credits = 0)
        g.passDays(Campaign.DEADLINE + 1)
        assertEquals(CampaignOutcome.CALOTE, g.campaign!!.outcome)
        assertEquals("Calote", g.epilogue()!!.title)
    }

    // ---- salvamento ----

    @Test
    fun `o save guarda mundo vivo, nave, contratos, precos vistos, jornal e campanha`() {
        val g = game(campaign = true, credits = 60_000)
        val yard = g.world.region.systems.first { Upgrade.MOTOR.soldAt(it) }
        g.teleport(yard); g.buyUpgrade(Upgrade.MOTOR)
        g.offers().firstOrNull { it.kind != ContractKind.LACRADA }?.let { g.acceptContract(it) }
        g.offers().firstOrNull { it.kind == ContractKind.LACRADA && it.tons <= g.holdFree }?.let { g.acceptContract(it) }
        g.payCreditor(2_000)
        val s = g.world.region.systems.first { it !== g.location }
        repeat(6) { g.world.pushMeters(s, -60, 60, 0); g.passDays(1) }
        g.passDays(30)
        val back = Game.fromSave(SaveCodec.decode(SaveCodec.encode(g.toSave())))
        assertEquals(g.toSave().copy(savedAt = 0), back.toSave().copy(savedAt = 0))
        assertEquals(g.world.controller(s).id, back.world.controller(s).id)
        assertEquals(g.world.crises.map { it.kind to it.need }, back.world.crises.map { it.kind to it.need })
        assertEquals(1, back.level(Upgrade.MOTOR))
        assertEquals(g.contracts.map { it.id }, back.contracts.map { it.id })
        assertEquals(2_000, back.campaign!!.paid)
        assertEquals(g.news.size, back.news.size)
        // e o jogo segue igual nos dois
        g.passDays(20); back.passDays(20)
        assertEquals(g.toSave().copy(savedAt = 0), back.toSave().copy(savedAt = 0))
    }

    @Test
    fun `save antigo, sem os campos novos, continua abrindo em modo livre`() {
        val old = game(living = false).toSave()
        val full = kotlinx.serialization.json.Json.parseToJsonElement(SaveCodec.encode(old)).let { it as kotlinx.serialization.json.JsonObject }
        val json = kotlinx.serialization.json.JsonObject(full.filterKeys { it !in setOf("living", "campaign", "upgrades", "stats", "contracts", "known", "news", "crises") }).toString()
        val g = Game.fromSave(SaveCodec.decode(json))
        assertNull(g.campaign)
        assertFalse(g.world.living)
        assertEquals(Game.HOLD_CAPACITY, g.holdCapacity)
    }
}
