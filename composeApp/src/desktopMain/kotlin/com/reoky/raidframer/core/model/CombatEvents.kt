package com.reoky.raidframer.core.model

interface CombatEvent {
  val timestamp: Long
  val cid: String
  val source: String
  val target: String
}

// What to do if the game gives us a blank string...
const val UNKNOWN_TARGET = "Unknown Target"

/*
 * I made this function to essentially help sanitize input from the IPC. Some events didn't have character names or
 * the game produces blanks.
 */
inline fun <reified T : CombatEvent> CombatEvent.normalize(): T {
  val s = source.ifBlank { UNKNOWN_TARGET }
  val t = target.ifBlank { UNKNOWN_TARGET }
  return when (this) {
    is DamageEvent -> DamageEvent(timestamp, cid, s, t, damage, spell, spellId, critical)
    is HealEvent -> HealEvent(timestamp, cid, s, t, amount, spell, spellId, critical)
    is CastingEvent -> CastingEvent(timestamp, cid, s, t, spell, spellId)
    is SuccessfulCastEvent -> SuccessfulCastEvent(timestamp, cid, s, t, spell, spellId)
    is BuffGainedEvent -> BuffGainedEvent(timestamp, cid, s, t, buff, buffId)
    is BuffEndedEvent -> BuffEndedEvent(timestamp, cid, s, t, buff, buffId)
    is DebuffGainedEvent -> DebuffGainedEvent(timestamp, cid, s, t, debuff, debuffId)
    is DebuffEndedEvent -> DebuffEndedEvent(timestamp, cid, s, t, debuff, debuffId)
    is DebuffAppliedEvent -> DebuffAppliedEvent(timestamp, cid, s, t, debuff, debuffId)
    is BuffAppliedEvent -> BuffAppliedEvent(timestamp, cid, s, t, buff, buffId)
    else -> throw IllegalArgumentException("Unsupported CombatEvent subtype: ${this::class.simpleName}") // better to just crash if this were to happen because this is supposed to be type-safe friends!
  } as T
}

data class DamageEvent(
  override val timestamp: Long,
  override val cid: String,
  override val source: String,
  override val target: String,
  val damage: Int,
  val spell: String,
  val spellId: Int,
  val critical: Boolean,
) : CombatEvent

data class HealEvent(
  override val timestamp: Long,
  override val cid: String,
  override val source: String,
  override val target: String,
  val amount: Int,
  val spell: String,
  val spellId: Int,
  val critical: Boolean,
) : CombatEvent

data class CastingEvent(
  override val timestamp: Long,
  override val cid: String,
  override val source: String,
  override val target: String,
  val spell: String,
  val spellId: Int,
  ) : CombatEvent

data class SuccessfulCastEvent(
  override val timestamp: Long,
  override val cid: String,
  override val source: String,
  override val target: String,
  val spell: String,
  val spellId: Int,
  ) : CombatEvent

data class BuffGainedEvent(
  override val timestamp: Long,
  override val cid: String,
  override val source: String,
  override val target: String,
  val buff: String,
  val buffId: Int,
) : CombatEvent

data class BuffEndedEvent(
  override val timestamp: Long,
  override val cid: String,
  override val source: String,
  override val target: String,
  val buff: String,
  val buffId: Int,
) : CombatEvent

data class DebuffGainedEvent(
  override val timestamp: Long,
  override val cid: String,
  override val source: String,
  override val target: String,
  val debuff: String,
  val debuffId: Int,
) : CombatEvent

data class DebuffEndedEvent(
  override val timestamp: Long,
  override val cid: String,
  override val source: String,
  override val target: String,
  val debuff: String,
  val debuffId: Int,
) : CombatEvent

data class DebuffAppliedEvent(
  override val timestamp: Long,
  override val cid: String,
  override val source: String,
  override val target: String,
  val debuff: String,
  val debuffId: Int,
) : CombatEvent

data class BuffAppliedEvent(
  override val timestamp: Long,
  override val cid: String,
  override val source: String,
  override val target: String,
  val buff: String,
  val buffId: Int,
) : CombatEvent
