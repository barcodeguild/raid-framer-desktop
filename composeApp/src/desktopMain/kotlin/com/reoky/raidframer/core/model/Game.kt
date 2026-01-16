package com.reoky.raidframer.core.model


// the three factions as an enum with a string helper to restrive from string values that ignore case
enum class Faction(val value: String) {
  NUIA("Nuia"),
  HARANYA("Haranya"),
  PIRATE("Pirate"),
  UNKNOWN("Unknown");

  companion object {
    fun fromString(value: String): Faction {
      return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
  }
}

enum class FactionStatus(val value: String) {
  FRIENDLY("Friendly"),
  HOSTILE("Hostile"),
  NEUTRAL("Neutral"),
  UNKNOWN("Unknown");

  companion object {
    fun fromString(value: String): FactionStatus {
      return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
  }
}
