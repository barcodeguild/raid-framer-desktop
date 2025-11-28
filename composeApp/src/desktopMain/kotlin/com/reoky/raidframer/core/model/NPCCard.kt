package com.reoky.raidframer.core.model

/**
 * Data class representing an NPC's casting events in the game. This was created to be type compatible with PlayerCard,
 * as NPCs and Players share many of the same event types.
 */
data class NPCCard (
  override val name: String, // npc name
  override val lastEvent: Long, // last event for this npc

  // stuff that we hold in memory for NPCs too
  override val recentCastSuccessfulCastEvent: List<EventParserInteractor.SuccessfulCastEvent>,
  override val recentCastEvents: List<EventParserInteractor.CastingEvent>,
  override val recentDamageEvents: List<EventParserInteractor.DamageEvent>,
  override val recentHealEvents: List<EventParserInteractor.HealEvent>,
  override val recentBuffGainedEvents: List<EventParserInteractor.BuffGainedEvent>,
  override val recentBuffEndedEvent: List<EventParserInteractor.BuffEndedEvent>,
  override val recentDebuffGainedEvent: List<EventParserInteractor.DebuffGainedEvent>,
  override val recentDebuffEndedEvent: List<EventParserInteractor.DebuffEndedEvent>,

  // counters for NPCs
  override val sessionDamageTotal: Long,
  override val sessionHealTotal: Long,
  override val sessionCCTotal: Int,
  override val sessionDebuffTotal: Int

) : GameCard