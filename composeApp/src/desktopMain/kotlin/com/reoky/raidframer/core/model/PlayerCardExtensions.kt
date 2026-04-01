package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.definitions.charmedDebuffIds
import com.reoky.raidframer.core.definitions.copiedWithUtilityItemDetectionMiddleWare
import com.reoky.raidframer.core.definitions.distressedDebuffIds
import com.reoky.raidframer.core.definitions.findDebuffByName
import com.reoky.raidframer.core.definitions.gliderUsageDebuffIds
import com.reoky.raidframer.core.definitions.silencedDebuffIds
import com.reoky.raidframer.core.definitions.copiedWithPotionDetectionMiddleWare
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
  val isOde = if (event.spell == "Ode to Recovery") true else false // is this ode?
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalHealing = cache.lifetimeTotalHealing + event.amount
    ),
    recentHealEvents = (this.recentHealEvents + event), // optional to takeLast(n)
    sessionHealTotal = this.sessionHealTotal + event.amount,
    sessionOdeHealsTotal = if (isOde) this.sessionOdeHealsTotal + event.amount else this.sessionOdeHealsTotal
  )
}

/*
 * When a damage event is posted a damage and a damage taken event are posted to the source and target player card's respectively. This way we can handle
 * manipulations to the target card separately from crediting the source. (This is the player card of the target)
 */
fun PlayerCard.postDamageTakenEvent(event: DamageEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalDamageTaken = cache.lifetimeTotalDamageTaken + event.damage
    ),
    sessionDamageTakenTotal = this.sessionDamageTakenTotal + event.damage
  )
}

/*
 * When a heals event is posted a player heal event and a heals received event are posted to the healer and the target respectively. The difference being
 * the player card of the healer gets credit for heals done and the player card of the target gets credit for heals received. (This is the player card of the target)
 */
fun PlayerCard.postHealsReceivedEvent(event: HealEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalHealsReceived = cache.lifetimeTotalHealsReceived + event.amount
    ),
    sessionHealsReceivedTotal = this.sessionHealsReceivedTotal + event.amount
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
  val isMarasNineTails = event.spell == "Charm (Rider Skill)" && (PlayerCacheInteractor.isRealPlayer(event.target) || RFConfig.state.value.allowPVEDamage)

  var card = this.copiedWithUtilityItemDetectionMiddleWare(event) // handles item spells
  card = card.copiedWithPotionDetectionMiddleWare(event) // increments potion usages

  return card.copy(
    lastEvent = event.timestamp,
    cache = card.cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalCharms = if (isMarasNineTails) (card.cache?.lifetimeTotalCharms ?: 0) + 1 else (card.cache?.lifetimeTotalCharms ?: 0)
    ),
    sessionCharmTotal = if (isMarasNineTails) card.sessionCharmTotal + 1 else card.sessionCharmTotal,
    recentCastSuccessfulCastEvents = (card.recentCastSuccessfulCastEvents + event)
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
  val isCharm = event.debuffId in charmedDebuffIds
  val isDistress = event.debuffId in distressedDebuffIds
  val isSilence = event.debuffId in silencedDebuffIds
  val isGlider = event.debuffId in gliderUsageDebuffIds && System.currentTimeMillis() - this.lastGliderUse > 5000L // glider debuff applied, but only count if more than 5 second since last use to avoid double-counting from game bug
  val isSongs = event.debuffId == 853 || event.debuffId == 847 || event.debuffId == 31367 || event.debuffId == 772 // Unguarded, Lethargy, Weakened Energy, Unpleasant Sensation
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)
  return card.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalDebuffsApplied = cache.lifetimeTotalDebuffsApplied + 1,
      lifetimeTotalCCDelivered = if (isCC) cache.lifetimeTotalCCDelivered + 1 else cache.lifetimeTotalCCDelivered,
      lifetimeTotalCharms = if (isCharm) cache.lifetimeTotalCharms + 1 else cache.lifetimeTotalCharms,
      lifetimeTotalSongs = if (isSongs) cache.lifetimeTotalSongs + 1 else cache.lifetimeTotalSongs,
      lifetimeTotalGliderUses = if (isGlider) cache.lifetimeTotalGliderUses + 1 else cache.lifetimeTotalGliderUses,
      lifetimeTotalDistresses = if (isDistress) cache.lifetimeTotalDistresses + 1 else cache.lifetimeTotalDistresses,
      lifetimeTotalSilences = if (isSilence) cache.lifetimeTotalSilences + 1 else cache.lifetimeTotalSilences
    ),
    recentDebuffAppliedEvents = (this.recentDebuffAppliedEvents + event), // optional to takeLast(n)
    sessionDebuffTotal = this.sessionDebuffTotal + 1,
    sessionCharmTotal = if (isCharm) sessionCharmTotal + 1 else sessionCharmTotal,
    sessionSongsTotal = if (isSongs) this.sessionSongsTotal + 1 else this.sessionSongsTotal,
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
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)
  return card.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalBuffsApplied = cache.lifetimeTotalBuffsApplied + 1
    ),
    recentBuffAppliedEvents = (this.recentBuffAppliedEvents + event), // optional to takeLast(n)
    sessionBuffTotal = this.sessionBuffTotal + 1
  )
}

/**
 * Record that this player killed someone.
 */
fun PlayerCard.postKillEvent(timestamp: Long, victimName: String): PlayerCard {
  return this.copy(
    lastEvent = timestamp,
    cache = cache?.copy(
      lastSeen = timestamp,
      lifetimeTotalKills = cache.lifetimeTotalKills + 1
    ),
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
    cache = cache?.copy(
      lastSeen = timestamp,
      lifetimeTotalKillsKB = cache.lifetimeTotalKillsKB + 1
    ),
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
