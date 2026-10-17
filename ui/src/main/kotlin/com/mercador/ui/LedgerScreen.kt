package com.mercador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercador.core.game.Game
import com.mercador.core.game.Trait

/** Livro de contas: quem você está se tornando (maquete 4). */
@Composable
fun LedgerScreen(game: Game, voice: String, onTab: (Tab) -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Noir.bg)) {
        Column(Modifier.fillMaxWidth().background(Noir.panel).padding(16.dp)) {
            T("LIVRO DE CONTAS", 22f, Noir.text, Noir.serif, spacing = 2f)
            Spacer(Modifier.height(4.dp))
            T("o que você fez · não pode ser apagado", 12f, Noir.muted, Noir.mono)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
        Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp)) {
            Label("Quem você está se tornando")
            Spacer(Modifier.height(10.dp))
            TraitChips(game)
            Spacer(Modifier.height(10.dp))
            T("“$voice”", 13f, Noir.sub, Noir.serif, italic = true)
            Spacer(Modifier.height(14.dp))
            Label("Reputação")
            Spacer(Modifier.height(8.dp))
            for (f in game.factions.factions) {
                val r = game.standing.reputation(f)
                val label = when { r <= -50 -> "hostil"; r <= -15 -> "frio"; r < 15 -> "neutro"; r < 50 -> "útil"; r < 80 -> "confiável"; else -> "aliado" }
                val color = Noir.faction(f)
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    T(f.name, 13f, Noir.text.copy(alpha = .85f), modifier = Modifier.weight(1.1f))
                    Bar((r + 100) / 200f, color, Modifier.weight(1.4f), 8)
                    T(label, 11f, color, align = TextAlign.End, modifier = Modifier.weight(.7f))
                }
            }
            Spacer(Modifier.height(12.dp))
            Label("Entradas")
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
            for (e in game.ledger.takeLast(3).reversed()) {
                val permanent = e.text.contains("violência")
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    T("DIA ${e.day} · ${e.systemName.uppercase()}" + if (permanent) " · MARCA PERMANENTE" else "", 10.5f, if (permanent) Noir.red else Noir.muted, Noir.mono)
                    Spacer(Modifier.height(2.dp))
                    T(e.text, 12.5f, Noir.text, Noir.serif, lineHeight = 17f)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
            }
            if (game.ledger.isEmpty()) T("Nenhuma entrada. Ainda.", 12.5f, Noir.sub, Noir.serif, italic = true, modifier = Modifier.padding(vertical = 8.dp))
            Spacer(Modifier.height(10.dp))
            game.openConsequences().firstOrNull()?.let { (p, due) ->
                Card(Modifier.fillMaxWidth(), border = Noir.dangerLine, fill = Noir.dangerBg) {
                    Label("Consequência em aberto", Noir.salmon)
                    Spacer(Modifier.height(4.dp))
                    T("${p.name}, ${p.role}, ainda está por aí e sabe onde procurar.", 13f, Noir.text, Noir.serif, lineHeight = 18f)
                    T("chegada prevista: dia ~$due", 10.5f, Color(0xFFA56A62), Noir.mono, align = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.weight(1f))
            OutlineButton("Ver todas as entradas (${game.ledger.size})", Modifier.fillMaxWidth(), Noir.sub)
        }
        BottomNav(Tab.LIVRO, onSelect = onTab)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TraitChips(game: Game) {
    val traits = game.traits.toList()
    if (traits.isEmpty()) { T("Nenhum traço ainda.", 12f, Noir.sub, Noir.serif, italic = true); return }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (t in traits) Chip(t, game)
    }
}

@Composable
private fun Chip(t: Trait, game: Game) {
    val color = when (t) { Trait.CALEJADO -> Noir.amber; Trait.INSONE -> Noir.violet; Trait.DEVEDOR, Trait.MARCADO -> Noir.red; Trait.FRIO -> Noir.teal }
    val fading = !t.permanent
    val left = game.traitDaysLeft(t)
    val label = if (fading && left != null && left < 60) "${t.label} (se dissipando)" else t.label
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (fading && left != null && left < 60) Noir.track else Noir.bg)
            .drawBehind {
                drawRoundRect(if (fading && left != null && left < 60) Noir.line else color, cornerRadius = CornerRadius(size.height / 2),
                    style = Stroke(1.5f * density, pathEffect = if (fading && left != null && left < 60) PathEffect.dashPathEffect(floatArrayOf(6f * density, 4f * density)) else null))
            }.padding(horizontal = 14.dp, vertical = 7.dp),
    ) { T(label, 13f, if (fading && left != null && left < 60) Noir.sub else color) }
}
