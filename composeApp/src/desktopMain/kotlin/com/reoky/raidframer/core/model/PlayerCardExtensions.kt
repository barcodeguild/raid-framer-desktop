package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.*
import com.reoky.raidframer.core.definitions.battleHymnBuffIds
import com.reoky.raidframer.core.definitions.charmedDebuffIds
import com.reoky.raidframer.core.definitions.copiedWithUtilityItemDetectionMiddleWare
import com.reoky.raidframer.core.definitions.criticalDiscordAoEDebuffIds
import com.reoky.raidframer.core.definitions.criticalDiscordNonAoEDebuffIds
import com.reoky.raidframer.core.definitions.dissonanceNonAoEDebuffIds
import com.reoky.raidframer.core.definitions.dissonanceQuakeDebuffIds
import com.reoky.raidframer.core.definitions.distressedDebuffIds
import com.reoky.raidframer.core.definitions.shackleDebuffIds
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
import com.reoky.raidframer.core.definitions.SPELL_CAST_ID_lookup
import com.reoky.raidframer.core.definitions.ItemSpell
import com.reoky.raidframer.core.definitions.PotionDefinition
import com.reoky.raidframer.core.definitions.GliderSpell
import com.reoky.raidframer.core.definitions.isPlayerBehaviorSpell
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
 * Increments the realPlayerBehaviorMetric when a player performs an action only real players can do:
 * - Casting a spell from any of the 13 player skill trees (via possibleCastIDs)
 * - Using a utility item (via ItemSpell matching)
 * - Using a potion (via PotionDefinition matching)
 * - Using a glider (via GliderSpell matching on BuffGainedEvent)
 * - Casting a known player-only spell (via isPlayerBehaviorSpell matching)
 *
 * This metric is session-only and used to filter pets that share player names.
 */
fun PlayerCard.postBehaviorMetricIncrement(event: CombatEvent): PlayerCard {
  var increment = 0L
  when (event) {
    is SuccessfulCastEvent -> {
      if (event.spellId in SPELL_CAST_ID_lookup) increment++
      if (isPlayerBehaviorSpell(event.spellId)) increment++
      val matchedItem = ItemSpell.entries.any { item ->
        event.spellId in item.itemSpecificSkillIds ||
            item.possibleSpellNames.any { it.equals(event.spell, ignoreCase = true) }
      }
      if (matchedItem) increment++
      val matchedPotion = PotionDefinition.entries.any { it.skillId == event.spellId }
      if (matchedPotion) increment++
    }

    is BuffGainedEvent -> {
      val matchedGlider = GliderSpell.findByBuffId(event.buffId) != null
      if (matchedGlider) increment++
    }
  }
  if (increment == 0L) return this
  return this.copy(realPlayerBehaviorMetric = this.realPlayerBehaviorMetric + increment)
}

/**
 * Add a damage event to the PlayerCard, updating recent events and session totals.
 */
