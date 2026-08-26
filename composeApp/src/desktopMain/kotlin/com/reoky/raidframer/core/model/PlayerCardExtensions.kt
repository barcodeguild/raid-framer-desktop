package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.PlayerCacheEntity
import com.reoky.raidframer.core.definitions.charmedDebuffIds
import com.reoky.raidframer.core.definitions.copiedWithUtilityItemDetectionMiddleWare
import com.reoky.raidframer.core.definitions.distressedDebuffIds
import com.reoky.raidframer.core.definitions.findDebuffByName
import com.reoky.raidframer.core.definitions.findDebuffById
import com.reoky.raidframer.core.definitions.gliderUsageDebuffIds
import com.reoky.raidframer.core.definitions.copiedWithGliderDetectionMiddleWare
import com.reoky.raidframer.core.definitions.silencedDebuffIds
import com.reoky.raidframer.core.definitions.tigerStrikeDebuffIds
import com.reoky.raidframer.core.definitions.freezeDebuffIds
import com.reoky.raidframer.core.definitions.trippedDebuffIds
import com.reoky.raidframer.core.definitions.bubbleTrapDebuffIds
import com.reoky.raidframer.core.definitions.bracingBlastImmunityBuffIds
import com.reoky.raidframer.core.definitions.shieldStripDebuffIds
import com.reoky.raidframer.core.definitions.weaponDisablesDebuffIds
import com.reoky.raidframer.core.definitions.potionDisablesDebuffIds
import com.reoky.raidframer.core.definitions.bdGliderDebuffIds
import com.reoky.raidframer.core.definitions.crystalWingsDebuffIds
import com.reoky.raidframer.core.definitions.gliderDisablesDebuffIds
import com.reoky.raidframer.core.definitions.provokedDebuffIds
import com.reoky.raidframer.core.definitions.defianceBuffIds
import com.reoky.raidframer.core.definitions.gardenDefianceCastId
import com.reoky.raidframer.core.definitions.purgeBuffId
import com.reoky.raidframer.core.definitions.blacklistedDebuffIds
import com.reoky.raidframer.core.definitions.blacklistedDebuffNames
import com.reoky.raidframer.core.definitions.blacklistedBuffNames
import com.reoky.raidframer.core.definitions.copiedWithPotionDetectionMiddleWare
import com.reoky.raidframer.core.definitions.isOdeToRecovery
import com.reoky.raidframer.core.definitions.sacrificeBuffIds
import com.reoky.raidframer.core.definitions.deepTranquilityBuffId
import com.reoky.raidframer.core.definitions.throwDaggerDebuffIds
import com.reoky.raidframer.core.definitions.absorbLifeforceDebuffIds
import com.reoky.raidframer.core.definitions.blindedByCrowsDebuffIds
import com.reoky.raidframer.core.definitions.mistSunderDebuffIds
import com.reoky.raidframer.core.definitions.corrosiveBarrageDebuffIds
import com.reoky.raidframer.core.definitions.stunDebuffIds
import com.reoky.raidframer.core.definitions.staggerDebuffIds
import com.reoky.raidframer.core.definitions.petrificationDebuffIds
import com.reoky.raidframer.core.definitions.deedendDebuffIds
import com.reoky.raidframer.core.definitions.regularSunderBuffIds
import com.reoky.raidframer.core.definitions.impaleDebuffIds
import com.reoky.raidframer.core.definitions.protectiveWingsBuffIds
import com.reoky.raidframer.core.definitions.courageousActionBuffIds
import com.reoky.raidframer.core.definitions.manaBarrierBuffIds
import com.reoky.raidframer.core.definitions.reviveSpellIds
import com.reoky.raidframer.core.definitions.lootBuffAmountForIds
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor

// Performance: configurable event history depth from settings
private val eventHistoryDepth: Int get() = RFConfig.state.value.performanceEventHistoryDepth.coerceIn(50, 5000)

// Performance: configurable battle graph spell depth (max targets per player for BySpell maps)
private val battleGraphSpellDepth: Int get() = RFConfig.state.value.performanceBattleGraphSpellDepth.coerceIn(10, 200)
private const val MAX_SPELLS_PER_TARGET = 10

/**
 * Caps a nested target→(spell→value) map to keep only the top [maxTargets] targets
 * by total value, and within each target, only the top [maxSpells] spells.
 * This bounds memory from O(targets × spells) to O(maxTargets × maxSpells).
 */
private fun <T : Number> Map<String, Map<String, T>>.cappedBySpell(
  maxTargets: Int,
  maxSpells: Int
): Map<String, Map<String, T>> {
  if (size <= maxTargets && values.all { it.size <= maxSpells }) return this
  return entries
    .sortedByDescending { (_, spells) -> spells.values.sumOf { it.toLong() } }
    .take(maxTargets)
    .associate { (target, spells) ->
      target to if (spells.size <= maxSpells) spells
      else spells.entries
        .sortedByDescending { it.value.toLong() }
        .take(maxSpells)
        .associate { it.key to it.value }
    }
}

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
  //if (event.source == event.target) return this // skip self-damage
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)
  return card.copy(
    lastEvent = event.timestamp,
    cache = card.cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalDamage = (card.cache?.lifetimeTotalDamage ?: 0L) + event.damage
    ),
    recentDamageEvents = (this.recentDamageEvents + event).takeLast(eventHistoryDepth),
    sessionSpellDamageMap = run {
      val spellKey = event.spell.ifBlank { "Unknown" }
      this.sessionSpellDamageMap + (spellKey to ((this.sessionSpellDamageMap[spellKey] ?: 0L) + event.damage))
    },
    sessionDamageTotal = this.sessionDamageTotal + event.damage,
    sessionDamageToPlayer = if (RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionDamageToPlayer + (event.target to ((this.sessionDamageToPlayer[event.target] ?: 0L) + event.damage))
    } else this.sessionDamageToPlayer,
    sessionDamageToPlayerBySpell = if (RFConfig.state.value.performanceBattleGraphEnabled) {
      run {
        val spellKey = event.spell.ifBlank { "Unknown" }
        val targetMap = this.sessionDamageToPlayerBySpell[event.target] ?: emptyMap()
        (this.sessionDamageToPlayerBySpell + (event.target to (targetMap + (spellKey to ((targetMap[spellKey] ?: 0L) + event.damage)))))
          .cappedBySpell(battleGraphSpellDepth, MAX_SPELLS_PER_TARGET)
      }
    } else this.sessionDamageToPlayerBySpell
  )
}

