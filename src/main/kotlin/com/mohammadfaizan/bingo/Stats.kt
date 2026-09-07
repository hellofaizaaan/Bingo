package com.mohammadfaizan.bingo

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

/** Per-player games-played / wins, stored in `stats.yml`. */
object Stats {

    private lateinit var file: File
    private lateinit var yml: YamlConfiguration

    fun init(plugin: Bingo) {
        file = File(plugin.dataFolder, "stats.yml")
        yml = YamlConfiguration.loadConfiguration(file)
    }

    data class Row(val uuid: UUID, val played: Int, val wins: Int)

    fun recordGame(participants: Collection<UUID>, winners: Collection<UUID>) {
        participants.forEach { yml.set("players.$it.played", yml.getInt("players.$it.played") + 1) }
        winners.forEach { yml.set("players.$it.wins", yml.getInt("players.$it.wins") + 1) }
        runCatching { yml.save(file) }
    }

    fun of(uuid: UUID): Row = Row(uuid, yml.getInt("players.$uuid.played"), yml.getInt("players.$uuid.wins"))

    fun top(n: Int): List<Row> {
        val sec = yml.getConfigurationSection("players") ?: return emptyList()
        return sec.getKeys(false)
            .mapNotNull { k -> runCatching { UUID.fromString(k) }.getOrNull() }
            .map { of(it) }
            .filter { it.played > 0 }
            .sortedWith(compareByDescending<Row> { it.wins }.thenByDescending { it.played })
            .take(n)
    }
}
