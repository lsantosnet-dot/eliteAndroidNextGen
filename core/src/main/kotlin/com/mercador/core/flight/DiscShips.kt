package com.mercador.core.flight

/**
 * Modelos da versão em disco do BBC Elite, gerados a partir dos arquivos de naves
 * elite-ships-a..p.asm (disassembly de Mark Moxon) por tools/extract_disc_ships.py do repositório eliteAndroid:
 * cabeçalho, VERTEX, EDGE e FACE (normais do original) na ordem original.
 * Não edite à mão.
 */
object DiscShips {
    val ADDER: ShipBlueprint = ShipBlueprint.define("Adder") {
        stats(energy = 85, speed = 24, laserByte = 16, bounty = 40, canisters = 0, gunVertex = 0, explosionVertices = 4, cargoItem = 0)
        visibilityDistance = 23
        targetableArea = 2500
        normalScale = 2
        vertex(-18, 0, 40) // 0
        vertex(18, 0, 40) // 1
        vertex(30, 0, -24) // 2
        vertex(30, 0, -40) // 3
        vertex(18, -7, -40) // 4
        vertex(-18, -7, -40) // 5
        vertex(-30, 0, -40) // 6
        vertex(-30, 0, -24) // 7
        vertex(-18, 7, -40) // 8
        vertex(18, 7, -40) // 9
        vertex(-18, 7, 13) // 10
        vertex(18, 7, 13) // 11
        vertex(-18, -7, 13) // 12
        vertex(18, -7, 13) // 13
        vertex(-11, 3, 29) // 14
        vertex(11, 3, 29) // 15
        vertex(11, 4, 24) // 16
        vertex(-11, 4, 24) // 17
        edge(0, 1, 1, 0, 31) // 0
        edge(1, 2, 3, 2, 7) // 1
        edge(2, 3, 5, 4, 31) // 2
        edge(3, 4, 6, 5, 31) // 3
        edge(4, 5, 14, 7, 31) // 4
        edge(5, 6, 10, 8, 31) // 5
        edge(6, 7, 10, 9, 31) // 6
        edge(7, 0, 12, 11, 7) // 7
        edge(3, 9, 6, 4, 31) // 8
        edge(9, 8, 13, 7, 31) // 9
        edge(8, 6, 9, 8, 31) // 10
        edge(0, 10, 11, 0, 31) // 11
        edge(7, 10, 11, 9, 31) // 12
        edge(1, 11, 2, 0, 31) // 13
        edge(2, 11, 4, 2, 31) // 14
        edge(0, 12, 12, 1, 31) // 15
        edge(7, 12, 12, 10, 31) // 16
        edge(1, 13, 3, 1, 31) // 17
        edge(2, 13, 5, 3, 31) // 18
        edge(10, 11, 13, 0, 31) // 19
        edge(12, 13, 14, 1, 31) // 20
        edge(8, 10, 13, 9, 31) // 21
        edge(9, 11, 13, 4, 31) // 22
        edge(5, 12, 14, 10, 31) // 23
        edge(4, 13, 14, 5, 31) // 24
        edge(14, 15, 0, 0, 5) // 25
        edge(15, 16, 0, 0, 3) // 26
        edge(16, 17, 0, 0, 4) // 27
        edge(17, 14, 0, 0, 3) // 28
        face(0, 39, 10, 31) // 0
        face(0, -39, 10, 31) // 1
        face(69, 50, 13, 31) // 2
        face(69, -50, 13, 31) // 3
        face(30, 52, 0, 31) // 4
        face(30, -52, 0, 31) // 5
        face(0, 0, -160, 31) // 6
        face(0, 0, -160, 31) // 7
        face(0, 0, -160, 31) // 8
        face(-30, 52, 0, 31) // 9
        face(-30, -52, 0, 31) // 10
        face(-69, 50, 13, 31) // 11
        face(-69, -50, 13, 31) // 12
        face(0, 28, 0, 31) // 13
        face(0, -28, 0, 31) // 14
    }

    val ANACONDA: ShipBlueprint = ShipBlueprint.define("Anaconda") {
        stats(energy = 252, speed = 14, laserByte = 63, bounty = 0, canisters = 7, gunVertex = 12, explosionVertices = 10, cargoItem = 0)
        visibilityDistance = 50
        targetableArea = 10000
        normalScale = 1
        vertex(0, 7, -58) // 0
        vertex(-43, -13, -37) // 1
        vertex(-26, -47, -3) // 2
        vertex(26, -47, -3) // 3
        vertex(43, -13, -37) // 4
        vertex(0, 48, -49) // 5
        vertex(-69, 15, -15) // 6
        vertex(-43, -39, 40) // 7
        vertex(43, -39, 40) // 8
        vertex(69, 15, -15) // 9
        vertex(-43, 53, -23) // 10
        vertex(-69, -1, 32) // 11
        vertex(0, 0, 254) // 12
        vertex(69, -1, 32) // 13
        vertex(43, 53, -23) // 14
        edge(0, 1, 1, 0, 30) // 0
        edge(1, 2, 2, 0, 30) // 1
        edge(2, 3, 3, 0, 30) // 2
        edge(3, 4, 4, 0, 30) // 3
        edge(0, 4, 5, 0, 30) // 4
        edge(0, 5, 5, 1, 29) // 5
        edge(1, 6, 2, 1, 29) // 6
        edge(2, 7, 3, 2, 29) // 7
        edge(3, 8, 4, 3, 29) // 8
        edge(4, 9, 5, 4, 29) // 9
        edge(5, 10, 6, 1, 30) // 10
        edge(6, 10, 7, 1, 30) // 11
        edge(6, 11, 7, 2, 30) // 12
        edge(7, 11, 8, 2, 30) // 13
        edge(7, 12, 8, 3, 31) // 14
        edge(8, 12, 9, 3, 31) // 15
        edge(8, 13, 9, 4, 30) // 16
        edge(9, 13, 10, 4, 30) // 17
        edge(9, 14, 10, 5, 30) // 18
        edge(5, 14, 6, 5, 30) // 19
        edge(10, 14, 11, 6, 30) // 20
        edge(10, 12, 11, 7, 31) // 21
        edge(11, 12, 8, 7, 31) // 22
        edge(12, 13, 10, 9, 31) // 23
        edge(12, 14, 11, 10, 31) // 24
        face(0, -51, -49, 30) // 0
        face(-51, 18, -87, 30) // 1
        face(-77, -57, -19, 30) // 2
        face(0, -90, 16, 31) // 3
        face(77, -57, -19, 30) // 4
        face(51, 18, -87, 30) // 5
        face(0, 111, -20, 30) // 6
        face(-97, 72, 24, 31) // 7
        face(-108, -68, 34, 31) // 8
        face(108, -68, 34, 31) // 9
        face(97, 72, 24, 31) // 10
        face(0, 94, 18, 31) // 11
    }

    val ASP_MK_2: ShipBlueprint = ShipBlueprint.define("Asp Mk II") {
        stats(energy = 150, speed = 40, laserByte = 41, bounty = 200, canisters = 0, gunVertex = 8, explosionVertices = 5, cargoItem = 0)
        visibilityDistance = 40
        targetableArea = 3600
        normalScale = 1
        vertex(0, -18, 0) // 0
        vertex(0, -9, -45) // 1
        vertex(43, 0, -45) // 2
        vertex(69, -3, 0) // 3
        vertex(43, -14, 28) // 4
        vertex(-43, 0, -45) // 5
        vertex(-69, -3, 0) // 6
        vertex(-43, -14, 28) // 7
        vertex(26, -7, 73) // 8
        vertex(-26, -7, 73) // 9
        vertex(43, 14, 28) // 10
        vertex(-43, 14, 28) // 11
        vertex(0, 9, -45) // 12
        vertex(-17, 0, -45) // 13
        vertex(17, 0, -45) // 14
        vertex(0, -4, -45) // 15
        vertex(0, 4, -45) // 16
        vertex(0, -7, 73) // 17
        vertex(0, -7, 83) // 18
        edge(0, 1, 2, 1, 22) // 0
        edge(0, 4, 1, 0, 22) // 1
        edge(0, 7, 2, 0, 22) // 2
        edge(1, 2, 11, 1, 31) // 3
        edge(2, 3, 6, 1, 31) // 4
        edge(3, 8, 9, 7, 16) // 5
        edge(8, 9, 4, 0, 31) // 6
        edge(6, 9, 10, 8, 16) // 7
        edge(5, 6, 5, 2, 31) // 8
        edge(1, 5, 11, 2, 31) // 9
        edge(3, 4, 7, 1, 31) // 10
        edge(4, 8, 7, 0, 31) // 11
        edge(6, 7, 8, 2, 31) // 12
        edge(7, 9, 8, 0, 31) // 13
        edge(2, 12, 11, 6, 31) // 14
        edge(5, 12, 11, 5, 31) // 15
        edge(10, 12, 6, 3, 22) // 16
        edge(11, 12, 5, 3, 22) // 17
        edge(10, 11, 4, 3, 22) // 18
        edge(6, 11, 10, 5, 31) // 19
        edge(9, 11, 10, 4, 31) // 20
        edge(3, 10, 9, 6, 31) // 21
        edge(8, 10, 9, 4, 31) // 22
        edge(13, 15, 11, 11, 10) // 23
        edge(15, 14, 11, 11, 9) // 24
        edge(14, 16, 11, 11, 8) // 25
        edge(16, 13, 11, 11, 8) // 26
        edge(18, 17, 4, 0, 10) // 27
        face(0, -35, 5, 31) // 0
        face(8, -38, -7, 31) // 1
        face(-8, -38, -7, 31) // 2
        face(0, 24, -1, 22) // 3
        face(0, 43, 19, 31) // 4
        face(-6, 28, -2, 31) // 5
        face(6, 28, -2, 31) // 6
        face(59, -64, 31, 31) // 7
        face(-59, -64, 31, 31) // 8
        face(80, 46, 50, 31) // 9
        face(-80, 46, 50, 31) // 10
        face(0, 0, -90, 31) // 11
    }

