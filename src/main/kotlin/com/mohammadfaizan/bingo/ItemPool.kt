package com.mohammadfaizan.bingo

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import java.util.Random

/** Weighted item source for card generation, built from the `item-pool` config section. */
class ItemPool private constructor(
    private val tiers: Map<String, List<Material>>,
    private val distribution: Map<String, Int>,
) {

    /** Total distinct materials available — used to sanity-check requested card sizes. */
    val poolSize: Int get() = tiers.values.flatten().toSet().size

    fun roll(rng: Random): Material {
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
            val tiers = LinkedHashMap<String, List<Material>>()
            val dist = LinkedHashMap<String, Int>()

            section?.getConfigurationSection("distribution")?.let { d ->
                d.getKeys(false).forEach { dist[it] = d.getInt(it) }
            }
            section?.getKeys(false)?.forEach { key ->
                if (key == "distribution") return@forEach
                val mats = section.getStringList(key).mapNotNull { Material.matchMaterial(it.trim().uppercase()) }
                if (mats.isNotEmpty()) tiers[key] = mats
            }

            if (tiers.isEmpty()) {
                tiers["default"] = listOf(
                    Material.STONE, Material.DIRT, Material.OAK_LOG, Material.IRON_INGOT,
                    Material.GOLD_INGOT, Material.DIAMOND, Material.COAL, Material.APPLE,
                )
            }
            if (dist.isEmpty()) tiers.keys.forEach { dist[it] = 1 }
            return ItemPool(tiers, dist)
        }
    }
}
