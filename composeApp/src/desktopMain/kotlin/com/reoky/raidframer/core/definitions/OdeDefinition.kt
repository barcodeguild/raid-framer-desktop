package com.reoky.raidframer.core.definitions

/**
 * Ode to Recovery spell detection.
 * The game doesn't produce spell ids for this spell on heal events,
 * so we check the spell names against the languages that the game client supports.
 * Only English, Korean, Chinese, and Russian are included as these are the only
 * language packs shipped by the ArcheAge game client.
 */
val odeSpellNamesI18N = listOf(
  "Ode to Recovery",      // English
  "치유의 무곡",            // Korean
  "生命乐章",               // Chinese
  "Песнь исцеления"       // Russian
)

/**
 * Check if a spell name matches Ode to Recovery.
 * Uses case-insensitive contains to handle variations in spell name formatting.
 */
fun isOdeToRecovery(spellName: String): Boolean {
  return odeSpellNamesI18N.any { spellName.contains(it, ignoreCase = true) }
}
