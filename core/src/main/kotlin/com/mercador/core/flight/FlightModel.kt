package com.mercador.core.flight

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/** Controles lidos a cada passo (teclado, toque ou joystick). */
class FlightControls {
    var rollKey = 0
    var pitchKey = 0
    var stickX: Float? = null
    var stickY: Float? = null
    var accelerate = false
    var decelerate = false
    var targetSpeed: Float? = null
    var fire = false
}

/** Resultado de um passo de simulação. */
enum class FlightEvent { NONE, DOCKED, BUMPED, CRASHED, HYPERSPACE, STATION_APPEARED }

/**
 * O que o voo precisa saber do sistema: o governo do Elite (0 = anarquia … 7 = corporativo; aqui vem da
 * vigilância), o nível tecnológico (escolhe o arquivo de naves e a estação) e a semente (planeta e sol).
 */
class FlightSystem(val government: Int, val techLevel: Int, val seed: Long)

/**
 * A "bolha local" do universo e a física de voo do Elite original, portada do eliteAndroid:
 *
 *  - [PlayerShip.applyControls]: loop principal partes 2 e 3 (α, β, DELTA);
 *  - para cada nave: MVEIT (movimento próprio, rotação do universo pela rolagem/arfagem
 *    do jogador e avanço do jogador);
 *  - ISDK: checagem de atracação na estação;
 *  - planeta e sol (SOS1/SOLAR, MV40), surgimento da estação perto do planeta (parte 14),
 *    altitude e calor do sol (parte 15) e o salto dentro do sistema (WARP).
 *
 * O tempo é medido em "ciclos" do loop principal do original: [TICK_RATE] ciclos por segundo,
 * simulados em passos fixos de 1/[STEP_RATE] s.
 */
class FlightModel(seed: Long = Random.nextLong()) {

    val rng = Random(seed)
    val player = PlayerShip()
    val ships = ArrayList<ShipInstance>()
    var station: ShipInstance? = null
        private set
    var planet: Planet? = null
        private set
    /** O sol (só aparece ao chegar pelo hiperespaço; some quando a estação surge). */
    var sun: SpaceObject? = null
        private set
    val dust = Stardust(rng = rng)

    var view = View.FRONT
        set(value) {
            if (field != value) { field = value; dust.reset(value) } // LOOK1 → NWSTARS
        }

    private var accumulator = 0f

    /** Combate, tráfego e mísseis. */
    val combat = Combat(this, rng)

    /** Morte causada pelo combate (laser ou míssil inimigo) neste passo. */
    var playerDestroyed = false

    /** Clarão vermelho de "fomos atingidos" (1 → 0), para o HUD. */
    var hitFlash = 0f

    /** Contagem regressiva do hiperespaço (QQ22). */
    val hyperspace = HyperspaceCountdown()

    /** LOMOD: o arquivo de naves carregado. Só os tipos presentes nele podem surgir. */
    var shipFile: Map<Int, ShipBlueprint> = DiscShips.SHIP_FILES[0]
        private set

    /**
     * LOMOD: escolhe o arquivo de naves de um sistema: 2 bits aleatórios, depois governo
     * ≥ 3 (estável) e tech level ≥ 10 (interno) — "A" a "P".
     */
    fun loadShipFile(system: FlightSystem) {
        var a = rng.nextInt(256) and 3
        a = (a shl 1) or (if (system.government >= 3) 1 else 0)
        a = (a shl 1) or (if (system.techLevel >= 10) 1 else 0)
        shipFile = DiscShips.SHIP_FILES[a]
    }

    /** O tipo [type] está no arquivo de naves carregado (o míssil está sempre). */
    fun hasShip(type: ShipType): Boolean = type == ShipType.MISSILE || shipFile[type.id] === type.blueprint

    /** Tipo da estação no arquivo atual: Dodecaedro (sistemas de tech level alto) ou Coriolis. */
    val stationType: ShipType get() = if (hasShip(ShipType.DODO)) ShipType.DODO else ShipType.STATION

