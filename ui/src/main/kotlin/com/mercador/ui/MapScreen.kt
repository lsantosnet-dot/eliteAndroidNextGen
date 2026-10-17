package com.mercador.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mercador.core.game.Game
import com.mercador.core.game.PursuitRules
import com.mercador.core.universe.StarSystem

/** Mapa da galáxia: a mesa de inteligência (maquete 1). */
@Composable
fun MapScreen(game: Game, selected: StarSystem, onSelect: (StarSystem) -> Unit = {}, onTab: (Tab) -> Unit = {}) {
    val region = game.world.region
    val factions = game.factions.factions
    Column(Modifier.fillMaxSize().background(Noir.bg)) {
        Column(Modifier.fillMaxWidth().background(Noir.panel).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                T("GALÁXIA ${game.world.region.galaxy} · ${game.location.name.uppercase()}", 16f, Noir.text, Noir.serif, spacing = 1.5f, modifier = Modifier.weight(1f))
                T(credits(game.credits), 15f, Noir.amber, Noir.mono)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                T("DIA ${game.day} · ATENÇÃO ${attentionBars(game.standing.heat(game.faction))}", 11f, Noir.muted, Noir.mono, modifier = Modifier.weight(1f))
                T("COMB ${game.fuel} / ${Game.FUEL_CAPACITY}", 11f, Noir.muted, Noir.mono)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
        val measurer = rememberTextMeasurer()
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Canvas(Modifier.fillMaxSize()) {
                val pad = 36f * density
                val xs = region.systems.map { it.x }; val ys = region.systems.map { it.y }
                val minX = xs.min().toFloat(); val maxX = xs.max().toFloat(); val minY = ys.min().toFloat(); val maxY = ys.max().toFloat()
                fun pos(s: StarSystem) = Offset(
                    pad + (s.x - minX) / (maxX - minX).coerceAtLeast(1f) * (size.width - 2 * pad),
                    pad + 24f * density + (s.y - minY) / (maxY - minY).coerceAtLeast(1f) * (size.height - 2 * pad - 24f * density),
                )
                // grade
                val step = 60f * density
                var gx = 0f; while (gx < size.width) { drawLine(Noir.line.copy(alpha = .35f), Offset(gx, 0f), Offset(gx, size.height)); gx += step }
                var gy = 0f; while (gy < size.height) { drawLine(Noir.line.copy(alpha = .35f), Offset(0f, gy), Offset(size.width, gy)); gy += step }
                // influência: manchas de cor por facção
                for (f in factions) {
                    val mine = region.systems.filter { it.faction.id == f.id }
                    if (mine.isEmpty()) continue
                    val c = Offset(mine.map { pos(it).x }.average().toFloat(), mine.map { pos(it).y }.average().toFloat())
                    drawCircle(Brush.radialGradient(listOf(Noir.faction(f).copy(alpha = .28f), Color.Transparent), c, 150f * density), 150f * density, c)
                }
                // rotas: vizinhos ao alcance
                for (s in region.systems) for (n in region.neighbors(s, 8.0).take(2)) {
                    drawLine(Noir.muted.copy(alpha = .35f), pos(s), pos(n), 1f * density)
                }
                // zona interditada: facção que o trata como inimigo
                for (f in factions.filter { game.standing.isRestricted(it) }) {
                    val mine = region.systems.filter { it.faction.id == f.id }
                    if (mine.isNotEmpty()) {
                        val c = Offset(mine.map { pos(it).x }.average().toFloat(), mine.map { pos(it).y }.average().toFloat())
                        drawCircle(Noir.red, 55f * density, c, style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 5f * density))))
                        drawText(measurer, "ZONA INTERDITADA", c + Offset(-60f * density, -72f * density), TextStyle(Noir.red, 10.sp, letterSpacing = 3.sp))
                    }
                }
                // rota planejada
                if (selected !== game.location) drawLine(Noir.amber, pos(game.location), pos(selected), 2f * density,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * density, 5f * density)))
                // sistemas
                for (s in region.systems) {
                    val p = pos(s); val col = Noir.faction(s.faction)
                    val here = s === game.location; val sel = s === selected
                    drawCircle(col, (if (here || sel) 5f else 3.5f) * density, p)
                    if (here || sel) drawCircle(if (here) Noir.text else Noir.amber, 9f * density, p, style = Stroke(2f * density))
                    drawText(measurer, s.name, p + Offset(10f * density, -8f * density),
                        TextStyle(if (sel) Noir.amber else Noir.text.copy(alpha = .8f), 11.sp, fontStyle = FontStyle.Normal))
                }
                // legenda
                var lx = 14f * density
                for (f in factions) {
                    drawCircle(Noir.faction(f), 4f * density, Offset(lx, 18f * density))
                    val r = measurer.measure(f.name, TextStyle(Noir.muted, 10.sp))
                    drawText(r, topLeft = Offset(lx + 9f * density, 11f * density))
                    lx += 9f * density + r.size.width + 14f * density
                }
            }
        }
        SystemCard(game, selected)
        BottomNav(Tab.MAPA, onSelect = onTab)
    }
}

@Composable
private fun SystemCard(game: Game, s: StarSystem) {
    val m = game.world.meters(s)
    val color = Noir.faction(s.faction)
    val cost = if (s === game.location) 0 else game.fuelCost(s)
    val chance = PursuitRules.encounterChance(game.standing.heat(s.faction), s.traits.surveillance,
        game.cargo.keys.any { it.bannedBy(s.traits.ideology) }, game.standing.isRestricted(s.faction))
    val risk = when { chance < .15 -> "baixo"; chance < .35 -> "médio"; else -> "alto" }
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(color))
        Card(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(s.name, 24f, Noir.text, Noir.serif, modifier = Modifier.weight(1f))
                T(s.faction.name.uppercase(), 11f, color, spacing = 1.5f)
            }
            Spacer(Modifier.height(4.dp))
            T("${s.traits.economy.label} · TL ${s.traits.techLevel} · ${s.traits.population} mi · corrupção ${s.traits.corruption}", 11f, Noir.muted, Noir.mono)
            Spacer(Modifier.height(10.dp))
            Row {
                Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Meter("Controle", m.control / 100f, Noir.amber, labelWidth = 70)
                    Meter("Descont.", m.discontent / 100f, Noir.red, labelWidth = 70)
                    Meter("Abastec.", m.supply / 100f, Noir.teal, labelWidth = 70)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    T(if (cost == 0) "Você está aqui" else "Salto: $cost LY · ${game.travelDays(s)} dias", 11f, Noir.text)
                    T("Risco de rota: $risk", 11f, Noir.text)
                    if (m.unrest) T("⚠ À beira da revolta", 11f, Noir.red) else if (m.rationing) T("⚠ Racionamento", 11f, Noir.red)
                }
            }
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Planejar salto", Modifier.fillMaxWidth())
        }
    }
}
