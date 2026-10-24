package com.mercador.core.flight

import kotlin.math.abs

/**
 * Estado de voo do jogador, com a mesma conversão do original (loop principal, parte 2):
 *
 *  - JSTX / JSTY: taxa de rolagem/arfagem do "manche", −127..+127 (no original 1..255
 *    com centro em 128). Aqui JSTX > 0 = rolar à direita e JSTY > 0 = puxar o nariz para
 *    cima ("X" no original);
 *  - ALP1 = |JSTX|/8 se |JSTX| < 32, senão |JSTX|/4  → 0..31;  α = ±ALP1/256 rad por ciclo
 *    (rolar à direita gira o universo no sentido oposto: α negativo);
 *  - BET1 = (|JSTY|+4)/16, metade se < 3  → 0..8;  β = ±BET1/256 rad por ciclo;
 *  - DELTA: velocidade 1..40 (unidades por ciclo).
 *
 * Teclado (DOKEY): cada ciclo com a tecla pressionada soma ±7 em JSTX e ±14 em JSTY;
 * sem tecla, o amortecimento (cntr) puxa JSTX 2 e JSTY 1 em direção ao centro, e apertar
 * o sentido oposto recentraliza na hora (auto-recentre).
 */
class PlayerShip {

    var jstx = 0f
        private set
    var jsty = 0f
        private set

    /** DELTA: velocidade atual (1..40). */
    var speed = 12f
        private set

    /** Energia (0..255) e escudos dianteiro/traseiro (FSH/ASH, 0..255). */
    var energy = 255f
    var foreShield = 255f
    var aftShield = 255f

    /** QQ14: combustível em décimos de ano-luz (tanque cheio = 70 = 7.0 LY). */
    var fuel = 70f
    /** CABTMP: temperatura da cabine (sobe perto do sol; 30 no espaço aberto). */
    var cabinTemp = 30f
    /** GNTMP: temperatura do laser. */
    var laserTemp = 0f
    /** ALTIT: altitude acima do planeta (255 = longe de qualquer planeta). */
    var altitude = 255f
    /** NOMSL: mísseis a bordo (o Cobra começa com 3, máximo 4). */
    var missiles = 3
    /** ECM instalado (bulbo "E" do painel). */
    var hasEcm = false

    // Equipamentos da loja (EQSHP), com os nomes das variáveis do original.
    /** BST: coletores de combustível (sol) e de contêineres. */
    var fuelScoops = false
    /** ESCP: cápsula de fuga. */
    var escapePod = false
    /** BOMB: bomba de energia (uso único). */
    var energyBomb = false
    /** ENGY: unidade de energia extra (recarga em dobro). */
    var energyUnit = false
    /** ENGY = 2: a unidade de energia da marinha (recompensa da missão 2; recarga tripla). */
    var navyEnergyUnit = false
    /** DKCMP: computador de atracação. */
    var dockingComputer = true // de fábrica neste remake (no original, item da loja)
    /** GHYP: hiperpropulsor galáctico (uso único). */
    var galacticHyperdrive = false

    /** FIST: status legal (0 = limpo, 1..49 = infrator, 50+ = fugitivo). */
    var legalStatus = 0
    /** TALLY: naves abatidas (define a classificação de combate). */
    var kills = 0

    /**
     * LASER: potência por vista (frente, trás, esquerda, direita). O Cobra sai de fábrica
     * com um laser de pulso (potência 15, [PULSE_LASER]) só na frente; o bit 7 marca o
     * laser de feixe ([BEAM_LASER]).
     */
    val lasers = intArrayOf(PULSE_LASER, 0, 0, 0)

    /** Equipamento padrão do Cobra Mk III do JAMESON. */
    fun resetEquipment() {
        lasers[0] = PULSE_LASER; lasers.fill(0, 1)
        missiles = 3
        hasEcm = false; fuelScoops = false; escapePod = false; energyBomb = false
        energyUnit = false; navyEnergyUnit = false; dockingComputer = true; galacticHyperdrive = false
    }

    /** LASCT: contagem do pulso do laser em sincronismos verticais (50 por segundo). */
    var laserCount = 0f
        private set

