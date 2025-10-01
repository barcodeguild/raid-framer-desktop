package com.reoky.raidframer.ui.graphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class CandlestickDataFrame(
  val xValues: List<String>,
  val opens: List<Double>,
  val highs: List<Double>,
  val lows: List<Double>,
  val closes: List<Double>
)

data class CandlestickColorScheme(
  val bullish: Color,
  val bearish: Color,
  val neutral: Color,
  val background: Color,
  val grid: Color,
  val axis: Color,
  val text: Color
)

data class GraphMeta(
  val title: String,
  val description: String,
  val xAxisLabel: String,
  val yAxisLabel: String,
  val colorScheme: List<Color>,
  val data: CandlestickDataFrame
)

@Composable
fun CandlestickChart(
  title: String = "Default Title",
  xAxisLabel: String = "Time",
  yAxisLabel: String = "Damage Per Second",
  data: CandlestickDataFrame,
  colorScheme: CandlestickColorScheme,
  modifier: Modifier = Modifier
    .fillMaxWidth()
    .height(300.dp)
) {
  val textMeasurer = rememberTextMeasurer()

  val candleCount = data.xValues.size
  val maxY = data.highs.maxOrNull() ?: 1.0
  val minY = data.lows.minOrNull() ?: 0.0
  val yRange = maxY - minY

  var scale by remember { mutableStateOf(3.5f) }
  var scrollXOffset by remember { mutableStateOf(0f) }
  var scrollYOffset by remember { mutableStateOf(0f) }
  var dragOffset by remember { mutableStateOf(Offset.Zero) }
  var isDragging by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
          scale = (scale * zoom).coerceIn(1f, 10f)

          // Horizontal scrolling
          scrollXOffset = (scrollXOffset - pan.x / scale)
            .coerceIn(0f, ((candleCount - candleCount / scale).coerceAtLeast(0f)).toFloat())

          // Vertical scrolling (we allow overscroll vertically)
          val labelPaddingTop = 40f
          val labelPaddingBottom = 40f
          val chartHeight = size.height - labelPaddingTop - labelPaddingBottom

          val pixelsPerUnit = chartHeight / yRange
          scrollYOffset += (pan.y / pixelsPerUnit).toFloat()

          val visibleYRange = yRange / scale
          val maxYOffset = (yRange - visibleYRange).coerceAtLeast(0.0)
          scrollYOffset = scrollYOffset.coerceIn(0.0f, maxYOffset.toFloat())
        }

        // Mouse wheel zoom (for Windows users)
        detectTapGestures(
          onPress = { offset ->
            // Track drag start position
            dragOffset = offset
            isDragging = true
          }
        )
      }
      .pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            val event = awaitPointerEvent()

            // Scroll wheel zoom
            event.changes.firstOrNull()?.scrollDelta?.let { scroll ->
              val delta = scroll.y
              if (delta != 0f) {
                scale = (scale * (1f - delta * 0.1f)).coerceIn(1f, 10f)
              }
            }
          }
        }
      }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val visibleCandles = (candleCount / scale).roundToInt().coerceAtLeast(1)
      val startIndex = scrollXOffset.roundToInt().coerceIn(0, candleCount - visibleCandles)
      val endIndex = (startIndex + visibleCandles).coerceAtMost(candleCount)

      val candleWidth = size.width / (visibleCandles * 2)
      val candleSpacing = size.width / visibleCandles


      // title text
      drawText(
        textMeasurer = textMeasurer,
        text = title,
        style = TextStyle(
          color = colorScheme.text,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        ),
        topLeft = Offset(size.width / 2 - 60f, 8f) // top padding
      )

      // axis labels
      drawText(
        textMeasurer = textMeasurer,
        text = xAxisLabel,
        style = TextStyle(
          color = colorScheme.text,
          fontSize = 14.sp
        ),
        topLeft = Offset(size.width / 2 - 40f, size.height - 32f)
      )

      withTransform({
        rotate(-90f, pivot = Offset(24f, size.height / 2))
      }) {
        drawText(
          textMeasurer = textMeasurer,
          text = yAxisLabel,
          style = TextStyle(
            color = colorScheme.text,
            fontSize = 14.sp
          ),
          topLeft = Offset(24f, size.height / 2)
        )
      }

      // Draw grid lines
      for (i in 0..4) {
        val y = size.height * i / 4
        drawLine(
          color = colorScheme.grid,
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1f
        )
      }

      // Draw axes
      drawLine(
        color = colorScheme.axis,
        start = Offset(0f, 0f),
        end = Offset(0f, size.height),
        strokeWidth = 2f
      )
      drawLine(
        color = colorScheme.axis,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = 2f
      )

      // Draw candlesticks
      for (i in startIndex until endIndex) {
        val open = data.opens[i]
        val close = data.closes[i]
        val high = data.highs[i]
        val low = data.lows[i]

        val labelPaddingTop = 40f
        val labelPaddingBottom = 40f
        val chartHeight = size.height - labelPaddingTop - labelPaddingBottom

        val x = candleSpacing * (i - startIndex) + candleSpacing / 2

        val yOpen = size.height - labelPaddingBottom - (((open - minY - scrollYOffset) / yRange) * chartHeight).toFloat()
        val yClose = size.height - labelPaddingBottom - (((close - minY - scrollYOffset) / yRange) * chartHeight).toFloat()
        val yHigh = size.height - labelPaddingBottom - (((high - minY - scrollYOffset) / yRange) * chartHeight).toFloat()
        val yLow = size.height - labelPaddingBottom - (((low - minY - scrollYOffset) / yRange) * chartHeight).toFloat()

        val candleColor = when {
          close > open -> colorScheme.bullish
          close < open -> colorScheme.bearish
          else -> colorScheme.neutral
        }

        drawLine(
          color = candleColor,
          start = Offset(x, yHigh),
          end = Offset(x, yLow),
          strokeWidth = 2f
        )
        drawRect(
          color = candleColor,
          topLeft = Offset(x - candleWidth / 2, minOf(yOpen, yClose)),
          size = androidx.compose.ui.geometry.Size(
            candleWidth,
            kotlin.math.abs(yOpen - yClose)
          )
        )
      } // end candlestick drawing loop
    }
  }

}
