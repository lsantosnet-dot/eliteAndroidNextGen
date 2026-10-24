package com.mercador.core.flight

import kotlin.math.abs

/**
 * Matemática do radar 3D "prato" (rotina SCAN), em coordenadas do painel do BBC Elite
 * (256 px de largura; y cresce para baixo, painel de 192 a 255).
 *
 *  - Só aparecem naves com |x_hi|, |y_hi|, |z_hi| ≤ 63 (coordenadas < 64·256).
 *  - Ponto:  x = 123 + x_hi
 *  - Base da haste (sobre o prato): y = 220 − z_hi/4   (205..235)
 *  - Altura da haste: −y_hi/2; o ponto fica em base − y_hi/2, limitado a 194..246.
 *
 * O radar sempre usa o referencial da vista frontal (SCAN roda dentro do MVEIT, antes do PLUT).
 */
object Scanner {

    const val CENTRE_X = 123f
    const val CENTRE_Y = 220f
    const val RANGE_HI = 63

    /** Resultado de [project]: x do ponto, y da base da haste e y do ponto. */
    class Blip(var x: Float = 0f, var baseY: Float = 0f, var dotY: Float = 0f)

    /** Retorna false se a nave estiver fora do alcance do radar. */
    fun project(pos: Vec3, out: Blip): Boolean {
        val xHi = hi(pos.x); val yHi = hi(pos.y); val zHi = hi(pos.z)
        if (abs(xHi) > RANGE_HI || abs(yHi) > RANGE_HI || abs(zHi) > RANGE_HI) return false
        out.x = CENTRE_X + xHi
        out.baseY = CENTRE_Y - divToward0(zHi, 4)
        out.dotY = (out.baseY - divToward0(yHi, 2)).coerceIn(194f, 246f)
        return true
    }

    /** Byte alto de uma coordenada sinal-magnitude de 16 bits: |v|/256 com o sinal de v. */
    private fun hi(v: Float): Int = (abs(v) / 256f).toInt().let { if (v < 0f) -it else it }

    private fun divToward0(v: Int, d: Int): Float = (abs(v) / d).let { if (v < 0) -it else it }.toFloat()
}
