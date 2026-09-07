package com.mohammadfaizan.bingo

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import kotlin.random.Random

/** The whole game: state, teams, the card, the scan loop, and win detection. */
object BingoGame {

    enum class State { IDLE, COUNTDOWN, RUNNING, ENDED }

    private lateinit var plugin: Bingo
    private val rng = Random(System.nanoTime())

    var state: State = State.IDLE
        private set
    var mode: BingoMode = BingoMode.STANDARD
        private set

    private var size = 5
    private var card: Card? = null
    private var pool: ItemPool = ItemPool.from(null)

    val teams = LinkedHashMap<String, BingoTeam>()
    private val playerTeam = HashMap<UUID, String>()
    private val lockedBy = HashMap<Int, String>() // lockout: cell index -> team id

    private var scanTask: BukkitTask? = null
    private var countdownTask: BukkitTask? = null
    private var bar: BossBar? = null
    private var endsAt = 0L
    private var timeLimitSecs = 0

    val currentCard: Card? get() = card

    fun init(p: Bingo) {
        plugin = p
        reload()
    }

    fun reload() {
        pool = ItemPool.from(plugin.config.getConfigurationSection("item-pool"))
        loadTeams()
    }

    private fun loadTeams() {
        val sec = plugin.config.getConfigurationSection("teams")
        val wanted = LinkedHashMap<String, BingoTeam>()
        sec?.getKeys(false)?.forEach { key ->
            val t = sec.getConfigurationSection(key) ?: return@forEach
            val id = key.lowercase()
            val existing = teams[id]
            val team = BingoTeam(id, t.getString("display", key) ?: key, sanitizeColor(t.getString("color", "white")))
            if (existing != null) team.members.addAll(existing.members)
            wanted[id] = team
        }
        if (wanted.isEmpty()) {
            listOf("red", "blue", "green", "yellow").forEach { c ->
                wanted[c] = BingoTeam(c, c.replaceFirstChar { it.uppercase() }, c)
            }
        }
        // drop memberships for teams that no longer exist
        playerTeam.entries.removeIf { it.value !in wanted }
        teams.clear()
        teams.putAll(wanted)
    }

    // ---- membership ----

    fun join(player: Player, teamId: String): Boolean {
        val t = teams[teamId.lowercase()] ?: return false
        leave(player)
        t.members.add(player.uniqueId)
        playerTeam[player.uniqueId] = t.id
        return true
    }

    fun leave(player: Player) {
        playerTeam.remove(player.uniqueId)?.let { teams[it]?.members?.remove(player.uniqueId) }
    }

    fun teamOf(u: UUID): BingoTeam? = playerTeam[u]?.let { teams[it] }

    fun lockedTeam(i: Int): BingoTeam? = lockedBy[i]?.let { teams[it] }

    // ---- lifecycle ----

