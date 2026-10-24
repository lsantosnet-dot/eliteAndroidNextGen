package com.mercador.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercador.core.game.Upgrade
import com.mercador.core.rng.Rng
import com.mercador.core.universe.Faction
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animações curtas em tela cheia, da mesma família do salto no hiperespaço: progresso pelo relógio de quadros,
 * cada quadro é função pura de p (0 a 1) e um toque pula. O controlador enfileira várias (salto, revolta no
 * caminho, atracação ou interceptação) e elas tocam em ordem.
 */
sealed interface Cinematic { val durationMs: Long }

/** A nave se aproxima da estação girando, alinha com a fenda e entra (fim de toda viagem calma). */
class Docking(val station: String, val faction: Faction, val seed: Long) : Cinematic { override val durationMs get() = DOCKING_MS }

/** A patrulha aparece no radar e te alcança: alarme vermelho antes da decisão. */
class Interception(val system: String, val patrol: Faction, val reason: String) : Cinematic { override val durationMs get() = INTERCEPTION_MS }

/** Um sistema troca de dono no caminho: a cor velha racha e a nova inunda. */
class Revolt(val system: String, val old: Faction, val new: Faction) : Cinematic { override val durationMs get() = REVOLT_MS }

/** O estaleiro instala uma melhoria: varredura, faíscas e a peça acesa na nave. */
class Install(val upgrade: Upgrade, val level: Int) : Cinematic { override val durationMs get() = INSTALL_MS }

const val DOCKING_MS = 1700L
const val INTERCEPTION_MS = 2000L
const val REVOLT_MS = 2400L
const val INSTALL_MS = 2000L

/** Toca [cinematic] e chama [onDone] ao fim (ou ao toque, para pular). */
@Composable
fun CinematicPlayer(cinematic: Cinematic, onDone: () -> Unit) {
    var progress by remember(cinematic) { mutableFloatStateOf(0f) }
    LaunchedEffect(cinematic) {
        var start = -1L
        while (progress < 1f) {
            withFrameNanos { now ->
                if (start < 0) start = now
                progress = ((now - start) / 1_000_000f / cinematic.durationMs).coerceIn(0f, 1f)
            }
        }
        onDone()
    }
    Box(Modifier.fillMaxSize().clickable { onDone() }) { CinematicFrame(cinematic, progress) }
}

/** Um quadro de qualquer animação. */
@Composable
fun CinematicFrame(cinematic: Cinematic, p: Float) = when (cinematic) {
    is Jump -> HyperspaceScreen(cinematic, p)
    is Docking -> DockingScreen(cinematic, p)
    is Interception -> InterceptionScreen(cinematic, p)
    is Revolt -> RevoltScreen(cinematic, p)
    is Install -> InstallScreen(cinematic, p)
}

private fun ease(p: Float) = p * p * (3f - 2f * p)
private fun window(p: Float, from: Float, to: Float) = ((p - from) / (to - from)).coerceIn(0f, 1f)
/** Sobe de 0 a 1 entre [inA] e [inB] e desce de volta entre [outA] e [outB]: a opacidade dos letreiros. */
private fun fade(p: Float, inA: Float, inB: Float, outA: Float, outB: Float) = window(p, inA, inB) * (1f - window(p, outA, outB))

