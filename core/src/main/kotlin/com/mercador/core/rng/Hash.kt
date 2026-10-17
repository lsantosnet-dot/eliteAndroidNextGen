package com.mercador.core.rng

/** SplitMix64: mistura de 64 bits com acesso direto (sem depender de ordem de geração). */
fun mix64(x: Long): Long {
    var z = x + -0x61c8864680b583ebL
    z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
    z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
    return z xor (z ushr 31)
}

/** Deriva uma semente filha a partir do pai e de uma sequência de chaves (nível da árvore de sementes). */
fun derive(parent: Long, vararg keys: Long): Long {
    var h = mix64(parent)
    for (k in keys) h = mix64(h xor mix64(k))
    return h
}

/** Gerador pseudoaleatório determinístico sobre SplitMix64. */
class Rng(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /** Inteiro em [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        return ((nextLong() ushr 1) % bound).toInt()
    }

    /** Inteiro em [lo, hi] (inclusive). */
    fun range(lo: Int, hi: Int): Int = lo + nextInt(hi - lo + 1)

    /** Double em [0, 1). */
    fun nextDouble(): Double = (nextLong() ushr 11) * (1.0 / (1L shl 53))

    fun chance(p: Double): Boolean = nextDouble() < p

    fun <T> pick(items: List<T>): T = items[nextInt(items.size)]
}