/**
 * Add a heal event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postHealEvent(event: HealEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  //if (event.source == event.target) return this // skip self-heals
  val isOde = isOdeToRecovery(event.spell)
  val allowOdeAsHeal = RFConfig.state.value.allowOdeToRecoveryCountAsHeals
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalHealing = cache.lifetimeTotalHealing + event.amount
    ),
    recentHealEvents = (this.recentHealEvents + event).takeLast(eventHistoryDepth),
    sessionHealTotal = if (isOde && !allowOdeAsHeal) this.sessionHealTotal else this.sessionHealTotal + event.amount,
    sessionOdeHealsTotal = if (isOde) this.sessionOdeHealsTotal + event.amount else this.sessionOdeHealsTotal,
    sessionSpellHealMap = run {
      val spellKey = event.spell.ifBlank { "Unknown" }
      if (isOde && !allowOdeAsHeal) {
        this.sessionSpellHealMap
      } else {
        this.sessionSpellHealMap + (spellKey to ((this.sessionSpellHealMap[spellKey] ?: 0L) + event.amount))
      }
    },
    sessionHealToPlayer = if (!RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionHealToPlayer
    } else if (isOde && !allowOdeAsHeal) {
      this.sessionHealToPlayer
    } else {
      this.sessionHealToPlayer + (event.target to ((this.sessionHealToPlayer[event.target] ?: 0L) + event.amount))
    },
    sessionHealToPlayerBySpell = if (!RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionHealToPlayerBySpell
    } else if (isOde && !allowOdeAsHeal) {
      this.sessionHealToPlayerBySpell
    } else {
      run {
        val spellKey = event.spell.ifBlank { "Unknown" }
        val targetMap = this.sessionHealToPlayerBySpell[event.target] ?: emptyMap()
        (this.sessionHealToPlayerBySpell + (event.target to (targetMap + (spellKey to ((targetMap[spellKey] ?: 0L) + event.amount)))))
          .cappedBySpell(battleGraphSpellDepth, MAX_SPELLS_PER_TARGET)
      }
    }
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
    sessionDamageTakenTotal = this.sessionDamageTakenTotal + event.damage,
    sessionDamageFromPlayer = if (RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionDamageFromPlayer + (event.source to ((this.sessionDamageFromPlayer[event.source] ?: 0L) + event.damage))
    } else this.sessionDamageFromPlayer
  )
}

/*
 * When a heals event is posted a player heal event and a heals received event are posted to the healer and the target respectively. The difference being
 * the player card of the healer gets credit for heals done and the player card of the target gets credit for heals received. (This is the player card of the target)
 */
fun PlayerCard.postHealsReceivedEvent(event: HealEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  val isOde = isOdeToRecovery(event.spell)
  val allowOdeAsHeal = RFConfig.state.value.allowOdeToRecoveryCountAsHeals
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalHealsReceived = cache.lifetimeTotalHealsReceived + event.amount
    ),
    sessionHealsReceivedTotal = if (isOde && !allowOdeAsHeal) this.sessionHealsReceivedTotal else this.sessionHealsReceivedTotal + event.amount,
    sessionHealFromPlayer = if (!RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionHealFromPlayer
    } else if (isOde && !allowOdeAsHeal) {
      this.sessionHealFromPlayer
    } else {
      this.sessionHealFromPlayer + (event.source to ((this.sessionHealFromPlayer[event.source] ?: 0L) + event.amount))
    }
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
    recentCastEvents = (this.recentCastEvents + event).takeLast(eventHistoryDepth)
  )
}

