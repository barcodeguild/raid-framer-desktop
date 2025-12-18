package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.GroupSpec
import com.reoky.raidframer.ui.component.graphs.MultiPlayerMetricLineChart
import com.reoky.raidframer.ui.component.graphs.PlayerMetricMiniLineGraphComponent
import kotlin.String

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
fun SummaryOverlay(wm: WindowManager? = null, playerName: String = "") {

  // Semi-transparent backdrop
  Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.60f))) {
    TitleBarComponent(
      title = "",
      onClose = { wm?.closeWindow(OverlayType.SUMMARY) }
    )

    // Place the Koala-powered per-player damage chart
    MultiPlayerMetricLineChart(
      groups = listOf(
        GroupSpec(
          "",
          filter = { it.name == "" },
          color = Color.Magenta
        ),
        GroupSpec(
          "",
          filter = { it.name == "" },
          color = Color.Green
        ),
        GroupSpec(
          "",
          filter = { it.name == "" },
          color = Color.Cyan
        ),
      ),
      smoothing = false,
      smoothingWindow = 1,
      minutesWindow = 15,
      mode = GameMonitorInteractor.currentMode,
      minXAxisLabels = 2,
      maxXAxisLabels = 8,
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
    )
  }
}