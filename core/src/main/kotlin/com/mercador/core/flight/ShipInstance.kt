package com.mercador.core.flight


/** Aponta o nariz na direção [nose] mantendo o teto o mais próximo possível de [up]. */
fun <T : SpaceObject> T.orient(nose: Vec3, up: Vec3 = Vec3.Y): T {
    setOrientation(nose, up)
    return this
}

/**
 * Uma nave: um [SpaceObject] com modelo 3D, cor e o estado de combate do bloco INWK:
 * energia (#35), mísseis e estado de explosão (#31), aceleração (#28), flags NEWB (#36)
 * e o byte de IA (#32:
 * bit 7 = IA ligada, bits 1-6 = agressividade, bit 0 = E.C.M.; nos mísseis, bit 6 = hostil
 * a nós e bits 1-5 = alvo).
 */
class ShipInstance(
    val blueprint: ShipBlueprint,
    position: Vec3 = Vec3(),
    /** Cor RGB (0xRRGGBB) do wireframe e do radar. */
    var color: Int,
    val type: ShipType = ShipType.entries.first { it.blueprint === blueprint },
) : SpaceObject(position) {

    constructor(type: ShipType, position: Vec3) : this(type.blueprint, position, type.color, type)

    /**
     * NEWB (byte #36 da versão em disco): papel e estado da nave — ver [Newb]. Ao surgir,
     * vem da tabela E% sem os bits 4 e 7 (NW8).
     */
    var newb = type.defaultNewb and 0b01101111

    /** Bit 2 do NEWB: hostil (na estação, atracar nela é fatal). */
    var hostile: Boolean
        get() = newb and Newb.HOSTILE != 0
        set(v) { newb = if (v) newb or Newb.HOSTILE else newb and Newb.HOSTILE.inv() }

    /** Posição na bolha local (escalona o TACTICS: cada nave a cada 8 ciclos). */
    var slot = 0

    var energy = blueprint.maxEnergy.toFloat()
    var missiles = blueprint.missiles
    var acceleration = 0f
    var aiFlag = 0

    /** Míssil: alvo (null = nós, se [aiFlag] tiver o bit 6). */
    var missileTarget: ShipInstance? = null

    /**
     * Bit 6 do #31: disparando o laser contra nós. No original o bit fica ligado até o
     * próximo desenho; aqui o feixe fica aceso por um ciclo do loop principal.
     */
    var laserFlash = 0f
    var firingLaser: Boolean
        get() = laserFlash > 0f
        set(v) { laserFlash = if (v) 1f else 0f }

    /** Bit 7 do #31: destruída; a nuvem da explosão (DOEXP) cresce de 18 até 256. */
    var killed = false
    var cloudCounter = 0f

    val hasAi: Boolean get() = aiFlag and 0x80 != 0
    val hasEcm: Boolean get() = aiFlag and 1 != 0
    val isHostileMissile: Boolean get() = type == ShipType.MISSILE && aiFlag and 0x40 != 0
}

/**
 * Um objeto no espaço (nave, planeta, sol), no referencial do jogador — como no Elite
 * original, o jogador está sempre na origem olhando para +z (x à direita, y para cima) e é
 * o universo que se move.
 *
 * Equivale ao bloco de dados INWK: posição, os três vetores de orientação nosev (nariz),
 * roofv (teto) e sidev (lateral), velocidade (byte #27) e os contadores de rolagem (#29)
 * e arfagem (#30). Um ponto (x, y, z) do modelo vai para
 * `position + sidev·x + roofv·y + nosev·z`.
 *
 * Convenção do original (ZINF/TIDY): sidev = nosev × roofv.
 */
open class SpaceObject(val position: Vec3 = Vec3()) {
    // Orientação padrão do ZINF: nariz apontando para o jogador.
    val nosev = Vec3(0f, 0f, -1f)
    val roofv = Vec3(0f, 1f, 0f)
    val sidev = Vec3(1f, 0f, 0f)

    /** Velocidade própria (byte #27). Cada ciclo a nave anda nosev · speed · 96/64. */
    var speed = 0f

