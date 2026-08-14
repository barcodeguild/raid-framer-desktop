package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.definitions.AreaEffectSpellConfig
import com.reoky.raidframer.core.definitions.areaEffectBuffIds
import com.reoky.raidframer.core.definitions.areaEffectSpellConfigs
import com.reoky.raidframer.core.model.BuffGainedEvent
import com.reoky.raidframer.core.model.CombatEvent
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.DebuffGainedEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Attributes area-effect spells (like Sunder Earth) where the buff/debuff source is an NPC
 * area object rather than the player who cast the spell. Correlates SPELL_DAMAGE events
 * (which have the real player as source) with SPELL_AURA_APPLIED events (which have the
 * area NPC as source) to determine the actual caster.
 *
 * Flow:
 * 1. All SPELL_DAMAGE events are buffered (for CID + timing correlation).
 * 2. When a sunder aura arrives (buff ID in areaEffectBuffIds), we search buffered damages
 *    for a match by CID and temporal proximity.
 * 3. On match, we post a corrected event to PlayerCacheInteractor with the real caster.
 * 4. We also learn each player's sunder type for future disambiguation.
 */
object AreaEffectAttributorInteractor : Interactor() {

  private const val TAG = "AreaEffectAttributor"
  private val mutex = Mutex()

  private const val DAMAGE_BUFFER_RETENTION_MS = 10_000L
  private const val AURA_TIMEOUT_MS = 10_000L
  private const val TYPE_LEARNING_RETENTION_MS = 3_600_000L // 1 hour

  // --- Internal data classes ---

  private data class DamageRecord(
    val source: String,
    val target: String,
    val targetCID: String,
    val timestamp: Long,
    val spell: String,
  )

  private data class Attribution(
    val caster: String,
    val confidence: Confidence,
  )

  private enum class Confidence {
    DIRECT_CID,
    CASTER_IN_AREA,
    FALLBACK,
  }

  private data class PendingAura(
    val event: CombatEvent,
    val buffId: Int,
    val targetCID: String,
    val targetName: String,
    val timestamp: Long,
    val receivedAt: Long = System.currentTimeMillis(),
  )

  private data class ActiveArea(
    val caster: String,
    val config: AreaEffectSpellConfig,
    val startTime: Long,
    val endTime: Long,
  )

  private data class SunderTypeRecord(
    val buffId: Int,
    val learnedAt: Long,
  )

  // --- Buffers ---

  private val recentDamages = mutableListOf<DamageRecord>()
  private val pendingAuras = mutableListOf<PendingAura>()
  private val activeAreas = mutableListOf<ActiveArea>()
  private val knownTypes = mutableMapOf<String, SunderTypeRecord>()

  // --- Public API ---

  /** Called for ALL damage events. Only buffers damage that matches a known area-effect spell name. */
  fun onDamageEvent(event: DamageEvent) {
    val matchedConfig = areaEffectSpellConfigs.find { event.spell in it.damageSpellNames } ?: return
    scope.launch {
      mutex.withLock {
        recentDamages.add(
          DamageRecord(
            source = event.source,
            target = event.target,
            targetCID = event.cid,
            timestamp = event.timestamp,
            spell = event.spell,
          )
        )
        val cutoff = System.currentTimeMillis() - DAMAGE_BUFFER_RETENTION_MS
        recentDamages.removeAll { it.timestamp < cutoff }
      }
    }
  }

  /**
   * Check if an aura event is from an area-effect spell and should be intercepted.
   * Returns true if the buff ID is a known area-effect buff.
   */
  fun isAreaEffectAura(event: CombatEvent): Boolean {
    return when (event) {
      is BuffGainedEvent -> event.buffId in areaEffectBuffIds
      is DebuffGainedEvent -> event.debuffId in areaEffectBuffIds
      else -> false
    }
  }

  /** Called for sunder aura events. Intercepts before PlayerCacheInteractor. */
  fun onAuraEvent(event: CombatEvent) {
    val (buffId, targetCID, targetName, timestamp) = extractAuraInfo(event) ?: return

    scope.launch {
      mutex.withLock {
        pendingAuras.add(PendingAura(event, buffId, targetCID, targetName, timestamp))
      }
    }
  }

