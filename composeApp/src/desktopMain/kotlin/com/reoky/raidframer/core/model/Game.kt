package com.reoky.raidframer.core.model

import kotlinx.serialization.Serializable

data class RaidMember(val name: String, val health: Int, val role: Int)

typealias Party = List<RaidMember>

// the three factions as an enum with a string helper to restrive from string values that ignore case
enum class Faction(val value: String) {
  NUIA("Nuia"),
  HARANYA("Haranya"),
  PIRATE("Pirate"),
  UNKNOWN("Unknown");

  companion object {
    fun fromString(value: String): Faction {
      return values().firstOrNull { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
  }
}


// raid framer buff event mode
@Serializable
data class ARRaidFrames(
  val slot: Int = 0,
  val playerName: String = "",
  val gearScore: Int = 0,
  val characterBuild: String = "",
  val lastZone: String = "",
  val distance: Int = -1,
  val lastUpdated: Long = 0L
)




// this is what the lua addon sends when a buff event occurs
// it's not perfectly compatible by we can pull the fields we need
@Serializable
data class ARBuffEvent(
  val cid: String? = null,
  val eventType: String? = null,
  val source: String? = null,
  val target: String? = null,
  val buffId: Int? = null,
  val buffName: String? = null,
  val school: String? = null,
  val auraType: String? = null,
  val unknownBool: Boolean? = null,
  val envType: String? = null,
  val amount: Double? = null,
  val result: String? = null,
  val timestamp: Long = 0L
)
