package com.reoky.raidframer.core.helpers

import com.reoky.raidframer.core.definitions.RaidBuffRequirements
import com.reoky.raidframer.core.definitions.toCopyString
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.RaidBuffGracePeriod

/**
 * Consolidated helper for building copy-to-clipboard messages used throughout
 * the Raid Caller overlay. Each function returns a single-line string suitable
 * for pasting into game chat.
 */
object RaidCallerCopyMessages {

    /**
     * Loot Threshold copy message.
     * Example: "The loot requirement for this raid is 150% or higher."
     */
    fun lootThreshold(threshold: Int): String {
        return "The loot requirement for this raid is ${threshold}% or higher."
    }

    /**
     * Buff Mode (Required Buffs) copy message — reuses the compact buff criteria string.
     * Example: "Required Buffs: Gob, Feast, Statue, Drum | Loot: 170%"
     */
    fun buffMode(requirements: RaidBuffRequirements): String {
        val copy = requirements.toCopyString()
        // toCopyString() already returns "Buffs: ..." or "No buffs required."
        // Replace "Buffs:" with "Required Buffs:" for the standalone message
        return if (copy.startsWith("Buffs:")) {
            "Required ${copy}"
        } else if (copy == "No buffs required.") {
            "No buffs required."
        } else {
            copy
        }
    }

    /**
     * Buff Grace period copy message.
     * Example: "The buff grace period is 30 minutes. Players should remain in range of the raid to be scanned for buffs. Buffs outside of grace are considered void."
     */
    fun buffGrace(gracePeriod: RaidBuffGracePeriod): String {
        val minutes = gracePeriod.millis / 60_000
        val timeLabel = if (minutes == 0L) "disabled" else "${minutes} minute${if (minutes != 1L) "s" else ""}"
        return "The buff grace period is $timeLabel. Players should remain in range of the raid to be scanned for buffs. Buffs outside of grace are considered void."
    }

    /**
     * Gear Score summary copy message.
     * Example: "The raid average gear score is 17,362. The player with the highest GS is [name] at 20,123, and lowest GS is [name] with a GS of 1,456."
     */
    fun gearScore(
        avgGs: Int,
        lowestGs: Int,
        highestGs: Int,
        lowestPlayerName: String? = null,
        highestPlayerName: String? = null
    ): String {
        val avgPart = "The raid average gear score is ${avgGs.toLocaleString()}."
        val details = mutableListOf<String>()
        if (highestPlayerName != null && highestGs > 0) {
            details.add("The player with the highest GS is $highestPlayerName at ${highestGs.toLocaleString()}")
        }
        if (lowestPlayerName != null && lowestGs > 0) {
            details.add("lowest GS is $lowestPlayerName with a GS of ${lowestGs.toLocaleString()}")
        }
        return if (details.isNotEmpty()) {
            "$avgPart ${details.joinToString(", ")}."
        } else {
            "$avgPart Avg: ${avgGs.toLocaleString()}, Lowest: ${lowestGs.toLocaleString()}, Highest: ${highestGs.toLocaleString()}."
        }
    }

    /**
     * Raid counts copy message.
     * Example: "There are 25 players in raid one and 24 players in raid two for 49 players in total."
     */
    fun raidCounts(mainRaidCount: Int, coRaidCount: Int): String {
        val total = mainRaidCount + coRaidCount
        return "There are $mainRaidCount players in raid one and $coRaidCount players in raid two for $total players in total."
    }