fun PlayerCard.postDamageEvent(event: DamageEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  //if (event.source == event.target) return this // skip self-damage
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)

  // Hit quality stats: crit count, max hit (damage), total hit count (damage)
  val newCritCount = if (event.critical) this.sessionCritDamageCount + 1 else this.sessionCritDamageCount
  val newMaxHitDamage = maxOf(this.sessionMaxHitDamage, event.damage)
  val newMaxHitDamageSpell = if (event.damage > this.sessionMaxHitDamage) event.spell.ifBlank { "Unknown" } else this.sessionMaxHitDamageSpell
  val newDamageHitCount = this.sessionDamageHitCount + 1

  return card.copy(
    lastEvent = event.timestamp,
    cache = card.cache?.let { cache ->
      cache.copy(
        identity = cache.identity.copy(lastSeen = event.timestamp),
        lifetimeCombat = cache.lifetimeCombat.copy(
          lifetimeTotalDamage = cache.lifetimeTotalDamage + event.damage
        ),
        lifetimeHitQuality = cache.lifetimeHitQuality.copy(
          lifetimeTotalCritDamageCount = cache.lifetimeHitQuality.lifetimeTotalCritDamageCount + if (event.critical) 1 else 0,
          lifetimeTotalDamageEventCount = cache.lifetimeHitQuality.lifetimeTotalDamageEventCount + 1,
          lifetimeMaxHitDamage = if (event.damage > cache.lifetimeHitQuality.lifetimeMaxHitDamage) event.damage else cache.lifetimeHitQuality.lifetimeMaxHitDamage,
          lifetimeMaxHitDamageSpell = if (event.damage > cache.lifetimeHitQuality.lifetimeMaxHitDamage) event.spell.ifBlank { "Unknown" } else cache.lifetimeHitQuality.lifetimeMaxHitDamageSpell,
          lifetimeDamageHitCount = cache.lifetimeHitQuality.lifetimeDamageHitCount + 1,
          lifetimeDamageTotalForAvg = cache.lifetimeHitQuality.lifetimeDamageTotalForAvg + event.damage
        )
      )
    },
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
        (this.sessionDamageToPlayerBySpell + (event.target to (targetMap + (spellKey to ((targetMap[spellKey]
          ?: 0L) + event.damage)))))
          .cappedBySpell(battleGraphSpellDepth, MAX_SPELLS_PER_TARGET)
      }
    } else this.sessionDamageToPlayerBySpell,
    // Hit quality stats (damage only)
    sessionCritDamageCount = newCritCount,
    sessionDamageEventCount = this.sessionDamageEventCount + 1,
    sessionMaxHitDamage = newMaxHitDamage,
    sessionMaxHitDamageSpell = newMaxHitDamageSpell,
    sessionDamageHitCount = newDamageHitCount
  )
}

/**
 * Add a heal event to the PlayerCard, updating recent events and session totals.
 * Healing hit quality stats exclude self-casts (potions, self-heals) — only counts
 * spells cast on targets other than oneself.
 */
