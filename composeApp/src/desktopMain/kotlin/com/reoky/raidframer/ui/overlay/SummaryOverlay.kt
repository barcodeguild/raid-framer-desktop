package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.core.interactor.RealtimeMetricsInteractor
import com.reoky.raidframer.core.mock.mockCandlestickDataFrame
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.graphs.CandlestickChart
import com.reoky.raidframer.ui.graphs.CandlestickColorScheme

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
fun SummaryOverlay(wm: WindowManager? = null, playerName: String = "Reoky") {
  val interactor = remember { RealtimeMetricsInteractor() }
  val realtime = interactor.realtimeComputer

  val candlesState by realtime.candles.collectAsState(initial = mockCandlestickDataFrame())
  val currentCandleState by realtime.current.collectAsState(initial = null as Double?)

  // Semi-transparent backdrop
  Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.60f))) {
    TitleBarComponent(
      title = "$playerName's Summary",
      onClose = { wm?.closeWindow(OverlayType.SUMMARY) }
    )

    // Chart area takes remaining space
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .padding(12.dp)
    ) {
      CandlestickChart(
        data = candlesState,
        colorScheme = CandlestickColorScheme(
          bullish = Color(0xFF4CAF50),
          bearish = Color(0xFFF44336),
          neutral = Color(0xFF9E9E9E),
          background = Color.Transparent,
          grid = Color(0xFF333333),
          axis = Color(0xFFFFFFFF),
          text = Color(0xFFFFFFFF)
        ),
        currentValue = currentCandleState
      )
    }
  }

  interactor.start()
}