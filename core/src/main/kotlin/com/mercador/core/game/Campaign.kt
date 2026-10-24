package com.mercador.core.game

/**
 * A campanha (pergunta Q2 do design): uma dívida grande com um agiota e um prazo. Parcelas cumulativas
 * cobradas em marcos apertam a tensão; perder uma manda um cobrador e engorda a dívida. No fim, quitada
 * ou não, um epílogo lê o livro de contas. Depois disso o jogo segue em modo livre.
 */
class Campaign(
    val creditor: Person,
    var total: Int,
    var paid: Int = 0,
    val deadline: Int = DEADLINE,
    /** Quantos marcos já foram conferidos (cumpridos ou não). */
    var checked: Int = 0,
    var missed: Int = 0,
    var outcome: CampaignOutcome? = null,
    var epilogueSeen: Boolean = false,
) {
    val remaining: Int get() = (total - paid).coerceAtLeast(0)
    val over: Boolean get() = outcome != null

    /** Marco i: (dia, fração cumulativa da dívida que tem de estar paga). */
    fun milestone(i: Int): Pair<Int, Double> = (deadline * MILESTONE_AT[i]).toInt() to MILESTONE_SHARE[i]

    /** Próximo marco ainda não conferido: dia e quanto falta pagar até ele; null se acabou. */
    fun next(): Pair<Int, Int>? {
        if (over || checked >= MILESTONE_AT.size) return null
        val (day, share) = milestone(checked)
        return day to (Math.ceil(total * share).toInt() - paid).coerceAtLeast(0)
    }

    companion object {
        const val DEBT = 24_000
        const val DEADLINE = 300
        val MILESTONE_AT = doubleArrayOf(0.25, 0.5, 0.75, 1.0)
        val MILESTONE_SHARE = doubleArrayOf(0.12, 0.35, 0.65, 1.0)
        /** Cada parcela perdida soma esta fração do que falta à dívida. */
        const val PENALTY = 0.10
    }
}

enum class CampaignOutcome { QUITADA, CALOTE }

/** O fim da campanha: um título e parágrafos que leem quem o jogador se tornou. */
class Epilogue(val title: String, val paragraphs: List<String>)

/** Contadores do que o jogador fez, para o epílogo (o livro guarda o texto; isto guarda os números). */
enum class Stat { CONTRATOS_CUMPRIDOS, CONTRATOS_FALHOS, LACRADAS_ENTREGUES, CRISES_SOCORRIDAS, REVOLTAS_PROVOCADAS, ENTREGAS_RACIONAMENTO, ARMAS_A_TIRANOS, MELHORIAS }

object Epilogues {
    fun compose(g: Game): Epilogue? {
        val c = g.campaign ?: return null
        val o = c.outcome ?: return null
        val who = "${c.creditor.name}, ${c.creditor.role} de ${c.creditor.faction.name}"
        val p = ArrayList<String>()
        val title: String
        if (o == CampaignOutcome.QUITADA) {
            title = "Quitado"
            p += "No dia ${g.day}, $who conta o dinheiro duas vezes e devolve o seu nome. A dívida acabou." +
                if (c.missed == 0) " Sem um atraso sequer: nem ele esperava isso." else " Custou ${c.missed} cobrança(s) e mais do que o combinado."
        } else {
            title = "Calote"
            p += "O prazo venceu no dia ${c.deadline} com ${c.remaining} cr ainda devidos a $who. A partir de hoje, o seu nome é uma dívida em todas as estações que ele alcança."
        }
        val s = { st: Stat -> g.stat(st) }
        when {
            g.hasTrait(Trait.MARCADO) -> p += "Você pagou também de outro jeito. Há uma pessoa que não volta, e um nome que você não consegue esquecer."
            s(Stat.CRISES_SOCORRIDAS) + s(Stat.ENTREGAS_RACIONAMENTO) >= 3 ->
                p += "Em ${s(Stat.CRISES_SOCORRIDAS) + s(Stat.ENTREGAS_RACIONAMENTO)} lugares, alguém comeu ou se curou porque você chegou a tempo. Ninguém lá sabe o seu nome. Talvez seja melhor assim."
            s(Stat.ARMAS_A_TIRANOS) >= 2 -> p += "As armas que você vendeu ainda estão em uso. Os governos que você armou agradecem em silêncio; o povo deles, não."
            else -> p += "Você comprou barato e vendeu caro, e o mundo seguiu como se você não estivesse lá. Quase."
        }
        if (s(Stat.LACRADAS_ENTREGUES) > 0) p += "Você levou ${s(Stat.LACRADAS_ENTREGUES)} carga(s) sem perguntar o que eram. Agora sabe. Fez assim mesmo."
        if (s(Stat.REVOLTAS_PROVOCADAS) > 0) p += "Pelo menos ${s(Stat.REVOLTAS_PROVOCADAS)} revolta(s) começaram com um carregamento seu. Os mapas mudaram de cor por sua causa."
        if (s(Stat.CONTRATOS_CUMPRIDOS) >= 5) p += "${s(Stat.CONTRATOS_CUMPRIDOS)} contratos cumpridos: nas estações, falam de você como alguém que entrega."
        val best = g.factions.factions.maxBy { g.standing.reputation(it) }
        val worst = g.factions.factions.minBy { g.standing.reputation(it) }
        if (g.standing.reputation(best) >= 30) p += "${best.name} abre as portas quando você atraca."
        if (g.standing.reputation(worst) <= -30) p += "${worst.name} ainda tem o seu rosto na parede da alfândega."
        p += "A nave tem ${g.holdCapacity} t de porão e ${g.credits} cr no cofre. O livro tem ${g.ledger.size} linhas. Nenhuma pode ser apagada."
        p += "O jogo continua. A dívida não é mais o motivo; descubra qual é."
        return Epilogue(title, p)
    }
}
