package com.mercador.ui

import com.mercador.core.flight.FlightSession
import com.mercador.core.flight.Newb
import com.mercador.core.flight.ShipInstance
import com.mercador.core.flight.ShipType
import com.mercador.core.flight.Vec3
import com.mercador.core.flight.View
import com.mercador.core.game.Game

/** Cenas de voo montadas à mão e determinísticas, para as pré-visualizações e os testes de tela. */
object FlightDemo {
    /** Logo depois de decolar, olhando para trás: a estação girando e o planeta à frente. */
    fun launch(g: Game): FlightSession {
        val s = FlightSession(g, g.reachable().first(), seed = 21)
        s.launch()
        s.controls.targetSpeed = 20f
        repeat(150) { s.update(1f / 60f) }
        s.cycleView(View.REAR)
        repeat(2) { s.update(1f / 60f) }
        return s
    }

    /** Chegada num sistema: um bando de piratas à frente, o laser aceso, um míssil travado e um tiro inimigo. */
    fun combat(g: Game): FlightSession {
        val s = FlightSession(g, g.reachable().first(), seed = 22)
        s.launch()
        val m = s.model
        m.arrive(FlightSession.systemInfo(g.location))
        m.ships.clear()
        fun put(t: ShipType, x: Float, y: Float, z: Float, nose: Vec3, up: Vec3 = Vec3(0f, 1f, 0f)) =
            ShipInstance(t, Vec3(x, y, z)).also { it.setOrientation(nose, up); m.addShip(it) }
        val lead = put(ShipType.COBRA_PIRATE, 40f, 30f, 1500f, Vec3(-.3f, -.2f, -1f), Vec3(.4f, 1f, 0f))
        lead.newb = lead.newb or Newb.PIRATE or Newb.HOSTILE
        put(ShipType.SIDEWINDER, -700f, 260f, 2600f, Vec3(.4f, -.5f, -.8f), Vec3(.6f, .8f, 0f))
        put(ShipType.MAMBA, 900f, -380f, 3400f, Vec3(-.8f, .5f, -.4f), Vec3(.3f, .6f, .7f))
        put(ShipType.CANISTER, -200f, -300f, 900f, Vec3(.3f, .8f, .2f))
        m.planet!!.position.set(-60_000f, 30_000f, 160_000f)
        m.combat.armMissile()
        s.controls.fire = true
        s.controls.targetSpeed = 30f
        repeat(2) { s.update(1f / 60f) }
        m.ships.forEach { if (it.type == ShipType.SIDEWINDER) it.firingLaser = true }
        m.player.foreShield = 150f; m.player.energy = 200f
        s.flash("BANDO DE 3 PIRATAS")
        return s
    }

    /** Witchspace: sem estrela nem estação, só os Thargoids e seus Thargons vindo. */
    fun witchspace(g: Game): FlightSession {
        val s = FlightSession(g, g.reachable().first(), seed = 23)
        s.launch()
        val m = s.model
        m.enterWitchspace()
        val spots = listOf(Vec3(-500f, 200f, 1800f), Vec3(900f, -150f, 2600f), Vec3(-1300f, -500f, 3600f), Vec3(300f, 700f, 4200f))
        m.ships.filter { it.type == ShipType.THARGOID }.forEachIndexed { i, t ->
            t.position.set(spots[i % spots.size]); t.setOrientation(Vec3(.2f * (i - 1), -.1f, -1f))
        }
        m.ships.filter { it.type == ShipType.THARGON }.forEachIndexed { i, t ->
            t.position.set(spots[i % spots.size]).add(250f, -150f, -400f)
        }
        repeat(2) { s.update(1f / 60f) }
        s.flash("WITCHSPACE! THARGOIDS")
        return s
    }
}
