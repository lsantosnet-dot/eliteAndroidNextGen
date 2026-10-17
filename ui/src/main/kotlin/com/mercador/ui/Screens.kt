package com.mercador.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercador.core.game.*
import com.mercador.core.story.StoryCard
import com.mercador.core.story.Trigger

/** Silhueta da patrulha sobre o brilho vermelho: a mesma arte da perseguição e do confronto. */
@Composable
fun PatrolArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height * .42f)
        drawCircle(Brush.radialGradient(listOf(Noir.red.copy(alpha = .45f), Color.Transparent), c, size.height * .62f), size.height * .62f, c)
        for (r in listOf(50f, 90f, 130f)) drawCircle(Color(0xFF2A1A1C), r * density, c, style = Stroke(1f * density))
        val k = density
        val pts = listOf(-92f to 18f, -30f to -6f, -18f to -34f, 0f to -50f, 18f to -34f, 30f to -6f, 92f to 18f, 40f to 26f, 18f to 52f, 0f to 58f, -18f to 52f, -40f to 26f)
        val path = Path().apply {
            moveTo(c.x + pts[0].first * k, c.y + pts[0].second * k)
            pts.drop(1).forEach { lineTo(c.x + it.first * k, c.y + it.second * k) }
            close()
        }
        drawPath(path, Color(0xFF131012)); drawPath(path, Color(0xFF6B2A26), style = Stroke(1.5f * density))
        drawCircle(Noir.red, 7f * k, Offset(c.x, c.y + 6 * k))
        drawCircle(Noir.salmon, 3f * k, Offset(c.x - 62 * k, c.y + 18 * k)); drawCircle(Noir.salmon, 3f * k, Offset(c.x + 62 * k, c.y + 18 * k))
    }
}

private fun chanceLabel(p: Double) = when { p >= .6 -> "boa chance"; p >= .35 -> "chance incerta"; else -> "chance baixa" }

/** A patrulha alcançou o jogador carregando contrabando: a decisão com custo (seção 7.1). */
@Composable
fun PursuitScreen(game: Game, onRespond: (Response) -> Unit = {}) {
    val p = game.pursuit ?: return
    val f = p.patrol
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0709))) {
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            PatrolArt(Modifier.fillMaxSize())
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                T("PATRULHA DE ${f.name.uppercase()} TE ALCANÇOU", 11f, Noir.red, spacing = 2f, align = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                T("em ${p.system.name} · ${p.evidenceTons} t de carga comprometedora", 10.5f, Noir.muted, Noir.mono, align = TextAlign.Center)
            }
        }
        Card(Modifier.fillMaxWidth().padding(10.dp)) {
            T("“Parada de rotina, mercador. Abra o porão e não terá problema.” Não é rotina, e os dois sabem.", 13.5f, Noir.text, Noir.serif, lineHeight = 19f)
        }
        Column(Modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            val avail = game.responses()
            for (r in Response.values()) {
                val on = r in avail
                val detail = when (r) {
                    Response.SUBORNAR -> "${credits(PursuitRules.bribeCost(p))} · ${chanceLabel(PursuitRules.bribeChance(p))}"
                    Response.JOGAR_FORA -> "perde ${p.evidenceTons} t e a prova some"
                    Response.BLEFAR -> chanceLabel(PursuitRules.bluffChance(p, game.standing.reputation(f))) + " · se falhar, multa e carga perdida"
                    Response.FUGIR -> "${PursuitRules.FLEE_FUEL} de combustível · ${chanceLabel(PursuitRules.fleeChance(p))}"
                    Response.ENTREGAR_SE -> "multa ${credits(PursuitRules.surrenderFine(p))} · perde a carga"
                    Response.CONFRONTAR -> "uma cena, sem tiro · pode custar mais, pode custar menos"
                }
                Column(
                    Modifier.fillMaxWidth().alpha(if (on) 1f else .4f).clip(RoundedCornerShape(8.dp)).background(Noir.panel)
                        .border(1.dp, Noir.line, RoundedCornerShape(8.dp)).clickable(enabled = on) { onRespond(r) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    T(r.label, 14f, Noir.text, weight = FontWeight.Bold)
                    T(detail.replaceFirstChar { it.uppercase() }, 11.5f, Noir.sub)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().background(Noir.panel).padding(10.dp), contentAlignment = Alignment.Center) {
            T("ATENÇÃO DE ${f.name.uppercase()} ${attentionBars(game.standing.heat(f))} · sem pausa", 11f, Noir.muted, Noir.mono, spacing = 1f)
        }
    }
}

/** Storylet: evento curto com escolhas (seção 9). */
@Composable
fun StoryScreen(card: StoryCard, voice: String, onChoose: (Int) -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Noir.bg).padding(horizontal = 14.dp, vertical = 22.dp)) {
        Label(if (card.trigger == Trigger.SALTO) "Em trânsito" else "Ao chegar", Noir.amber)
        Spacer(Modifier.height(6.dp))
        T(card.storylet.title, 26f, Noir.text, Noir.serif)
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth()) { T(card.text, 14.5f, Noir.text, Noir.serif, lineHeight = 21f) }
        Spacer(Modifier.height(8.dp))
        T("“$voice”", 12.5f, Noir.sub, Noir.serif, italic = true, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            card.choices.forEachIndexed { i, c ->
                Column(
                    Modifier.fillMaxWidth().alpha(if (c.available) 1f else .4f).clip(RoundedCornerShape(8.dp)).background(Noir.panel)
                        .border(1.dp, Noir.line, RoundedCornerShape(8.dp)).clickable(enabled = c.available) { onChoose(i) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    T(c.label, 14f, Noir.text, weight = FontWeight.Bold)
                    if (c.hint.isNotBlank()) T(c.hint.replaceFirstChar { it.uppercase() }, 11.5f, Noir.sub)
                }
            }
        }
    }
}

