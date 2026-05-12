package com.reoky.raidframer.core.model

import kotlin.text.compareTo


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

enum class PlayerRole(val value: Int) {
  BLUE(0),
  GREEN(1),
  PINK(2),
  RED(3),
  PURPLE(4),
  UNKNOWN(-1);

  companion object {
    fun fromInt(value: Int): PlayerRole {
      return entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
  }
}
