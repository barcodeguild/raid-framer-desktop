package com.reoky.raidframer.ui.capture

import androidx.compose.ui.awt.ComposeWindow
import java.awt.Rectangle
import java.awt.image.BufferedImage

/** Captures a Compose window's visible desktop pixels without capturing the surrounding game. */
object ComposeWindowCaptureService {
  fun capture(window: ComposeWindow): BufferedImage? {
    val bounds = window.bounds
    if (bounds.width <= 0 || bounds.height <= 0 || !window.isShowing) return null
    return ScreenCaptureService.captureRegion(Rectangle(bounds.x, bounds.y, bounds.width, bounds.height))
  }
}