/**
 * Add a successful cast event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postSuccessfulCastEvent(event: SuccessfulCastEvent): PlayerCard {
  val isMarasNineTails = event.spell == "Charm (Rider Skill)" && (PlayerCacheInteractor.isRealPlayer(event.target) || RFConfig.state.value.allowPVEDamage)
  val isGardenDefiance = event.spellId == gardenDefianceCastId
  val isRevive = event.spellId in reviveSpellIds

  var card = this.copiedWithUtilityItemDetectionMiddleWare(event) // handles item spells
  card = card.copiedWithPotionDetectionMiddleWare(event) // increments potion usages

  return card.copy(
    lastEvent = event.timestamp,
    cache = card.cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalCharms = if (isMarasNineTails) (card.cache?.lifetimeTotalCharms ?: 0) + 1 else (card.cache?.lifetimeTotalCharms ?: 0),
      lifetimeTotalGardenDefiance = if (isGardenDefiance) (card.cache?.lifetimeTotalGardenDefiance ?: 0L) + 1 else (card.cache?.lifetimeTotalGardenDefiance ?: 0L),
      lifetimeTotalRevive = if (isRevive) (card.cache?.lifetimeTotalRevive ?: 0L) + 1 else (card.cache?.lifetimeTotalRevive ?: 0L)
    ),
    sessionCharmTotal = if (isMarasNineTails) card.sessionCharmTotal + 1 else card.sessionCharmTotal,
    sessionGardenDefianceTotal = if (isGardenDefiance) card.sessionGardenDefianceTotal + 1 else card.sessionGardenDefianceTotal,
    sessionReviveTotal = if (isRevive) card.sessionReviveTotal + 1 else card.sessionReviveTotal,
    recentCastSuccessfulCastEvents = (card.recentCastSuccessfulCastEvents + event).takeLast(eventHistoryDepth)
  )
}

/**
 * Add a buff gained event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postBuffGainedEvent(event: BuffGainedEvent): PlayerCard {
  val card = this.copiedWithGliderDetectionMiddleWare(event) // detects glider usage via self-applied buffs

  return card.copy(
    lastEvent = event.timestamp,
    cache = card.cache?.copy(
      lastSeen = event.timestamp,
    ),
    recentBuffGainedEvents = (this.recentBuffGainedEvents + event).takeLast(eventHistoryDepth)
  )
}

/**
 * Add a buff ended event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postBuffEndedEvent(event: BuffEndedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(lastSeen = event.timestamp),
    recentBuffEndedEvents = (this.recentBuffEndedEvents + event).takeLast(eventHistoryDepth)
  )
}

/**
 * Add a debuff gained event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDebuffGainedEvent(event: DebuffGainedEvent): PlayerCard {
  //if (event.source == event.target) return this // skip self-applied debuffs
  val isCC = findDebuffById(event.debuffId)?.consideredCC == true
    ?: findDebuffByName(event.debuff)?.consideredCC == true
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(lastSeen = event.timestamp),
    recentDebuffGainedEvents = (this.recentDebuffGainedEvents + event).takeLast(eventHistoryDepth),
    sessionCCFromPlayer = if (isCC && RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionCCFromPlayer + (event.source to ((this.sessionCCFromPlayer[event.source] ?: 0) + 1))
    } else {
      this.sessionCCFromPlayer
    }
  )
}

/**
 * Add a debuff ended event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDebuffEndedEvent(event: DebuffEndedEvent): PlayerCard {
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(lastSeen = event.timestamp),
    recentDebuffEndedEvents = (this.recentDebuffEndedEvents + event).takeLast(eventHistoryDepth),
  )
}

/**
 * Essentially we maintain the reverse relationship on each PlayerCard (but only for applied debuffs/buffs)
 * so that we can track who applied what debuff to whom, and how many times, etc. This costs more memory but
 * makes analysis way easier later on, and uses less CPU to compute. (See: Graph Databases vs Relational Databases heh)
 */
