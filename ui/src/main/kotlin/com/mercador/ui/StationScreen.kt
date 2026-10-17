package com.mercador.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercador.core.economy.Commodity
import com.mercador.core.economy.Market
import com.mercador.core.economy.PriceReason
import com.mercador.core.economy.Trend
import com.mercador.core.game.Game
import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive
import com.mercador.core.world.MeterModel
import kotlin.math.roundToInt

/** Estação: mercado com consequências (maquete 2). */
@Composable
fun StationScreen(game: Game, onTab: (Tab) -> Unit = {}) {
    val s = game.location
    val color = Noir.faction(s.faction)
    val vigiado = s.traits.surveillance >= 60
    Column(Modifier.fillMaxSize().background(Noir.bg)) {
        Box(Modifier.fillMaxWidth().height(190.dp)) {
            Skyline(s.seedKey(), color)
            Column(Modifier.padding(16.dp)) {
                T("Estação ${s.name}", 26f, Noir.text, Noir.serif)
                Spacer(Modifier.height(4.dp))
                T("${s.faction.name.uppercase()} · ${if (vigiado) "PORTO VIGIADO" else "PORTO LIVRE"}", 11f, color, spacing = 1.5f)
            }
            Column(Modifier.align(Alignment.TopEnd).padding(16.dp), horizontalAlignment = Alignment.End) {
                T(credits(game.credits), 14f, color, Noir.mono)
                Spacer(Modifier.height(4.dp))
                T("DIA ${game.day}", 11f, Noir.muted, Noir.mono)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            listOf("Mercado", "Dossiê", "Bar", "Contratos").forEachIndexed { i, t ->
                Column {
                    T(t.uppercase(), 12f, if (i == 0) Noir.amber else Noir.muted, spacing = 1.5f)
                    if (i == 0) Box(Modifier.padding(top = 5.dp).width(54.dp).height(2.dp).background(Noir.amber))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            T("MERCADORIA", 10f, Noir.muted, spacing = 1.5f, modifier = Modifier.weight(2.1f))
            T("PREÇO", 10f, Noir.muted, spacing = 1.5f, modifier = Modifier.weight(1f))
            T("TENDÊNCIA", 10f, Noir.muted, spacing = 1.5f, modifier = Modifier.weight(1.2f))
            T("ESTOQUE", 10f, Noir.muted, spacing = 1.5f, align = TextAlign.End, modifier = Modifier.weight(1f))
        }
        Column(Modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            val shown = game.world.quotes(s).sortedByDescending { it.illegal || it.commodity in MeterModel.essential }.take(5)
            for (e in shown) MarketRow(game, e.commodity)
        }
        Spacer(Modifier.height(10.dp))
        Hold(game)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp), border = color.copy(alpha = .5f)) {
            Label("Livro de contas · última linha", color)
            Spacer(Modifier.height(4.dp))
            T(game.ledger.lastOrNull()?.text ?: "Nada ainda. O livro espera.", 13f, Noir.text, Noir.serif, italic = true, lineHeight = 18f)
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PrimaryButton("Comprar", Modifier.weight(1f))
            OutlineButton("Vender", Modifier.weight(1f))
        }
        BottomNav(Tab.ESTACAO, onSelect = onTab)
    }
}

@Composable
private fun MarketRow(game: Game, c: Commodity) {
    val s = game.location
    val e = game.world.quote(s, c)
    val prev = Market.entry(s, c, game.day - Market.PERIOD_DAYS).price
    val pct = if (prev > 0) ((e.price - prev) * 100.0 / prev).roundToInt() else 0
    val up = e.trend == Trend.SOBE; val down = e.trend == Trend.CAI
    val tcolor = if (up) Noir.red else if (down) Noir.teal else Noir.muted
    val tag = when {
        e.illegal -> "legal em ${game.factions.factions.firstOrNull { !c.bannedBy(it.ideology) }?.name?.substringAfterLast(' ') ?: "outro lugar"} · lucro alto"
        e.reason == PriceReason.ESCASSO && c in MeterModel.essential -> "essencial · " + (if (game.world.meters(s).rationing) "racionado" else "escasso aqui")
        c in MeterModel.essential -> "essencial · ${e.reason.label}".trimEnd(' ', '·')
        else -> e.reason.label.ifEmpty { s.traits.economy.label.lowercase() }
    }
    val base = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
    Row(
        (if (e.illegal) base.background(Noir.dangerBg).border(1.dp, Noir.dangerLine, RoundedCornerShape(8.dp)) else base.background(Noir.panel))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(2.1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(c.label, 14f, Noir.text)
                if (e.illegal) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF3A1A1A)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                        T("PROIBIDO", 8f, Noir.salmon, spacing = 1f)
                    }
                }
            }
            T(tag, 10.5f, if (e.illegal) Color(0xFFA56A62) else Noir.muted)
        }
        T("${e.price}", 14f, Noir.amber, Noir.mono, modifier = Modifier.weight(1f))
        T((if (up) "▲ +" else if (down) "▼ " else "▬ ") + "$pct%", 12f, tcolor, modifier = Modifier.weight(1.2f))
        T(if (e.stock == 0) "—" else "${e.stock} t", 13f, Noir.text, Noir.mono, align = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Hold(game: Game) {
    val cap = Game.HOLD_CAPACITY.toFloat()
    val illegal = game.evidence()
    val legal = game.cargo.filterKeys { it !in illegal }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Label("Porão ${game.holdUsed} / ${Game.HOLD_CAPACITY} t")
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(Noir.track)) {
            val colors = listOf(Noir.amber, Noir.violet, Noir.teal)
            legal.entries.forEachIndexed { i, (_, t) -> Box(Modifier.weight(t / cap).fillMaxHeight().background(colors[i % colors.size])) }
            if (illegal.isNotEmpty()) Box(Modifier.weight(illegal.values.sum() / cap).fillMaxHeight().background(Noir.red))
            val free = cap - game.holdUsed
            if (free > 0) Spacer(Modifier.weight(free / cap))
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            legal.entries.take(2).forEach { (c, t) -> T("${c.label} $t t", 11f, Noir.text) }
            if (illegal.isNotEmpty()) T("Carga oculta ${illegal.values.sum()} t", 11f, Noir.salmon)
            if (game.cargo.isEmpty()) T("Porão vazio", 11f, Noir.muted)
        }
    }
}

/** Ilustração 2D estilizada da estação: skyline determinística pelo sistema, com sol na cor da facção. */
@Composable
private fun Skyline(seed: Long, color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF14110C), Noir.bg)))
        val sun = Offset(w * .74f, h * .5f)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .35f), Color.Transparent), sun, h * .6f), h * .6f, sun)
        drawCircle(color, 22f * density, sun)
        val rng = Rng(derive(seed, 0x5C7))
        val path = Path().apply {
            moveTo(0f, h)
            var x = 0f; var y = h * .6f
            lineTo(0f, y)
            while (x < w) {
                val bw = (30f + rng.nextInt(50)) * density
                y = h * (.45f + rng.nextDouble().toFloat() * .35f)
                lineTo(x, y); lineTo(minOf(x + bw, w), y)
                x += bw
            }
            lineTo(w, h); close()
        }
        drawPath(path, Color(0xFF0A0C0F))
        drawPath(path, Noir.line, style = androidx.compose.ui.graphics.drawscope.Stroke(1f * density))
        repeat(7) {
            drawRect(color.copy(alpha = .8f), Offset(rng.nextInt(w.toInt()).toFloat(), h * (.6f + rng.nextDouble().toFloat() * .3f)), Size(3f * density, 3f * density))
        }
    }
}
