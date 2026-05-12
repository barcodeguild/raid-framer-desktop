package com.reoky.raidframer.core.model

/**
 * Similar to PlayerCard, except represents a battle companion/pet with associated combat data.
 * The problem is that pets aren't players, but they have owners, so the combat relationship is indirect.
 * So I figure, we just make one and do the same thing right?
 */
data class PetCard(
  val petId: String, // it's name + owner to lowercase spaced with _ separated by hyphen
  val name: String, // pet/companion name
  val owner: String, // owner's character name
  val recentCids: List<String>, // these change so keeping a list
  val lastEvent: Long, // timestamp of last event
  val petType: String = "default", // type for icon rendering

  // Recent events held in memory (not persisted)
  val recentDamageEvents: List<DamageEvent> = listOf(),
  val recentDebuffAppliedEvents: List<DebuffAppliedEvent> = listOf(),

  // Session totals
  val sessionDamageTotal: Long = 0L,
  val sessionDebuffTotal: Int = 0
)