fun PlayerCard.postHealEvent(event: HealEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  //if (event.source == event.target) return this // skip self-heals
  val isOde = isOdeToRecovery(event.spell)
  val allowOdeAsHeal = RFConfig.state.value.allowOdeToRecoveryCountAsHeals

  // Filter self-casts for healing hit quality stats — only count heals on other players
  val isSelfCast = event.source == event.target
  val countHealQuality = !isSelfCast

  // Hit quality stats: crit count, max hit (heal), total hit count (heal) — excludes self-casts
  val newCritCount = if (countHealQuality && event.critical) this.sessionCritHealCount + 1 else this.sessionCritHealCount
  val newMaxHitHeal = if (countHealQuality) maxOf(this.sessionMaxHitHeal, event.amount) else this.sessionMaxHitHeal
  val newMaxHitHealSpell = if (countHealQuality && event.amount > this.sessionMaxHitHeal) event.spell.ifBlank { "Unknown" } else this.sessionMaxHitHealSpell
  val newHealHitCount = if (countHealQuality) this.sessionHealHitCount + 1 else this.sessionHealHitCount

  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.let { c ->
      c.copy(
        identity = c.identity.copy(lastSeen = event.timestamp),
        lifetimeCombat = c.lifetimeCombat.copy(lifetimeTotalHealing = c.lifetimeTotalHealing + event.amount),
        lifetimeAvoidance = c.lifetimeAvoidance.copy(
          lifetimeTotalAbsorbs = if (CombatEventsHelper.isAbsorbShieldSkill(event.spell)) {
            c.lifetimeTotalAbsorbs + event.amount.coerceAtLeast(0)
          } else c.lifetimeTotalAbsorbs,
          lifetimeTotalOdeHeals = if (isOde) c.lifetimeTotalOdeHeals + event.amount else c.lifetimeTotalOdeHeals
        ),
        lifetimeHitQuality = c.lifetimeHitQuality.copy(
          lifetimeTotalCritHealCount = if (countHealQuality) c.lifetimeHitQuality.lifetimeTotalCritHealCount + if (event.critical) 1 else 0 else c.lifetimeHitQuality.lifetimeTotalCritHealCount,
          lifetimeTotalHealEventCount = if (countHealQuality) c.lifetimeHitQuality.lifetimeTotalHealEventCount + 1 else c.lifetimeHitQuality.lifetimeTotalHealEventCount,
          lifetimeMaxHitHeal = if (countHealQuality && event.amount > c.lifetimeHitQuality.lifetimeMaxHitHeal) event.amount else c.lifetimeHitQuality.lifetimeMaxHitHeal,
          lifetimeMaxHitHealSpell = if (countHealQuality && event.amount > c.lifetimeHitQuality.lifetimeMaxHitHeal) event.spell.ifBlank { "Unknown" } else c.lifetimeHitQuality.lifetimeMaxHitHealSpell,
          lifetimeHealHitCount = if (countHealQuality) c.lifetimeHitQuality.lifetimeHealHitCount + 1 else c.lifetimeHitQuality.lifetimeHealHitCount,
          lifetimeHealTotalForAvg = if (countHealQuality) c.lifetimeHitQuality.lifetimeHealTotalForAvg + event.amount else c.lifetimeHitQuality.lifetimeHealTotalForAvg
        )
      )
    },
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
        (this.sessionHealToPlayerBySpell + (event.target to (targetMap + (spellKey to ((targetMap[spellKey]
          ?: 0L) + event.amount)))))
          .cappedBySpell(battleGraphSpellDepth, MAX_SPELLS_PER_TARGET)
      }
    },
    // absorbs chart (source-side): SPELL_HEALED with absorb-shield skill name
    sessionAbsorbTotal = if (CombatEventsHelper.isAbsorbShieldSkill(event.spell)) {
      this.sessionAbsorbTotal + event.amount.coerceAtLeast(0)
    } else this.sessionAbsorbTotal,
    // Hit quality stats (healing only, excludes self-casts)
    sessionCritHealCount = newCritCount,
    sessionHealEventCount = if (countHealQuality) this.sessionHealEventCount + 1 else this.sessionHealEventCount,
    sessionMaxHitHeal = newMaxHitHeal,
    sessionMaxHitHealSpell = newMaxHitHealSpell,
    sessionHealHitCount = newHealHitCount
  )
}

/**
 * Avoidance counting. Every kind credits the avoider (target card): BLOCK,
 * PARRY, DODGE, IMMUNE and MISS all mean the defender avoided the hit.
 * BLOCK/PARRY also back-calculate the HP that was prevented (see
 * [CombatEventsHelper.mitigatedHp]) and credit it toward sessionAbsorbTotal,
 * the same "mitigation prevented damage" bucket the absorbs ranking shows.
 * DODGE/IMMUNE/MISS report 0 damage taken, so no HP is recoverable for them.
 * postAvoidanceCaused is kept for callers that still route source-side; it is
 * a no-op record so historical behavior stays visible in recent events.
 */