/** Letreiro de cima (o que está acontecendo) e de baixo (onde/quem), como no salto. */
@Composable
private fun Captions(top: String, title: String, sub: String, color: Color, topAlpha: Float, alpha: Float) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        T(top, 11f, color.copy(alpha = topAlpha), spacing = 3f, align = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        T(title, 16f, Noir.text.copy(alpha = alpha), Noir.mono, align = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        T(sub, 11f, Noir.muted.copy(alpha = alpha), Noir.mono, align = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
    }
}

/** Céu fixo por semente, com as estrelas se abrindo devagar do centro (a nave avança). */
private fun DrawScope.drift(seed: Long, p: Float, speed: Float, tint: Color = Color.White) {
    val r = Rng(seed)
    val c = Offset(size.width / 2, size.height * .44f)
    val maxR = size.maxDimension * .7f
    repeat(90) {
        val a = r.nextDouble() * 2 * PI
        val f = ((r.nextDouble().toFloat() + p * speed) % 1f)
        val rr = maxR * f * f
        drawCircle(tint.copy(alpha = .2f + .6f * f), (.6f + 1.4f * f) * density, c + Offset(cos(a).toFloat() * rr, sin(a).toFloat() * rr))
    }
}

// ---------------------------------------------------------------- atracação

@Composable
fun DockingScreen(d: Docking, p: Float) {
    val color = Noir.faction(d.faction)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(Modifier.fillMaxSize()) { drift(d.seed, p, .35f); drawStation(p, color); drawDockFade(p) }
        Captions("ATRACANDO", "Estação ${d.station}", "${d.faction.name} · autorização concedida", color,
            1f - window(p, .55f, .8f), fade(p, .08f, .3f, .7f, .88f))
    }
}

/**
 * A estação de Elite (Coriolis) vista de frente: um quadrado com as faces triangulares e a fenda no meio. Ela cresce
 * (aproximação), gira cada vez mais devagar até a fenda ficar alinhada e, no fim, a fenda engole a tela.
 */
private fun DrawScope.drawStation(p: Float, color: Color) {
    val c = Offset(size.width / 2, size.height * .44f)
    val e = ease(p)
    val r = size.minDimension * (.12f + 1.9f * e * e * e + .18f * e)
    // gira e desacelera até alinhar (ângulo final = 0)
    val angle = (1f - e) * (1f - e) * 2.4f
    fun rot(x: Float, y: Float): Offset {
        val ca = cos(angle); val sa = sin(angle)
        return c + Offset((x * ca - y * sa) * r, (x * sa + y * ca) * r)
    }
    // brilho de luzes de navegação na cor da facção
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .18f), Color.Transparent), c, r * 1.6f), r * 1.6f, c)
    val outer = listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f)
    val mid = listOf(0f to -.62f, .62f to 0f, 0f to .62f, -.62f to 0f)
    val hull = Path().apply {
        val o = outer.map { rot(it.first, it.second) }
        moveTo(o[0].x, o[0].y); o.drop(1).forEach { lineTo(it.x, it.y) }; close()
    }
    drawPath(hull, Color(0xFF0B0F14))
    drawPath(hull, color.copy(alpha = .35f), style = Stroke(5f * density))
    drawPath(hull, color, style = Stroke(1.6f * density))
    // faces: cada canto liga às duas faces vizinhas do losango interno
    for (i in 0 until 4) {
        val a = rot(outer[i].first, outer[i].second)
        drawLine(color.copy(alpha = .6f), a, rot(mid[i].first, mid[i].second), 1f * density)
        drawLine(color.copy(alpha = .6f), a, rot(mid[(i + 3) % 4].first, mid[(i + 3) % 4].second), 1f * density)
        drawLine(color.copy(alpha = .6f), rot(mid[i].first, mid[i].second), rot(mid[(i + 1) % 4].first, mid[(i + 1) % 4].second), 1f * density)
    }
    // a fenda de atracação, com luzes que piscam ao longo da borda
    val slot = listOf(-.3f to -.08f, .3f to -.08f, .3f to .08f, -.3f to .08f).map { rot(it.first, it.second) }
    val slotPath = Path().apply { moveTo(slot[0].x, slot[0].y); slot.drop(1).forEach { lineTo(it.x, it.y) }; close() }
    drawPath(slotPath, Color.Black)
    drawPath(slotPath, Noir.text.copy(alpha = .9f), style = Stroke(1.4f * density))
    val blink = if (((p * 14f).toInt() % 2) == 0) 1f else .35f
    for (k in 0..4) {
        val x = -.3f + .15f * k
        drawCircle(Noir.amber.copy(alpha = blink), 2.2f * density, rot(x, -.12f))
        drawCircle(Noir.amber.copy(alpha = 1.35f - blink), 2.2f * density, rot(x, .12f))
    }
}

