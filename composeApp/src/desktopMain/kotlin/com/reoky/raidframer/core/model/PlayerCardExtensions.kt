package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.database.isPlayerNameOnWhitelist
import com.reoky.raidframer.core.definitions.findDebuffByName
import com.reoky.raidframer.core.definitions.findSkillByName
import com.reoky.raidframer.core.interactor.Log
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor


/*
  * Determine if the PlayerCard should be upgraded to a real player based on heuristics. (oh eek!)
  * The upgrade is permanent and cannot be undone. Also, the actual flag is changed outside this method
  * because the cache needs to be saved back to the database and the UI updated.
 */
fun PlayerCard.shouldUpgradeToPlayer(): Boolean {
  // rule out the easy stuff first
  if (this.name.contains(" ")) return false // only NPCs can have spaces in their names, auto-non-player
  if (this.name in listOf("Unknown Target", "Fren", "Meina", "Glenn", "Charybdis")) return false // we might want a blacklist feature in the future where people can add their own NPC names
  if (this.name.isPlayerNameOnWhitelist()) return true // name is on the whitelist, auto-upgrade
  this.recentDebuffGainedEvents.takeLast(100).let {
    return it.map { event -> event.debuff }.contains("Preparing Glider") // NPCs can't open their gliders
  }
}

/**
 * Add a damage event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDamageEvent(event: DamageEvent): PlayerCard {
  val targetIsPlayer = PlayerCacheInteractor.isRealPlayer(event.target)
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
    ),
    recentDamageEvents = (this.recentDamageEvents + event), // optional to takeLast(n)
    sessionDamageTotal = if (targetIsPlayer) this.sessionDamageTotal + event.damage else this.sessionDamageTotal
  )
}

/**
 * Add a heal event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postHealEvent(event: HealEvent): PlayerCard {
  val targetIsPlayer = PlayerCacheInteractor.isRealPlayer(event.target)
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
    ),
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
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(lastSeen = event.timestamp),
    recentCastSuccessfulCastEvents = (this.recentCastSuccessfulCastEvents + event) // optional to takeLast(n)
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
    recentBuffEndedEvents = (this.recentBuffEndedEvents + event) // optional to takeLast(n)
  )
}

/**
 * Add a debuff gained event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDebuffGainedEvent(event: DebuffGainedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(lastSeen = event.timestamp),
    recentDebuffGainedEvents = (this.recentDebuffGainedEvents + event), // optional to takeLast(n)
  )
}

/**
 * Add a debuff ended event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDebuffEndedEvent(event: DebuffEndedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(lastSeen = event.timestamp),
    recentDebuffEndedEvents = (this.recentDebuffEndedEvents + event), // optional to takeLast(n)
  )
}

/*
 * Essentially we maintain the reverse relationship on each PlayerCard (but only for applied debuffs/buffs)
 * so that we can track who applied what debuff to whom, and how many times, etc. This costs more memory but
 * makes analysis way easier later on, and uses less CPU to compute. (See: Graph Databases vs Relational Databases heh)
 */
fun PlayerCard.postDebuffAppliedEvent(event: DebuffAppliedEvent): PlayerCard {
  val isCC = findDebuffByName(event.debuff)?.consideredCC == true
  Log.info("PlayerCardExtensions", "${event.source} applied ${event.debuff} to ${event.target}: (isCC=$isCC)")
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalCCDelivered = if (isCC) cache.lifetimeTotalCCDelivered + 1 else cache.lifetimeTotalCCDelivered
    ),
    recentDebuffAppliedEvents = (this.recentDebuffAppliedEvents + event), // optional to takeLast(n)
    sessionDebuffTotal = this.sessionDebuffTotal + 1,
    sessionCCTotal = if (isCC) this.sessionCCTotal + 1 else this.sessionCCTotal,
  )
}

fun PlayerCard.postBuffAppliedEvent(event: BuffAppliedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(lastSeen = event.timestamp),
    recentBuffAppliedEvents = (this.recentBuffAppliedEvents + event) // optional to takeLast(n)
  )
}

