package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.database.PlayerCacheEntity

/**
 * Represents a real player character with associated cached data and recent events.
 *
 * See: [PlayerCacheEntity] for database storage, and also PlayerCardExtensions for helper methods. eek
 */
data class PlayerCard (
  val name: String, // player character name
  val lastEvent: Long, // timestamp of the last event seen for this player

  // cache management for player data
  val isLoaded: Boolean = false,
  val isRealPlayer: Boolean = false, // determined after some analysis
  val cache: PlayerCacheEntity?, // saved to the database

  // recent builds / specs
  val currentBuild: String,

  // important buffs/debuffs currently active on the player
  val isBuildingAggression: Boolean = false,
  val isCharmed: Boolean = false,
  val isDistressed: Boolean = false,

  // held in memory only for each player, this prevents having singular exhaustive lists of events like before
  // are cleared between sessions and not persisted to the database
  val recentCastSuccessfulCastEvent: List<SuccessfulCastEvent> = listOf(),
  val recentCastEvents: List<CastingEvent> = listOf(),
  val recentDamageEvents: List<DamageEvent> = listOf(),
  val recentHealEvents: List<HealEvent> = listOf(),
  val recentBuffGainedEvents: List<BuffGainedEvent> = listOf(),
  val recentBuffEndedEvent: List<BuffEndedEvent> = listOf(),
  val recentDebuffGainedEvent: List<DebuffGainedEvent> = listOf(),
  val recentDebuffEndedEvent: List<DebuffEndedEvent> = listOf(),

  // session counter totals : when a new session starts, these reset to 0 and the totals are written to the database cache
  val sessionDamageTotal: Long = 0L,
  val sessionHealTotal: Long = 0L,
  val sessionCCTotal: Int = 0, // only crowd control effects (Snare, Stun, Silence, Trip, etc)
  val sessionDebuffTotal: Int = 0, // all debuffs
)