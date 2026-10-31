package com.reoky.raidframer.core.definitions

import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.interactor.PatternAnalysisInteractor
import com.reoky.raidframer.core.model.DebuffGainedEvent

/**
 * All pattern registrations for PatternAnalysisInteractor live here.
 * This keeps the interactor class completely generic it knows nothing about
 * specific debuff IDs, synthetic IDs, or each event type. We are essentially defining
 * patterns to search for in this file. The pattern revolving logic is in PatternAnalysisInteractor.
 */

// --- Synthetic ID constants ---
// All synthetic IDs use negative numbers to distinguish them from real game IDs.
// The game only generates positive IDs (some in the 9000000+ range), so negatives
// are guaranteed to never collide. Players looking up these IDs on the ArcheRage
// wiki will immediately know they are app-internal identifiers.
private const val SYNTHETIC_ID_OFFSET = -10000
private const val SYNTHETIC_AOE_DISCORD_ID = SYNTHETIC_ID_OFFSET - 1

// Debuff IDs that should be intercepted for time-window classification
private val criticalDiscordDebuffIds = setOf(21992)

/**
 * Register all pattern analysis rules.
 * Called automatically by PatternAnalysisInteractor when start() is invoked.
 */
fun initializePatternAnalysis() {
  registerCriticalDiscordClassification()
}

/**
 * Classifies Critical Discord (debuff 21992) as AoE or non-AoE based on the number
 * of targets hit within a time window. When the AoE version is cast, multiple stacks
 * of debuff 21992 are applied by the same caster within the same time window (one per
 * target). The non-AoE version applies a single stack.
 *
 * Uses a 3-second quiet period to account for IPC lag: events from the same AoE cast
 * may arrive spread across multiple seconds due to network delays between the Lua
 * companion and the desktop app.
 *
 * The AoE and Non-AoE versions of Discord can have the same ID 21992 so that's the reason gotta do this, friends.
 */
private fun registerCriticalDiscordClassification() {
  PatternAnalysisInteractor.register(
    PatternAnalysisInteractor.PatternRegistration(
      key = "critical_discord_aoe",
      filter = { event ->
        event is DebuffGainedEvent && event.debuffId in criticalDiscordDebuffIds
      },
      windowMs = 3000L, // 3s quiet period to absorb IPC lag between Lua and desktop
      groupKey = { event ->
        // Group by caster name so each caster is classified independently
        (event as? DebuffGainedEvent)?.source ?: ""
      },
      onResult = { groupKey, events ->
        val caster = groupKey
        val eventCount = events.size

        if (eventCount > 1) {
          // AoE: multiple targets hit by the same caster in the same time window
          // Post one classified event per target, each with the synthetic AoE ID
          for (event in events) {
            val debuffEvent = event as DebuffGainedEvent
            PlayerCacheInteractor.postEventInternal(
              DebuffGainedEvent(
                timestamp = debuffEvent.timestamp,
                cid = debuffEvent.cid,
                source = debuffEvent.source,
                target = debuffEvent.target,
                debuff = debuffEvent.debuff,
                debuffId = SYNTHETIC_AOE_DISCORD_ID
              )
            )
          }
        } else {
          // Non-AoE: single target — post the original event unchanged
          val event = events.first() as DebuffGainedEvent
          PlayerCacheInteractor.postEventInternal(event)
        }
      }
    )
  )
}