    val ASTEROID: ShipBlueprint = ShipBlueprint.define("Asteroid") {
        stats(energy = 60, speed = 30, laserByte = 0, bounty = 5, canisters = 0, gunVertex = 0, explosionVertices = 7, cargoItem = 0)
        visibilityDistance = 50
        targetableArea = 6400
        normalScale = 1
        vertex(0, 80, 0) // 0
        vertex(-80, -10, 0) // 1
        vertex(0, -80, 0) // 2
        vertex(70, -40, 0) // 3
        vertex(60, 50, 0) // 4
        vertex(50, 0, 60) // 5
        vertex(-40, 0, 70) // 6
        vertex(0, 30, -75) // 7
        vertex(0, -50, -60) // 8
        edge(0, 1, 2, 7, 31) // 0
        edge(0, 4, 6, 13, 31) // 1
        edge(3, 4, 5, 12, 31) // 2
        edge(2, 3, 4, 11, 31) // 3
        edge(1, 2, 3, 10, 31) // 4
        edge(1, 6, 2, 3, 31) // 5
        edge(2, 6, 1, 3, 31) // 6
        edge(2, 5, 1, 4, 31) // 7
        edge(5, 6, 0, 1, 31) // 8
        edge(0, 5, 0, 6, 31) // 9
        edge(3, 5, 4, 5, 31) // 10
        edge(0, 6, 0, 2, 31) // 11
        edge(4, 5, 5, 6, 31) // 12
        edge(1, 8, 8, 10, 31) // 13
        edge(1, 7, 7, 8, 31) // 14
        edge(0, 7, 7, 13, 31) // 15
        edge(4, 7, 12, 13, 31) // 16
        edge(3, 7, 9, 12, 31) // 17
        edge(3, 8, 9, 11, 31) // 18
        edge(2, 8, 10, 11, 31) // 19
        edge(7, 8, 8, 9, 31) // 20
        face(9, 66, 81, 31) // 0
        face(9, -66, 81, 31) // 1
        face(-72, 64, 31, 31) // 2
        face(-64, -73, 47, 31) // 3
        face(45, -79, 65, 31) // 4
        face(135, 15, 35, 31) // 5
        face(38, 76, 70, 31) // 6
        face(-66, 59, -39, 31) // 7
        face(-67, -15, -80, 31) // 8
        face(66, -14, -75, 31) // 9
        face(-70, -80, -40, 31) // 10
        face(58, -102, -51, 31) // 11
        face(81, 9, -67, 31) // 12
        face(47, 94, -63, 31) // 13
    }

    val BOA: ShipBlueprint = ShipBlueprint.define("Boa") {
        stats(energy = 250, speed = 24, laserByte = 28, bounty = 0, canisters = 5, gunVertex = 0, explosionVertices = 8, cargoItem = 0)
        visibilityDistance = 40
        targetableArea = 4900
        normalScale = 0
        vertex(0, 0, 93) // 0
        vertex(0, 40, -87) // 1
        vertex(38, -25, -99) // 2
        vertex(-38, -25, -99) // 3
        vertex(-38, 40, -59) // 4
        vertex(38, 40, -59) // 5
        vertex(62, 0, -67) // 6
        vertex(24, -65, -79) // 7
        vertex(-24, -65, -79) // 8
        vertex(-62, 0, -67) // 9
        vertex(0, 7, -107) // 10
        vertex(13, -9, -107) // 11
        vertex(-13, -9, -107) // 12
        edge(0, 5, 11, 6, 31) // 0
        edge(0, 7, 10, 8, 31) // 1
        edge(0, 9, 9, 7, 31) // 2
        edge(0, 4, 9, 6, 29) // 3
        edge(0, 6, 11, 8, 29) // 4
        edge(0, 8, 10, 7, 29) // 5
        edge(4, 5, 6, 3, 31) // 6
        edge(5, 6, 11, 0, 31) // 7
        edge(6, 7, 8, 4, 31) // 8
        edge(7, 8, 10, 1, 31) // 9
        edge(8, 9, 7, 5, 31) // 10
        edge(4, 9, 9, 2, 31) // 11
        edge(1, 4, 3, 2, 24) // 12
        edge(1, 5, 3, 0, 24) // 13
        edge(3, 9, 5, 2, 24) // 14
        edge(3, 8, 5, 1, 24) // 15
        edge(2, 6, 4, 0, 24) // 16
        edge(2, 7, 4, 1, 24) // 17
        edge(1, 10, 2, 0, 22) // 18
        edge(2, 11, 1, 0, 22) // 19
        edge(3, 12, 2, 1, 22) // 20
        edge(10, 11, 12, 0, 14) // 21
        edge(11, 12, 12, 1, 14) // 22
        edge(12, 10, 12, 2, 14) // 23
        face(43, 37, -60, 31) // 0
        face(0, -45, -89, 31) // 1
        face(-43, 37, -60, 31) // 2
        face(0, 40, 0, 31) // 3
        face(62, -32, -20, 31) // 4
        face(-62, -32, -20, 31) // 5
        face(0, 23, 6, 31) // 6
        face(-23, -15, 9, 31) // 7
        face(23, -15, 9, 31) // 8
        face(-26, 13, 10, 31) // 9
        face(0, -31, 12, 31) // 10
        face(26, 13, 10, 31) // 11
        face(0, 0, -107, 14) // 12
    }

    val BOULDER: ShipBlueprint = ShipBlueprint.define("Boulder") {
        stats(energy = 20, speed = 30, laserByte = 0, bounty = 1, canisters = 0, gunVertex = 0, explosionVertices = 2, cargoItem = 0)
        visibilityDistance = 20
        targetableArea = 900
        normalScale = 2
        vertex(-18, 37, -11) // 0
        vertex(30, 7, 12) // 1
        vertex(28, -7, -12) // 2
        vertex(2, 0, -39) // 3
        vertex(-28, 34, -30) // 4
        vertex(5, -10, 13) // 5
        vertex(20, 17, -30) // 6
        edge(0, 1, 5, 1, 31) // 0
        edge(1, 2, 6, 2, 31) // 1
        edge(2, 3, 7, 3, 31) // 2
        edge(3, 4, 8, 4, 31) // 3
        edge(4, 0, 9, 0, 31) // 4
        edge(0, 5, 1, 0, 31) // 5
        edge(1, 5, 2, 1, 31) // 6
        edge(2, 5, 3, 2, 31) // 7
        edge(3, 5, 4, 3, 31) // 8
        edge(4, 5, 4, 0, 31) // 9
        edge(0, 6, 9, 5, 31) // 10
        edge(1, 6, 6, 5, 31) // 11
        edge(2, 6, 7, 6, 31) // 12
        edge(3, 6, 8, 7, 31) // 13
        edge(4, 6, 9, 8, 31) // 14
        face(-15, -3, 8, 31) // 0
        face(-7, 12, 30, 31) // 1
        face(32, -47, 24, 31) // 2
        face(-3, -39, -7, 31) // 3
        face(-5, -4, -1, 31) // 4
        face(49, 84, 8, 31) // 5
        face(112, 21, -21, 31) // 6
        face(76, -35, -82, 31) // 7
        face(22, 56, -137, 31) // 8
        face(40, 110, -38, 31) // 9
    }

    val CANISTER: ShipBlueprint = ShipBlueprint.define("Canister") {
        stats(energy = 17, speed = 15, laserByte = 0, bounty = 0, canisters = 0, gunVertex = 0, explosionVertices = 3, cargoItem = 0)
        visibilityDistance = 12
        targetableArea = 400
        normalScale = 2
        vertex(24, 16, 0) // 0
        vertex(24, 5, 15) // 1
        vertex(24, -13, 9) // 2
        vertex(24, -13, -9) // 3
        vertex(24, 5, -15) // 4
        vertex(-24, 16, 0) // 5
        vertex(-24, 5, 15) // 6
        vertex(-24, -13, 9) // 7
        vertex(-24, -13, -9) // 8
        vertex(-24, 5, -15) // 9
        edge(0, 1, 0, 1, 31) // 0
        edge(1, 2, 0, 2, 31) // 1
        edge(2, 3, 0, 3, 31) // 2
        edge(3, 4, 0, 4, 31) // 3
        edge(0, 4, 0, 5, 31) // 4
        edge(0, 5, 1, 5, 31) // 5
        edge(1, 6, 1, 2, 31) // 6
        edge(2, 7, 2, 3, 31) // 7
        edge(3, 8, 3, 4, 31) // 8
        edge(4, 9, 4, 5, 31) // 9
        edge(5, 6, 1, 6, 31) // 10
        edge(6, 7, 2, 6, 31) // 11
        edge(7, 8, 3, 6, 31) // 12
        edge(8, 9, 4, 6, 31) // 13
        edge(9, 5, 5, 6, 31) // 14
        face(96, 0, 0, 31) // 0
        face(0, 41, 30, 31) // 1
        face(0, -18, 48, 31) // 2
        face(0, -51, 0, 31) // 3
        face(0, -18, -48, 31) // 4
        face(0, 41, -30, 31) // 5
        face(-96, 0, 0, 31) // 6
    }

    val COBRA_MK_1: ShipBlueprint = ShipBlueprint.define("Cobra Mk I") {
        stats(energy = 90, speed = 26, laserByte = 18, bounty = 75, canisters = 3, gunVertex = 10, explosionVertices = 5, cargoItem = 0)
        visibilityDistance = 19
        targetableArea = 9801
        normalScale = 2
        vertex(-18, -1, 50) // 0
        vertex(18, -1, 50) // 1
        vertex(-66, 0, 7) // 2
        vertex(66, 0, 7) // 3
        vertex(-32, 12, -38) // 4
        vertex(32, 12, -38) // 5
        vertex(-54, -12, -38) // 6
        vertex(54, -12, -38) // 7
        vertex(0, 12, -6) // 8
        vertex(0, -1, 50) // 9
        vertex(0, -1, 60) // 10
        edge(1, 0, 1, 0, 31) // 0
        edge(0, 2, 3, 2, 31) // 1
        edge(2, 6, 8, 3, 31) // 2
        edge(6, 7, 7, 1, 31) // 3
        edge(7, 3, 9, 5, 31) // 4
        edge(3, 1, 5, 4, 31) // 5
        edge(2, 4, 8, 2, 31) // 6
        edge(4, 5, 7, 6, 31) // 7
        edge(5, 3, 9, 4, 31) // 8
        edge(0, 8, 2, 0, 20) // 9
        edge(8, 1, 4, 0, 20) // 10
        edge(4, 8, 6, 2, 16) // 11
        edge(8, 5, 6, 4, 16) // 12
        edge(4, 6, 8, 7, 31) // 13
        edge(5, 7, 9, 7, 31) // 14
        edge(0, 6, 3, 1, 20) // 15
        edge(1, 7, 5, 1, 20) // 16
        edge(10, 9, 1, 0, 2) // 17
        face(0, 41, 10, 31) // 0
        face(0, -27, 3, 31) // 1
        face(-8, 46, 8, 31) // 2
        face(-12, -57, 12, 31) // 3
        face(8, 46, 8, 31) // 4
        face(12, -57, 12, 31) // 5
        face(0, 49, 0, 31) // 6
        face(0, 0, -154, 31) // 7
        face(-121, 111, -62, 31) // 8
        face(121, 111, -62, 31) // 9
    }

