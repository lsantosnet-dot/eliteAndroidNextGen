package com.mercador.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.mercador.core.game.Bar
import com.mercador.core.game.BarOffer
import com.mercador.core.game.Contract
import com.mercador.core.game.ContractKind
import com.mercador.core.game.Contracts
import com.mercador.core.game.Game
import com.mercador.core.game.Upgrade
import com.mercador.core.rng.Rng
import com.mercador.core.rng.derive
import com.mercador.core.world.MeterModel
import kotlin.math.roundToInt

/** Estado de interação da estação (vem do [GameController]; os padrões servem às pré-visualizações). */
class StationUi(
    val tab: StationTab = StationTab.MERCADO,
    val selected: Commodity? = null,
    val quantity: Int = 1,
    val notice: String? = null,
    val rumor: String? = null,
    val onTab: (StationTab) -> Unit = {},
    val onSelect: (Commodity) -> Unit = {},
    val onQuantity: () -> Unit = {},
    val onBuy: () -> Unit = {},
    val onSell: () -> Unit = {},
    val onRumor: (BarOffer) -> Unit = {},
    val onAccept: (Contract) -> Unit = {},
    val onDeliver: (Long) -> Unit = {},
    val onAbandon: (Long) -> Unit = {},
    val onUpgrade: (Upgrade) -> Unit = {},
)

/** Estação: mercado com consequências (maquete 2). */
@Composable
fun StationScreen(game: Game, onTab: (Tab) -> Unit = {}, ui: StationUi = StationUi()) {
    val s = game.location
    val ruler = game.faction
    val color = Noir.faction(ruler)
    val vigiado = s.traits.surveillance >= 60
    val crisis = game.world.crisisAt(s)
    Column(Modifier.fillMaxSize().background(Noir.bg)) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            Skyline(s.seedKey(), color)
            Column(Modifier.padding(16.dp)) {
                T("Estação ${s.name}", 26f, Noir.text, Noir.serif)
                Spacer(Modifier.height(4.dp))
                T("${ruler.name.uppercase()} · ${if (vigiado) "PORTO VIGIADO" else "PORTO LIVRE"}", 11f, color, spacing = 1.5f)
                crisis?.let {
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Noir.dangerBg).border(1.dp, Noir.dangerLine, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        T("${it.kind.label.uppercase()} · ${it.daysLeft(game.day)} DIAS" + if (it.need > 0) " · FALTAM ${it.need} T" else "", 10f, Noir.salmon, spacing = 1f)
                    }
                }
            }
            Column(Modifier.align(Alignment.TopEnd).padding(16.dp), horizontalAlignment = Alignment.End) {
                T(credits(game.credits), 14f, color, Noir.mono)
                Spacer(Modifier.height(4.dp))
                T("DIA ${game.day}", 11f, Noir.muted, Noir.mono)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            for (t in StationTab.values()) Column(Modifier.clickable { ui.onTab(t) }) {
                val badge = if (t == StationTab.CONTRATOS && game.contracts.any { game.canDeliver(it) }) " •" else ""
                T(t.label.uppercase() + badge, 12f, if (t == ui.tab) Noir.amber else Noir.muted, spacing = 1.2f)
                if (t == ui.tab) Box(Modifier.padding(top = 5.dp).width(54.dp).height(2.dp).background(Noir.amber))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (ui.tab) {
                StationTab.MERCADO -> MarketBody(game, ui)
                StationTab.CONTRATOS -> ContractsBody(game, ui)
                StationTab.ESTALEIRO -> ShipyardBody(game, ui)
                StationTab.SISTEMA -> SystemBody(game)
                StationTab.BAR -> BarBody(game, ui)
            }
        }
        if (ui.tab == StationTab.MERCADO) {
            ui.notice?.let { T(it, 12f, Noir.amber, italic = true, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Noir.line, RoundedCornerShape(8.dp)).clickable { ui.onQuantity() }, contentAlignment = Alignment.Center) {
                    T("×${ui.quantity}", 13f, Noir.text, Noir.mono)
                }
                PrimaryButton("Comprar", Modifier.weight(1f), ui.onBuy)
                OutlineButton("Vender", Modifier.weight(1f), onClick = ui.onSell)
            }
        } else ui.notice?.let { T(it, 12f, Noir.amber, italic = true, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
        BottomNav(Tab.ESTACAO, onSelect = onTab)
    }
}