    /** MJ: estamos no witchspace (sem planeta, sol nem estação; só Thargoids). */
    var witchspace = false
        private set

    private fun clear(system: FlightSystem?) {
        ships.clear()
        witchspace = false
        station = null
        sun = null
        dust.active = dust.count
        combat.reset()
        system?.let { combat.government = it.government; loadShipFile(it) }
        playerDestroyed = false
        hitFlash = 0f
        hyperspace.cancel()
        view = View.FRONT
        dust.reset(view)
    }

    /**
     * TT110: sai da estação — ela fica logo atrás (z = −256) com a fenda voltada para nós,
     * e o planeta à frente (SOS1 com z_sign = 1, ou seja z = 65536). Não há sol.
     */
    fun launch(system: FlightSystem) {
        clear(system)
        planet = Planet(Vec3(0f, 0f, 65536f), hasCrater = (system.techLevel and 2) != 0)
        val st = newStation(Vec3(0f, 0f, -256f)).orient(Vec3(0f, 0f, 1f)) // ZINF + NwS1 (nosev invertido) + TIDY
        addShip(st)
        station = st
        player.reset()
    }

    /**
     * TT18 + SOLAR: chegada pelo hiperespaço. Sem estação; o planeta fica à frente e o sol
     * atrás, em posições tiradas da semente do sistema (QQ15):
     *  - planeta: z_sign = (s0_hi AND 7 + 6) / 2 (3..7), e x_sign = y_sign = z_sign/2 com o
     *    sinal vindo do bit que sobrou da divisão;
     *  - sol: z_sign = −((s1_hi AND 7) OR 1); x_sign = x_hi = s2_hi AND 3; y_sign do planeta.
     * (Cada unidade de "sign" vale 65536.)
     */
    fun arrive(system: FlightSystem) {
        clear(system)
        player.legalStatus = player.legalStatus ushr 1 // SOLAR: LSR FIST
        val s0Hi = (system.seed ushr 8 and 0xFF).toInt()
        val s1Hi = (system.seed ushr 24 and 0xFF).toInt()
        val s2Hi = (system.seed ushr 40 and 0xFF).toInt()
        val a = (s0Hi and 7) + 6
        val zSign = a ushr 1
        val xyMag = zSign ushr 1
        val xy = if (a and 1 != 0) -xyMag else xyMag
        planet = Planet(Vec3(xy * SIGN_UNIT, xy * SIGN_UNIT, zSign * SIGN_UNIT), hasCrater = (system.techLevel and 2) != 0)
        val sunX = (s2Hi and 3).let { it * SIGN_UNIT + it * 256f }
        sun = SpaceObject(Vec3(sunX, xy * SIGN_UNIT, -((s1Hi and 7) or 1) * SIGN_UNIT))
        player.resetAfterJump()
        combat.spawnCheck() // quem estava no ponto de chegada
    }

    /**
     * MJP: o salto falhou e caímos no witchspace, entre as estrelas. Não há planeta, sol nem
     * estação, a poeira estelar quase some (NOSTM = 3) e surgem 4 Thargoids, cada um com um
     * Thargon (GTHG). O status legal não é reduzido (não há SOLAR).
     */
    fun enterWitchspace() {
        clear(null)
        planet = null
        witchspace = true
        shipFile = DiscShips.SHIP_FILES[3] // MJP: arquivo D, um dos que têm Thargoids
        combat.government = 0
        while (ships.count { it.type == ShipType.THARGOID } < 4 && ships.size < NOSH - 2) combat.spawnThargoid()
        player.resetAfterJump()
        dust.active = 3
    }

