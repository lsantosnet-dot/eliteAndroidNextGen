package com.mercador.core.story

import com.mercador.core.economy.Commodity
import com.mercador.core.game.ContractKind
import com.mercador.core.game.Game
import com.mercador.core.game.Trait
import com.mercador.core.game.Upgrade
import com.mercador.core.world.CrisisKind

/**
 * Quarto lote: o mundo vivo. Crises, revoltas, a carga lacrada, a nave e o agiota da campanha entram nas
 * histórias, e o trânsito (salto) ganha mais encontros para que cada viagem traga alguma coisa.
 */
internal object StoryContentLote4 {
    private val S = Trigger.SALTO
    private val C = Trigger.CHEGADA
    private fun Game.crisis() = world.crisisAt(location)
    private fun Game.hasSealed() = contracts.any { it.kind == ContractKind.LACRADA }
    private fun Game.tons(c: Commodity) = ownTons(c)
    private fun Game.owing() = campaign?.let { !it.over && it.remaining > 0 } == true

    val all: List<Storylet> = listOf(
        Storylet("fila-da-fome", "A fila", setOf(C), 14,
            { it.crisis()?.kind == CrisisKind.FOME && (it.tons(Commodity.COMIDA) >= 2 || it.tons(Commodity.AGUA) >= 2) },
            "A fome em {sistema} já tem fila própria: ela começa na doca e some de vista nos corredores. Alguém reconhece a sua nave como cargueiro e a fila inteira se vira para você.",
            listOf(
                Choice("Distribuir 2 t de graça", "reputação, e a fila te conhece", { it.tons(Commodity.COMIDA) >= 2 || it.tons(Commodity.AGUA) >= 2 }) { o ->
                    val c = if (o.has(Commodity.COMIDA, 2)) Commodity.COMIDA else Commodity.AGUA
                    o.lose(c, 2); o.meters(discontent = -4, supply = 3); o.rep(4)
                    o.ledger("Distribuiu ${c.label.lowercase()} de graça na fila da fome em {sistema}.")
                    o.voice("Dois minutos de herói. A fila continua.")
                    "As caixas somem em minutos. A fila não diminui, mas alguém lá no fundo come hoje."
                },
                Choice("Vender ali mesmo, a preço de fome", "+lucro, a fila não esquece") { o ->
                    o.credits(160); o.rep(-3); o.meters(discontent = 3)
                    o.voice("Oferta e procura. Diz isso olhando para eles.")
                    "Pagam com o que têm: moedas, anéis, um relógio parado. Rende 160 cr."
                },
                Choice("Fechar a escotilha", "") { o -> o.voice("Não dá para alimentar um sistema inteiro. Dá?"); "As batidas no casco param quando você desliga as luzes." },
            )),

        Storylet("quarentena", "Quarentena", setOf(C), 12, { it.crisis()?.kind == CrisisKind.EPIDEMIA },
            "{sistema} está em quarentena. Um médico sem dormir há dias pede que você leve amostras de sangue ao laboratório mais próximo. Não paga nada, e é arriscado.",
            listOf(
                Choice("Levar as amostras (2 dias)", "reputação e favor") { o ->
                    o.days(2); o.rep(5); o.favor()
                    if (o.rng.chance(0.25)) { o.trait(Trait.INSONE, 30); "Você entrega as amostras. Passa duas noites medindo a própria febre." }
                    else "As amostras chegam. Semanas depois, um nome de vacina com o nome de {sistema}."
                },
                Choice("Recusar", "") { o -> o.voice("Medo também é uma decisão."); "O médico assente, como quem já ouviu muitos nãos." },
            )),

        Storylet("bloqueio-de-passagem", "Bloqueio", setOf(C), 11, { it.crisis()?.kind == CrisisKind.BLOQUEIO },
            "Naves de {facção} fecham as rotas de {sistema}. Um oficial avisa que só passa quem paga o pedágio de guerra, ou quem conhece alguém.",
            listOf(
                Choice("Pagar o pedágio", "-150 cr", { it.credits >= 150 }) { o -> o.credits(-150); "Você paga. O recibo tem o brasão de {facção} e nenhum valor legal." },
                Choice("Cobrar um favor", "gasta um favor", { it.favors(it.faction) > 0 }) { o ->
                    o.g.standing.addHeat(o.faction, -5); o.g.useFavor(o.faction); "Um nome dito baixinho, e as naves abrem caminho."
                },
                Choice("Furar o bloqueio", "atenção sobe muito") { o ->
                    o.heat(20); o.fuel(-2)
                    if (o.rng.chance(0.5)) "Você mergulha entre duas fragatas com o motor no vermelho. Passa. Eles anotaram a placa."
                    else { o.days(1); "Te pegam na saída. Um dia preso numa doca de inspeção, e uma ficha nova em {facção}." }
                },
            )),

        Storylet("refugiados-de-guerra", "Refugiados", setOf(S), 10, { g -> g.world.crises.any { it.kind == CrisisKind.GUERRA } && g.holdFree >= 2 },
            "Uma nave de refugiados pede abrigo. Fogem da guerra que já está no jornal. São doze, com crianças, e o porão tem espaço.",
            listOf(
                Choice("Levá-los a bordo", "reputação, e um dia a mais", { it.holdFree >= 2 }) { o ->
                    o.days(1); o.rep(3); o.secret(o.rival)
                    o.ledger("Levou refugiados de guerra no porão até {sistema}.")
                    "Na chegada, um deles, ex-oficial, te conta o que viu do lado de {rival}. Vale mais que o frete."
                },
                Choice("Dar combustível e seguir", "-3 combustível", { it.fuel >= 4 }) { o -> o.fuel(-3); o.voice("Não é o bastante. É o que eu tinha."); "Eles agradecem. Ninguém sabe se chegam." },
                Choice("Seguir viagem", "") { o -> o.voice("Doze. Eu contei."); "O sinal deles fica para trás." },
            )),

        Storylet("governo-novo", "Governo novo", setOf(C), 16,
            { g -> g.news.any { it.systemName == g.location.name && it.text.startsWith("Revolta") && g.day - it.day <= 40 } },
            "{sistema} acaba de trocar de dono. Os cartazes de {facção} ainda têm cola fresca, e os antigos fiscais procuram emprego, ou vingança.",
            listOf(
                Choice("Oferecer serviços ao novo governo", "reputação com {facção}") { o -> o.rep(6); o.g.standing.addReputation(o.rival, -4); "O novo intendente anota o seu nome numa lista curta. Por ora, uma lista boa." },
                Choice("Comprar os segredos de um fiscal deposto", "-120 cr, um segredo", { it.credits >= 120 }) { o -> o.credits(-120); o.secret(); "Por 120 cr ele te conta onde o novo governo guarda os esqueletos que herdou." },
                Choice("Não se meter", "") { o -> o.voice("Revoluções têm memória curta e listas longas."); "Você faz o seu comércio e não olha os cartazes." },
            )),

        Storylet("carga-que-se-mexe", "A carga se mexe", setOf(S), 13, { it.hasSealed() },
            "No meio do salto, um barulho no porão. Uma das caixas lacradas tem o lacre rompido por dentro. Algo ali respira, ou é o casco esfriando.",
            listOf(
                Choice("Abrir a caixa", "vai saber o que leva") { o ->
                    val c = o.g.contracts.firstOrNull { it.kind == ContractKind.LACRADA }
                    o.voice("Agora eu sei. Não dá para desaprender.")
                    if (c == null) "A caixa está vazia. Alguém já descarregou o que havia ali, e você nunca vai saber o quê."
                    else "Você levanta a tampa: ${c.commodity.label.lowercase()}, ${c.tons} t. O barulho era o casco. A culpa é outra coisa."
                },
                Choice("Não abrir", "") { o -> o.voice("O que eu não sei não pesa. Pesa sim."); "Você aumenta a música da cabine até o salto terminar." },
            )),

        Storylet("mecanico-clandestino", "Mecânico clandestino", setOf(C), 9,
            { it.location.traits.corruption >= 50 && it.credits >= 900 && Upgrade.values().any { u -> it.level(u) < u.maxLevel } },
            "Num hangar sem placa de {sistema}, uma mecânica oferece instalar peças \"que caíram de um cargueiro\". Metade do preço de estaleiro, nenhuma garantia.",
            listOf(
                Choice("Pagar 900 cr", "uma melhoria, talvez", { it.credits >= 900 }) { o ->
                    o.credits(-900)
                    val u = Upgrade.values().filter { o.g.level(it) < it.maxLevel }.let { it[o.rng.nextInt(it.size)] }
                    if (o.rng.chance(0.75)) { o.g.grantUpgrade(u); o.ledger("Instalou peças roubadas num hangar de {sistema}."); "Funciona. Você ganhou ${u.label.lowercase()}, e não pergunta de quem era." }
                    else { o.heat(10); "A peça era rastreada. Antes de funcionar, já avisou {facção} onde você está." }
                },
                Choice("Recusar", "") { o -> "Ela dá de ombros e volta a desmontar um motor que não é de ninguém." },
            )),

        Storylet("recado-do-agiota", "Recado do agiota", setOf(S, C), 12, { it.owing() && it.campaign!!.next()?.let { (d, falta) -> falta > 0 && d - it.day in 0..20 } == true },
            "Uma mensagem sem remetente chega no canal privado. Só uma foto: a sua nave atracada, tirada de perto. Embaixo, um número: o que falta da próxima parcela.",
            listOf(
                Choice("Responder que vai pagar", "") { o -> o.voice("Promessa a agiota é outra forma de dívida."); "A resposta vem em segundos: \"sabemos\"." },
                Choice("Mandar o agiota esperar", "atenção, e um aviso", { it.hasFame() }) { o ->
                    o.g.adjustDebt(-300); o.trait(Trait.INSONE, 20)
                    "Você manda uma foto da sua própria arma. Ele desconta 300 cr \"pelo bom humor\". Você não dorme direito."
                },
            )),

        Storylet("proposta-do-agiota", "Outro jeito de pagar", setOf(C), 8, { it.owing() && it.holdFree >= 4 },
            "Um homem de {facção} senta à sua mesa no bar e fala em nome do seu credor. Há um jeito de abater a dívida sem dinheiro: levar uma encomenda que ninguém mais leva.",
            listOf(
                Choice("Aceitar: 4 t de narcóticos (abate 1.500 cr)", "a carga é proibida em muitos lugares", { it.holdFree >= 4 }) { o ->
                    val n = o.gain(Commodity.DROGAS, 4); o.g.adjustDebt(-1_500)
                    o.ledger("Pagou parte da dívida levando narcóticos para o agiota.")
                    "Ele empurra $n t para o seu porão antes de você terminar o copo. A dívida encolhe 1.500 cr. A sua ficha vai crescer."
                },
                Choice("Recusar", "") { o -> o.voice("Ainda escolho o que carrego. Por enquanto."); "Ele sorri como quem tem tempo. E tem." },
            )),

        Storylet("comboio-em-rota", "Comboio", setOf(S), 9, { it.world.crises.isNotEmpty() },
            "Um comboio de mercadores cruza a sua rota rumo à crise mais recente do jornal. O capitão oferece lugar na formação: mais lento, mais seguro.",
            listOf(
                Choice("Entrar no comboio (+1 dia)", "atenção cai, e você fica sabendo de coisas") { o ->
                    o.days(1); o.heat(-8)
                    val cr = o.g.world.crises.firstOrNull() ?: return@Choice "O rádio do comboio só fala de preços velhos. Pelo menos ninguém te abordou."
                    "No rádio do comboio só se fala de ${cr.kind.label.lowercase()} em ${cr.systemName}. Falta ${cr.kind.wants.joinToString(" e ") { it.label.lowercase() }}, e paga-se bem."
                },
                Choice("Seguir sozinho", "") { o -> o.voice("Comboios são para quem tem a quem esperar."); "Você acelera e o comboio vira um ponto no radar." },
            )),
    )
}
