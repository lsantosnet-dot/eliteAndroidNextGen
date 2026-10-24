package com.mercador.core.flight


/**
 * As quatro vistas espaciais do original (variável VIEW: 0 frente, 1 trás, 2 esquerda,
 * 3 direita). A rotina PLUT troca/inverte eixos das coordenadas antes de desenhar:
 *  - trás:     x → −x, z → −z
 *  - esquerda: x → z,  z → −x
 *  - direita:  x → −z, z → x
 */
enum class View(val label: String) {
    FRONT("FRONT VIEW"),
    REAR("REAR VIEW"),
    LEFT("LEFT VIEW"),
    RIGHT("RIGHT VIEW");

    /** Converte do referencial do jogador para o da câmera desta vista (PLUT). */
    fun toView(x: Float, y: Float, z: Float, out: Vec3): Vec3 = when (this) {
        FRONT -> out.set(x, y, z)
        REAR -> out.set(-x, y, -z)
        LEFT -> out.set(z, y, -x)
        RIGHT -> out.set(-z, y, x)
    }

    /** Inversa de [toView]. */
    fun fromView(x: Float, y: Float, z: Float, out: Vec3): Vec3 = when (this) {
        FRONT -> out.set(x, y, z)
        REAR -> out.set(-x, y, -z)
        LEFT -> out.set(-z, y, x)
        RIGHT -> out.set(z, y, -x)
    }
}