  // --- Processing ---

  override suspend fun interact() {
    tryAttributePendingAuras()
    cleanupExpiredEntries()
  }

  private suspend fun tryAttributePendingAuras() {
    mutex.withLock {
      val now = System.currentTimeMillis()
      val stillPending = mutableListOf<PendingAura>()

      for (aura in pendingAuras) {
        val attribution = findCasterForAura(aura)
        if (attribution != null) {
          postAttributedEvent(aura, attribution.caster)
          // Only direct CID correlation can safely teach us which Sunder variant
          // a caster owns; fallback/area guesses may belong to another player's area.
          if (attribution.confidence == Confidence.DIRECT_CID) {
            learnSunderType(attribution.caster, aura.buffId, aura.timestamp)
          }
          val config = areaEffectSpellConfigs.find { aura.buffId in it.auraBuffIds }
          if (config != null) {
            val hasArea = activeAreas.any { area ->
              area.caster.equals(attribution.caster, ignoreCase = true) &&
                area.config.auraBuffIds == config.auraBuffIds &&
                aura.timestamp in (area.startTime - config.correlationWindowMs)..area.endTime
            }
            if (!hasArea) {
              activeAreas.add(
                ActiveArea(
                  caster = attribution.caster,
                  config = config,
                  startTime = aura.timestamp,
                  endTime = aura.timestamp + config.areaLifetimeMs,
                )
              )
            }
          }
        } else if (now - aura.receivedAt < AURA_TIMEOUT_MS) {
          stillPending.add(aura)
        } else {
          // Timed out — only use a fallback when the caster's learned type agrees.
          val singleCaster = findSingleCasterInWindow(aura.timestamp)
          if (singleCaster != null && knownTypes[singleCaster]?.buffId == aura.buffId) {
            postAttributedEvent(aura, singleCaster)
          } else {
            Log.debug(TAG, "Giving up on aura ${aura.buffId} on ${aura.targetName} (CID:${aura.targetCID}) — ambiguous attribution")
          }
        }
      }
      pendingAuras.clear()
      pendingAuras.addAll(stillPending)
    }
  }

  // --- Attribution logic ---

  private fun findCasterForAura(aura: PendingAura): Attribution? {
    val config = areaEffectSpellConfigs.find { aura.buffId in it.auraBuffIds } ?: return null
    val windowStart = aura.timestamp - config.correlationWindowMs
    val windowEnd = aura.timestamp + config.correlationWindowMs

    // Strategy 1: Direct CID match — only sunder damage events are in recentDamages (filtered by spell name),
    // so the caster is the one whose damage target CID matches the aura target CID.
    val cidMatches = recentDamages.filter { dmg ->
      dmg.timestamp in windowStart..windowEnd && dmg.targetCID == aura.targetCID
    }
    if (cidMatches.size == 1) {
      Log.debug(TAG, "CID match: ${cidMatches.single().source} for aura ${aura.buffId} on ${aura.targetName}")
      return Attribution(cidMatches.single().source, Confidence.DIRECT_CID)
    }
    if (cidMatches.size > 1) {
      val disambiguated = disambiguateByKnownType(cidMatches.map { it.source }, aura.buffId)
      if (disambiguated != null) {
        Log.debug(TAG, "CID match + known type disambiguation: $disambiguated for aura ${aura.buffId} on ${aura.targetName}")
        return Attribution(disambiguated, Confidence.DIRECT_CID)
      }
    }

    // Strategy 2: Caster-in-area match (Regular Sunder only: aura target is the caster themselves).
    // Skipped for Mist Sunder (debuff on enemies) where the aura target is the enemy, not the caster.
    // Otherwise during duels the enemy's own damage could be mis-matched as the caster.
    if (config.appliesToAllies) {
      val casterInArea = recentDamages.filter { dmg ->
        dmg.timestamp in windowStart..windowEnd
      }.filter { dmg ->
        val sourceCard = PlayerCacheInteractor.getCard(dmg.source)
        sourceCard != null && aura.targetCID in sourceCard.recentCids
      }
      if (casterInArea.size == 1) {
        Log.debug(TAG, "Caster-in-area match: ${casterInArea.single().source} for aura ${aura.buffId} on ${aura.targetName}")
        return Attribution(casterInArea.single().source, Confidence.CASTER_IN_AREA)
      }
    }

    // Strategy 3: Active area match (for ongoing 7s attribution when players walk in/out)
    val activeMatch = activeAreas.filter { area ->
      aura.timestamp in area.startTime..area.endTime &&
        aura.buffId in area.config.auraBuffIds &&
        knownTypes[area.caster]?.buffId == aura.buffId
    }
    if (activeMatch.size == 1) {
      Log.debug(TAG, "Active area match: ${activeMatch.single().caster} for aura ${aura.buffId} on ${aura.targetName}")
      return Attribution(activeMatch.single().caster, Confidence.FALLBACK)
    }

    // Strategy 4: Single-caster fallback (only one unique source in window)
    val fallback = findSingleCasterInWindow(aura.timestamp)
    if (fallback != null && knownTypes[fallback]?.buffId == aura.buffId) {
      return Attribution(fallback, Confidence.FALLBACK)
    }
    return null
  }