    /**
     * WARP ("J"): salto dentro do sistema. No original aproxima planeta e sol em 65536; aqui o salto
     * para antes de algum corpo à frente (planeta, sol ou estação), a [JUMP_MARGIN] dele, para o resto
     * da aproximação ser pilotada. Recusado no witchspace, com naves por perto (o "lixo" não conta)
     * ou se o que está à frente já está perto demais. Retorna o motivo da recusa, ou null se saltou.
     */
    fun inSystemJump(): String? {
        if (witchspace) return "SEM ESTRELA PARA SALTAR"
        if (ships.any { !it.killed && !it.type.isJunk && !it.type.isStation && maxSign(it.position) == 0 }) return "NAVES POR PERTO"
        var step = SIGN_UNIT
        for (body in listOfNotNull(planet, sun, station)) {
            val p = body.position
            val r = if (body is Planet || body === sun) Planet.RADIUS else 0f
            // só limita o que está à frente, dentro de um cone largo
            if (p.z <= 0f || abs(p.x) > p.z + r || abs(p.y) > p.z + r) continue
            step = minOf(step, p.z - r - JUMP_MARGIN)
        }
        if (step < MIN_JUMP) return "MASSA À FRENTE"
        for (body in listOfNotNull<SpaceObject>(planet, sun)) body.position.z -= step
        for (ship in ships) ship.position.z -= step
        ships.removeAll { it !== station && maxSign(it.position) >= 1 }
        if (rng.nextInt(256) < 96) combat.spawnHostiles() // quem estava no caminho
        return null
    }

    /** Por que o hiperespaço não pode começar agora (estação perto demais), ou null. */
    fun hyperspaceBlock(): String? {
        val st = station ?: return null
        return if (st.position.len() < HYPER_LOCK) "PERTO DEMAIS DA ESTAÇÃO" else null
    }

    /** Avança o tempo real [delta] (s). Retorna o primeiro evento relevante ocorrido. */
    fun update(delta: Float, controls: FlightControls): FlightEvent {
        accumulator += delta.coerceAtMost(0.25f)
        val stepDt = 1f / STEP_RATE
        var result = FlightEvent.NONE
        while (accumulator >= stepDt) {
            accumulator -= stepDt
            val e = step(TICK_RATE / STEP_RATE, controls)
            if (e == FlightEvent.DOCKED || e == FlightEvent.CRASHED || e == FlightEvent.HYPERSPACE) { accumulator = 0f; return e }
            if (e != FlightEvent.NONE) result = e
        }
        return result
    }

    /** Disparos do laser neste passo (para o desenho do feixe). */
    var laserFired = false
        private set

    /** Um passo de [f] ciclos do original. */
    fun step(f: Float, controls: FlightControls): FlightEvent {
        player.applyControls(
            f, controls.rollKey, controls.pitchKey, controls.stickX, controls.stickY,
            controls.accelerate, controls.decelerate, controls.targetSpeed,
        )
        laserFired = player.updateLaser(f, f / TICK_RATE, controls.fire, view, rng)
        if (laserFired) combat.laserShot(player.lasers[view.ordinal])
        if (hitFlash > 0f) hitFlash = (hitFlash - f / 4f).coerceAtLeast(0f)
        val alpha = player.alpha * f
        val beta = player.beta * f
        val distance = player.speed * f

        for (ship in ships) {
            ship.moveSelf(f)
            ship.applyPlayerMotion(alpha, beta, distance)
        }
        planet?.let { it.moveSelf(f); it.applyPlayerMotion(alpha, beta, distance) }
        sun?.applyPlayerMotion(alpha, beta, distance) // MV40 + MV45: o sol não gira
        dust.update(alpha, beta, distance, view)

        // Tarefas agendadas pelo contador do loop principal (a cada 32 ciclos).
        val before = player.mainLoopCounter.toInt()
        player.updateSystems(f)
        var event = FlightEvent.NONE
        for (tick in before + 1..player.mainLoopCounter.toInt()) {
            when (tick % 32) {
                0 -> if (spawnStation()) event = FlightEvent.STATION_APPEARED
                10 -> {
                    if (player.energy < 50f) combat.messages += "ENERGIA BAIXA"
                    if (!checkAltitude()) return FlightEvent.CRASHED
                }
                20 -> if (!sunHeat()) return FlightEvent.CRASHED
            }
            if (tick % 256 == 0) combat.spawnCheck()
            // MVEIT parte 2: TACTICS a cada 8 ciclos por nave (escalonado pelo slot); mísseis sempre.
            for (ship in ships.toList()) {
                if (ship.hasAi && (ship.type == ShipType.MISSILE || ((tick xor ship.slot) and 7) == 0)) combat.tactics(ship)
            }
        }
        combat.step(f)
        // Naves que se afastam demais saem da bolha local.
        ships.removeAll { it !== station && maxSign(it.position) >= 1 }
        if (combat.checkCollisions() || playerDestroyed) return FlightEvent.CRASHED
        if (hyperspace.update(f)) return FlightEvent.HYPERSPACE

        val docking = checkDocking(f)
        return if (docking != FlightEvent.NONE) docking else event
    }

