package com.reoky.raidframer.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun TimeRangeSlider(
  minTime: Float,
  maxTime: Float,
  startTime: Float,
  endTime: Float,
  activityLevels: List<Float>, // values between 0..1
  onRangeChange: (Float, Float) -> Unit
) {
  val sliderWidth = 400.dp
  val handleRadius = 10.dp
  Box(
    modifier = Modifier.width(sliderWidth).height(60.dp)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val widthPx = size.width
      val heightPx = size.height

      // Draw activity level bar
      val barHeight = heightPx * 0.3f
      activityLevels.forEachIndexed { i, level ->
        val x = i * widthPx / (activityLevels.size - 1)
        drawLine(
          color = Color.Green.copy(alpha = level),
          start = Offset(x, heightPx / 2 - barHeight / 2),
          end = Offset(x, heightPx / 2 + barHeight / 2),
          strokeWidth = 2f
        )
      }

      // Draw time markers (ticks)
      for (i in 0..10) {
        val x = i * widthPx / 10
        drawLine(
          color = Color.Gray,
          start = Offset(x, heightPx / 2 - 12),
          end = Offset(x, heightPx / 2 + 12),
          strokeWidth = 1f
        )
      }

      // Draw slider bar
      drawLine(
        color = Color.LightGray,
        start = Offset(0f, heightPx / 2),
        end = Offset(widthPx, heightPx / 2),
        strokeWidth = 4f
      )

      // Draw handles
      val startX = ((startTime - minTime) / (maxTime - minTime)) * widthPx
      val endX = ((endTime - minTime) / (maxTime - minTime)) * widthPx
      drawCircle(Color.Blue, handleRadius.toPx(), Offset(startX, heightPx / 2))
      drawCircle(Color.Red, handleRadius.toPx(), Offset(endX, heightPx / 2))
    }
    // Add gesture handling for dragging handles (omitted for brevity)
  }
}
