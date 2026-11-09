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
  val distance: Int,
  /** True when the scan actually returned buff data, including a confirmed empty result. */
  val confirmed: Boolean = true
)

/**
 * Who was in a raid at a moment in time. Recorded on every roster update, but only kept when
 * membership actually changes, so the Buff History graph and snapshot cards can resolve each
 * bucket's roster instead of applying the current roster to all of history.
 */
data class RaidRosterFrame(
  val observedAt: Long,
  val playerNames: Set<String>
)

data class RaidBuffObservation(
  val member: RaidFramePayload,
  val snapshot: RaidBuffSnapshot?,
  val isCurrent: Boolean
) {
  val observedAt: Long? get() = snapshot?.observedAt
  val isKnown: Boolean get() = snapshot?.confirmed == true
}