    /** LASX/LASY: centro aleatório (±4 px em torno do centro da vista) do último disparo. */
    var laserX = 0
        private set
    var laserY = 0
        private set

    /** O último disparo foi de um laser de feixe. */
    private var beamShot = false

    /**
     * O feixe aparece nos 3 primeiros sincronismos de cada pulso (LASCT 10..8); o laser de
     * feixe dispara a cada ciclo e fica aceso enquanto o gatilho estiver apertado.
     */
    val laserBeamVisible: Boolean get() = if (beamShot) laserCount > 0f else laserCount >= 8f

    /** MCNT: contador do loop principal (agenda recargas e o pisca do painel). */
    var mainLoopCounter = 0f
        private set

    /** α e β do ciclo atual (rad por ciclo). */
    var alpha = 0f
        private set
    var beta = 0f
        private set

    fun reset(launchSpeed: Float = LAUNCH_SPEED) {
        jstx = 0f; jsty = 0f; speed = launchSpeed
        energy = 255f; foreShield = 255f; aftShield = 255f
        cabinTemp = 30f; laserTemp = 0f; altitude = 255f
        alpha = 0f; beta = 0f; laserCount = 0f
    }

    /** RES2 após o hiperespaço: manche centralizado, laser frio; escudos e energia cheios. */
    fun resetAfterJump() {
        jstx = 0f; jsty = 0f; alpha = 0f; beta = 0f
        energy = 255f; foreShield = 255f; aftShield = 255f
        laserTemp = 0f; laserCount = 0f; altitude = 255f
    }

    /**
     * Loop principal parte 13: a cada 8 ciclos, se a energia estiver acima de 50%
     * (bit 7 de ENERGY), cada escudo recarrega 1 (SHD); depois a energia sobe 1.
     */
    fun updateSystems(f: Float) {
        val before = mainLoopCounter.toInt()
        mainLoopCounter += f
        val after = mainLoopCounter.toInt()
        for (tick in before + 1..after) {
            if (tick % 8 != 0) continue
            if (energy >= 128f) {
                aftShield = (aftShield + 1f).coerceAtMost(255f)
                foreShield = (foreShield + 1f).coerceAtMost(255f)
            }
            val engy = if (navyEnergyUnit) 2 else if (energyUnit) 1 else 0
            energy = (energy + engy + 1).coerceAtMost(255f) // ENGY + 1
        }
    }

    /**
     * Aplica os controles de um (fragmento de) ciclo [f].
     *
     * Modo teclado: [rollKey]/[pitchKey] em −1, 0, +1. Modo analógico (joystick / touch):
     * [stickX]/[stickY] em −1..1 definem JSTX/JSTY diretamente, como o joystick do original.
     */
    fun applyControls(
        f: Float,
        rollKey: Int = 0,
        pitchKey: Int = 0,
        stickX: Float? = null,
        stickY: Float? = null,
        accelerate: Boolean = false,
        decelerate: Boolean = false,
        targetSpeed: Float? = null,
    ) {
        jstx = if (stickX != null) (stickX * 127f).coerceIn(-127f, 127f)
        else keyAxis(jstx, rollKey, 7f * f, 2f * f)
        jsty = if (stickY != null) (stickY * 127f).coerceIn(-127f, 127f)
        else keyAxis(jsty, pitchKey, 14f * f, 1f * f)

        if (accelerate) speed = (speed + f).coerceAtMost(MAX_SPEED)
        if (decelerate) speed = (speed - f).coerceAtLeast(MIN_SPEED)
        if (targetSpeed != null) {
            val t = targetSpeed.coerceIn(MIN_SPEED, MAX_SPEED)
            speed = if (speed < t) (speed + f).coerceAtMost(t) else (speed - f).coerceAtLeast(t)
        }

        alpha = -alp1(jstx) / 256f * kotlin.math.sign(jstx)
        beta = bet1(jsty) / 256f * kotlin.math.sign(jsty)
    }

