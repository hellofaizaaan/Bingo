package com.mohammadfaizan.bingo

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

/** Marker holder for the read-only bingo card inventory. */
class CardHolder : InventoryHolder {
    lateinit var inv: Inventory
    override fun getInventory(): Inventory = inv
}

/** `/bingo card` — a chest view of the current card from the viewer's team's perspective. */
object CardMenu {

    fun open(player: Player) {
        val card = BingoGame.currentCard
        if (card == null) {
            Text.send(player, "<red>No card yet — ask an admin to <white>/bingo start</white>.")
            return
        }
        val holder = CardHolder()
        val inv = Bukkit.createInventory(
            holder, 54,
            Text.mm("<gradient:#4ade80:#22d3ee><bold>Bingo Card</bold></gradient> <gray>${card.size}×${card.size}"),
        )
        holder.inv = inv

        val myTeam = BingoGame.teamOf(player.uniqueId)
        for (i in card.cellList.indices) {
            val slot = slotFor(card.size, i / card.size, i % card.size)
            inv.setItem(slot, cellItem(card, i, myTeam))
        }
        player.openInventory(inv)
    }

    private fun slotFor(size: Int, row: Int, col: Int): Int = when (size) {
        3 -> (row + 2) * 9 + (col + 3)
        4 -> (row + 1) * 9 + (col + 3)
        6 -> row * 9 + (col + 2)
        else -> (row + 1) * 9 + (col + 2)
    }

    private fun cellItem(card: Card, i: Int, myTeam: BingoTeam?): ItemStack {
        val cell = card.cellList[i]
        val mineHas = myTeam?.has(i) == true
        val locked = BingoGame.lockedTeam(i)

        // spectator: no team — show who holds each cell
        if (myTeam == null) {
            val holders = BingoGame.teamsHolding(i)
            return if (holders.isEmpty()) {
                label(ItemStack(cell.icon), "<white>${cell.display}", "<dark_gray>unclaimed")
            } else {
                glow(ItemStack(cell.icon), "<white>${cell.display}", "<gray>held by ${holders.joinToString(", ") { it.display }}")
            }
        }

        return when {
            mineHas -> glow(ItemStack(cell.icon), "<green>${cell.display}", "<green>✔ obtained")
            locked != null && locked !== myTeam ->
                label(ItemStack(Material.BARRIER), "<red>${cell.display}", "<red>locked by ${locked.display}")
            else -> label(ItemStack(cell.icon), "<white>${cell.display}", "<dark_gray>not yet")
        }
    }

    private fun label(item: ItemStack, title: String, lore: String): ItemStack {
        val meta = item.itemMeta ?: return item
        meta.displayName(plain(title))
        meta.lore(listOf(plain(lore)))
        item.itemMeta = meta
        return item
    }

    private fun glow(item: ItemStack, title: String, lore: String): ItemStack {
        val meta = item.itemMeta ?: return item
        meta.displayName(plain(title))
        meta.lore(listOf(plain(lore)))
        meta.addEnchant(Enchantment.UNBREAKING, 1, true)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        item.itemMeta = meta
        return item
    }

    private fun plain(mm: String): Component = Text.mm(mm).decoration(TextDecoration.ITALIC, false)
}
