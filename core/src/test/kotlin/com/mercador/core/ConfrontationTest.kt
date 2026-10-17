package com.mercador.core

import com.mercador.core.economy.Commodity
import com.mercador.core.game.*
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConfrontationTest {
    private fun newGame(seed: Long): Game {
        val world = World(Region.around(Universe(seed)))
        return Game(world, world.region.systems[0], credits = 100_000)
    }

    private fun chase(seed: Long): Game? {
        val g = newGame(seed)
        repeat(200) {
            g.refuel()
            val dest = g.reachable().firstOrNull() ?: return null
            val c = Commodity.values().firstOrNull { it.bannedBy(dest.traits.ideology) && !it.bannedBy(g.location.traits.ideology) &&
                g.world.quote(g.location, it).stock > 0 }
            if (c != null) g.buy(c, minOf(g.holdFree, g.world.quote(g.location, c).stock, 6))
            if (g.travel(dest) is Outcome.Pursued) return g
            for ((k, t) in g.cargo.toMap()) g.sell(k, t)
            g.standing.addHeat(dest.faction, 25)
        }
        return null
    }

    private fun scene(): Game = (1L..40L).firstNotNullOfOrNull { chase(it) } ?: error("sem perseguição")

    private fun startScene(g: Game): Confrontation {
        val out = g.respond(Response.CONFRONTAR)
        assertTrue(out is Outcome.Confronting)
        return (out as Outcome.Confronting).scene
    }

    @Test
    fun `confrontar abre uma cena com pessoa nomeada e bloqueia o comercio`() {
        val g = scene()
        val s = startScene(g)
        assertNull(g.pursuit)
        assertNotNull(g.confrontation)
        assertTrue(s.opponent.name.contains(" "))
        assertTrue(s.maxRounds in 3..5)
        assertEquals(Outcome.Fail(Failure.PERSEGUICAO_PENDENTE), g.buy(Commodity.COMIDA, 1))
        assertEquals(Outcome.Fail(Failure.SEM_PERSEGUICAO), g.respond(Response.BLEFAR))
    }

    @Test
    fun `posturas dependem de fama, segredo, favor e carga`() {
        val g = scene()
        val s = startScene(g)
        val f = s.opponent.faction
        var p = g.postures()
        assertTrue(Posture.RESISTIR in p && Posture.VIOLENCIA in p && Posture.SACRIFICAR in p)
        assertFalse(Posture.INTIMIDAR in p || Posture.EXPOR_SEGREDO in p || Posture.COBRAR_FAVOR in p)
        g.grantSecret(f); g.grantFavor(f)
        p = g.postures()
        assertTrue(Posture.EXPOR_SEGREDO in p && Posture.COBRAR_FAVOR in p)
        assertEquals(Outcome.Fail(Failure.POSTURA_INDISPONIVEL), g.act(Posture.INTIMIDAR))
    }

    @Test
    fun `expor segredo e cobrar favor gastam o recurso para sempre`() {
        val g = scene()
        val s = startScene(g)
        val f = s.opponent.faction
        g.grantSecret(f); g.grantFavor(f)
        g.act(Posture.EXPOR_SEGREDO)
        assertEquals(0, g.secretsAbout(f))
        if (g.confrontation != null) {
            g.act(Posture.COBRAR_FAVOR)
            assertEquals(0, g.favors(f))
        }
    }

    @Test
    fun `segredo e favor derrubam o oponente rapido`() {
        val g = scene()
        val s = startScene(g)
        val f = s.opponent.faction
        repeat(3) { g.grantSecret(f) }
        val c0 = s.opponentCool
        g.act(Posture.EXPOR_SEGREDO)
        assertTrue(s.opponentCool <= c0 - 35 || s.finished)
    }

    @Test
    fun `cena sempre termina em ate maxRounds e vira linha permanente no livro`() {
        val g = scene()
        val s = startScene(g)
        val lines0 = g.ledger.size
        var out: Outcome = Outcome.Confronting(s)
        var n = 0
        while (out is Outcome.Confronting) { out = g.act(Posture.RESISTIR); n++; assertTrue(n <= s.maxRounds) }
        assertTrue(out is Outcome.SceneEnded)
        assertNull(g.confrontation)
        assertEquals(lines0 + 1, g.ledger.size)
        assertTrue(g.ledger.last().text.contains(s.opponent.name))
        g.world.advance(500)
        assertEquals(lines0 + 1, g.ledger.size)
    }

    @Test
    fun `perder a cena custa carga e multa, vencer mantem a carga`() {
        var won = false; var lost = false
        for (seed in 1L..40L) {
            val g = chase(seed) ?: continue
            val tons = g.pursuit!!.evidenceTons
            val credits0 = g.credits
            startScene(g)
            var out: Outcome = Outcome.Confronting(g.confrontation!!)
            while (out is Outcome.Confronting) out = g.act(Posture.RESISTIR)
            val end = (out as Outcome.SceneEnded).ending
            if (end == Ending.ELE_CEDEU) { won = true; assertTrue(g.credits == credits0); assertTrue(g.evidence().values.sum() >= tons) }
            else { lost = true; assertTrue(g.credits <= credits0); assertTrue(g.evidence().isEmpty()) }
        }
        assertTrue(won || lost)
    }

    @Test
    fun `violencia e irreversivel, marca para sempre e gera retaliacao com nome`() {
        val g = scene()
        val s = startScene(g)
        val heat0 = g.standing.heat(s.opponent.faction)
        val rep0 = g.standing.reputation(s.opponent.faction)
        val cargo0 = g.cargo.toMap()
        val credits0 = g.credits
        val out = g.act(Posture.VIOLENCIA) as Outcome.SceneEnded
        assertEquals(Ending.VIOLENCIA, out.ending)
        assertTrue(g.hasTrait(Trait.MARCADO))
        assertTrue(g.hasTrait(Trait.INSONE))
        assertTrue(g.standing.heat(s.opponent.faction) > heat0)
        assertTrue(g.standing.reputation(s.opponent.faction) < rep0)
        assertEquals(cargo0, g.cargo.toMap(), "violência não dá recompensa nem tira a carga")
        assertEquals(credits0, g.credits)
        assertEquals(1, g.pendingRetaliations())
    }

    @Test
    fun `retaliacao chega meses depois como cena contra parente`() {
        val g = scene()
        startScene(g)
        g.act(Posture.VIOLENCIA)
        var scene: Confrontation? = null
        repeat(400) {
            if (scene != null) return@repeat
            g.credits = 100_000
            g.refuel()
            for ((k, t) in g.cargo.toMap()) g.sell(k, t)
            val d = g.reachable().firstOrNull() ?: return@repeat
            val out = g.travel(d)
            if (out is Outcome.Pursued) g.respond(Response.JOGAR_FORA)
            if (out is Outcome.Confronting && out.scene.retaliation) scene = out.scene
            g.confrontation?.let { if (it !== scene) { while (g.confrontation != null) g.act(Posture.RESISTIR) } }
        }
        assertNotNull(scene, "retaliação nunca chegou")
        assertTrue(scene!!.opponent.role.startsWith("parente de"))
        assertEquals(0, g.pendingRetaliations())
    }

    @Test
    fun `tracos se dissipam com o tempo mas marcas da violencia nao`() {
        val g = scene()
        startScene(g)
        g.act(Posture.VIOLENCIA)
        g.cargo.clear()
        g.credits = 0
        val day0 = g.day
        while (g.day - day0 < 100) { g.credits = 0; assertTrue(g.haulFreight() is Outcome.Ok) }
        assertTrue(g.hasTrait(Trait.MARCADO))
        assertFalse(g.hasTrait(Trait.INSONE), "Insone devia ter passado após 90 dias")
    }

    @Test
    fun `mesma cena mesmo resultado`() {
        fun run(): List<Int> {
            val g = scene()
            val s = startScene(g)
            val trail = ArrayList<Int>()
            while (g.confrontation != null) { g.act(Posture.RESISTIR); trail += s.playerCool; trail += s.opponentCool }
            return trail
        }
        assertEquals(run(), run())
    }

    @Test
    fun `entregar bens essenciais em racionamento rende favor`() {
        val g = newGame(2024)
        val s = g.location
        repeat(10) { g.world.recordTrade(s, Commodity.COMIDA, 100, com.mercador.core.world.Side.BUY) }
        g.cargo[Commodity.REMEDIO] = 12
        g.sell(Commodity.REMEDIO, 12)
        assertEquals(1, g.favors(s.faction))
    }
}