fun PlayerCard.postDebuffAppliedEvent(event: DebuffAppliedEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  //if (event.source == event.target) return this // skip self-casts (e.g. self-inflicted debuffs)
  val isCC = findDebuffById(event.debuffId)?.consideredCC == true
    ?: findDebuffByName(event.debuff)?.consideredCC == true
  val isCharm = event.debuffId in charmedDebuffIds
  val isDistress = event.debuffId in distressedDebuffIds
  val isSilence = event.debuffId in silencedDebuffIds
  val isGlider = event.debuffId in gliderUsageDebuffIds && System.currentTimeMillis() - this.lastGliderUse > 5000L // glider debuff applied, but only count if more than 5 second since last use to avoid double-counting from game bug
  val isSongs = event.debuffId == 853 || event.debuffId == 847 || event.debuffId == 31367 || event.debuffId == 772 // Unguarded, Lethargy, Weakened Energy, Unpleasant Sensation
  val isTigerStrike = event.debuffId in tigerStrikeDebuffIds
  val isFreeze = event.debuffId in freezeDebuffIds
  val isTrips = event.debuffId in trippedDebuffIds
  val isBubbles = event.debuffId in bubbleTrapDebuffIds
  val isShieldStrip = event.debuffId in shieldStripDebuffIds
  val isWeaponDisables = event.debuffId in weaponDisablesDebuffIds
  val isPotionDisables = event.debuffId in potionDisablesDebuffIds
  val isBdGlider = event.debuffId in bdGliderDebuffIds
  val isCrystalWings = event.debuffId in crystalWingsDebuffIds
  val isGliderDisables = event.debuffId in gliderDisablesDebuffIds
  val isProvoked = event.debuffId in provokedDebuffIds
  val isThrowDagger = event.debuffId in throwDaggerDebuffIds
  val isStuns = event.debuffId in stunDebuffIds
  val isStaggers = event.debuffId in staggerDebuffIds
  val isPetrification = event.debuffId in petrificationDebuffIds
  val isAbsorbLifeforce = event.debuffId in absorbLifeforceDebuffIds
  val isCorrosiveBarrage = event.debuffId in corrosiveBarrageDebuffIds
  val isBlindedByCrows = event.debuffId in blindedByCrowsDebuffIds
  val isMistSunder = event.debuffId in mistSunderDebuffIds
  val isDeependDebuff = event.debuffId in deedendDebuffIds
  val isImpale = event.debuffId in impaleDebuffIds
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)
  return card.copy(
    lastEvent = event.timestamp,
    cache = card.cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalDebuffsApplied = (card.cache?.lifetimeTotalDebuffsApplied ?: 0L) + 1,
      lifetimeTotalCCDelivered = if (isCC) (card.cache?.lifetimeTotalCCDelivered ?: 0L) + 1 else (card.cache?.lifetimeTotalCCDelivered ?: 0L),
      lifetimeTotalCharms = if (isCharm) (card.cache?.lifetimeTotalCharms ?: 0L) + 1 else (card.cache?.lifetimeTotalCharms ?: 0L),
      lifetimeTotalSongs = if (isSongs) (card.cache?.lifetimeTotalSongs ?: 0L) + 1 else (card.cache?.lifetimeTotalSongs ?: 0L),
      lifetimeTotalGliderUses = if (isGlider) (card.cache?.lifetimeTotalGliderUses ?: 0L) + 1 else (card.cache?.lifetimeTotalGliderUses ?: 0L),
      lifetimeTotalDistresses = if (isDistress) (card.cache?.lifetimeTotalDistresses ?: 0L) + 1 else (card.cache?.lifetimeTotalDistresses ?: 0L),
      lifetimeTotalSilences = if (isSilence) (card.cache?.lifetimeTotalSilences ?: 0L) + 1 else (card.cache?.lifetimeTotalSilences ?: 0L),
      lifetimeTotalTigerStrikes = if (isTigerStrike) (card.cache?.lifetimeTotalTigerStrikes ?: 0L) + 1 else (card.cache?.lifetimeTotalTigerStrikes ?: 0L),
      lifetimeTotalFreezes = if (isFreeze) (card.cache?.lifetimeTotalFreezes ?: 0L) + 1 else (card.cache?.lifetimeTotalFreezes ?: 0L),
      lifetimeTotalTrips = if (isTrips) (card.cache?.lifetimeTotalTrips ?: 0L) + 1 else (card.cache?.lifetimeTotalTrips ?: 0L),
      lifetimeTotalBubbles = if (isBubbles) (card.cache?.lifetimeTotalBubbles ?: 0L) + 1 else (card.cache?.lifetimeTotalBubbles ?: 0L),
      lifetimeTotalShieldStrip = if (isShieldStrip) (card.cache?.lifetimeTotalShieldStrip ?: 0L) + 1 else (card.cache?.lifetimeTotalShieldStrip ?: 0L),
      lifetimeTotalWeaponDisables = if (isWeaponDisables) (card.cache?.lifetimeTotalWeaponDisables ?: 0L) + 1 else (card.cache?.lifetimeTotalWeaponDisables ?: 0L),
      lifetimeTotalPotionDisables = if (isPotionDisables) (card.cache?.lifetimeTotalPotionDisables ?: 0L) + 1 else (card.cache?.lifetimeTotalPotionDisables ?: 0L),
      lifetimeTotalBdGlider = if (isBdGlider) (card.cache?.lifetimeTotalBdGlider ?: 0L) + 1 else (card.cache?.lifetimeTotalBdGlider ?: 0L),
      lifetimeTotalCrystalWings = if (isCrystalWings) (card.cache?.lifetimeTotalCrystalWings ?: 0L) + 1 else (card.cache?.lifetimeTotalCrystalWings ?: 0L),
      lifetimeTotalGliderDisables = if (isGliderDisables) (card.cache?.lifetimeTotalGliderDisables ?: 0L) + 1 else (card.cache?.lifetimeTotalGliderDisables ?: 0L),
      lifetimeTotalProvoked = if (isProvoked) (card.cache?.lifetimeTotalProvoked ?: 0L) + 1 else (card.cache?.lifetimeTotalProvoked ?: 0L),
      lifetimeTotalThrowDagger = if (isThrowDagger) (card.cache?.lifetimeTotalThrowDagger ?: 0L) + 1 else (card.cache?.lifetimeTotalThrowDagger ?: 0L),
      lifetimeTotalStuns = if (isStuns) (card.cache?.lifetimeTotalStuns ?: 0L) + 1 else (card.cache?.lifetimeTotalStuns ?: 0L),
      lifetimeTotalStaggers = if (isStaggers) (card.cache?.lifetimeTotalStaggers ?: 0L) + 1 else (card.cache?.lifetimeTotalStaggers ?: 0L),
      lifetimeTotalPetrification = if (isPetrification) (card.cache?.lifetimeTotalPetrification ?: 0L) + 1 else (card.cache?.lifetimeTotalPetrification ?: 0L),
      lifetimeTotalAbsorbLifeforce = if (isAbsorbLifeforce) (card.cache?.lifetimeTotalAbsorbLifeforce ?: 0L) + 1 else (card.cache?.lifetimeTotalAbsorbLifeforce ?: 0L),
      lifetimeTotalCorrosiveBarrage = if (isCorrosiveBarrage) (card.cache?.lifetimeTotalCorrosiveBarrage ?: 0L) + 1 else (card.cache?.lifetimeTotalCorrosiveBarrage ?: 0L),
      lifetimeTotalBlindedByCrows = if (isBlindedByCrows) (card.cache?.lifetimeTotalBlindedByCrows ?: 0L) + 1 else (card.cache?.lifetimeTotalBlindedByCrows ?: 0L),
      lifetimeTotalMistSunder = if (isMistSunder) (card.cache?.lifetimeTotalMistSunder ?: 0L) + 1 else (card.cache?.lifetimeTotalMistSunder ?: 0L),
      lifetimeTotalDeependDebuff = if (isDeependDebuff) (card.cache?.lifetimeTotalDeependDebuff ?: 0L) + 1 else (card.cache?.lifetimeTotalDeependDebuff ?: 0L),
      lifetimeTotalImpaleImmunity = if (isImpale) (card.cache?.lifetimeTotalImpaleImmunity ?: 0L) + 1 else (card.cache?.lifetimeTotalImpaleImmunity ?: 0L),
    ),
    recentDebuffAppliedEvents = (this.recentDebuffAppliedEvents + event).takeLast(eventHistoryDepth),
    sessionDebuffTotal = this.sessionDebuffTotal + 1,
    sessionCharmTotal = if (isCharm) sessionCharmTotal + 1 else sessionCharmTotal,
    sessionSongsTotal = if (isSongs) this.sessionSongsTotal + 1 else this.sessionSongsTotal,
    sessionDistressTotal = if (isDistress) sessionDistressTotal + 1 else sessionDistressTotal,
    sessionSilenceTotal = if (isSilence) sessionSilenceTotal + 1 else sessionSilenceTotal,
    sessionGliderTotal = if (isGlider) sessionGliderTotal + 1 else sessionGliderTotal,
    sessionTigerStrikeTotal = if (isTigerStrike) sessionTigerStrikeTotal + 1 else sessionTigerStrikeTotal,
    sessionFreezeTotal = if (isFreeze) sessionFreezeTotal + 1 else sessionFreezeTotal,
    sessionTripsTotal = if (isTrips) sessionTripsTotal + 1 else sessionTripsTotal,
    sessionBubblesTotal = if (isBubbles) sessionBubblesTotal + 1 else sessionBubblesTotal,
    sessionShieldStripTotal = if (isShieldStrip) sessionShieldStripTotal + 1 else sessionShieldStripTotal,
    sessionWeaponDisablesTotal = if (isWeaponDisables) sessionWeaponDisablesTotal + 1 else sessionWeaponDisablesTotal,
    sessionPotionDisablesTotal = if (isPotionDisables) sessionPotionDisablesTotal + 1 else sessionPotionDisablesTotal,
    sessionBdGliderTotal = if (isBdGlider) sessionBdGliderTotal + 1 else sessionBdGliderTotal,
    sessionCrystalWingsTotal = if (isCrystalWings) sessionCrystalWingsTotal + 1 else sessionCrystalWingsTotal,
    sessionGliderDisablesTotal = if (isGliderDisables) sessionGliderDisablesTotal + 1 else sessionGliderDisablesTotal,
    sessionProvokedTotal = if (isProvoked) sessionProvokedTotal + 1 else sessionProvokedTotal,
    sessionThrowDaggerTotal = if (isThrowDagger) sessionThrowDaggerTotal + 1 else sessionThrowDaggerTotal,
    sessionStunsTotal = if (isStuns) sessionStunsTotal + 1 else sessionStunsTotal,
    sessionStaggersTotal = if (isStaggers) sessionStaggersTotal + 1 else sessionStaggersTotal,
    sessionPetrificationTotal = if (isPetrification) sessionPetrificationTotal + 1 else sessionPetrificationTotal,
    sessionAbsorbLifeforceTotal = if (isAbsorbLifeforce) sessionAbsorbLifeforceTotal + 1 else sessionAbsorbLifeforceTotal,
    sessionCorrosiveBarrageTotal = if (isCorrosiveBarrage) sessionCorrosiveBarrageTotal + 1 else sessionCorrosiveBarrageTotal,
    sessionBlindedByCrowsTotal = if (isBlindedByCrows) sessionBlindedByCrowsTotal + 1 else sessionBlindedByCrowsTotal,
    sessionMistSunderTotal = if (isMistSunder) sessionMistSunderTotal + 1 else sessionMistSunderTotal,
    sessionDeependDebuffTotal = if (isDeependDebuff) sessionDeependDebuffTotal + 1 else sessionDeependDebuffTotal,
    sessionImpaleImmunityTotal = if (isImpale) sessionImpaleImmunityTotal + 1 else sessionImpaleImmunityTotal,
    sessionCCTotal = if (isCC) card.sessionCCTotal + 1 else card.sessionCCTotal,
    sessionSpellCCMap = if (isCC) {
      val debuffKey = event.debuff.ifBlank { "Unknown" }
      this.sessionSpellCCMap + (debuffKey to ((this.sessionSpellCCMap[debuffKey] ?: 0) + 1))
    } else {
      this.sessionSpellCCMap
    },
    sessionCCToPlayer = if (isCC && RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionCCToPlayer + (event.target to ((this.sessionCCToPlayer[event.target] ?: 0) + 1))
    } else {
      this.sessionCCToPlayer
    },
    sessionCCToPlayerBySpell = if (isCC && RFConfig.state.value.performanceBattleGraphEnabled) {
      val debuffKey = event.debuff.ifBlank { "Unknown" }
      val targetMap = this.sessionCCToPlayerBySpell[event.target] ?: emptyMap()
      (this.sessionCCToPlayerBySpell + (event.target to (targetMap + (debuffKey to ((targetMap[debuffKey] ?: 0) + 1)))))
        .cappedBySpell(battleGraphSpellDepth, MAX_SPELLS_PER_TARGET)
    } else {
      this.sessionCCToPlayerBySpell
    },

    // --- ALL debuffs adjacency (not just CC) ---
    sessionDebuffToPlayer = if (RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionDebuffToPlayer + (event.target to ((this.sessionDebuffToPlayer[event.target] ?: 0) + 1))
    } else this.sessionDebuffToPlayer,
    sessionDebuffToPlayerBySpell = if (RFConfig.state.value.performanceBattleGraphEnabled) {
      run {
        val debuffKey = event.debuff.ifBlank { "Unknown" }
        val targetMap = this.sessionDebuffToPlayerBySpell[event.target] ?: emptyMap()
        (this.sessionDebuffToPlayerBySpell + (event.target to (targetMap + (debuffKey to ((targetMap[debuffKey] ?: 0) + 1)))))
          .cappedBySpell(battleGraphSpellDepth, MAX_SPELLS_PER_TARGET)
      }
    } else this.sessionDebuffToPlayerBySpell,
    sessionSpellDebuffMap = run {
      val debuffKey = event.debuff.ifBlank { "Unknown" }
      // Filter out blacklisted debuffs from the dropdown map
      if (event.debuffId in blacklistedDebuffIds || debuffKey in blacklistedDebuffNames) {
        this.sessionSpellDebuffMap
      } else {
        this.sessionSpellDebuffMap + (debuffKey to ((this.sessionSpellDebuffMap[debuffKey] ?: 0) + 1))
      }
    },

    // --- Charm adjacency ---
    sessionCharmToPlayer = if (isCharm && RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionCharmToPlayer + (event.target to ((this.sessionCharmToPlayer[event.target] ?: 0) + 1))
    } else {
      this.sessionCharmToPlayer
    },
    sessionCharmToPlayerBySpell = if (isCharm && RFConfig.state.value.performanceBattleGraphEnabled) {
      val debuffKey = event.debuff.ifBlank { "Unknown" }
      val targetMap = this.sessionCharmToPlayerBySpell[event.target] ?: emptyMap()
      (this.sessionCharmToPlayerBySpell + (event.target to (targetMap + (debuffKey to ((targetMap[debuffKey] ?: 0) + 1)))))
        .cappedBySpell(battleGraphSpellDepth, MAX_SPELLS_PER_TARGET)
    } else {
      this.sessionCharmToPlayerBySpell
    },

    // --- Distress adjacency ---
    sessionDistressToPlayer = if (isDistress && RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionDistressToPlayer + (event.target to ((this.sessionDistressToPlayer[event.target] ?: 0) + 1))
    } else {
      this.sessionDistressToPlayer
    },
    sessionDistressToPlayerBySpell = if (isDistress && RFConfig.state.value.performanceBattleGraphEnabled) {
      val debuffKey = event.debuff.ifBlank { "Unknown" }
      val targetMap = this.sessionDistressToPlayerBySpell[event.target] ?: emptyMap()
      (this.sessionDistressToPlayerBySpell + (event.target to (targetMap + (debuffKey to ((targetMap[debuffKey] ?: 0) + 1)))))
        .cappedBySpell(battleGraphSpellDepth, MAX_SPELLS_PER_TARGET)
    } else {
      this.sessionDistressToPlayerBySpell
    },

    // --- Silence adjacency ---
    sessionSilenceToPlayer = if (isSilence && RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionSilenceToPlayer + (event.target to ((this.sessionSilenceToPlayer[event.target] ?: 0) + 1))
    } else {
      this.sessionSilenceToPlayer
    },
    sessionSilenceToPlayerBySpell = if (isSilence && RFConfig.state.value.performanceBattleGraphEnabled) {
      val debuffKey = event.debuff.ifBlank { "Unknown" }
      val targetMap = this.sessionSilenceToPlayerBySpell[event.target] ?: emptyMap()
      (this.sessionSilenceToPlayerBySpell + (event.target to (targetMap + (debuffKey to ((targetMap[debuffKey] ?: 0) + 1)))))
        .cappedBySpell(battleGraphSpellDepth, MAX_SPELLS_PER_TARGET)
    } else {
      this.sessionSilenceToPlayerBySpell
    },

    lastGliderUse = if (isGlider) event.timestamp else this.lastGliderUse, // update glider use timestamp if applicable
  )
}