    /**
     * Dragon breath usage copy message.
     * Example: "Dragon Usage in this PvP so far: Nuia 4 breaths (12M dmg), Haranya 7 breaths (24M dmg) and Pirate 0 breaths for 0 dmg."
     */
    fun dragon(
        counts: Map<Faction, Int>,
        damages: Map<Faction, Long>
    ): String {
        val factions = Faction.entries.filter { it != Faction.UNKNOWN }
        val parts = factions.map { faction ->
            val count = counts[faction] ?: 0
            val damage = damages[faction] ?: 0L
            val breathLabel = if (count == 1) "1 breath" else "$count breaths"
            val dmgLabel = if (damage > 0) " (${damage.humanReadableAbbreviation()} dmg)" else " for 0 dmg"
            "${faction.name.decapitalize()} $breathLabel$dmgLabel"
        }
        return "Dragon Usage in this PvP so far: ${parts.joinToString(", ") { part ->
            // Fix the last item with "and"
            part
        }.let { joined ->
            val lastComma = joined.lastIndexOf(',')
            if (lastComma >= 0) {
                joined.substring(0, lastComma) + " and" + joined.substring(lastComma + 1)
            } else {
                joined
            }
        }}."
    }

    /**
     * Riso missiles usage copy message.
     * Example: "Riso Missiles in this PvP so far: Nuia 4 uses (200k dmg), Haranya 1 use (74k dmg), and Pirate with 3 uses (320k dmg)."
     */
    fun riso(
        counts: Map<Faction, Int>,
        damages: Map<Faction, Long>
    ): String {
        val factions = Faction.entries.filter { it != Faction.UNKNOWN }
        val parts = factions.map { faction ->
            val count = counts[faction] ?: 0
            val damage = damages[faction] ?: 0L
            val useLabel = if (count == 1) "1 use" else "$count uses"
            val dmgLabel = if (damage > 0) " (${damage.humanReadableAbbreviation()} dmg)" else ""
            "${faction.name.decapitalize()} $useLabel$dmgLabel"
        }
        val joined = parts.joinToString(", ")
        val lastComma = joined.lastIndexOf(',')
        val withAnd = if (lastComma >= 0) {
            joined.substring(0, lastComma) + " and" + joined.substring(lastComma + 1)
        } else {
            joined
        }
        return "Riso Missiles in this PvP so far: $withAnd."
    }

    /**
     * Coherence stats copy message.
     * Example: "In this PvP: Raid members are in render about 79% of the time, near raid (<60 meters) 67% of time, and inside the clump (<15 meters), about 37% of the time."
     */
    fun coherence(renderPct: Int, raidPct: Int, clumpPct: Int): String {
        return "In this PvP: Raid members are in render about ${renderPct}% of the time, near raid (<60 meters) ${raidPct}% of time, and inside the clump (<15 meters), about ${clumpPct}% of the time."
    }

    /**
     * Meta class breakdown copy message.
     * Example: "This raid currently has: 4 CC, 5 Healers, 10 Mages, 2 Dancers, and 5 Ranged based on player classes out of 49. 10 people are playing non-meta classes."
     */
    fun meta(
        cc: Int,
        healer: Int,
        melee: Int,
        mage: Int,
        dancer: Int,
        ranged: Int,
        nonMeta: Int,
        totalPlayers: Int
    ): String {
        val parts = mutableListOf<String>()
        if (cc > 0) parts.add("$cc CC")
        if (healer > 0) parts.add("$healer Healer${if (healer != 1) "s" else ""}")
        if (mage > 0) parts.add("$mage Mage${if (mage != 1) "s" else ""}")
        if (dancer > 0) parts.add("$dancer Dancer${if (dancer != 1) "s" else ""}")
        if (ranged > 0) parts.add("$ranged Ranged")
        if (melee > 0) parts.add("$melee Melee")

        val classesPart = if (parts.isNotEmpty()) {
            val joined = parts.joinToString(", ")
            val lastComma = joined.lastIndexOf(',')
            val withAnd = if (lastComma >= 0) {
                joined.substring(0, lastComma) + ", and" + joined.substring(lastComma + 1)
            } else {
                joined
            }
            "This raid currently has: $withAnd based on player classes out of $totalPlayers."
        } else {
            "This raid has no recognized meta class assignments out of $totalPlayers."
        }

        return if (nonMeta > 0) {
            "$classesPart $nonMeta people are playing non-meta classes."
        } else {
            classesPart
        }
    }

    /**
     * SoTF (Soul of the Fallen / War Time) copy message.
     * Example: "Out of 49 raid members 43 players have either SoTF or War Time buff."
     */
    fun soTf(count: Int, total: Int): String {
        return "Out of $total raid members $count players have either SoTF or War Time buff."
    }

    /**
     * Rebirth trauma copy message (for pie chart).
     * Example: "23/52 players have rebirth with an average of 1 min and 32 seconds."
     */
    fun rebirth(count: Int, total: Int, avgText: String): String {
        return "$count/$total players have rebirth with an average of $avgText."
    }

    /**
     * Loot buffed copy message (for pie chart).
     * Example: "15/52 players are currently loot buffed to the 170% level."
     */
    fun lootBuffed(count: Int, total: Int, avgPercent: Int): String {
        return "$count/$total players are currently loot buffed to the ${avgPercent}% level."
    }

    /**
     * Min buffed copy message (for pie chart).
     * Example: "5/52 players are currently buffed to the Custom level."
     */
    fun minBuffed(count: Int, total: Int, modeLabel: String): String {
        return "$count/$total players are currently buffed to the $modeLabel level."
    }
}

/**
 * Formats an integer with locale-aware thousand separators (e.g., 17,362).
 */
private fun Int.toLocaleString(): String {
    return String.format("%,d", this)
}