fun PlayerCard.postAvoidanceTaken(event: AvoidanceEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(name) && !RFConfig.state.value.allowPVEDamage) return this
  val recent = (recentAvoidanceEvents + event).takeLast(eventHistoryDepth)
  val mitigated = CombatEventsHelper.mitigatedHp(event.missResult, event.damageTaken)
  return when (event.missResult) {
    AvoidanceKind.BLOCK -> copy(
      lastEvent = event.timestamp,
      recentAvoidanceEvents = recent,
      sessionBlockTotal = sessionBlockTotal + 1,
      sessionBlockMitigated = sessionBlockMitigated + mitigated,
      sessionAbsorbTotal = sessionAbsorbTotal + mitigated,
      cache = cache?.copy(
        lifetimeAvoidance = cache.lifetimeAvoidance.copy(
          lifetimeTotalBlocks = cache.lifetimeTotalBlocks + 1,
          lifetimeTotalBlockMitigated = cache.lifetimeTotalBlockMitigated + mitigated,
          lifetimeTotalAbsorbs = cache.lifetimeTotalAbsorbs + mitigated
        )
      ),
    )
    AvoidanceKind.PARRY -> copy(
      lastEvent = event.timestamp,
      recentAvoidanceEvents = recent,
      sessionParryTotal = sessionParryTotal + 1,
      sessionParryMitigated = sessionParryMitigated + mitigated,
      sessionAbsorbTotal = sessionAbsorbTotal + mitigated,
      cache = cache?.copy(
        lifetimeAvoidance = cache.lifetimeAvoidance.copy(
          lifetimeTotalParries = cache.lifetimeTotalParries + 1,
          lifetimeTotalParryMitigated = cache.lifetimeTotalParryMitigated + mitigated,
          lifetimeTotalAbsorbs = cache.lifetimeTotalAbsorbs + mitigated
        )
      ),
    )
    AvoidanceKind.DODGE -> copy(
      lastEvent = event.timestamp,
      recentAvoidanceEvents = recent,
      sessionEvadeTotal = sessionEvadeTotal + 1,
      cache = cache?.copy(lifetimeAvoidance = cache.lifetimeAvoidance.copy(lifetimeTotalEvades = cache.lifetimeTotalEvades + 1))
    )
    AvoidanceKind.IMMUNE -> copy(
      lastEvent = event.timestamp,
      recentAvoidanceEvents = recent,
      sessionImmuneTotal = sessionImmuneTotal + 1,
      cache = cache?.copy(lifetimeAvoidance = cache.lifetimeAvoidance.copy(lifetimeTotalImmunes = cache.lifetimeTotalImmunes + 1))
    )
    AvoidanceKind.MISS -> copy(
      lastEvent = event.timestamp,
      recentAvoidanceEvents = recent,
      sessionMissTotal = sessionMissTotal + 1,
      cache = cache?.copy(lifetimeAvoidance = cache.lifetimeAvoidance.copy(lifetimeTotalMisses = cache.lifetimeTotalMisses + 1))
    )
    else -> copy(lastEvent = event.timestamp, recentAvoidanceEvents = recent)
  }
}

fun PlayerCard.postAvoidanceCaused(event: AvoidanceEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(name) && !RFConfig.state.value.allowPVEDamage) return this
  // Source-side is only a touch so the card exists for rendering; all
  // avoidance points belong to the avoider (target card).
  val recent = (recentAvoidanceEvents + event).takeLast(eventHistoryDepth)
  return copy(lastEvent = event.timestamp, recentAvoidanceEvents = recent)
}

/*
 * When a damage event is posted a damage and a damage taken event are posted to the source and target player card's respectively. This way we can handle
 * manipulations to the target card separately from crediting the source. (This is the player card of the target)
 */
fun PlayerCard.postDamageTakenEvent(event: DamageEvent): PlayerCard {
  if (!PlayerCacheInteractor.isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return this
  val absorb = event.absorbedAmount.coerceAtLeast(0).toLong()
  return this.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      identity = cache.identity.copy(lastSeen = event.timestamp),
      lifetimeCombat = cache.lifetimeCombat.copy(lifetimeTotalDamageTaken = cache.lifetimeTotalDamageTaken + event.damage),
      lifetimeAvoidance = cache.lifetimeAvoidance.copy(lifetimeTotalAbsorbs = cache.lifetimeTotalAbsorbs + absorb)
    ),
    sessionDamageTakenTotal = this.sessionDamageTakenTotal + event.damage,
    sessionAbsorbTotal = this.sessionAbsorbTotal + absorb, // damage_absorbed credited to target
    sessionDamageFromPlayer = if (RFConfig.state.value.performanceBattleGraphEnabled) {
      this.sessionDamageFromPlayer + (event.source to ((this.sessionDamageFromPlayer[event.source]
        ?: 0L) + event.damage))
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
      identity = cache.identity.copy(lastSeen = event.timestamp),
      lifetimeCombat = cache.lifetimeCombat.copy(lifetimeTotalHealsReceived = cache.lifetimeTotalHealsReceived + event.amount)
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
      identity = cache.identity.copy(lastSeen = event.timestamp)
    ),
    recentCastEvents = (this.recentCastEvents + event).takeLast(eventHistoryDepth)
  )
}

