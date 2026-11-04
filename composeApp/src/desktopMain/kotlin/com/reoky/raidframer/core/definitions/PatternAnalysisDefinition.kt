package com.reoky.raidframer.core.definitions

import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.interactor.PatternAnalysisInteractor
import com.reoky.raidframer.core.model.BuffEndedEvent
import com.reoky.raidframer.core.model.DebuffGainedEvent
import com.reoky.raidframer.core.model.SuccessfulCastEvent
import com.reoky.raidframer.core.model.postStillnessAffectedEnemies
import com.reoky.raidframer.core.model.postStillnessCharmsCleansed
import com.reoky.raidframer.core.model.postStillnessCleansed
import com.reoky.raidframer.core.model.postStillnessDispelled

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

// Stillness spell ID (11/03/26)
private const val stillnessSpellId = 10665

/**
 * Register all pattern analysis rules.
 * Called automatically by PatternAnalysisInteractor when start() is invoked.
 */
fun initializePatternAnalysis() {
  registerCriticalDiscordClassification()
  registerStillnessCleanses()
  registerStillnessDispels()
  registerStillnessCharmsCleansed()
  registerStillnessAffectedEnemies()
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

// --- Stillness pattern registrations (11/03/26) ---

/**
 * Counts song debuff removals on same-faction targets in the same Lua second as a Stillness cast.
 * Friendly initiator's successful cast of Stillness → same-faction players lose song debuffs.
 * No Silence confirmation needed for same-faction targets.
 */
private fun registerStillnessCleanses() {
  PatternAnalysisInteractor.register(
    PatternAnalysisInteractor.PatternRegistration(
      key = "stillness_cleanses",
      filter = { false }, // unused for multi-stage
      windowMs = 5000L, // 5s quiet period for multi-stage initiator expiration
      groupKey = { "" }, // unused for multi-stage
      onResult = { _, _ -> }, // unused for multi-stage
      initiatorFilter = { event ->
        event is SuccessfulCastEvent && event.spellId == stillnessSpellId
      },
      collectorFilter = { event ->
        event is BuffEndedEvent && event.buffId in songsDebuffIds
      },
      correlator = { initiator, collected ->
        val caster = (initiator as SuccessfulCastEvent).source
        val casterCard = PlayerCacheInteractor.getCard(caster)
        val casterFaction = casterCard?.lastKnownFaction
        if (casterFaction != null) {
          // Count unique same-faction targets that lost song debuffs
          val uniqueTargets = collected.filterIsInstance<BuffEndedEvent>().filter { event ->
            val targetCard = PlayerCacheInteractor.getCard(event.target)
            val targetFaction = targetCard?.lastKnownFaction
            targetFaction != null && targetFaction == casterFaction
          }.map { it.target }.toSet().size

          if (uniqueTargets > 0) {
            PlayerCacheInteractor.postStillnessCleansed(caster, uniqueTargets)
          }
        }
      }
    )
  )
}

/**
 * Counts song buff removals on opposite-faction targets only when Silence was applied by the initiator.
 * Opposite faction → confirm Silence (debuff 245) was applied by the initiator → count song buff removals.
 * Collects both BuffEndedEvent (song buff removals) and DebuffGainedEvent (Silence applications)
 * so the correlator can match silenced targets with their dispelled buffs.
 */
private fun registerStillnessDispels() {
  PatternAnalysisInteractor.register(
    PatternAnalysisInteractor.PatternRegistration(
      key = "stillness_dispels",
      filter = { false },
      windowMs = 5000L,
      groupKey = { "" },
      onResult = { _, _ -> },
      initiatorFilter = { event ->
        event is SuccessfulCastEvent && event.spellId == stillnessSpellId
      },
      collectorFilter = { event ->
        // Collect both song buff removals AND Silence applications
        (event is BuffEndedEvent && event.buffId in songsBuffIds) ||
        (event is DebuffGainedEvent && event.debuffId in silencedDebuffIds)
      },
      correlator = { initiator, collected ->
        val caster = (initiator as SuccessfulCastEvent).source
        val casterCard = PlayerCacheInteractor.getCard(caster)
        val casterFaction = casterCard?.lastKnownFaction
        if (casterFaction != null) {
          // Find targets that were silenced by the initiator (DebuffGainedEvent for silence)
          val silencedTargets = collected.filterIsInstance<DebuffGainedEvent>()
            .filter { it.debuffId in silencedDebuffIds && it.source == caster }
            .map { it.target }.toSet()

          // Count unique opposite-faction targets that lost song buffs AND were silenced
          val uniqueTargets = collected.filterIsInstance<BuffEndedEvent>().filter { event ->
            event.target in silencedTargets
          }.map { it.target }.toSet().size

          if (uniqueTargets > 0) {
            PlayerCacheInteractor.postStillnessDispelled(caster, uniqueTargets)
          }
        }
      }
    )
  )
}

/**
 * Counts only Charmed debuff removals (subset of cleanses).
 * Same logic as cleanses but filtered to charmedDebuffIds only.
 */
private fun registerStillnessCharmsCleansed() {
  PatternAnalysisInteractor.register(
    PatternAnalysisInteractor.PatternRegistration(
      key = "stillness_charms_cleansed",
      filter = { false },
      windowMs = 5000L,
      groupKey = { "" },
      onResult = { _, _ -> },
      initiatorFilter = { event ->
        event is SuccessfulCastEvent && event.spellId == stillnessSpellId
      },
      collectorFilter = { event ->
        event is BuffEndedEvent && event.buffId in charmedDebuffIds
      },
      correlator = { initiator, collected ->
        val caster = (initiator as SuccessfulCastEvent).source
        val casterCard = PlayerCacheInteractor.getCard(caster)
        val casterFaction = casterCard?.lastKnownFaction
        if (casterFaction != null) {
          val uniqueTargets = collected.filterIsInstance<BuffEndedEvent>().filter { event ->
            val targetCard = PlayerCacheInteractor.getCard(event.target)
            val targetFaction = targetCard?.lastKnownFaction
            targetFaction != null && targetFaction == casterFaction
          }.map { it.target }.toSet().size

          if (uniqueTargets > 0) {
            PlayerCacheInteractor.postStillnessCharmsCleansed(caster, uniqueTargets)
          }
        }
      }
    )
  )
}

/**
 * Tracks the number of unique targets hit per Stillness cast (Silence applications).
 * Updates most/average/least affected enemies stats.
 */
private fun registerStillnessAffectedEnemies() {
  PatternAnalysisInteractor.register(
    PatternAnalysisInteractor.PatternRegistration(
      key = "stillness_affected_enemies",
      filter = { false },
      windowMs = 5000L,
      groupKey = { "" },
      onResult = { _, _ -> },
      initiatorFilter = { event ->
        event is SuccessfulCastEvent && event.spellId == stillnessSpellId
      },
      collectorFilter = { event ->
        event is DebuffGainedEvent && event.debuffId in silencedDebuffIds
      },
      correlator = { initiator, collected ->
        val caster = (initiator as SuccessfulCastEvent).source
        // Count unique targets that received Silence from the initiator
        val uniqueTargets = collected.filterIsInstance<DebuffGainedEvent>()
          .filter { it.source == caster }
          .map { it.target }.toSet().size

        if (uniqueTargets > 0) {
          PlayerCacheInteractor.postStillnessAffectedEnemies(caster, uniqueTargets)
        }
      }
    )
  )
}
