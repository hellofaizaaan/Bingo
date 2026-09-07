package com.mohammadfaizan.bingo

import org.bukkit.Material

/** One square on a card: either an exact item, or a "any of these" group. */
sealed interface Cell {
    val display: String

    /** Item to render in the card GUI. */
    val icon: Material

    fun matches(mat: Material): Boolean
}

data class MaterialCell(val mat: Material) : Cell {
    override val display: String get() = niceName(mat)
    override val icon: Material get() = mat
    override fun matches(mat: Material): Boolean = mat == this.mat
}

data class GroupCell(val name: String, val members: Set<Material>) : Cell {
    override val display: String get() = "any " + name.replace('-', ' ')
    override val icon: Material get() = members.firstOrNull() ?: Material.PAPER
    override fun matches(mat: Material): Boolean = mat in members
}

fun niceName(mat: Material): String =
    mat.name.lowercase().replace('_', ' ').split(' ')
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
