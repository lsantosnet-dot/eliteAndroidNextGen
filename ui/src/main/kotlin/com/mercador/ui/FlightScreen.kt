package com.mercador.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercador.core.flight.FlightResult
import com.mercador.core.flight.FlightSession
import com.mercador.core.flight.Planet
import com.mercador.core.flight.PlayerShip
import com.mercador.core.flight.Scanner
import com.mercador.core.flight.SegmentSink
import com.mercador.core.flight.ShipInstance
import com.mercador.core.flight.ShipType
import com.mercador.core.flight.Vec3
import com.mercador.core.flight.View
import com.mercador.core.flight.WireframeProjector
import com.mercador.core.universe.StarSystem
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * O voo do Elite dentro do Mercador: a vista 3D em arame (naves do original, planeta, sol, poeira), o painel
 * (escudos, energia, velocidade, radar "prato" e bússola) e os controles de toque (manche, acelerador, gatilho
 * e os botões de hiperespaço, salto curto, míssil, atracação, vista e destino). No PC também pelo teclado:
 * setas = manche, espaço/shift = acelerar/frear, A = laser, H = hiperespaço, J = salto, M = míssil,
 * C = atracar, F1–F4 = vistas, T = destino.
 *
 * O relógio de quadros move a simulação; tudo o que termina o voo (atracar, morrer) ou o leva a outro
 * sistema volta ao controlador por [onResult].
 */
@Composable
fun FlightScreen(
    session: FlightSession,
    onResult: (FlightResult) -> Unit,
    onSelectTarget: (StarSystem) -> Unit,
    /** Sem relógio (pré-visualizações e testes): o quadro fica parado. */
    live: Boolean = true,
) {
    var frame by remember { mutableLongStateOf(0L) }
    var picking by remember { mutableStateOf(false) }
    val keys = remember { HashSet<Key>() }
    val focus = remember { FocusRequester() }
    val c = session.controls

    if (live) LaunchedEffect(session) {
        // A tela pode ter saído no meio de um gesto (uma animação tomou a frente): nada fica preso.
        c.fire = false; c.stickX = null; c.stickY = null; c.rollKey = 0; c.pitchKey = 0
        c.accelerate = false; c.decelerate = false
        if (c.targetSpeed == null) c.targetSpeed = session.model.player.speed
        runCatching { focus.requestFocus() }
        var last = -1L
        while (true) {
            val r = withFrameNanos { now ->
                val dt = if (last < 0) 0f else ((now - last) / 1e9f)
                last = now
                applyKeys(keys, session)
                session.update(dt).also { frame++ }
            }
            if (r != null) onResult(r)
        }
    }

    Column(
        Modifier.fillMaxSize().background(Color.Black)
            .focusRequester(focus).focusable()
            .onKeyEvent { e ->
                when (e.type) {
                    KeyEventType.KeyDown -> {
                        if (keys.add(e.key)) keyPress(e.key, session, onResult) { picking = !picking }
                        if (e.key == Key.A) c.fire = true
                        true
                    }
                    KeyEventType.KeyUp -> { keys.remove(e.key); if (e.key == Key.A) c.fire = false; true }
                    else -> false
                }
            },
    ) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            val projector = remember { WireframeProjector() }
            Canvas(Modifier.fillMaxSize()) { frame.let { drawSpace(session, projector, it) } }
            FlightTopHud(session, frame)
            if (picking) TargetPicker(session, { picking = false }) { onSelectTarget(it); picking = false }
        }
        Dashboard(session, frame)
        ButtonStrip(session, frame, onResult) { picking = !picking }
        ControlDeck(session, frame)
    }
}

// ------------------------------------------------------------------ teclado

private fun applyKeys(keys: Set<Key>, s: FlightSession) {
    val c = s.controls
    c.rollKey = (if (Key.DirectionRight in keys) 1 else 0) - (if (Key.DirectionLeft in keys) 1 else 0)
    // seta para baixo puxa o manche: nariz para cima (como o "X" do original)
    c.pitchKey = (if (Key.DirectionDown in keys) 1 else 0) - (if (Key.DirectionUp in keys) 1 else 0)
    c.accelerate = Key.Spacebar in keys
    c.decelerate = Key.ShiftLeft in keys || Key.ShiftRight in keys || Key.Slash in keys
    if (c.accelerate || c.decelerate) c.targetSpeed = null
}