    val COBRA_MK_3: ShipBlueprint = ShipBlueprint.define("Cobra Mk III") {
        stats(energy = 150, speed = 28, laserByte = 19, bounty = 0, canisters = 3, gunVertex = 21, explosionVertices = 9, cargoItem = 0)
        visibilityDistance = 50
        targetableArea = 9025
        normalScale = 1
        vertex(32, 0, 76) // 0
        vertex(-32, 0, 76) // 1
        vertex(0, 26, 24) // 2
        vertex(-120, -3, -8) // 3
        vertex(120, -3, -8) // 4
        vertex(-88, 16, -40) // 5
        vertex(88, 16, -40) // 6
        vertex(128, -8, -40) // 7
        vertex(-128, -8, -40) // 8
        vertex(0, 26, -40) // 9
        vertex(-32, -24, -40) // 10
        vertex(32, -24, -40) // 11
        vertex(-36, 8, -40) // 12
        vertex(-8, 12, -40) // 13
        vertex(8, 12, -40) // 14
        vertex(36, 8, -40) // 15
        vertex(36, -12, -40) // 16
        vertex(8, -16, -40) // 17
        vertex(-8, -16, -40) // 18
        vertex(-36, -12, -40) // 19
        vertex(0, 0, 76) // 20
        vertex(0, 0, 90) // 21
        vertex(-80, -6, -40) // 22
        vertex(-80, 6, -40) // 23
        vertex(-88, 0, -40) // 24
        vertex(80, 6, -40) // 25
        vertex(88, 0, -40) // 26
        vertex(80, -6, -40) // 27
        edge(0, 1, 0, 11, 31) // 0
        edge(0, 4, 4, 12, 31) // 1
        edge(1, 3, 3, 10, 31) // 2
        edge(3, 8, 7, 10, 31) // 3
        edge(4, 7, 8, 12, 31) // 4
        edge(6, 7, 8, 9, 31) // 5
        edge(6, 9, 6, 9, 31) // 6
        edge(5, 9, 5, 9, 31) // 7
        edge(5, 8, 7, 9, 31) // 8
        edge(2, 5, 1, 5, 31) // 9
        edge(2, 6, 2, 6, 31) // 10
        edge(3, 5, 3, 7, 31) // 11
        edge(4, 6, 4, 8, 31) // 12
        edge(1, 2, 0, 1, 31) // 13
        edge(0, 2, 0, 2, 31) // 14
        edge(8, 10, 9, 10, 31) // 15
        edge(10, 11, 9, 11, 31) // 16
        edge(7, 11, 9, 12, 31) // 17
        edge(1, 10, 10, 11, 31) // 18
        edge(0, 11, 11, 12, 31) // 19
        edge(1, 5, 1, 3, 29) // 20
        edge(0, 6, 2, 4, 29) // 21
        edge(20, 21, 0, 11, 6) // 22
        edge(12, 13, 9, 9, 20) // 23
        edge(18, 19, 9, 9, 20) // 24
        edge(14, 15, 9, 9, 20) // 25
        edge(16, 17, 9, 9, 20) // 26
        edge(15, 16, 9, 9, 19) // 27
        edge(14, 17, 9, 9, 17) // 28
        edge(13, 18, 9, 9, 19) // 29
        edge(12, 19, 9, 9, 19) // 30
        edge(2, 9, 5, 6, 30) // 31
        edge(22, 24, 9, 9, 6) // 32
        edge(23, 24, 9, 9, 6) // 33
        edge(22, 23, 9, 9, 8) // 34
        edge(25, 26, 9, 9, 6) // 35
        edge(26, 27, 9, 9, 6) // 36
        edge(25, 27, 9, 9, 8) // 37
        face(0, 62, 31, 31) // 0
        face(-18, 55, 16, 31) // 1
        face(18, 55, 16, 31) // 2
        face(-16, 52, 14, 31) // 3
        face(16, 52, 14, 31) // 4
        face(-14, 47, 0, 31) // 5
        face(14, 47, 0, 31) // 6
        face(-61, 102, 0, 31) // 7
        face(61, 102, 0, 31) // 8
        face(0, 0, -80, 31) // 9
        face(-7, -42, 9, 31) // 10
        face(0, -30, 6, 31) // 11
        face(7, -42, 9, 31) // 12
    }

    val COBRA_MK_3_P: ShipBlueprint = ShipBlueprint.define("Cobra Mk III (pirate)") {
        stats(energy = 150, speed = 28, laserByte = 18, bounty = 175, canisters = 1, gunVertex = 21, explosionVertices = 9, cargoItem = 0)
        visibilityDistance = 50
        targetableArea = 9025
        normalScale = 1
        vertex(32, 0, 76) // 0
        vertex(-32, 0, 76) // 1
        vertex(0, 26, 24) // 2
        vertex(-120, -3, -8) // 3
        vertex(120, -3, -8) // 4
        vertex(-88, 16, -40) // 5
        vertex(88, 16, -40) // 6
        vertex(128, -8, -40) // 7
        vertex(-128, -8, -40) // 8
        vertex(0, 26, -40) // 9
        vertex(-32, -24, -40) // 10
        vertex(32, -24, -40) // 11
        vertex(-36, 8, -40) // 12
        vertex(-8, 12, -40) // 13
        vertex(8, 12, -40) // 14
        vertex(36, 8, -40) // 15
        vertex(36, -12, -40) // 16
        vertex(8, -16, -40) // 17
        vertex(-8, -16, -40) // 18
        vertex(-36, -12, -40) // 19
        vertex(0, 0, 76) // 20
        vertex(0, 0, 90) // 21
        vertex(-80, -6, -40) // 22
        vertex(-80, 6, -40) // 23
        vertex(-88, 0, -40) // 24
        vertex(80, 6, -40) // 25
        vertex(88, 0, -40) // 26
        vertex(80, -6, -40) // 27
        edge(0, 1, 0, 11, 31) // 0
        edge(0, 4, 4, 12, 31) // 1
        edge(1, 3, 3, 10, 31) // 2
        edge(3, 8, 7, 10, 31) // 3
        edge(4, 7, 8, 12, 31) // 4
        edge(6, 7, 8, 9, 31) // 5
        edge(6, 9, 6, 9, 31) // 6
        edge(5, 9, 5, 9, 31) // 7
        edge(5, 8, 7, 9, 31) // 8
        edge(2, 5, 1, 5, 31) // 9
        edge(2, 6, 2, 6, 31) // 10
        edge(3, 5, 3, 7, 31) // 11
        edge(4, 6, 4, 8, 31) // 12
        edge(1, 2, 0, 1, 31) // 13
        edge(0, 2, 0, 2, 31) // 14
        edge(8, 10, 9, 10, 31) // 15
        edge(10, 11, 9, 11, 31) // 16
        edge(7, 11, 9, 12, 31) // 17
        edge(1, 10, 10, 11, 31) // 18
        edge(0, 11, 11, 12, 31) // 19
        edge(1, 5, 1, 3, 29) // 20
        edge(0, 6, 2, 4, 29) // 21
        edge(20, 21, 0, 11, 6) // 22
        edge(12, 13, 9, 9, 20) // 23
        edge(18, 19, 9, 9, 20) // 24
        edge(14, 15, 9, 9, 20) // 25
        edge(16, 17, 9, 9, 20) // 26
        edge(15, 16, 9, 9, 19) // 27
        edge(14, 17, 9, 9, 17) // 28
        edge(13, 18, 9, 9, 19) // 29
        edge(12, 19, 9, 9, 19) // 30
        edge(2, 9, 5, 6, 30) // 31
        edge(22, 24, 9, 9, 6) // 32
        edge(23, 24, 9, 9, 6) // 33
        edge(22, 23, 9, 9, 8) // 34
        edge(25, 26, 9, 9, 6) // 35
        edge(26, 27, 9, 9, 6) // 36
        edge(25, 27, 9, 9, 8) // 37
        face(0, 62, 31, 31) // 0
        face(-18, 55, 16, 31) // 1
        face(18, 55, 16, 31) // 2
        face(-16, 52, 14, 31) // 3
        face(16, 52, 14, 31) // 4
        face(-14, 47, 0, 31) // 5
        face(14, 47, 0, 31) // 6
        face(-61, 102, 0, 31) // 7
        face(61, 102, 0, 31) // 8
        face(0, 0, -80, 31) // 9
        face(-7, -42, 9, 31) // 10
        face(0, -30, 6, 31) // 11
        face(7, -42, 9, 31) // 12
    }

    val CONSTRICTOR: ShipBlueprint = ShipBlueprint.define("Constrictor") {
        stats(energy = 252, speed = 36, laserByte = 52, bounty = 0, canisters = 3, gunVertex = 0, explosionVertices = 10, cargoItem = 0)
        visibilityDistance = 45
        targetableArea = 4225
        normalScale = 2
        vertex(20, -7, 80) // 0
        vertex(-20, -7, 80) // 1
        vertex(-54, -7, 40) // 2
        vertex(-54, -7, -40) // 3
        vertex(-20, 13, -40) // 4
        vertex(20, 13, -40) // 5
        vertex(54, -7, -40) // 6
        vertex(54, -7, 40) // 7
        vertex(20, 13, 5) // 8
        vertex(-20, 13, 5) // 9
        vertex(20, -7, 62) // 10
        vertex(-20, -7, 62) // 11
        vertex(25, -7, -25) // 12
        vertex(-25, -7, -25) // 13
        vertex(15, -7, -15) // 14
        vertex(-15, -7, -15) // 15
        vertex(0, -7, 0) // 16
        edge(0, 1, 9, 0, 31) // 0
        edge(1, 2, 9, 1, 31) // 1
        edge(1, 9, 1, 0, 31) // 2
        edge(0, 8, 2, 0, 31) // 3
        edge(0, 7, 9, 2, 31) // 4
        edge(7, 8, 3, 2, 31) // 5
        edge(2, 9, 4, 1, 31) // 6
        edge(2, 3, 9, 4, 31) // 7
        edge(6, 7, 9, 3, 31) // 8
        edge(6, 8, 7, 3, 31) // 9
        edge(5, 8, 7, 6, 31) // 10
        edge(4, 9, 6, 5, 31) // 11
        edge(3, 9, 5, 4, 31) // 12
        edge(3, 4, 8, 5, 31) // 13
        edge(4, 5, 8, 6, 31) // 14
        edge(5, 6, 8, 7, 31) // 15
        edge(3, 6, 9, 8, 31) // 16
        edge(8, 9, 6, 0, 31) // 17
        edge(10, 12, 9, 9, 18) // 18
        edge(12, 14, 9, 9, 5) // 19
        edge(14, 10, 9, 9, 10) // 20
        edge(11, 15, 9, 9, 10) // 21
        edge(13, 15, 9, 9, 5) // 22
        edge(11, 13, 9, 9, 18) // 23
        face(0, 55, 15, 31) // 0
        face(-24, 75, 20, 31) // 1
        face(24, 75, 20, 31) // 2
        face(44, 75, 0, 31) // 3
        face(-44, 75, 0, 31) // 4
        face(-44, 75, 0, 31) // 5
        face(0, 53, 0, 31) // 6
        face(44, 75, 0, 31) // 7
        face(0, 0, -160, 31) // 8
        face(0, -27, 0, 31) // 9
    }

