package com.mercador.core.universe

enum class Economy(val label: String) {
    AGRICOLA("Agrícola"), INDUSTRIAL("Industrial"), MINERADORA("Mineradora"),
    COMERCIAL("Comercial"), TECNOLOGICA("Tecnológica"), PORTUARIA("Portuária")
}

enum class Ideology(val label: String) {
    MERCANTIL("Mercantil"), AUTORITARIA("Autoritária"), RELIGIOSA("Religiosa"), CLANICA("Clânica")
}

/** Como os agentes de uma facção agem (seção 4/8.3 do design). */
enum class Doctrine(val label: String) {
    BRUTAL("Brutal"), MERCANTIL("Mercantil"), BUROCRATICA("Burocrática"), COBRADORA("Cobradora")
}

enum class RareResource(val label: String) {
    NENHUM("Nenhum"), CRISTAL("Cristal de salto"), ISOTOPO("Isótopo raro"), BIOTOXINA("Biotoxina"), ARTEFATO("Artefato")
}

/**
 * Traços de um sistema. Os cinco primeiros vêm do Elite; os demais são novos (seção 3.3).
 * Escalas de 0 a 100, exceto [techLevel] (1..15) e [population] (milhões).
 */
data class SystemTraits(
    val economy: Economy,
    val techLevel: Int,
    val population: Int,
    val productivity: Int,
    val corruption: Int,
    val surveillance: Int,
    val stability: Int,
    val ideology: Ideology,
    val cultureIndex: Int,
    val rareResource: RareResource,
)
