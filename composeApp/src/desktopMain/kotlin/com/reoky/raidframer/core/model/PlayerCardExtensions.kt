package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.definitions.copiedWithUtilityItemDetectionMiddleWare
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
  this.recentDebuffGainedEvents.takeLast(100).let {
    return it.map { event -> event.debuff }.contains("Preparing Glider") // NPCs can't open their gliders
  }
}

/**
 * Add a damage event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDamageEvent(event: DamageEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)
  return card.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalDamage = cache.lifetimeTotalDamage + event.damage
    ),
    recentDamageEvents = (this.recentDamageEvents + event), // optional to takeLast(n)
    sessionDamageTotal = this.sessionDamageTotal + event.damage
  )
}

/**
 * Add a heal event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postHealEvent(event: HealEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalHealing = cache.lifetimeTotalHealing + event.amount
    ),
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
    cache = cache?.copy(
      lastSeen = event.timestamp
    ),
    recentCastEvents = (this.recentCastEvents + event) // optional to takeLast(n)
  )
}

/**
 * Add a successful cast event to the PlayerCard, updating recent events.
 */

fun PlayerCard.postSuccessfulCastEvent(event: SuccessfulCastEvent): PlayerCard {
  val shouldIncrementPotionUses = event.spell in (listOf("Mana Potion", "Minor Mana Potion", "Healing Potion", "Minor Healing Potion", "Found Wild Ginseng!"))
  val isMarasNineTails = event.spell == "Charm (Rider Skill)" && (PlayerCacheInteractor.isRealPlayer(event.target) || RFConfig.state.value.allowPVEDamage)
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)
  return card.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalCharms = if (isMarasNineTails) cache.lifetimeTotalCharms + 1 else cache.lifetimeTotalCharms
    ),
    sessionPotionTotal = if (shouldIncrementPotionUses) this.sessionPotionTotal + 1 else this.sessionPotionTotal,
    sessionCharmTotal = if (isMarasNineTails) this.sessionCharmTotal + 1 else this.sessionCharmTotal,
    recentCastSuccessfulCastEvents = (this.recentCastSuccessfulCastEvents + event) // optional to takeLast(n)
  )
}

/**
 * Add a buff gained event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postBuffGainedEvent(event: BuffGainedEvent): PlayerCard {
  //val isBDGlider = if (event.buffId)
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp
    ),
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
 * Essentially we maintain the reverse relationship on each PlayerCard (but only for applied debuffs/buffs)
 * so that we can track who applied what debuff to whom, and how many times, etc. This costs more memory but
 * makes analysis way easier later on, and uses less CPU to compute. (See: Graph Databases vs Relational Databases heh)
 */
fun PlayerCard.postDebuffAppliedEvent(event: DebuffAppliedEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  val isCC = findDebuffByName(event.debuff)?.consideredCC == true
  val isCharm = event.debuff == "Charmed"
  val isDistress = event.debuff == "Distressed"
  val isSilence = event.debuff == "Silence"
  val isGlider = event.debuff == "Preparing Glider" && System.currentTimeMillis() - this.lastGliderUse > 5000L // glider debuff applied, but only count if more than 5 second since last use to avoid double-counting from game bug
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)
  return card.copy(
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

/*
 * Used to measure buff applications from this player to others.
 */
fun PlayerCard.postBuffAppliedEvent(event: BuffAppliedEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  val isSongs = event.buff.contains("Bulwark Ballad (Rank") || event.buff.contains("Bloody Chanty (Rank")
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)
  return card.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalSongs = if (isSongs) cache.lifetimeTotalSongs + 1 else cache.lifetimeTotalSongs,
      lifetimeTotalBuffsApplied = cache.lifetimeTotalBuffsApplied + 1
    ),
    recentBuffAppliedEvents = (this.recentBuffAppliedEvents + event), // optional to takeLast(n)
    sessionSongsTotal = if (isSongs) this.sessionSongsTotal + 1 else this.sessionSongsTotal,
    sessionBuffTotal = this.sessionBuffTotal + 1
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
 * Record that this player killed someone (Killing Blow method).
 */
fun PlayerCard.postKillEventKB(timestamp: Long, victimName: String): PlayerCard {
  return this.copy(
    lastEvent = timestamp,
    recentKillsKB = this.recentKillsKB + (timestamp to victimName),
    sessionKillTotalKB = this.sessionKillTotalKB + 1
  )
}

/**
 * Increment the death counter and record who did it (both methods).
 */
fun PlayerCard.postDeathEvent(
  timestamp: Long,
  killerMostDamage: String?,
  killerKillingBlow: String?
): PlayerCard {
  val updatedKilledBys = if (killerMostDamage != null) {
    this.recentKilledBys + (timestamp to killerMostDamage)
  } else {
    this.recentKilledBys
  }

  val updatedKilledBysKB = if (killerKillingBlow != null) {
    this.recentKilledByKB + (timestamp to killerKillingBlow)
  } else {
    this.recentKilledByKB
  }

  return this.copy(
    lastEvent = timestamp,
    cache = cache?.copy(
      lastSeen = timestamp,
      lifetimeTotalDeaths = cache.lifetimeTotalDeaths + 1
    ),
    sessionDeathTotal = this.sessionDeathTotal + 1,
    recentKilledBys = updatedKilledBys,
    recentKilledByKB = updatedKilledBysKB
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