@Composable
private fun MarketBody(game: Game, ui: StationUi) {
    val s = game.location
    val color = Noir.faction(game.faction)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        T("MERCADORIA", 10f, Noir.muted, spacing = 1.5f, modifier = Modifier.weight(2.1f))
        T("PREÇO", 10f, Noir.muted, spacing = 1.5f, modifier = Modifier.weight(1f))
        T("TENDÊNCIA", 10f, Noir.muted, spacing = 1.5f, modifier = Modifier.weight(1.2f))
        T("ESTOQUE", 10f, Noir.muted, spacing = 1.5f, align = TextAlign.End, modifier = Modifier.weight(1f))
    }
    Column(Modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val shown = game.world.quotes(s).sortedByDescending { it.illegal || it.commodity in MeterModel.essential }
        for (e in shown) MarketRow(game, e.commodity, e.commodity == ui.selected) { ui.onSelect(e.commodity) }
    }
    Spacer(Modifier.height(10.dp))
    Hold(game)
    Spacer(Modifier.height(8.dp))
    Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(bottom = 8.dp), border = color.copy(alpha = .5f)) {
        Label("Livro de contas · última linha", color)
        Spacer(Modifier.height(4.dp))
        T(game.ledger.lastOrNull()?.text ?: "Nada ainda. O livro espera.", 13f, Noir.text, Noir.serif, italic = true, lineHeight = 18f)
    }
}

@Composable
private fun SystemBody(game: Game) {
    val s = game.location
    val t = s.traits
    val m = game.world.meters(s)
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        T("${t.economy.label} · tecnologia ${t.techLevel} · ${t.population} mi de habitantes", 12f, Noir.muted, Noir.mono)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Meter("Controle", m.control / 100f, Noir.amber, labelWidth = 80)
            Meter("Descont.", m.discontent / 100f, Noir.red, labelWidth = 80)
            Meter("Abastec.", m.supply / 100f, Noir.teal, labelWidth = 80)
            Meter("Corrupção", t.corruption / 100f, Noir.violet, labelWidth = 80)
            Meter("Vigilância", t.surveillance / 100f, Noir.salmon, labelWidth = 80)
        }
        Column {
            Label("Bens proibidos aqui", Noir.salmon)
            Spacer(Modifier.height(4.dp))
            T(com.mercador.core.economy.Laws.banned(game.ideology).joinToString(" · ") { it.label }, 13f, Noir.text, Noir.serif)
        }
        if (game.faction.id != s.faction.id) T("Desde a revolta, governado por ${game.faction.name} (antes, ${s.faction.name}).", 12f, Noir.salmon, Noir.serif, italic = true)
        Column {
            Label("Relações da ${game.faction.name}")
            Spacer(Modifier.height(6.dp))
            for (o in game.factions.factions.filter { it.id != game.faction.id }) {
                val r = game.factions.relation(game.faction.id, o.id)
                Row { T(o.name, 12.5f, Noir.faction(o), modifier = Modifier.weight(1f)); T(if (r > 20) "aliada" else if (r < -20) "hostil" else "neutra", 12f, Noir.muted) }
            }
        }
    }
}

@Composable
private fun BarBody(game: Game, ui: StationUi) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        T("Informação é mercadoria. No bar, quem fala cobra.", 12.5f, Noir.sub, Noir.serif, italic = true)
        for (o in Bar.offers(game)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Noir.panel).border(1.dp, Noir.line, RoundedCornerShape(8.dp))
                    .clickable { ui.onRumor(o) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    T(o.kind.label, 14f, Noir.text, weight = FontWeight.Bold)
                    o.about?.let { T(it.name, 11.5f, Noir.faction(it)) }
                }
                T(credits(o.price), 13f, Noir.amber, Noir.mono)
            }
        }
        ui.rumor?.let {
            Card(Modifier.fillMaxWidth(), border = Noir.amber.copy(alpha = .5f)) { T("“$it”", 13.5f, Noir.text, Noir.serif, italic = true, lineHeight = 19f) }
        }
    }
}

