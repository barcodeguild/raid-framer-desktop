package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.database.PlayerCacheDao

/**
  * Save the current PlayerCard's cache to the database. The PlayerCacheInteractor does this periodically for all loaded PlayerCards.
  */
suspend fun PlayerCard.saveToCache(dao: PlayerCacheDao) {
  if (this.cache == null) return
  assert(this.isRealPlayer) // for good measure, friends
  dao.insert(this.cache)
}

/*
  * Determine if the PlayerCard should be upgraded to a real player based on heuristics. (oh eek!)
  * The upgrade is permanent and cannot be undone. Also, the actual flag is changed outside this method
  * because the cache needs to be saved back to the database and the UI updated.
 */
fun PlayerCard.shouldUpgradeToPlayer(): Boolean {
  // rule out the easy stuff first
  if (this.name.contains(" ")) return false // only NPCs can have spaces in their names
  if (this.name in listOf("Unknown", "Monster", "Critter")) return false // common NPC names
  val recentCasts = this.recentCastEvents.takeLast(100)
  val recentHeals = this.recentHealEvents.takeLast(100)
  return false
}

/**
 * Add a damage event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDamageEvent(event: DamageEvent): PlayerCard {
  return this.copy(
      lastEvent = event.timestamp,
      recentDamageEvents = (this.recentDamageEvents + event), // optional to takeLast(n)
      sessionDamageTotal = this.sessionDamageTotal + event.damage
    )
}

/**
 * Add a heal event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postHealEvent(event: HealEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentHealEvents = (this.recentHealEvents + event), // optional to takeLast(n)
    sessionHealTotal = this.sessionHealTotal + event.amount
  )
}

/**
 * Add a casting event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postCastingEvent(event: CastingEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentCastEvents = (this.recentCastEvents + event) // optional to takeLast(n)
  )
}

/**
 * Add a successful cast event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postSuccessfulCastEvent(event: SuccessfulCastEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentCastSuccessfulCastEvent = (this.recentCastSuccessfulCastEvent + event) // optional to takeLast(n)
  )
}

/**
 * Add a buff gained event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postBuffGainedEvent(event: BuffGainedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentBuffGainedEvents = (this.recentBuffGainedEvents + event) // optional to takeLast(n)
  )
}

/**
 * Add a buff ended event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postBuffEndedEvent(event: BuffEndedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentBuffEndedEvent = (this.recentBuffEndedEvent + event) // optional to takeLast(n)
  )
}

/**
 * Add a debuff gained event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDebuffGainedEvent(event: DebuffGainedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentDebuffGainedEvent = (this.recentDebuffGainedEvent + event), // optional to takeLast(n)
  )
}

/**
 * Add a debuff ended event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDebuffEndedEvent(event: DebuffEndedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    recentDebuffEndedEvent = (this.recentDebuffEndedEvent + event), // optional to takeLast(n)
  )
}

