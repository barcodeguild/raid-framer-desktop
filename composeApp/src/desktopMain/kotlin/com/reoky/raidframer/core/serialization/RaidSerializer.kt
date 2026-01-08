package com.reoky.raidframer.core.serialization

import kotlinx.serialization.Serializable

typealias Party = List<RaidFramePayload>

@Serializable
data class RaidFramePayload(
  val slot: Int = 0,
  val playerName: String = "",
  val gearScore: Int = 0,
  val role: Int = 0,
  val characterBuild: String = "",
  val lastZone: String = "",
  val distance: Int = -1,
  val lastUpdated: Long = 0L
) {
}