/** No fim, o escuro do hangar: a tela vai ao fundo noir, de onde a estação aparece. */
private fun DrawScope.drawDockFade(p: Float) {
    val a = window(p, .8f, 1f)
    if (a > 0f) drawRect(Noir.bg.copy(alpha = a))
}

// ---------------------------------------------------------------- interceptação

@Composable
fun InterceptionScreen(i: Interception, p: Float) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0709))) {
        Canvas(Modifier.fillMaxSize()) { drawAlarm(p); drawRadar(p); drawPatrol(p) }
        Captions("INTERCEPTADO", "Patrulha de ${i.patrol.name}", "${i.system} · ${i.reason}", Noir.red,
            if (((p * 8f).toInt() % 2) == 0) 1f else .45f, fade(p, .3f, .5f, .92f, 1f))
    }
}

/** Vinheta vermelha que pulsa como sirene, cada vez mais forte. */
private fun DrawScope.drawAlarm(p: Float) {
    val pulse = .5f + .5f * sin(p * 2 * PI.toFloat() * 4f)
    val a = (.15f + .4f * p) * pulse
    val c = Offset(size.width / 2, size.height / 2)
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Noir.red.copy(alpha = a)), c, size.maxDimension * .75f))
}

/** O scanner: anéis, a varredura que gira e o ponto da patrulha correndo para o centro. */
private fun DrawScope.drawRadar(p: Float) {
    val c = Offset(size.width / 2, size.height * .62f)
    val r = size.minDimension * .42f
    val alpha = 1f - window(p, .55f, .8f)
    for (k in 1..3) drawCircle(Noir.red.copy(alpha = .35f * alpha), r * k / 3f, c, style = Stroke(1f * density))
    drawLine(Noir.red.copy(alpha = .25f * alpha), c - Offset(r, 0f), c + Offset(r, 0f), 1f * density)
    drawLine(Noir.red.copy(alpha = .25f * alpha), c - Offset(0f, r), c + Offset(0f, r), 1f * density)
    val sweep = p * 2 * PI.toFloat() * 2.2f
    for (k in 0 until 12) {
        val a = sweep - k * .05f
        drawLine(Noir.red.copy(alpha = (.6f - k * .05f) * alpha), c, c + Offset(cos(a), sin(a)) * r, (2f - k * .12f) * density, cap = StrokeCap.Round)
    }
    // o ponto: começa na borda, de cima, e fecha a distância
    val d = 1f - ease(window(p, 0f, .6f))
    val blip = c + Offset(.35f, -.94f) * (r * (.08f + .92f * d))
    drawCircle(Noir.salmon.copy(alpha = alpha), 5f * density, blip)
    drawCircle(Noir.salmon.copy(alpha = .4f * alpha), (9f + 6f * sin(p * 40f)) * density, blip, style = Stroke(1.2f * density))
    // você, no centro
    drawCircle(Noir.amber.copy(alpha = alpha), 4f * density, c)
}

/** A silhueta da patrulha desce e cresce até tomar a tela, com os faróis acesos. */
private fun DrawScope.drawPatrol(p: Float) {
    val e = ease(window(p, .45f, 1f))
    if (e <= 0f) return
    val k = density * (.4f + 1.5f * e)
    val c = Offset(size.width / 2, size.height * (.1f + .32f * e))
    val pts = listOf(-92f to 18f, -30f to -6f, -18f to -34f, 0f to -50f, 18f to -34f, 30f to -6f, 92f to 18f, 40f to 26f, 18f to 52f, 0f to 58f, -18f to 52f, -40f to 26f)
    val path = Path().apply {
        moveTo(c.x + pts[0].first * k, c.y + pts[0].second * k)
        pts.drop(1).forEach { lineTo(c.x + it.first * k, c.y + it.second * k) }
        close()
    }
    drawCircle(Brush.radialGradient(listOf(Noir.red.copy(alpha = .4f * e), Color.Transparent), c, 140f * k), 140f * k, c)
    drawPath(path, Color(0xFF131012).copy(alpha = e))
    drawPath(path, Color(0xFF6B2A26).copy(alpha = e), style = Stroke(1.5f * density))
    drawCircle(Noir.red.copy(alpha = e), 7f * k, Offset(c.x, c.y + 6 * k))
    // faróis: dois cones de luz que varrem a tela
    for (side in listOf(-1f, 1f)) {
        val from = Offset(c.x + side * 62 * k, c.y + 18 * k)
        drawCircle(Noir.salmon.copy(alpha = e), 3f * k, from)
        val swing = sin(p * 9f + side) * .25f
        val cone = Path().apply {
            moveTo(from.x, from.y)
            lineTo(from.x + (side * .2f + swing - .18f) * size.height, size.height)
            lineTo(from.x + (side * .2f + swing + .18f) * size.height, size.height)
            close()
        }
        drawPath(cone, Brush.verticalGradient(listOf(Noir.salmon.copy(alpha = .28f * e), Color.Transparent), from.y, size.height))
    }
}

