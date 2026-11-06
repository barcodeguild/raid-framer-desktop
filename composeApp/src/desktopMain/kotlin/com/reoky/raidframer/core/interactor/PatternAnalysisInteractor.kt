package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.definitions.initializePatternAnalysis
import com.reoky.raidframer.core.model.CombatEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object PatternAnalysisInteractor : Interactor() {

  private const val TAG = "PatternAnalysis"

  /**
   * Registration configuration for a pattern analysis rule.
   * Supports two modes:
   * 1. Single-filter (existing): filter + windowMs + groupKey + onResult
   * 2. Multi-stage (new): initiatorFilter + collectorFilter + windowMs + onResult
   *
   * @param key Unique identifier for this registration (e.g., "critical_discord_aoe")
   * @param filter Predicate for single-filter patterns (ignored if initiatorFilter is set)
   * @param windowMs Quiet period in milliseconds after the last event arrives before processing.
   *                 Accounts for IPC lag — events arriving within this window stay grouped.
   * @param groupKey Extractor to group events (e.g., caster name, target CID) — for single-filter patterns
   * @param onResult Handler invoked when a group expires, receiving the group key and events
   * @param initiatorFilter Optional: for multi-stage patterns, predicate to match the initiator event
   * @param collectorFilter Optional: for multi-stage patterns, predicate to match collector events
   * @param correlator Optional: for multi-stage patterns, handler receiving initiator + collected events
   */
  data class PatternRegistration(
    val key: String,
    val filter: (CombatEvent) -> Boolean,
    val windowMs: Long,
    val groupKey: (CombatEvent) -> String,
    val onResult: (String, List<CombatEvent>) -> Unit,
    // Multi-stage pattern fields (optional)
    val initiatorFilter: ((CombatEvent) -> Boolean)? = null,
    val collectorFilter: ((CombatEvent) -> Boolean)? = null,
    val correlator: ((CombatEvent, List<CombatEvent>) -> Unit)? = null,
  )

  private val mutex = Mutex()

  private val registrations = mutableListOf<PatternRegistration>()

  /** Buffered events for single-filter patterns, paired with their registration key and arrival time. */
  private val pendingEvents = mutableListOf<PendingEvent>()

  private data class PendingEvent(
    val registrationKey: String,
    val event: CombatEvent,
    val arrivalTime: Long,
  )

  // --- Multi-stage pattern state ---

  /** Active initiators for multi-stage patterns, keyed by (registrationKey, initiatorKey). */
  private val activeInitiators = mutableMapOf<String, InitiatorBucket>()

  /** Mutable holder for an initiator event and its collected events for multi-stage patterns. */
  private class InitiatorBucket(
    val registrationKey: String,
    val initiatorEvent: CombatEvent,
    val collectorEvents: MutableList<CombatEvent> = mutableListOf(),
    var lastArrivalTime: Long,
  )

  /** Buffered collector events for multi-stage patterns, paired with their registration key and arrival time. */
  private val pendingCollectorEvents = mutableListOf<PendingCollectorEvent>()

  private data class PendingCollectorEvent(
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
   * when an event matches a registered filter. For multi-stage patterns, this method
   * determines whether the event is an initiator or a collector.
   */
  fun queueEvent(registrationKey: String, event: CombatEvent) {
    scope.launch {
      mutex.withLock {
        val registration = registrations.find { it.key == registrationKey }
        if (registration == null) {
          Log.debug(TAG, "Dropping event for unknown registration: $registrationKey")
          return@withLock
        }

        val now = System.currentTimeMillis()

        // Multi-stage pattern: check if this is an initiator or collector event
        if (registration.initiatorFilter != null && registration.collectorFilter != null) {
          if (registration.initiatorFilter(event)) {
            // This is an initiator event — open a new collection bucket
            val initiatorKey = "${registrationKey}::${event.source}::${event.timestamp}"
            activeInitiators[initiatorKey] = InitiatorBucket(
              registrationKey = registrationKey,
              initiatorEvent = event,
              lastArrivalTime = now
            )
            Log.debug(TAG, "Initiator matched [$registrationKey] by ${event.source} at timestamp ${event.timestamp}")
          } else if (registration.collectorFilter(event)) {
            // This is a collector event — buffer it for correlation
            pendingCollectorEvents.add(PendingCollectorEvent(registrationKey, event, now))
          }
        } else {
          // Single-filter pattern — use existing logic
          pendingEvents.add(PendingEvent(registrationKey, event, now))
        }
      }
    }
  }

  /**
   * Check if an event should be intercepted by any registered pattern.
   * Returns all matching registration keys. For multi-stage patterns, checks both
   * initiatorFilter and collectorFilter. An event can match multiple registrations
   * (e.g., a Silence DebuffGainedEvent can match both stillness_dispels and stillness_affected_enemies).
   */
  fun findMatchingRegistrations(event: CombatEvent): List<String> {
    val matched = mutableListOf<String>()
    for (reg in registrations) {
      if (reg.initiatorFilter != null && reg.initiatorFilter(event)) {
        matched.add(reg.key)
      } else if (reg.collectorFilter != null && reg.collectorFilter(event)) {
        matched.add(reg.key)
      } else if (reg.filter(event)) {
        matched.add(reg.key)
      }
    }
    return matched
  }

  /**
   * Returns true if the event should pass through directly to PlayerCacheInteractor.
   * Multi-stage patterns (initiatorFilter + collectorFilter) only correlate and update counters —
   * they do not repost events, so the caller should post directly.
   * Single-filter patterns repost events from their onResult handler, so the caller should
   * skip posting directly when a single-filter pattern matches.
   */
  fun shouldPassthrough(event: CombatEvent, matchedKeys: List<String>): Boolean {
    if (matchedKeys.isEmpty()) return true
    for (key in matchedKeys) {
      val reg = registrations.find { it.key == key }
      if (reg == null) continue
      // If this registration is a single-filter pattern (no initiatorFilter), it will repost from onResult
      if (reg.initiatorFilter == null && reg.filter(event)) {
        return false
      }
    }
    return true
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
        activeInitiators.clear()
        pendingCollectorEvents.clear()
        Log.info(TAG, "Cleared all pattern registrations and pending events")
      }
    }
    super.stop()
  }

  override suspend fun interact() {
    processExpiredGroups()
    processMultiStagePatterns()
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

  /**
   * Process multi-stage patterns: correlate collector events with their initiators
   * using Lua timestamp matching, then invoke the correlator handler.
   */
  private suspend fun processMultiStagePatterns() {
    mutex.withLock {
      if (pendingCollectorEvents.isEmpty() && activeInitiators.isEmpty()) return

      val now = System.currentTimeMillis()

      // Match pending collector events to active initiators by Lua timestamp
      val matchedCollectors = mutableListOf<PendingCollectorEvent>()

      for (pending in pendingCollectorEvents) {
        val collectorEvent = pending.event
        // Find active initiators with matching Lua timestamp
        for ((initiatorKey, bucket) in activeInitiators) {
          if (bucket.registrationKey == pending.registrationKey &&
              bucket.initiatorEvent.timestamp == collectorEvent.timestamp) {
            bucket.collectorEvents.add(collectorEvent)
            bucket.lastArrivalTime = pending.arrivalTime
            matchedCollectors.add(pending)
            break // Only match to the first matching initiator
          }
        }
      }

      // Remove matched collectors from pending
      for (matched in matchedCollectors) {
        pendingCollectorEvents.remove(matched)
      }

      // Process expired initiators (quiet for longer than windowMs)
      val expiredInitiators = mutableListOf<String>()
      for ((initiatorKey, bucket) in activeInitiators) {
        val registration = registrations.find { it.key == bucket.registrationKey }
        if (registration == null) {
          expiredInitiators.add(initiatorKey)
          continue
        }

        if (now - bucket.lastArrivalTime > registration.windowMs) {
          // Expired — invoke correlator if present
          if (registration.correlator != null && bucket.collectorEvents.isNotEmpty()) {
            registration.correlator(bucket.initiatorEvent, bucket.collectorEvents.toList())
            Log.debug(TAG, "Multi-stage result [$bucket.registrationKey] initiator=${bucket.initiatorEvent.source}, collectors=${bucket.collectorEvents.size}")
          }
          expiredInitiators.add(initiatorKey)
        }
      }

      // Remove expired initiators
      for (key in expiredInitiators) {
        activeInitiators.remove(key)
      }

      // Clean up old pending collector events that have been sitting too long
      val maxCollectorAge = registrations.maxOfOrNull { it.windowMs } ?: 5000L
      pendingCollectorEvents.removeAll { now - it.arrivalTime > maxCollectorAge * 2 }
    }
  }

  private fun registrationFor(compositeKey: String): PatternRegistration? {
    val registrationKey = compositeKey.split("::")[0]
    return registrations.find { it.key == registrationKey }
  }
}