    val CORIOLIS: ShipBlueprint = ShipBlueprint.define("Coriolis") {
        stats(energy = 240, speed = 0, laserByte = 6, bounty = 0, canisters = 0, gunVertex = 0, explosionVertices = 12, cargoItem = 0)
        visibilityDistance = 120
        targetableArea = 25600
        normalScale = 0
        vertex(160, 0, 160) // 0
        vertex(0, 160, 160) // 1
        vertex(-160, 0, 160) // 2
        vertex(0, -160, 160) // 3
        vertex(160, -160, 0) // 4
        vertex(160, 160, 0) // 5
        vertex(-160, 160, 0) // 6
        vertex(-160, -160, 0) // 7
        vertex(160, 0, -160) // 8
        vertex(0, 160, -160) // 9
        vertex(-160, 0, -160) // 10
        vertex(0, -160, -160) // 11
        vertex(10, -30, 160) // 12
        vertex(10, 30, 160) // 13
        vertex(-10, 30, 160) // 14
        vertex(-10, -30, 160) // 15
        edge(0, 3, 0, 1, 31) // 0
        edge(0, 1, 0, 2, 31) // 1
        edge(1, 2, 0, 3, 31) // 2
        edge(2, 3, 0, 4, 31) // 3
        edge(3, 4, 1, 5, 31) // 4
        edge(0, 4, 1, 6, 31) // 5
        edge(0, 5, 2, 6, 31) // 6
        edge(5, 1, 2, 8, 31) // 7
        edge(1, 6, 3, 8, 31) // 8
        edge(2, 6, 3, 7, 31) // 9
        edge(2, 7, 4, 7, 31) // 10
        edge(3, 7, 4, 5, 31) // 11
        edge(8, 11, 10, 13, 31) // 12
        edge(8, 9, 11, 13, 31) // 13
        edge(9, 10, 12, 13, 31) // 14
        edge(10, 11, 9, 13, 31) // 15
        edge(4, 11, 5, 10, 31) // 16
        edge(4, 8, 6, 10, 31) // 17
        edge(5, 8, 6, 11, 31) // 18
        edge(5, 9, 8, 11, 31) // 19
        edge(6, 9, 8, 12, 31) // 20
        edge(6, 10, 7, 12, 31) // 21
        edge(7, 10, 7, 9, 31) // 22
        edge(7, 11, 5, 9, 31) // 23
        edge(12, 13, 0, 0, 30) // 24
        edge(13, 14, 0, 0, 30) // 25
        edge(14, 15, 0, 0, 30) // 26
        edge(15, 12, 0, 0, 30) // 27
        face(0, 0, 160, 31) // 0
        face(107, -107, 107, 31) // 1
        face(107, 107, 107, 31) // 2
        face(-107, 107, 107, 31) // 3
        face(-107, -107, 107, 31) // 4
        face(0, -160, 0, 31) // 5
        face(160, 0, 0, 31) // 6
        face(-160, 0, 0, 31) // 7
        face(0, 160, 0, 31) // 8
        face(-107, -107, -107, 31) // 9
        face(107, -107, -107, 31) // 10
        face(107, 107, -107, 31) // 11
        face(-107, 107, -107, 31) // 12
        face(0, 0, -160, 31) // 13
    }

    val DODO: ShipBlueprint = ShipBlueprint.define("Dodecahedron") {
        stats(energy = 240, speed = 0, laserByte = 0, bounty = 0, canisters = 0, gunVertex = 0, explosionVertices = 12, cargoItem = 0)
        visibilityDistance = 125
        targetableArea = 32400
        normalScale = 0
        vertex(0, 150, 196) // 0
        vertex(143, 46, 196) // 1
        vertex(88, -121, 196) // 2
        vertex(-88, -121, 196) // 3
        vertex(-143, 46, 196) // 4
        vertex(0, 243, 46) // 5
        vertex(231, 75, 46) // 6
        vertex(143, -196, 46) // 7
        vertex(-143, -196, 46) // 8
        vertex(-231, 75, 46) // 9
        vertex(143, 196, -46) // 10
        vertex(231, -75, -46) // 11
        vertex(0, -243, -46) // 12
        vertex(-231, -75, -46) // 13
        vertex(-143, 196, -46) // 14
        vertex(88, 121, -196) // 15
        vertex(143, -46, -196) // 16
        vertex(0, -150, -196) // 17
        vertex(-143, -46, -196) // 18
        vertex(-88, 121, -196) // 19
        vertex(-16, 32, 196) // 20
        vertex(-16, -32, 196) // 21
        vertex(16, 32, 196) // 22
        vertex(16, -32, 196) // 23
        edge(0, 1, 1, 0, 31) // 0
        edge(1, 2, 2, 0, 31) // 1
        edge(2, 3, 3, 0, 31) // 2
        edge(3, 4, 4, 0, 31) // 3
        edge(4, 0, 5, 0, 31) // 4
        edge(5, 10, 6, 1, 31) // 5
        edge(10, 6, 7, 1, 31) // 6
        edge(6, 11, 7, 2, 31) // 7
        edge(11, 7, 8, 2, 31) // 8
        edge(7, 12, 8, 3, 31) // 9
        edge(12, 8, 9, 3, 31) // 10
        edge(8, 13, 9, 4, 31) // 11
        edge(13, 9, 10, 4, 31) // 12
        edge(9, 14, 10, 5, 31) // 13
        edge(14, 5, 6, 5, 31) // 14
        edge(15, 16, 11, 7, 31) // 15
        edge(16, 17, 11, 8, 31) // 16
        edge(17, 18, 11, 9, 31) // 17
        edge(18, 19, 11, 10, 31) // 18
        edge(19, 15, 11, 6, 31) // 19
        edge(0, 5, 5, 1, 31) // 20
        edge(1, 6, 2, 1, 31) // 21
        edge(2, 7, 3, 2, 31) // 22
        edge(3, 8, 4, 3, 31) // 23
        edge(4, 9, 5, 4, 31) // 24
        edge(10, 15, 7, 6, 31) // 25
        edge(11, 16, 8, 7, 31) // 26
        edge(12, 17, 9, 8, 31) // 27
        edge(13, 18, 10, 9, 31) // 28
        edge(14, 19, 10, 6, 31) // 29
        edge(20, 21, 0, 0, 30) // 30
        edge(21, 23, 0, 0, 20) // 31
        edge(23, 22, 0, 0, 23) // 32
        edge(22, 20, 0, 0, 20) // 33
        face(0, 0, 196, 31) // 0
        face(103, 142, 88, 31) // 1
        face(169, -55, 89, 31) // 2
        face(0, -176, 88, 31) // 3
        face(-169, -55, 89, 31) // 4
        face(-103, 142, 88, 31) // 5
        face(0, 176, -88, 31) // 6
        face(169, 55, -89, 31) // 7
        face(103, -142, -88, 31) // 8
        face(-103, -142, -88, 31) // 9
        face(-169, 55, -89, 31) // 10
        face(0, 0, -196, 31) // 11
    }

    val ESCAPE_POD: ShipBlueprint = ShipBlueprint.define("Escape Pod") {
        stats(energy = 17, speed = 8, laserByte = 0, bounty = 0, canisters = 0, gunVertex = 0, explosionVertices = 4, cargoItem = 2)
        visibilityDistance = 8
        targetableArea = 256
        normalScale = 4
        vertex(-7, 0, 36) // 0
        vertex(-7, -14, -12) // 1
        vertex(-7, 14, -12) // 2
        vertex(21, 0, 0) // 3
        edge(0, 1, 3, 2, 31) // 0
        edge(1, 2, 3, 0, 31) // 1
        edge(2, 3, 1, 0, 31) // 2
        edge(3, 0, 2, 1, 31) // 3
        edge(0, 2, 3, 1, 31) // 4
        edge(3, 1, 2, 0, 31) // 5
        face(52, 0, -122, 31) // 0
        face(39, 103, 30, 31) // 1
        face(39, -103, 30, 31) // 2
        face(-112, 0, 0, 31) // 3
    }

    val FER_DE_LANCE: ShipBlueprint = ShipBlueprint.define("Fer-de-Lance") {
        stats(energy = 160, speed = 30, laserByte = 18, bounty = 0, canisters = 0, gunVertex = 0, explosionVertices = 5, cargoItem = 0)
        visibilityDistance = 40
        targetableArea = 1600
        normalScale = 1
        vertex(0, -14, 108) // 0
        vertex(-40, -14, -4) // 1
        vertex(-12, -14, -52) // 2
        vertex(12, -14, -52) // 3
        vertex(40, -14, -4) // 4
        vertex(-40, 14, -4) // 5
        vertex(-12, 2, -52) // 6
        vertex(12, 2, -52) // 7
        vertex(40, 14, -4) // 8
        vertex(0, 18, -20) // 9
        vertex(-3, -11, 97) // 10
        vertex(-26, 8, 18) // 11
        vertex(-16, 14, -4) // 12
        vertex(3, -11, 97) // 13
        vertex(26, 8, 18) // 14
        vertex(16, 14, -4) // 15
        vertex(0, -14, -20) // 16
        vertex(-14, -14, 44) // 17
        vertex(14, -14, 44) // 18
        edge(0, 1, 9, 1, 31) // 0
        edge(1, 2, 9, 2, 31) // 1
        edge(2, 3, 9, 3, 31) // 2
        edge(3, 4, 9, 4, 31) // 3
        edge(0, 4, 9, 5, 31) // 4
        edge(0, 5, 1, 0, 28) // 5
        edge(5, 6, 6, 2, 28) // 6
        edge(6, 7, 7, 3, 28) // 7
        edge(7, 8, 8, 4, 28) // 8
        edge(0, 8, 5, 0, 28) // 9
        edge(5, 9, 6, 0, 15) // 10
        edge(6, 9, 7, 6, 11) // 11
        edge(7, 9, 8, 7, 11) // 12
        edge(8, 9, 8, 0, 15) // 13
        edge(1, 5, 2, 1, 14) // 14
        edge(2, 6, 3, 2, 14) // 15
        edge(3, 7, 4, 3, 14) // 16
        edge(4, 8, 5, 4, 14) // 17
        edge(10, 11, 0, 0, 8) // 18
        edge(11, 12, 0, 0, 9) // 19
        edge(10, 12, 0, 0, 11) // 20
        edge(13, 14, 0, 0, 8) // 21
        edge(14, 15, 0, 0, 9) // 22
        edge(13, 15, 0, 0, 11) // 23
        edge(16, 17, 9, 9, 12) // 24
        edge(16, 18, 9, 9, 12) // 25
        edge(17, 18, 9, 9, 8) // 26
        face(0, 24, 6, 28) // 0
        face(-68, 0, 24, 31) // 1
        face(-63, 0, -37, 31) // 2
        face(0, 0, -104, 31) // 3
        face(63, 0, -37, 31) // 4
        face(68, 0, 24, 31) // 5
        face(-12, 46, -19, 28) // 6
        face(0, 45, -22, 28) // 7
        face(12, 46, -19, 28) // 8
        face(0, -28, 0, 31) // 9
    }

