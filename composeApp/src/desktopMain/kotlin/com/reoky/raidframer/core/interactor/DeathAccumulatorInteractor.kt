package com.reoky.raidframer.core.interactor

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

  // how far to look ahead and behind the death timestamp for matching damage events (remember we assume ooo logs are possible, even though unlikely)
  private const val LOOK_AHEAD_MS = 10000L  // search ahead to see if events are still coming in after the death even is posted
  private const val LOOK_BEHIND_MS = 15000L // Search this far back from death timestamp (so if someone falls and dies, we can still potentially credit the person who damaged them before they fell)

  // Thread-safety for the local queue
  private val localMutex = Mutex()

  data class PendingDeath(val playerName: String, val timestamp: Long)

  // called from outside the interactor to queue up deaths for batch processing
  private val pendingDeaths = mutableListOf<PendingDeath>()
  fun queueDeath(playerName: String, timestamp: Long) {
    scope.launch {
      localMutex.withLock {
        pendingDeaths.add(PendingDeath(playerName, timestamp))
      }
    }
  }

  // every n seconds in a coroutine, process the queued deaths
  override suspend fun interact() {
    val now = System.currentTimeMillis()
    val safeLookAhead = LOOK_AHEAD_MS + 2000L // Extra 2s buffer for log lag

    // 1. Collect deaths ready to process
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

    Log.info(TAG, "Processing batch of ${deathsToProcess.size} players deaths at once to save resources.")

    // 2. Search Phase: Find killers using normalized name lookup
    val candidatesSnapshot = PlayerCacheInteractor.getRealPlayersSnapshot()

    // Build normalized victim lookup map for O(1) lookups
    val victimLookup = deathsToProcess.groupBy { it.playerName.normalizePlayerName() }

    // Map: Death Event -> Pair(KillerCard, TimeDelta)
    val matches = mutableMapOf<PendingDeath, Pair<PlayerCard, Long>>()

    Log.debug(TAG, "Scanning ${candidatesSnapshot.size} candidates for victims: ${deathsToProcess.map { it.playerName }}")

    // Single pass through all players' damage events
    candidatesSnapshot.forEach { potentialKiller ->
      potentialKiller.recentDamageEvents.forEach { event ->
        val normalizedTarget = event.target.normalizePlayerName()

        // Check if this damage event targets any victim in our batch (normalized)
        victimLookup[normalizedTarget]?.forEach { death ->
          val minTime = death.timestamp - LOOK_BEHIND_MS
          val maxTime = death.timestamp + LOOK_AHEAD_MS

          if (event.timestamp in minTime..maxTime) {
            val delta = abs(event.timestamp - death.timestamp)
            val currentBest = matches[death]

            // Take this event if it's closer in time than our current best match
            if (currentBest == null || delta < currentBest.second) {
              matches[death] = potentialKiller to delta
              Log.debug(TAG, "${potentialKiller.name} hit ${death.playerName} (delta: ${delta}ms)")
            }
          }
        }
      }
    }

    // 3. Reporting Phase: Compile results and hand off to PlayerCacheInteractor
    // gib to playercacheinteractor for storage
    // the playercacheinteractor will handle updating playercards appropriately in its own scope
    val results = deathsToProcess.map { death ->
      val result = matches[death]
      val killer = result?.first
      val delta = result?.second

      if (killer != null) {
        Log.info(TAG, "Attributed kill of ${death.playerName} to ${killer.name} (${delta}ms)")
      } else {
        Log.debug(TAG, "Death of ${death.playerName} was unattributed. (Environmental / PvE / Outside Time Window)")
      }

      // Return triple: (Victim, Timestamp, Killer?)
      Triple(death.playerName, death.timestamp, killer?.name)
    }

    /// yaaaas process the batch friends
    PlayerCacheInteractor.processDeathBatch(results)
  }

  /**
   * Normalizes player names for case-insensitive comparison.
   */
  private fun String.normalizePlayerName(): String {
    return this.trim().lowercase()
  }
}