    /**
     * Loop principal partes 3 e 4: o laser só dispara com LASCT = 0, temperatura < 242 e
     * um laser instalado na vista atual. Cada disparo: LASCT = 10 (pulso → 5 tiros/s) ou
     * 0 (feixe → um tiro por ciclo do loop principal), GNTMP += 8, energia −1 (DENGY) e
     * centro do feixe em posição aleatória (LASLI).
     * Retorna true se disparou neste passo. [seconds] é o tempo real do passo.
     */
    fun updateLaser(f: Float, seconds: Float, firing: Boolean, view: View, rng: kotlin.random.Random = kotlin.random.Random.Default): Boolean {
        laserCount = (laserCount - 50f * seconds).coerceAtLeast(0f)
        laserTemp = (laserTemp - f).coerceAtLeast(0f) // TT100: esfria 1 por ciclo
        val power = lasers[view.ordinal]
        if (!firing || laserCount > 0f || laserTemp >= 242f || power == 0) return false
        beamShot = power and 0x80 != 0
        // Feixe: LASCT = 0, mas só volta a disparar no próximo ciclo do loop principal.
        laserCount = if (beamShot) 50f * seconds / f else (power and 0b11111010).toFloat()
        laserTemp = (laserTemp + 8f).coerceAtMost(255f)
        energy = (energy - 1f).coerceAtLeast(0f)
        laserX = 128 - 4 + rng.nextInt(8)
        laserY = 96 - 4 + rng.nextInt(8)
        return true
    }

    /** Batida leve (MA67): para a nave (DELTA = 1) e causa 5 de dano por ciclo. */
    fun bump(f: Float): Boolean {
        speed = MIN_SPEED
        return damage(5f * f, front = true)
    }

    /**
     * OOPS: o dano vai primeiro para o escudo do lado atingido; o que sobrar sai da energia.
     * Retorna true se a nave foi destruída.
     */
    fun damage(amount: Float, front: Boolean): Boolean {
        var rest = amount
        if (front) {
            val absorbed = minOf(foreShield, rest); foreShield -= absorbed; rest -= absorbed
        } else {
            val absorbed = minOf(aftShield, rest); aftShield -= absorbed; rest -= absorbed
        }
        energy -= rest
        if (rest > 0f && energy >= 0f) onHullHit()
        return energy < 0f
    }

    /** Chamado quando um dano passa dos escudos sem nos matar. */
    var onHullHit: () -> Unit = {}

    companion object {
        const val MIN_SPEED = 1f
        const val MAX_SPEED = 40f
        /** Velocidade ao sair da estação (TT110: DELTA = 12). */
        const val LAUNCH_SPEED = 12f

        /** POW: laser de pulso; o de feixe é POW + 128. */
        const val PULSE_LASER = 15
        const val BEAM_LASER = PULSE_LASER + 128
        /** Armlas = INT(128.5 + 1.5·POW): laser militar (dispara como o de feixe, potência 23). */
        const val MILITARY_LASER = 151
        /** Mlas: laser de mineração (pulsos lentos de potência 50; parte asteroides). */
        const val MINING_LASER = 50

        /** ALP1: magnitude do ângulo de rolagem (0..31) a partir de JSTX. */
        fun alp1(jstx: Float): Float {
            val a = abs(jstx).toInt()
            return (if (a < 32) a / 8 else a / 4).toFloat()
        }

        /** BET1: magnitude do ângulo de arfagem (0..8) a partir de JSTY. */
        fun bet1(jsty: Float): Float {
            var b = (abs(jsty).toInt() + 4) / 16
            if (b < 3) b /= 2
            return b.toFloat()
        }

        private fun keyAxis(value: Float, key: Int, step: Float, damping: Float): Float = when {
            // BUMP2/REDU2: se o resultado ainda estiver do lado oposto, recentraliza (auto-recentre).
            key > 0 -> (value + step).let { if (it < 0f) 0f else it }.coerceAtMost(127f)
            key < 0 -> (value - step).let { if (it > 0f) 0f else it }.coerceAtLeast(-127f)
            value > 0f -> (value - damping).coerceAtLeast(0f)                       // cntr
            value < 0f -> (value + damping).coerceAtMost(0f)
            else -> 0f
        }
    }
}
