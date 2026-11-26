package com.reoky.raidframer.ui

import java.awt.Point
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class OverlayPlacementTest {
  private val areas = listOf(Rectangle(0, 32, 1536, 832), Rectangle(-1280, -200, 1280, 960))

  @Test fun unreachableTitleReturnsToPrimary() {
    for (point in listOf(Point(9, 1031), Point(1600, 300), Point(50, -300))) {
      assertEquals(Point(0, 32), reachablePosition(Rectangle(point.x, point.y, 738, 204), areas))
    }
  }

  @Test fun reachableSecondaryPositionIsPreserved() {
    assertEquals(Point(-1200, -150), reachablePosition(Rectangle(-1200, -150, 738, 204), areas))
  }
}