/**
 * Add a successful cast event to the PlayerCard, updating recent events.
 */
fun PlayerCard.postSuccessfulCastEvent(event: SuccessfulCastEvent): PlayerCard {
  val isMarasNineTails =
    event.spell == "Charm (Rider Skill)" && (PlayerCacheInteractor.isRealPlayer(event.target) || RFConfig.state.value.allowPVEDamage)
  val isGardenDefiance = event.spellId == gardenDefianceCastId
  val isRevive = event.spellId in reviveSpellIds

  var card = this.copiedWithUtilityItemDetectionMiddleWare(event) // handles item spells
  card = card.copiedWithPotionDetectionMiddleWare(event) // increments potion usages
  card = card.postBehaviorMetricIncrement(event) // track player-only behavior

  return card.copy(
    lastEvent = event.timestamp,
    cache = card.cache?.copy(
      identity = card.cache.identity.copy(lastSeen = event.timestamp),
      lifetimeCombat = card.cache.lifetimeCombat.copy(
        lifetimeTotalCharms = if (isMarasNineTails) card.cache.lifetimeTotalCharms + 1 else card.cache.lifetimeTotalCharms
      ),
      lifetimeDebuffs = card.cache.lifetimeDebuffs.copy(
        lifetimeTotalGardenDefiance = if (isGardenDefiance) card.cache.lifetimeTotalGardenDefiance + 1 else card.cache.lifetimeTotalGardenDefiance,
        lifetimeTotalRevive = if (isRevive) card.cache.lifetimeTotalRevive + 1 else card.cache.lifetimeTotalRevive
      )
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
  var card = this.copiedWithGliderDetectionMiddleWare(event) // detects glider usage via self-applied buffs
  card = card.postBehaviorMetricIncrement(event) // track glider usage as player behavior

  return card.copy(
    lastEvent = event.timestamp,
    cache = card.cache?.copy(
      identity = card.cache.identity.copy(lastSeen = event.timestamp),
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
    cache = cache?.withIdentity { copy(lastSeen = event.timestamp) },
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
    cache = cache?.withIdentity { copy(lastSeen = event.timestamp) },
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
    cache = cache?.withIdentity { copy(lastSeen = event.timestamp) },
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
  val isGlider =
    event.debuffId in gliderUsageDebuffIds && System.currentTimeMillis() - this.lastGliderUse > 5000L // glider debuff applied, but only count if more than 5 second since last use to avoid double-counting from game bug
  val isSongs =
    event.debuffId == 853 || event.debuffId == 847 || event.debuffId == 31367 || event.debuffId == 772 // Unguarded, Lethargy, Weakened Energy, Unpleasant Sensation
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
  val isDissonanceQuake = event.debuffId in dissonanceQuakeDebuffIds
  val isCriticalDiscordAoE = event.debuffId in criticalDiscordAoEDebuffIds
  val isShackle = event.debuffId in shackleDebuffIds
  val isDissonanceNonAoE = event.debuffId in dissonanceNonAoEDebuffIds
  val isCriticalDiscordNonAoE = event.debuffId in criticalDiscordNonAoEDebuffIds
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)
  return card.copy(
    lastEvent = event.timestamp,
    cache = card.cache?.copy(
      identity = card.cache.identity.copy(lastSeen = event.timestamp),
      lifetimeCombat = card.cache.lifetimeCombat.copy(
        lifetimeTotalDebuffsApplied = card.cache.lifetimeTotalDebuffsApplied + 1,
        lifetimeTotalCCDelivered = card.cache.lifetimeTotalCCDelivered + if (isCC) 1 else 0,
        lifetimeTotalCharms = card.cache.lifetimeTotalCharms + if (isCharm) 1 else 0,
        lifetimeTotalSongs = card.cache.lifetimeTotalSongs + if (isSongs) 1 else 0,
        lifetimeTotalGliderUses = card.cache.lifetimeTotalGliderUses + if (isGlider) 1 else 0,
        lifetimeTotalDistresses = card.cache.lifetimeTotalDistresses + if (isDistress) 1 else 0,
        lifetimeTotalSilences = card.cache.lifetimeTotalSilences + if (isSilence) 1 else 0,
        lifetimeTotalTigerStrikes = card.cache.lifetimeTotalTigerStrikes + if (isTigerStrike) 1 else 0,
        lifetimeTotalFreezes = card.cache.lifetimeTotalFreezes + if (isFreeze) 1 else 0,
        lifetimeTotalTrips = card.cache.lifetimeTotalTrips + if (isTrips) 1 else 0,
        lifetimeTotalBubbles = card.cache.lifetimeTotalBubbles + if (isBubbles) 1 else 0,
        lifetimeTotalShieldStrip = card.cache.lifetimeTotalShieldStrip + if (isShieldStrip) 1 else 0,
        lifetimeTotalWeaponDisables = card.cache.lifetimeTotalWeaponDisables + if (isWeaponDisables) 1 else 0,
        lifetimeTotalPotionDisables = card.cache.lifetimeTotalPotionDisables + if (isPotionDisables) 1 else 0,
        lifetimeTotalBdGlider = card.cache.lifetimeTotalBdGlider + if (isBdGlider) 1 else 0,
        lifetimeTotalCrystalWings = card.cache.lifetimeTotalCrystalWings + if (isCrystalWings) 1 else 0,
        lifetimeTotalGliderDisables = card.cache.lifetimeTotalGliderDisables + if (isGliderDisables) 1 else 0,
        lifetimeTotalProvoked = card.cache.lifetimeTotalProvoked + if (isProvoked) 1 else 0,
        // ODE stats — Songcraft triplet tracking (10/18/26)
        lifetimeTotalDissonanceQuake = card.cache.lifetimeTotalDissonanceQuake + if (isDissonanceQuake) 1 else 0,
        lifetimeTotalCriticalDiscordAoE = card.cache.lifetimeTotalCriticalDiscordAoE + if (isCriticalDiscordAoE) 1 else 0,
        lifetimeTotalShackle = card.cache.lifetimeTotalShackle + if (isShackle) 1 else 0,
        lifetimeTotalDissonanceNonAoE = card.cache.lifetimeTotalDissonanceNonAoE + if (isDissonanceNonAoE) 1 else 0,
        lifetimeTotalCriticalDiscordNonAoE = card.cache.lifetimeTotalCriticalDiscordNonAoE + if (isCriticalDiscordNonAoE) 1 else 0
      ),
      lifetimeDebuffs = card.cache.lifetimeDebuffs.copy(
        lifetimeTotalThrowDagger = card.cache.lifetimeTotalThrowDagger + if (isThrowDagger) 1 else 0,
        lifetimeTotalStuns = card.cache.lifetimeTotalStuns + if (isStuns) 1 else 0,
        lifetimeTotalStaggers = card.cache.lifetimeTotalStaggers + if (isStaggers) 1 else 0,
        lifetimeTotalPetrification = card.cache.lifetimeTotalPetrification + if (isPetrification) 1 else 0,
        lifetimeTotalAbsorbLifeforce = card.cache.lifetimeTotalAbsorbLifeforce + if (isAbsorbLifeforce) 1 else 0,
        lifetimeTotalCorrosiveBarrage = card.cache.lifetimeTotalCorrosiveBarrage + if (isCorrosiveBarrage) 1 else 0,
        lifetimeTotalBlindedByCrows = card.cache.lifetimeTotalBlindedByCrows + if (isBlindedByCrows) 1 else 0,
        lifetimeTotalMistSunder = card.cache.lifetimeTotalMistSunder + if (isMistSunder) 1 else 0,
        lifetimeTotalDeependDebuff = card.cache.lifetimeTotalDeependDebuff + if (isDeependDebuff) 1 else 0,
        lifetimeTotalImpaleImmunity = card.cache.lifetimeTotalImpaleImmunity + if (isImpale) 1 else 0
      )
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
    // ODE stats — Songcraft triplet tracking (10/18/26)
    sessionDissonanceQuakeTotal = if (isDissonanceQuake) sessionDissonanceQuakeTotal + 1 else sessionDissonanceQuakeTotal,
    sessionCriticalDiscordAoETotal = if (isCriticalDiscordAoE) sessionCriticalDiscordAoETotal + 1 else sessionCriticalDiscordAoETotal,
    sessionShackleTotal = if (isShackle) sessionShackleTotal + 1 else sessionShackleTotal,
    sessionDissonanceNonAoETotal = if (isDissonanceNonAoE) sessionDissonanceNonAoETotal + 1 else sessionDissonanceNonAoETotal,
    sessionCriticalDiscordNonAoETotal = if (isCriticalDiscordNonAoE) sessionCriticalDiscordNonAoETotal + 1 else sessionCriticalDiscordNonAoETotal,
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
        (this.sessionDebuffToPlayerBySpell + (event.target to (targetMap + (debuffKey to ((targetMap[debuffKey]
          ?: 0) + 1)))))
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
      (this.sessionCharmToPlayerBySpell + (event.target to (targetMap + (debuffKey to ((targetMap[debuffKey]
        ?: 0) + 1)))))
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
      (this.sessionDistressToPlayerBySpell + (event.target to (targetMap + (debuffKey to ((targetMap[debuffKey]
        ?: 0) + 1)))))
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
      (this.sessionSilenceToPlayerBySpell + (event.target to (targetMap + (debuffKey to ((targetMap[debuffKey]
        ?: 0) + 1)))))
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
  // ODE stats — Battle Hymn (buff, not debuff) (10/18/26)
  val isBattleHymn = event.buffId in battleHymnBuffIds
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)
  return card.copy(
    lastEvent = event.timestamp,
    cache = card.cache?.copy(
      identity = card.cache.identity.copy(lastSeen = event.timestamp),
      lifetimeCombat = card.cache.lifetimeCombat.copy(
        lifetimeTotalBuffsApplied = card.cache.lifetimeTotalBuffsApplied + 1,
        lifetimeTotalCCDelivered = card.cache.lifetimeTotalCCDelivered + if (isBracingImmunity) 1 else 0,
        lifetimeTotalBracings = card.cache.lifetimeTotalBracings + if (isBracingImmunity) 1 else 0,
        // ODE stats — Battle Hymn (buff) (10/18/26)
        lifetimeTotalBattleHymn = card.cache.lifetimeTotalBattleHymn + if (isBattleHymn) 1 else 0
      ),
      lifetimeDebuffs = card.cache.lifetimeDebuffs.copy(
        lifetimeTotalDefiance = card.cache.lifetimeTotalDefiance + if (isDefiance) 1 else 0,
        lifetimeTotalSacDances = card.cache.lifetimeTotalSacDances + if (isSacDance) 1 else 0,
        lifetimeTotalPurges = card.cache.lifetimeTotalPurges + if (isPurge) 1 else 0,
        lifetimeTotalDeepTranquility = card.cache.lifetimeTotalDeepTranquility + if (isDeepTranquility) 1 else 0,
        lifetimeTotalRegularSunder = card.cache.lifetimeTotalRegularSunder + if (isRegularSunder) 1 else 0,
        lifetimeTotalMistSunder = card.cache.lifetimeTotalMistSunder + if (isMistSunder) 1 else 0,
        lifetimeTotalProtectiveWings = card.cache.lifetimeTotalProtectiveWings + if (isProtectiveWings) 1 else 0,
        lifetimeTotalCourageousAction = card.cache.lifetimeTotalCourageousAction + if (isCourageousAction) 1 else 0,
        lifetimeTotalManaBarrier = card.cache.lifetimeTotalManaBarrier + if (isManaBarrier) 1 else 0
      )
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
    // ODE stats — Battle Hymn (buff) (10/18/26)
    sessionBattleHymnTotal = if (isBattleHymn) sessionBattleHymnTotal + 1 else sessionBattleHymnTotal,
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
      identity = cache.identity.copy(lastSeen = timestamp),
      lifetimeCombat = cache.lifetimeCombat.copy(lifetimeTotalKills = cache.lifetimeTotalKills + 1)
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
      identity = cache.identity.copy(lastSeen = timestamp),
      lifetimeCombat = cache.lifetimeCombat.copy(lifetimeTotalKillsKB = cache.lifetimeTotalKillsKB + 1)
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
      identity = cache.identity.copy(lastSeen = timestamp),
      lifetimeCombat = cache.lifetimeCombat.copy(lifetimeTotalDeaths = cache.lifetimeTotalDeaths + 1)
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
      identity = cache.identity.copy(
        leaderships = if (newLeadership in 1..5) newLeadership else 0,
        lastSeen = timestamp
      )
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
 * Updated algorithm (biases towards damage less):
 *  - Kills: 1 point per kill (e.g., 50 kills = 50 points)
 *  - Damage: 1 point per 200k (e.g., 10M dmg = 50 points)
 *  - Heals: 1 point per 100k (e.g., 5M heals = 50 points)
 *  - CC: 0.2 point per CC point (e.g., 250 CC = 50 points)
 *  - Songs: 0.06 points per song (e.g., 833 songs = 50 points)
 *  - Charms: 0.33334 points per charm (e.g., 150 charms = 50 points)
 */
fun PlayerCard.pvpPerformancePoints(): Int {
  val killPoints = sessionKillTotal * 1.0
  val damagePoints = sessionDamageTotal / 200_000.0
  val healPoints = sessionHealTotal / 100_000.0
  val ccPoints = sessionCCTotal * 0.2
  val songsPoints = sessionSongsTotal * 0.06
  val charmsPoints = sessionCharmTotal * 0.33334
  return (killPoints + damagePoints + healPoints + ccPoints + songsPoints + charmsPoints).toInt()
}

/**
 * Sets the faction and faction status on a PlayerCard, updating both the card fields and the cache entity.
 * If the cache exists, it copies the new values. If not, it creates a new cache object preserving
 * all identity data (spec, guild, gear score, etc.) from the card to avoid data loss.
 */
fun PlayerCard.setFaction(faction: Faction, factionStatus: FactionStatus): PlayerCard {
  val updatedCache = if (cache != null) {
    cache.copy(
      identity = cache.identity.copy(
        lastKnownFaction = faction.value,
        lastKnownFactionStatus = factionStatus.value
      )
    )
  } else {
    // No existing cache — create one that preserves all identity data from the card
    PlayerCacheEntity(
      playerName = this.name,
      identity = PlayerCacheIdentity(
        lastSeen = this.lastEvent,
        lastKnownSpec = this.currentBuild,
        lastKnownLevel = 0,
        lastKnownGearScore = this.lastKnownGearScore,
        leaderships = this.leaderships,
        lastKnownFaction = faction.value,
        lastKnownFactionStatus = factionStatus.value,
        lastKnownGuild = this.lastKnownGuild,
        lastKnownRegion = ""
      ),
      gliders = PlayerCacheGliders(),
      utilityItems = PlayerCacheUtilityItems(),
      snakeItems = PlayerCacheSnakeItems(),
      blackDragonItems = PlayerCacheBlackDragonItems(),
      dungeonItems = PlayerCacheDungeonItems(),
      potions = PlayerCachePotions(),
      lifetimeCombat = PlayerCacheLifetimeCombat(),
      lifetimeDebuffs = PlayerCacheLifetimeDebuffs(),
      lifetimeAvoidance = PlayerCacheLifetimeAvoidance()
    )
  }

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

