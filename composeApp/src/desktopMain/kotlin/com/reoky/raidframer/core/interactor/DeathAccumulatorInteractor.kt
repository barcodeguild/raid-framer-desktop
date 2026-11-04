package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.model.PlayerCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/**
 * Validates death events by cross-referencing them with damage events from other players.
 * Computes `killing blow` and `most damage` attributions in parallel for complete statistics.
 */
object DeathAccumulatorInteractor : Interactor() {

  private const val TAG = "DeathAccumulator"

  private const val LOOK_AHEAD_MS = 10000L
  private const val LOOK_BEHIND_MS = 15000L

  private val localMutex = Mutex()

  data class PendingDeath(val playerName: String, val timestamp: Long)

  data class DeathAttribution(
    val victimName: String,
    val timestamp: Long,
    val killerMostDamage: String?,
    val killerKillingBlow: String?,
    val killerMostDamageSpells: Map<String, Long> = emptyMap(), // spell -> damage in pre-death window
    val killerMostDamageAmount: Long = 0L, // total damage dealt by the most-damage killer
    val killerKillingBlowDamage: Long = 0L // damage of the killing blow hit
  )

  private val pendingDeaths = mutableListOf<PendingDeath>()

  fun queueDeath(playerName: String, timestamp: Long) {
    scope.launch {
      localMutex.withLock {
        pendingDeaths.add(PendingDeath(playerName, timestamp))
      }
    }
  }

  override suspend fun interact() {
    val now = System.currentTimeMillis()
    val safeLookAhead = LOOK_AHEAD_MS + 2000L

    val deathsToProcess = mutableListOf<PendingDeath>()
    localMutex.withLock {
      val iterator = pendingDeaths.iterator()
      while (iterator.hasNext()) {
        val death = iterator.next()
        if (now - death.timestamp > safeLookAhead) {
          deathsToProcess.add(death)
          iterator.remove()
        }
      }
    }

    if (deathsToProcess.isEmpty()) return

    Log.info(TAG, "Processing batch of ${deathsToProcess.size} player deaths (computing both methods)")

    val candidatesSnapshot = PlayerCacheInteractor.getRealPlayersSnapshot()
    val victimLookup = deathsToProcess.groupBy { it.playerName.normalizePlayerName() }

    // Compute both attributions in parallel
    val killingBlowMatches = findKillingBlows(candidatesSnapshot, victimLookup, deathsToProcess)
    val mostDamageMatches = findMostDamage(candidatesSnapshot, victimLookup, deathsToProcess)

    val results = deathsToProcess.map { death ->
      val killerKB = killingBlowMatches[death]?.first?.name
      val killerKBDamage = killingBlowMatches[death]?.second ?: 0L
      val killerMDResult = mostDamageMatches[death]
      val killerMD = killerMDResult?.first?.name
      val killerMDSpells = killerMDResult?.second ?: emptyMap()
      val killerMDAmount = killerMDResult?.third ?: 0L

      if (killerKB != null || killerMD != null) {
        Log.info(TAG, "Death of ${death.playerName}: KB=${killerKB ?: "none"}, MD=${killerMD ?: "none"}")
      } else {
        Log.debug(TAG, "Death of ${death.playerName} was unattributed by both methods.")
      }

      DeathAttribution(
        victimName = death.playerName,
        timestamp = death.timestamp,
        killerMostDamage = killerMD,
        killerKillingBlow = killerKB,
        killerMostDamageSpells = killerMDSpells,
        killerMostDamageAmount = killerMDAmount,
        killerKillingBlowDamage = killerKBDamage
      )
    }

    PlayerCacheInteractor.processDeathBatch(results)
  }

  private fun findKillingBlows(
    candidates: List<PlayerCard>,
    victimLookup: Map<String, List<PendingDeath>>,
    deaths: List<PendingDeath>
  ): Map<PendingDeath, Pair<PlayerCard, Long>> {
    val matches = mutableMapOf<PendingDeath, Triple<PlayerCard, Long, Int>>()

    candidates.forEach { potentialKiller ->
      potentialKiller.recentDamageEvents.forEach { event ->
        val normalizedTarget = event.target.normalizePlayerName()
        victimLookup[normalizedTarget]?.forEach { death ->
          val timeDiff = abs(event.timestamp - death.timestamp)
          if (timeDiff <= LOOK_AHEAD_MS && event.timestamp <= death.timestamp) {
            val existingMatch = matches[death]
            if (existingMatch == null || timeDiff < existingMatch.second) {
              matches[death] = Triple(potentialKiller, timeDiff, event.damage)
            }
          }
        }
      }
    }

    return matches.mapValues { Pair(it.value.first, it.value.third.toLong()) }
  }

  private fun findMostDamage(
    candidates: List<PlayerCard>,
    victimLookup: Map<String, List<PendingDeath>>,
    deaths: List<PendingDeath>
  ): Map<PendingDeath, Triple<PlayerCard, Map<String, Long>, Long>> {
    val damageAccumulator = mutableMapOf<PendingDeath, MutableMap<PlayerCard, Long>>()
    val spellAccumulator = mutableMapOf<PendingDeath, MutableMap<String, Long>>()

    candidates.forEach { potentialKiller ->
      potentialKiller.recentDamageEvents.forEach { event ->
        val normalizedTarget = event.target.normalizePlayerName()
        victimLookup[normalizedTarget]?.forEach { death ->
          if (event.timestamp <= death.timestamp &&
            event.timestamp >= death.timestamp - LOOK_BEHIND_MS) {
            damageAccumulator
              .getOrPut(death) { mutableMapOf() }
              .merge(potentialKiller, event.damage.toLong()) { old, new -> old + new }
            // Collect spell breakdown for the winning killer
            val spellKey = event.spell.ifBlank { "Unknown" }
            spellAccumulator
              .getOrPut(death) { mutableMapOf() }
              .merge(spellKey, event.damage.toLong()) { old, new -> old + new }
          }
        }
      }
    }

    return damageAccumulator.mapValues { (death, damageMap) ->
      val killer = damageMap.maxByOrNull { it.value }?.key
      val totalDamage = killer?.let { damageMap[it] } ?: 0L
      val spells = if (killer != null) spellAccumulator[death] ?: emptyMap() else emptyMap()
      Log.debug(TAG, "Most damage to ${death.playerName}: ${killer?.name} with $totalDamage total")
      killer?.let { Triple(it, spells, totalDamage) }
    }.filterValues { it != null }.mapValues { it.value!! }
  }

  private fun String.normalizePlayerName(): String {
    return this.trim().lowercase()
  }
}

enum class KillCounterMode {
  KILLING_BLOW,
  MOST_DAMAGE;

  companion object {
    fun fromString(value: String): KillCounterMode {
      return entries.find { it.name == value } ?: KILLING_BLOW
    }
  }
}