    val GECKO: ShipBlueprint = ShipBlueprint.define("Gecko") {
        stats(energy = 70, speed = 30, laserByte = 16, bounty = 55, canisters = 0, gunVertex = 0, explosionVertices = 5, cargoItem = 0)
        visibilityDistance = 18
        targetableArea = 9801
        normalScale = 3
        vertex(-10, -4, 47) // 0
        vertex(10, -4, 47) // 1
        vertex(-16, 8, -23) // 2
        vertex(16, 8, -23) // 3
        vertex(-66, 0, -3) // 4
        vertex(66, 0, -3) // 5
        vertex(-20, -14, -23) // 6
        vertex(20, -14, -23) // 7
        vertex(-8, -6, 33) // 8
        vertex(8, -6, 33) // 9
        vertex(-8, -13, -16) // 10
        vertex(8, -13, -16) // 11
        edge(0, 1, 3, 0, 31) // 0
        edge(1, 5, 2, 1, 31) // 1
        edge(5, 3, 8, 1, 31) // 2
        edge(3, 2, 7, 0, 31) // 3
        edge(2, 4, 6, 5, 31) // 4
        edge(4, 0, 5, 4, 31) // 5
        edge(5, 7, 8, 2, 31) // 6
        edge(7, 6, 7, 3, 31) // 7
        edge(6, 4, 6, 4, 31) // 8
        edge(0, 2, 5, 0, 29) // 9
        edge(1, 3, 1, 0, 30) // 10
        edge(0, 6, 4, 3, 29) // 11
        edge(1, 7, 3, 2, 30) // 12
        edge(2, 6, 7, 6, 20) // 13
        edge(3, 7, 8, 7, 20) // 14
        edge(8, 10, 3, 3, 16) // 15
        edge(9, 11, 3, 3, 17) // 16
        face(0, 31, 5, 31) // 0
        face(4, 45, 8, 31) // 1
        face(25, -108, 19, 31) // 2
        face(0, -84, 12, 31) // 3
        face(-25, -108, 19, 31) // 4
        face(-4, 45, 8, 31) // 5
        face(-88, 16, -214, 31) // 6
        face(0, 0, -187, 31) // 7
        face(88, 16, -214, 31) // 8
    }

    val KRAIT: ShipBlueprint = ShipBlueprint.define("Krait") {
        stats(energy = 80, speed = 30, laserByte = 16, bounty = 100, canisters = 1, gunVertex = 0, explosionVertices = 3, cargoItem = 0)
        visibilityDistance = 25
        targetableArea = 3600
        normalScale = 1
        vertex(0, 0, 96) // 0
        vertex(0, 18, -48) // 1
        vertex(0, -18, -48) // 2
        vertex(90, 0, -3) // 3
        vertex(-90, 0, -3) // 4
        vertex(90, 0, 87) // 5
        vertex(-90, 0, 87) // 6
        vertex(0, 5, 53) // 7
        vertex(0, 7, 38) // 8
        vertex(-18, 7, 19) // 9
        vertex(18, 7, 19) // 10
        vertex(18, 11, -39) // 11
        vertex(18, -11, -39) // 12
        vertex(36, 0, -30) // 13
        vertex(-18, 11, -39) // 14
        vertex(-18, -11, -39) // 15
        vertex(-36, 0, -30) // 16
        edge(0, 1, 3, 0, 31) // 0
        edge(0, 2, 2, 1, 31) // 1
        edge(0, 3, 1, 0, 31) // 2
        edge(0, 4, 3, 2, 31) // 3
        edge(1, 4, 5, 3, 31) // 4
        edge(4, 2, 5, 2, 31) // 5
        edge(2, 3, 4, 1, 31) // 6
        edge(3, 1, 4, 0, 31) // 7
        edge(3, 5, 1, 0, 30) // 8
        edge(4, 6, 3, 2, 30) // 9
        edge(1, 2, 5, 4, 8) // 10
        edge(7, 10, 0, 0, 9) // 11
        edge(8, 10, 0, 0, 6) // 12
        edge(7, 9, 3, 3, 9) // 13
        edge(8, 9, 3, 3, 6) // 14
        edge(11, 13, 4, 4, 8) // 15
        edge(13, 12, 4, 4, 8) // 16
        edge(12, 11, 4, 4, 7) // 17
        edge(14, 15, 5, 5, 7) // 18
        edge(15, 16, 5, 5, 8) // 19
        edge(16, 14, 5, 5, 8) // 20
        face(3, 24, 3, 31) // 0
        face(3, -24, 3, 31) // 1
        face(-3, -24, 3, 31) // 2
        face(-3, 24, 3, 31) // 3
        face(38, 0, -77, 31) // 4
        face(-38, 0, -77, 31) // 5
    }

    val MAMBA: ShipBlueprint = ShipBlueprint.define("Mamba") {
        stats(energy = 90, speed = 30, laserByte = 18, bounty = 150, canisters = 1, gunVertex = 0, explosionVertices = 7, cargoItem = 0)
        visibilityDistance = 25
        targetableArea = 4900
        normalScale = 2
        vertex(0, 0, 64) // 0
        vertex(-64, -8, -32) // 1
        vertex(-32, 8, -32) // 2
        vertex(32, 8, -32) // 3
        vertex(64, -8, -32) // 4
        vertex(-4, 4, 16) // 5
        vertex(4, 4, 16) // 6
        vertex(8, 3, 28) // 7
        vertex(-8, 3, 28) // 8
        vertex(-20, -4, 16) // 9
        vertex(20, -4, 16) // 10
        vertex(-24, -7, -20) // 11
        vertex(-16, -7, -20) // 12
        vertex(16, -7, -20) // 13
        vertex(24, -7, -20) // 14
        vertex(-8, 4, -32) // 15
        vertex(8, 4, -32) // 16
        vertex(8, -4, -32) // 17
        vertex(-8, -4, -32) // 18
        vertex(-32, 4, -32) // 19
        vertex(32, 4, -32) // 20
        vertex(36, -4, -32) // 21
        vertex(-36, -4, -32) // 22
        vertex(-38, 0, -32) // 23
        vertex(38, 0, -32) // 24
        edge(0, 1, 0, 2, 31) // 0
        edge(0, 4, 0, 3, 31) // 1
        edge(1, 4, 0, 4, 31) // 2
        edge(1, 2, 2, 4, 30) // 3
        edge(2, 3, 1, 4, 30) // 4
        edge(3, 4, 3, 4, 30) // 5
        edge(5, 6, 1, 1, 14) // 6
        edge(6, 7, 1, 1, 12) // 7
        edge(7, 8, 1, 1, 13) // 8
        edge(5, 8, 1, 1, 12) // 9
        edge(9, 11, 0, 0, 20) // 10
        edge(9, 12, 0, 0, 16) // 11
        edge(10, 13, 0, 0, 16) // 12
        edge(10, 14, 0, 0, 20) // 13
        edge(13, 14, 0, 0, 14) // 14
        edge(11, 12, 0, 0, 14) // 15
        edge(15, 16, 4, 4, 13) // 16
        edge(17, 18, 4, 4, 14) // 17
        edge(15, 18, 4, 4, 12) // 18
        edge(16, 17, 4, 4, 12) // 19
        edge(20, 21, 4, 4, 7) // 20
        edge(20, 24, 4, 4, 5) // 21
        edge(21, 24, 4, 4, 5) // 22
        edge(19, 22, 4, 4, 7) // 23
        edge(19, 23, 4, 4, 5) // 24
        edge(22, 23, 4, 4, 5) // 25
        edge(0, 2, 1, 2, 30) // 26
        edge(0, 3, 1, 3, 30) // 27
        face(0, -24, 2, 30) // 0
        face(0, 24, 2, 30) // 1
        face(-32, 64, 16, 30) // 2
        face(32, 64, 16, 30) // 3
        face(0, 0, -127, 30) // 4
    }

    val MORAY: ShipBlueprint = ShipBlueprint.define("Moray") {
        stats(energy = 100, speed = 25, laserByte = 16, bounty = 50, canisters = 1, gunVertex = 0, explosionVertices = 5, cargoItem = 0)
        visibilityDistance = 40
        targetableArea = 900
        normalScale = 2
        vertex(15, 0, 65) // 0
        vertex(-15, 0, 65) // 1
        vertex(0, 18, -40) // 2
        vertex(-60, 0, 0) // 3
        vertex(60, 0, 0) // 4
        vertex(30, -27, -10) // 5
        vertex(-30, -27, -10) // 6
        vertex(-9, -4, -25) // 7
        vertex(9, -4, -25) // 8
        vertex(0, -18, -16) // 9
        vertex(13, 3, 49) // 10
        vertex(6, 0, 65) // 11
        vertex(-13, 3, 49) // 12
        vertex(-6, 0, 65) // 13
        edge(0, 1, 7, 0, 31) // 0
        edge(1, 3, 6, 1, 31) // 1
        edge(3, 6, 6, 3, 24) // 2
        edge(5, 6, 7, 4, 24) // 3
        edge(4, 5, 8, 5, 24) // 4
        edge(0, 4, 8, 2, 31) // 5
        edge(1, 6, 7, 6, 15) // 6
        edge(0, 5, 8, 7, 15) // 7
        edge(0, 2, 2, 0, 15) // 8
        edge(1, 2, 1, 0, 15) // 9
        edge(2, 3, 3, 1, 17) // 10
        edge(2, 4, 5, 2, 17) // 11
        edge(2, 5, 5, 4, 13) // 12
        edge(2, 6, 4, 3, 13) // 13
        edge(7, 8, 4, 4, 5) // 14
        edge(7, 9, 4, 4, 7) // 15
        edge(8, 9, 4, 4, 7) // 16
        edge(10, 11, 0, 0, 5) // 17
        edge(12, 13, 0, 0, 5) // 18
        face(0, 43, 7, 31) // 0
        face(-10, 49, 7, 31) // 1
        face(10, 49, 7, 31) // 2
        face(-59, -28, -101, 24) // 3
        face(0, -52, -78, 24) // 4
        face(59, -28, -101, 24) // 5
        face(-72, -99, 50, 31) // 6
        face(0, -83, 30, 31) // 7
        face(72, -99, 50, 31) // 8
    }

    val PLATE: ShipBlueprint = ShipBlueprint.define("Alloy Plate") {
        stats(energy = 16, speed = 16, laserByte = 0, bounty = 0, canisters = 0, gunVertex = 0, explosionVertices = 1, cargoItem = 8)
        visibilityDistance = 5
        targetableArea = 100
        normalScale = 3
        vertex(-15, -22, -9) // 0
        vertex(-15, 38, -9) // 1
        vertex(19, 32, 11) // 2
        vertex(10, -46, 6) // 3
        edge(0, 1, 15, 15, 31) // 0
        edge(1, 2, 15, 15, 16) // 1
        edge(2, 3, 15, 15, 20) // 2
        edge(3, 0, 15, 15, 16) // 3
        face(0, 0, 0, 0) // 0
    }