private fun keyPress(k: Key, s: FlightSession, onResult: (FlightResult) -> Unit, togglePicker: () -> Unit) {
    when (k) {
        Key.H -> s.toggleHyperspace()
        Key.J -> s.inSystemJump()
        Key.M -> s.missile()
        Key.C -> s.dockingComputer()?.let(onResult)
        Key.T -> togglePicker()
        Key.F1 -> s.cycleView(View.FRONT)
        Key.F2 -> s.cycleView(View.REAR)
        Key.F3 -> s.cycleView(View.LEFT)
        Key.F4 -> s.cycleView(View.RIGHT)
        else -> Unit
    }
}

// ------------------------------------------------------------------ vista 3D

private val SPACE_LINE = Color(0xFF7FE8FF)
private val PLANET = Noir.teal
private val SUN = Color(0xFFFFC44D)
private fun rgb(c: Int) = Color(0xFF000000.toInt() or c)

private val tmpA = Vec3()
private val tmpB = Vec3()

private fun DrawScope.drawSpace(s: FlightSession, proj: WireframeProjector, frame: Long) {
    val m = s.model
    val w = size.width; val h = size.height
    proj.viewportWidth = w; proj.viewportHeight = h; proj.view = m.view
    m.dust.halfTanX = .5f; m.dust.halfTanY = h / 2f / w
    drawRect(Color.Black)

    // poeira: pontos que crescem quando passam perto
    for (i in 0 until m.dust.active) {
        m.dust.inView(i, m.view, tmpA)
        val d = tmpA.z
        if (!proj.projectPoint(tmpA.x, tmpA.y, d, tmpB)) continue
        drawCircle(Color.White.copy(alpha = .85f), (if (d < 360f) 1.8f else 1.1f) * density, Offset(tmpB.x, tmpB.y))
    }

    m.sun?.let { sun ->
        if (proj.projectWorld(sun.position, tmpB)) {
            val r = Planet.RADIUS / tmpB.z * proj.focal
            val c = Offset(tmpB.x, tmpB.y)
            if (r < 2f) drawCircle(SUN, 2f * density, c)
            else {
                drawCircle(Brush.radialGradient(listOf(SUN.copy(alpha = .35f), Color.Transparent), c, r * 1.8f), r * 1.8f, c)
                drawCircle(Brush.radialGradient(listOf(Color.White, SUN, SUN.copy(alpha = .9f)), c, r), r, c)
            }
        }
    }
    m.planet?.let { drawPlanet(it, proj) }

    // naves: as distantes primeiro
    val ships = m.ships.sortedByDescending { m.view.toView(it.position.x, it.position.y, it.position.z, tmpA).z }
    for (ship in ships) {
        if (ship.killed) drawExplosion(ship, proj) else drawShip(ship, proj)
    }

    // lasers inimigos apontados para nós
    for (ship in ships) if (!ship.killed && ship.firingLaser && proj.projectWorld(ship.position, tmpB)) {
        val from = Offset(tmpB.x, tmpB.y)
        val jitter = ((frame * 7919 + ship.slot * 31) % 60 - 30) * density
        drawLine(Noir.red.copy(alpha = .5f), from, Offset(w / 2 + jitter, h), 5f * density, StrokeCap.Round)
        drawLine(Color(0xFFFF6A5A), from, Offset(w / 2 + jitter, h), 1.6f * density, StrokeCap.Round)
    }

    // nosso laser: dois feixes dos cantos de baixo até o centro da mira (LASLI)
    val p = m.player
    val laser = p.lasers[m.view.ordinal]
    if (p.laserBeamVisible && laser != 0) {
        val col = when (laser) {
            PlayerShip.PULSE_LASER -> Color(0xFFE8FF6A)
            PlayerShip.BEAM_LASER -> Color(0xFF6AF0FF)
            else -> Color(0xFFFF6AE8)
        }
        val tip = Offset(w / 2 + (p.laserX - 128) * density, h / 2 + (p.laserY - 96) * density)
        for (x in listOf(w * .22f, w * .78f)) {
            drawLine(col.copy(alpha = .35f), Offset(x, h), tip, 7f * density, StrokeCap.Round)
            drawLine(col, Offset(x, h), tip, 2f * density, StrokeCap.Round)
        }
    }

    drawCrosshair(laser != 0)
    drawNavMarker(s, proj)
    m.combat.missileTarget?.let { t ->
        if (proj.projectWorld(t.position, tmpB)) {
            val r = 14f * density
            drawRect(Noir.red, Offset(tmpB.x - r, tmpB.y - r), Size(2 * r, 2 * r), style = Stroke(1.6f * density))
        }
    }
    if (m.hitFlash > 0f) drawRect(Noir.red.copy(alpha = .28f * m.hitFlash))
    if (m.combat.ecmActive) drawRect(Color(0xFF6AF0FF).copy(alpha = .06f + .05f * ((frame / 4) % 2)))
}