  private fun findSingleCasterInWindow(timestamp: Long): String? {
    // Use a slightly wider window for the fallback since auras can arrive out of order
    val windowStart = timestamp - 3000L
    val windowEnd = timestamp + 3000L
    val sources = recentDamages
      .filter { it.timestamp in windowStart..windowEnd }
      .map { it.source }
      .distinct()
    if (sources.size == 1) {
      Log.debug(TAG, "Single-caster fallback: ${sources.single()} for aura at $timestamp")
      return sources.single()
    }
    return null
  }

  private fun disambiguateByKnownType(candidates: List<String>, buffId: Int): String? {
    val matchingType = candidates.filter { knownTypes[it]?.buffId == buffId }
    return when (matchingType.size) {
      1 -> matchingType.single()
      else -> null
    }
  }

  // --- Event posting ---

  private fun postAttributedEvent(aura: PendingAura, caster: String) {
    when (aura.event) {
      is BuffGainedEvent -> {
        val corrected = aura.event.copy(source = caster)
        PlayerCacheInteractor.postEvent(corrected)
        Log.info(TAG, "Attributed buff ${aura.buffId} (${aura.event.buff}) on ${aura.targetName} (CID:${aura.targetCID}) to $caster")
      }
      is DebuffGainedEvent -> {
        val corrected = aura.event.copy(source = caster)
        PlayerCacheInteractor.postEvent(corrected)
        Log.info(TAG, "Attributed debuff ${aura.buffId} (${aura.event.debuff}) on ${aura.targetName} (CID:${aura.targetCID}) to $caster")
      }
    }
  }

  // --- Learning ---

  private fun learnSunderType(caster: String, buffId: Int, timestamp: Long) {
    val existing = knownTypes[caster]
    if (existing == null || timestamp > existing.learnedAt) {
      knownTypes[caster] = SunderTypeRecord(buffId, timestamp)
      Log.debug(TAG, "Learned sunder type for $caster: buffId=$buffId (${if (buffId == 2596) "Regular" else if (buffId == 24562) "Mist" else "Unknown"})")
    }
  }

  // --- Cleanup ---

  private suspend fun cleanupExpiredEntries() {
    mutex.withLock {
      val now = System.currentTimeMillis()
      recentDamages.removeAll { now - it.timestamp > DAMAGE_BUFFER_RETENTION_MS }
      activeAreas.removeAll { now > it.endTime }
      knownTypes.entries.removeIf { now - it.value.learnedAt > TYPE_LEARNING_RETENTION_MS }
    }
  }

  // --- Helpers ---

  private fun extractAuraInfo(event: CombatEvent): AuraInfo? {
    return when (event) {
      is BuffGainedEvent -> AuraInfo(event.buffId, event.cid, event.target, event.timestamp)
      is DebuffGainedEvent -> AuraInfo(event.debuffId, event.cid, event.target, event.timestamp)
      else -> null
    }
  }

  private data class AuraInfo(
    val buffId: Int,
    val targetCID: String,
    val targetName: String,
    val timestamp: Long,
  )
}
