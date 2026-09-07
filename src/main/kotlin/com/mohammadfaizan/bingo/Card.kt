package com.mohammadfaizan.bingo

import org.bukkit.Material
import java.util.Random

/** A generated bingo card: `size`×`size` distinct materials, reproducible from `seed`. */
class Card(val size: Int, val seed: Long, val items: List<Material>) {

    val cells: Int get() = size * size

    fun indexOf(mat: Material): Int = items.indexOf(mat)

    /** Row / column / diagonal index groups, for STANDARD and LOCKOUT win checks. */
    val lines: List<IntArray> by lazy {
        val ls = ArrayList<IntArray>()
        for (r in 0 until size) ls.add(IntArray(size) { c -> r * size + c })
        for (c in 0 until size) ls.add(IntArray(size) { r -> r * size + c })
        ls.add(IntArray(size) { i -> i * size + i })
        ls.add(IntArray(size) { i -> i * size + (size - 1 - i) })
        ls
    }

    companion object {
        fun generate(size: Int, seed: Long, pool: ItemPool): Card {
            val rng = Random(seed)
            val need = size * size
            val chosen = LinkedHashSet<Material>()
            var guard = 0
            while (chosen.size < need && guard++ < 100_000) chosen.add(pool.roll(rng))
            return Card(size, seed, chosen.toList())
        }
    }
}
