package com.mercador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class Tab(val label: String) { MAPA("Mapa"), ESTACAO("Estação"), DOSSIE("Dossiê"), LIVRO("Livro") }

/** Medidor fino com rótulo; nunca só cor (seção 10.1). */
@Composable
fun Meter(label: String, fraction: Float, color: Color, modifier: Modifier = Modifier, labelWidth: Int = 0) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (labelWidth > 0) {
            Box(Modifier.width(labelWidth.dp)) { T(label.uppercase(), 9f, Noir.muted, Noir.mono, spacing = 1f) }
            Bar(fraction, color, Modifier.weight(1f))
        } else {
            Column(Modifier.weight(1f)) {
                T(label.uppercase(), 9f, Noir.muted, spacing = 1.5f)
                Spacer(Modifier.height(4.dp))
                Bar(fraction, color, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun Bar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Int = 8) {
    Box(modifier.height(height.dp).clip(RoundedCornerShape(50)).background(Noir.track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, border: Color = Noir.line, fill: Color = Noir.panel, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(fill).border(1.dp, border, RoundedCornerShape(10.dp)).padding(14.dp),
        content = content,
    )
}

@Composable
fun BottomNav(selected: Tab, modifier: Modifier = Modifier, onSelect: (Tab) -> Unit = {}) {
    Column(modifier.fillMaxWidth().background(Noir.panel)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
        Row(Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.CenterVertically) {
            for (t in Tab.values()) Box(Modifier.weight(1f).clickable { onSelect(t) }, contentAlignment = Alignment.Center) {
                T(t.label.uppercase(), 11f, if (t == selected) Noir.amber else Noir.muted, spacing = 1.5f, align = TextAlign.Center)
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(8.dp)).background(Noir.amber).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { T(text.uppercase(), 13f, Noir.bg, weight = FontWeight.Bold, spacing = 1.5f) }
}

@Composable
fun OutlineButton(text: String, modifier: Modifier = Modifier, color: Color = Noir.amber, onClick: () -> Unit = {}) {
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, color, RoundedCornerShape(8.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { T(text.uppercase(), 13f, color, weight = FontWeight.Bold, spacing = 1.5f) }
}

/** Barras de atenção ▮▮▮▯▯ em texto, como na maquete. */
fun attentionBars(heat: Int): String {
    val n = (heat / 20).coerceIn(0, 5)
    return "▮".repeat(n) + "▯".repeat(5 - n)
}