    /**
     * Contadores de rolagem/arfagem (bytes #29/#30): sinal = sentido, |valor| = ciclos
     * restantes girando 1/16 rad por ciclo. 127 = gira para sempre (sem amortecimento).
     */
    var rollCounter = 0f
    var pitchCounter = 0f

    private val tmp = Vec3()

    fun setOrientation(nose: Vec3, up: Vec3 = Vec3.Y) {
        nosev.set(nose).nor()
        roofv.set(up).mulAdd(nosev, -up.dot(nosev)).nor()
        sidev.set(nosev).crs(roofv)
    }

    /** Equivalente ao TIDY: mantém a base ortonormal (sidev = nosev × roofv). */
    fun orthonormalize() {
        nosev.nor()
        roofv.mulAdd(nosev, -roofv.dot(nosev)).nor()
        sidev.set(nosev).crs(roofv)
    }

    /** Transforma um ponto do modelo para o referencial do jogador. */
    fun toWorld(x: Float, y: Float, z: Float, out: Vec3): Vec3 = out.set(position)
        .mulAdd(sidev, x).mulAdd(roofv, y).mulAdd(nosev, z)

    /**
     * MVEIT partes 3 e 8: movimento e rotação da própria nave.
     * [f] é a fração de um ciclo do loop principal do original (1 = um ciclo completo).
     */
    fun moveSelf(f: Float) {
        if (speed != 0f) position.mulAdd(nosev, speed * 1.5f * f) // nosev_hi(96) · speed / 64

        if (pitchCounter != 0f) {
            mvs5(roofv, nosev, pitchCounter > 0f, f) // pitch: (roofv, nosev)
            pitchCounter = damp(pitchCounter, f)
        }
        if (rollCounter != 0f) {
            mvs5(roofv, sidev, rollCounter > 0f, f) // roll: (roofv, sidev)
            rollCounter = damp(rollCounter, f)
        }
        orthonormalize()
    }

    /**
     * MVEIT partes 5, 6 e 7: aplica a rolagem [alpha] e a arfagem [beta] do jogador
     * (radianos por ciclo, já multiplicados por f) e o avanço [playerDistance] do jogador.
     */
    fun applyPlayerMotion(alpha: Float, beta: Float, playerDistance: Float) {
        // Parte 5: gira a posição (aproximação de ângulo pequeno do original).
        val p = position
        val k2 = p.y - alpha * p.x
        p.z += beta * k2
        p.y = k2 - beta * p.z
        p.x += alpha * p.y
        // Parte 6: o jogador avança, então tudo "vem" em sua direção.
        p.z -= playerDistance
        // Parte 7: MVS4 nos três vetores de orientação.
        mvs4(nosev, alpha, beta); mvs4(roofv, alpha, beta); mvs4(sidev, alpha, beta)
    }

    private fun damp(counter: Float, f: Float): Float {
        val mag = kotlin.math.abs(counter)
        if (mag >= 127f) return counter // 127 = sem amortecimento
        val next = (mag - f).coerceAtLeast(0f)
        return if (counter > 0f) next else -next
    }

    private companion object {
        /** MVS4: rolagem (x/y) e depois arfagem (y/z) com o algoritmo de círculo de Minsky. */
        fun mvs4(v: Vec3, alpha: Float, beta: Float) {
            v.y -= alpha * v.x
            v.x += alpha * v.y
            v.y -= beta * v.z
            v.z += beta * v.y
        }

        /**
         * MVS5: gira o par (a, b) em 1/16 rad por ciclo. Sentido positivo:
         *   a = a·(1 − 1/512) + b/16 ;  b = b·(1 − 1/512) − a/16
         * (aqui com seno/cosseno exatos do ângulo f/16, idêntico no limite f = 1).
         */
        fun mvs5(a: Vec3, b: Vec3, positive: Boolean, f: Float) {
            val angle = f / 16f
            val c = kotlin.math.cos(angle)
            val s = if (positive) kotlin.math.sin(angle) else -kotlin.math.sin(angle)
            val ax = a.x; val ay = a.y; val az = a.z
            a.set(ax * c + b.x * s, ay * c + b.y * s, az * c + b.z * s)
            b.set(b.x * c - ax * s, b.y * c - ay * s, b.z * c - az * s)
        }
    }
}
