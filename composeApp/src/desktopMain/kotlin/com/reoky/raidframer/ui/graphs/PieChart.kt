package com.reoky.raidframer.ui.graphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class PieChartData(
  val value: Float,
  val color: Color
)

@Composable
fun PieChart(
  data: List<PieChartData>,
  modifier: Modifier = Modifier.size(200.dp)
) {
  val total = data.sumOf { it.value.toDouble() }.toFloat()
  val angles = remember(data) {
    data.map { 360f * it.value / total }
  }

  Box(
    modifier = modifier.background(Color.Transparent),
  ) {
    Canvas(modifier = Modifier.width(200.dp).height(200.dp)) {
      var startAngle = -90f

      data.zip(angles).forEachIndexed { index, (slice, angle) ->
        drawArc(
          color = slice.color,
          startAngle = startAngle + (angle * 0f), // adjust this value to control the amount of explosion
          sweepAngle = angle,
          useCenter = true
        )

        if (index < data.lastIndex) {
          // add a small gap between slices
          drawLine(
            color = Color.Black, // set the line color as you want
            start = Offset(x = startAngle + angle * 10f - 20f, y = startAngle + angle * 10f - 20f), // calculate the starting point of the gap
            end = Offset(x = startAngle + angle * 10f, y =  startAngle + angle * 10f),
            strokeWidth = 2f // adjust this value to control the thickness of the gap
          )
        }

        startAngle += angle
      }
    }
  }
}