/*
 * Used to measure buff applications from this player to others.
 */
fun PlayerCard.postBuffAppliedEvent(event: BuffAppliedEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  //if (event.source == event.target) return this // skip self-casts (e.g. resurgence on yourself)
  val isBracingImmunity = event.buffId in bracingBlastImmunityBuffIds
  val isDefiance = event.buffId in defianceBuffIds
  val isSacDance = event.buffId in sacrificeBuffIds
  val isPurge = event.buffId == purgeBuffId
  val isDeepTranquility = event.buffId == deepTranquilityBuffId
  val isRegularSunder = event.buffId in regularSunderBuffIds
  val isMistSunder = event.buffId in mistSunderDebuffIds
  val isProtectiveWings = event.buffId in protectiveWingsBuffIds
  val isCourageousAction = event.buffId in courageousActionBuffIds
  val isManaBarrier = event.buffId in manaBarrierBuffIds
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)
  return card.copy(
    lastEvent = event.timestamp,
    cache = card.cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalBuffsApplied = (card.cache?.lifetimeTotalBuffsApplied ?: 0L) + 1,
      lifetimeTotalCCDelivered = if (isBracingImmunity) (card.cache?.lifetimeTotalCCDelivered ?: 0L) + 1 else (card.cache?.lifetimeTotalCCDelivered ?: 0L),
      lifetimeTotalBracings = if (isBracingImmunity) (card.cache?.lifetimeTotalBracings ?: 0L) + 1 else (card.cache?.lifetimeTotalBracings ?: 0L),
      lifetimeTotalDefiance = if (isDefiance) (card.cache?.lifetimeTotalDefiance ?: 0L) + 1 else (card.cache?.lifetimeTotalDefiance ?: 0L),
      lifetimeTotalSacDances = if (isSacDance) (card.cache?.lifetimeTotalSacDances ?: 0L) + 1 else (card.cache?.lifetimeTotalSacDances ?: 0L),
      lifetimeTotalPurges = if (isPurge) (card.cache?.lifetimeTotalPurges ?: 0L) + 1 else (card.cache?.lifetimeTotalPurges ?: 0L),
      lifetimeTotalDeepTranquility = if (isDeepTranquility) (card.cache?.lifetimeTotalDeepTranquility ?: 0L) + 1 else (card.cache?.lifetimeTotalDeepTranquility ?: 0L),
      lifetimeTotalRegularSunder = if (isRegularSunder) (card.cache?.lifetimeTotalRegularSunder ?: 0L) + 1 else (card.cache?.lifetimeTotalRegularSunder ?: 0L),
      lifetimeTotalMistSunder = if (isMistSunder) (card.cache?.lifetimeTotalMistSunder ?: 0L) + 1 else (card.cache?.lifetimeTotalMistSunder ?: 0L),
      lifetimeTotalProtectiveWings = if (isProtectiveWings) (card.cache?.lifetimeTotalProtectiveWings ?: 0L) + 1 else (card.cache?.lifetimeTotalProtectiveWings ?: 0L),
      lifetimeTotalCourageousAction = if (isCourageousAction) (card.cache?.lifetimeTotalCourageousAction ?: 0L) + 1 else (card.cache?.lifetimeTotalCourageousAction ?: 0L),
      lifetimeTotalManaBarrier = if (isManaBarrier) (card.cache?.lifetimeTotalManaBarrier ?: 0L) + 1 else (card.cache?.lifetimeTotalManaBarrier ?: 0L),
    ),
    recentBuffAppliedEvents = (this.recentBuffAppliedEvents + event).takeLast(eventHistoryDepth),
    sessionBuffTotal = this.sessionBuffTotal + 1,
    sessionDefianceTotal = if (isDefiance) card.sessionDefianceTotal + 1 else card.sessionDefianceTotal,
    sessionSacDanceTotal = if (isSacDance) card.sessionSacDanceTotal + 1 else card.sessionSacDanceTotal,
    sessionPurgeTotal = if (isPurge) sessionPurgeTotal + 1 else sessionPurgeTotal,
    sessionDeepTranquilityTotal = if (isDeepTranquility) sessionDeepTranquilityTotal + 1 else sessionDeepTranquilityTotal,
    sessionRegularSunderTotal = if (isRegularSunder) sessionRegularSunderTotal + 1 else sessionRegularSunderTotal,
    sessionMistSunderTotal = if (isMistSunder) sessionMistSunderTotal + 1 else sessionMistSunderTotal,
    sessionProtectiveWingsTotal = if (isProtectiveWings) sessionProtectiveWingsTotal + 1 else sessionProtectiveWingsTotal,
    sessionCourageousActionTotal = if (isCourageousAction) sessionCourageousActionTotal + 1 else sessionCourageousActionTotal,
    sessionManaBarrierTotal = if (isManaBarrier) sessionManaBarrierTotal + 1 else sessionManaBarrierTotal,
    sessionBracingsTotal = if (isBracingImmunity) sessionBracingsTotal + 1 else sessionBracingsTotal,
    sessionCCTotal = if (isBracingImmunity) card.sessionCCTotal + 1 else card.sessionCCTotal,
    // --- Buff adjacency ---
    sessionBuffToPlayer = if (RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionBuffToPlayer + (event.target to ((this.sessionBuffToPlayer[event.target] ?: 0) + 1))
    } else this.sessionBuffToPlayer,
    sessionBuffToPlayerBySpell = if (RFConfig.state.value.performanceBattleGraphEnabled) {
      val buffKey = event.buff.ifBlank { "Unknown" }
      val targetMap = this.sessionBuffToPlayerBySpell[event.target] ?: emptyMap()
      (this.sessionBuffToPlayerBySpell + (event.target to (targetMap + (buffKey to ((targetMap[buffKey] ?: 0) + 1)))))
        .cappedBySpell(battleGraphSpellDepth, MAX_SPELLS_PER_TARGET)
    } else this.sessionBuffToPlayerBySpell,
    sessionSpellBuffMap = run {
      val buffKey = event.buff.ifBlank { "Unknown" }
      // Filter out blacklisted buffs from the dropdown map
      if (buffKey in blacklistedBuffNames) {
        this.sessionSpellBuffMap
      } else {
        this.sessionSpellBuffMap + (buffKey to ((this.sessionSpellBuffMap[buffKey] ?: 0) + 1))
      }
    }
  )
}

