package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.reoky.raidframer.ui.graphs.CandlestickChart
import com.reoky.raidframer.ui.graphs.CandlestickColorScheme
import com.reoky.raidframer.ui.graphs.CandlestickDataFrame

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
    data = CandlestickDataFrame(
      xValues = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct"),
      opens = listOf(14.2, 6.7, 8.8, 11.2, 4.0, 8.5, 7.5, 3.6, 5.4, 6.7),
      highs = listOf(15.5, 9.6, 10.7, 11.7, 9.9, 10.5, 8.5, 6.1, 8.5, 10.7),
      lows = listOf(7.5, 6.1, 8.5, 5.4, 4.0, 6.7, 3.6, 3.5, 5.0, 6.0),
      closes = listOf(8.0, 8.6, 10.7, 6.5, 9.8, 7.5, 6.1, 5.4, 7.5, 9.6)
    ),
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
