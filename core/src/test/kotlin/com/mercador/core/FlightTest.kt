package com.mercador.core

import com.mercador.core.economy.Commodity
import com.mercador.core.flight.*
import com.mercador.core.game.Game
import com.mercador.core.game.Upgrade
import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FlightTest {
    private fun game(credits: Int = 5_000): Game {
        val world = World(Region.around(Universe(2024)))
        return Game(world, world.region.systems[0], credits = credits, stories = false)
    }

    /** Roda o voo em passos de 1/60 s até [seconds] ou até algo grande acontecer. */
    private fun FlightSession.run(seconds: Float): FlightResult? {
        var t = 0f
        while (t < seconds) {
            update(1f / 60f)?.let { return it }
            t += 1f / 60f
        }
        return null
    }

    @Test
    fun `os modelos do Elite vieram inteiros`() {
        assertEquals(16, DiscShips.SHIP_FILES.size)
        for (t in ShipType.entries) {
            assertTrue(t.blueprint.vertexCount >= 4, t.name)
            assertTrue(t.blueprint.edgeCount >= 4, t.name)
            assertTrue(t.blueprint.boundingRadius > 0f, t.name)
        }
        assertTrue(DiscShips.SHIP_FILES.all { f -> f.values.any { it === DiscShips.CORIOLIS || it === DiscShips.DODO } })
    }

    @Test
    fun `decolar poe a estacao atras e o planeta a frente, e a massa da estacao segura o hiperespaco`() {
        val g = game()
        val s = FlightSession(g, g.reachable().first(), seed = 1)
        s.launch()
        val m = s.model
        assertNotNull(m.station)
        assertTrue(m.station!!.position.z < 0f)
        assertTrue(m.planet!!.position.z > 0f)
        assertEquals(PlayerShip.PULSE_LASER, m.player.lasers[0])
        assertEquals(FlightSession.MISSILES, m.player.missiles)
        assertEquals("PERTO DEMAIS DA ESTAÇÃO", s.toggleHyperspace())
        assertFalse(m.hyperspace.active)
        // a toda velocidade, para longe da estação
        s.controls.targetSpeed = PlayerShip.MAX_SPEED
        assertNull(s.run(25f))
        assertTrue(m.station!!.position.len() > FlightModel.HYPER_LOCK)
        assertTrue(s.toggleHyperspace().startsWith("HIPERESPAÇO"))
        assertTrue(m.hyperspace.active)
        assertEquals("CONTAGEM CANCELADA", s.toggleHyperspace())
    }

    @Test
    fun `sem destino ou sem combustivel o hiperespaco nao comeca`() {
        val g = game()
        val s = FlightSession(g, null, seed = 2)
        s.launch()
        s.controls.targetSpeed = PlayerShip.MAX_SPEED
        s.run(25f)
        assertEquals("ESCOLHA UM DESTINO", s.toggleHyperspace())
        val far = g.world.region.systems.filter { it !== g.location }.maxBy { g.fuelCost(it) }
        s.selectTarget(far)
        if (g.fuelCost(far) > g.fuel) assertNotEquals(true, s.toggleHyperspace().startsWith("HIPERESPAÇO"))
    }

    /** Decola, afasta-se da estação e salta; devolve o resultado do salto. */
    private fun pilotJump(seed: Long, g: Game = game()): Pair<FlightSession, FlightResult?> {
        val dest = g.reachable().first()
        val s = FlightSession(g, dest, seed)
        s.launch()
        s.controls.targetSpeed = PlayerShip.MAX_SPEED
        s.run(25f)
        s.toggleHyperspace()
        return s to s.run(15f)
    }

    @Test
    fun `o salto pilotado leva o jogo ao destino e o voo recomeca la, longe da estacao`() {
        var found = false
        for (seed in 1L..40L) {
            val g = game()
            val dest = g.reachable().first()
            val fuel = g.fuel
            val (s, r) = pilotJump(seed, g)
            if (r !is FlightResult.Jumped) continue
            found = true
            assertSame(dest, g.location)
            assertSame(dest, r.to)
            assertEquals(fuel - r.ly, g.fuel)
            // chegada: planeta à frente, sol atrás, sem estação
            assertNull(s.model.station)
            assertNotNull(s.model.planet)
            assertNotNull(s.model.sun)
            assertFalse(s.inWitchspace)
            break
        }
        assertTrue(found, "nenhum salto completo em 40 sementes")
    }

    @Test
    fun `um salto que falha cai no witchspace com Thargoids, e o combustivel queimado conta no salto seguinte`() {
        for (seed in 1L..4000L) {
            val g = game()
            val dest = g.reachable().first()
            val fuel = g.fuel
            val cost = g.fuelCost(dest)
            val (s, r) = pilotJump(seed, g)
            if (r !is FlightResult.Misjumped) continue
            assertTrue(s.inWitchspace)
            assertNotSame(dest, g.location) // ainda não chegou
            assertEquals(fuel - (cost + 1) / 2, g.fuel)
            assertTrue(s.model.ships.count { it.type == ShipType.THARGOID } >= 1)
            assertNull(s.model.planet)
            assertEquals(cost - (cost + 1) / 2, s.jumpCost())
            // tira os Thargoids do caminho e salta de novo para o mesmo destino
            s.model.ships.clear()
            assertTrue(s.toggleHyperspace().startsWith("HIPERESPAÇO"))
            val again = s.run(15f)
            if (again is FlightResult.Misjumped) continue // não falha no witchspace, mas por garantia
            assertTrue(again is FlightResult.Jumped, "$again")
            assertSame(dest, g.location)
            assertEquals(fuel - cost, g.fuel)
            return
        }
        fail<Unit>("nenhum salto falhou em 4000 sementes")
    }

    @Test
    fun `laser abate pirata, paga a recompensa e conta o abate`() {
        val g = game()
        val s = FlightSession(g, null, seed = 3)
        s.launch()
        s.model.ships.remove(s.model.station) // longe de tudo
        val pirate = ShipInstance(ShipType.SIDEWINDER, Vec3(0f, 0f, 900f))
        pirate.newb = pirate.newb or Newb.PIRATE
        s.model.addShip(pirate)
        s.controls.targetSpeed = 1f
        val credits = g.credits
        s.controls.fire = true
        repeat(240) { if (!pirate.killed) s.update(1f / 60f) }
        assertTrue(pirate.killed)
        assertEquals(1, s.kills)
        assertEquals(credits + ShipType.SIDEWINDER.blueprint.bounty, g.credits)
        assertEquals(ShipType.SIDEWINDER.blueprint.bounty, s.bounty)
    }

    @Test
    fun `abater a policia esquenta a faccao local e suja a ficha`() {
        val g = game()
        val s = FlightSession(g, null, seed = 4)
        s.launch()
        s.model.ships.remove(s.model.station)
        val cop = ShipInstance(ShipType.VIPER, Vec3(0f, 0f, 900f))
        s.model.addShip(cop)
        val heat = g.standing.heat(g.faction)
        s.model.combat.kill(cop, byUs = true)
        assertTrue(g.standing.heat(g.faction) >= heat + FlightSession.COP_KILL_HEAT || g.standing.heat(g.faction) == 100)
        assertTrue(s.model.player.legalStatus >= 64)
    }

    @Test
    fun `o laser de feixe vem do estaleiro`() {
        val g = game(50_000)
        g.grantUpgrade(Upgrade.LASER)
        val s = FlightSession(g, null, seed = 5)
        s.launch()
        assertEquals(PlayerShip.BEAM_LASER, s.model.player.lasers[0])
    }

    /** Põe a estação logo à frente com a fenda voltada para nós e [roofX] para escolher o alinhamento. */
    private fun stationAhead(s: FlightSession, aligned: Boolean) {
        val st = s.model.station!!
        st.position.set(0f, 0f, 700f)
        st.rollCounter = 0f
        st.setOrientation(Vec3(0f, 0f, -1f), if (aligned) Vec3(1f, 0f, 0f) else Vec3(0f, 1f, 0f))
    }

    @Test
    fun `entrar alinhado na fenda atraca`() {
        val g = game()
        val s = FlightSession(g, null, seed = 6)
        s.launch()
        stationAhead(s, aligned = true)
        s.controls.targetSpeed = 10f
        val r = s.run(10f)
        assertTrue(r is FlightResult.Docked, "$r")
        assertSame(g.location, (r as FlightResult.Docked).system)
    }

    @Test
    fun `bater na estacao rapido destroi a nave, a carga se perde e a seguradora cobra`() {
        val g = game()
        val c = Commodity.values().first { g.world.quote(g.location, it).stock >= 3 && !g.isIllegalHere(it) }
        g.buy(c, 3)
        val credits = g.credits
        val s = FlightSession(g, null, seed = 7)
        s.launch()
        stationAhead(s, aligned = false)
        s.controls.targetSpeed = PlayerShip.MAX_SPEED
        val r = s.run(10f)
        assertTrue(r is FlightResult.Destroyed, "$r")
        assertTrue(g.cargo.isEmpty())
        assertTrue(g.credits < credits)
        assertTrue(g.ledger.last().text.contains("destruída"))
    }

    @Test
    fun `o computador de atracacao so funciona perto da estacao`() {
        val g = game()
        val s = FlightSession(g, null, seed = 8)
        s.launch()
        assertTrue(s.dockingComputer() is FlightResult.Docked)
        s.launch()
        s.model.station!!.position.set(0f, 0f, 40_000f)
        assertNull(s.dockingComputer())
        assertEquals("APROXIME-SE DA ESTAÇÃO", s.message)
    }

    @Test
    fun `o salto curto aproxima o planeta e para antes dele, e naves por perto o impedem`() {
        val g = game()
        val m = FlightModel(9)
        m.arrive(FlightSession.systemInfo(g.location))
        m.ships.clear()
        val before = m.planet!!.position.z
        assertNull(m.inSystemJump())
        val after = m.planet!!.position.z
        assertTrue(after < before)
        // salta até não poder mais: nunca entra no planeta
        repeat(20) { m.inSystemJump() }
        val p = m.planet!!.position
        assertTrue(p.len() > Planet.RADIUS)
        m.ships.clear()
        m.addShip(ShipInstance(ShipType.KRAIT, Vec3(0f, 0f, 3000f)))
        assertEquals("NAVES POR PERTO", m.inSystemJump())
    }

    @Test
    fun `contêiner que passa por baixo vira carga`() {
        val g = game()
        val s = FlightSession(g, null, seed = 10)
        s.launch()
        s.model.ships.remove(s.model.station)
        val free = g.holdFree
        s.model.addShip(ShipInstance(ShipType.CANISTER, Vec3(0f, -20f, 30f)))
        s.update(1f / 60f)
        assertEquals(free - 1, g.holdFree)
    }

    @Test
    fun `na chegada a estacao surge perto do planeta`() {
        val g = game()
        val m = FlightModel(11)
        m.arrive(FlightSession.systemInfo(g.location))
        m.ships.clear()
        // aproxima o planeta e espera o loop principal checar (o ponto da estação gira com o planeta)
        m.planet!!.position.set(0f, 0f, 30_000f)
        m.sun!!.position.set(0f, 0f, -400_000f)
        val c = FlightControls().apply { targetSpeed = 1f }
        var appeared = false
        repeat(3600) { if (!appeared && m.update(1f / 60f, c) == FlightEvent.STATION_APPEARED) appeared = true }
        assertTrue(appeared)
        assertNotNull(m.station)
        assertNull(m.sun)
    }
}