    val PYTHON: ShipBlueprint = ShipBlueprint.define("Python") {
        stats(energy = 250, speed = 20, laserByte = 27, bounty = 0, canisters = 5, gunVertex = 0, explosionVertices = 9, cargoItem = 0)
        visibilityDistance = 40
        targetableArea = 6400
        normalScale = 0
        vertex(0, 0, 224) // 0
        vertex(0, 48, 48) // 1
        vertex(96, 0, -16) // 2
        vertex(-96, 0, -16) // 3
        vertex(0, 48, -32) // 4
        vertex(0, 24, -112) // 5
        vertex(-48, 0, -112) // 6
        vertex(48, 0, -112) // 7
        vertex(0, -48, 48) // 8
        vertex(0, -48, -32) // 9
        vertex(0, -24, -112) // 10
        edge(0, 8, 2, 3, 31) // 0
        edge(0, 3, 0, 2, 31) // 1
        edge(0, 2, 1, 3, 31) // 2
        edge(0, 1, 0, 1, 31) // 3
        edge(2, 4, 9, 5, 31) // 4
        edge(1, 2, 1, 5, 31) // 5
        edge(2, 8, 7, 3, 31) // 6
        edge(1, 3, 0, 4, 31) // 7
        edge(3, 8, 2, 6, 31) // 8
        edge(2, 9, 7, 10, 31) // 9
        edge(3, 4, 4, 8, 31) // 10
        edge(3, 9, 6, 11, 31) // 11
        edge(3, 5, 8, 8, 7) // 12
        edge(3, 10, 11, 11, 7) // 13
        edge(2, 5, 9, 9, 7) // 14
        edge(2, 10, 10, 10, 7) // 15
        edge(2, 7, 9, 10, 31) // 16
        edge(3, 6, 8, 11, 31) // 17
        edge(5, 6, 8, 12, 31) // 18
        edge(5, 7, 9, 12, 31) // 19
        edge(7, 10, 12, 10, 31) // 20
        edge(6, 10, 11, 12, 31) // 21
        edge(4, 5, 8, 9, 31) // 22
        edge(9, 10, 10, 11, 31) // 23
        edge(1, 4, 4, 5, 31) // 24
        edge(8, 9, 6, 7, 31) // 25
        face(-27, 40, 11, 31) // 0
        face(27, 40, 11, 31) // 1
        face(-27, -40, 11, 31) // 2
        face(27, -40, 11, 31) // 3
        face(-19, 38, 0, 31) // 4
        face(19, 38, 0, 31) // 5
        face(-19, -38, 0, 31) // 6
        face(19, -38, 0, 31) // 7
        face(-25, 37, -11, 31) // 8
        face(25, 37, -11, 31) // 9
        face(25, -37, -11, 31) // 10
        face(-25, -37, -11, 31) // 11
        face(0, 0, -112, 31) // 12
    }

    val PYTHON_P: ShipBlueprint = ShipBlueprint.define("Python (pirate)") {
        stats(energy = 250, speed = 20, laserByte = 27, bounty = 200, canisters = 2, gunVertex = 0, explosionVertices = 9, cargoItem = 0)
        visibilityDistance = 40
        targetableArea = 6400
        normalScale = 0
        vertex(0, 0, 224) // 0
        vertex(0, 48, 48) // 1
        vertex(96, 0, -16) // 2
        vertex(-96, 0, -16) // 3
        vertex(0, 48, -32) // 4
        vertex(0, 24, -112) // 5
        vertex(-48, 0, -112) // 6
        vertex(48, 0, -112) // 7
        vertex(0, -48, 48) // 8
        vertex(0, -48, -32) // 9
        vertex(0, -24, -112) // 10
        edge(0, 8, 2, 3, 31) // 0
        edge(0, 3, 0, 2, 31) // 1
        edge(0, 2, 1, 3, 31) // 2
        edge(0, 1, 0, 1, 31) // 3
        edge(2, 4, 9, 5, 31) // 4
        edge(1, 2, 1, 5, 31) // 5
        edge(2, 8, 7, 3, 31) // 6
        edge(1, 3, 0, 4, 31) // 7
        edge(3, 8, 2, 6, 31) // 8
        edge(2, 9, 7, 10, 31) // 9
        edge(3, 4, 4, 8, 31) // 10
        edge(3, 9, 6, 11, 31) // 11
        edge(3, 5, 8, 8, 7) // 12
        edge(3, 10, 11, 11, 7) // 13
        edge(2, 5, 9, 9, 7) // 14
        edge(2, 10, 10, 10, 7) // 15
        edge(2, 7, 9, 10, 31) // 16
        edge(3, 6, 8, 11, 31) // 17
        edge(5, 6, 8, 12, 31) // 18
        edge(5, 7, 9, 12, 31) // 19
        edge(7, 10, 12, 10, 31) // 20
        edge(6, 10, 11, 12, 31) // 21
        edge(4, 5, 8, 9, 31) // 22
        edge(9, 10, 10, 11, 31) // 23
        edge(1, 4, 4, 5, 31) // 24
        edge(8, 9, 6, 7, 31) // 25
        face(-27, 40, 11, 31) // 0
        face(27, 40, 11, 31) // 1
        face(-27, -40, 11, 31) // 2
        face(27, -40, 11, 31) // 3
        face(-19, 38, 0, 31) // 4
        face(19, 38, 0, 31) // 5
        face(-19, -38, 0, 31) // 6
        face(19, -38, 0, 31) // 7
        face(-25, 37, -11, 31) // 8
        face(25, 37, -11, 31) // 9
        face(25, -37, -11, 31) // 10
        face(-25, -37, -11, 31) // 11
        face(0, 0, -112, 31) // 12
    }

    val SHUTTLE: ShipBlueprint = ShipBlueprint.define("Shuttle") {
        stats(energy = 32, speed = 8, laserByte = 0, bounty = 0, canisters = 15, gunVertex = 0, explosionVertices = 8, cargoItem = 0)
        visibilityDistance = 22
        targetableArea = 2500
        normalScale = 2
        vertex(0, -17, 23) // 0
        vertex(-17, 0, 23) // 1
        vertex(0, 18, 23) // 2
        vertex(18, 0, 23) // 3
        vertex(-20, -20, -27) // 4
        vertex(-20, 20, -27) // 5
        vertex(20, 20, -27) // 6
        vertex(20, -20, -27) // 7
        vertex(5, 0, -27) // 8
        vertex(0, -2, -27) // 9
        vertex(-5, 0, -27) // 10
        vertex(0, 3, -27) // 11
        vertex(0, -9, 35) // 12
        vertex(3, -1, 31) // 13
        vertex(4, 11, 25) // 14
        vertex(11, 4, 25) // 15
        vertex(-3, -1, 31) // 16
        vertex(-3, 11, 25) // 17
        vertex(-10, 4, 25) // 18
        edge(0, 1, 2, 0, 31) // 0
        edge(1, 2, 10, 4, 31) // 1
        edge(2, 3, 11, 6, 31) // 2
        edge(0, 3, 12, 8, 31) // 3
        edge(0, 7, 8, 1, 31) // 4
        edge(0, 4, 2, 1, 24) // 5
        edge(1, 4, 3, 2, 31) // 6
        edge(1, 5, 4, 3, 24) // 7
        edge(2, 5, 5, 4, 31) // 8
        edge(2, 6, 6, 5, 12) // 9
        edge(3, 6, 7, 6, 31) // 10
        edge(3, 7, 8, 7, 24) // 11
        edge(4, 5, 9, 3, 31) // 12
        edge(5, 6, 9, 5, 31) // 13
        edge(6, 7, 9, 7, 31) // 14
        edge(4, 7, 9, 1, 31) // 15
        edge(0, 12, 12, 0, 16) // 16
        edge(1, 12, 10, 0, 16) // 17
        edge(2, 12, 11, 10, 16) // 18
        edge(3, 12, 12, 11, 16) // 19
        edge(8, 9, 9, 9, 16) // 20
        edge(9, 10, 9, 9, 7) // 21
        edge(10, 11, 9, 9, 9) // 22
        edge(8, 11, 9, 9, 7) // 23
        edge(13, 14, 11, 11, 5) // 24
        edge(14, 15, 11, 11, 8) // 25
        edge(13, 15, 11, 11, 7) // 26
        edge(16, 17, 10, 10, 5) // 27
        edge(17, 18, 10, 10, 8) // 28
        edge(16, 18, 10, 10, 7) // 29
        face(-55, -55, 40, 31) // 0
        face(0, -74, 4, 31) // 1
        face(-51, -51, 23, 31) // 2
        face(-74, 0, 4, 31) // 3
        face(-51, 51, 23, 31) // 4
        face(0, 74, 4, 31) // 5
        face(51, 51, 23, 31) // 6
        face(74, 0, 4, 31) // 7
        face(51, -51, 23, 31) // 8
        face(0, 0, -107, 31) // 9
        face(-41, 41, 90, 31) // 10
        face(41, 41, 90, 31) // 11
        face(55, -55, 40, 31) // 12
    }

    val SIDEWINDER: ShipBlueprint = ShipBlueprint.define("Sidewinder") {
        stats(energy = 70, speed = 37, laserByte = 16, bounty = 50, canisters = 0, gunVertex = 0, explosionVertices = 6, cargoItem = 0)
        visibilityDistance = 20
        targetableArea = 4225
        normalScale = 2
        vertex(-32, 0, 36) // 0
        vertex(32, 0, 36) // 1
        vertex(64, 0, -28) // 2
        vertex(-64, 0, -28) // 3
        vertex(0, 16, -28) // 4
        vertex(0, -16, -28) // 5
        vertex(-12, 6, -28) // 6
        vertex(12, 6, -28) // 7
        vertex(12, -6, -28) // 8
        vertex(-12, -6, -28) // 9
        edge(0, 1, 0, 5, 31) // 0
        edge(1, 2, 2, 6, 31) // 1
        edge(1, 4, 0, 2, 31) // 2
        edge(0, 4, 0, 1, 31) // 3
        edge(0, 3, 1, 4, 31) // 4
        edge(3, 4, 1, 3, 31) // 5
        edge(2, 4, 2, 3, 31) // 6
        edge(3, 5, 3, 4, 31) // 7
        edge(2, 5, 3, 6, 31) // 8
        edge(1, 5, 5, 6, 31) // 9
        edge(0, 5, 4, 5, 31) // 10
        edge(6, 7, 3, 3, 15) // 11
        edge(7, 8, 3, 3, 12) // 12
        edge(6, 9, 3, 3, 12) // 13
        edge(8, 9, 3, 3, 12) // 14
        face(0, 32, 8, 31) // 0
        face(-12, 47, 6, 31) // 1
        face(12, 47, 6, 31) // 2
        face(0, 0, -112, 31) // 3
        face(-12, -47, 6, 31) // 4
        face(0, -32, 8, 31) // 5
        face(12, -47, 6, 31) // 6
    }