private fun DrawScope.drawShip(ship: ShipInstance, proj: WireframeProjector) {
    val path = Path()
    val n = proj.project(ship, SegmentSink { x0, y0, x1, y1 -> path.moveTo(x0, y0); path.lineTo(x1, y1) })
    val col = rgb(ship.color)
    if (n < 0) {
        if (proj.projectWorld(ship.position, tmpB)) drawCircle(col, 1.6f * density, Offset(tmpB.x, tmpB.y))
        return
    }
    if (n == 0) return
    drawPath(path, col.copy(alpha = .22f), style = Stroke(4.5f * density, cap = StrokeCap.Round))
    drawPath(path, col, style = Stroke(1.3f * density, cap = StrokeCap.Round))
}

/** DOEXP: a nuvem de destroços cresce com o contador (18 → 256) e se apaga. */
private fun DrawScope.drawExplosion(ship: ShipInstance, proj: WireframeProjector) {
    if (!proj.projectWorld(ship.position, tmpB)) return
    val k = (ship.cloudCounter / 256f).coerceIn(0f, 1f)
    val size = ship.blueprint.boundingRadius / tmpB.z * proj.focal * (0.6f + 2.4f * k)
    val c = Offset(tmpB.x, tmpB.y)
    val fade = 1f - k
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE08A).copy(alpha = .55f * fade), Color.Transparent), c, size.coerceAtLeast(2f)), size.coerceAtLeast(2f), c)
    var seed = ship.slot * 1103515245 + 12345
    repeat(28) {
        seed = seed * 1103515245 + 12345
        val a = ((seed ushr 8) and 1023) / 1023f * 6.2832f
        seed = seed * 1103515245 + 12345
        val d = ((seed ushr 8) and 1023) / 1023f
        val o = Offset(c.x + cos(a) * size * d, c.y + sin(a) * size * d)
        drawCircle((if (it % 3 == 0) Color.White else rgb(ship.color)).copy(alpha = fade), (1f + 1.4f * d) * density, o)
    }
}

