package com.mercador.core.flight

/**
 * Contagem do hiperespaço (QQ22 / QQ22+1): o número na tela começa em 15; o contador
 * interno também começa em 15 e, a cada vez que zera, o número na tela cai 1 e o interno
 * volta a 5. Quando o número da tela chega a 0, os motores de hiperespaço disparam.
 * (O primeiro passo leva 15 ciclos e os seguintes, 5 — 85 ciclos ao todo.)
 */
class HyperspaceCountdown {

    /** QQ22+1: número mostrado na tela (0 = sem contagem). */
    var shown = 0
        private set
    private var internal = 0f

    val active: Boolean get() = shown > 0

    /** wW: começa a contagem (ignorado se já houver uma em andamento). */
    fun start(): Boolean {
        if (active) return false
        shown = 15
        internal = 15f
        return true
    }

    fun cancel() {
        shown = 0
        internal = 0f
    }

    /** Avança [f] ciclos; retorna true no instante em que o salto acontece. */
    fun update(f: Float): Boolean {
        if (!active) return false
        internal -= f
        while (internal <= 0f && shown > 0) {
            shown--
            internal += 5f
        }
        if (shown == 0) { internal = 0f; return true }
        return false
    }
}
