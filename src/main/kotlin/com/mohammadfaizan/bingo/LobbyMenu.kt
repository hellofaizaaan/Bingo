package com.mohammadfaizan.bingo

import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class LobbyHolder : InventoryHolder {
    lateinit var inv: Inventory
    override fun getInventory(): Inventory = inv
}

/** `/bingo lobby` — click a wool to join that team, or the barrier to spectate. */
object LobbyMenu {

    const val SLOT_SPECTATE = 22
    private const val MAX_TEAMS = 18

    fun open(player: Player) {
        if (BingoGame.state == BingoGame.State.RUNNING) {
            Text.send(player, "<red>A game is already running.")
            return
        }
        val holder = LobbyHolder()
        val inv = Bukkit.createInventory(
            holder, 27,
            Text.mm("<gradient:#4ade80:#22d3ee><bold>Pick a team</bold>"),
        )
        holder.inv = inv
        BingoGame.teams.values.take(MAX_TEAMS).forEachIndexed { i, t -> inv.setItem(i, teamItem(t, player)) }
        inv.setItem(SLOT_SPECTATE, plain(ItemStack(Material.BARRIER), "<gray>Spectate", "<dark_gray>watch without playing"))
        player.openInventory(inv)
    }

    fun teamAt(slot: Int): BingoTeam? =
        if (slot in 0 until MAX_TEAMS) BingoGame.teams.values.toList().getOrNull(slot) else null

    private fun teamItem(t: BingoTeam, viewer: Player): ItemStack {
        val item = ItemStack(paneMat(t.colorMm))
        val meta = item.itemMeta ?: return item
        val mine = BingoGame.teamOf(viewer.uniqueId)?.id == t.id
        meta.displayName(Text.mm("<${t.colorMm}><bold>${t.display}").decoration(TextDecoration.ITALIC, false))
        meta.lore(
            listOf(
                Text.mm("<gray>${t.members.size} players").decoration(TextDecoration.ITALIC, false),
                Text.mm(if (mine) "<green>✔ your team" else "<yellow>click to join").decoration(TextDecoration.ITALIC, false),
            ),
        )
        if (mine) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
        item.itemMeta = meta
        return item
    }

    private fun plain(item: ItemStack, title: String, lore: String): ItemStack {
        val meta = item.itemMeta ?: return item
        meta.displayName(Text.mm(title).decoration(TextDecoration.ITALIC, false))
        meta.lore(listOf(Text.mm(lore).decoration(TextDecoration.ITALIC, false)))
        item.itemMeta = meta
        return item
    }

    private fun paneMat(mm: String): Material = when (mm) {
        "red", "dark_red" -> Material.RED_WOOL
        "blue", "dark_blue" -> Material.BLUE_WOOL
        "green" -> Material.LIME_WOOL
        "dark_green" -> Material.GREEN_WOOL
        "yellow" -> Material.YELLOW_WOOL
        "gold" -> Material.ORANGE_WOOL
        "aqua", "dark_aqua" -> Material.CYAN_WOOL
        "light_purple" -> Material.MAGENTA_WOOL
        "dark_purple" -> Material.PURPLE_WOOL
        "gray" -> Material.LIGHT_GRAY_WOOL
        "dark_gray" -> Material.GRAY_WOOL
        "black" -> Material.BLACK_WOOL
        else -> Material.WHITE_WOOL
    }
}