/** Planeta em arame: contorno e equador/meridiano (ou a cratera), só a metade voltada para nós. */
private fun DrawScope.drawPlanet(pl: Planet, proj: WireframeProjector) {
    val v = proj.view
    val c = v.toView(pl.position.x, pl.position.y, pl.position.z, Vec3())
    if (c.z <= proj.nearZ) return
    val r = Planet.RADIUS / c.z * proj.focal
    val sc = Offset(proj.viewportWidth / 2 + c.x / c.z * proj.focal, proj.viewportHeight / 2 - c.y / c.z * proj.focal)
    if (r < 1.5f) { drawCircle(PLANET, 1.5f * density, sc); return }
    if (r > 20_000f) return // dentro dele: a altitude já avisou
    drawCircle(PLANET.copy(alpha = .08f), r, sc)
    drawCircle(PLANET.copy(alpha = .3f), r, sc, style = Stroke(4f * density))
    drawCircle(PLANET, r, sc, style = Stroke(1.4f * density))
    val n = v.toView(pl.nosev.x, pl.nosev.y, pl.nosev.z, Vec3())
    val ro = v.toView(pl.roofv.x, pl.roofv.y, pl.roofv.z, Vec3())
    val si = v.toView(pl.sidev.x, pl.sidev.y, pl.sidev.z, Vec3())
    val path = Path()
    fun circleOn(pole: Vec3, u: Vec3, w: Vec3, angle: Float) {
        val ca = cos(angle); val sa = sin(angle)
        var prev: Offset? = null
        for (i in 0..64) {
            val t = i * 6.2832f / 64
            val ox = (pole.x * ca + u.x * sa * cos(t) + w.x * sa * sin(t)) * Planet.RADIUS
            val oy = (pole.y * ca + u.y * sa * cos(t) + w.y * sa * sin(t)) * Planet.RADIUS
            val oz = (pole.z * ca + u.z * sa * cos(t) + w.z * sa * sin(t)) * Planet.RADIUS
            val px = c.x + ox; val py = c.y + oy; val pz = c.z + oz
            val visible = ox * px + oy * py + oz * pz < 0f && pz > proj.nearZ
            val pt = if (visible) Offset(proj.viewportWidth / 2 + px / pz * proj.focal, proj.viewportHeight / 2 - py / pz * proj.focal) else null
            if (pt != null && prev != null) { path.moveTo(prev.x, prev.y); path.lineTo(pt.x, pt.y) }
            prev = pt
        }
    }
    if (pl.hasCrater) circleOn(n, si, ro, .45f)
    else { circleOn(ro, n, si, 1.5708f); circleOn(si, n, ro, 1.5708f) } // equador e meridiano
    drawPath(path, PLANET.copy(alpha = .8f), style = Stroke(1.1f * density))
}

private fun DrawScope.drawCrosshair(armed: Boolean) {
    val c = Offset(size.width / 2, size.height / 2)
    val a = 6f * density; val b = 16f * density
    val col = (if (armed) Noir.amber else Noir.muted).copy(alpha = .85f)
    for ((dx, dy) in listOf(1f to 0f, -1f to 0f, 0f to 1f, 0f to -1f))
        drawLine(col, c + Offset(dx * a, dy * a), c + Offset(dx * b, dy * b), 1.4f * density)
}

/**
 * Ajuda de navegação (do eliteAndroid; o original só tem a bússola): colchetes no alvo da bússola — a
 * estação, ou o planeta enquanto ela não surge — ou uma seta na borda apontando para onde virar.
 */
private fun DrawScope.drawNavMarker(s: FlightSession, proj: WireframeProjector) {
    val m = s.model
    val target = m.compassTarget ?: return
    val col = if (target === m.station) Noir.amber else PLANET
    val w = size.width; val h = size.height
    val margin = 22f * density
    m.view.toView(target.position.x, target.position.y, target.position.z, tmpA)
    if (tmpA.z > 0f) {
        val sx = w / 2 + tmpA.x / tmpA.z * proj.focal
        val sy = h / 2 - tmpA.y / tmpA.z * proj.focal
        if (sx in margin..w - margin && sy in margin..h - margin) {
            val r = 18f * density; val k = 7f * density
            for ((dx, dy) in listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)) {
                val corner = Offset(sx + dx * r, sy + dy * r)
                drawLine(col, corner, corner + Offset(-dx * k, 0f), 1.6f * density)
                drawLine(col, corner, corner + Offset(0f, -dy * k), 1.6f * density)
            }
            return
        }
    }
    // fora da tela ou atrás: seta na borda, na direção para onde virar
    var dx = tmpA.x; var dy = -tmpA.y
    if (abs(dx) < 1e-3f && abs(dy) < 1e-3f) dy = 1f
    val ang = atan2(dy, dx)
    val rx = w / 2 - margin; val ry = h / 2 - margin
    val t = minOf(rx / abs(cos(ang)).coerceAtLeast(1e-3f), ry / abs(sin(ang)).coerceAtLeast(1e-3f))
    val tip = Offset(w / 2 + cos(ang) * t, h / 2 + sin(ang) * t)
    val back = 14f * density; val side = 8f * density
    val bx = -cos(ang); val by = -sin(ang)
    val p = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(tip.x + bx * back - by * side, tip.y + by * back + bx * side)
        lineTo(tip.x + bx * back + by * side, tip.y + by * back - bx * side)
        close()
    }
    drawPath(p, col)
}