// ---------------------------------------------------------------- revolta

@Composable
fun RevoltScreen(r: Revolt, p: Float) {
    val oldC = Noir.faction(r.old); val newC = Noir.faction(r.new)
    Box(Modifier.fillMaxSize().background(Noir.bg)) {
        Canvas(Modifier.fillMaxSize()) { drawRevolt(r.system.hashCode().toLong(), p, oldC, newC) }
        Captions("REVOLTA EM ${r.system.uppercase()}", "${r.old.name}  →  ${r.new.name}", "o poder caiu · as leis mudaram", Noir.salmon,
            fade(p, 0f, .1f, .85f, 1f), fade(p, .45f, .6f, .9f, 1f))
    }
}

/**
 * O sistema no mapa, na cor de quem governa. A cor velha racha (fissuras que crescem do centro), estoura em cacos
 * que voam, e uma onda na cor nova toma o lugar. Brasas sobem o tempo todo.
 */
private fun DrawScope.drawRevolt(seed: Long, p: Float, oldC: Color, newC: Color) {
    val c = Offset(size.width / 2, size.height * .44f)
    val r = size.minDimension * .2f
    val rng = Rng(seed)
    // brasas
    val embers = Rng(seed xor 0xE3B)
    repeat(40) {
        val x = embers.nextDouble().toFloat() * size.width
        val sp = .4f + embers.nextDouble().toFloat()
        val y = size.height * (1f - ((embers.nextDouble().toFloat() + p * sp) % 1f))
        drawCircle(Noir.salmon.copy(alpha = .15f + .35f * (y / size.height)), (1f + 1.5f * sp) * density, Offset(x + sin(p * 12f + it) * 6f * density, y))
    }
    val crack = window(p, .1f, .4f)
    val burst = ease(window(p, .38f, .75f))
    val flood = ease(window(p, .45f, .9f))
    // a cor velha, inteira até rachar
    if (burst < 1f) {
        drawCircle(Brush.radialGradient(listOf(oldC.copy(alpha = .45f * (1f - burst)), Color.Transparent), c, r * 2.4f), r * 2.4f, c)
        if (burst == 0f) drawCircle(oldC, r * .5f, c)
    }
    // fissuras: linhas quebradas do centro para fora
    val cracks = List(9) { i -> (i / 9f) * 2 * PI.toFloat() + rng.nextDouble().toFloat() * .5f }
    if (burst == 0f) for (a0 in cracks) {
        var pt = c; var a = a0
        val steps = (6 * crack).toInt()
        repeat(steps) {
            a += (rng.nextDouble().toFloat() - .5f) * .7f
            val nxt = pt + Offset(cos(a), sin(a)) * (r * .12f)
            drawLine(Noir.bg, pt, nxt, 2.4f * density, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = .8f), pt, nxt, .8f * density, cap = StrokeCap.Round)
            pt = nxt
        }
    }
    // cacos voando
    if (burst > 0f) for ((i, a) in cracks.withIndex()) {
        val d = r * (.25f + 2.6f * burst) * (.7f + .1f * (i % 4))
        val center = c + Offset(cos(a + .17f), sin(a + .17f)) * d
        val spin = burst * 6f * (if (i % 2 == 0) 1 else -1)
        val s = r * .22f * (1f - burst * .6f)
        val tri = Path().apply {
            for (k in 0..2) {
                val ang = spin + k * 2.1f
                val v = center + Offset(cos(ang), sin(ang)) * s
                if (k == 0) moveTo(v.x, v.y) else lineTo(v.x, v.y)
            }
            close()
        }
        drawPath(tri, oldC.copy(alpha = 1f - burst))
    }
    // onda da cor nova
    if (flood > 0f) {
        drawCircle(Brush.radialGradient(listOf(newC.copy(alpha = .5f * flood), Color.Transparent), c, r * 2.6f * flood + 1f), r * 2.6f * flood + 1f, c)
        drawCircle(newC.copy(alpha = 1f - flood), r * 3.2f * flood, c, style = Stroke(3f * density))
        drawCircle(newC, r * .5f * flood, c)
        drawCircle(Noir.text.copy(alpha = flood), r * .5f * flood + 5f * density, c, style = Stroke(1.5f * density))
    }
}