@Composable
private fun ContractsBody(game: Game, ui: StationUi) {
    Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (game.contracts.isNotEmpty()) {
            Label("Em aberto (${game.contracts.size} / ${Contracts.MAX_ACTIVE})", Noir.amber, Modifier.padding(horizontal = 6.dp))
            for (k in game.contracts) ContractCard(game, k, active = true, ui = ui)
        }
        Label("Ofertas desta estação", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        val offers = game.offers()
        if (offers.isEmpty()) T("Nenhuma oferta agora. Volte em alguns dias.", 12.5f, Noir.sub, Noir.serif, italic = true, modifier = Modifier.padding(horizontal = 6.dp))
        for (k in offers) ContractCard(game, k, active = false, ui = ui)
    }
}

@Composable
private fun ContractCard(game: Game, k: Contract, active: Boolean, ui: StationUi) {
    val sealed = k.kind == ContractKind.LACRADA
    val left = k.daysLeft(game.day)
    val dest = game.world.region.systems.firstOrNull { it.id == k.destination }
    val dist = dest?.let { "%.1f".format(game.world.region.distanceLy(game.location, it)) + " LY" } ?: ""
    val kindColor = when (k.kind) { ContractKind.LACRADA -> Noir.salmon; ContractKind.SOCORRO -> Noir.teal; ContractKind.ENTREGA -> Noir.amber }
    Card(Modifier.fillMaxWidth(), border = if (sealed) Noir.dangerLine else Noir.line, fill = if (sealed) Noir.dangerBg else Noir.panel) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(k.kind.label.uppercase(), 10f, kindColor, spacing = 1.5f, modifier = Modifier.weight(1f))
            T(credits(k.reward), 14f, Noir.amber, Noir.mono)
        }
        Spacer(Modifier.height(4.dp))
        T("${k.tons} t de ${k.cargoLabel} para ${k.destName}", 14f, Noir.text, weight = FontWeight.Bold)
        T("$dist · ${if (active) "faltam $left dia(s)" else "prazo: dia ${k.deadline} ($left dias)"} · ${k.clientName}", 11f, if (active && left <= 3) Noir.salmon else Noir.muted, Noir.mono)
        val hint = when (k.kind) {
            ContractKind.ENTREGA -> if (active) "você tem ${game.ownTons(k.commodity)} t; compre o resto onde for barato" else "compre a carga você mesmo; o contrato paga mais que o mercado"
            ContractKind.SOCORRO -> "encurta a crise de lá e rende favor"
            ContractKind.LACRADA -> "sem perguntas: a carga vem agora, ocupa o porão e pode ser proibida no caminho"
        }
        T(hint, 11.5f, Noir.sub, Noir.serif, italic = true)
        Spacer(Modifier.height(8.dp))
        if (active) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (game.canDeliver(k)) PrimaryButton("Entregar", Modifier.weight(1f)) { ui.onDeliver(k.id) }
            OutlineButton("Desistir", Modifier.weight(1f), Noir.sub) { ui.onAbandon(k.id) }
        } else OutlineButton("Aceitar", Modifier.fillMaxWidth()) { ui.onAccept(k) }
    }
}

@Composable
private fun ShipyardBody(game: Game, ui: StationUi) {
    Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val here = game.shipyard()
        T(if (here.isEmpty()) "Esta estação não tem estaleiro nem mercado negro." else "O dinheiro compra o que vem depois do próximo salto.",
            12.5f, Noir.sub, Noir.serif, italic = true, modifier = Modifier.padding(horizontal = 6.dp))
        for ((u, price) in here) {
            val lvl = game.level(u)
            Card(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T(u.label, 14f, Noir.text, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    T("nível $lvl / ${u.maxLevel}", 11f, Noir.muted, Noir.mono)
                }
                T(u.effect, 11.5f, Noir.sub)
                Spacer(Modifier.height(8.dp))
                if (price == null) OutlineButton("No máximo", Modifier.fillMaxWidth(), Noir.muted)
                else if (price <= game.credits) PrimaryButton("Instalar · ${credits(price)}", Modifier.fillMaxWidth()) { ui.onUpgrade(u) }
                else OutlineButton("Instalar · ${credits(price)}", Modifier.fillMaxWidth(), Noir.muted) { ui.onUpgrade(u) }
            }
        }
        val elsewhere = Upgrade.values().filter { u -> here.none { it.first == u } }
        if (elsewhere.isNotEmpty()) {
            Label("Em outros lugares", modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
            for (u in elsewhere) Row(Modifier.padding(horizontal = 6.dp)) {
                Column(Modifier.weight(1f)) {
                    T(u.label + " · nível ${game.level(u)} / ${u.maxLevel}", 12.5f, Noir.text)
                    T(u.where, 11f, Noir.muted)
                }
            }
        }
    }
}