    /**
     * Parte 14: perto do planeta (todos os eixos < 65536), a estação surge em
     * planeta + 2·nosev (uma altitude igual ao raio) se esse ponto estiver a menos de
     * 192·256 em cada eixo; ela herda a orientação do planeta com o nosev invertido
     * (fenda voltada para o planeta). O sol é retirado (WPLS).
     */
    private fun spawnStation(): Boolean {
        val pl = planet ?: return false
        if (station != null || maxSign(pl.position) > 0) return false
        val spot = Vec3(pl.position).mulAdd(pl.nosev, 2f * Planet.RADIUS)
        if (abs(spot.x) >= 192f * 256f || abs(spot.y) >= 192f * 256f || abs(spot.z) >= 192f * 256f) return false
        val st = newStation(spot)
        st.nosev.set(pl.nosev).scl(-1f); st.roofv.set(pl.roofv); st.orthonormalize()
        addShip(st)
        station = st
        sun = null
        combat.messages += "ESTAÇÃO ESPACIAL À VISTA"
        return true
    }

    /**
     * Parte 15: com o planeta a menos de 65536 em todos os eixos, altitude =
     * √(d_hi² − 96²) (d_hi = distância/256; 96 = raio/256). Zero ou menos = colisão.
     * Retorna false se o jogador bateu no planeta.
     */
    private fun checkAltitude(): Boolean {
        player.altitude = 255f
        val pl = planet ?: return true
        if (maxSign(pl.position) > 0) return true
        val d = pl.position.len() / 256f
        val a2 = d * d - 96f * 96f
        if (d * d >= 65536f) return true // MAS3 estourou: ainda longe
        if (a2 <= 0f) return false
        player.altitude = sqrt(a2).coerceAtMost(255f)
        return player.altitude >= 1f
    }

    /**
     * Parte 15, iteração 20: a temperatura da cabine volta a 30 e, perto do sol (todos os
     * eixos < 65536), CABTMP = 255 − A + 30, com A = Σ(coordenada_hi²)/256 (MAS3).
     * Se passar de 255, morremos queimados. Retorna false se o jogador morreu.
     */
    private fun sunHeat(): Boolean {
        player.cabinTemp = 30f
        val s = sun ?: return true
        if (station != null || maxSign(s.position) > 0) return true
        val p = s.position
        fun sq(c: Float) = (abs(c) / 256f).toInt().let { it * it / 256 }
        val a = sq(p.x) + sq(p.y) + sq(p.z)
        if (a > 255) return true // MAS3 estourou: A = 255, CABTMP = 31
        val temp = 255 - a + 30
        if (temp > 255) return false
        player.cabinTemp = temp.toFloat()
        if (temp >= 200) combat.messages += "CALOR NA CABINE"
        return true
    }

    /**
     * Computador de atracação: com a estação não hostil e a menos de [DOCK_COMPUTER_RANGE], a atracação
     * é imediata (GOIN). Retorna o motivo da recusa, ou null.
     */
    fun dockingComputerBlock(): String? {
        val st = station ?: return "SEM ESTAÇÃO POR PERTO"
        if (st.hostile) return "A ESTAÇÃO RECUSA: HOSTIL"
        if (st.position.len() > DOCK_COMPUTER_RANGE) return "APROXIME-SE DA ESTAÇÃO"
        return null
    }