    val SPLINTER: ShipBlueprint = ShipBlueprint.define("Splinter") {
        stats(energy = 20, speed = 10, laserByte = 0, bounty = 0, canisters = 0, gunVertex = 0, explosionVertices = 4, cargoItem = 11)
        visibilityDistance = 8
        targetableArea = 256
        normalScale = 5
        vertex(-24, -25, 16) // 0
        vertex(0, 12, -10) // 1
        vertex(11, -6, 2) // 2
        vertex(12, 42, 7) // 3
        edge(0, 1, 3, 2, 31) // 0
        edge(1, 2, 3, 0, 31) // 1
        edge(2, 3, 1, 0, 31) // 2
        edge(3, 0, 2, 1, 31) // 3
        edge(0, 2, 3, 1, 31) // 4
        edge(3, 1, 2, 0, 31) // 5
        face(35, 0, 4, 31) // 0
        face(3, 4, 8, 31) // 1
        face(1, 8, 12, 31) // 2
        face(18, 12, 0, 31) // 3
    }

    val THARGOID: ShipBlueprint = ShipBlueprint.define("Thargoid") {
        stats(energy = 240, speed = 39, laserByte = 22, bounty = 500, canisters = 0, gunVertex = 15, explosionVertices = 8, cargoItem = 0)
        visibilityDistance = 55
        targetableArea = 9801
        normalScale = 2
        vertex(32, -48, 48) // 0
        vertex(32, -68, 0) // 1
        vertex(32, -48, -48) // 2
        vertex(32, 0, -68) // 3
        vertex(32, 48, -48) // 4
        vertex(32, 68, 0) // 5
        vertex(32, 48, 48) // 6
        vertex(32, 0, 68) // 7
        vertex(-24, -116, 116) // 8
        vertex(-24, -164, 0) // 9
        vertex(-24, -116, -116) // 10
        vertex(-24, 0, -164) // 11
        vertex(-24, 116, -116) // 12
        vertex(-24, 164, 0) // 13
        vertex(-24, 116, 116) // 14
        vertex(-24, 0, 164) // 15
        vertex(-24, 64, 80) // 16
        vertex(-24, 64, -80) // 17
        vertex(-24, -64, -80) // 18
        vertex(-24, -64, 80) // 19
        edge(0, 7, 4, 8, 31) // 0
        edge(0, 1, 0, 4, 31) // 1
        edge(1, 2, 1, 4, 31) // 2
        edge(2, 3, 2, 4, 31) // 3
        edge(3, 4, 3, 4, 31) // 4
        edge(4, 5, 4, 5, 31) // 5
        edge(5, 6, 4, 6, 31) // 6
        edge(6, 7, 4, 7, 31) // 7
        edge(0, 8, 0, 8, 31) // 8
        edge(1, 9, 0, 1, 31) // 9
        edge(2, 10, 1, 2, 31) // 10
        edge(3, 11, 2, 3, 31) // 11
        edge(4, 12, 3, 5, 31) // 12
        edge(5, 13, 5, 6, 31) // 13
        edge(6, 14, 6, 7, 31) // 14
        edge(7, 15, 7, 8, 31) // 15
        edge(8, 15, 8, 9, 31) // 16
        edge(8, 9, 0, 9, 31) // 17
        edge(9, 10, 1, 9, 31) // 18
        edge(10, 11, 2, 9, 31) // 19
        edge(11, 12, 3, 9, 31) // 20
        edge(12, 13, 5, 9, 31) // 21
        edge(13, 14, 6, 9, 31) // 22
        edge(14, 15, 7, 9, 31) // 23
        edge(16, 17, 9, 9, 30) // 24
        edge(18, 19, 9, 9, 30) // 25
        face(103, -60, 25, 31) // 0
        face(103, -60, -25, 31) // 1
        face(103, -25, -60, 31) // 2
        face(103, 25, -60, 31) // 3
        face(64, 0, 0, 31) // 4
        face(103, 60, -25, 31) // 5
        face(103, 60, 25, 31) // 6
        face(103, 25, 60, 31) // 7
        face(103, -25, 60, 31) // 8
        face(-48, 0, 0, 31) // 9
    }

    val THARGON: ShipBlueprint = ShipBlueprint.define("Thargon") {
        stats(energy = 20, speed = 30, laserByte = 16, bounty = 50, canisters = 0, gunVertex = 0, explosionVertices = 3, cargoItem = 15)
        visibilityDistance = 20
        targetableArea = 1600
        normalScale = 2
        vertex(-9, 0, 40) // 0
        vertex(-9, -38, 12) // 1
        vertex(-9, -24, -32) // 2
        vertex(-9, 24, -32) // 3
        vertex(-9, 38, 12) // 4
        vertex(9, 0, -8) // 5
        vertex(9, -10, -15) // 6
        vertex(9, -6, -26) // 7
        vertex(9, 6, -26) // 8
        vertex(9, 10, -15) // 9
        edge(0, 1, 0, 1, 31) // 0
        edge(1, 2, 0, 2, 31) // 1
        edge(2, 3, 0, 3, 31) // 2
        edge(3, 4, 0, 4, 31) // 3
        edge(0, 4, 0, 5, 31) // 4
        edge(0, 5, 1, 5, 31) // 5
        edge(1, 6, 1, 2, 31) // 6
        edge(2, 7, 2, 3, 31) // 7
        edge(3, 8, 3, 4, 31) // 8
        edge(4, 9, 4, 5, 31) // 9
        edge(5, 6, 1, 6, 31) // 10
        edge(6, 7, 2, 6, 31) // 11
        edge(7, 8, 3, 6, 31) // 12
        edge(8, 9, 4, 6, 31) // 13
        edge(9, 5, 5, 6, 31) // 14
        face(-36, 0, 0, 31) // 0
        face(20, -5, 7, 31) // 1
        face(46, -42, -14, 31) // 2
        face(36, 0, -104, 31) // 3
        face(46, 42, -14, 31) // 4
        face(20, 5, 7, 31) // 5
        face(36, 0, 0, 31) // 6
    }

    val TRANSPORTER: ShipBlueprint = ShipBlueprint.define("Transporter") {
        stats(energy = 32, speed = 10, laserByte = 0, bounty = 0, canisters = 0, gunVertex = 12, explosionVertices = 5, cargoItem = 0)
        visibilityDistance = 16
        targetableArea = 2500
        normalScale = 2
        vertex(0, 10, -26) // 0
        vertex(-25, 4, -26) // 1
        vertex(-28, -3, -26) // 2
        vertex(-25, -8, -26) // 3
        vertex(26, -8, -26) // 4
        vertex(29, -3, -26) // 5
        vertex(26, 4, -26) // 6
        vertex(0, 6, 12) // 7
        vertex(-30, -1, 12) // 8
        vertex(-33, -8, 12) // 9
        vertex(33, -8, 12) // 10
        vertex(30, -1, 12) // 11
        vertex(-11, -2, 30) // 12
        vertex(-13, -8, 30) // 13
        vertex(14, -8, 30) // 14
        vertex(11, -2, 30) // 15
        vertex(-5, 6, 2) // 16
        vertex(-18, 3, 2) // 17
        vertex(-5, 7, -7) // 18
        vertex(-18, 4, -7) // 19
        vertex(-11, 6, -14) // 20
        vertex(-11, 5, -7) // 21
        vertex(5, 7, -14) // 22
        vertex(18, 4, -14) // 23
        vertex(11, 5, -7) // 24
        vertex(5, 6, -3) // 25
        vertex(18, 3, -3) // 26
        vertex(11, 4, 8) // 27
        vertex(11, 5, -3) // 28
        vertex(-16, -8, -13) // 29
        vertex(-16, -8, 16) // 30
        vertex(17, -8, -13) // 31
        vertex(17, -8, 16) // 32
        vertex(-13, -3, -26) // 33
        vertex(13, -3, -26) // 34
        vertex(9, 3, -26) // 35
        vertex(-8, 3, -26) // 36
        edge(0, 1, 7, 0, 31) // 0
        edge(1, 2, 1, 0, 31) // 1
        edge(2, 3, 2, 0, 31) // 2
        edge(3, 4, 3, 0, 31) // 3
        edge(4, 5, 4, 0, 31) // 4
        edge(5, 6, 5, 0, 31) // 5
        edge(0, 6, 6, 0, 31) // 6
        edge(0, 7, 7, 6, 16) // 7
        edge(1, 8, 7, 1, 31) // 8
        edge(2, 9, 2, 1, 11) // 9
        edge(3, 9, 3, 2, 31) // 10
        edge(4, 10, 4, 3, 31) // 11
        edge(5, 10, 5, 4, 11) // 12
        edge(6, 11, 6, 5, 31) // 13
        edge(7, 8, 8, 7, 17) // 14
        edge(8, 9, 9, 1, 17) // 15
        edge(10, 11, 10, 5, 17) // 16
        edge(7, 11, 11, 6, 17) // 17
        edge(7, 15, 12, 11, 19) // 18
        edge(7, 12, 12, 8, 19) // 19
        edge(8, 12, 9, 8, 16) // 20
        edge(9, 13, 9, 3, 31) // 21
        edge(10, 14, 10, 3, 31) // 22
        edge(11, 15, 11, 10, 16) // 23
        edge(12, 13, 13, 9, 31) // 24
        edge(13, 14, 13, 3, 31) // 25
        edge(14, 15, 13, 10, 31) // 26
        edge(12, 15, 13, 12, 31) // 27
        edge(16, 17, 7, 7, 7) // 28
        edge(18, 19, 7, 7, 7) // 29
        edge(19, 20, 7, 7, 7) // 30
        edge(18, 20, 7, 7, 7) // 31
        edge(20, 21, 7, 7, 7) // 32
        edge(22, 23, 6, 6, 7) // 33
        edge(23, 24, 6, 6, 7) // 34
        edge(24, 22, 6, 6, 7) // 35
        edge(25, 26, 6, 6, 7) // 36
        edge(26, 27, 6, 6, 7) // 37
        edge(25, 27, 6, 6, 7) // 38
        edge(27, 28, 6, 6, 7) // 39
        edge(29, 30, 3, 3, 6) // 40
        edge(31, 32, 3, 3, 6) // 41
        edge(33, 34, 0, 0, 8) // 42
        edge(34, 35, 0, 0, 5) // 43
        edge(35, 36, 0, 0, 5) // 44
        edge(36, 33, 0, 0, 5) // 45
        face(0, 0, -103, 31) // 0
        face(-111, 48, -7, 31) // 1
        face(-105, -63, -21, 31) // 2
        face(0, -34, 0, 31) // 3
        face(105, -63, -21, 31) // 4
        face(111, 48, -7, 31) // 5
        face(8, 32, 3, 31) // 6
        face(-8, 32, 3, 31) // 7
        face(-8, 34, 11, 19) // 8
        face(-75, 32, 79, 31) // 9
        face(75, 32, 79, 31) // 10
        face(8, 34, 11, 19) // 11
        face(0, 38, 17, 31) // 12
        face(0, 0, 121, 31) // 13
    }

