package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.model.PlayerCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/**
 * Validates death events by cross-referencing them with damage events from other players.
 * Uses a batch processing approach to efficiently scan for killers across multiple deaths simultaneously.
 * Reasons we have to do this:
 * 1. What if the events arrived slightly out of order due to log lag?
 * 2. What if multiple players damaged the victim within the time window?
 * 3. What if, like, a lot of friends died in that time window, and we'd have to scan all the buckets that many times to find each killer?
 * Basically, this allows us to batch up deaths in a big RvR battle and process them all at once, minimizing redundant scans and reducing the
 * time complexity from O(players * deaths) to O(players)/O(players + deaths), which for a large number of deaths is orders of magnitude faster.
 */
object DeathAccumulatorInteractor : Interactor() {

  private const val TAG = "DeathAccumulator"

  private const val LOOK_AHEAD_MS = 10000L
  private const val LOOK_BEHIND_MS = 15000L

  private val localMutex = Mutex()

  data class PendingDeath(val playerName: String, val timestamp: Long)

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

    // Get current mode from config
    val mode = KillCounterMode.fromString(RFConfig.state.value.killCounterMode)

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

    Log.info(TAG, "Processing batch of ${deathsToProcess.size} players deaths (mode: $mode)")

    val candidatesSnapshot = PlayerCacheInteractor.getRealPlayersSnapshot()
    val victimLookup = deathsToProcess.groupBy { it.playerName.normalizePlayerName() }

    val matches = when (mode) {
      KillCounterMode.KILLING_BLOW -> findKillingBlows(candidatesSnapshot, victimLookup, deathsToProcess)
      KillCounterMode.MOST_DAMAGE -> findMostDamage(candidatesSnapshot, victimLookup, deathsToProcess)
    }

    val results = deathsToProcess.map { death ->
      val killer = matches[death]
      if (killer != null) {
        Log.info(TAG, "Attributed kill of ${death.playerName} to ${killer.name} ($mode)")
      } else {
        Log.debug(TAG, "Death of ${death.playerName} was unattributed.")
      }
      Triple(death.playerName, death.timestamp, killer?.name)
    }

    PlayerCacheInteractor.processDeathBatch(results)
  }

  private fun findKillingBlows(
    candidates: List<PlayerCard>,
    victimLookup: Map<String, List<PendingDeath>>,
    deaths: List<PendingDeath>
  ): Map<PendingDeath, PlayerCard> {
    val matches = mutableMapOf<PendingDeath, Pair<PlayerCard, Long>>()

    candidates.forEach { potentialKiller ->
      potentialKiller.recentDamageEvents.forEach { event ->
        val normalizedTarget = event.target.normalizePlayerName()

        victimLookup[normalizedTarget]?.forEach { death ->
          val minTime = death.timestamp - LOOK_BEHIND_MS
          val maxTime = death.timestamp + LOOK_AHEAD_MS

          if (event.timestamp in minTime..maxTime) {
            val delta = abs(event.timestamp - death.timestamp)
            val currentBest = matches[death]

            if (currentBest == null || delta < currentBest.second) {
              matches[death] = potentialKiller to delta
              Log.debug(TAG, "${potentialKiller.name} hit ${death.playerName} (delta: ${delta}ms)")
            }
          }
        }
      }
    }

    return matches.mapValues { it.value.first }
  }

  private fun findMostDamage(
    candidates: List<PlayerCard>,
    victimLookup: Map<String, List<PendingDeath>>,
    deaths: List<PendingDeath>
  ): Map<PendingDeath, PlayerCard> {
    // Map: Death -> Map of (Killer -> TotalDamage)
    val damageAccumulator = mutableMapOf<PendingDeath, MutableMap<PlayerCard, Long>>()

    candidates.forEach { potentialKiller ->
      potentialKiller.recentDamageEvents.forEach { event ->
        val normalizedTarget = event.target.normalizePlayerName()

        victimLookup[normalizedTarget]?.forEach { death ->
          val minTime = death.timestamp - LOOK_BEHIND_MS
          val maxTime = death.timestamp + LOOK_AHEAD_MS

          if (event.timestamp in minTime..maxTime) {
            val damageMap = damageAccumulator.getOrPut(death) { mutableMapOf() }
            damageMap[potentialKiller] = damageMap.getOrDefault(potentialKiller, 0L) + event.damage
          }
        }
      }
    }

    // Find player with most damage for each death
    return damageAccumulator.mapValues { (death, damageMap) ->
      damageMap.maxByOrNull { it.value }?.key.also { killer ->
        if (killer != null) {
          Log.debug(TAG, "${killer.name} dealt ${damageMap[killer]} total damage to ${death.playerName}")
        }
      }
    }.filterValues { it != null }.mapValues { it.value!! }
  }

  private fun String.normalizePlayerName(): String {
    return this.trim().lowercase()
  }
}

// new modes for kill counting
enum class KillCounterMode {
  KILLING_BLOW,
  MOST_DAMAGE;

  companion object {
    fun fromString(value: String): KillCounterMode {
      return entries.find { it.name == value } ?: KILLING_BLOW
    }
  }
}