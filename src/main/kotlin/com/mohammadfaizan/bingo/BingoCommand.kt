package com.mohammadfaizan.bingo

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class BingoCommand : CommandExecutor, TabCompleter {

    private val subs = listOf("start", "stop", "join", "leave", "card", "teams", "status", "reroll", "reload")
    private val adminSubs = setOf("start", "stop", "reroll", "reload")
    private val modeNames = BingoMode.entries.map { it.name.lowercase() }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            help(sender, label)
            return true
        }

        val sub = args[0].lowercase()
        if (sub in adminSubs && !sender.hasPermission("bingo.admin")) {
            Text.send(sender, "<red>You don't have permission for that.")
            return true
        }

        when (sub) {
            "start" -> {
                val cfg = Bingo.instance.config
                val mode = BingoMode.parse(args.getOrNull(1))
                    ?: BingoMode.parse(cfg.getString("default-mode")) ?: BingoMode.STANDARD
                val sz = args.getOrNull(2)?.toIntOrNull() ?: cfg.getInt("default-size", 5)
                val seed = args.getOrNull(3)?.toLongOrNull()
                BingoGame.start(sender, mode, sz, seed)
            }
            "stop" -> BingoGame.stop(sender)
            "reroll" -> BingoGame.reroll(sender)
            "reload" -> {
                Bingo.instance.reloadConfig()
                BingoGame.reload()
                Text.send(sender, "<green>Config + item pool reloaded.")
            }
            "join" -> {
                val p = sender as? Player ?: return notPlayer(sender)
                val team = args.getOrNull(1) ?: run {
                    Text.send(sender, "<red>Usage: /$label join <team>")
                    return true
                }
                if (BingoGame.join(p, team)) Text.send(sender, "<green>Joined <white>$team</white>.")
                else Text.send(sender, "<red>No team '<white>$team</white>'. See <white>/bingo teams</white>.")
            }
            "leave" -> {
                val p = sender as? Player ?: return notPlayer(sender)
                BingoGame.leave(p)
                Text.send(sender, "<gray>Left your team.")
            }
            "card" -> {
                val p = sender as? Player ?: return notPlayer(sender)
                CardMenu.open(p)
            }
            "teams" -> BingoGame.teamsList(sender)
            "status" -> BingoGame.status(sender)
            else -> help(sender, label)
        }
        return true
    }

    private fun notPlayer(sender: CommandSender): Boolean {
        Text.send(sender, "<red>Players only.")
        return true
    }

    private fun help(sender: CommandSender, label: String) {
        Text.raw(sender, "<gradient:#4ade80:#22d3ee><bold>Bingo</bold></gradient> <dark_gray>·</dark_gray> <gray>commands")
        Text.raw(sender, "  <white>/$label start [mode] [size] [seed]</white> <gray>— begin (${modeNames.joinToString(", ")})")
        Text.raw(sender, "  <white>/$label join <team></white> <dark_gray>·</dark_gray> <white>leave</white> <dark_gray>·</dark_gray> <white>card</white> <dark_gray>·</dark_gray> <white>teams</white>")
        Text.raw(sender, "  <white>/$label status</white> <dark_gray>·</dark_gray> <white>stop</white> <dark_gray>·</dark_gray> <white>reroll</white> <dark_gray>·</dark_gray> <white>reload</white>")
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        return when {
            args.size == 1 -> subs.filter { it.startsWith(args[0].lowercase()) }
            args.size == 2 && args[0].equals("start", true) ->
                (modeNames + listOf("3", "4", "5")).filter { it.startsWith(args[1].lowercase()) }
            args.size == 2 && args[0].equals("join", true) ->
                BingoGame.teams.keys.filter { it.startsWith(args[1].lowercase()) }
            args.size == 3 && args[0].equals("start", true) -> listOf("3", "4", "5").filter { it.startsWith(args[2]) }
            else -> emptyList()
        }
    }
}
