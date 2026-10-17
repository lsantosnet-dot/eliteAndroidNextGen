package com.mercador.core.story

import com.mercador.core.economy.Commodity
import com.mercador.core.game.Game
import com.mercador.core.game.Trait
import com.mercador.core.universe.Economy
import com.mercador.core.world.Meters

/** Segundo lote de storylets: dívida, trabalho, consciência e pequenos favores entre estranhos. */
internal object StoryContentLote2 {
    private val S = Trigger.SALTO
    private val C = Trigger.CHEGADA
    private fun Game.m(): Meters = world.meters(location)
    private fun Game.tons(c: Commodity) = cargo.getOrDefault(c, 0)

    val all: List<Storylet> = listOf(
        Storylet("leilao-de-sucata", "Leilão de sucata", setOf(C), 9,
            { it.location.traits.economy == Economy.MINERADORA || it.location.traits.economy == Economy.INDUSTRIAL && it.credits >= 300 },
            "Num galpão ao fundo da doca de {sistema}, um leiloeiro vende lotes de sucata industrial sem olhar o que há dentro. Lance mínimo: 200 cr.",
            listOf(
                Choice("Dar o lance (200 cr)", "pode render máquinas, pode ser só lata", { it.credits >= 200 && it.holdFree >= 3 }) { o ->
                    o.credits(-200)
                    if (o.rng.chance(.5)) { val n = o.gain(Commodity.MAQUINAS, o.rng.range(3, 5)); "O lote vem com peças inteiras: $n t de máquinas que ainda ligam." }
                    else "Era lata, só lata. O leiloeiro dá de ombros: ‘é o jogo’."
                },
                Choice("Só olhar", "") { o -> o.voice("Tem coisa que a gente só ganha olhando."); "Você sai do galpão com as mãos limpas." },
            )),

        Storylet("o-contador", "O contador", setOf(C), 9, { it.location.traits.corruption >= 50 && it.credits >= 500 },
            "Um contador de óculos grossos o aborda perto do guichê de {sistema}. Diz que seus números ‘têm história demais’ e que, por 100 cr, a história pode ser reescrita.",
            listOf(
                Choice("Pagar 100 cr", "a atenção cai muito", { it.credits >= 100 }) { o ->
                    o.credits(-100); o.heat(-15); o.rep(-1)
                    o.ledger("Pagou um contador de {sistema} para reescrever o passado dos seus números.")
                    "As colunas passam a fazer sentido. Você tenta não pensar em quem paga a diferença."
                },
                Choice("Recusar", "") { o -> "Ele ajeita os óculos e some entre as bancas." },
            )),

        Storylet("greve-nas-docas", "Greve nas docas", setOf(C), 11, { it.m().discontent >= 55 && it.m().control >= 35 },
            "Há um piquete diante do guindaste principal de {sistema}. Os estivadores pedem que ninguém descarregue. A segurança de {facção} pede o contrário.",
            listOf(
                Choice("Furar o piquete", "reputação sobe, o povo se fecha") { o ->
                    o.rep(3); o.meters(discontent = 5, control = 2)
                    o.ledger("Furou o piquete da doca de {sistema}.")
                    o.voice("Alguém precisa descarregar. Sempre há uma desculpa."); "Os olhares dos estivadores pesam mais que a carga."
                },
                Choice("Parar com eles (2 dias)", "o povo respira, a atenção sobe") { o ->
                    o.days(2); o.heat(6); o.rep(-3); o.meters(discontent = -4)
                    o.ledger("Parou com os estivadores na greve de {sistema}.")
                    "Ao fim do segundo dia, alguém lhe passa uma caneca sem dizer nada."
                },
                Choice("Esperar passar", "") { o -> o.days(1); "Você almoça no fundo da nave e espera a poeira baixar." },
            )),

        Storylet("passageiro-discreto", "Passageiro discreto", setOf(S), 8, { true },
            "Um pedido chega pelo canal aberto: uma pessoa de capa precisa de carona até a próxima estação e paga em dinheiro vivo. Pergunta sobre o destino, nenhuma sobre a carga.",
            listOf(
                Choice("Aceitar (+150 cr)", "pode atrair olhares") { o ->
                    o.credits(150); if (o.rng.chance(.4)) o.heat(8)
                    o.ledger("Levou uma pessoa de capa, sem perguntas, até {sistema}.")
                    "A pessoa desce na doca sem se despedir. O dinheiro, ao menos, é real."
                },
                Choice("Recusar", "") { o -> o.voice("Quem pergunta menos dorme melhor, mas só até certo ponto."); "A conversa termina em estática." },
            )),

        Storylet("livro-de-um-morto", "O livro de um morto", setOf(C), 9, { it.ledger.size >= 3 },
            "Numa banca de {sistema}, um velho vende o diário encadernado de um mercador que morreu devendo. Todos os nomes que importam estão nas últimas páginas.",
            listOf(
                Choice("Comprar por 120 cr", "obtém um segredo", { it.credits >= 120 }) { o ->
                    o.credits(-120); o.secret(o.rival); o.voice("Os mortos escrevem melhor do que os vivos pagam.")
                    "O diário tem um capítulo inteiro sobre {rival}. Você lê sentado na escotilha."
                },
                Choice("Deixar na banca", "") { o -> "O velho guarda o livro como quem guarda uma ferida." },
            )),

        Storylet("arquivista", "A arquivista", setOf(C), 8, { it.location.traits.economy == Economy.TECNOLOGICA && it.credits >= 200 },
            "No subsolo da torre de dados de {sistema}, uma arquivista vende cópias de registros que {facção} jura não ter. Aceita só dinheiro.",
            listOf(
                Choice("Comprar um registro (200 cr)", "obtém um segredo sobre {rival}; a atenção sobe", { it.credits >= 200 }) { o ->
                    o.credits(-200); o.secret(o.rival); o.heat(5)
                    o.ledger("Comprou um registro proibido na torre de dados de {sistema}."); "Os arquivos são frios, completos e perigosos."
                },
                Choice("Só conversar", "reputação sobe um pouco") { o -> o.rep(1); "Ela fala de poeira, de estantes e de quem sabe demais." },
            )),

        Storylet("nave-em-quarentena", "Nave em quarentena", setOf(S), 7, { it.fuel >= 3 },
            "Uma nave pisca o sinal de quarentena. A tripulação, pelo rádio, jura que é só uma avaria no filtro de ar. Ninguém consegue confirmar.",
            listOf(
                Choice("Atracar e ajudar (2 dias, -1 combustível)", "pode custar caro", { it.fuel >= 1 }) { o ->
                    o.days(2); o.fuel(-1)
                    if (o.rng.chance(.3)) { o.credits(-80); "Era mesmo a doença. Você perde dois dias e 80 cr em desinfecção." }
                    else { o.favor(); "Era só o filtro. A tripulação promete nunca esquecer." }
                },
                Choice("Avisar a patrulha", "reputação sobe") { o -> o.rep(2); o.heat(-2); "A patrulha agradece o aviso. O sinal da nave some atrás de você." },
                Choice("Seguir viagem", "") { o -> o.voice("Medo também é uma forma de prudência."); "Você aumenta a velocidade sem olhar o painel." },
            )),

        Storylet("inspetor-aposentado", "O inspetor aposentado", setOf(C), 8, { it.location.traits.surveillance >= 50 },
            "Num bar de {sistema}, um ex-inspetor de {facção} paga uma rodada para quem escuta. Fala sobre a ronda da madrugada como quem conta uma receita.",
            listOf(
                Choice("Pagar uma bebida (40 cr)", "a atenção cai", { it.credits >= 40 }) { o ->
                    o.credits(-40); o.heat(-8); o.voice("Todo sistema tem um buraco no relógio. Basta saber a hora.")
                    "Ele fala do buraco na ronda. Você anota de cabeça."
                },
                Choice("Só escutar", "") { o -> "Você escuta por educação. Ele agradece a companhia, não a bebida." },
            )),

        Storylet("feira-de-reliquias", "Feira de relíquias", setOf(C), 10, { it.tons(Commodity.RELIQUIAS) >= 1 },
            "Em {sistema}, um colecionador reconhece o selo das suas relíquias de longe. Pergunta, polido, o que você pretende fazer com elas.",
            listOf(
                Choice("Vender 1 t por 700 cr", "+700 cr, reputação cai", { it.tons(Commodity.RELIQUIAS) >= 1 }) { o ->
                    o.lose(Commodity.RELIQUIAS, 1); o.credits(700); o.rep(-1)
                    o.ledger("Vendeu uma relíquia a um colecionador de {sistema}."); "Ele paga sem regatear. As mãos tremem na embalagem."
                },
                Choice("Doar ao museu local", "favor e reputação", { it.tons(Commodity.RELIQUIAS) >= 1 }) { o ->
                    o.lose(Commodity.RELIQUIAS, 1); o.rep(5); o.favor()
                    o.ledger("Doou uma relíquia ao museu de {sistema}."); o.voice("Algumas coisas pertencem a todos, ou a ninguém.")
                    "O curador chora um pouco. Você finge não ver."
                },
                Choice("Recusar a oferta", "") { o -> "O colecionador deixa um cartão e uma cortesia desconfiada." },
            )),

        Storylet("oferta-de-escolta", "Oferta de escolta", setOf(S), 7, { it.credits >= 200 },
            "Um cargueiro armado se oferece para escoltá-lo até a próxima estação. O preço é razoável. O sorriso, nem tanto.",
            listOf(
                Choice("Pagar 100 cr", "a atenção cai um pouco", { it.credits >= 100 }) { o ->
                    o.credits(-100); o.heat(-5); "Os dois atravessam o vazio em formação. Ninguém ousa se aproximar."
                },
                Choice("Recusar", "pode custar combustível") { o ->
                    if (o.rng.chance(.3)) { o.fuel(-2); "O cargueiro dá uma volta ameaçadora e vai embora. Você perde combustível na manobra." }
                    else "O cargueiro dá de ombros e some no escuro."
                },
            )),

        Storylet("soldado-desertor", "Soldado desertor", setOf(C), 9, { it.m().control >= 60 },
            "Perto da doca de {sistema}, um soldado de {facção}, sem insígnia, pede passagem escondida. Jura que não machucou ninguém.",
            listOf(
                Choice("Acolher a bordo", "segredo; a atenção sobe", { true }) { o ->
                    o.secret(); o.heat(12); o.rep(-4)
                    o.ledger("Escondeu um soldado desertor de {facção} em {sistema}.")
                    "No porão, o soldado conta o que viu. Você não perguntou, mas agora sabe."
                },
                Choice("Denunciar à guarda", "reputação e favor") { o ->
                    o.rep(4); o.favor(); o.voice("Cada um escolhe de que lado dorme."); "A guarda leva o soldado sem olhar para você."
                },
                Choice("Fingir que não viu", "") { o -> "Você vira o rosto. A multidão fecha o espaço." },
            )),

        Storylet("radio-pirata", "Rádio pirata", setOf(S), 7, { it.m().discontent >= 40 },
            "Uma transmissão sem licença corta o canal de comunicação: uma voz pede greve geral nos sistemas de {facção}. Dá a coordenada exata de um esconderijo.",
            listOf(
                Choice("Ouvir até o fim", "o povo se mexe") { o ->
                    o.meters(discontent = 3); o.voice("Uma voz no escuro basta, às vezes."); "A transmissão corta de repente, no meio de uma frase."
                },
                Choice("Relatar à patrulha", "reputação sobe") { o -> o.rep(3); o.heat(-3); "A patrulha agradece. O silêncio depois pesa mais que o rádio." },
                Choice("Desligar o receptor", "") { o -> "Silêncio. Você aperta o botão com mais força do que devia." },
            )),

        Storylet("brinde-com-inimigo", "Brinde com o inimigo", setOf(C), 8, { it.standing.reputation(it.faction) <= -15 && it.credits >= 100 },
            "Um representante local de {facção} lhe oferece uma bebida. Fala em ‘mal-entendidos’ e em ‘recomeço’. Os olhos são de quem calcula.",
            listOf(
                Choice("Aceitar e pagar 80 cr de desculpas", "a reputação melhora", { it.credits >= 80 }) { o ->
                    o.credits(-80); o.rep(6); o.heat(-6); o.ledger("Brindou com um representante de {facção} em {sistema}."); "Os copos tilintam. Nenhum dos dois acredita, mas ambos fingem."
                },
                Choice("Recusar", "") { o -> o.voice("Orgulho custa caro, e sempre cobra à vista."); "O representante deixa o copo cheio na mesa." },
            )),

        Storylet("peso-errado", "Peso errado", setOf(C), 10, { it.holdUsed >= 8 },
            "O fiscal de {sistema} pesa seu porão e diz que a conta não fecha: falta uma tonelada no manifesto. Ele olha para o relógio, não para os papéis.",
            listOf(
                Choice("Pagar 40 cr de ‘taxa de pesagem’", "", { it.credits >= 40 }) { o -> o.credits(-40); "A conta fecha. O relógio também." },
                Choice("Contestar a balança", "pode virar discussão") { o ->
                    o.heat(4)
                    if (o.rng.chance(.4)) { o.scene("fiscal de pesagem"); "O fiscal levanta a voz." }
                    else "Ele refaz a pesagem com cara de poucos amigos. Dessa vez, fecha."
                },
            )),

        Storylet("crianca-no-porao", "Uma criança no porão", setOf(S), 6, { true },
            "Há um barulho no porão. Atrás de uma caixa, uma criança magra, de roupa grande demais, segura um pedaço de pão duro. Embarcou escondida na última doca.",
            listOf(
                Choice("Entregar à guarda na próxima estação", "reputação sobe") { o ->
                    o.rep(3); o.heat(-3); o.ledger("Entregou uma criança clandestina à guarda de {sistema}.")
                    o.voice("Fiz o que era certo. Era mesmo?"); "A guarda leva a criança pela mão. Ela olha para trás uma vez."
                },
                Choice("Levar a um abrigo (60 cr)", "favor", { it.credits >= 60 }) { o ->
                    o.credits(-60); o.favor(); o.meters(supply = 1)
                    o.ledger("Pagou um abrigo para uma criança que viajava clandestina em seu porão.")
                    "O abrigo tem camas e uma sopa quente. A criança dorme antes de terminar de comer."
                },
                Choice("Deixar na próxima doca e seguir", "") { o ->
                    o.voice("O espaço é grande demais para ficar só."); o.ledger("Deixou uma criança clandestina numa doca de {sistema}, sem ver o que aconteceu depois.")
                    "Você deixa o pão e o último cobertor limpo. Depois, prefere não saber."
                },
            )),

        Storylet("comida-estocada", "Comida estocada", setOf(C), 8, { it.m().rationing && it.holdFree >= 5 && it.credits >= 100 },
            "No fundo de um armazém de {sistema}, alguém abre uma porta e mostra pilhas de comida que o racionamento diz não existir. Pergunta se você leva.",
            listOf(
                Choice("Comprar 5 t (50 cr)", "agrava a escassez", { it.credits >= 50 && it.holdFree >= 5 }) { o ->
                    o.credits(-50); o.gain(Commodity.COMIDA, 5); o.meters(supply = -3, discontent = 2)
                    o.ledger("Comprou comida estocada em pleno racionamento, em {sistema}."); "Você carrega os sacos na penumbra. Ninguém fala."
                },
                Choice("Denunciar à cooperativa", "reputação e favor") { o ->
                    o.rep(4); o.favor(); o.meters(supply = 3, discontent = -2)
                    o.ledger("Entregou à cooperativa de {sistema} o esconderijo de comida estocada."); "No dia seguinte, a fila anda mais rápido."
                },
                Choice("Fingir que não viu", "") { o -> o.voice("Não era comigo. Nunca é."); "O armazenista fecha a porta devagar." },
            )),

        Storylet("o-aprendiz", "O aprendiz", setOf(C), 8, { it.hasTrait(Trait.CALEJADO) },
            "Uma jovem negociante de {sistema} reconhece sua nave pela fama. Pede, sem rodeios, um conselho sobre como sobreviver no ramo.",
            listOf(
                Choice("Ensinar o que sabe", "favor") { o ->
                    o.favor(); o.voice("Passar adiante é a única herança que se mantém."); "Você fala por uma hora. No fim, ela anota tudo."
                },
                Choice("Dizer que desista", "") { o -> o.voice("Eu mesmo não sei por que continuo."); "Ela escuta, agradece, e continua o que estava fazendo." },
            )),

        Storylet("taxa-de-passagem", "Taxa de passagem", setOf(S), 9, { it.credits >= 90 && it.standing.reputation(it.faction) <= -10 },
            "Uma barreira de {facção} fecha o corredor de saltos antes de {sistema}. O oficial de plantão cobra 90 cr de ‘taxa administrativa’. O recibo é manuscrito.",
            listOf(
                Choice("Pagar a taxa", "", { it.credits >= 90 }) { o -> o.credits(-90); o.rep(1); "O oficial carimba e abre a barreira." },
                Choice("Forçar a passagem (-2 combustível)", "a atenção sobe", { it.fuel >= 2 }) { o ->
                    o.fuel(-2); o.heat(10); o.rep(-3); o.ledger("Forçou uma barreira de {facção} antes de {sistema}."); "O alarme toca atrás de você enquanto a nave rasga o corredor."
                },
                Choice("Dar meia-volta", "") { o -> o.days(1); "Você perde um dia contornando a barreira." },
            )),

        Storylet("papeis-falsos", "Papéis impecáveis", setOf(C), 8, { it.location.traits.corruption >= 60 && it.credits >= 300 && it.standing.heat(it.faction) >= 15 },
            "Alguém da burocracia de {sistema} oferece um jogo completo de papéis novos, com selos que só os próprios selos conseguem desmentir.",
            listOf(
                Choice("Comprar por 250 cr", "a atenção cai muito", { it.credits >= 250 }) { o ->
                    o.credits(-250); o.heat(-40); o.rep(-2)
                    o.ledger("Comprou papéis novos em {sistema} para apagar o próprio rastro."); "Seu nome agora é de outra pessoa, com um passado muito mais limpo."
                },
                Choice("Recusar", "") { o -> o.voice("Não vou ser outro. Basta o que já sou."); "O burocrata guarda a pasta sem dizer nada." },
            )),

        Storylet("cantor-de-rua", "O cantor de rua", setOf(C), 6, { it.m().discontent >= 40 },
            "Sob um painel de neon quebrado em {sistema}, alguém canta uma canção antiga sobre o que o povo perdeu. Poucos páram para ouvir, e nenhum se atreve a aplaudir.",
            listOf(
                Choice("Deixar 10 cr no chapéu", "o povo respira um pouco", { it.credits >= 10 }) { o ->
                    o.credits(-10); o.meters(discontent = -1); o.voice("Dez moedas. Uma canção. A conta é justa."); "O cantor agradece só com os olhos."
                },
                Choice("Parar e ouvir inteira", "") { o -> o.voice("Havia uma casa no meio dessa canção."); "A canção termina. A multidão se dispersa em silêncio." },
                Choice("Seguir caminho", "") { o -> "O som ainda o acompanha por dois corredores." },
            )),
    )
}
