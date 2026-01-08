package com.reoky.raidframer.core.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class CombatEventPayload {

  @Serializable
  @SerialName("SPELL_CAST_START")
  data class SpellCastStartPayload(
    @Serializable(with = SecondsToMillisSerializer::class)
    val timestamp: Long,
    val cid: String,
    val source: String = "Unknown Target",
    val target: String = "Unknown Target",
    val spellId: String,
    val spellName: String,
    val damageType: String
  ) : CombatEventPayload()

  @Serializable
  @SerialName("SPELL_CAST_SUCCESS")
  data class SpellCastSuccessPayload(
    @Serializable(with = SecondsToMillisSerializer::class)
    val timestamp: Long,
    val cid: String,
    val source: String = "Unknown Target",
    val target: String = "Unknown Target",
    val spellId: String,
    val spellName: String,
    val damageType: String
  ) : CombatEventPayload()

  @Serializable
  @SerialName("SPELL_AURA_APPLIED")
  data class BuffGainedPayload(
    @Serializable(with = SecondsToMillisSerializer::class)
    val timestamp: Long,
    val cid: String,
    val source: String = "Unknown Target",
    val target: String = "Unknown Target",
    val buffId: Int,
    val buffName: String,
    val damageType: String,
    val buffType: String, // DEBUFF / BUFF
    val isActive: Boolean
  ) : CombatEventPayload()

  @Serializable
  @SerialName("SPELL_AURA_REMOVED")
  data class BuffEndedPayload(
    @Serializable(with = SecondsToMillisSerializer::class)
    val timestamp: Long,
    val cid: String,
    val source: String = "Unknown Target",
    val target: String = "Unknown Target",
    val buffId: Int,
    val buffName: String,
    val damageType: String,
    val buffType: String, // DEBUFF / BUFF
    val isActive: Boolean
  ) : CombatEventPayload()

  @Serializable
  @SerialName("SPELL_DAMAGE")
  data class DamagePayload(
    @Serializable(with = SecondsToMillisSerializer::class)
    val timestamp: Long,
    val cid: String,
    val source: String = "Unknown Target",
    val target: String = "Unknown Target",
    val unknownInt: Int,
    val spell: String,
    val damageType: String,
    val amount: Int,
    val pool: String, // HEALTH / MANA
    val result: String, // HIT / CRITICAL
    val f11: Int,
    val f12: Int,
    val f13: Boolean,
    val f14: String, // sometimes false sometime 0
    val f15: Boolean
  ) : CombatEventPayload()

  @Serializable
  @SerialName("SPELL_HEALED")
  data class HealPayload(
    @Serializable(with = SecondsToMillisSerializer::class)
    val timestamp: Long,
    val cid: String,
    val source: String = "Unknown Target",
    val target: String = "Unknown Target",
    val unknownInt: Int,
    val spell: String,
    val damageType: String, // HOLY
    val amount: Int,
    val result: String, // HIT / CRITICAL
    val f10: Boolean,
    val f11: Int
  ) : CombatEventPayload()


  // if a person heals after a duel we've seen this event come through
  // as a healing from the source to themselves
  @Serializable
  @SerialName("SPELL_ENERGIZE")
  data class EnergizePayload(
    @Serializable(with = SecondsToMillisSerializer::class)
    val timestamp: Long,
    val cid: String,
    val source: String = "Unknown Target",
    val target: String = "Unknown Target",
    val unknownInt: Int,
    val spell: String,
    val damageType: String, // HOLY
    val amount: Int,
    val pool: String, // MANA / HEALTH / HOLY
  ) : CombatEventPayload()

  // Thinking this means damage as a result of a spell like burning and earthen grip's damage
  @Serializable
  @SerialName("SPELL_DOT_DAMAGE")
  data class ConditionDamagePayload(
    @Serializable(with = SecondsToMillisSerializer::class)
    val timestamp: Long,
    val cid: String,
    val source: String = "Unknown Target",
    val target: String = "Unknown Target",
    val unknownInt: Int,
    val spell: String,
    val damageType: String, // PHYSICAL
    val amount: Int,
    val pool: String, // HEALTH / MANA
    val result: String, // HIT / CRITICAL
    val f11: Int,
    val f12: Int,
    val f13: Boolean,
    val f14: String, // sometimes false sometime 0
    val f15: Boolean
  ) : CombatEventPayload()

  @Serializable
  @SerialName("ENVIRONMENTAL_DAMAGE")
  data class EnvironmentalDamagePayload(
    @Serializable(with = SecondsToMillisSerializer::class)
    val timestamp: Long,
    val cid: String,
    val source: String = "Unknown Target",
    val target: String = "Unknown Target",
    val damageType: String, // FALLING / DROWNING
    val unknownInt: Int,
    val amount: Int,
    val pool: String, // HEALTH / MANA
    val result: String, // HIT / CRITICAL
    val f10: Int,
    val f11: Int,
    val f12: Boolean,
    val f13: String, // sometimes false sometime 0
    val f14: Boolean // might need to change this to string
  ) : CombatEventPayload()
}
