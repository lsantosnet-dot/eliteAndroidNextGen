package com.mercador.core

import com.mercador.core.economy.Commodity
import com.mercador.core.game.*
import com.mercador.core.story.*
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StoryTest {
    private fun newGame(seed: Long = 2024, stories: Boolean = true): Game {
        val world = World(Region.around(Universe(seed)))
        return Game(world, world.region.systems[0], credits = 5000, stories = stories)
    }

    @Test
    fun `conteudo e consistente`() {
        val all = StoryContent.all
        assertTrue(all.size >= 62, "storylets: ${all.size}")
        assertEquals(all.size, all.map { it.id }.toSet().size)
        for (s in all) {
            assertTrue(s.choices.isNotEmpty() && s.text.isNotBlank() && s.triggers.isNotEmpty() && s.weight > 0, s.id)
            for (c in s.choices) assertTrue(c.label.isNotBlank(), s.id)
        }
    }

    @Test
    fun `lacunas sao preenchidas com o mundo e o governador e estavel`() {
        val g = newGame()
        val a = StoryEngine.fill(g, "{governador} de {sistema} ({facção}) e {rival}", 1)
        assertFalse(a.contains("{"))
        assertTrue(a.contains(g.location.name) && a.contains(g.faction.name))
        assertEquals(a, StoryEngine.fill(g, "{governador} de {sistema} ({facção}) e {rival}", 99))
    }

    @Test
    fun `so surgem storylets cujas condicoes valem e cada uma tem opcao disponivel`() {
        val g = newGame()
        for (t in Trigger.values()) for (s in StoryEngine.eligible(g, t)) {
            assertTrue(s.cond(g), s.id)
            assertTrue(s.choices.any { it.req(g) }, s.id)
        }
        // sem marca de violência, a storylet do eco não pode surgir
        assertTrue(StoryEngine.eligible(g, Trigger.CHEGADA).none { it.id == "eco" })
        g.addTrait(Trait.MARCADO, null)
        assertTrue(StoryEngine.eligible(g, Trigger.CHEGADA).any { it.id == "eco" })
    }

    @Test
    fun `sorteio e deterministico`() {
        fun first(): String? {
            val g = newGame()
            repeat(200) {
                g.refuel(); val d = g.reachable().firstOrNull() ?: return null
                val out = g.travel(d)
                if (out is Outcome.Story) return out.card.storylet.id + g.day
                if (g.busy) return null
            }
            return null
        }
        assertNotNull(first())
        assertEquals(first(), first())
    }

    @Test
    fun `historia pendente bloqueia o comercio e escolher resolve`() {
        val g = (1L..30L).firstNotNullOfOrNull { seed ->
            val g = newGame(seed)
            repeat(100) {
                g.refuel(); val d = g.reachable().firstOrNull() ?: return@repeat
                if (g.travel(d) is Outcome.Story) return@firstNotNullOfOrNull g
                if (g.busy) return@firstNotNullOfOrNull null
            }
            null
        } ?: error("nenhuma história")
        val card = g.pendingStory!!
        assertEquals(Outcome.Fail(Failure.PERSEGUICAO_PENDENTE), g.buy(Commodity.COMIDA, 1))
        assertEquals(Outcome.Fail(Failure.ESCOLHA_INDISPONIVEL), g.chooseStory(99))
        val i = card.choices.indexOfFirst { it.available }
        val out = g.chooseStory(i)
        assertTrue(out is Outcome.StoryResolved)
        assertFalse((out as Outcome.StoryResolved).text.contains("{"))
        assertNull(g.pendingStory)
        assertEquals(Outcome.Fail(Failure.SEM_HISTORIA), g.chooseStory(0))
    }

    @Test
    fun `um ano de historias nao quebra invariantes e usa varias storylets`() {
        val seen = HashSet<String>()
        for (seed in 1L..8L) {
            val g = newGame(seed)
            var steps = 0
            while (g.day < 365 && steps++ < 600) {
                g.credits = maxOf(g.credits, 300)
                when {
                    g.pursuit != null -> g.respond(Response.JOGAR_FORA)
                    g.confrontation != null -> g.act(Posture.RESISTIR)
                    g.pendingStory != null -> {
                        val card = g.pendingStory!!
                        seen += card.storylet.id
                        val ok = card.choices.indices.filter { card.choices[it].available }
                        g.chooseStory(ok[(g.day + steps) % ok.size])
                    }
                    else -> {
                        g.refuel()
                        val d = g.reachable().let { it[(g.day * 7 + steps) % it.size] }
                        g.travel(d)
                    }
                }
                assertTrue(g.credits >= 0)
                assertTrue(g.fuel in 0..Game.FUEL_CAPACITY)
                assertTrue(g.holdUsed <= Game.HOLD_CAPACITY, "porão ${g.holdUsed}")
                for (s in g.world.region.systems) { val m = g.world.meters(s); assertTrue(m.control in 0..100 && m.discontent in 0..100 && m.supply in 0..100) }
            }
            for (l in g.ledger + g.voiceLog) assertFalse(l.text.contains("{"), l.text)
        }
        assertTrue(seen.size >= 8, "storylets vistas: ${seen.size} $seen")
    }

    @Test
    fun `escolha pode abrir um confronto`() {
        val g = newGame()
        g.addTrait(Trait.MARCADO, null)
        val story = StoryContent.all.first { it.id == "rosto-conhecido" }
        val card = StoryEngine.card(g, story, Trigger.CHEGADA, 1L)
        // injeta o cartão pelo caminho público: força a escolha via Ops
        val ops = Ops(g, com.mercador.core.rng.Rng(1))
        story.choices[1].run(ops)
        assertNotNull(g.confrontation)
        assertTrue(card.choices[1].available)
    }

    @Test
    fun `voz interior reage ao que voce fez e e deterministica`() {
        val g = newGame()
        val neutral = InnerVoice.line(g)
        assertEquals(neutral, InnerVoice.line(g))
        g.addTrait(Trait.MARCADO, null)
        val marked = InnerVoice.line(g)
        val marcadoLines = setOf("Tem coisas que o dinheiro não limpa.", "Eu fiz isso. Não há versão em que eu não tenha feito.", "Alguém, em algum lugar, ainda me procura.")
        assertTrue(marked in marcadoLines)
        assertNotEquals(neutral, marked)
    }

    @Test
    fun `bar vende informacao verdadeira e segredos viram moeda`() {
        val g = newGame()
        val offers = Bar.offers(g)
        assertEquals(offers.map { it.kind to it.price }, Bar.offers(g).map { it.kind to it.price })
        val secret = offers.first { it.kind == RumorKind.SEGREDO }
        val c0 = g.credits
        val (out, text) = g.buyRumor(secret)
        assertTrue(out is Outcome.Ok)
        assertEquals(c0 - secret.price, g.credits)
        assertEquals(1, g.secretsAbout(secret.about!!))
        assertNotNull(text)

        val sale = g.buyRumor(offers.first { it.kind == RumorKind.ONDE_VENDER }).second!!
        val best = Bar.bestSale(g)
        if (best != null) assertTrue(sale.contains(best.second.name))

        g.credits = 0
        assertEquals(Outcome.Fail(Failure.SEM_DINHEIRO), g.buyRumor(offers[0]).first)
    }

    @Test
    fun `toda escolha de toda storylet roda sem quebrar invariantes nem deixar lacunas`() {
        val traits = listOf(Trait.CALEJADO, Trait.MARCADO, Trait.INSONE, Trait.DEVEDOR)
        for (seed in 1L..3L) for (story in StoryContent.all) for ((i, choice) in story.choices.withIndex()) {
            val g = newGame(seed, stories = false)
            g.credits = 10_000
            for (c in Commodity.values()) g.cargo[c] = 1
            traits.forEach { g.grantTrait(it, 60) }
            val ops = Ops(g, com.mercador.core.rng.Rng(seed * 31 + i))
            val text = StoryEngine.fill(g, choice.run(ops), seed)
            val ctx = "${story.id}#$i"
            assertFalse(text.contains("{") || text.isBlank(), ctx)
            assertTrue(g.credits >= 0, ctx); assertTrue(g.fuel in 0..Game.FUEL_CAPACITY, ctx)
            assertTrue(g.holdUsed <= Game.HOLD_CAPACITY + Commodity.values().size, ctx)
            for (l in g.ledger + g.voiceLog) assertFalse(l.text.contains("{"), "$ctx: ${l.text}")
            val m = g.world.meters(g.location)
            assertTrue(m.control in 0..100 && m.discontent in 0..100 && m.supply in 0..100, ctx)
        }
    }

    @Test
    fun `nenhuma storylet fica sem saida e as dicas nao deixam placeholders`() {
        val g = newGame(1, stories = false)
        for (s in StoryContent.all) {
            // numa situação sem nada (sem dinheiro, sem carga), a escolha "gratuita" tem de existir quando a condição valer
            val card = StoryEngine.card(g, s, Trigger.CHEGADA, 1L)
            for (c in card.choices) assertFalse((c.label + c.hint).contains("{"), s.id)
            if (s.cond(g)) assertTrue(card.choices.any { it.available }, "${s.id} sem opção disponível quando a condição vale")
        }
    }
}
