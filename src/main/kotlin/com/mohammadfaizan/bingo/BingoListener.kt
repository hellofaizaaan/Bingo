package com.mohammadfaizan.bingo

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent

class BingoListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        BingoGame.onPlayerJoin(event.player)
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        BingoGame.onDeath(event.entity)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (!BingoGame.isCardItem(event.item)) return
        event.isCancelled = true
        CardMenu.open(event.player)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        when (event.inventory.holder) {
            is CardHolder -> event.isCancelled = true
            is LobbyHolder -> {
                event.isCancelled = true
                val p = event.whoClicked as? Player ?: return
                val slot = event.rawSlot
                if (slot == LobbyMenu.SLOT_SPECTATE) {
                    BingoGame.spectate(p)
                    p.closeInventory()
                    return
                }
                LobbyMenu.teamAt(slot)?.let { t ->
                    if (BingoGame.join(p, t.id)) Text.send(p, "<green>Joined <white>${t.display}</white>.")
                    Bukkit.getScheduler().runTask(Bingo.instance, Runnable { LobbyMenu.open(p) })
                }
            }
            else -> (event.whoClicked as? Player)?.let { scheduleScan(it) }
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        val h = event.inventory.holder
        if (h is CardHolder || h is LobbyHolder) event.isCancelled = true
    }

    @EventHandler
    fun onPickup(event: EntityPickupItemEvent) {
        (event.entity as? Player)?.let { scheduleScan(it) }
    }

    @EventHandler
    fun onCraft(event: CraftItemEvent) {
        (event.whoClicked as? Player)?.let { scheduleScan(it) }
    }

    /** Re-check this player's inventory one tick later (after the item has landed). */
    private fun scheduleScan(player: Player) {
        if (BingoGame.state != BingoGame.State.RUNNING) return
        if (BingoGame.teamOf(player.uniqueId) == null) return
        Bukkit.getScheduler().runTask(Bingo.instance, Runnable { BingoGame.scanPlayerNow(player) })
    }
}
