package com.reoky.raidframer.core.model

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
  val isCharm = event.debuff == "Charmed"
  val isDistress = event.debuff == "Distressed"
  val isSilence = event.debuff == "Silence"
  val isGlider = event.debuff == "Preparing Glider" && System.currentTimeMillis() - this.lastGliderUse > 1000L // glider debuff applied, but only count if more than 1 second since last use to avoid double-counting from game bug
  if (isCC) Log.info("PlayerCardExt", "CC: ${event.source} applied ${event.debuff} to ${event.target} with ts ${event.timestamp}")
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalDebuffsApplied = cache.lifetimeTotalDebuffsApplied + 1,
      lifetimeTotalCCDelivered = if (isCC) cache.lifetimeTotalCCDelivered + 1 else cache.lifetimeTotalCCDelivered,
      lifetimeTotalCharms = if (isCharm) cache.lifetimeTotalCharms + 1 else cache.lifetimeTotalCharms,
      lifetimeTotalGliderUses = if (isGlider) cache.lifetimeTotalGliderUses + 1 else cache.lifetimeTotalGliderUses,
      lifetimeTotalDistresses = if (isDistress) cache.lifetimeTotalDistresses + 1 else cache.lifetimeTotalDistresses,
      lifetimeTotalSilences = if (isSilence) cache.lifetimeTotalSilences + 1 else cache.lifetimeTotalSilences
    ),
    recentDebuffAppliedEvents = (this.recentDebuffAppliedEvents + event), // optional to takeLast(n)
    sessionDebuffTotal = this.sessionDebuffTotal + 1,
    sessionCharmTotal = if (isCharm) sessionCharmTotal + 1 else sessionCharmTotal,
    sessionDistressTotal = if (isDistress) sessionDistressTotal + 1 else sessionDistressTotal,
    sessionSilenceTotal = if (isSilence) sessionSilenceTotal + 1 else sessionSilenceTotal,
    sessionGliderTotal = if (isGlider) sessionGliderTotal + 1 else sessionGliderTotal,
    sessionCCTotal = if (isCC) this.sessionCCTotal + 1 else this.sessionCCTotal,
    lastGliderUse = if (isGlider) event.timestamp else this.lastGliderUse, // update glider use timestamp if applicable
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
    cache = cache?.copy(
      lastSeen = timestamp,
      lifetimeTotalDeaths = cache.lifetimeTotalDeaths + 1
    ),
    sessionDeathTotal = this.sessionDeathTotal + 1,
    recentKilledBys = updatedKilledBys
  )
}

fun PlayerCard.updatePlayerLeadership(newLeadership: Int): PlayerCard {
  val timestamp = System.currentTimeMillis()
  return this.copy(
    leaderships = if (newLeadership in 1..5) newLeadership else 0, // zero is regular player
    lastEvent = timestamp,
    cache = this.cache?.copy(
      leaderships = if (newLeadership in 1..5) newLeadership else 0,
      lastSeen = timestamp
    )
  )
}
