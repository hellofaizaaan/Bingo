package com.mohammadfaizan.bingo

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import java.util.Random

/** Weighted [Cell] source for card generation, built from the `item-pool` config section. */
class ItemPool private constructor(
    private val tiers: Map<String, List<Cell>>,
    private val distribution: Map<String, Int>,
) {

    /** Distinct cells available — used to sanity-check requested card sizes. */
    val poolSize: Int get() = tiers.values.flatten().toSet().size

    fun roll(rng: Random): Cell {
        val list = tiers[weightedTier(rng)] ?: tiers.values.first()
        return list[rng.nextInt(list.size)]
    }

    private fun weightedTier(rng: Random): String {
        val entries = distribution.entries.filter { (tiers[it.key]?.isNotEmpty() == true) && it.value > 0 }
        if (entries.isEmpty()) return tiers.keys.first()
        val total = entries.sumOf { it.value }
        var roll = rng.nextInt(total)
        for (e in entries) {
            if (roll < e.value) return e.key
            roll -= e.value
        }
        return entries.last().key
    }

    companion object {
        fun from(section: ConfigurationSection?): ItemPool {
            val groups = LinkedHashMap<String, Set<Material>>()
            section?.getConfigurationSection("groups")?.let { g ->
                g.getKeys(false).forEach { key ->
                    val mats = g.getStringList(key).mapNotNull { Material.matchMaterial(it.trim().uppercase()) }.toSet()
                    if (mats.isNotEmpty()) groups[key.lowercase()] = mats
                }
            }

            val tiers = LinkedHashMap<String, List<Cell>>()
            val dist = LinkedHashMap<String, Int>()
            section?.getConfigurationSection("distribution")?.let { d ->
                d.getKeys(false).forEach { dist[it] = d.getInt(it) }
            }
            section?.getKeys(false)?.forEach { key ->
                if (key == "distribution" || key == "groups") return@forEach
                val cells = section.getStringList(key).mapNotNull { raw ->
                    val e = raw.trim()
                    if (e.startsWith("#")) {
                        val g = e.removePrefix("#").lowercase()
                        groups[g]?.let { GroupCell(g, it) }
                    } else {
                        Material.matchMaterial(e.uppercase())?.let { MaterialCell(it) }
                    }
                }
                if (cells.isNotEmpty()) tiers[key] = cells
            }

            if (tiers.isEmpty()) {
                tiers["default"] = listOf(
                    Material.STONE, Material.DIRT, Material.OAK_LOG, Material.IRON_INGOT,
                    Material.GOLD_INGOT, Material.DIAMOND, Material.COAL, Material.APPLE,
                ).map { MaterialCell(it) }
            }
            if (dist.isEmpty()) tiers.keys.forEach { dist[it] = 1 }
            return ItemPool(tiers, dist)
        }
    }
}
