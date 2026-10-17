package com.mercador.core.story

import com.mercador.core.game.Game
import com.mercador.core.game.Trait
import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive

/**
 * Voz interior (seção 8.4): frases curtas de monólogo que reagem ao que o jogador fez.
 * Primeira regra que casa vence; a frase é escolhida por sorteio determinístico no dia.
 */
object InnerVoice {
    private class Rule(val cond: (Game) -> Boolean, val lines: List<String>)

    private val rules = listOf(
        Rule({ it.hasTrait(Trait.MARCADO) }, listOf(
            "Tem coisas que o dinheiro não limpa.", "Eu fiz isso. Não há versão em que eu não tenha feito.", "Alguém, em algum lugar, ainda me procura.")),
        Rule({ it.hasTrait(Trait.INSONE) }, listOf(
            "Fecho os olhos e vejo o livro de contas.", "Dormir é um luxo que eu deixei de pagar.")),
        Rule({ it.hasTrait(Trait.DEVEDOR) }, listOf(
            "Cada salto, a dívida cresce um pouco mais.", "Devo a quem não perdoa.")),
        Rule({ it.standing.heat(it.faction) >= 50 }, listOf(
            "Estão de olho. Sempre estão, mas agora é diferente.", "O manifesto está limpo. Eu não.")),
        Rule({ g -> g.cargo.keys.any { g.isIllegalHere(it) } }, listOf(
            "O porão pesa mais do que as toneladas dizem.", "Um scanner, uma pergunta. É tudo o que separa o lucro da cadeia.")),
        Rule({ it.ledger.lastOrNull()?.text?.contains("racionamento") == true }, listOf(
            "A fila ainda está na minha cabeça.", "Alguns lugares não têm o que vender, só o que perder.")),
        Rule({ it.credits >= 5000 }, listOf(
            "Dinheiro de sobra. O que isso me compra além de mais um salto?", "Já cheguei longe demais para parar por vontade.")),
        Rule({ it.credits < 200 }, listOf(
            "Pouco combustível e menos moedas. Pelo menos não tenho o que perder.", "Recomeçar não é novidade.")),
        Rule({ true }, listOf(
            "Mais um salto. Mais uma porta.", "Comprar, vender, esquecer.", "O espaço é grande. Os poderes são pequenos, mas os dedos são longos.")),
    )

    fun line(g: Game): String {
        val rule = rules.first { it.cond(g) }
        return Rng(derive(g.location.seedKey(), g.day.toLong(), 0x701)).pick(rule.lines)
    }
}