/** Desfecho de uma decisão, em tela cheia, até o jogador continuar. */
@Composable
fun ReportScreen(report: Report, onContinue: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Noir.bg).padding(20.dp), verticalArrangement = Arrangement.Center) {
        Label(report.kicker, Noir.amber)
        Spacer(Modifier.height(14.dp))
        T(report.text, 17f, Noir.text, Noir.serif, lineHeight = 25f)
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Continuar", Modifier.fillMaxWidth(), onContinue)
    }
}

/** Dossiê do mercador: estado, tanque, atenção por facção, favores e segredos. */
@Composable
fun DossierScreen(game: Game, notice: String?, onTab: (Tab) -> Unit = {}, onRefuel: () -> Unit = {}, onLoan: () -> Unit = {}, onFreight: () -> Unit = {}, onRepay: () -> Unit = {}, onSlots: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Noir.bg)) {
        Column(Modifier.fillMaxWidth().background(Noir.panel).padding(16.dp)) {
            T("DOSSIÊ", 22f, Noir.text, Noir.serif, spacing = 2f)
            Spacer(Modifier.height(4.dp))
            T("dia ${game.day} · ${game.location.name}", 12f, Noir.muted, Noir.mono)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row { T("Créditos", 13f, Noir.sub, modifier = Modifier.weight(1f)); T(credits(game.credits), 14f, Noir.amber, Noir.mono) }
                if (game.debt > 0) Row { T("Dívida", 13f, Noir.sub, modifier = Modifier.weight(1f)); T(credits(game.debt), 14f, Noir.red, Noir.mono) }
                Row { T("Combustível", 13f, Noir.sub, modifier = Modifier.weight(1f)); T("${game.fuel} / ${Game.FUEL_CAPACITY}", 14f, Noir.text, Noir.mono) }
                Row { T("Porão", 13f, Noir.sub, modifier = Modifier.weight(1f)); T("${game.holdUsed} / ${Game.HOLD_CAPACITY} t", 14f, Noir.text, Noir.mono) }
            }
            Column {
                Label("No porão"); Spacer(Modifier.height(6.dp))
                if (game.cargo.isEmpty()) T("Vazio.", 12.5f, Noir.sub, Noir.serif, italic = true)
                for ((c, t) in game.cargo) Row {
                    T(c.label + if (game.isIllegalHere(c)) "  · proibido aqui" else "", 13f, if (game.isIllegalHere(c)) Noir.salmon else Noir.text, modifier = Modifier.weight(1f))
                    T("$t t", 13f, Noir.text, Noir.mono)
                }
            }
            Column {
                Label("Atenção, favores e segredos"); Spacer(Modifier.height(8.dp))
                for (f in game.factions.factions) {
                    val color = Noir.faction(f)
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        T(f.name, 12.5f, Noir.text, modifier = Modifier.weight(1.3f))
                        Bar(game.standing.heat(f) / 100f, color, Modifier.weight(1f), 6)
                        T("  ${game.favors(f)}f ${game.secretsAbout(f)}s", 11f, Noir.muted, Noir.mono, modifier = Modifier.width(56.dp))
                    }
                }
                T("f = favores a cobrar · s = segredos", 10f, Noir.muted, Noir.mono)
            }
            if (notice != null) T(notice, 12f, Noir.salmon, italic = true)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlineButton("Reabastecer (${credits((Game.FUEL_CAPACITY - game.fuel) * Game.FUEL_PRICE)})", Modifier.fillMaxWidth(), Noir.amber, onRefuel)
                if (game.debt > 0) OutlineButton("Pagar dívida", Modifier.fillMaxWidth(), Noir.teal, onRepay)
                OutlineButton("Pedir empréstimo de socorro", Modifier.fillMaxWidth(), Noir.sub, onLoan)
                OutlineButton("Pegar frete miúdo (5 dias)", Modifier.fillMaxWidth(), Noir.sub, onFreight)
                OutlineButton("Jogos salvos · novo jogo", Modifier.fillMaxWidth(), Noir.violet, onSlots)
            }
        }
        BottomNav(Tab.DOSSIE, onSelect = onTab)
    }
}
