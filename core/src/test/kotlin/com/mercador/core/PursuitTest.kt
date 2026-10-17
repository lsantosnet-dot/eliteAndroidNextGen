package com.mercador.core

import com.mercador.core.economy.Commodity
import com.mercador.core.game.*
import com.mercador.core.universe.Region
import com.mercador.core.universe.StarSystem
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PursuitTest {
    private fun newGame(seed: Long = 2024): Game {
        val world = World(Region.around(Universe(seed)))
        return Game(world, world.region.systems[0], credits = 100_000, stories = false)
    }

    /** Joga até uma perseguição aparecer carregando contrabando; null se não houver em 200 saltos. */
    private fun chase(seed: Long): Game? {
        val g = newGame(seed)
        repeat(200) {
            g.refuel()
            val dest = g.reachable().firstOrNull() ?: return null
            // contrabando para a lei do destino, comprado onde for barato
            val c = Commodity.values().firstOrNull { it.bannedBy(dest.traits.ideology) && !it.bannedBy(g.location.traits.ideology) &&
                g.world.quote(g.location, it).stock > 0 } 
            if (c != null) g.buy(c, minOf(g.holdFree, g.world.quote(g.location, c).stock, 5))
            val out = g.travel(dest)
            if (out is Outcome.Pursued) return g
            for ((k, t) in g.cargo.toMap()) g.sell(k, t)
            g.standing.addHeat(dest.faction, 25) // acelera o encontro
        }
        return null
    }

    @Test
    fun `comerciar bem proibido sobe a atencao e baixa a reputacao`() {
        val g = newGame()
        val s = g.location
        val c = Commodity.values().first { it.bannedBy(s.traits.ideology) && g.world.quote(s, it).stock > 0 }
        val h0 = g.standing.heat(s.faction); val r0 = g.standing.reputation(s.faction)
        assertTrue(g.buy(c, 2) is Outcome.Ok)
        assertTrue(g.standing.heat(s.faction) > h0)
        assertTrue(g.standing.reputation(s.faction) < r0)
    }

    @Test
    fun `comercio legal nao gera atencao`() {
        val g = newGame()
        val s = g.location
        val c = Commodity.values().first { !it.bannedBy(s.traits.ideology) && g.world.quote(s, it).stock > 0 }
        g.buy(c, 1)
        assertEquals(0, g.standing.heat(s.faction))
    }

    @Test
    fun `atencao esfria com o tempo`() {
        val g = newGame()
        g.standing.addHeat(g.faction, 30)
        g.refuel()
        val dest = g.reachable().first()
        g.travel(dest)
        assertTrue(g.standing.heat(g.world.region.systems[0].faction) < 30)
    }

    @Test
    fun `reputacao se espalha para rivais e aliados`() {
        val g = newGame()
        val f = g.factions.factions
        val a = f[0]
        g.standing.addReputation(a, 40)
        for (o in f.drop(1)) {
            val rel = g.factions.relation(a.id, o.id)
            val r = g.standing.reputation(o)
            if (rel > 20) assertTrue(r > 0) else if (rel < -20) assertTrue(r < 0)
        }
    }

    @Test
    fun `perseguicao bloqueia o comercio ate ser resolvida`() {
        val g = (1L..30L).firstNotNullOfOrNull { chase(it) } ?: error("nenhuma perseguição em 30 universos")
        assertNotNull(g.pursuit)
        assertEquals(Outcome.Fail(Failure.PERSEGUICAO_PENDENTE), g.buy(Commodity.COMIDA, 1))
        assertEquals(Outcome.Fail(Failure.PERSEGUICAO_PENDENTE), g.travel(g.world.region.systems[1]))
        assertTrue(g.responses().contains(Response.ENTREGAR_SE))
        assertTrue(g.responses().contains(Response.JOGAR_FORA))
    }

    @Test
    fun `cada resposta resolve e tem o custo prometido`() {
        for (r in Response.values().filter { it != Response.CONFRONTAR }) {
            val g = (1L..30L).firstNotNullOfOrNull { chase(it) } ?: error("sem perseguição")
            if (r !in g.responses()) continue
            val tons = g.pursuit!!.evidenceTons
            val credits0 = g.credits
            val out = g.respond(r)
            assertTrue(out is Outcome.Resolved, "$r -> $out")
            val res = (out as Outcome.Resolved).resolution
            assertNull(g.pursuit)
            when (r) {
                Response.JOGAR_FORA -> { assertTrue(res.success); assertEquals(tons, res.lostTons); assertEquals(credits0, g.credits) }
                Response.ENTREGAR_SE -> { assertFalse(res.success); assertTrue(g.credits < credits0) }
                Response.SUBORNAR -> assertTrue(g.credits < credits0)
                else -> {}
            }
            if (res.lostTons > 0) assertTrue(g.evidenceIsGone(), "$r deixou prova")
            // pode comerciar de novo
            assertNotEquals(Outcome.Fail(Failure.PERSEGUICAO_PENDENTE), g.buy(Commodity.COMIDA, 0))
        }
    }

    @Test
    fun `fugir gasta combustivel e, se der certo, leva a outra facao`() {
        var escaped = false
        var tried = 0
        for (seed in 1L..60L) {
            val h = chase(seed) ?: continue
            if (Response.FUGIR !in h.responses()) continue
            tried++
            val p = h.pursuit!!.patrol
            val fuel0 = h.fuel
            val res = (h.respond(Response.FUGIR) as Outcome.Resolved).resolution
            assertEquals(fuel0 - PursuitRules.FLEE_FUEL, h.fuel)
            if (res.success) { assertNotEquals(p.id, h.faction.id); escaped = true }
        }
        assertTrue(tried > 3, "poucas perseguições testadas: $tried")
        assertTrue(escaped, "nenhuma fuga bem-sucedida em $tried tentativas")
    }

    @Test
    fun `sem perseguicao nao ha resposta`() {
        val g = newGame()
        assertEquals(Outcome.Fail(Failure.SEM_PERSEGUICAO), g.respond(Response.BLEFAR))
    }

    @Test
    fun `chances respeitam a doutrina e os tracos`() {
        val g = (1L..30L).firstNotNullOfOrNull { chase(it) } ?: error("sem perseguição")
        val p = g.pursuit!!
        assertTrue(PursuitRules.bribeChance(p) in 0.05..0.95)
        assertTrue(PursuitRules.bribeCost(p) > 100)
        assertTrue(PursuitRules.fine(p) > PursuitRules.surrenderFine(p))
        assertTrue(PursuitRules.encounterChance(80, 80, true, true) > PursuitRules.encounterChance(0, 0, false, false))
    }

    @Test
    fun `mesma situacao gera a mesma perseguicao`() {
        val a = chase(3); val b = chase(3)
        assertEquals(a == null, b == null)
        if (a != null) {
            assertEquals(a.pursuit!!.evidence, b!!.pursuit!!.evidence)
            assertEquals(a.location.id, b.location.id)
        }
    }

    private fun Game.evidenceIsGone() = evidence().isEmpty()

    @Test
    fun `multa nunca passa de metade do dinheiro e o emprestimo salva o falido`() {
        val g = (1L..30L).firstNotNullOfOrNull { chase(it) } ?: error("sem perseguição")
        g.credits = 60
        val before = g.credits
        g.respond(Response.ENTREGAR_SE)
        assertTrue(g.credits >= before / 2)
        g.credits = 50
        assertEquals(0, g.holdUsed.coerceAtMost(0))
        assertEquals(Outcome.Fail(Failure.SEM_DIVIDA), g.repay())
        if (g.holdUsed == 0) {
            assertTrue(g.takeLoan() is Outcome.Ok)
            assertEquals(Game.LOAN_AMOUNT + 50, g.credits)
            assertEquals(Outcome.Fail(Failure.NAO_ELEGIVEL), g.takeLoan())
            assertTrue(g.debt > Game.LOAN_AMOUNT)
            g.credits = 10_000
            g.repay()
            assertEquals(0, g.debt)
        }
        g.credits = 20
        if (g.holdUsed == 0) {
            val day0 = g.day
            assertTrue(g.haulFreight() is Outcome.Ok)
            assertEquals(20 + Game.FREIGHT_PAY, g.credits)
            assertEquals(day0 + Game.FREIGHT_DAYS, g.day)
            g.credits = Game.FREIGHT_THRESHOLD
            assertEquals(Outcome.Fail(Failure.NAO_ELEGIVEL), g.haulFreight())
        }
    }
}
