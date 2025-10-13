package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.reoky.raidframer.core.mock.mockCandlestickDataFrame
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
    RaidOverlay(listOf())
  }
}

@Composable
fun SummaryOverlay() {
  CandlestickChart(
    data = mockCandlestickDataFrame(),
    colorScheme = CandlestickColorScheme(
      bullish = Color(0xFF4CAF50),
      bearish = Color(0xFFF44336),
      neutral = Color(0xFF9E9E9E),
      background = Color.Transparent,
      grid = Color(0xFF333333),
      axis = Color(0xFFFFFFFF),
      text = Color(0xFFFFFFFF)
    )
  )
}
