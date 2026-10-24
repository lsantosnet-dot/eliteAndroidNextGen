package com.mercador.core.flight

import kotlin.random.Random

/**
 * Poeira espacial (NOSTM = 18 partículas no espaço, como no original).
 *
 * O original calcula a poeira direto em coordenadas de tela, com uma rotina por vista
 * (STARS1 frente, STARS6 trás, STARS2 laterais). Aqui as partículas vivem em 3D no
 * referencial do jogador e recebem exatamente a mesma rotação/avanço das naves (MVEIT),
 * o que reproduz os mesmos movimentos nas quatro vistas com um só código. Quando uma
 * partícula sai da vista atual ela renasce em posição aleatória da tela, como no original.
 */
class Stardust(val count: Int = 18, private val rng: Random = Random.Default) {

    private val x = FloatArray(count)
    private val y = FloatArray(count)
    private val z = FloatArray(count)
    private val tmp = Vec3()

    /** Meia largura/altura da vista em tangente (x/z, y/z). Atualizado por quem desenha. */
    var halfTanX = 0.5f
    var halfTanY = 0.75f

    /** NOSTM: partículas em uso (o witchspace tem só 3). */
    var active = count
        set(value) { field = value.coerceIn(0, count) }

    fun reset(view: View) {
        for (i in 0 until count) respawn(i, view)
    }

    private fun rnd(a: Float, b: Float) = a + rng.nextFloat() * (b - a)

    /** Partícula aleatória dentro do volume visível da vista atual. */
    private fun respawn(i: Int, view: View) {
        val depth = rnd(NEAR * 2f, FAR)
        val vx = rnd(-halfTanX, halfTanX) * depth
        val vy = rnd(-halfTanY, halfTanY) * depth
        view.fromView(vx, vy, depth, tmp)
        x[i] = tmp.x; y[i] = tmp.y; z[i] = tmp.z
    }

    /** Mesma transformação do MVEIT partes 5 e 6. */
    fun update(alpha: Float, beta: Float, distance: Float, view: View) {
        for (i in 0 until active) {
            val k2 = y[i] - alpha * x[i]
            z[i] += beta * k2
            y[i] = k2 - beta * z[i]
            x[i] += alpha * y[i]
            z[i] -= distance

            view.toView(x[i], y[i], z[i], tmp)
            if (tmp.z < NEAR || tmp.z > FAR * 1.2f ||
                kotlin.math.abs(tmp.x) > tmp.z * halfTanX * 1.1f || kotlin.math.abs(tmp.y) > tmp.z * halfTanY * 1.1f
            ) respawn(i, view)
        }
    }

    /** Partícula [i] no referencial da vista [view] (para desenhar). */
    fun inView(i: Int, view: View, out: Vec3): Vec3 = view.toView(x[i], y[i], z[i], out)

    companion object {
        const val NEAR = 20f
        const val FAR = 1200f
    }
}
