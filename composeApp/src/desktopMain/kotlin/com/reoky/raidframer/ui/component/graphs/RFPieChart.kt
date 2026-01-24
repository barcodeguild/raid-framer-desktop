package com.reoky.raidframer.ui.component.graphs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.core.helpers.RFGraphColor
import com.reoky.raidframer.core.helpers.pickNextColor
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi

data class PieChartSlice(
  val label: String,
  val value: Float,
  val color: Color? = null
)

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun RFPieChart(
  data: List<PieChartSlice>,
  modifier: Modifier = Modifier,
  showLabels: Boolean = true
) {
  val usedColors = remember(data) {
    val colors = mutableSetOf<RFGraphColor>()
    data.map { slice ->
      if (slice.color == null) {
        val nextColor = pickNextColor(colors)
        colors.add(nextColor)
        nextColor.color
      } else {
        slice.color
      }
    }
  }

  val values = remember(data) { data.map { it.value } }

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(200.dp)
        .background(Color.Transparent)
        .graphicsLayer(alpha = 0.99f) // Forces hardware acceleration
    ) {
      PieChart(
        values = values,
        modifier = Modifier.fillMaxSize(),
        slice = { index ->
          DefaultSlice(usedColors[index])
        }
      )
    }

    if (showLabels) {
      Spacer(modifier = Modifier.height(16.dp))

      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
      ) {
        data.forEachIndexed { index, slice ->
          val color = usedColors[index]
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(12.dp)
                .background(color, shape = MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "${slice.label}: ${slice.value.toInt()}",
              style = MaterialTheme.typography.body2,
              color = Color.White,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }
  }
}
