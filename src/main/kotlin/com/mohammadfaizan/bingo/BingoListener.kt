package com.mohammadfaizan.bingo

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerJoinEvent

class BingoListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        BingoGame.onPlayerJoin(event.player)
    }

    @EventHandler
    fun onMenuClick(event: InventoryClickEvent) {
        if (event.inventory.holder is CardHolder) event.isCancelled = true
    }

    @EventHandler
    fun onMenuDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is CardHolder) event.isCancelled = true
    }
}
