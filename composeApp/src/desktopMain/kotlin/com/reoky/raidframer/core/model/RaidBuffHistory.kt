package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.serialization.RaidFramePayload

enum class RaidBuffGracePeriod(val millis: Long) {
  IMMEDIATE(10_000L),
  FIFTEEN_MINUTES(15 * 60_000L),
  THIRTY_MINUTES(30 * 60_000L),
  ONE_HOUR(60 * 60_000L),
  SIX_HOURS(6 * 60 * 60_000L)
}

data class RaidBuffSnapshot(
  val observedAt: Long,
  val buffIds: Set<Int>,
  val distance: Int
)

data class RaidBuffObservation(
  val member: RaidFramePayload,
  val snapshot: RaidBuffSnapshot?,
  val isCurrent: Boolean
)
