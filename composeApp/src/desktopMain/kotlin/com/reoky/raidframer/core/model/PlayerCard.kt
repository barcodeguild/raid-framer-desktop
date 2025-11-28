package com.reoky.raidframer.core.model

import EventParserInteractor
import com.reoky.raidframer.core.database.PlayerCacheEntity

/**
 * Represents a real player character with associated cached data and recent events.
 *
 * See: [PlayerCacheEntity] for database storage, and also PlayerCardExtensions for helper methods. eek
 */
data class PlayerCard (
  override val name: String, // player character name
  override val lastEvent: Long, // timestamp of the last event seen for this player

  // cache management for player data
  val isLoaded: Boolean = false,
  val cache: PlayerCacheEntity, // saved to the database

  // held in memory only for each player, this prevents having singular exhaustive lists of events like before
  // are cleared between sessions and not persisted to the database
  override val recentCastSuccessfulCastEvent: List<EventParserInteractor.SuccessfulCastEvent> = listOf(),
  override val recentCastEvents: List<EventParserInteractor.CastingEvent> = listOf(),
  override val recentDamageEvents: List<EventParserInteractor.DamageEvent> = listOf(),
  override val recentHealEvents: List<EventParserInteractor.HealEvent> = listOf(),
  override val recentBuffGainedEvents: List<EventParserInteractor.BuffGainedEvent> = listOf(),
  override val recentBuffEndedEvent: List<EventParserInteractor.BuffEndedEvent> = listOf(),
  override val recentDebuffGainedEvent: List<EventParserInteractor.DebuffGainedEvent> = listOf(),
  override val recentDebuffEndedEvent: List<EventParserInteractor.DebuffEndedEvent> = listOf(),

  // session counter totals : when a new session starts, these reset to 0 and the totals are written to the database cache
  override val sessionDamageTotal: Long = 0L,
  override val sessionHealTotal: Long = 0L,
  override val sessionCCTotal: Int = 0, // only crowd control effects (Snare, Stun, Silence, Trip, etc)
  override val sessionDebuffTotal: Int = 0, // all debuffs
) : GameCard

// only players have caches and lazy loading, but npc events need to be
// held in memory too
interface GameCard {
  val name: String
  val lastEvent: Long

  val recentCastSuccessfulCastEvent: List<EventParserInteractor.SuccessfulCastEvent>
  val recentCastEvents: List<EventParserInteractor.CastingEvent>
  val recentDamageEvents: List<EventParserInteractor.DamageEvent>
  val recentHealEvents: List<EventParserInteractor.HealEvent>
  val recentBuffGainedEvents: List<EventParserInteractor.BuffGainedEvent>
  val recentBuffEndedEvent: List<EventParserInteractor.BuffEndedEvent>
  val recentDebuffGainedEvent: List<EventParserInteractor.DebuffGainedEvent>
  val recentDebuffEndedEvent: List<EventParserInteractor.DebuffEndedEvent>

  val sessionDamageTotal: Long
  val sessionHealTotal: Long
  val sessionCCTotal: Int
  val sessionDebuffTotal: Int
}