    val VIPER: ShipBlueprint = ShipBlueprint.define("Viper") {
        stats(energy = 100, speed = 32, laserByte = 17, bounty = 0, canisters = 0, gunVertex = 0, explosionVertices = 9, cargoItem = 0)
        visibilityDistance = 23
        targetableArea = 5625
        normalScale = 1
        vertex(0, 0, 72) // 0
        vertex(0, 16, 24) // 1
        vertex(0, -16, 24) // 2
        vertex(48, 0, -24) // 3
        vertex(-48, 0, -24) // 4
        vertex(24, -16, -24) // 5
        vertex(-24, -16, -24) // 6
        vertex(24, 16, -24) // 7
        vertex(-24, 16, -24) // 8
        vertex(-32, 0, -24) // 9
        vertex(32, 0, -24) // 10
        vertex(8, 8, -24) // 11
        vertex(-8, 8, -24) // 12
        vertex(-8, -8, -24) // 13
        vertex(8, -8, -24) // 14
        edge(0, 3, 2, 4, 31) // 0
        edge(0, 1, 1, 2, 30) // 1
        edge(0, 2, 3, 4, 30) // 2
        edge(0, 4, 1, 3, 31) // 3
        edge(1, 7, 0, 2, 30) // 4
        edge(1, 8, 0, 1, 30) // 5
        edge(2, 5, 4, 5, 30) // 6
        edge(2, 6, 3, 5, 30) // 7
        edge(7, 8, 0, 6, 31) // 8
        edge(5, 6, 5, 6, 30) // 9
        edge(4, 8, 1, 6, 31) // 10
        edge(4, 6, 3, 6, 30) // 11
        edge(3, 7, 2, 6, 31) // 12
        edge(3, 5, 6, 4, 30) // 13
        edge(9, 12, 6, 6, 19) // 14
        edge(9, 13, 6, 6, 18) // 15
        edge(10, 11, 6, 6, 19) // 16
        edge(10, 14, 6, 6, 18) // 17
        edge(11, 14, 6, 6, 16) // 18
        edge(12, 13, 6, 6, 16) // 19
        face(0, 32, 0, 31) // 0
        face(-22, 33, 11, 31) // 1
        face(22, 33, 11, 31) // 2
        face(-22, -33, 11, 31) // 3
        face(22, -33, 11, 31) // 4
        face(0, -32, 0, 31) // 5
        face(0, 0, -48, 31) // 6
    }

    val WORM: ShipBlueprint = ShipBlueprint.define("Worm") {
        stats(energy = 30, speed = 23, laserByte = 8, bounty = 0, canisters = 0, gunVertex = 0, explosionVertices = 3, cargoItem = 0)
        visibilityDistance = 19
        targetableArea = 9801
        normalScale = 3
        vertex(10, -10, 35) // 0
        vertex(-10, -10, 35) // 1
        vertex(5, 6, 15) // 2
        vertex(-5, 6, 15) // 3
        vertex(15, -10, 25) // 4
        vertex(-15, -10, 25) // 5
        vertex(26, -10, -25) // 6
        vertex(-26, -10, -25) // 7
        vertex(8, 14, -25) // 8
        vertex(-8, 14, -25) // 9
        edge(0, 1, 7, 0, 31) // 0
        edge(1, 5, 7, 3, 31) // 1
        edge(5, 7, 7, 5, 31) // 2
        edge(7, 6, 7, 6, 31) // 3
        edge(6, 4, 7, 4, 31) // 4
        edge(4, 0, 7, 2, 31) // 5
        edge(0, 2, 2, 0, 31) // 6
        edge(1, 3, 3, 0, 31) // 7
        edge(4, 2, 4, 2, 31) // 8
        edge(5, 3, 5, 3, 31) // 9
        edge(2, 8, 4, 1, 31) // 10
        edge(8, 6, 6, 4, 31) // 11
        edge(3, 9, 5, 1, 31) // 12
        edge(9, 7, 6, 5, 31) // 13
        edge(2, 3, 1, 0, 31) // 14
        edge(8, 9, 6, 1, 31) // 15
        face(0, 88, 70, 31) // 0
        face(0, 69, 14, 31) // 1
        face(70, 66, 35, 31) // 2
        face(-70, 66, 35, 31) // 3
        face(64, 49, 14, 31) // 4
        face(-64, 49, 14, 31) // 5
        face(0, 0, -200, 31) // 6
        face(0, -80, 0, 31) // 7
    }

    /** XX21 de cada arquivo de naves (D.MOA … D.MOP): tipo (1..31) → modelo. */
    val SHIP_FILES: List<Map<Int, ShipBlueprint>> = listOf(
        mapOf(2 to CORIOLIS, 3 to ESCAPE_POD, 4 to PLATE, 5 to CANISTER, 12 to PYTHON, 16 to VIPER, 17 to SIDEWINDER, 18 to MAMBA, 19 to KRAIT, 24 to COBRA_MK_3_P, 27 to FER_DE_LANCE), // A
        mapOf(2 to DODO, 3 to ESCAPE_POD, 4 to PLATE, 5 to CANISTER, 6 to BOULDER, 11 to COBRA_MK_3, 16 to VIPER, 18 to MAMBA, 19 to KRAIT, 20 to ADDER, 23 to WORM), // B
        mapOf(2 to CORIOLIS, 3 to ESCAPE_POD, 5 to CANISTER, 7 to ASTEROID, 8 to SPLINTER, 11 to COBRA_MK_3, 13 to BOA, 16 to VIPER, 17 to SIDEWINDER, 19 to KRAIT, 29 to THARGOID, 30 to THARGON), // C
        mapOf(2 to DODO, 3 to ESCAPE_POD, 5 to CANISTER, 7 to ASTEROID, 8 to SPLINTER, 11 to COBRA_MK_3, 16 to VIPER, 17 to SIDEWINDER, 21 to GECKO, 22 to COBRA_MK_1, 29 to THARGOID, 30 to THARGON), // D
        mapOf(2 to CORIOLIS, 3 to ESCAPE_POD, 4 to PLATE, 5 to CANISTER, 6 to BOULDER, 11 to COBRA_MK_3, 16 to VIPER, 17 to SIDEWINDER, 18 to MAMBA, 19 to KRAIT, 21 to GECKO, 22 to COBRA_MK_1), // E
        mapOf(2 to DODO, 3 to ESCAPE_POD, 5 to CANISTER, 6 to BOULDER, 10 to TRANSPORTER, 11 to COBRA_MK_3, 13 to BOA, 16 to VIPER, 17 to SIDEWINDER, 21 to GECKO), // F
        mapOf(2 to CORIOLIS, 3 to ESCAPE_POD, 4 to PLATE, 5 to CANISTER, 6 to BOULDER, 11 to COBRA_MK_3, 16 to VIPER, 18 to MAMBA, 19 to KRAIT, 20 to ADDER, 31 to CONSTRICTOR), // G
        mapOf(2 to DODO, 3 to ESCAPE_POD, 5 to CANISTER, 7 to ASTEROID, 8 to SPLINTER, 9 to SHUTTLE, 12 to PYTHON, 16 to VIPER, 17 to SIDEWINDER, 19 to KRAIT, 24 to COBRA_MK_3_P), // H
        mapOf(2 to CORIOLIS, 3 to ESCAPE_POD, 4 to PLATE, 5 to CANISTER, 16 to VIPER, 17 to SIDEWINDER, 18 to MAMBA, 19 to KRAIT, 20 to ADDER, 24 to COBRA_MK_3_P, 26 to PYTHON_P), // I
        mapOf(2 to DODO, 3 to ESCAPE_POD, 4 to PLATE, 5 to CANISTER, 16 to VIPER, 17 to SIDEWINDER, 18 to MAMBA, 19 to KRAIT, 21 to GECKO, 22 to COBRA_MK_1, 24 to COBRA_MK_3_P), // J
        mapOf(2 to CORIOLIS, 3 to ESCAPE_POD, 5 to CANISTER, 7 to ASTEROID, 8 to SPLINTER, 11 to COBRA_MK_3, 12 to PYTHON, 16 to VIPER, 17 to SIDEWINDER, 21 to GECKO, 22 to COBRA_MK_1, 28 to MORAY), // K
        mapOf(2 to DODO, 3 to ESCAPE_POD, 5 to CANISTER, 11 to COBRA_MK_3, 14 to ANACONDA, 16 to VIPER, 17 to SIDEWINDER, 18 to MAMBA, 23 to WORM, 27 to FER_DE_LANCE), // L
        mapOf(2 to CORIOLIS, 5 to CANISTER, 16 to VIPER, 17 to SIDEWINDER, 18 to MAMBA, 19 to KRAIT, 20 to ADDER, 21 to GECKO, 22 to COBRA_MK_1, 24 to COBRA_MK_3_P), // M
        mapOf(2 to DODO, 3 to ESCAPE_POD, 5 to CANISTER, 7 to ASTEROID, 8 to SPLINTER, 11 to COBRA_MK_3, 16 to VIPER, 17 to SIDEWINDER, 19 to KRAIT, 21 to GECKO, 25 to ASP_MK_2), // N
        mapOf(2 to CORIOLIS, 3 to ESCAPE_POD, 4 to PLATE, 5 to CANISTER, 7 to ASTEROID, 8 to SPLINTER, 11 to COBRA_MK_3, 16 to VIPER, 18 to MAMBA, 19 to KRAIT, 22 to COBRA_MK_1, 26 to PYTHON_P), // O
        mapOf(2 to DODO, 3 to ESCAPE_POD, 4 to PLATE, 5 to CANISTER, 6 to BOULDER, 7 to ASTEROID, 8 to SPLINTER, 11 to COBRA_MK_3, 16 to VIPER, 17 to SIDEWINDER, 19 to KRAIT, 25 to ASP_MK_2), // P
    )

    /** E%: flags NEWB padrão de cada tipo de nave (1..31). */
    val NEWB: Map<Int, Int> = mapOf(1 to 0b00000000, 2 to 0b00000000, 3 to 0b00000001, 4 to 0b00000000, 5 to 0b00000000, 6 to 0b00000000, 7 to 0b00000000, 8 to 0b00000000, 9 to 0b00100001, 10 to 0b01100001, 11 to 0b10100000, 12 to 0b10100000, 13 to 0b10100000, 14 to 0b10100001, 16 to 0b11000010, 17 to 0b00001100, 18 to 0b10001100, 19 to 0b10001100, 20 to 0b10001100, 21 to 0b00001100, 22 to 0b10001100, 23 to 0b00000100, 24 to 0b10001100, 25 to 0b10001100, 26 to 0b10001100, 27 to 0b10000010, 28 to 0b00001100, 29 to 0b00001100, 30 to 0b00000100, 31 to 0b00000100)
}
