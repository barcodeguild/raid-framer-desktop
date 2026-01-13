package com.reoky.raidframer.core.model

interface CombatEvent {
  val timestamp: Long
  val cid: String
}

data class DamageEvent(
  override val timestamp: Long,
  override val cid: String,
  val caster: String,
  val target: String,
  val damage: Int,
  val spell: String,
  val critical: Boolean,
) : CombatEvent

data class HealEvent(
  override val timestamp: Long,
  override val cid: String,
  val caster: String,
  val target: String,
  val amount: Int,
  val spell: String,
  val critical: Boolean,
) : CombatEvent

data class CastingEvent(
  override val timestamp: Long,
  override val cid: String,
  val caster: String,
  val spell: String,
) : CombatEvent

data class SuccessfulCastEvent(
  override val timestamp: Long,
  override val cid: String,
  val caster: String,
  val spell: String,
) : CombatEvent

data class BuffGainedEvent(
  override val timestamp: Long,
  override val cid: String,
  val source: String? = null,
  val target: String,
  val buff: String,
) : CombatEvent

data class BuffEndedEvent(
  override val timestamp: Long,
  override val cid: String,
  val source: String? = null,
  val target: String,
  val buff: String,
) : CombatEvent

data class DebuffGainedEvent(
  override val timestamp: Long,
  override val cid: String,
  val source: String? = null,
  val target: String,
  val debuff: String,
) : CombatEvent

data class DebuffEndedEvent(
  override val timestamp: Long,
  override val cid: String,
  val source: String? = null,
  val target: String,
  val debuff: String,
) : CombatEvent

data class DebuffAppliedEvent(
  override val timestamp: Long,
  override val cid: String,
  val source: String? = null,
  val target: String,
  val debuff: String,
) : CombatEvent

data class BuffAppliedEvent(
  override val timestamp: Long,
  override val cid: String,
  val source: String? = null,
  val target: String,
  val buff: String,
) : CombatEvent
