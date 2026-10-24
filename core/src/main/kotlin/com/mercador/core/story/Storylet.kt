package com.mercador.core.story

import com.mercador.core.economy.Commodity
import com.mercador.core.game.Game
import com.mercador.core.game.Outcome
import com.mercador.core.game.People
import com.mercador.core.game.Trait
import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive
import com.mercador.core.universe.Faction

/** Quando uma storylet pode surgir: em trânsito (salto) ou ao chegar (seção 9). */
enum class Trigger { SALTO, CHEGADA }

/** Uma opção: [req] decide se aparece disponível; [run] aplica os efeitos e devolve o desfecho. */
class Choice(
    val label: String,
    val hint: String = "",
    val req: (Game) -> Boolean = { true },
    val run: (Ops) -> String,
)

/**
 * Evento curto com condições sobre o estado do mundo e texto com lacunas ({sistema}, {facção},
 * {governador}, {rival}) preenchidas por quem o mundo é. O confronto é um tipo especial: uma
 * escolha pode abrir uma cena (Ops.scene).
 */
class Storylet(
    val id: String,
    val title: String,
    val triggers: Set<Trigger>,
    val weight: Int,
    val cond: (Game) -> Boolean,
    val text: String,
    val choices: List<Choice>,
)

class CardChoice(val label: String, val hint: String, val available: Boolean)

/** Storylet sorteada, com o texto já preenchido, à espera da escolha do jogador. */
class StoryCard(val storylet: Storylet, val text: String, val trigger: Trigger, val choices: List<CardChoice>, internal val seed: Long)

/** Efeitos que uma escolha pode ter, todos limitados para nunca quebrar invariantes do jogo. */
class Ops(val g: Game, val rng: Rng) {
    val system = g.location
    val faction: Faction = g.faction
    val rival: Faction = g.factions.factions.filter { it.id != faction.id }.minBy { g.factions.relation(faction.id, it.id) }

    fun credits(n: Int) { g.credits = (g.credits + n).coerceAtLeast(0) }
    fun heat(n: Int) = g.standing.addHeat(faction, n)
    fun rep(n: Int) = g.standing.addReputation(faction, n)
    fun favor() = g.grantFavor(faction)
    fun secret(about: Faction = faction) = g.grantSecret(about)
    fun trait(t: Trait, days: Int? = null) = g.addTrait(t, days)
    fun dropTrait(t: Trait) = g.removeTrait(t)
    fun fuel(n: Int) = g.adjustFuel(n)
    fun days(n: Int) = g.passDays(n)
    fun has(c: Commodity, tons: Int) = (g.cargo[c] ?: 0) >= tons
    fun lose(c: Commodity, tons: Int) {
        val left = (g.cargo[c] ?: 0) - tons
        if (left <= 0) g.cargo.remove(c) else g.cargo[c] = left
    }
    /** Ganha carga até onde o porão permitir. */
    fun gain(c: Commodity, tons: Int): Int {
        val n = minOf(tons, g.holdFree)
        if (n > 0) g.cargo[c] = (g.cargo[c] ?: 0) + n
        return n
    }
    fun meters(control: Int = 0, discontent: Int = 0, supply: Int = 0) = g.world.pushMeters(system, control, discontent, supply)
    fun voice(text: String) = g.addVoice(StoryEngine.fill(g, text, 0))
    fun ledger(text: String) = g.note(StoryEngine.fill(g, text, 0))
    fun scene(role: String) {
        g.startScene(People.generate(rng, faction, role), emptyMap(), retaliation = false)
    }
}

object StoryEngine {
    /** Chance de haver uma história em cada chegada. */
    const val CHANCE = 0.6
    const val COOLDOWN_DAYS = 25

    fun eligible(g: Game, trigger: Trigger): List<Storylet> = StoryContent.all.filter { s ->
        trigger in s.triggers &&
            (g.lastSeen(s.id)?.let { g.day - it >= COOLDOWN_DAYS } ?: true) &&
            s.cond(g) && s.choices.any { it.req(g) }
    }

    /** Sorteio determinístico por sistema e dia: a mesma situação gera a mesma história. */
    fun draw(g: Game): StoryCard? {
        val seed = derive(g.location.seedKey(), g.day.toLong(), 0x5707)
        val rng = Rng(seed)
        if (!rng.chance(CHANCE)) return null
        val trigger = if (rng.chance(0.4)) Trigger.SALTO else Trigger.CHEGADA
        val pool = eligible(g, trigger).ifEmpty { eligible(g, if (trigger == Trigger.SALTO) Trigger.CHEGADA else Trigger.SALTO) }
        if (pool.isEmpty()) return null
        var roll = rng.nextInt(pool.sumOf { it.weight })
        val story = pool.first { roll -= it.weight; roll < 0 }
        return card(g, story, trigger, seed)
    }

    fun card(g: Game, story: Storylet, trigger: Trigger, seed: Long): StoryCard =
        StoryCard(story, fill(g, story.text, seed), trigger, story.choices.map { CardChoice(fill(g, it.label, seed), fill(g, it.hint, seed), it.req(g)) }, seed)

    fun resolve(g: Game, card: StoryCard, index: Int): String {
        val ops = Ops(g, Rng(derive(card.seed, index.toLong(), 0x0B5)))
        val text = fill(g, card.storylet.choices[index].run(ops), card.seed)
        return text
    }

    /** Lacunas preenchidas pelo mundo: o governador e o rival têm nome e são sempre os mesmos para o sistema. */
    fun fill(g: Game, template: String, seed: Long): String {
        val sys = g.location
        val faction = g.faction
        val rival = g.factions.factions.filter { it.id != faction.id }.minBy { g.factions.relation(faction.id, it.id) }
        val governor = People.generate(Rng(derive(sys.seedKey(), 0x607)), faction, "governador").name
        return template
            .replace("{sistema}", sys.name)
            .replace("{facção}", faction.name)
            .replace("{governador}", governor)
            .replace("{rival}", rival.name)
    }
}
