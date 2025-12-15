package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.calc.MetricRawSample
import com.reoky.raidframer.core.database.PlayerCacheDao
import com.reoky.raidframer.core.definitions.findSkillByName
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor


/*
  * Determine if the PlayerCard should be upgraded to a real player based on heuristics. (oh eek!)
  * The upgrade is permanent and cannot be undone. Also, the actual flag is changed outside this method
  * because the cache needs to be saved back to the database and the UI updated.
 */
fun PlayerCard.shouldUpgradeToPlayer(): Boolean {
  // rule out the easy stuff first
  if (this.name.contains(" ")) return false // only NPCs can have spaces in their names
  if (this.name in listOf("Unknown Target", "Fren", "Meina", "Glenn")) return false // we might want a blacklist in the future
  this.recentDebuffGainedEvent.takeLast(100).let {
    return it.map { event -> event.debuff }.contains("Preparing Glider") // NPCs can't open their gliders
  }
}

/**
 * Add a damage event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDamageEvent(event: DamageEvent): PlayerCard {
  val isCC = findSkillByName(event.spell)?.consideredCC == true
  val targetIsPlayer = PlayerCacheInteractor.isRealPlayer(event.target)
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalCCDelivered = if (isCC && isRealPlayer) (cache.lifetimeTotalCCDelivered + 1) else cache.lifetimeTotalCCDelivered
    ),
    sessionCCTotal = if (isCC && isRealPlayer) this.sessionCCTotal + 1 else this.sessionCCTotal,
    recentDamageEvents = (this.recentDamageEvents + event), // optional to takeLast(n)
    sessionDamageTotal = if (targetIsPlayer) this.sessionDamageTotal + event.damage else this.sessionDamageTotal
  )
}

/**
 * Add a heal event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postHealEvent(event: HealEvent): PlayerCard {
  val isCC = findSkillByName(event.spell)?.consideredCC == true
  val targetIsPlayer = PlayerCacheInteractor.isRealPlayer(event.target)
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalCCDelivered = if (isCC && isRealPlayer) (cache.lifetimeTotalCCDelivered + 1) else cache.lifetimeTotalCCDelivered
    ),
    sessionCCTotal = if (isCC && isRealPlayer) this.sessionCCTotal + 1 else this.sessionCCTotal,
    recentHealEvents = (this.recentHealEvents + event), // optional to takeLast(n)
    sessionHealTotal = if (targetIsPlayer) this.sessionHealTotal + event.amount else this.sessionHealTotal
  )
}

/**
 * Add a casting event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postCastingEvent(event: CastingEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(lastSeen = event.timestamp),
    recentCastEvents = (this.recentCastEvents + event) // optional to takeLast(n)
  )
}

/**
 * Add a successful cast event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postSuccessfulCastEvent(event: SuccessfulCastEvent): PlayerCard {
  val isCC = findSkillByName(event.spell)?.consideredCC
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalCCDelivered = if (isCC == true) (cache.lifetimeTotalCCDelivered + 1) else cache.lifetimeTotalCCDelivered
    ),
    sessionCCTotal = if (isCC == true) this.sessionCCTotal + 1 else this.sessionCCTotal,
    recentCastSuccessfulCastEvent = (this.recentCastSuccessfulCastEvent + event) // optional to takeLast(n)
  )

}

/**
 * Add a buff gained event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postBuffGainedEvent(event: BuffGainedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(lastSeen = event.timestamp),
    recentBuffGainedEvents = (this.recentBuffGainedEvents + event) // optional to takeLast(n)
  )
}

/**
 * Add a buff ended event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postBuffEndedEvent(event: BuffEndedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(lastSeen = event.timestamp),
    recentBuffEndedEvent = (this.recentBuffEndedEvent + event) // optional to takeLast(n)
  )
}

/**
 * Add a debuff gained event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDebuffGainedEvent(event: DebuffGainedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(lastSeen = event.timestamp),
    recentDebuffGainedEvent = (this.recentDebuffGainedEvent + event), // optional to takeLast(n)
  )
}

/**
 * Add a debuff ended event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDebuffEndedEvent(event: DebuffEndedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(lastSeen = event.timestamp),
    recentDebuffEndedEvent = (this.recentDebuffEndedEvent + event), // optional to takeLast(n)
  )
}

