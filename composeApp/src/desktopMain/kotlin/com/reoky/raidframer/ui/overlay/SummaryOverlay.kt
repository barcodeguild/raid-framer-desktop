package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.GroupSpec
import com.reoky.raidframer.ui.component.graphs.MultiPlayerMetricLineChart
import com.reoky.raidframer.core.helpers.pickNextColor
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.graphs_trend_graph
import kotlin.collections.setOf

@Preview
@Composable
fun PreviewSummaryOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    SummaryOverlay()
  }
}

@Composable
fun SummaryOverlay(wm: WindowManager? = null) {

  val currentPlayer by AppState.selectedPlayer.collectAsState()
  val metricType by AppState.selectedMetricType.collectAsState()

  // day amd month for title from system date
  val currentDateString = java.time.LocalDate.now().let {
    "${it.monthValue}/${it.dayOfMonth}"
  }
  val defaultColor = pickNextColor(setOf())

  // Semi-transparent backdrop
  Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.60f))) {
    TitleBarComponent(
      title = "$currentPlayer (${defaultColor.name.lowercase().capitalize(Locale.current)}) - ${metricType.displayName} ${stringResource(Res.string.graphs_trend_graph)} (${currentDateString})",
      onClose = { wm?.closeWindow(OverlayType.SUMMARY) }
    )

    currentPlayer?.let { playerName ->
      // Place the Koala-powered per-player damage chart
      MultiPlayerMetricLineChart(
        groups = listOf(
          GroupSpec(
            name = playerName,
            filter = { it.name == playerName },
            color = defaultColor.color
          )
        ),
        metricType = metricType,
        smoothing = false,
        smoothingWindow = 1,
        mode = GameMonitorInteractor.currentMode,
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
      )
    }
  }
}
