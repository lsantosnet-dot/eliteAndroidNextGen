package com.mercador.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
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
import com.mercador.core.universe.StarSystem

/** Mapa da galáxia: a mesa de inteligência (maquete 1). */
@Composable
fun MapScreen(game: Game, selected: StarSystem, onSelect: (StarSystem) -> Unit = {}, onTab: (Tab) -> Unit = {}, onTravel: () -> Unit = {}, notice: String? = null) {
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
                T("COMB ${game.fuel} / ${game.fuelCapacity}", 11f, Noir.muted, Noir.mono)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
        val measurer = rememberTextMeasurer()
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Canvas(Modifier.fillMaxSize().clipToBounds().pointerInput(region) {
                detectTapGestures { tap ->
                    val layout = mapLayout(region.systems, size.width.toFloat(), size.height.toFloat(), density)
                    layout.minByOrNull { (_, p) -> (p - tap).getDistance() }?.takeIf { (_, p) -> (p - tap).getDistance() < 28f * density }?.let { onSelect(it.key) }
                }
            }) {
                val layout = mapLayout(region.systems, size.width, size.height, density)
                fun pos(s: StarSystem) = layout.getValue(s)
                // grade
                val step = 60f * density
                var gx = 0f; while (gx < size.width) { drawLine(Noir.line.copy(alpha = .35f), Offset(gx, 0f), Offset(gx, size.height)); gx += step }
                var gy = 0f; while (gy < size.height) { drawLine(Noir.line.copy(alpha = .35f), Offset(0f, gy), Offset(size.width, gy)); gy += step }
                // influência: manchas de cor por facção
                for (f in factions) {
                    val mine = region.systems.filter { game.world.controller(it).id == f.id }
                    if (mine.isEmpty()) continue
                    val c = Offset(mine.map { pos(it).x }.average().toFloat(), mine.map { pos(it).y }.average().toFloat())
                    drawCircle(Brush.radialGradient(listOf(Noir.faction(f).copy(alpha = .28f), Color.Transparent), c, 150f * density), 150f * density, c)
                }
                // rotas: vizinhos ao alcance
                for (s in region.systems) for (n in region.neighbors(s, 8.0).take(1)) {
                    drawLine(Noir.muted.copy(alpha = .35f), pos(s), pos(n), 1f * density)
                }
                // zona interditada: facção que o trata como inimigo
                for (f in factions.filter { game.standing.isRestricted(it) }) {
                    val mine = region.systems.filter { game.world.controller(it).id == f.id }
                    if (mine.isNotEmpty()) {
                        val c = Offset(mine.map { pos(it).x }.average().toFloat(), mine.map { pos(it).y }.average().toFloat())
                        drawCircle(Noir.red, 55f * density, c, style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 5f * density))))
                        drawText(measurer.measure("ZONA INTERDITADA", TextStyle(Noir.red, 10.sp, letterSpacing = 3.sp)), topLeft = c + Offset(-60f * density, -72f * density))
                    }
                }
                // crises: anel pulsante vermelho e o nome da crise
                for (cr in game.world.crises) {
                    val sys = region.systems.firstOrNull { it.id == cr.system } ?: continue
                    val p = pos(sys)
                    drawCircle(Noir.red.copy(alpha = .18f), 16f * density, p)
                    drawCircle(Noir.red, 13f * density, p, style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * density, 3f * density))))
                    drawText(measurer.measure(cr.kind.label.uppercase(), TextStyle(Noir.salmon, 8.sp, letterSpacing = 1.sp)), topLeft = p + Offset(-14f * density, 14f * density))
                }
                // destinos de contrato: losango âmbar
                for (k in game.contracts) {
                    val sys = region.systems.firstOrNull { it.id == k.destination } ?: continue
                    val p = pos(sys); val r = 8f * density
                    val path = androidx.compose.ui.graphics.Path().apply { moveTo(p.x, p.y - r); lineTo(p.x + r, p.y); lineTo(p.x, p.y + r); lineTo(p.x - r, p.y); close() }
                    drawPath(path, Noir.amber, style = Stroke(1.5f * density))
                }
                // rota planejada
                if (selected !== game.location) drawLine(Noir.amber, pos(game.location), pos(selected), 2f * density,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * density, 5f * density)))
                // sistemas; rótulos só onde cabem (aqui, selecionado e vizinhos afastados), para não empilhar texto
                val labeled = ArrayList<Offset>()
                val ordered = region.systems.sortedByDescending { it === game.location || it === selected }
                for (s in ordered) {
                    val p = pos(s); val col = Noir.faction(game.world.controller(s))
                    val here = s === game.location; val sel = s === selected
                    drawCircle(col, (if (here || sel) 5f else 3.5f) * density, p)
                    if (here || sel) drawCircle(if (here) Noir.text else Noir.amber, 9f * density, p, style = Stroke(2f * density))
                    val free = labeled.none { (it - p).getDistance() < 46f * density }
                    if (here || sel || free) {
                        labeled += p
                        drawText(measurer.measure(s.name, TextStyle(if (sel) Noir.amber else Noir.text.copy(alpha = .8f), 11.sp, fontStyle = FontStyle.Normal)),
                            topLeft = p + Offset(10f * density, -8f * density))
                    }
                }
                // legenda
                var lx = 14f * density
                var ly = 18f * density
                for (f in factions) {
                    val r = measurer.measure(f.name, TextStyle(Noir.muted, 10.sp))
                    if (lx + 9f * density + r.size.width > size.width - 8f * density) { lx = 14f * density; ly += 16f * density }
                    drawCircle(Noir.faction(f), 4f * density, Offset(lx, ly))
                    drawText(r, topLeft = Offset(lx + 9f * density, ly - 7f * density))
                    lx += 9f * density + r.size.width + 14f * density
                }
            }
        }
        SystemCard(game, selected, onTravel, notice)
        BottomNav(Tab.MAPA, onSelect = onTab)
    }
}

