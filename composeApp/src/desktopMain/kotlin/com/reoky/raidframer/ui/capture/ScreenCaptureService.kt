package com.reoky.raidframer.ui.capture

import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage

/** Captures physical screen pixels for the Windows desktop target. */
object ScreenCaptureService {
  fun captureRegion(bounds: Rectangle): BufferedImage {
    require(bounds.width > 0 && bounds.height > 0) { "Capture bounds must be non-empty" }
    return Robot().createScreenCapture(bounds)
  }
}
