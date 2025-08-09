package com.reoky.raidframer.core.helpers

import java.awt.Toolkit

fun scaleDpForScreenResolution(dp: Float): Float {
  val screenSize = Toolkit.getDefaultToolkit().screenSize
  val userScreenWidth = screenSize.getWidth()
  val userScreenHeight = screenSize.getHeight()

  val baseScreenWidth = 2560.0
  val baseScreenHeight = 1440.0

  val widthScalingFactor = userScreenWidth / baseScreenWidth
  val heightScalingFactor = userScreenHeight / baseScreenHeight

  val scalingFactor = minOf(widthScalingFactor, heightScalingFactor).coerceIn(0.5, 2.0)

  return dp * scalingFactor.toFloat()
}