@Composable
private fun SystemCard(game: Game, s: StarSystem, onTravel: () -> Unit, notice: String?) {
    val m = game.world.meters(s)
    val ruler = game.world.controller(s)
    val color = Noir.faction(ruler)
    val cost = if (s === game.location) 0 else game.fuelCost(s)
    val chance = game.encounterChance(s)
    val crisis = game.world.crisisAt(s)
    val risk = when { chance < .15 -> "baixo"; chance < .35 -> "médio"; else -> "alto" }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(color))
        Card(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(s.name, 24f, Noir.text, Noir.serif, modifier = Modifier.weight(1f))
                T(ruler.name.uppercase(), 11f, color, spacing = 1.5f)
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
                    if (crisis != null) T("⚠ ${crisis.kind.label}: ${crisis.daysLeft(game.day)} dias", 11f, Noir.red)
                    else if (m.nearRevolt) T("⚠ À beira da revolta", 11f, Noir.red) else if (m.rationing) T("⚠ Racionamento", 11f, Noir.red)
                }
            }
            Spacer(Modifier.height(8.dp))
            Intel(game, s)
            Spacer(Modifier.height(10.dp))
            if (notice != null) { T(notice, 11.5f, Noir.salmon, italic = true, modifier = Modifier.padding(bottom = 6.dp)) }
            val can = s !== game.location && cost <= game.fuel
            if (can) PrimaryButton("Decolar rumo a ${s.name}", Modifier.fillMaxWidth(), onTravel)
            else OutlineButton(if (s === game.location) "Você está aqui" else "Combustível insuficiente", Modifier.fillMaxWidth(), Noir.muted, onTravel)
        }
    }
}

/**
 * O que o jogador sabe do mercado de [s]: venda estimada da carga, a melhor compra daqui para lá e contratos
 * a receber. Sem preços conhecidos, diz como consegui-los.
 */
@Composable
private fun Intel(game: Game, s: StarSystem) {
    val est = game.estimateSale(s)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (est == null) {
            T("Preços de lá: desconhecidos. Visite ou compre um rumor no bar.", 11f, Noir.muted, italic = true)
        } else {
            val age = game.day - est.seenDay
            val seen = if (s === game.location) "preços de hoje" else if (age == 0) "preços vistos hoje" else "preços de $age dia(s) atrás"
            if (est.revenue > 0) {
                val sign = if (est.profit >= 0) "+" else ""
                T("Sua carga lá: ${credits(est.revenue)} ($sign${credits(est.profit)}) · $seen", 11f, if (est.profit >= 0) Noir.teal else Noir.salmon)
            } else T("Carga para vender lá: nenhuma · $seen", 11f, Noir.muted)
            if (est.bannedTons > 0) T("${est.bannedTons} t da sua carga são proibidas lá", 11f, Noir.salmon)
            game.bestRun(s)?.let { (c, gain) -> T("Melhor compra daqui: ${c.label.lowercase()}, +$gain cr/t lá", 11f, Noir.amber) }
        }
        val pay = game.contracts.filter { it.destination == s.id }
        if (pay.isNotEmpty()) T("◆ ${pay.size} contrato(s) para cá: ${credits(pay.sumOf { it.reward })}", 11f, Noir.amber)
    }
}

/** Posição de cada sistema no canvas; o mesmo cálculo desenha o mapa e resolve o toque. */
internal fun mapLayout(systems: List<StarSystem>, w: Float, h: Float, density: Float): Map<StarSystem, Offset> {
    val pad = 36f * density
    val minX = systems.minOf { it.x }.toFloat(); val maxX = systems.maxOf { it.x }.toFloat()
    val minY = systems.minOf { it.y }.toFloat(); val maxY = systems.maxOf { it.y }.toFloat()
    return systems.associateWith { s ->
        Offset(
            pad + (s.x - minX) / (maxX - minX).coerceAtLeast(1f) * (w - 2 * pad),
            pad + 36f * density + (s.y - minY) / (maxY - minY).coerceAtLeast(1f) * (h - 2 * pad - 36f * density),
        )
    }
}
