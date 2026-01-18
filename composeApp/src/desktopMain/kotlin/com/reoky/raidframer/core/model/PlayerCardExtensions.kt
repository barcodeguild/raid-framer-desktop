package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.definitions.META_CC_SPECS
import com.reoky.raidframer.core.definitions.META_DANCER_SPECS
import com.reoky.raidframer.core.definitions.META_HEALER_SPECS
import com.reoky.raidframer.core.definitions.META_MAGE_SPECS
import com.reoky.raidframer.core.definitions.META_MELEE_SPECS
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.findDebuffByName
import com.reoky.raidframer.core.interactor.Log
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor


/**
 * Determine if the PlayerCard should be upgraded to a real player based on heuristics. (oh eek!)
 * The upgrade is permanent and cannot be undone. Also, the actual flag is changed outside this method
 * because the cache needs to be saved back to the database and the UI updated.
 */
fun PlayerCard.shouldUpgradeToPlayer(): Boolean {
  if (this.name.contains(" ")) return false // only NPCs can have spaces in their names, auto-non-player
  if (this.name in listOf("Unknown Target", "Fren", "Meina", "Glenn", "Charybdis")) return false // we might want a blacklist feature in the future where people can add their own NPC names
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

/**
 * Increment the death counter.
 */
fun PlayerCard.postDeathEvent(timestamp: Long): PlayerCard {
  return this.copy(
    lastEvent = timestamp,
    cache = cache?.copy(lastSeen = timestamp),
    sessionDeathTotal = this.sessionDeathTotal + 1
  )
}

/**
 * Essentially we maintain the reverse relationship on each PlayerCard (but only for applied debuffs/buffs)
 * so that we can track who applied what debuff to whom, and how many times, etc. This costs more memory but
 * makes analysis way easier later on, and uses less CPU to compute. (See: Graph Databases vs Relational Databases heh)
 */
fun PlayerCard.postDebuffAppliedEvent(event: DebuffAppliedEvent): PlayerCard {
  val isCC = findDebuffByName(event.debuff)?.consideredCC == true
  if (isCC) Log.info("PlayerCardExt", "CC: ${event.source} applied ${event.debuff} to ${event.target} with ts ${event.timestamp}")
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

/**
 * Record that this player killed someone.
 */
fun PlayerCard.postKillEvent(timestamp: Long, victimName: String): PlayerCard {
  return this.copy(
    lastEvent = timestamp,
    // Add to recent kills map
    recentKills = this.recentKills + (timestamp to victimName),
    // Increment kill score
    sessionKillTotal = this.sessionKillTotal + 1
  )
}

/**
 * Increment the death counter and record who did it.
 */
fun PlayerCard.postDeathEvent(timestamp: Long, killerName: String?): PlayerCard {
  val updatedKilledBys = if (killerName != null) {
    this.recentKilledBys + (timestamp to killerName)
  } else {
    this.recentKilledBys
  }

  return this.copy(
    lastEvent = timestamp,
    cache = cache?.copy(lastSeen = timestamp),
    sessionDeathTotal = this.sessionDeathTotal + 1,
    recentKilledBys = updatedKilledBys
  )
}

/*
 * Guess the player's role based on their recent actions / spec / past roles. Really whatever we can glean.
 * This is going to be kind of rough and ready at first.
 */
fun PlayerCard.guessPlayerRole(): PlayerRole {
  val spec = SpecType.fromName(this.currentBuild)

  // First handle all the META classes
  if (spec in META_CC_SPECS) return PlayerRole.GREEN
  if (spec in META_HEALER_SPECS) return PlayerRole.PINK
  if (spec in META_MAGE_SPECS) return PlayerRole.RED
  if (spec in META_MELEE_SPECS) return PlayerRole.GREEN // ?
  if (spec in META_DANCER_SPECS) return PlayerRole.PURPLE

  return PlayerRole.BLUE
}

