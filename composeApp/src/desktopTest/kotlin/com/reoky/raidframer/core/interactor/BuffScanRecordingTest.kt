package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.serialization.BuffPayload
import com.reoky.raidframer.core.serialization.RaidFramePayload
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Buff History graph's "not scannable" band is driven by which roster frames get recorded into
 * the buff timeline. The Lua addon only transmits buffs on INTERESTING_BUFF_IDS, so a *successful*
 * scan of a player who happens to have no raid buffs arrives as an empty list — that frame must
 * still count as scanned, otherwise those players sit in the grey band for the whole session.
 */
class BuffScanRecordingTest {

  @Test
  fun inspectedPlayerWithNoWhitelistedBuffsCountsAsScanned() {
    val inspectedWithoutInterestingBuffs = RaidFramePayload(
      playerName = "Aelith",
      buffScanTimestamp = 1_760_000_000L,
      buffCount = 9 // UnitBuffCount answered → the unit was inspected
    )

    assertTrue(inspectedWithoutInterestingBuffs.hasConfirmedBuffScan())
  }

  @Test
  fun playerWithWhitelistedBuffsCountsAsScanned() {
    val withBuffs = RaidFramePayload(
      playerName = "Brynn",
      buffScanTimestamp = 1_760_000_000L,
      buffCount = 4,
      buffs = listOf(BuffPayload(buff_id = 30766))
    )

    assertTrue(withBuffs.hasConfirmedBuffScan())
  }

  @Test
  fun emptyReadWithoutBuffCountStaysNotScannable() {
    val neverScanned = RaidFramePayload(playerName = "Cassian")
    val failedOrOutOfRangeRead = RaidFramePayload(
      playerName = "Dorian",
      buffScanTimestamp = 1_760_000_000L, // the scan pass ran…
      buffCount = 0 // …but UnitBuffCount returned nothing
    )

    assertFalse(neverScanned.hasConfirmedBuffScan())
    assertFalse(failedOrOutOfRangeRead.hasConfirmedBuffScan())
  }
}
