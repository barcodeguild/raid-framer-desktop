package com.reoky.raidframer.core.serialization

import kotlinx.serialization.Serializable

typealias Party = List<RaidFramePayload>

@Serializable
data class BuffTooltipPayload(
  val buff_id: Int = 0,
  val name: String = "",
  val description: String = "",
  val path: String = "",
  val category: String = "",
  val tipType: String = "",
  val mine: Boolean = false,
  val stack: Int = 0,
  val timeLeft: Int = 0,
  val timeUnit: String = "",
  val duration: Int = 0,
  val healAmount: Int = 0
)

@Serializable
data class BuffPayload(
  val buff_id: Int = 0,
  val tooltip: BuffTooltipPayload = BuffTooltipPayload()
)

@Serializable
data class RaidFramePayload(
  val slot: Int = 0,
  val playerName: String = "",
  val gearScore: Int = 0,
  val role: Int = 0,
  val characterBuild: String = "",
  val lastZone: String = "",
  val distance: Int = -1,
  @Serializable(with = SecondsToMillisSerializer::class)
  val lastUpdated: Long = 0L,
  val buffs: List<BuffPayload> = emptyList()
) {
}