// ------------------------------------------------------------------ HUD de cima

@Composable
private fun FlightTopHud(s: FlightSession, frame: Long) {
    val m = s.model
    val g = s.game
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                val where = if (s.inWitchspace) "WITCHSPACE" else g.location.name.uppercase()
                T(where, 11f, if (s.inWitchspace) Noir.red else Noir.amber, Noir.mono, spacing = 2f)
                val t = s.target
                T(
                    if (t == null) "SEM DESTINO · toque em DESTINO" else "HIPER → ${t.name.uppercase()} · ${s.jumpCost()} LY",
                    10f, Noir.sub, Noir.mono,
                )
                T(stationLine(s), 10f, Noir.muted, Noir.mono)
            }
            Column(horizontalAlignment = Alignment.End) {
                T(viewLabel(m.view), 10f, Noir.teal, Noir.mono, spacing = 1.5f)
                T(credits(g.credits), 10f, Noir.sub, Noir.mono)
                if (s.kills > 0) T("${s.kills} ABATE${if (s.kills > 1) "S" else ""}", 10f, Noir.salmon, Noir.mono)
            }
        }
        Spacer(Modifier.weight(1f))
        if (m.hyperspace.active) {
            T(m.hyperspace.shown.toString(), 54f, Noir.violet, Noir.mono, weight = FontWeight.Bold, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            T("CONTAGEM DO HIPERESPAÇO", 10f, Noir.violet, spacing = 2f, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.weight(1f))
        s.message?.let {
            T(it, 12f, Noir.amber, Noir.mono, spacing = 1.5f, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        if (m.combat.missileArmed || m.combat.missileTarget != null) {
            T(if (m.combat.missileTarget != null) "MÍSSIL TRAVADO · TOQUE PARA LANÇAR" else "MÍSSIL ARMADO · MIRE", 10f, Noir.red, Noir.mono, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun viewLabel(v: View) = when (v) { View.FRONT -> "VISTA FRENTE"; View.REAR -> "VISTA TRÁS"; View.LEFT -> "VISTA ESQ."; View.RIGHT -> "VISTA DIR." }

/** Distância ao alvo da bússola, em "km" (uma unidade do Elite ≈ 1 m). */
private fun stationLine(s: FlightSession): String {
    val m = s.model
    val st = m.station
    if (st != null) return "ESTAÇÃO ${"%.1f".format(st.position.len() / 1000f)} KM" + if (st.hostile) " · HOSTIL" else ""
    val pl = m.planet ?: return "SEM PLANETA"
    return "PLANETA ${"%.0f".format(((pl.position.len() - Planet.RADIUS) / 1000f).coerceAtLeast(0f))} KM"
}

/** O mapa de bordo: destinos da região, do mais perto ao mais longe, com o que o tanque alcança. */
@Composable
private fun TargetPicker(s: FlightSession, onClose: () -> Unit, onPick: (StarSystem) -> Unit) {
    val g = s.game
    val here = g.location
    val list = g.world.region.systems.filter { it !== here }.sortedBy { g.world.region.distanceLy(here, it) }.take(14)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .78f)).clickable { onClose() }) {
        Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(20.dp).clip(RoundedCornerShape(10.dp)).background(Noir.panel).border(1.dp, Noir.line, RoundedCornerShape(10.dp)).padding(12.dp)) {
            Label("Destino do hiperespaço")
            Spacer(Modifier.height(6.dp))
            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                for (sys in list) {
                    val cost = g.fuelCost(sys)
                    val ok = g.jumpCheck(sys, if (sys === s.witchspaceTarget) cost else 0) == null || (sys === s.witchspaceTarget)
                    val ruler = g.world.controller(sys)
                    Row(Modifier.fillMaxWidth().clickable { onPick(sys) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Noir.faction(ruler)))
                        Spacer(Modifier.width(8.dp))
                        T(sys.name, 13f, if (sys === s.target) Noir.amber else if (ok) Noir.text else Noir.muted, Noir.serif, modifier = Modifier.weight(1f))
                        T("$cost LY", 11f, if (ok) Noir.sub else Noir.red, Noir.mono)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            T("Combustível: ${g.fuel} / ${g.fuelCapacity} LY", 10f, Noir.muted, Noir.mono)
        }
    }
}

// ------------------------------------------------------------------ painel

@Composable
private fun Dashboard(s: FlightSession, frame: Long) {
    val m = s.model
    val p = m.player
    val g = s.game
    Row(Modifier.fillMaxWidth().height(92.dp).background(Noir.panel).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(104.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            HudBar("ESC.D", p.foreShield / 255f, Noir.teal)
            HudBar("ESC.T", p.aftShield / 255f, Noir.teal)
            HudBar("ENERG", p.energy / 255f, if (p.energy < 64f) Noir.red else Noir.amber)
            HudBar("COMB", g.fuel / g.fuelCapacity.toFloat().coerceAtLeast(1f), Noir.violet)
            HudBar("VEL", p.speed / PlayerShip.MAX_SPEED, Noir.text)
        }
        Canvas(Modifier.weight(1f).fillMaxHeight()) { frame.let { drawScanner(s) } }
        Column(Modifier.width(96.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            HudBar("TEMP", (p.cabinTemp - 30f) / 225f, Noir.salmon)
            HudBar("LASER", p.laserTemp / 242f, Noir.red)
            HudBar("ALT", p.altitude / 255f, Noir.teal)
            Row(verticalAlignment = Alignment.CenterVertically) {
                T("MÍSS", 8f, Noir.muted, Noir.mono, modifier = Modifier.width(32.dp))
                T("■".repeat(p.missiles) + "□".repeat((FlightSession.MISSILES - p.missiles).coerceAtLeast(0)), 9f,
                    if (m.combat.missileTarget != null) Noir.red else if (m.combat.missileArmed) Noir.amber else Noir.sub, Noir.mono)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                T("BÚSS", 8f, Noir.muted, Noir.mono, modifier = Modifier.width(32.dp))
                Canvas(Modifier.size(16.dp)) { frame.let { drawCompass(s) } }
            }
        }
    }
}

@Composable
private fun HudBar(label: String, fraction: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        T(label, 8f, Noir.muted, Noir.mono, modifier = Modifier.width(32.dp))
        Bar(fraction, color, Modifier.weight(1f), height = 6)
    }
}

/** SCAN: o radar 3D "prato" do original (alcance de 64·256 em cada eixo; haste = altura). */
private fun DrawScope.drawScanner(s: FlightSession) {
    val w = size.width; val h = size.height
    // coordenadas do painel do BBC: x 60..186, y 192..250
    fun sx(x: Float) = (x - 60f) / 126f * w
    fun sy(y: Float) = (y - 192f) / 58f * h
    val cx = sx(Scanner.CENTRE_X); val cy = sy(Scanner.CENTRE_Y)
    val rx = sx(Scanner.CENTRE_X + 63f) - cx; val ry = sy(Scanner.CENTRE_Y + 16f) - cy
    drawOval(Noir.teal.copy(alpha = .08f), Offset(cx - rx, cy - ry), Size(2 * rx, 2 * ry))
    drawOval(Noir.teal.copy(alpha = .6f), Offset(cx - rx, cy - ry), Size(2 * rx, 2 * ry), style = Stroke(1f * density))
    drawLine(Noir.teal.copy(alpha = .35f), Offset(cx - rx, cy), Offset(cx + rx, cy), 1f * density)
    drawLine(Noir.teal.copy(alpha = .35f), Offset(cx, cy - ry), Offset(cx, cy + ry), 1f * density)
    drawLine(Noir.teal.copy(alpha = .35f), Offset(cx, cy), Offset(cx - rx * .7f, cy - ry * 2.2f), 1f * density)
    drawLine(Noir.teal.copy(alpha = .35f), Offset(cx, cy), Offset(cx + rx * .7f, cy - ry * 2.2f), 1f * density)
    val blip = Scanner.Blip()
    for (ship in s.model.ships) {
        if (ship.killed || !Scanner.project(ship.position, blip)) continue
        val col = when {
            ship.type.isStation -> Noir.amber
            ship.type == ShipType.MISSILE -> Color(0xFFFFF060)
            else -> rgb(ship.color)
        }
        val x = sx(blip.x); val b = sy(blip.baseY); val d = sy(blip.dotY)
        drawLine(col, Offset(x, b), Offset(x, d), 1.4f * density)
        drawRect(col, Offset(x - 2.2f * density, d - 1.5f * density), Size(4.4f * density, 3f * density))
    }
}

/** COMPAS: ponto cheio se o alvo está à frente, vazado se está atrás. */
private fun DrawScope.drawCompass(s: FlightSession) {
    val r = size.minDimension / 2
    val c = Offset(size.width / 2, size.height / 2)
    drawCircle(Noir.line, r, c)
    val t = s.model.compassTarget ?: return
    tmpA.set(t.position).nor()
    val dot = c + Offset(tmpA.x * r * .75f, -tmpA.y * r * .75f)
    val col = if (t === s.model.station) Noir.amber else PLANET
    if (tmpA.z >= 0f) drawCircle(col, r * .28f, dot) else drawCircle(col, r * .28f, dot, style = Stroke(1.2f * density))
}

// ------------------------------------------------------------------ controles de toque

@Composable
private fun ButtonStrip(s: FlightSession, frame: Long, onResult: (FlightResult) -> Unit, onTarget: () -> Unit) {
    val m = s.model
    val hyper = if (m.hyperspace.active) "HIPER ${m.hyperspace.shown}" else "HIPER"
    val missile = when {
        m.combat.missileTarget != null -> "LANÇAR"
        m.combat.missileArmed -> "DESARMAR"
        else -> "MÍSSIL ${m.player.missiles}"
    }
    Row(Modifier.fillMaxWidth().background(Noir.bg).padding(horizontal = 6.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        StripButton(hyper, Noir.violet, Modifier.weight(1.15f), lit = m.hyperspace.active) { s.toggleHyperspace() }
        StripButton("SALTO", Noir.teal, Modifier.weight(1f)) { s.inSystemJump() }
        StripButton(missile, Noir.red, Modifier.weight(1.15f), lit = m.combat.missileArmed || m.combat.missileTarget != null) { s.missile() }
        StripButton("ATRACAR", Noir.amber, Modifier.weight(1.1f), lit = m.dockingComputerBlock() == null) { s.dockingComputer()?.let(onResult) }
        StripButton(if (m.view == View.FRONT) "TRÁS" else "FRENTE", Noir.sub, Modifier.weight(1f)) { s.cycleView() }
        StripButton("DESTINO", Noir.sub, Modifier.weight(1.1f)) { onTarget() }
    }
}

@Composable
private fun StripButton(text: String, color: Color, modifier: Modifier, lit: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier.height(32.dp).clip(RoundedCornerShape(6.dp)).background(if (lit) color.copy(alpha = .22f) else Noir.panel)
            .border(1.dp, color.copy(alpha = if (lit) 1f else .55f), RoundedCornerShape(6.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { T(text, 9f, color, Noir.mono, weight = FontWeight.Bold, align = TextAlign.Center) }
}

@Composable
private fun ControlDeck(s: FlightSession, frame: Long) {
    Row(Modifier.fillMaxWidth().height(150.dp).background(Noir.bg).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Joystick(s, Modifier.size(136.dp))
        Spacer(Modifier.weight(1f))
        Throttle(s, frame, Modifier.width(40.dp).fillMaxHeight())
        Spacer(Modifier.width(14.dp))
        FireButton(s, frame, Modifier.size(96.dp))
    }
}

/** Manche: arrastar para os lados rola, para baixo puxa o nariz para cima. Soltar devolve ao centro aos poucos (cntr). */
@Composable
private fun Joystick(s: FlightSession, modifier: Modifier) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    val c = s.controls
    Box(
        modifier.clip(CircleShape).background(Noir.panel).border(1.dp, Noir.line, CircleShape)
            .pointerInput(s) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val r = size.width / 2f
                    fun move(pos: Offset) {
                        var d = pos - Offset(r, r)
                        val len = hypot(d.x, d.y)
                        if (len > r) d = d * (r / len)
                        knob = d / r
                        // zona morta pequena no centro
                        c.stickX = dead(knob.x); c.stickY = dead(knob.y)
                    }
                    move(down.position)
                    while (true) {
                        val e = awaitPointerEvent()
                        val ch = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        move(ch.position); ch.consume()
                    }
                    knob = Offset.Zero
                    c.stickX = null; c.stickY = null
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            val ctr = Offset(size.width / 2, size.height / 2)
            drawCircle(Noir.line, r * .55f, ctr, style = Stroke(1f * density))
            drawLine(Noir.line, Offset(ctr.x, ctr.y - r * .85f), Offset(ctr.x, ctr.y + r * .85f), 1f * density)
            drawLine(Noir.line, Offset(ctr.x - r * .85f, ctr.y), Offset(ctr.x + r * .85f, ctr.y), 1f * density)
            val k = ctr + knob * (r * .72f)
            drawCircle(Noir.amber.copy(alpha = .25f), r * .3f, k)
            drawCircle(Noir.amber, r * .3f, k, style = Stroke(1.6f * density))
        }
        T("MANCHE", 8f, Noir.muted, Noir.mono, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

private fun dead(v: Float): Float = if (abs(v) < .08f) 0f else (abs(v) - .08f) / .92f * kotlin.math.sign(v)

/** Acelerador: a altura do dedo é a velocidade desejada (DELTA 1..40). */
@Composable
private fun Throttle(s: FlightSession, frame: Long, modifier: Modifier) {
    val p = s.model.player
    val c = s.controls
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).background(Noir.panel).border(1.dp, Noir.line, RoundedCornerShape(8.dp))
            .pointerInput(s) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun set(y: Float) {
                        val f = (1f - y / size.height).coerceIn(0f, 1f)
                        c.targetSpeed = PlayerShip.MIN_SPEED + f * (PlayerShip.MAX_SPEED - PlayerShip.MIN_SPEED)
                    }
                    set(down.position.y)
                    while (true) {
                        val e = awaitPointerEvent()
                        val ch = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        set(ch.position.y); ch.consume()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val f = (p.speed - PlayerShip.MIN_SPEED) / (PlayerShip.MAX_SPEED - PlayerShip.MIN_SPEED)
            val top = size.height * (1f - f)
            drawRect(Noir.amber.copy(alpha = .25f), Offset(0f, top), Size(size.width, size.height - top))
            val t = c.targetSpeed?.let { (it - PlayerShip.MIN_SPEED) / (PlayerShip.MAX_SPEED - PlayerShip.MIN_SPEED) } ?: f
            val ty = size.height * (1f - t)
            drawLine(Noir.amber, Offset(0f, ty), Offset(size.width, ty), 2f * density)
        }
        T("VEL", 8f, Noir.muted, Noir.mono, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
    }
}

/** Gatilho: segurar dispara o laser da vista atual (o pulso repete sozinho; o feixe fica aceso). */
@Composable
private fun FireButton(s: FlightSession, frame: Long, modifier: Modifier) {
    val c = s.controls
    val hot = s.model.player.laserTemp / 242f
    Box(
        modifier.clip(CircleShape).background(if (c.fire) Noir.red.copy(alpha = .35f) else Noir.dangerBg)
            .border(2.dp, Noir.red, CircleShape)
            .pointerInput(s) {
                awaitEachGesture {
                    awaitFirstDown()
                    c.fire = true
                    waitForUpOrCancellation()
                    c.fire = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T("FOGO", 13f, Noir.salmon, Noir.mono, weight = FontWeight.Bold, spacing = 2f)
            if (hot > .6f) T("QUENTE", 8f, Noir.red, Noir.mono)
        }
    }
}