    /**
     * NWSPS: a estação do arquivo de naves, com IA e E.C.M. (%10000001) e rolagem
     * anti-horária máxima sem amortecimento (contador 255).
     */
    private fun newStation(pos: Vec3): ShipInstance {
        val st = ShipInstance(stationType, pos)
        st.aiFlag = 0b10000001
        st.rollCounter = -127f
        return st
    }

    /** Põe [ship] na bolha local sem checagens (lançamento, testes e capturas de tela). */
    fun addShip(ship: ShipInstance) {
        ship.slot = combat.nextSlot()
        ships += ship
    }

    /**
     * NWSHP: cria uma nave, se o tipo estiver no arquivo de naves e houver vaga na bolha
     * local (NOSH = 12 posições, uma delas do planeta e outra do sol ou da estação).
     */
    fun spawn(ship: ShipInstance): Boolean {
        if (!hasShip(ship.type)) return false
        val used = ships.count { !it.type.isStation } + 2
        if (used >= NOSH) return false
        addShip(ship)
        return true
    }

    /** Bússola: a estação, se existir (zona segura), senão o planeta. */
    val compassTarget: SpaceObject? get() = station ?: planet

    /**
     * Loop principal partes 7 e 9: com a estação a menos de 128 unidades em cada eixo,
     * o jogador está "dentro" dela. Atraca se estiver alinhado ([isDockingAligned]);
     * caso contrário: abaixo de velocidade 5 é só uma batida (MA67); acima, morte.
     */
    private fun checkDocking(f: Float): FlightEvent {
        val st = station ?: return FlightEvent.NONE
        val p = st.position
        if (abs(p.x) >= 128f || abs(p.y) >= 128f || abs(p.z) >= 128f) return FlightEvent.NONE

        if (isDockingAligned(st)) return FlightEvent.DOCKED
        if (player.speed < 5f) return if (player.bump(f)) FlightEvent.CRASHED else FlightEvent.BUMPED
        return FlightEvent.CRASHED
    }

    companion object {
        /** NOSH: posições da bolha local (planeta, sol/estação e naves). */
        const val NOSH = 12

        /** Uma unidade do byte de "sinal" das coordenadas de 24 bits do original. */
        const val SIGN_UNIT = 65536f

        /** O salto curto para a esta distância da superfície (ou da estação) à frente. */
        const val JUMP_MARGIN = 12_000f
        /** Saltos menores que isto não valem a pena: "massa à frente". */
        const val MIN_JUMP = 4_000f
        /** A massa da estação segura o hiperespaço até esta distância (~17 s a toda velocidade). */
        const val HYPER_LOCK = 8_192f
        /** Alcance do computador de atracação (menor que a margem do salto curto: o fim da aproximação é pilotado). */
        const val DOCK_COMPUTER_RANGE = 6_000f

        /** MAS2: maior |coordenada| em unidades de sign (0 = tudo < 65536). */
        fun maxSign(p: Vec3): Int = (maxOf(abs(p.x), abs(p.y), abs(p.z)) / SIGN_UNIT).toInt()

        /** Ciclos do loop principal por segundo (calibração; no BBC variava com a carga). */
        const val TICK_RATE = 12f
        const val STEP_RATE = 60f

        /**
         * Atracação certa:
         *  1. a estação não é hostil;
         *  2. nosev_z ≤ −86/96: a fenda aponta para nós (ângulo de aproximação < 26°);
         *  3. o vetor até a estação tem z ≥ 89/96 (cone seguro de 22°);
         *  4. |roofv_x| ≥ 80/96: a fenda está perto da horizontal.
         */
        fun isDockingAligned(st: ShipInstance): Boolean {
            if (st.hostile) return false
            if (st.nosev.z > -86f / 96f) return false
            val len = st.position.len()
            if (len > 1e-3f && st.position.z / len < 89f / 96f) return false
            return abs(st.roofv.x) >= 80f / 96f
        }
    }
}
