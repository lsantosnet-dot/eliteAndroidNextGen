package com.mercador.core.flight

/**
 * Modelos gerados a partir de elite-source.asm (disassembly de Mark Moxon, versão cassete)
 * pelo script de extração: cabeçalho (bytes do bloco do modelo), VERTEX e EDGE na ordem original.
 * Não edite à mão.
 */
object OriginalShips {
    val MISSILE: ShipBlueprint = ShipBlueprint.define("Missile") {
        stats(energy = 2, speed = 44, laserByte = 0, bounty = 0, canisters = 0, gunVertex = 0, explosionVertices = 1)
        visibilityDistance = 14
        targetableArea = 1600
        vertex(0, 0, 68) // 0
        vertex(8, -8, 36) // 1
        vertex(8, 8, 36) // 2
        vertex(-8, 8, 36) // 3
        vertex(-8, -8, 36) // 4
        vertex(8, 8, -44) // 5
        vertex(8, -8, -44) // 6
        vertex(-8, -8, -44) // 7
        vertex(-8, 8, -44) // 8
        vertex(12, 12, -44) // 9
        vertex(12, -12, -44) // 10
        vertex(-12, -12, -44) // 11
        vertex(-12, 12, -44) // 12
        vertex(-8, 8, -12) // 13
        vertex(-8, -8, -12) // 14
        vertex(8, 8, -12) // 15
        vertex(8, -8, -12) // 16
        edge(0, 1, 1, 2, 31) // 0
        edge(0, 2, 2, 3, 31) // 1
        edge(0, 3, 0, 3, 31) // 2
        edge(0, 4, 0, 1, 31) // 3
        edge(1, 2, 4, 2, 31) // 4
        edge(1, 4, 1, 5, 31) // 5
        edge(3, 4, 0, 6, 31) // 6
        edge(2, 3, 3, 7, 31) // 7
        edge(2, 5, 4, 7, 31) // 8
        edge(1, 6, 4, 5, 31) // 9
        edge(4, 7, 5, 6, 31) // 10
        edge(3, 8, 6, 7, 31) // 11
        edge(7, 8, 6, 8, 31) // 12
        edge(5, 8, 7, 8, 31) // 13
        edge(5, 6, 4, 8, 31) // 14
        edge(6, 7, 5, 8, 31) // 15
        edge(6, 10, 5, 8, 8) // 16
        edge(5, 9, 7, 8, 8) // 17
        edge(8, 12, 7, 8, 8) // 18
        edge(7, 11, 5, 8, 8) // 19
        edge(9, 15, 4, 7, 8) // 20
        edge(10, 16, 4, 5, 8) // 21
        edge(12, 13, 6, 7, 8) // 22
        edge(11, 14, 5, 6, 8) // 23
    }

}