/**
 * Record that this player killed someone.
 */
fun PlayerCard.postKillEvent(
  timestamp: Long,
  victimName: String,
  preDeathSpells: Map<String, Long> = emptyMap()
): PlayerCard {
  return this.copy(
    lastEvent = timestamp,
    cache = cache?.copy(
      lastSeen = timestamp,
      lifetimeTotalKills = cache.lifetimeTotalKills + 1
    ),
    // Add to recent kills map (capped to eventHistoryDepth)
    recentKills = (this.recentKills + (timestamp to victimName))
      .entries.sortedByDescending { it.key }.take(eventHistoryDepth)
      .associate { it.key to it.value },
    // Increment kill score
    sessionKillTotal = this.sessionKillTotal + 1,
    // --- Kill adjacency ---
    sessionKillsToPlayer = if (RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionKillsToPlayer + (victimName to ((this.sessionKillsToPlayer[victimName] ?: 0) + 1))
    } else this.sessionKillsToPlayer,
    sessionKillsToPlayerBySpell = if (RFConfig.state.value.performanceBattleGraphEnabled) {
      run {
        val targetMap = this.sessionKillsToPlayerBySpell[victimName] ?: emptyMap()
        val merged = targetMap.toMutableMap()
        preDeathSpells.forEach { (spell, damage) ->
          merged[spell] = (merged[spell] ?: 0L) + damage
        }
        (this.sessionKillsToPlayerBySpell + (victimName to merged))
          .cappedBySpell(battleGraphSpellDepth, MAX_SPELLS_PER_TARGET)
      }
    } else this.sessionKillsToPlayerBySpell
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
    recentKillsKB = (this.recentKillsKB + (timestamp to victimName))
      .entries.sortedByDescending { it.key }.take(eventHistoryDepth)
      .associate { it.key to it.value },
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
    (this.recentKilledBys + (timestamp to killerMostDamage))
      .entries.sortedByDescending { it.key }.take(eventHistoryDepth)
      .associate { it.key to it.value }
  } else {
    this.recentKilledBys
  }

  val updatedKilledBysKB = if (killerKillingBlow != null) {
    (this.recentKilledByKB + (timestamp to killerKillingBlow))
      .entries.sortedByDescending { it.key }.take(eventHistoryDepth)
      .associate { it.key to it.value }
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

/**
 * Returns true if this player card meets the minimum PvP participation thresholds.
 * Default thresholds: 25k damage OR 25k heals OR 25 points of CC.
 */
fun PlayerCard.hasPvPParticipation(): Boolean {
  return this.sessionDamageTotal >= 25_000L || this.sessionHealTotal >= 25_000L || this.sessionCCTotal >= 25
}

/**
 * Compute a single "PvP performance score" summarizing how active a player was this session.
 * Updated algorithm:
 *  - Dmg/Heals: 1 point per 100k (e.g., 5.5M dmg = 55 points)
 *  - CC: 0.1 point per CC point (e.g., 2000 CC = 200 points)
 *  - Songs: 0.04 points per song (e.g., 2000 songs = 80 points)
 *  - Charms: 1 point per charm (e.g., 100 charms = 100 points)
 */
fun PlayerCard.pvpPerformancePoints(): Int {
  val damageAndHeals = (sessionDamageTotal + sessionHealTotal) / 100_000L
  val ccPoints = sessionCCTotal * 0.1
  val songsPoints = sessionSongsTotal * 0.04
  val charmsPoints = sessionCharmTotal * 1.0
  return (damageAndHeals + ccPoints + songsPoints + charmsPoints).toInt()
}

/**
 * Sets the faction and faction status on a PlayerCard, updating both the card fields and the cache entity.
 * If the cache exists, it copies the new values. If not, it creates a new cache object with the faction data.
 */
fun PlayerCard.setFaction(faction: Faction, factionStatus: FactionStatus): PlayerCard {
  val updatedCache = cache
    ?.copy(
      lastKnownFaction = faction.value,
      lastKnownFactionStatus = factionStatus.value
    )
    ?: PlayerCacheEntity(
      playerName = this.name,
      lastKnownFaction = faction.value,
      lastKnownFactionStatus = factionStatus.value
    )

  return this.copy(
    lastKnownFaction = faction.value,
    lastKnownFactionStatus = factionStatus.value,
    cache = updatedCache
  )
}

// --- Life Mend Tracking (buff 25875) ---

const val LIFE_MEND_BUFF_ID = 25875
private const val LIFE_MEND_MAX_SAMPLES = 25

/**
 * Records a Life Mend cast by this healer on a target.
 * Called when SPELL_AURA_APPLIED fires with buffId 25875.
 */
fun PlayerCard.postLifeMendApplied(target: String): PlayerCard {
  return copy(lifeMendTotal = lifeMendTotal + 1)
}

/**
 * Updates Life Mend stats with a new heal amount from the buff tooltip.
 * Called when FRAMES_UPDATE delivers buff data with healAmount > 0 for buff 25875.
 * Maintains a running average of the last [LIFE_MEND_MAX_SAMPLES] samples.
 */
fun PlayerCard.updateLifeMendStats(healAmount: Int): PlayerCard {
  if (healAmount <= 0) return this
  val newAmounts = (lifeMendHealAmounts + healAmount).takeLast(LIFE_MEND_MAX_SAMPLES)
  val avg = newAmounts.average().toInt()
  val quality = LifeMendQuality.fromAverage(avg)
  return copy(
    lifeMendHealAmounts = newAmounts,
    lifeMendAverage = avg,
    lifeMendQuality = quality
  )
}

/**
 * Records a fresh loot-buff scan result. [buffIds] is the set of buff IDs currently on the
 * player, and [buffCount] is the total number of active buffs (from X2Unit:UnitBuffCount).
 *
 * The combined loot percentage is the sum of every loot buff the player has. The current
 * amount always overwrites the previous one, but only a genuinely new peak is recorded so a
 * failed (empty) scan never clears the player's best loot buff showing.
 */
fun PlayerCard.updateLootBuffStats(buffIds: Set<Int>, buffCount: Int): PlayerCard {
  val amount = lootBuffAmountForIds(buffIds)
  return copy(
    sessionCurrentLootBuffAmount = amount,
    sessionPeakLootBuffAmount = maxOf(sessionPeakLootBuffAmount, amount),
    sessionCurrentBuffCount = buffCount
  )
}

/**
 * Accumulates time-in-range for the coherence rankings. [renderMs], [raidMs] and [clumpMs]
 * are the elapsed session milliseconds the player spent within the 120m / 60m / 30m thresholds.
 * Called from the roster-distance tracker in PlayerCacheInteractor.
 */
fun PlayerCard.accumulateCoherence(renderMs: Long, raidMs: Long, clumpMs: Long): PlayerCard {
  if (renderMs <= 0L && raidMs <= 0L && clumpMs <= 0L) return this
  return copy(
    sessionCoherenceRenderMs = sessionCoherenceRenderMs + renderMs,
    sessionCoherenceRaidMs = sessionCoherenceRaidMs + raidMs,
    sessionCoherenceClumpMs = sessionCoherenceClumpMs + clumpMs
  )
}

