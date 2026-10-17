package com.mercador.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.mercador.core.universe.Faction

/** "Dossiê noir" (seção 10): fundo quase preto, texto cinza-claro, uma cor por facção. */
object Noir {
    val bg = Color(0xFF0B0D10)
    val panel = Color(0xFF12161C)
    val panel2 = Color(0xFF161C24)
    val line = Color(0xFF262D36)
    val track = Color(0xFF1C222A)
    val text = Color(0xFFD8DDE3)
    val sub = Color(0xFF8A96A3)
    val muted = Color(0xFF7C8793)
    val amber = Color(0xFFD9A441)
    val red = Color(0xFFC0392B)
    val salmon = Color(0xFFE07A6A)
    val violet = Color(0xFF8B6FC4)
    val teal = Color(0xFF3FA7A0)
    val dangerBg = Color(0xFF140D0E)
    val dangerLine = Color(0xFF4A2020)

    /** Âmbar, vermelho, violeta, verde-água: a ordem das facções da maquete. O vermelho fica ligado a perigo. */
    private val factionColors = listOf(amber, red, violet, teal)
    fun faction(f: Faction): Color = factionColors[f.id % factionColors.size]

    val serif = FontFamily.Serif
    val mono = FontFamily.Monospace
    val sans = FontFamily.SansSerif
}

@Composable
fun T(
    text: String,
    size: Float = 13f,
    color: Color = Noir.text,
    family: FontFamily = Noir.sans,
    weight: FontWeight = FontWeight.Normal,
    spacing: Float = 0f,
    italic: Boolean = false,
    align: TextAlign = TextAlign.Start,
    lineHeight: Float = 0f,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text,
        modifier,
        TextStyle(
            color = color, fontSize = size.sp, fontFamily = family, fontWeight = weight, letterSpacing = spacing.sp,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal, textAlign = align,
            lineHeight = if (lineHeight > 0) lineHeight.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
        ),
    )
}

/** Rótulo de seção em caixa-alta espaçada, como nas maquetes. */
@Composable
fun Label(text: String, color: Color = Noir.muted, modifier: Modifier = Modifier) =
    T(text.uppercase(), 10f, color, spacing = 1.8f, modifier = modifier)

fun credits(n: Int): String {
    val s = n.toString()
    val grouped = s.reversed().chunked(3).joinToString(" ").reversed()
    return "$grouped cr"
}
