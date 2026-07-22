package com.reoky.raidframer.core.definitions

/*
 * Blacklists for buff and debuff IDs that should be excluded from the battle graph dropdown.
 * These are either already tracked in specific categories (charms, distress, silence, CC)
 * or are not useful for PvP analysis.
 *
 * Usage: Import these sets in PlayerCardExtensions.kt or BattleGraphOverlay.kt
 * to filter out unwanted entries from sessionSpellBuffMap and sessionSpellDebuffMap.
 *
 * Format: blacklistedBuffIds = setOf(id to "Buff Name (comment)")
 * The name/comment field serves as documentation for maintainability.
 */

// Debuffs already tracked in specific categories (charms, distress, silence, CC)
// or otherwise not useful for the general debuff dropdown
val blacklistedDebuffIds: Set<Int> = setOf(
  // Charmed debuffs (tracked separately in sessionCharmToPlayer)
  771,    // Charmed (Mara's Nine Tails)
  13916,  // Charmed
  15995,  // Charmed
  21432,  // Charmed
  21434,  // Charmed
  21162,  // Charmed

  // Silenced debuffs (tracked separately in sessionSilenceToPlayer)
  245, 257, 266, 1098, 1177, 2115, 2116, 2743, 3868, 3928, 4039,
  5525, 6147, 6366, 6893, 6981, 7040, 7400, 14730, 15721, 15937,
  16100, 16989, 21161, 21987, 22013, 22239, 22520, 22538, 23358,
  23469, 23523, 23524, 23815, 24168, 25234, 25718, 26965, 27145,
  27345, 27681, 28595, 28646, 28676, 28682, 28683, 29667, 29668,
  29926, 29987, 30935, 31862,

  // Distressed debuffs (tracked separately in sessionDistressToPlayer)
  828, 6896, 14284, 15175, 24925,

  // Glider usage debuffs (not useful for PvP analysis)
  4622, 20121, 8000279,

  // Songs debuffs (tracked separately in sessionSongsTotal)
  853,2177, 2200, 21995, // Unguarded
  847, 2176, 219, 15040,  // Lethargy
  836, 2174, 2193, 15039, 31367, // Weakened Energy
  772, 2169, 2188, 6849, 15051, 16341, 21994 // Unpleasant Sensation
)

// Buffs that are not useful for the battle graph dropdown
// (e.g., passive buffs, mounting buffs, transformation buffs)
val blacklistedBuffIds: Set<Int> = setOf(
  // Add buff IDs here as they are discovered
  // Format: id  // buff name
)

// Names of debuffs to blacklist (fallback for when IDs aren't available)
val blacklistedDebuffNames: Set<String> = setOf(
  "Unknown",
  "Preparing Glider"
)

// Names of buffs to blacklist (fallback for when IDs aren't available)
val blacklistedBuffNames: Set<String> = setOf(
  "Unknown"
)
