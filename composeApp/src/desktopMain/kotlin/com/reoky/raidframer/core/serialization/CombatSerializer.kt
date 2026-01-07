package com.reoky.raidframer.core.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class CombatEventPayload {

  @Serializable
  @SerialName("SPELL_AURA_APPLIED")
  data class BuffGainedPayload(
    val timestamp: Long,
    val cid: String,
    val source: String,
    val target: String,
    val buffId: Int,
    val buffName: String,
    val damageType: String,
    val buffType: String, // DEBUFF / BUFF
    val isActive: Boolean
  ) : CombatEventPayload()

  @Serializable
  @SerialName("SPELL_AURA_REMOVED")
  data class BuffEndedPayload(
    val timestamp: Long,
    val cid: String,
    val source: String,
    val target: String,
    val buffId: Int,
    val buffName: String,
    val damageType: String,
    val buffType: String, // DEBUFF / BUFF
    val isActive: Boolean
  ) : CombatEventPayload()

  @Serializable
  @SerialName("SPELL_DAMAGE")
  data class DamagePayload(
    val timestamp: Long,
    val cid: String,
    val source: String,
    val target: String,
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

}
