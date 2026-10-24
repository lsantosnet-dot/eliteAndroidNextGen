package com.mercador.core.flight


/**
 * Tipos de nave da versão em disco do BBC Elite (o número é o tipo do original, 1..31;
 * a estação, tipo 2, é a Coriolis ou a Dodecaedro conforme o arquivo de naves).
 *
 * [defaultNewb] é a tabela E%: as flags NEWB com que a nave surge — bit 0 comerciante,
 * 1 caçador de recompensas, 2 hostil, 3 pirata, 4 atracando, 5 inocente (atacá-la irrita
 * a estação), 6 polícia e 7 tem cápsula de fuga.
 */
enum class ShipType(val id: Int, val blueprint: ShipBlueprint, rgb: Int) {
    MISSILE(1, OriginalShips.MISSILE, 0xFFF060),          // MSL
    STATION(2, DiscShips.CORIOLIS, 0x1AD9FF),             // SST
    DODO(2, DiscShips.DODO, 0x1AD9FF),                    // SST (sistemas de tech level alto)
    ESCAPE_POD(3, DiscShips.ESCAPE_POD, 0xC0C0FF),        // ESC
    ALLOY_PLATE(4, DiscShips.PLATE, 0xB0B0C8),            // PLT
    CANISTER(5, DiscShips.CANISTER, 0xFFE080),            // OIL
    BOULDER(6, DiscShips.BOULDER, 0x9A9AB0),
    ASTEROID(7, DiscShips.ASTEROID, 0x9A9AB0),            // AST
    SPLINTER(8, DiscShips.SPLINTER, 0x9A9AB0),            // SPL
    SHUTTLE(9, DiscShips.SHUTTLE, 0x60FFB0),              // SHU
    TRANSPORTER(10, DiscShips.TRANSPORTER, 0x60FFB0),
    COBRA_TRADER(11, DiscShips.COBRA_MK_3, 0x40FF90),     // CYL
    PYTHON_TRADER(12, DiscShips.PYTHON, 0x40FF90),
    BOA(13, DiscShips.BOA, 0x40FF90),
    ANACONDA(14, DiscShips.ANACONDA, 0x40FF90),           // ANA
    VIPER(16, DiscShips.VIPER, 0x4C8CFF),                 // COPS: polícia
    SIDEWINDER(17, DiscShips.SIDEWINDER, 0xFF3070),       // SH3 (PACK: primeiro dos piratas em bando)
    MAMBA(18, DiscShips.MAMBA, 0xFF4040),
    KRAIT(19, DiscShips.KRAIT, 0xFF5060),                 // KRA
    ADDER(20, DiscShips.ADDER, 0xFF6040),                 // ADA
    GECKO(21, DiscShips.GECKO, 0xFF3050),
    COBRA_MK1(22, DiscShips.COBRA_MK_1, 0xFF7040),
    WORM(23, DiscShips.WORM, 0xFF50A0),                   // WRM
    COBRA_PIRATE(24, DiscShips.COBRA_MK_3_P, 0xFFB030),   // CYL2
    ASP(25, DiscShips.ASP_MK_2, 0xFF8030),                // ASP
    PYTHON(26, DiscShips.PYTHON_P, 0xFF7A30),
    FER_DE_LANCE(27, DiscShips.FER_DE_LANCE, 0xFFA040),
    MORAY(28, DiscShips.MORAY, 0xFF6080),
    THARGOID(29, DiscShips.THARGOID, 0x9CFF30),           // THG
    THARGON(30, DiscShips.THARGON, 0x70D040),             // TGL
    CONSTRICTOR(31, DiscShips.CONSTRICTOR, 0xFF2020);     // CON: missão 1

    /** Cor neon (0xRRGGBB) usada no wireframe e no radar. */
    val color: Int = rgb

    /** E%: flags NEWB padrão. */
    val defaultNewb: Int get() = DiscShips.NEWB[id] ?: 0

    val isStation: Boolean get() = id == 2

    /** JL ≤ tipo < JH: "lixo" (cápsula, placa, contêiner, pedras, Shuttle e Transporter). */
    val isJunk: Boolean get() = id in 3 until 11

    companion object {
        /** O tipo de número [id] (para a estação, a Coriolis). */
        fun byId(id: Int): ShipType? = entries.firstOrNull { it.id == id }
    }
}

/** Bits das flags NEWB (byte #36 do bloco da nave na versão em disco). */
object Newb {
    const val TRADER = 1
    const val BOUNTY_HUNTER = 2
    const val HOSTILE = 4
    const val PIRATE = 8
    const val DOCKING = 16
    const val INNOCENT = 32
    const val COP = 64
    /** Na tabela E%: tem cápsula de fuga. Na nave: atracou ou foi recolhida (sai da bolha). */
    const val GONE = 128
}
