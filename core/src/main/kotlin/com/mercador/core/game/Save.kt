package com.mercador.core.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Salvamento (design, seção 3.2): só o mundo VIVIDO. O mundo gerado sai da semente e nunca é
 * gravado; aqui vão o jogador, as diferenças do mundo (medidores e volumes), a reputação, o livro
 * de contas e o que está em aberto. Decisões pendentes (perseguição, cena, história) não entram:
 * ao recarregar, volta-se ao último momento calmo, e o sorteio determinístico refaz o mesmo encontro.
 */
@Serializable data class SysRef(val sx: Int, val sy: Int, val index: Int)
@Serializable data class MeterDeltaSave(val system: SysRef, val control: Double, val discontent: Double, val supply: Double)
@Serializable data class VolumeSave(val system: SysRef, val commodity: String, val tons: Double)
@Serializable data class LedgerSave(val day: Int, val system: String, val text: String)
@Serializable data class PersonSave(val name: String, val role: String, val faction: Int)
@Serializable data class RetaliationSave(val due: Int, val avenger: PersonSave)
@Serializable data class TraitSave(val trait: String, val until: Int? = null)

@Serializable
data class GameSave(
    val version: Int = VERSION,
    val universeSeed: Long,
    val galaxy: Long,
    /** Os sistemas da região, na ordem: a região não é recalculada, para não mudar se o algoritmo mudar. */
    val region: List<SysRef>,
    val stories: Boolean,
    val day: Int,
    val meterDeltas: List<MeterDeltaSave>,
    val volumes: List<VolumeSave>,
    val credits: Int,
    val fuel: Int,
    val debt: Int,
    val location: SysRef,
    val cargo: Map<String, Int>,
    val ledger: List<LedgerSave>,
    val voice: List<LedgerSave>,
    val traits: List<TraitSave>,
    val secrets: List<Int>,
    val favors: List<Int>,
    val heat: List<Int>,
    val reputation: List<Int>,
    val retaliations: List<RetaliationSave>,
    val sceneWins: Int,
    val intimidationWins: Int,
    val storySeen: Map<String, Int>,
    /** Metadados do slot (não afetam o jogo; defaults mantêm saves antigos legíveis). */
    val name: String = "",
    val savedAt: Long = 0,
    val locationName: String = "",
) {
    companion object { const val VERSION = 1 }
}

class SaveException(message: String, cause: Throwable? = null) : Exception(message, cause)

object SaveCodec {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun encode(save: GameSave): String = json.encodeToString(GameSave.serializer(), save)

    fun decode(text: String): GameSave {
        val save = try { json.decodeFromString(GameSave.serializer(), text) } catch (e: Exception) {
            throw SaveException("Arquivo de save ilegível ou corrompido.", e)
        }
        if (save.version != GameSave.VERSION) throw SaveException("Versão de save ${save.version} não suportada (esperada ${GameSave.VERSION}).")
        return save
    }
}

/** Arquivo de save em disco. Grava num temporário e renomeia, para um corte no meio não destruir o save anterior. */
class SaveStore(private val file: File) {
    fun exists(): Boolean = file.isFile

    fun write(save: GameSave) {
        file.absoluteFile.parentFile?.mkdirs()
        val tmp = File(file.absolutePath + ".tmp")
        tmp.writeText(SaveCodec.encode(save))
        if (!tmp.renameTo(file)) { file.delete(); check(tmp.renameTo(file)) { "não consegui gravar ${file.path}" } }
    }

    fun read(): GameSave = SaveCodec.decode(file.readText())

    class Loaded(val game: Game, val warning: String?)

    /**
     * Abre o jogo salvo, ou começa um novo. Um save ilegível nunca é apagado: vai para `.bak`
     * (para quem quiser tentar recuperar) e o jogador recebe um aviso junto com o jogo novo.
     */
    fun loadOrNew(newGame: () -> Game): Loaded {
        if (!exists()) return Loaded(newGame(), null)
        return try {
            Loaded(Game.fromSave(read()), null)
        } catch (e: SaveException) {
            val bak = File(file.path + ".bak")
            bak.delete(); file.renameTo(bak)
            Loaded(newGame(), "O save anterior estava ilegível (${e.message}) e foi guardado em ${bak.name}. Começando um jogo novo.")
        }
    }

    /** Grava o jogo se ele estiver num momento calmo; com decisão pendente, mantém o save anterior. */
    fun autosave(game: Game): Boolean {
        if (!game.canSave) return false
        write(game.toSave())
        return true
    }
}
