package com.mohammadfaizan.bingo

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/** Thin `game.yml` reader/writer for resuming an in-progress game across a restart. */
object GameStore {

    private lateinit var file: File

    fun init(plugin: Bingo) {
        file = File(plugin.dataFolder, "game.yml")
    }

    fun clear() {
        runCatching { if (file.exists()) file.delete() }
    }

    fun read(): YamlConfiguration? =
        if (::file.isInitialized && file.exists()) YamlConfiguration.loadConfiguration(file) else null

    fun write(block: (YamlConfiguration) -> Unit) {
        if (!::file.isInitialized) return
        val yml = YamlConfiguration()
        block(yml)
        runCatching { yml.save(file) }
    }
}
