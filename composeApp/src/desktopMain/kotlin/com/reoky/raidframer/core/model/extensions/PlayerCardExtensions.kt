package com.reoky.raidframer.core.model.extensions

import com.reoky.raidframer.core.database.PlayerCacheDao
import com.reoky.raidframer.core.model.PlayerCard

/**
  * Save the current PlayerCard's cache to the database. The PlayerCacheInteractor does this periodically for all loaded PlayerCards.
  */
suspend fun PlayerCard.saveToCache(dao: PlayerCacheDao) {
  dao.insert(this.cache)
}

/**
 * Add a damage event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDamageEvent(event: EventParserInteractor.DamageEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentDamageEvents = (this.recentDamageEvents + event), // optional to takeLast(n)
    sessionDamageTotal = this.sessionDamageTotal + event.damage
  )
}

/**
 * Add a heal event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postHealEvent(event: EventParserInteractor.HealEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentHealEvents = (this.recentHealEvents + event), // optional to takeLast(n)
    sessionHealTotal = this.sessionHealTotal + event.amount
  )
}

/**
 * Add a casting event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postCastingEvent(event: EventParserInteractor.CastingEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentCastEvents = (this.recentCastEvents + event) // optional to takeLast(n)
  )
}

/**
 * Add a successful cast event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postSuccessfulCastEvent(event: EventParserInteractor.SuccessfulCastEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentCastSuccessfulCastEvent = (this.recentCastSuccessfulCastEvent + event) // optional to takeLast(n)
  )
}

/**
 * Add a buff gained event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postBuffGainedEvent(event: EventParserInteractor.BuffGainedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentBuffGainedEvents = (this.recentBuffGainedEvents + event) // optional to takeLast(n)
  )
}

/**
 * Add a buff ended event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postBuffEndedEvent(event: EventParserInteractor.BuffEndedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentBuffEndedEvent = (this.recentBuffEndedEvent + event) // optional to takeLast(n)
  )
}

/**
 * Add a debuff gained event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDebuffGainedEvent(event: EventParserInteractor.DebuffGainedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentDebuffGainedEvent = (this.recentDebuffGainedEvent + event), // optional to takeLast(n)
  )
}

/**
 * Add a debuff ended event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDebuffEndedEvent(event: EventParserInteractor.DebuffEndedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentDebuffEndedEvent = (this.recentDebuffEndedEvent + event), // optional to takeLast(n)
  )
}

