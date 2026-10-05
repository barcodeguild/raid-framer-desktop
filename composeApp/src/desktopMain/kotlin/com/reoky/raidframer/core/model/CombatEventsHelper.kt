package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.interactor.Log

/**
 * Switching/counting helpers for avoidance + absorb events. Some keywords, like "ABSORB", come from the game client,
 * so we're adding support for multiple languages here to match the korean and chinese game clients.
 * English keywords are active; KO/ZH slots are intentionally empty until verified
 * in-game. Unknown non-blank skill names are logged to debug.log so testers can
 * report translations.
 */
object CombatEventsHelper {
  private const val TAG = "CombatEventsHelper"

  // English absorb-shield keywords (nope ABSORB/SHIELD/BARRIER/WARD/PROTECTIVE).
  private val absorbKeywordsEn = setOf("ABSORB", "SHIELD", "BARRIER", "WARD", "PROTECTIVE")

  // TODO(i18n): fill once verified in-game with KO/ZH clients. Leave empty until then.
  private val absorbKeywordsKo: Set<String> = emptySet()
  private val absorbKeywordsZh: Set<String> = emptySet()

  private val seenUnknownSkills = mutableSetOf<String>()
  private const val MAX_UNKNOWN_LOG = 500

  fun isAbsorbShieldSkill(skillName: String): Boolean {
    if (skillName.isBlank()) return false
    val upper = skillName.uppercase()
    if (absorbKeywordsEn.any { upper.contains(it) }) return true
    if (absorbKeywordsKo.any { skillName.contains(it) }) return true
    if (absorbKeywordsZh.any { skillName.contains(it) }) return true
    logUnknownSkill(skillName) // Logging this in case the game gives us something that isn't handled yet / another language.
    return false
  }

  fun isManaBarrierSkill(skillName: String): Boolean {
    if (skillName.isBlank()) return false
    if (skillName.equals("Mana Barrier", ignoreCase = true)) return true
    logUnknownSkill(skillName)
    return false
  }

  // --- Avoidance result parsing (i18n) ---
  // English miss-result keywords. KO/ZH slots stay empty until verified in-game;
  // unknown non-blank values are logged to debug.log for translation pickup.
  private val blockKeywordsEn = setOf("BLOCK")
  private val parryKeywordsEn = setOf("PARRY")
  private val dodgeKeywordsEn = setOf("DODGE", "EVADE")
  private val immuneKeywordsEn = setOf("IMMUNE")
  private val missKeywordsEn = setOf("MISS")

  // TODO(i18n): fill once verified in-game with KO/ZH clients. Leave empty until then.
  private val blockKeywordsKo: Set<String> = emptySet()
  private val parryKeywordsKo: Set<String> = emptySet()
  private val dodgeKeywordsKo: Set<String> = emptySet()
  private val immuneKeywordsKo: Set<String> = emptySet()
  private val missKeywordsKo: Set<String> = emptySet()
  private val blockKeywordsZh: Set<String> = emptySet()
  private val parryKeywordsZh: Set<String> = emptySet()
  private val dodgeKeywordsZh: Set<String> = emptySet()
  private val immuneKeywordsZh: Set<String> = emptySet()
  private val missKeywordsZh: Set<String> = emptySet()

  private val seenUnknownResults = mutableSetOf<String>()

  fun parseAvoidanceKind(raw: String): AvoidanceKind {
    val key = raw.trim()
    if (key.isEmpty()) return AvoidanceKind.OTHER
    val upper = key.uppercase()
    if (upper in blockKeywordsEn || key in blockKeywordsKo || key in blockKeywordsZh) return AvoidanceKind.BLOCK
    if (upper in parryKeywordsEn || key in parryKeywordsKo || key in parryKeywordsZh) return AvoidanceKind.PARRY
    if (upper in dodgeKeywordsEn || key in dodgeKeywordsKo || key in dodgeKeywordsZh) return AvoidanceKind.DODGE
    if (upper in immuneKeywordsEn || key in immuneKeywordsKo || key in immuneKeywordsZh) return AvoidanceKind.IMMUNE
    if (upper in missKeywordsEn || key in missKeywordsKo || key in missKeywordsZh) return AvoidanceKind.MISS
    if (key !in seenUnknownResults && seenUnknownResults.size < MAX_UNKNOWN_LOG) {
      seenUnknownResults += key
      Log.debug(TAG, "Unknown avoidance result: '$key' (report KO/ZH translation if non-English)")
    }
    return AvoidanceKind.OTHER
  }

  // The game's fixed mitigation on partial avoidance (verified from combat logs):
  // PARRY reduces the hit by 40% so the defender takes 60% of it,
  // BLOCK reduces it by 60% so the defender takes 40% of it.
  // We had reok do the calculation and so hopefully everything isn't backwards.
  private const val PARRY_KEPT_RATIO = 0.6
  private const val BLOCK_KEPT_RATIO = 0.4

  /**
   * Reverse-calculates the HP that was prevented by a partial-mitigation result.
   *
   * SPELL_MISSED / MELEE_MISSED report the damage the defender ACTUALLY took,
   * so we reconstruct the original hit (`round(taken / keptRatio)`) and subtract
   * what landed: parry 55 taken -> original 92 -> 37 prevented; block 55 taken
   * -> original 138 (137.5 rounds up) -> 83 prevented.
   *
   * Divide-first is deliberate: `taken * (1 - ratio) / ratio` hits an IEEE trap
   * (0.6/0.4 = 1.4999...98, so 55 -> 82 instead of 83), while `taken / ratio`
   * is tie-free for every Int (taken/0.6 can never land exactly on .5, and
   * taken/0.4 always lands on the exact representable half).
   *
   * DODGE/IMMUNE/MISS report 0 taken, so the original hit size is unknowable
   * and this returns 0 — those columns stay counts.
   */
  fun mitigatedHp(kind: AvoidanceKind, damageTaken: Int): Long = when (kind) {
    AvoidanceKind.PARRY -> Math.round(damageTaken / PARRY_KEPT_RATIO) - damageTaken
    AvoidanceKind.BLOCK -> Math.round(damageTaken / BLOCK_KEPT_RATIO) - damageTaken
    else -> 0L
  }

  /**
   * Log suffix like ", prevented 83" for blocks/parries, empty otherwise so
   * dodge/immune/miss lines don't claim "prevented 0" (unknowable, not zero).
   */
  fun mitigationLogInfo(kind: AvoidanceKind, damageTaken: Int): String {
    val prevented = mitigatedHp(kind, damageTaken)
    return if (prevented > 0) ", prevented $prevented" else ""
  }

  /**
   * Avoidance credit direction: every kind credits the avoider (target card).
   * BLOCK/PARRY/DODGE/IMMUNE/MISS all mean the defender avoided the attacker's
   * hit — e.g. IMMUNE pops over the invincible defender's head, so the defender
   * gets the Top Immune point, not the attacker.
   */
  fun creditAvoidanceToTarget(kind: AvoidanceKind): Boolean = kind != AvoidanceKind.OTHER

  private fun logUnknownSkill(skillName: String) {
    val key = skillName.trim()
    if (key.isBlank() || key in seenUnknownSkills) return
    if (seenUnknownSkills.size >= MAX_UNKNOWN_LOG) return
    seenUnknownSkills += key
    Log.debug(TAG, "Unknown skill for absorb classification: '$key' (report KO/ZH translation if non-English)")
  }
}
