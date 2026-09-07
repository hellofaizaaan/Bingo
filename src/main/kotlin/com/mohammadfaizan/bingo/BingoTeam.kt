package com.mohammadfaizan.bingo

import java.util.UUID

/** A team: members plus the set of card cells they've claimed. */
class BingoTeam(val id: String, val display: String, val colorMm: String) {

    val members = LinkedHashSet<UUID>()

    var claimed = BooleanArray(0)
        private set

    fun initCard(cells: Int) {
        claimed = BooleanArray(cells)
    }

    val claimedCount: Int get() = claimed.count { it }

    fun has(i: Int): Boolean = i in claimed.indices && claimed[i]

    fun claim(i: Int) {
        if (i in claimed.indices) claimed[i] = true
    }

    /** `<color><bold>Name</bold></color>` — safe to drop into a MiniMessage string. */
    fun coloredName(): String = "<$colorMm><bold>$display</bold></$colorMm>"
}
