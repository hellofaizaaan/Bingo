package com.mohammadfaizan.bingo

import org.bukkit.plugin.java.JavaPlugin

class Bingo : JavaPlugin() {

    companion object {
        lateinit var instance: Bingo
            private set
    }

    override fun onEnable() {
        instance = this
        saveDefaultConfig()

        Stats.init(this)
        GameStore.init(this)
        BingoGame.init(this)
        server.pluginManager.registerEvents(BingoListener(), this)

        getCommand("bingo")?.let {
            val handler = BingoCommand()
            it.setExecutor(handler)
            it.tabCompleter = handler
        }

        BingoGame.resumeIfPresent()
        logger.info("Bingo ready.")
    }

    override fun onDisable() {
        BingoGame.shutdown()
        logger.info("Bingo disabled.")
    }
}
