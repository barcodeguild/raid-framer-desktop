package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.database.PlayerCacheEntity

/**
 * Represents a real player character with associated cached data and recent events.
 *
 * See: [PlayerCacheEntity] for database storage, and also PlayerCardExtensions for helper methods. eek
 */
data class PlayerCard (
  val name: String, // player character name
  val recentCids: List<String>, // these change so keeping a list
  val lastEvent: Long, // timestamp of the last event seen for this player
  val lastKnownFaction: String = Faction.UNKNOWN.value, // see Faction definitions
  val lastKnownFactionStatus: String = FactionStatus.UNKNOWN.value, // friendly / hostile / neutral
  val lastKnownGuild: String = "", // string (not used for anything yet, thinking maybe dominion / duels support for future release)
  val currentBuild: String,

  // cache management for player data
  val isLoaded: Boolean = false,
  val isRealPlayer: Boolean = false, // determined after some analysis
  val cache: PlayerCacheEntity?, // saved to the database

  // important buffs/debuffs currently active on the player
  val isBuildingAggression: Boolean = false,
  val isCharmed: Boolean = false,
  val isDistressed: Boolean = false,

  // held in memory only for each player, this prevents having singular exhaustive lists of events like before
  // are cleared between sessions and not persisted to the database
  val recentCastSuccessfulCastEvents: List<SuccessfulCastEvent> = listOf(),
  val recentCastEvents: List<CastingEvent> = listOf(),
  val recentDamageEvents: List<DamageEvent> = listOf(),
  val recentHealEvents: List<HealEvent> = listOf(),
  val recentBuffGainedEvents: List<BuffGainedEvent> = listOf(),
  val recentBuffEndedEvents: List<BuffEndedEvent> = listOf(),
  val recentDebuffGainedEvents: List<DebuffGainedEvent> = listOf(),
  val recentDebuffEndedEvents: List<DebuffEndedEvent> = listOf(),
  val recentKills: Map<Long, String> = mapOf(), // Timestamp -> Victim Name
  val recentKilledBys: Map<Long, String> = mapOf(), // Timestamp -> Killer Name (stored in reverse for ez lookup)

  // cost more memory but useful for quick access and uses less CPU than scanning all events on all cards
  // this solution is similar to what facebook did before switching to graph databases; effectively storing some data twice by stashing it all in memcached
  // don't look at me aaaaaaaaaa
  val recentBuffAppliedEvents: List<BuffAppliedEvent> = listOf(),
  val recentDebuffAppliedEvents: List<DebuffAppliedEvent> = listOf(),

  // session counter totals : when a new session starts, these reset to 0 and the totals are written to the database cache
  val sessionDamageTotal: Long = 0L,
  val sessionHealTotal: Long = 0L,
  val sessionCCTotal: Int = 0, // only crowd control effects (Snare, Stun, Silence, Trip, etc)
  val sessionDebuffTotal: Int = 0, // all debuffs
  val sessionCharmTotal: Int = 0,
  val sessionDistressTotal: Int = 0,
  val sessionSilenceTotal: Int = 0,
  val sessionGliderTotal: Int = 0,
  val sessionItemSkillTotal: Int = 0,
  val sessionPotionTotal: Int = 0,
  val sessionKillTotal: Int = 0,
  val sessionDeathTotal: Int = 0
)

