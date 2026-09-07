package com.mohammadfaizan.bingo

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerJoinEvent

class BingoListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        BingoGame.onPlayerJoin(event.player)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.inventory.holder is CardHolder) {
            event.isCancelled = true
            return
        }
        (event.whoClicked as? Player)?.let { scheduleScan(it) }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is CardHolder) event.isCancelled = true
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