// ---------------------------------------------------------------- estaleiro

@Composable
fun InstallScreen(i: Install, p: Float) {
    Box(Modifier.fillMaxSize().background(Noir.bg)) {
        Canvas(Modifier.fillMaxSize()) { drawBlueprint(p); drawInstall(i.upgrade, p) }
        val done = window(p, .78f, .86f)
        Captions(if (done > 0f) "INSTALADO" else "ESTALEIRO · INSTALANDO", i.upgrade.label, "nível ${i.level} / ${i.upgrade.maxLevel} · ${i.upgrade.effect}",
            if (done > 0f) Noir.teal else Noir.amber, 1f, fade(p, .15f, .35f, 2f, 3f))
    }
}

/** Grade de planta técnica ao fundo. */
private fun DrawScope.drawBlueprint(p: Float) {
    val step = 24f * density
    val a = .12f + .08f * window(p, 0f, .2f)
    var x = 0f; while (x < size.width) { drawLine(Noir.teal.copy(alpha = a), Offset(x, 0f), Offset(x, size.height), .6f * density); x += step }
    var y = 0f; while (y < size.height) { drawLine(Noir.teal.copy(alpha = a), Offset(0f, y), Offset(size.width, y), .6f * density); y += step }
}

/** A nave em arame vista de cima; a peça nova acende, a varredura passa e as faíscas saem do ponto de solda. */
private fun DrawScope.drawInstall(u: Upgrade, p: Float) {
    val c = Offset(size.width / 2, size.height * .44f)
    val k = size.minDimension / 170f
    fun pt(x: Float, y: Float) = Offset(c.x + x * k, c.y + y * k)
    val hull = listOf(0f to -70f, 16f to -36f, 62f to 10f, 66f to 30f, 32f to 34f, 20f to 46f, -20f to 46f, -32f to 34f, -66f to 30f, -62f to 10f, -16f to -36f)
    val path = Path().apply {
        moveTo(pt(hull[0].first, hull[0].second).x, pt(hull[0].first, hull[0].second).y)
        hull.drop(1).forEach { val v = pt(it.first, it.second); lineTo(v.x, v.y) }
        close()
    }
    val draw = ease(window(p, 0f, .3f))
    drawPath(path, Color(0xFF0E1218).copy(alpha = draw))
    drawPath(path, Noir.text.copy(alpha = .7f * draw), style = Stroke(1.4f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 3f * density))))
    // a peça: onde ela fica na nave
    val glow = ease(window(p, .3f, .8f))
    val partColor = if (p >= .8f) Noir.teal else Noir.amber
    val weld: Offset = when (u) {
        Upgrade.PORAO -> {
            drawRect(partColor.copy(alpha = .25f * glow), pt(-22f, -6f), Size(44f * k, 36f * k))
            drawRect(partColor.copy(alpha = glow), pt(-22f, -6f), Size(44f * k, 36f * k), style = Stroke(1.6f * density))
            for (row in 0..2) drawLine(partColor.copy(alpha = .6f * glow), pt(-22f, 6f + row * 8f), pt(22f, 6f + row * 8f), 1f * density)
            pt(22f, 30f)
        }
        Upgrade.TANQUE -> {
            for (side in listOf(-1f, 1f)) {
                drawOval(partColor.copy(alpha = .25f * glow), pt(side * 52f - 9f, -4f), Size(18f * k, 30f * k))
                drawOval(partColor.copy(alpha = glow), pt(side * 52f - 9f, -4f), Size(18f * k, 30f * k), style = Stroke(1.6f * density))
            }
            pt(52f, 26f)
        }
        Upgrade.MOTOR -> {
            for (x in listOf(-24f, 0f, 24f)) {
                val e = pt(x, 50f)
                drawCircle(Brush.radialGradient(listOf(partColor.copy(alpha = .8f * glow), Color.Transparent), e, 26f * k * glow + 1f), 26f * k * glow + 1f, e)
                drawCircle(partColor.copy(alpha = glow), 5f * k, e, style = Stroke(1.6f * density))
            }
            pt(0f, 50f)
        }
        Upgrade.COMPARTIMENTO -> {
            drawRect(partColor.copy(alpha = glow), pt(-12f, -30f), Size(24f * k, 18f * k),
                style = Stroke(1.6f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * density, 3f * density))))
            pt(12f, -12f)
        }
        Upgrade.TRANSPONDER -> {
            val tip = pt(0f, -70f)
            for (ring in 0..2) {
                val f = ((p * 2.5f + ring / 3f) % 1f)
                drawCircle(partColor.copy(alpha = glow * (1f - f)), (6f + 40f * f) * k, tip, style = Stroke(1.4f * density))
            }
            drawCircle(partColor.copy(alpha = glow), 4f * k, tip)
            tip
        }
        Upgrade.LASER -> {
            // o canhão no nariz, com um feixe que acende
            drawLine(partColor.copy(alpha = glow), pt(0f, -50f), pt(0f, -78f), 3f * density)
            drawLine(partColor.copy(alpha = .5f * glow), pt(0f, -78f), pt(0f, -78f - 60f * glow), 1.6f * density)
            pt(0f, -50f)
        }
    }
    // varredura que desce pela nave
    val scan = window(p, .25f, .8f)
    if (scan in 0.001f..0.999f) {
        val y = pt(0f, -80f).y + (140f * k) * scan
        drawLine(Noir.teal.copy(alpha = .9f), Offset(c.x - 80f * k, y), Offset(c.x + 80f * k, y), 1.5f * density)
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Noir.teal.copy(alpha = .18f)), y - 30f * k, y), Offset(c.x - 80f * k, y - 30f * k), Size(160f * k, 30f * k))
    }
    // faíscas da solda
    val sparks = window(p, .3f, .78f)
    if (sparks > 0f && sparks < 1f) {
        val rng = Rng((p * 40f).toInt().toLong() + u.ordinal * 977L)
        repeat(14) {
            val a = rng.nextDouble() * 2 * PI
            val len = (4f + rng.nextDouble().toFloat() * 16f) * k
            val from = weld + Offset(cos(a).toFloat(), sin(a).toFloat()) * (2f * k)
            drawLine(if (it % 3 == 0) Color.White else Noir.amber, from, from + Offset(cos(a).toFloat(), sin(a).toFloat()) * len, 1.2f * density, cap = StrokeCap.Round)
        }
        drawCircle(Color.White, 3f * k, weld)
    }
    // carimbo de pronto
    val stamp = window(p, .8f, .9f)
    if (stamp > 0f) {
        val s = 1.6f - .6f * stamp
        val o = pt(0f, 78f)
        drawCircle(Noir.teal.copy(alpha = stamp), 16f * k * s, o, style = Stroke(2f * density))
        drawLine(Noir.teal.copy(alpha = stamp), o + Offset(-7f, 0f) * k * s, o + Offset(-2f, 5f) * k * s, 2.4f * density, cap = StrokeCap.Round)
        drawLine(Noir.teal.copy(alpha = stamp), o + Offset(-2f, 5f) * k * s, o + Offset(8f, -6f) * k * s, 2.4f * density, cap = StrokeCap.Round)
    }
}