    fun start(sender: CommandSender?, m: BingoMode, requestedSize: Int, seed: Long?): Boolean {
        if (state == State.COUNTDOWN || state == State.RUNNING) {
            Text.send(sender, "<red>A game is already running.")
            return false
        }

        if (plugin.config.getBoolean("auto-assign", true)) autoAssign()
        val active = teams.values.filter { it.members.isNotEmpty() }
        if (active.isEmpty()) {
            Text.send(sender, "<red>Nobody is on a team. Use <white>/bingo join <team></white> first.")
            return false
        }

        mode = m
        size = requestedSize.coerceIn(3, 5)
        val used = seed ?: rng.nextLong()
        card = Card.generate(size, used, pool)
        lockedBy.clear()
        teams.values.forEach { it.initCard(size * size) }

        val players = activePlayers()
        val cfg = plugin.config
        players.forEach { p ->
            if (cfg.getBoolean("on-start.clear-inventory", true)) p.inventory.clear()
            if (cfg.getBoolean("on-start.heal", true)) {
                p.health = 20.0
                p.foodLevel = 20
                p.saturation = 20f
                p.fireTicks = 0
                p.activePotionEffects.forEach { e -> p.removePotionEffect(e.type) }
            }
            if (p.gameMode != GameMode.CREATIVE) p.gameMode = GameMode.SURVIVAL
        }
        if (cfg.getBoolean("on-start.spread", true)) spread(players, cfg.getInt("spread-radius", 500))
        if (cfg.getBoolean("on-start.set-time-day", true)) Bukkit.getWorlds().forEach { it.time = 1000 }

        timeLimitSecs = cfg.getInt("time-limit", 0).coerceAtLeast(0)
        endsAt = 0L

        state = State.COUNTDOWN
        startBar()
        Text.broadcast(
            "<white>Bingo — <yellow>${mode.name.lowercase()}</yellow> <gray>${size}×${size}, seed <white>$used</white>",
        )

        var cd = cfg.getInt("start-countdown", 10).coerceIn(0, 60)
        countdownTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            Runnable {
                if (state != State.COUNTDOWN) {
                    countdownTask?.cancel()
                    return@Runnable
                }
                if (cd <= 0) {
                    countdownTask?.cancel()
                    countdownTask = null
                    beginRunning()
                } else {
                    val n = cd
                    activePlayers().forEach {
                        it.showTitle(Title.title(Text.mm("<yellow><bold>$n"), Text.mm("<gray>get ready")))
                        it.playSound(it.location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f)
                    }
                    cd--
                }
            },
            0L,
            20L,
        )
        return true
    }

    private fun beginRunning() {
        state = State.RUNNING
        if (timeLimitSecs > 0) endsAt = System.currentTimeMillis() + timeLimitSecs * 1000L
        activePlayers().forEach {
            it.showTitle(Title.title(Text.mm("<green><bold>GO!"), Text.mm("<gray>first to ${goalText()}")))
            it.playSound(it.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.5f)
        }
        Text.broadcast("<green><bold>GO!</bold></green> <gray>First to ${goalText()} wins.")
        scanTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { scan() }, 10L, 10L)
        updateBar()
    }

    fun stop(sender: CommandSender?) {
        if (state == State.IDLE) {
            Text.send(sender, "<red>No game is running.")
            return
        }
        state = State.IDLE
        stopTasks()
        clearBar()
        card = null
        lockedBy.clear()
        Text.broadcast("<gray>Bingo stopped.")
    }

    fun reroll(sender: CommandSender?): Boolean {
        if (state == State.RUNNING) {
            Text.send(sender, "<red>Can't reroll while a game is running.")
            return false
        }
        val used = rng.nextLong()
        card = Card.generate(size, used, pool)
        teams.values.forEach { it.initCard(size * size) }
        lockedBy.clear()
        Text.send(sender, "<green>New card rolled — seed <white>$used</white>. Open it with <white>/bingo card</white>.")
        return true
    }

    // ---- the scan ----

    private fun scan() {
        if (state != State.RUNNING) return
        val c = card ?: return
        if (endsAt in 1..System.currentTimeMillis()) {
            finishTimeUp()
            return
        }

        for ((uuid, teamId) in playerTeam) {
            val team = teams[teamId] ?: continue
            val player = Bukkit.getPlayer(uuid) ?: continue
            if (player.gameMode == GameMode.SPECTATOR) continue
            for (stack in player.inventory.contents) {
                val mat = stack?.type ?: continue
                if (mat == Material.AIR) continue
                val idx = c.indexOf(mat)
                if (idx < 0) continue

                if (mode == BingoMode.LOCKOUT) {
                    if (lockedBy.containsKey(idx)) continue
                    lockedBy[idx] = team.id
                    team.claim(idx)
                } else {
                    if (team.has(idx)) continue
                    team.claim(idx)
                }
                announceClaim(team, mat)
                checkWin(team)
                if (state != State.RUNNING) return
            }
        }
        updateBar()
    }

    private fun checkWin(team: BingoTeam) {
        if (state != State.RUNNING) return
        val c = card ?: return
        val won = when (mode) {
            BingoMode.BLACKOUT -> team.claimedCount >= c.cells
            BingoMode.COUNT -> team.claimedCount >= countTarget()
            else -> c.lines.any { line -> line.all { team.has(it) } }
        }
        if (won) endWithWinner(team)
    }

    private fun endWithWinner(team: BingoTeam) {
        state = State.ENDED
        stopTasks()
        Text.broadcast("${team.coloredName()} <gray>wins the bingo!")
        activePlayers().forEach { p ->
            val win = p.uniqueId in team.members
            p.showTitle(
                Title.title(
                    Text.mm(if (win) "<green><bold>YOU WIN" else "${team.coloredName()} <gray>wins"),
                    Text.mm("<gray>${team.claimedCount} cells"),
                ),
            )
            p.playSound(p.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
            if (win) firework(p)
        }
        clearBar()
    }

    private fun finishTimeUp() {
        val ranked = teams.values.filter { it.members.isNotEmpty() }.sortedByDescending { it.claimedCount }
        val lead = ranked.firstOrNull()
        val tie = lead != null && ranked.count { it.claimedCount == lead.claimedCount } > 1
        if (lead == null || tie) {
            state = State.ENDED
            stopTasks()
            clearBar()
            Text.broadcast("<yellow>Time! It's a draw at <white>${lead?.claimedCount ?: 0}</white> cells.")
        } else {
            endWithWinner(lead)
        }
    }

    // ---- status ----

    fun status(sender: CommandSender) {
        Text.raw(
            sender,
            "<gradient:#4ade80:#22d3ee><bold>Bingo</bold></gradient> <dark_gray>·</dark_gray> <white>$state</white> " +
                "<dark_gray>·</dark_gray> <gray>${mode.name.lowercase()} ${size}×${size}",
        )
        card?.let { Text.raw(sender, "<gray>Seed: <white>${it.seed}</white>  ·  goal: <white>${goalText()}</white>") }
        teams.values.forEach { t ->
            Text.raw(sender, "  ${t.coloredName()} <gray>— ${t.members.size} players · ${t.claimedCount} cells")
        }
    }

    fun teamsList(sender: CommandSender) {
        Text.raw(sender, "<gray>Teams — join with <white>/bingo join <name></white>:")
        teams.values.forEach { t ->
            val names = t.members.mapNotNull { Bukkit.getOfflinePlayer(it).name }.joinToString(", ").ifEmpty { "—" }
            Text.raw(sender, "  ${t.coloredName()} <dark_gray>(${t.id})</dark_gray> <gray>$names")
        }
    }

    // ---- helpers ----

    private fun autoAssign() {
        val active = teams.values.toList()
        if (active.isEmpty()) return
        val unassigned = Bukkit.getOnlinePlayers().filter {
            it.uniqueId !in playerTeam &&
                (it.gameMode == GameMode.SURVIVAL || it.gameMode == GameMode.ADVENTURE) &&
                !it.hasPermission("bingo.exempt")
        }
        unassigned.forEachIndexed { i, p ->
            val t = active[i % active.size]
            t.members.add(p.uniqueId)
            playerTeam[p.uniqueId] = t.id
        }
    }

    private fun activePlayers(): List<Player> = playerTeam.keys.mapNotNull { Bukkit.getPlayer(it) }

    private fun spread(players: List<Player>, radius: Int) {
        if (radius <= 0 || players.isEmpty()) return
        val w = players.first().world
        val spawn = w.spawnLocation
        players.forEach { p ->
            var tries = 0
            while (tries++ < 24) {
                val x = spawn.blockX + rng.nextInt(-radius, radius + 1)
                val z = spawn.blockZ + rng.nextInt(-radius, radius + 1)
                val y = w.getHighestBlockYAt(x, z)
                if (y > w.minHeight) {
                    p.teleport(Location(w, x + 0.5, y + 1.0, z + 0.5, p.location.yaw, p.location.pitch))
                    break
                }
            }
        }
    }

    private fun announceClaim(team: BingoTeam, mat: Material) {
        val total = card?.cells ?: 0
        Text.broadcastRaw(
            "${team.coloredName()} <gray>got <white>${nice(mat)}</white> <dark_gray>(${team.claimedCount}/$total)",
        )
        activePlayers().forEach { it.playSound(it.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.1f) }
    }

    private fun firework(p: Player) {
        val fw = p.world.spawn(p.location, Firework::class.java)
        val meta = fw.fireworkMeta
        meta.addEffect(
            FireworkEffect.builder().withColor(Color.LIME, Color.AQUA).with(FireworkEffect.Type.BALL_LARGE)
                .flicker(true).trail(true).build(),
        )
        meta.power = 1
        fw.fireworkMeta = meta
    }

    private fun startBar() {
        val b = BossBar.bossBar(Component.text("Bingo"), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS)
        bar = b
        Bukkit.getOnlinePlayers().forEach { it.showBossBar(b) }
    }

    fun onPlayerJoin(player: Player) {
        bar?.let { if (state != State.IDLE && state != State.ENDED) player.showBossBar(it) }
    }

    private fun updateBar() {
        val b = bar ?: return
        val lead = teams.values.maxByOrNull { it.claimedCount }
        val leadName = lead?.display ?: "—"
        val leadCount = lead?.claimedCount ?: 0
        val total = card?.cells ?: 1
        if (endsAt > 0L) {
            val remain = ((endsAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            b.progress((remain.toFloat() / timeLimitSecs.coerceAtLeast(1)).coerceIn(0f, 1f))
            b.name(Text.mm("<green>Bingo <gray>— <white>${remain}s</white> left <dark_gray>|</dark_gray> lead <white>$leadName</white> $leadCount"))
        } else {
            b.progress((leadCount.toFloat() / total).coerceIn(0f, 1f))
            b.name(Text.mm("<green>Bingo <gray>— ${mode.name.lowercase()} <dark_gray>|</dark_gray> lead <white>$leadName</white> $leadCount/$total"))
        }
    }

    private fun clearBar() {
        bar?.let { b -> Bukkit.getOnlinePlayers().forEach { it.hideBossBar(b) } }
        bar = null
    }

    private fun stopTasks() {
        scanTask?.cancel(); scanTask = null
        countdownTask?.cancel(); countdownTask = null
    }

    private fun countTarget(): Int {
        val cells = size * size
        return plugin.config.getInt("count-target", (cells + 1) / 2 + 1).coerceIn(1, cells)
    }

    private fun goalText(): String = when (mode) {
        BingoMode.BLACKOUT -> "the whole card"
        BingoMode.COUNT -> "${countTarget()} items"
        else -> "a line"
    }

    private fun nice(mat: Material): String =
        mat.name.lowercase().replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    private val VALID_COLORS = setOf(
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
        "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white",
    )

    private fun sanitizeColor(c: String?): String =
        (c ?: "white").lowercase().takeIf { it in VALID_COLORS } ?: "white"
}
