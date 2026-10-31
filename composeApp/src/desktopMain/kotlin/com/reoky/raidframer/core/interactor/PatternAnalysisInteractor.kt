package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.definitions.initializePatternAnalysis
import com.reoky.raidframer.core.model.CombatEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object PatternAnalysisInteractor : Interactor() {

  private const val TAG = "PatternAnalysis"

  /**
   * Registration configuration for a single pattern analysis rule.
   *
   * @param key Unique identifier for this registration (e.g., "critical_discord_aoe")
   * @param filter Predicate to determine which events to capture
   * @param windowMs Quiet period in milliseconds after the last event arrives before processing.
   *                 Accounts for IPC lag — events arriving within this window stay grouped.
   * @param groupKey Extractor to group events (e.g., caster name, target CID)
   * @param onResult Handler invoked when a group expires, receiving the group key and events
   */
  data class PatternRegistration(
    val key: String,
    val filter: (CombatEvent) -> Boolean,
    val windowMs: Long,
    val groupKey: (CombatEvent) -> String,
    val onResult: (String, List<CombatEvent>) -> Unit,
  )

  private val mutex = Mutex()

  private val registrations = mutableListOf<PatternRegistration>()

  /** Buffered events waiting to be processed, paired with their registration key and arrival time. */
  private val pendingEvents = mutableListOf<PendingEvent>()

  private data class PendingEvent(
    val registrationKey: String,
    val event: CombatEvent,
    val arrivalTime: Long,
  )

  /**
   * Register a new pattern analysis rule.
   */
  fun register(registration: PatternRegistration) {
    scope.launch {
      mutex.withLock {
        registrations.add(registration)
        Log.info(TAG, "Registered pattern: ${registration.key} (window: ${registration.windowMs}ms)")
      }
    }
  }

  /**
   * Queue an event for processing. Called by interceptors (e.g., CompanionInteractor)
   * when an event matches a registered filter.
   */
  fun queueEvent(registrationKey: String, event: CombatEvent) {
    scope.launch {
      mutex.withLock {
        pendingEvents.add(PendingEvent(registrationKey, event, System.currentTimeMillis()))
      }
    }
  }

  /**
   * Check if an event should be intercepted by any registered pattern.
   * Returns the registration key if a match is found, null otherwise.
   */
  fun findMatchingRegistration(event: CombatEvent): String? {
    return registrations.find { it.filter(event) }?.key
  }

  /**
   * Self-initialize by loading all pattern registrations from the definitions package.
   * Called automatically when start() is invoked.
   */
  override fun start(delay: Long) {
    initializePatternAnalysis()
    super.start(delay)
  }

  /**
   * Clear all registrations when stopping, allowing clean re-initialization on restart.
   */
  override fun stop() {
    scope.launch {
      mutex.withLock {
        registrations.clear()
        pendingEvents.clear()
        Log.info(TAG, "Cleared all pattern registrations and pending events")
      }
    }
    super.stop()
  }

  override suspend fun interact() {
    processExpiredGroups()
  }

  private suspend fun processExpiredGroups() {
    mutex.withLock {
      if (pendingEvents.isEmpty()) return

      val now = System.currentTimeMillis()

      // Group pending events by (registration key, group key) using arrival-time sliding window
      // Key: "registrationKey::groupKey" -> list of (pending event, events)
      val groups = mutableMapOf<String, MutableList<PendingEvent>>()

      for (pending in pendingEvents) {
        val registration = registrations.find { it.key == pending.registrationKey }
        if (registration == null) {
          Log.debug(TAG, "Dropping event for unknown registration: ${pending.registrationKey}")
          continue
        }

        val group = registration.groupKey(pending.event)
        val compositeKey = "${pending.registrationKey}::$group"
        groups.getOrPut(compositeKey) { mutableListOf() }.add(pending)
      }

      // Determine which groups have expired (quiet for longer than windowMs)
      val expiredGroups = mutableMapOf<String, MutableList<PendingEvent>>()
      val stillPending = mutableListOf<PendingEvent>()

      for ((compositeKey, groupEvents) in groups) {
        // Find the latest arrival time in this group
        val latestArrival = groupEvents.maxOf { it.arrivalTime }

        // If enough time has passed since the last event in this group, it's expired
        if (now - latestArrival > registrationFor(compositeKey)?.windowMs ?: 0L) {
          expiredGroups[compositeKey] = groupEvents
        } else {
          stillPending.addAll(groupEvents)
        }
      }

      // Process expired groups
      for ((compositeKey, events) in expiredGroups) {
        val parts = compositeKey.split("::")
        if (parts.size < 2) continue
        val registrationKey = parts[0]
        val groupKey = parts.drop(1).joinToString("::") // Rejoin in case group key contained "::"

        val registration = registrations.find { it.key == registrationKey }
        if (registration != null) {
          val combatEvents = events.map { it.event }
          Log.debug(TAG, "Processing group [$registrationKey] group=$groupKey, events=${combatEvents.size}")
          registration.onResult(groupKey, combatEvents)
        }
      }

      // Keep only still-pending events
      pendingEvents.clear()
      pendingEvents.addAll(stillPending)
    }
  }

  private fun registrationFor(compositeKey: String): PatternRegistration? {
    val registrationKey = compositeKey.split("::")[0]
    return registrations.find { it.key == registrationKey }
  }
}
