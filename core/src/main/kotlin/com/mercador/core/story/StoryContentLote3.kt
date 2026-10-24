package com.mercador.core.story

import com.mercador.core.economy.Commodity
import com.mercador.core.game.Game
import com.mercador.core.game.Trait
import com.mercador.core.universe.Economy
import com.mercador.core.universe.Ideology
import com.mercador.core.world.Meters

/** Terceiro lote: costumes de cada ideologia e economia, e o que a carga que você leva faz com quem a vê. */
internal object StoryContentLote3 {
    private val S = Trigger.SALTO
    private val C = Trigger.CHEGADA
    private fun Game.m(): Meters = world.meters(location)
    private fun Game.tons(c: Commodity) = cargo.getOrDefault(c, 0)
    private fun Game.legalTons() = cargo.entries.filter { !isIllegalHere(it.key) }.sumOf { it.value }

    val all: List<Storylet> = listOf(
        Storylet("colheita-atrasada", "Colheita atrasada", setOf(C), 9,
            { it.location.traits.economy == Economy.AGRICOLA && it.tons(Commodity.COMBUSTIVEL) >= 3 },
            "As colheitadeiras de {sistema} estão paradas por falta de combustível, e a safra apodrece no campo. Uma cooperativa procura quem venda, e paga em comida.",
            listOf(
                Choice("Trocar 3 t de combustível por 4 t de comida", "", { it.tons(Commodity.COMBUSTIVEL) >= 3 }) { o ->
                    o.lose(Commodity.COMBUSTIVEL, 3); val n = o.gain(Commodity.COMIDA, 4); o.meters(supply = 3); o.rep(2)
                    o.ledger("Trocou combustível por comida com uma cooperativa de {sistema}."); "Os motores pegam. Você sai com $n t de comida ainda cheirando a terra."
                },
                Choice("Doar 3 t", "reputação e favor", { it.tons(Commodity.COMBUSTIVEL) >= 3 }) { o ->
                    o.lose(Commodity.COMBUSTIVEL, 3); o.meters(supply = 4, discontent = -2); o.rep(5); o.favor()
                    o.ledger("Doou combustível a uma cooperativa de {sistema} na hora da colheita."); "A cooperativa canta enquanto os motores rodam."
                },
                Choice("Seguir viagem", "") { o -> "Você passa por cima dos campos com a janela fechada." },
            )),

        Storylet("mare-de-conteineres", "Maré de contêineres", setOf(C), 8, { it.location.traits.economy == Economy.PORTUARIA },
            "A doca de {sistema} está entupida de contêineres, e a fila de atracação dá a volta na estação. Um despachante se aproxima com um sorriso pronto.",
            listOf(
                Choice("Pagar 60 cr para furar a fila", "", { it.credits >= 60 }) { o -> o.credits(-60); o.rep(-1); "Em dez minutos você está na doca. Os outros pilotos tomam nota da sua placa." },
                Choice("Esperar na fila (2 dias)", "") { o -> o.days(2); o.rep(1); o.voice("Esperar também é um jeito de pagar."); "A fila anda. Você lê, dorme e anda com ela." },
            )),

        Storylet("cla-em-luto", "Clã em luto", setOf(C), 9, { it.ideology == Ideology.CLANICA },
            "Uma procissão de {facção} bloqueia a avenida principal de {sistema}: um dos mais velhos do clã morreu. Há velas, tambores e uma fila de cabeças baixas.",
            listOf(
                Choice("Acompanhar a procissão (1 dia)", "reputação e favor") { o ->
                    o.days(1); o.rep(4); o.favor(); o.voice("Eu não conheci quem enterram, mas sei o que é perder."); "Você caminha no fim da fila. Ninguém o expulsa."
                },
                Choice("Deixar 30 cr para as velas", "", { it.credits >= 30 }) { o -> o.credits(-30); o.rep(1); "Uma criança aceita as moedas e agradece sem olhar." },
                Choice("Furar a procissão", "reputação cai, a atenção sobe") { o -> o.rep(-5); o.heat(3); o.ledger("Atravessou a procissão fúnebre de {facção} em {sistema}."); "O tambor para. Todos olham para você." },
            )),

        Storylet("edital-da-ordem", "Edital da ordem", setOf(C), 9, { it.ideology == Ideology.AUTORITARIA },
            "Um edital novo cobre a parede do terminal de {sistema}: proíbe reuniões de mais de quatro pessoas depois do toque de recolher. Um guarda vigia quem lê.",
            listOf(
                Choice("Assinar a ciência e seguir as regras", "reputação sobe") { o -> o.rep(2); o.meters(control = 1); o.voice("Regras são uma espécie de abrigo, até que deixam de ser."); "O guarda carimba seu nome e olha para a próxima pessoa." },
                Choice("Arrancar o edital", "a atenção sobe, o povo se mexe") { o ->
                    o.heat(12); o.meters(discontent = 3, control = -2); o.rep(-3)
                    o.ledger("Arrancou um edital de {facção} no terminal de {sistema}."); "O papel rasga em duas tiras. Alguém, atrás, ri baixo."
                },
                Choice("Ler e ir embora", "") { o -> "Você memoriza o número de cada artigo, por reflexo." },
            )),

        Storylet("contrato-de-exclusividade", "Contrato de exclusividade", setOf(C), 8,
            { it.ideology == Ideology.MERCANTIL && it.credits >= 400 },
            "A Guilda em {sistema} estende um contrato: 200 cr agora, em troca da promessa de não negociar com ninguém que {rival} aprove. A letra miúda ocupa três páginas.",
            listOf(
                Choice("Assinar (+200 cr)", "reputação sobe aqui, cai com {rival}") { o ->
                    o.credits(200); o.rep(4); o.g.standing.addReputation(o.rival, -4)
                    o.ledger("Assinou contrato de exclusividade com a Guilda de {sistema}, contra {rival}."); "A caneta é boa. O papel, ainda melhor."
                },
                Choice("Não assinar", "") { o -> "O representante recolhe as páginas, impassível. ‘Volte quando o preço subir’." },
            )),

        Storylet("livros-proibidos", "Livros proibidos", setOf(C), 10, { it.tons(Commodity.LIVROS) >= 1 && it.isIllegalHere(Commodity.LIVROS) },
            "Em {sistema}, onde {facção} proíbe livros demais, uma leitora fareja o seu porão como quem fareja água no deserto. Pergunta baixinho se o que leva é o que ela pensa.",
            listOf(
                Choice("Vender 1 t por 300 cr", "a atenção sobe", { it.tons(Commodity.LIVROS) >= 1 }) { o ->
                    o.lose(Commodity.LIVROS, 1); o.credits(300); o.heat(8); o.ledger("Vendeu livros proibidos em {sistema}."); "O pacote some num carrinho de feira."
                },
                Choice("Doar à biblioteca clandestina", "o povo respira, a atenção sobe muito", { it.tons(Commodity.LIVROS) >= 1 }) { o ->
                    o.lose(Commodity.LIVROS, 1); o.heat(10); o.meters(control = -2, discontent = -1); o.favor()
                    o.ledger("Doou livros a uma biblioteca clandestina de {sistema}."); o.voice("Uma página pode mais que um navio."); "A biblioteca não tem endereço. Tem uma senha, e você a decora."
                },
                Choice("Fingir que não entendeu", "") { o -> "Ela agradece a cortesia com um aceno quase invisível." },
            )),

        Storylet("armas-para-a-milicia", "Armas para a milícia", setOf(C), 9, { it.tons(Commodity.ARMAS) >= 3 && it.m().control < 45 },
            "O governo de {sistema} é fraco, e uma milícia de bairro percebeu. Um comandante sem farda pergunta o preço das armas que você carrega.",
            listOf(
                Choice("Vender 3 t por 1.200 cr", "o controle aperta, o povo teme", { it.tons(Commodity.ARMAS) >= 3 }) { o ->
                    o.lose(Commodity.ARMAS, 3); o.credits(1200); o.meters(control = 6, discontent = 5); o.rep(-2)
                    o.ledger("Armou uma milícia de bairro em {sistema}."); o.voice("Eu nunca vi os rostos de quem vai usar isso."); "O dinheiro vem em notas pequenas e molhadas."
                },
                Choice("Recusar", "") { o -> o.rep(1); "O comandante não insiste. Anota sua placa mesmo assim." },
            )),

        Storylet("festa-de-rua", "Festa de rua", setOf(C), 7, { it.tons(Commodity.DROGAS) >= 1 && !it.isIllegalHere(Commodity.DROGAS) },
            "É noite de festa em {sistema}, e a música vem de todos os lados. Entre dois tambores, alguém lhe pergunta, sem rodeios, se você ‘trouxe algo para animar’.",
            listOf(
                Choice("Vender 2 t por 600 cr", "o povo respira", { it.tons(Commodity.DROGAS) >= 2 }) { o ->
                    o.lose(Commodity.DROGAS, 2); o.credits(600); o.meters(discontent = -2); o.ledger("Vendeu narcóticos numa festa de rua em {sistema}."); "A festa vira madrugada. Você sai com os bolsos cheios e os ouvidos zumbindo."
                },
                Choice("Dizer que não", "") { o -> o.voice("Nem toda festa precisa de mim."); "Você bebe água e vê a dança passar." },
            )),

        Storylet("presente-ao-governador", "Presente ao governador", setOf(C), 8, { it.tons(Commodity.LUXO) >= 1 },
            "{governador} manda um assessor ao seu cais: o governo de {sistema} aprecia ‘gestos de boa vontade’ de quem passa pela estação com bens finos.",
            listOf(
                Choice("Presentear 1 t", "reputação e favor", { it.tons(Commodity.LUXO) >= 1 }) { o ->
                    o.lose(Commodity.LUXO, 1); o.rep(5); o.favor(); o.ledger("Presenteou {governador} com bens finos em {sistema}."); "O assessor sorri. O bilhete de agradecimento chega antes da sua partida."
                },
                Choice("Vender 1 t por 300 cr", "", { it.tons(Commodity.LUXO) >= 1 }) { o -> o.lose(Commodity.LUXO, 1); o.credits(300); o.rep(-1); "O assessor paga em dia. A cortesia, não." },
                Choice("Recusar a visita", "reputação cai") { o -> o.rep(-3); "O assessor assente, mas anota o nome da nave." },
            )),

        Storylet("eletronicos-com-defeito", "Eletrônicos com defeito", setOf(C), 8, { it.tons(Commodity.ELETRONICOS) >= 1 && it.credits >= 100 },
            "Um comerciante de {sistema} diz que um lote que você vendeu antes veio com defeito, e exige reembolso. A nota fiscal que ele mostra é difícil de contestar.",
            listOf(
                Choice("Reembolsar 100 cr", "", { it.credits >= 100 }) { o -> o.credits(-100); o.rep(1); "Ele conta as notas duas vezes e se despede com respeito demais." },
                Choice("Insistir que o lote estava certo", "pode custar atenção") { o ->
                    o.rep(-3); if (o.rng.chance(.5)) o.heat(5)
                    "Ele resmunga e se afasta. Em volta, os outros pilotos acompanham a cena."
                },
            )),

        Storylet("estrela-moribunda", "Estrela moribunda", setOf(S), 7, { true },
            "No ponto de saída do corredor, uma estrela gigante vermelha pulsa pela última vez. A luz inunda a cabine e deixa tudo da cor de brasa.",
            listOf(
                Choice("Parar para ver (1 dia)", "se estiver Insone, descansa") { o ->
                    o.days(1); o.dropTrait(Trait.INSONE); o.voice("Tudo isso já aconteceu mil anos atrás, e eu só estou vendo agora."); "A luz some devagar. Você demora a se lembrar de ligar os motores."
                },
                Choice("Seguir caminho", "") { o -> "O brilho vira ponto no retrovisor." },
            )),

        Storylet("corrida-de-naves", "Corrida de naves", setOf(S), 7, { it.credits >= 100 },
            "Três pilotos locais propõem uma corrida até a próxima estação, com apostas no ar. Pedem 100 cr para entrar no bolão.",
            listOf(
                Choice("Apostar 100 cr", "45% de chance de ganhar 250 cr", { it.credits >= 100 }) { o ->
                    o.credits(-100)
                    if (o.rng.chance(.45)) { o.credits(250); o.ledger("Ganhou uma corrida de naves antes de {sistema}."); "Você cruza a linha com uma nave de diferença e o bolão inteiro nas mãos." }
                    else "Você perde por pouco. Ou por muito: ninguém avisa."
                },
                Choice("Ficar fora", "") { o -> "Os pilotos zombam do seu juízo e somem no escuro." },
            )),

        Storylet("mensagem-na-garrafa", "Mensagem numa baliza velha", setOf(S), 7, { true },
            "Uma baliza de navegação sem manutenção repete um aviso em código militar antigo. O conteúdo, quando decifrado, é um nome, uma data e uma localização.",
            listOf(
                Choice("Recolher os dados (1 dia)", "pode render um segredo") { o ->
                    o.days(1)
                    if (o.rng.chance(.6)) { o.secret(o.rival); "O código revela uma ordem antiga contra {rival}. Alguém, em algum lugar, ainda a obedece." }
                    else "Só ruído e poeira. O dia, pelo menos, foi seu."
                },
                Choice("Deixar a baliza em paz", "") { o -> "A mensagem segue repetindo para ninguém." },
            )),

        Storylet("balsa-de-refugiados", "Balsa de refugiados", setOf(S), 8, { it.tons(Commodity.COMIDA) >= 3 || it.tons(Commodity.AGUA) >= 3 },
            "Uma balsa superlotada pisca um pedido de socorro. As crianças aparecem pelas escotilhas, magras, acenando com um pano branco.",
            listOf(
                Choice("Dar 3 t de comida", "favor e reputação", { it.tons(Commodity.COMIDA) >= 3 }) { o ->
                    o.lose(Commodity.COMIDA, 3); o.favor(); o.rep(3); o.ledger("Deu comida a uma balsa de refugiados antes de {sistema}."); "A balsa se afasta. Os panos brancos balançam até sumirem."
                },
                Choice("Dar 3 t de água", "favor e reputação", { it.tons(Commodity.AGUA) >= 3 }) { o ->
                    o.lose(Commodity.AGUA, 3); o.favor(); o.rep(3); o.ledger("Deu água a uma balsa de refugiados antes de {sistema}."); "Uma mão pequena aperta o vidro da escotilha em agradecimento."
                },
                Choice("Seguir viagem", "") { o -> o.voice("Não cabia mais ninguém no meu porão. Era isso que eu repetia."); "O pedido de socorro some atrás de você." },
            )),

        Storylet("alfandega-fantasma", "Alfândega fantasma", setOf(S), 8, { it.credits >= 70 },
            "Um posto de alfândega surge do nada no corredor de saltos. A bandeira é difícil de identificar, e o fiscal fala com pressa demais.",
            listOf(
                Choice("Pagar 70 cr", "", { it.credits >= 70 }) { o -> o.credits(-70); "O ‘fiscal’ sorri e abre o corredor. A estação desaparece em minutos." },
                Choice("Fugir (-1 combustível)", "pode dar errado", { it.fuel >= 1 }) { o ->
                    o.fuel(-1)
                    if (o.rng.chance(.5)) { o.fuel(-2); "Você escapa, mas um tiro de aviso pega o tanque. Perde combustível." } else "O posto fica para trás. Ninguém o persegue."
                },
            )),

        Storylet("o-credor", "O credor", setOf(C), 18, { it.debt > 0 && it.credits >= 300 },
            "O credor pessoal de {sistema} sabe de sua dívida e de seu dinheiro. Propõe, com cortesia perigosa, quitar tudo à vista, ‘para o bem de todos’.",
            listOf(
                Choice("Quitar agora", "", { it.credits >= 300 }) { o -> o.g.repay(); o.ledger("Quitou à vista a dívida que devia em {sistema}."); o.voice("Livre. Por um tempo, pelo menos."); "O credor aperta sua mão e some. A dívida some com ele." },
                Choice("Adiar", "") { o -> "Ele sorri e anota a data. A conversa, claramente, não acabou." },
            )),

        Storylet("rival-impaciente", "Rival impaciente", setOf(C), 15, { it.hasTrait(Trait.FRIO) },
            "Um comerciante de {sistema}, com mais orgulho que juízo, o interpela no meio da doca. Diz que sua fama de ‘frio’ não passa de lenda, e fala alto para a plateia ouvir.",
            listOf(
                Choice("Ignorar o desafio", "") { o -> o.voice("Orgulho barato não paga combustível."); "Você passa direto. Alguns riem; outros repensam." },
                Choice("Encarar até ele recuar", "pode virar cena") { o -> o.scene("comerciante rival"); "O silêncio na doca é de novo seu." },
            )),

        Storylet("caixa-plantada", "Uma caixa que não é sua", setOf(C), 9, { it.holdUsed >= 10 && it.credits >= 100 },
            "A inspeção de {sistema} encontra uma caixa que você não carregou. O lacre é novo, o conteúdo é proibido, e o fiscal parece ter esperado exatamente por isso.",
            listOf(
                Choice("Pagar 100 cr para ‘esclarecer’", "", { it.credits >= 100 }) { o -> o.credits(-100); o.rep(-1); "A caixa some do inventário. O fiscal, também." },
                Choice("Protestar com provas", "pode virar atrito") { o ->
                    if (o.rng.chance(.5)) { o.heat(10); o.rep(-2); "O fiscal anota a reclamação como ‘obstrução’." } else { o.rep(1); "O supervisor chega, olha o lacre e manda o fiscal embora." }
                },
            )),

        Storylet("dia-de-feira", "Dia de feira", setOf(C), 8, { it.location.traits.economy == Economy.COMERCIAL && it.m().supply >= 55 && it.legalTons() >= 2 },
            "Em {sistema}, é dia de feira geral. As bancas se estendem pelo cais, e os compradores pagam mais por quem chega com mercadoria honesta.",
            listOf(
                Choice("Vender 2 t com bônus de feira", "preço 30% acima") { o ->
                    val c = o.g.cargo.entries.filter { !o.g.isIllegalHere(it.key) && it.value >= 2 }.maxByOrNull { it.key.basePrice }?.key
                    if (c != null) { o.lose(c, 2); val v = (c.basePrice * 2 * 1.3).toInt(); o.credits(v); o.meters(supply = -1); "Duas toneladas de ${c.label.lowercase()} saem em minutos por $v cr." }
                    else { o.credits(60); "Você faz alguns negócios miúdos e sai com 60 cr a mais." }
                },
                Choice("Só passear pelas bancas", "") { o -> o.voice("Quando foi a última vez que olhei sem comprar?"); "Você volta à nave com um doce na mão e o orçamento intacto." },
            )),

        Storylet("nome-da-nave", "O nome da nave", setOf(C), 6, { true },
            "O cartório da estação de {sistema} exige o registro do nome da nave. O escrivão espera, caneta em riste, um nome que ficará nos livros para sempre.",
            listOf(
                Choice("‘Cúmplice’", "o livro guarda o nome") { o -> o.ledger("Registrou a nave com o nome ‘Cúmplice’ em {sistema}."); o.voice("Cada nome tem um preço."); "O escrivão escreve devagar. A tinta demora a secar." },
                Choice("‘Sem Nome’", "a atenção cai um pouco") { o -> o.heat(-2); o.ledger("Registrou a nave como ‘Sem Nome’ em {sistema}."); "O escrivão levanta uma sobrancelha e aceita." },
                Choice("‘Última Parada’", "") { o -> o.ledger("Registrou a nave com o nome ‘Última Parada’ em {sistema}."); o.voice("Isso era para ser uma piada."); "O escrivão não ri. Registra em silêncio." },
            )),
    )
}
