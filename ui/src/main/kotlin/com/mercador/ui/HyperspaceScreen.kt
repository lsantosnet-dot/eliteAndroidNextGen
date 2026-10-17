package com.mercador.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercador.core.rng.Rng
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Dados do salto que a animação mostra (o jogo já mudou de lugar quando ela toca). */
class Jump(val from: String, val to: String, val ly: Int, val days: Int)

/** Duração fixa: é sempre a mesma animação. */
const val HYPERSPACE_MS = 2800L

/**
 * Toca a animação de hiperespaço e chama [onDone] ao fim (ou ao toque, para pular).
 * O progresso vem do relógio de quadros, então a duração não depende do aparelho.
 */
@Composable
fun HyperspaceJump(jump: Jump, onDone: () -> Unit) {
    var progress by remember(jump) { mutableFloatStateOf(0f) }
    LaunchedEffect(jump) {
        var start = -1L
        while (progress < 1f) {
            withFrameNanos { now ->
                if (start < 0) start = now
                progress = ((now - start) / 1_000_000f / HYPERSPACE_MS).coerceIn(0f, 1f)
            }
        }
        onDone()
    }
    Box(Modifier.fillMaxSize().clickable { onDone() }) { HyperspaceScreen(jump, progress) }
}

private const val STARS = 150
private const val RINGS = 7

/** Um quadro da animação, função pura de [p] (0 a 1): estrelas em riscos, túnel de anéis e a nave entrando nele. */
@Composable
fun HyperspaceScreen(jump: Jump, p: Float) {
    // Estrelas fixas por semente: o mesmo céu em toda viagem.
    val stars = remember {
        val r = Rng(0x4850)
        List(STARS) { Triple(r.nextDouble() * 2 * PI, r.nextDouble().toFloat(), .5f + r.nextDouble().toFloat()) }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(Modifier.fillMaxSize()) { drawTunnel(stars, p); drawShip(p); drawFlash(p) }
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val fade = (1f - p * 1.6f).coerceIn(0f, 1f)
            T("SALTO PARA O HIPERESPAÇO", 11f, Noir.amber.copy(alpha = fade), spacing = 3f, align = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            val label = ((p - .15f) * 4f).coerceIn(0f, 1f) * (1f - ((p - .85f) * 7f).coerceIn(0f, 1f))
            T("${jump.from}  →  ${jump.to}", 16f, Noir.text.copy(alpha = label), Noir.mono, align = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            T("${jump.ly} LY · ${jump.days} dias", 11f, Noir.muted.copy(alpha = label), Noir.mono, align = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
        }
    }
}

/** Distância acumulada percorrida: devagar no início, cada vez mais rápido (decolagem). */
private fun travelled(p: Float) = .4f * p + 3.6f * p * p * p
private fun speed(p: Float) = .4f + 10.8f * p * p

private fun DrawScope.drawTunnel(stars: List<Triple<Double, Float, Float>>, p: Float) {
    val c = Offset(size.width / 2, size.height * .44f)
    val maxR = size.maxDimension * .75f
    val t = travelled(p)
    val v = speed(p)
    // brilho no ponto de fuga, que cresce com a velocidade
    drawCircle(Brush.radialGradient(listOf(Noir.teal.copy(alpha = .10f + .28f * p), Color.Transparent), c, maxR * .55f), maxR * .55f, c)
    // anéis do túnel (o "tubo" do Elite), vindo do centro para fora
    for (k in 0 until RINGS) {
        val f = ((k.toFloat() / RINGS + t * .55f) % 1f)
        val r = maxR * f * f
        drawCircle(
            (if (k % 2 == 0) Noir.teal else Noir.violet).copy(alpha = (.10f + .32f * p) * f * (1f - f * .4f)),
            r, c, style = Stroke(width = (1f + 2.2f * f) * density),
        )
    }
    // estrelas: riscos radiais, tanto mais longos quanto maior a velocidade
    for ((angle, base, s) in stars) {
        val f = ((base + t * s * .5f) % 1f)
        val rEnd = maxR * f * f
        val len = (.006f + .045f * v * s * .35f) * maxR * (.3f + f)
        val rStart = (rEnd - len).coerceAtLeast(0f)
        val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        val a = (.25f + .75f * f).coerceIn(0f, 1f)
        val col = Color.White.copy(alpha = a).let { if (s > 1.1f) lerpColor(it, Noir.teal, .45f) else it }
        drawLine(col, c + dir * rStart, c + dir * rEnd, strokeWidth = (.7f + 1.8f * f) * density, cap = StrokeCap.Round)
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha,
)

/** A nave vista por trás, em arame âmbar, que encolhe e sobe para o ponto de fuga enquanto o motor brilha. */
private fun DrawScope.drawShip(p: Float) {
    val ease = p * p * (3f - 2f * p)
    val scale = (1.05f - .62f * ease) * density
    val c = Offset(size.width / 2, size.height * (.74f - .24f * ease))
    // tremor leve, mais forte no meio do salto
    val shake = sin(p * 90f) * 1.6f * density * (p * (1f - p) * 4f)
    val o = c + Offset(shake, 0f)
    fun pt(x: Float, y: Float) = Offset(o.x + x * scale, o.y + y * scale)
    val hull = Path().apply {
        val pts = listOf(0f to -46f, 14f to -22f, 58f to 6f, 64f to 20f, 30f to 22f, 18f to 30f, -18f to 30f, -30f to 22f, -64f to 20f, -58f to 6f, -14f to -22f)
        moveTo(pt(pts[0].first, pts[0].second).x, pt(pts[0].first, pts[0].second).y)
        pts.drop(1).forEach { lineTo(pt(it.first, it.second).x, pt(it.first, it.second).y) }
        close()
    }
    val fadeIn = (1f - ((p - .78f) * 4.5f).coerceIn(0f, 1f))
    // brilho do motor
    val glow = (28f + 26f * p) * scale
    drawCircle(Brush.radialGradient(listOf(Noir.amber.copy(alpha = .75f * fadeIn), Color.Transparent), pt(0f, 24f), glow), glow, pt(0f, 24f))
    drawPath(hull, Color(0xFF0E1218).copy(alpha = fadeIn))
    drawPath(hull, Noir.amber.copy(alpha = .35f * fadeIn), style = Stroke(5f * density))
    drawPath(hull, Noir.amber.copy(alpha = fadeIn), style = Stroke(1.6f * density))
    // arestas internas e motores
    for ((a, b) in listOf((14f to -22f) to (0f to 6f), (-14f to -22f) to (0f to 6f), (0f to 6f) to (0f to 28f), (58f to 6f) to (30f to 22f), (-58f to 6f) to (-30f to 22f)))
        drawLine(Noir.amber.copy(alpha = .55f * fadeIn), pt(a.first, a.second), pt(b.first, b.second), 1f * density)
    for (x in listOf(-40f, -22f, 22f, 40f)) drawCircle(Noir.salmon.copy(alpha = fadeIn), 3.2f * scale, pt(x, 25f))
}

/** Clarão branco no fim do salto, que cobre a tela antes da chegada. */
private fun DrawScope.drawFlash(p: Float) {
    val a = ((p - .86f) / .14f).coerceIn(0f, 1f)
    if (a > 0f) drawRect(Color.White.copy(alpha = a * a))
}
