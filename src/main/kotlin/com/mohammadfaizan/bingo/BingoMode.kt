package com.mohammadfaizan.bingo

/** How a bingo game is won. */
enum class BingoMode {
    /** First team to a full row, column or diagonal. */
    STANDARD,

    /** First team to every cell on the card. */
    BLACKOUT,

    /** Cells lock globally when claimed; first team to a line of its own cells wins. */
    LOCKOUT,

    /** First team to `count-target` cells, lines ignored. */
    COUNT;

    companion object {
        fun parse(s: String?): BingoMode? =
            if (s == null) null else entries.firstOrNull { it.name.equals(s, ignoreCase = true) }
    }
}
