package com.mohammadfaizan.bingo

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

/** Bingo's visual style: a MiniMessage theme + send helpers. */
object Text {

    private val MM = MiniMessage.miniMessage()

    const val PREFIX = "<gradient:#4ade80:#22d3ee><bold>BINGO</bold></gradient> <dark_gray>▏</dark_gray> "

    fun mm(s: String, vararg ph: Pair<String, String>): Component {
        val resolvers = Array<TagResolver>(ph.size) { Placeholder.parsed(ph[it].first, ph[it].second) }
        return MM.deserialize(s, *resolvers)
    }

    fun prefixed(s: String, vararg ph: Pair<String, String>): Component = mm(PREFIX + s, *ph)

    fun broadcast(s: String, vararg ph: Pair<String, String>) = Bukkit.broadcast(prefixed(s, *ph))

    fun broadcastRaw(s: String, vararg ph: Pair<String, String>) = Bukkit.broadcast(mm(s, *ph))

    fun send(target: CommandSender?, s: String, vararg ph: Pair<String, String>) {
        target?.sendMessage(prefixed(s, *ph))
    }

    fun raw(target: CommandSender, s: String, vararg ph: Pair<String, String>) = target.sendMessage(mm(s, *ph))
}