@Composable
private fun MarketRow(game: Game, c: Commodity, selected: Boolean = false, onClick: () -> Unit = {}) {
    val s = game.location
    val e = game.world.quote(s, c)
    val prev = Market.entry(s, c, game.day - Market.PERIOD_DAYS, game.ideology).price
    val pct = if (prev > 0) ((e.price - prev) * 100.0 / prev).roundToInt() else 0
    val up = e.trend == Trend.SOBE; val down = e.trend == Trend.CAI
    val tcolor = if (up) Noir.red else if (down) Noir.teal else Noir.muted
    val crisis = game.world.crisisAt(s)
    val tag = when {
        crisis != null && c in crisis.kind.wants -> "crise: ${crisis.kind.label.lowercase()} · paga ×${"%.1f".format(crisis.kind.mult)}"
        crisis != null && c in crisis.kind.surplus -> "sobra: ${crisis.kind.label.lowercase()} · barato"
        e.illegal -> "legal em ${game.factions.factions.firstOrNull { !c.bannedBy(it.ideology) }?.name?.substringAfterLast(' ') ?: "outro lugar"} · lucro alto"
        e.reason == PriceReason.ESCASSO && c in MeterModel.essential -> "essencial · " + (if (game.world.meters(s).rationing) "racionado" else "escasso aqui")
        c in MeterModel.essential -> "essencial · ${e.reason.label}".trimEnd(' ', '·')
        else -> e.reason.label.ifEmpty { s.traits.economy.label.lowercase() }
    }
    val base = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClick() }
    Row(
        (if (e.illegal) base.background(Noir.dangerBg).border(1.dp, if (selected) Noir.amber else Noir.dangerLine, RoundedCornerShape(8.dp))
         else base.background(Noir.panel).border(1.dp, if (selected) Noir.amber else Color.Transparent, RoundedCornerShape(8.dp)))
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
    val cap = game.holdCapacity.toFloat()
    val own = game.cargo.keys.associateWith { game.ownTons(it) }.filterValues { it > 0 }
    val illegal = own.filterKeys { game.isIllegalHere(it) }
    val legal = own.filterKeys { it !in illegal }
    val sealed = game.sealedTons
    Column(Modifier.padding(horizontal = 16.dp)) {
        Label("Porão ${game.holdUsed} / ${game.holdCapacity} t" + if (game.hiddenCapacity > 0) " · ${game.hiddenCapacity} t ocultas" else "")
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(Noir.track)) {
            val colors = listOf(Noir.amber, Noir.violet, Noir.teal)
            legal.entries.forEachIndexed { i, (_, t) -> Box(Modifier.weight(t / cap).fillMaxHeight().background(colors[i % colors.size])) }
            if (illegal.isNotEmpty()) Box(Modifier.weight(illegal.values.sum() / cap).fillMaxHeight().background(Noir.red))
            if (sealed > 0) Box(Modifier.weight(sealed / cap).fillMaxHeight().background(Noir.salmon))
            val free = cap - game.holdUsed
            if (free > 0) Spacer(Modifier.weight(free / cap))
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            legal.entries.take(2).forEach { (c, t) -> T("${c.label} $t t", 11f, Noir.text) }
            if (illegal.isNotEmpty()) T("Carga oculta ${illegal.values.sum()} t", 11f, Noir.salmon)
            if (sealed > 0) T("Lacrada $sealed t", 11f, Noir.salmon)
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
