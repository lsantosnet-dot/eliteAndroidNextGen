package com.mercador.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercador.core.game.Confrontation
import com.mercador.core.game.Game
import com.mercador.core.game.Posture
import com.mercador.core.universe.Doctrine

/** Confronto: sem tiro, só pressão (maquete 3). */
@Composable
fun ConfrontationScreen(game: Game, scene: Confrontation, voice: String, onPosture: (Posture) -> Unit = {}) {
    val f = scene.opponent.faction
    val fc = Noir.faction(f)
    val danger = Noir.red
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0709))) {
        Box(Modifier.fillMaxWidth().height(196.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height * .42f)
                drawCircle(Brush.radialGradient(listOf(danger.copy(alpha = .45f), Color.Transparent), c, size.height * .62f), size.height * .62f, c)
                for (r in listOf(50f, 90f, 130f)) drawCircle(Color(0xFF2A1A1C), r * density, c, style = Stroke(1f * density))
                val k = density
                val path = Path().apply {
                    fun p(x: Float, y: Float) = Offset(c.x + x * k, c.y + y * k)
                    val pts = listOf(-92f to 18f, -30f to -6f, -18f to -34f, 0f to -50f, 18f to -34f, 30f to -6f, 92f to 18f, 40f to 26f, 18f to 52f, 0f to 58f, -18f to 52f, -40f to 26f)
                    moveTo(p(pts[0].first, pts[0].second).x, p(pts[0].first, pts[0].second).y)
                    pts.drop(1).forEach { lineTo(p(it.first, it.second).x, p(it.first, it.second).y) }
                    close()
                }
                drawPath(path, Color(0xFF131012)); drawPath(path, Color(0xFF6B2A26), style = Stroke(1.5f * density))
                drawCircle(danger, 7f * k, Offset(c.x, c.y + 6 * k))
                drawCircle(Noir.salmon, 3f * k, Offset(c.x - 62 * k, c.y + 18 * k)); drawCircle(Noir.salmon, 3f * k, Offset(c.x + 62 * k, c.y + 18 * k))
            }
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                T("PATRULHA DE ${f.name.uppercase()} · ${scene.opponent.role.uppercase()}", 11f, danger, spacing = 2.2f, align = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                T("${scene.opponent.name} · doutrina: ${doctrineLine(scene.doctrine)}", 10f, Noir.muted, Noir.mono, align = TextAlign.Center)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Meter("Seu sangue-frio", scene.playerCool / 100f, Noir.teal, Modifier.weight(1f))
                Meter("Deles", scene.opponentCool / 100f, danger, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Meter("Sua alavancagem", scene.playerLeverage / 100f, Noir.amber, Modifier.weight(1f))
                Meter("Alavancagem deles", scene.opponentLeverage / 100f, danger, Modifier.weight(1f))
            }
            T("RODADA ${minOf(scene.round + 1, scene.maxRounds)} / ${scene.maxRounds}", 10f, Noir.muted, Noir.mono, align = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
        Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
            T(scene.log.lastOrNull() ?: openingLine(scene), 13.5f, Noir.text, Noir.serif, lineHeight = 19f)
            Spacer(Modifier.height(8.dp))
            T(voice, 12f, Noir.sub, Noir.serif, italic = true, lineHeight = 17f)
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (p in game.postures()) PostureRow(p, onPosture)
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
        Box(Modifier.fillMaxWidth().background(Noir.panel).padding(10.dp), contentAlignment = Alignment.Center) {
            T("ATENÇÃO DE ${f.name.uppercase()} ${attentionBars(game.standing.heat(f))} · sem pausa", 11f, Noir.muted, Noir.mono, spacing = 1f)
        }
    }
}

private fun doctrineLine(d: Doctrine) = when (d) {
    Doctrine.BRUTAL -> "ordem acima da lei"
    Doctrine.MERCANTIL -> "tudo tem um preço"
    Doctrine.BUROCRATICA -> "o formulário vence"
    Doctrine.COBRADORA -> "toda dívida se paga"
}

private fun openingLine(s: Confrontation) = when (s.doctrine) {
    Doctrine.BRUTAL -> "“Abra o porão, mercador. O Regime não precisa de prova — só de motivo.”"
    Doctrine.MERCANTIL -> "“Podemos resolver isto como gente civilizada. Tudo depende do número que você disser.”"
    Doctrine.BUROCRATICA -> "“Seu manifesto está incompleto. Há um procedimento para isso, e ele é longo.”"
    Doctrine.COBRADORA -> "“Alguém aqui lembra de quem lhe deve favores. Hoje a conta chega.”"
}

@Composable
private fun PostureRow(p: Posture, onPosture: (Posture) -> Unit) {
    val violent = p == Posture.VIOLENCIA
    val title = if (violent) Noir.salmon else Noir.text
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(if (violent) Noir.dangerBg else Noir.panel)
            .border(1.dp, if (violent) Noir.dangerLine else Noir.line, RoundedCornerShape(8.dp))
            .clickable { onPosture(p) }.padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        T(p.label, 14f, title, weight = FontWeight.Bold)
        T(p.cost.replaceFirstChar { it.uppercase() }, 11.5f, if (violent) Color(0xFFA56A62) else Noir.sub)
    }
}
