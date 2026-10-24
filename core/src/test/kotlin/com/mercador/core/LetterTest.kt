package com.mercador.core

import com.mercador.core.game.ContractKind
import com.mercador.core.game.Game
import com.mercador.core.game.Upgrade
import com.mercador.core.rng.Rng
import com.mercador.core.story.Ops
import com.mercador.core.story.StoryContent
import com.mercador.core.story.canTakeLetter
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LetterTest {
    private val story = StoryContent.all.first { it.id == "carta-do-governador" }

    /** Um jogo em que o rival da facção local governa algum sistema (a carta tem para onde ir). */
    private fun game(seed: Long = 2024): Game {
        for (s in seed until seed + 50) {
            val world = World(Region.around(Universe(s)))
            val g = Game(world, world.region.systems[0], credits = 5_000, stories = false)
            if (g.canTakeLetter()) return g
        }
        error("nenhuma semente com rival no mapa")
    }

    private fun takeLetter(g: Game, choice: Int = 0) = story.choices[choice].run(Ops(g, Rng(1))).also { assertTrue(g.contracts.any { it.kind == ContractKind.CARTA }) }

    @Test
    fun `levar a carta vira um contrato que nao ocupa o porao e paga so na entrega`() {
        val g = game()
        val credits = g.credits
        val free = g.holdFree
        val text = takeLetter(g)
        val k = g.contracts.single()
        assertEquals(ContractKind.CARTA, k.kind)
        assertEquals("uma carta lacrada", k.what)
        assertEquals(free, g.holdFree)
        assertEquals(credits, g.credits)
        assertTrue(text.contains(k.destName))
        assertNotSame(g.location, g.world.region.systems.first { it.id == k.destination })
        // o destino é governado pelo rival de quem pediu
        val dest = g.world.region.systems.first { it.id == k.destination }
        assertNotEquals(k.client, g.world.controller(dest).id)
        assertTrue(g.ledger.last().text.contains(k.destName))
    }

    @Test
    fun `abrir a carta tambem obriga a entregar`() {
        val g = game()
        takeLetter(g, choice = 1)
        assertEquals(ContractKind.CARTA, g.contracts.single().kind)
    }

    @Test
    fun `entregar no destino paga e rende favor com o governador`() {
        val g = game()
        takeLetter(g)
        val k = g.contracts.single()
        val client = g.factions.factions[k.client]
        val favors = g.favors(client)
        assertFalse(g.canDeliver(k)) // ainda não chegou
        g.teleport(g.world.region.systems.first { it.id == k.destination })
        assertTrue(g.canDeliver(k))
        val credits = g.credits
        val (o, text) = g.deliverContract(k.id)
        assertFalse(o is com.mercador.core.game.Outcome.Fail)
        assertEquals(credits + k.reward, g.credits)
        assertTrue(g.contracts.isEmpty())
        assertTrue(g.favors(client) >= favors)
        assertTrue(text!!.contains("${k.reward} cr"))
    }

    @Test
    fun `prazo vencido quebra a carta e o governador manda alguem`() {
        val g = game()
        takeLetter(g)
        val k = g.contracts.single()
        val before = g.pendingRetaliations()
        g.passDays(k.deadline - g.day + 1)
        assertTrue(g.contracts.isEmpty())
        assertEquals(before + 1, g.pendingRetaliations())
        assertTrue(g.openConsequences().any { it.first.role.contains(k.clientName) })
    }

    @Test
    fun `a revista pode achar a carta, mas nunca no compartimento oculto`() {
        var found = false
        for (seed in 2024L until 2124L) {
            val g = game(seed)
            takeLetter(g)
            val k = g.contracts.single()
            for (f in g.factions.factions) g.standing.addHeat(f, 100) // patrulha em toda chegada provável
            val dest = g.reachable().firstOrNull { it.id != k.destination } ?: continue
            g.refuel()
            g.travel(dest)
            if (g.ledger.any { it.text.contains("apreendeu a carta") }) {
                assertTrue(g.contracts.none { it.kind == ContractKind.CARTA })
                assertTrue(g.pendingRetaliations() >= 1)
                found = true
                break
            }
        }
        assertTrue(found, "nenhuma revista achou a carta em 100 sementes")
        // com o compartimento oculto, a mesma situação nunca acha
        for (seed in 2024L until 2124L) {
            val g = game(seed)
            g.grantUpgrade(Upgrade.COMPARTIMENTO)
            takeLetter(g)
            val k = g.contracts.single()
            for (f in g.factions.factions) g.standing.addHeat(f, 100)
            val dest = g.reachable().firstOrNull { it.id != k.destination } ?: continue
            g.refuel(); g.travel(dest)
            assertFalse(g.ledger.any { it.text.contains("apreendeu a carta") })
        }
    }

    @Test
    fun `a carta sobrevive a salvar e carregar`() {
        val g = game()
        takeLetter(g)
        val back = Game.fromSave(g.toSave())
        val k = back.contracts.single()
        assertEquals(ContractKind.CARTA, k.kind)
        assertEquals(g.contracts.single().destName, k.destName)
    }

    @Test
    fun `sem vaga para contrato a historia so oferece recusar`() {
        val g = game()
        // enche os contratos com ofertas da estação (ou com cartas de dias diferentes)
        var guard = 0
        while (g.contracts.size < com.mercador.core.game.Contracts.MAX_ACTIVE && guard++ < 50) {
            g.offers().firstOrNull { it.kind != ContractKind.LACRADA }?.let { g.acceptContract(it) } ?: g.passDays(4)
        }
        assertFalse(g.canTakeLetter())
        assertFalse(story.choices[0].req(g))
        assertTrue(story.choices[2].req(g))
    }
}
