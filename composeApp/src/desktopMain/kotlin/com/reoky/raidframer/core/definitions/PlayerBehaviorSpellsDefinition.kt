package com.reoky.raidframer.core.definitions

/**
 * Additional spell IDs that indicate a real player is casting.
 * These are spells that don't belong to any of the 13 player skill trees,
 * aren't utility items, gliders, or potions, but are only castable by real players.
 *
 * Examples include Fold Wings, Summon Anywhere Warehouse, and other player-specific actions.
 */
data class PlayerBehaviorSpell(
  val spellId: Int,
  val spellName: String,
  val description: String
)

val PLAYER_BEHAVIOR_SPELLS: Map<Int, PlayerBehaviorSpell> = listOf(
  PlayerBehaviorSpell(34031, "Fold Wings", "Player action to fold glider wings"),
  PlayerBehaviorSpell(33820, "Summon Anywhere Warehouse", "Player action to summon warehouse access"),
).associateBy { it.spellId }

/**
 * Check if a spell ID indicates real player behavior.
 */
fun isPlayerBehaviorSpell(spellId: Int): Boolean = spellId in PLAYER_BEHAVIOR_SPELLS
