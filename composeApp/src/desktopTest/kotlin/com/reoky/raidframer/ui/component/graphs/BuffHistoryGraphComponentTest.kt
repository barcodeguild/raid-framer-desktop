package com.reoky.raidframer.ui.component.graphs

import com.reoky.raidframer.core.definitions.RaidBuffKey
import com.reoky.raidframer.core.definitions.RaidBuffRequirements
import com.reoky.raidframer.core.model.RaidBuffSnapshot
import com.reoky.raidframer.core.model.RaidRosterFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuffHistoryGraphComponentTest {
  private val t0 = 1_700_000_000_000L
  private val gobletId = 24469
  private val requirements = RaidBuffRequirements(selected = setOf(RaidBuffKey.GOBLET))

  private fun snapshot(at: Long, vararg buffIds: Int) = RaidBuffSnapshot(at, buffIds.toSet(), distance = 10)

  private fun cards(
    timeline: Map<String, List<RaidBuffSnapshot>>,
    names: List<String>,
    sessionStart: Long = t0
  ) = computeSnapshotCards(
    timeline = timeline,
    sessionStart = sessionStart,
    allPlayerNames = names,
    requirements = requirements,
    individualBuffKey = null,
    mode = BuffHistoryMode.ALL_BUFFS
  )

  @Test
  fun bucketsStartAtSessionStartAndCoverTheTimeline() {
    val timeline = mapOf(
      "A" to listOf(snapshot(t0, gobletId), snapshot(t0 + 60_000, gobletId)),
      "B" to listOf(snapshot(t0 + 60_000, 200))
    )

    val cards = cards(timeline, listOf("A", "B"))

    // 60s session → 30s buckets, starting exactly at the session start
    assertEquals(listOf(t0, t0 + 30_000, t0 + 60_000), cards.map { it.timestamp })
  }

  @Test
  fun playerStatusIsEvaluatedAtTheBucketMidpoint() {
    val timeline = mapOf(
      "A" to listOf(snapshot(t0, gobletId), snapshot(t0 + 60_000, gobletId)),
      "B" to listOf(snapshot(t0 + 60_000, 200))
    )

    val cards = cards(timeline, listOf("A", "B"))

    // Midpoints are t0+15s, t0+45s and t0+75s: B only has a snapshot at t0+60s.
    assertEquals(
      listOf(
        mapOf("A" to BuffHistoryPlayerStatus.BUFFED, "B" to BuffHistoryPlayerStatus.NOT_SCANNABLE),
        mapOf("A" to BuffHistoryPlayerStatus.BUFFED, "B" to BuffHistoryPlayerStatus.NOT_SCANNABLE),
        mapOf("A" to BuffHistoryPlayerStatus.BUFFED, "B" to BuffHistoryPlayerStatus.NOT_BUFFED)
      ),
      cards.map { it.playerNameToStatus }
    )
  }

  @Test
  fun dataPointsMatchTheSnapshotCounts() {
    val timeline = mapOf(
      "A" to listOf(snapshot(t0, gobletId), snapshot(t0 + 60_000, gobletId)),
      "B" to listOf(snapshot(t0 + 60_000, 200))
    )

    val points = computeBuffHistoryDataPoints(cards(timeline, listOf("A", "B")))

    assertEquals(3, points.size)
    assertEquals(1, points[0].buffedCount)
    assertEquals(1, points[0].notScannableCount)
    assertEquals(0, points[0].unbuffedCount)
    assertEquals(2, points[0].totalPlayers)

    assertEquals(1, points[2].buffedCount)
    assertEquals(0, points[2].notScannableCount)
    assertEquals(1, points[2].unbuffedCount)
    assertEquals(2, points[2].totalPlayers)
  }

  @Test
  fun unknownSessionStartFallsBackToTheFirstScan() {
    val timeline = mapOf("A" to listOf(snapshot(t0, gobletId)))

    val cards = cards(timeline, listOf("A"), sessionStart = 0L)

    assertEquals(listOf(t0), cards.map { it.timestamp })
  }

  @Test
  fun singleBucketStillProducesTwoGraphPoints() {
    val timeline = mapOf("A" to listOf(snapshot(t0, gobletId)))

    val points = computeBuffHistoryDataPoints(cards(timeline, listOf("A")))

    assertEquals(2, points.size)
    assertEquals(t0, points[0].timestamp)
    assertTrue(points[1].timestamp > points[0].timestamp)
    assertEquals(1, points[1].buffedCount)
  }

  @Test
  fun scannedPlayerWithoutWhitelistedBuffsIsUnbuffedNotMissing() {
    // The addon only transmits whitelisted buffs, so a successful scan of a player with no raid
    // buffs is recorded with an empty set: they are scanned and unbuffed, never "not scannable".
    val timeline = mapOf(
      "A" to listOf(snapshot(t0, gobletId)),
      "B" to listOf(snapshot(t0))
    )

    val points = computeBuffHistoryDataPoints(cards(timeline, listOf("A", "B")))

    assertEquals(1, points[0].buffedCount)
    assertEquals(1, points[0].unbuffedCount)
    assertEquals(0, points[0].notScannableCount)
    assertEquals(2, points[0].totalPlayers)
  }

  private fun rosterFrame(at: Long, vararg names: String) = RaidRosterFrame(at, names.toSet())

  @Test
  fun bucketTotalsFollowTheHistoricalRoster() {
    // A..D at t0, D leaves at t0+60s: early buckets total 4, later buckets total 3 — no ghosts.
    val timeline = mapOf(
      "A" to listOf(snapshot(t0, gobletId), snapshot(t0 + 60_000, gobletId), snapshot(t0 + 120_000, gobletId)),
      "B" to listOf(snapshot(t0, gobletId), snapshot(t0 + 60_000), snapshot(t0 + 120_000, gobletId)),
      "C" to listOf(snapshot(t0 + 120_000, gobletId)),
      "D" to listOf(snapshot(t0, 200))
    )
    val rosters = mapOf(
      0 to listOf(rosterFrame(t0, "A", "B", "C", "D"), rosterFrame(t0 + 60_000, "A", "B", "C"))
    )

    val cards = computeSnapshotCards(
      timeline = timeline,
      sessionStart = t0,
      allPlayerNames = listOf("A", "B", "C"),
      requirements = requirements,
      individualBuffKey = null,
      mode = BuffHistoryMode.ALL_BUFFS,
      rosterTimeline = rosters
    )

    // 120s session → 30s buckets evaluated at +15/+45/+75/+105/+135s.
    assertEquals(listOf(4, 4, 3, 3, 3), cards.map { it.playerNameToStatus.size })
    assertTrue(cards.takeLast(3).all { "D" !in it.playerNameToStatus })
    // B had no whitelisted buffs at t0+60s yet still counts as scanned (unbuffed, not missing).
    assertEquals(BuffHistoryPlayerStatus.NOT_BUFFED, cards[2].playerNameToStatus["B"])
    assertEquals(BuffHistoryPlayerStatus.NOT_SCANNABLE, cards[2].playerNameToStatus["C"])
  }

  @Test
  fun bucketsWithAnEmptyRosterAreSkipped() {
    val timeline = mapOf("A" to listOf(snapshot(t0, gobletId), snapshot(t0 + 120_000, gobletId)))
    val rosters = mapOf(0 to listOf(rosterFrame(t0, "A"), rosterFrame(t0 + 60_000)))

    val cards = computeSnapshotCards(
      timeline = timeline,
      sessionStart = t0,
      allPlayerNames = emptyList(),
      requirements = requirements,
      individualBuffKey = null,
      mode = BuffHistoryMode.ALL_BUFFS,
      rosterTimeline = rosters
    )

    // 30s buckets evaluated at t0+15s … t0+135s; the last three fall after everyone left.
    assertEquals(2, cards.size)
    assertTrue(cards.all { it.playerNameToStatus.size == 1 })
  }
}
