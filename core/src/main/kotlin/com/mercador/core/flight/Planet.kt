package com.mercador.core.flight


/**
 * O planeta do sistema (tipos 128 e 130 do original). Raio fixo de 24576 (0 96 0).
 * O tipo depende do bit 1 do tech level (SOS1): 128 = equador e meridiano,
 * 130 = cratera. Gira 1/16 rad por ciclo em rolagem e arfagem (contadores 127).
 */
class Planet(position: Vec3, val hasCrater: Boolean) : SpaceObject(position) {
    init {
        rollCounter = 127f
        pitchCounter = 127f
    }

    companion object {
        const val RADIUS = 24576f
    }
}
