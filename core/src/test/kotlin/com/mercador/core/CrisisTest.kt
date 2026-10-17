package com.mercador.core

import com.mercador.core.universe.Region
import com.mercador.core.universe.Universe
import com.mercador.core.world.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CrisisTest {
    @Test
    fun `parte dos sistemas nasce em crise ou tensao, o resto fica calmo, e e deterministico`() {
        var crisis = 0; var tense = 0; var total = 0
        for (seed in 1L..40L) {
            val w = World(Region.around(Universe(seed)))
            val again = World(Region.around(Universe(seed)))
            for ((i, s) in w.region.systems.withIndex()) {
                val m = w.meters(s)
                assertEquals(m, again.meters(again.region.systems[i]))
                total++
                if (m.rationing) crisis++
                if (m.discontent >= 35) tense++
            }
        }
        val c = crisis * 100.0 / total; val t = tense * 100.0 / total
        assertTrue(c in 8.0..25.0, "racionamento em $c% dos sistemas")
        assertTrue(t in 10.0..40.0, "descontentamento >= 35 em $t% dos sistemas")
    }
}
