package com.reoky.raidframer.ui.component.graphs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.helpers.RFColors
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RaidComparisonPieChart(
  title: String,
  icon: String,
  dataFlow: StateFlow<Map<String, Float>>,
  modifier: Modifier = Modifier
) {
  val data by dataFlow.collectAsState()

  val pieData = data.map { (raidName, value) ->
    PieChartSlice(
      label = raidName,
      value = value
    )
  }

  Column(
    modifier = modifier
      .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium)
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(bottom = 12.dp)
    ) {
      Text(
        text = icon,
        fontFamily = FontsHelper.faSolid(),
        fontSize = 16.sp,
        color = Color.White,
        modifier = Modifier.padding(end = 8.dp)
      )
      Text(
        text = title,
        style = MaterialTheme.typography.h6,
        color = Color.White,
        textAlign = TextAlign.Center
      )
    }

    if (pieData.isEmpty()) {
      Box(
        modifier = Modifier
          .size(200.dp)
          .background(Color.Transparent),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No data available",
          color = RFColors.TextSecondary,
          style = MaterialTheme.typography.caption
        )
      }
    } else {
      RFPieChart(
        data = pieData,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}
