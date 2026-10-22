package com.reoky.raidframer.ui.component.graphs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.helpers.RFColors
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.sample
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.general_no_data_available
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(FlowPreview::class)
@Composable
fun RaidComparisonPieChart(
  title: String,
  icon: String,
  dataFlow: StateFlow<Map<String, Float>>,
  modifier: Modifier = Modifier,
  factionColors: Map<String, Color> = emptyMap(),
  updateDebounceMs: Long = 7000L,
  copyMessage: String? = null,
) {
  // Sample the live pump: damage/heal totals change on nearly every combat
  // event, and each emission re-triggers KoalaPlot's slice animation. NOTE:
  // debounce() is wrong here — it waits for a quiet period that never comes
  // during active combat, so hot flows like damage freeze. sample() emits the
  // latest value on a fixed interval, keeping the chart up-to-date without
  // the constant re-animation.
  var displayedData by remember { mutableStateOf(dataFlow.value) }
  LaunchedEffect(dataFlow) {
    dataFlow.sample(updateDebounceMs).collect { displayedData = it }
  }

  val pieData = displayedData.map { (raidName, value) ->
    PieChartSlice(
      label = raidName,
      value = value,
      color = factionColors[raidName]
    )
  }

  // Hover and copy state for the entire chart area
  val copyInteraction = remember { MutableInteractionSource() }
  val isCopyHovered by copyInteraction.collectIsHoveredAsState()
  var showCopied by remember { mutableStateOf(false) }
  LaunchedEffect(showCopied) {
    if (showCopied) {
      delay(1500)
      showCopied = false
    }
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

    // Wrap the pie chart in a hoverable box when copyMessage is provided
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .then(if (copyMessage != null) Modifier
          .hoverable(copyInteraction)
          .clickable(interactionSource = copyInteraction, indication = null) {
            if (!showCopied) {
              Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(copyMessage), null)
              showCopied = true
            }
          }
        else Modifier),
      contentAlignment = Alignment.Center
    ) {
      if (pieData.isEmpty()) {
        Box(
          modifier = Modifier
            .size(120.dp)
            .background(Color.Transparent),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(8.dp)
              .background(Color.White.copy(alpha = 0.05f), shape = CircleShape)
          )
          Text(
            text = stringResource(Res.string.general_no_data_available),
            color = RFColors.TextSecondary,
            style = MaterialTheme.typography.caption
          )
        }
      } else {
        // pass a smaller chartSize so the total component height is reduced (~250dp total)
        RFPieChart(
          data = pieData,
          modifier = Modifier.fillMaxWidth(),
          chartSize = 120.dp,
          labelMarkerSize = 10.dp,
          labelsSpacerHeight = 8.dp
        )
      }

      // Dim overlay with copy prompt — appears on hover
      if (copyMessage != null && (isCopyHovered || showCopied)) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.7f)
            .background(Color.Black.copy(alpha = 0.6f)),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (showCopied) {
              Text(
                text = "✓ Copied!",
                color = Color(0xFF4CAF50),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            } else {
              Text(
                text = "🗫",
                color = Color.White,
                fontSize = 24.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Copy",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}
