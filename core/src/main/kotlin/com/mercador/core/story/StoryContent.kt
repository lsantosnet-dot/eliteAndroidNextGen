package com.mercador.core.story

import com.mercador.core.economy.Commodity
import com.mercador.core.game.Game
import com.mercador.core.game.Trait
import com.mercador.core.universe.Economy
import com.mercador.core.universe.Ideology
import com.mercador.core.world.Meters

/**
 * Conteúdo da fatia vertical: storylets curtas, tom sombrio e político (seção 9).
 * Meta do design: 100 a 200. Aqui, o primeiro lote.
 */
object StoryContent {
    private val S = Trigger.SALTO
    private val C = Trigger.CHEGADA
    private fun Game.m(): Meters = world.meters(location)
    private fun Game.carryingBanned() = cargo.keys.any { isIllegalHere(it) }

    /** Primeiro lote; os demais ficam em arquivos próprios (StoryContentLote2...) e se somam em [all]. */
    private val lote1: List<Storylet> = listOf(
        Storylet("mercador-em-apuros", "Mercador em apuros", setOf(S), 10, { it.fuel >= 6 },
            "Um cargueiro à deriva pede combustível pelo canal aberto. O tanque dele está seco; o seu não está cheio, mas dá para repartir.",
            listOf(
                Choice("Dar 4 unidades de combustível", "-4 combustível", { it.fuel >= 5 }) { o ->
                    o.fuel(-4); o.favor(); o.voice("Ninguém vai saber. É por isso que vale.")
                    "Ele agradece em três línguas e promete lembrar de você em {sistema}."
                },
                Choice("Vender o combustível", "+120 cr, reputação cai", { it.fuel >= 5 }) { o ->
                    o.fuel(-4); o.credits(120); o.rep(-2); o.voice("Preço de mercado. Só isso.")
                    "Ele paga sem discutir. Não agradece."
                },
                Choice("Seguir viagem", "") { o ->
                    o.voice("Não era problema meu. Repito até acreditar.")
                    "O sinal dele some atrás de você."
                },
            )),

        Storylet("sinal-estranho", "Sinal estranho", setOf(S), 8, { true },
            "Um sinal sem origem repete a mesma sequência. Não é de nenhuma facção.",
            listOf(
                Choice("Investigar (2 dias)", "pode render, pode custar combustível") { o ->
                    o.days(2)
                    if (o.rng.chance(0.5)) {
                        val n = o.gain(Commodity.RELIQUIAS, 1)
                        if (n > 0) "Entre destroços antigos, uma relíquia intacta. Cabe no porão." else "Havia uma relíquia, mas o porão está cheio."
                    } else { o.fuel(-2); "Era isca. Você perde dois dias e duas unidades de combustível." }
                },
                Choice("Ignorar", "") { o -> o.voice("Nem tudo que chama merece resposta."); "O sinal se apaga sozinho." },
            )),

        Storylet("capsula-de-fuga", "Cápsula de fuga", setOf(S), 5, { true },
            "Uma cápsula de fuga bate no casco. Dentro, uma pessoa de uniforme rasgado, viva.",
            listOf(
                Choice("Resgatar (1 dia)", "ela sabe de coisas") { o ->
                    o.days(1); o.secret(o.rival); o.rep(-1)
                    "Entre um gole de água e outro, ela entrega um segredo sobre {rival}. Pede só que não diga de onde veio."
                },
                Choice("Chamar a patrulha", "") { o -> o.rep(2); o.heat(-3); "A patrulha chega em horas. Você é anotado como cidadão exemplar." },
                Choice("Deixar a cápsula", "") { o -> o.voice("O espaço é grande. Cabe quem ficou."); "Você não olha para trás." },
            )),

        Storylet("naufragio", "Naufrágio", setOf(S), 6, { it.holdFree >= 3 },
            "Os restos de um cargueiro giram devagar. Dá para ver caixas inteiras entre as chapas.",
            listOf(
                Choice("Recolher o que couber", "pode atrair atenção", { it.holdFree >= 3 }) { o ->
                    val c = listOf(Commodity.MINERIO, Commodity.MAQUINAS, Commodity.ELETRONICOS, Commodity.COMIDA)[o.rng.nextInt(4)]
                    val n = o.gain(c, o.rng.range(3, 6))
                    if (o.rng.chance(0.25)) { o.heat(8); "Você leva $n t de ${c.label.lowercase()}, mas uma patrulha viu." }
                    else "Você leva $n t de ${c.label.lowercase()} sem testemunhas."
                },
                Choice("Deixar em paz", "") { o -> o.voice("Havia gente aqui."); "Você segue." },
            )),

        Storylet("eco", "Eco", setOf(S, C), 20, { it.hasTrait(Trait.MARCADO) },
            "Por um instante, a cabine cheira ao lugar onde tudo aconteceu. Não há nada ali.",
            listOf(
                Choice("Respirar fundo", "") { o -> o.voice("Tem nome. Tinha nome."); "Passa. Sempre passa, até voltar." },
            )),

        Storylet("insonia", "Sem dormir", setOf(C), 15, { it.hasTrait(Trait.INSONE) },
            "Você não dorme há tempo demais. Os números do livro de contas dançam quando fecha os olhos.",
            listOf(
                Choice("Descansar 2 dias", "o Insone passa") { o ->
                    o.days(2); o.dropTrait(Trait.INSONE); o.voice("Dormi. Não sei se mereci."); "Você dorme de verdade pela primeira vez em semanas."
                },
                Choice("Seguir assim", "") { o -> "Café, números, mais um salto." },
            )),

        Storylet("fila-do-remedio", "A fila do remédio", setOf(C), 14, { it.m().rationing && it.cargo.containsKey(Commodity.REMEDIO) },
            "Há racionamento em {sistema}. A fila diante da farmácia dobra o quarteirão. Seu porão tem remédio, e todo mundo sabe.",
            listOf(
                Choice("Doar 3 t", "-3 t, reputação e favor", { it.cargo.getOrDefault(Commodity.REMEDIO, 0) >= 3 }) { o ->
                    o.lose(Commodity.REMEDIO, 3); o.meters(discontent = -4, supply = 5); o.rep(4); o.favor()
                    o.ledger("Doou remédio na fila de {sistema} durante o racionamento.")
                    o.voice("Remédio não paga aluguel, mas paga o sono.")
                    "A fila anda. Alguém chora. Ninguém pergunta seu nome, e você agradece."
                },
                Choice("Vender a três vezes o preço", "+400 cr, o povo odeia", { it.cargo.getOrDefault(Commodity.REMEDIO, 0) >= 2 }) { o ->
                    o.lose(Commodity.REMEDIO, 2); o.credits(400); o.meters(discontent = 6); o.rep(-4)
                    o.ledger("Vendeu remédio a três vezes o preço durante o racionamento em {sistema}.")
                    "O comprador paga com a mão trêmula. A fila vê tudo."
                },
                Choice("Passar sem olhar", "") { o -> o.voice("Cada um carrega o que pode."); "Você abre caminho pelo corredor lateral." },
            )),

        Storylet("racionamento-doca", "Racionamento na doca", setOf(C), 12, { it.m().rationing && it.cargo.getOrDefault(Commodity.COMIDA, 0) >= 5 },
            "Na doca de {sistema}, um fiscal de {facção} conta caixas de comida. As suas estão na conta dele.",
            listOf(
                Choice("Entregar 5 t à cooperativa", "-5 t", { it.cargo.getOrDefault(Commodity.COMIDA, 0) >= 5 }) { o ->
                    o.lose(Commodity.COMIDA, 5); o.meters(discontent = -5, supply = 6); o.rep(3)
                    o.ledger("Entregou comida à cooperativa de {sistema}, passando por cima do fiscal.")
                    "O fiscal anota seu nome e não diz nada. Nos corredores, aplaudem baixo."
                },
                Choice("Deixar o fiscal levar 2 t", "", { it.cargo.getOrDefault(Commodity.COMIDA, 0) >= 2 }) { o ->
                    o.lose(Commodity.COMIDA, 2); o.rep(1); o.meters(control = 2)
                    "Você assina o recibo. {facção} agradece por escrito."
                },
                Choice("Fingir que não é com você", "") { o -> "A conta dele fecha sem a sua assinatura. Por ora." },
            )),

        Storylet("carta-do-governador", "A carta do governador", setOf(C), 12, { it.m().control >= 40 && it.location.traits.corruption >= 55 },
            "{governador}, governador de {sistema}, mostra um envelope lacrado. Quer que chegue a {rival}, sem que ninguém na alfândega saiba.",
            listOf(
                Choice("Levar a carta e não perguntar", "+250 cr, favor", { true }) { o ->
                    o.credits(250); o.favor(); o.heat(4)
                    o.ledger("Aceitou levar uma carta lacrada de {governador} para {rival}.")
                    "O envelope pesa menos do que devia. Ou mais."
                },
                Choice("Abrir a carta antes", "obtém um segredo; perde a confiança", { true }) { o ->
                    o.secret(); o.rep(-8)
                    o.ledger("Abriu a carta de {governador} antes de entregá-la.")
                    o.voice("Saber é uma forma de dívida.")
                    "Nomes, valores, uma data. Agora você carrega algo mais perigoso que a própria carta."
                },
                Choice("Recusar", "") { o -> o.rep(-2); "{governador} guarda o envelope sem mudar de expressão." },
            )),

        Storylet("agente-da-junta", "Agente da junta", setOf(C), 10, { it.m().control >= 55 && it.m().discontent >= 45 },
            "Um homem sem insígnia encosta na sua nave em {sistema}. Fala em nome de quem manda. Quer armas, e rápido.",
            listOf(
                Choice("Vender 5 t de armas", "+900 cr, o controle aperta", { it.cargo.getOrDefault(Commodity.ARMAS, 0) >= 5 }) { o ->
                    o.lose(Commodity.ARMAS, 5); o.credits(900); o.meters(control = 8, discontent = 4); o.rep(3)
                    o.ledger("Vendeu armas à junta de {sistema} enquanto o povo passava necessidade.")
                    o.voice("O dinheiro é limpo. Eu é que não.")
                    "O pagamento chega em notas novas. Ele não sorri."
                },
                Choice("Recusar", "reputação cai") { o ->
                    o.rep(-3); o.heat(5); "Ele olha para a nave inteira antes de ir."
                },
            )),

        Storylet("panfletos", "Panfletos", setOf(C), 9, { it.m().discontent >= 50 },
            "Uma pilha de panfletos clandestinos foi esquecida num banco da doca. Falam em greve geral.",
            listOf(
                Choice("Distribuir pela estação", "atenção sobe, o povo se mexe") { o ->
                    o.heat(15); o.meters(discontent = 6, control = -3); o.rep(-3)
                    o.ledger("Distribuiu panfletos clandestinos em {sistema}.")
                    "As mãos pegam o papel sem olhar para você."
                },
                Choice("Entregar à segurança", "reputação sobe") { o ->
                    o.rep(4); o.meters(control = 3); o.voice("É o preço de ficar em paz."); "O sargento de {facção} aperta sua mão."
                },
                Choice("Largar onde estava", "") { o -> "Alguém vai achar. Não será você." },
            )),

        Storylet("epidemia", "Febre de doca", setOf(C), 10, { it.m().supply < 55 && it.location.traits.techLevel <= 6 },
            "Uma febre corre pela parte baixa de {sistema}. A estação está em quarentena parcial, e sua carga foi inspecionada duas vezes.",
            listOf(
                Choice("Fornecer remédio (4 t)", "-4 t", { it.cargo.getOrDefault(Commodity.REMEDIO, 0) >= 4 }) { o ->
                    o.lose(Commodity.REMEDIO, 4); o.meters(supply = 6, discontent = -4); o.rep(5); o.favor()
                    o.ledger("Furou a quarentena de {sistema} com remédio.")
                    "A quarentena abre o suficiente para você passar."
                },
                Choice("Aproveitar para vender caro", "+300 cr", { it.cargo.getOrDefault(Commodity.REMEDIO, 0) >= 2 }) { o ->
                    o.lose(Commodity.REMEDIO, 2); o.credits(300); o.rep(-3); o.meters(discontent = 3)
                    "O dinheiro vem rápido. A doca o reconhece depois."
                },
                Choice("Partir antes do cerco", "-1 combustível") { o -> o.fuel(-1); "Você sai com a selagem ainda meio aberta." },
            )),

        Storylet("fiscal-corrupto", "Fiscal sensível", setOf(C), 12, { it.location.traits.corruption >= 50 && it.carryingBanned() },
            "Um fiscal em {sistema} passa a mão pelo seu manifesto sem ler. Pergunta, quase gentil, se há algo que precise 'ser esquecido'.",
            listOf(
                Choice("Pagar 60 cr", "-atenção", { it.credits >= 60 }) { o ->
                    o.credits(-60); o.heat(-12); o.rep(-1); o.ledger("Pagou um fiscal em {sistema} para esquecer o manifesto."); "O manifesto desaparece."
                },
                Choice("Recusar", "atenção sobe") { o -> o.heat(8); "Ele sorri como quem anota." },
            )),

        Storylet("nome-comprado", "Um nome à venda", setOf(C), 8, { it.credits >= 200 },
            "No bar de {sistema}, alguém que não bebe vende um nome e uma data. Diz que é de {rival}.",
            listOf(
                Choice("Comprar por 150 cr", "obtém um segredo sobre {rival}", { it.credits >= 150 }) { o ->
                    o.credits(-150); o.secret(o.rival); o.voice("Informação é a única mercadoria que não pesa."); "Ele se levanta e some na multidão."
                },
                Choice("Ignorar", "") { o -> "Ele procura outro cliente." },
            )),

        Storylet("cobrador", "O cobrador", setOf(C), 25, { it.hasTrait(Trait.DEVEDOR) && it.debt > 0 },
            "Um cobrador espera ao pé da sua nave. Sabe exatamente quanto você deve.",
            listOf(
                Choice("Pagar 100 cr adiantados", "-100 cr", { it.credits >= 100 }) { o ->
                    o.credits(-100); o.rep(1); "Ele conta as notas duas vezes."
                },
                Choice("Recusar", "ele não vai embora") { o ->
                    o.scene("cobrador"); "Ele para na sua frente."
                },
            )),

        Storylet("sacerdote", "O sacerdote", setOf(C), 8, { it.ideology == Ideology.RELIGIOSA },
            "Um sacerdote de {facção} abençoa as naves que atracam em {sistema}. Pede uma oferta para a sua.",
            listOf(
                Choice("Oferecer 50 cr", "+reputação", { it.credits >= 50 }) { o -> o.credits(-50); o.rep(4); "A bênção é breve e sincera." },
                Choice("Recusar", "") { o -> o.rep(-1); "Ele ainda murmura algo, mesmo assim." },
            )),

        Storylet("sociedade-duvidosa", "Sociedade duvidosa", setOf(C), 9, { it.ideology == Ideology.MERCANTIL && it.credits >= 1200 },
            "A Guilda oferece uma cota num carregamento. A tinta do contrato ainda está úmida. Pede 500 cr.",
            listOf(
                Choice("Entrar por 500 cr", "pode render 800 ou nada", { it.credits >= 500 }) { o ->
                    o.credits(-500)
                    if (o.rng.chance(0.55)) { o.credits(800); "O carregamento chega. A Guilda paga em dia." }
                    else { o.rep(-2); "O carregamento 'se perdeu'. A Guilda lamenta." }
                },
                Choice("Recusar", "") { o -> "A Guilda não insiste. Fica anotado." },
            )),

        Storylet("auditoria", "Auditoria", setOf(C), 12, { it.location.traits.surveillance >= 65 && it.standing.heat(it.faction) >= 30 },
            "Um auditor de {facção} pede para ver os seus livros em {sistema}. Fala em 'inconsistências'.",
            listOf(
                Choice("Pagar uma multa de 80 cr", "atenção cai", { it.credits >= 80 }) { o ->
                    o.credits(-80); o.heat(-20); o.ledger("Pagou multa de auditoria em {sistema}."); "Tudo regularizado. Por enquanto."
                },
                Choice("Recusar", "atenção sobe") { o -> o.heat(10); o.rep(-2); "O auditor fecha a pasta devagar." },
            )),

        Storylet("contrabandista-local", "Oferta no escuro", setOf(C), 9, { it.location.traits.corruption >= 40 && it.holdFree >= 4 },
            "Alguém em {sistema} oferece 4 t de um bem que {facção} proíbe, a preço de queima.",
            listOf(
                Choice("Comprar", "atenção sobe", { it.holdFree >= 4 }) { o ->
                    val banned = Commodity.values().filter { it.bannedBy(o.g.ideology) }
                    val c = banned[o.rng.nextInt(banned.size)]
                    val price = (c.basePrice * 4 * 0.6).toInt()
                    if (o.g.credits >= price) {
                        o.credits(-price); o.gain(c, 4); o.heat(10); o.rep(-2)
                        o.ledger("Comprou 4 t de ${c.label.lowercase()} proibidos em {sistema}.")
                        "O trato se fecha em segundos. O volume pesa mais do que parece."
                    } else "Você não tem o dinheiro. Ele vai embora sem pressa."
                },
                Choice("Recusar", "") { o -> "Ele some entre as docas." },
            )),

        Storylet("caridade", "Obra de caridade", setOf(C), 8, { it.credits >= 800 && it.m().discontent >= 55 },
            "Uma cozinha comunitária de {sistema} pede dinheiro. Todo mundo ali sabe quanto você ganhou na última entrega.",
            listOf(
                Choice("Doar 200 cr", "o povo respira", { it.credits >= 200 }) { o ->
                    o.credits(-200); o.meters(discontent = -5, supply = 3); o.rep(3)
                    o.voice("Dinheiro bem gasto também é dinheiro."); "A cozinha abre mais cedo naquela semana."
                },
                Choice("Guardar o dinheiro", "") { o -> o.voice("Mercador não é santo. Mercador paga o combustível."); "Você finge ter pressa." },
            )),

        Storylet("rosto-conhecido", "Um rosto conhecido", setOf(C), 18, { it.hasTrait(Trait.MARCADO) },
            "Em {sistema}, alguém na doca encara você sem desviar o olhar. Uma mão fecha devagar.",
            listOf(
                Choice("Fingir que não viu", "") { o -> o.voice("Eu sei quem sou. Eles também."); "A pessoa vai embora, mas o olhar fica." },
                Choice("Encarar de volta", "pode virar confronto") { o -> o.scene("parente de uma vítima"); "O silêncio se estica." },
            )),

        Storylet("ordem-de-reduzir", "Pedido de silêncio", setOf(C), 8, { it.location.traits.economy == Economy.TECNOLOGICA && it.m().control >= 50 },
            "Um técnico da estação pede discretamente que você não conte o que viu no setor 4 de {sistema}. Em troca, oferece acesso a um mercado exclusivo.",
            listOf(
                Choice("Aceitar o silêncio", "+150 cr, reputação sobe") { o ->
                    o.credits(150); o.rep(2); o.ledger("Guardou silêncio sobre o setor 4 de {sistema}."); "Vocês nunca estiveram ali."
                },
                Choice("Fazer perguntas", "obtém um segredo; atenção sobe") { o ->
                    o.secret(); o.heat(10); o.voice("Perguntar é gratuito, mas não é de graça."); "O técnico fala mais do que queria."
                },
            )),
    )

    val all: List<Storylet> = lote1 + StoryContentLote2.all + StoryContentLote3.all + StoryContentLote4.all
}
