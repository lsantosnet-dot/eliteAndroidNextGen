package com.mercador.core.game

import java.io.File

/** O que a lista de jogos mostra de cada slot, sem carregar o jogo. */
data class SlotInfo(
    val id: Int,
    val name: String,
    val day: Int,
    val credits: Int,
    val location: String,
    val savedAt: Long,
    /** Preenchido quando o arquivo não pôde ser lido: o slot aparece, mas não carrega e não é apagado sozinho. */
    val problem: String? = null,
) { val playable: Boolean get() = problem == null }

/**
 * Vários jogos salvos numa pasta, um arquivo por slot (`slot-N.json`). Nenhuma operação apaga um
 * save ilegível por conta própria; só [delete], pedido pelo jogador, remove arquivo.
 */
class SaveSlots(private val dir: File, private val clock: () -> Long = System::currentTimeMillis) {
    private val pattern = Regex("""slot-(\d+)\.json""")

    private fun file(id: Int) = File(dir, "slot-$id.json")
    private fun store(id: Int) = SaveStore(file(id))

    private fun ids(): List<Int> =
        dir.listFiles()?.mapNotNull { pattern.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }?.sorted() ?: emptyList()

    /** Do mais recente ao mais antigo; slots com problema vão para o fim. */
    fun list(): List<SlotInfo> = ids().map { id ->
        try {
            val s = store(id).read()
            SlotInfo(id, s.name.ifBlank { "Jogo $id" }, s.day, s.credits, s.locationName, s.savedAt)
        } catch (e: SaveException) {
            SlotInfo(id, "Jogo $id", 0, 0, "", file(id).lastModified(), e.message ?: "ilegível")
        }
    }.sortedWith(compareByDescending<SlotInfo> { it.playable }.thenByDescending { it.savedAt }.thenByDescending { it.id })

    fun lastUsed(): SlotInfo? = list().firstOrNull { it.playable }

    /** Cria um slot novo com [game] e devolve o id. O nome padrão é "Jogo N". */
    fun create(game: Game, name: String? = null): Int {
        val id = (ids().maxOrNull() ?: 0) + 1
        save(id, name ?: "Jogo $id", game)
        return id
    }

    /** Grava o jogo no slot. Com decisão pendente não grava (mantém o último momento calmo) e devolve false. */
    fun save(id: Int, name: String, game: Game): Boolean {
        if (!game.canSave) return false
        store(id).write(game.toSave().copy(name = name, savedAt = clock()))
        return true
    }

    fun load(id: Int): Game {
        if (!file(id).isFile) throw SaveException("Não existe o jogo $id.")
        return Game.fromSave(store(id).read())
    }

    fun nameOf(id: Int): String = list().firstOrNull { it.id == id }?.name ?: "Jogo $id"

    /** Remove o slot de vez. Quem chama deve ter confirmado com o jogador. */
    fun delete(id: Int): Boolean = file(id).delete()

    fun rename(id: Int, name: String) {
        val s = store(id).read()
        store(id).write(s.copy(name = name.trim().ifBlank { "Jogo $id" }.take(40)))
    }
}
