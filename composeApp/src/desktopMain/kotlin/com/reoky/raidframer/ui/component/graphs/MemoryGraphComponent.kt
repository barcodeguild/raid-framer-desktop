package com.reoky.raidframer.ui.component.graphs

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.koalaplot.core.line.AreaBaseline
import io.github.koalaplot.core.line.AreaPlot2
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.xygraph.TickPosition
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import kotlinx.coroutines.delay
import kotlin.math.max

private const val SAMPLE_COUNT = 60
private const val SAMPLE_INTERVAL_MS = 2000L

/**
 * Returns the JVM heap usage in MB (totalMemory - freeMemory).
 * This is the most reliable cross-platform memory metric.
 */
private fun getHeapUsedMB(): Int {
  val runtime = Runtime.getRuntime()
  return ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()
}

/**
 * A compact memory usage graph that samples JVM heap every 2 seconds
 * and displays the last 60 samples (2 minutes) as an area chart.
 * Current and peak values are overlaid as faint centered text.
 *
 * @param modifier The modifier to apply to the outer container.
 * @param sampleCount The number of data points to keep (default 60 = 2 minutes at 2s intervals).
 */
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun MemoryGraphComponent(
  modifier: Modifier = Modifier,
  sampleCount: Int = SAMPLE_COUNT
) {
  var samples by remember { mutableStateOf(IntArray(sampleCount) { 0 }) }

  LaunchedEffect(sampleCount) {
    while (true) {
      val memMB = getHeapUsedMB()
      val newSamples = IntArray(sampleCount)
      System.arraycopy(samples, 1, newSamples, 0, sampleCount - 1)
      newSamples[sampleCount - 1] = memMB
      samples = newSamples
      delay(SAMPLE_INTERVAL_MS)
    }
  }

  val currentMB = getHeapUsedMB()
  val peakMB = samples.max()

  val memColor = when {
    currentMB < 1024 -> Color(0xFF4CAF50) // green
    currentMB < 2048 -> Color(0xFFFFC107) // yellow
    currentMB < 3072 -> Color(0xFFFF9800) // orange
    else -> Color(0xFFF44336)              // red
  }

  val series = remember(samples) {
    List(sampleCount) { i -> DefaultPoint(i.toFloat(), samples[i].toFloat()) }
  }

  val maxY = remember(peakMB, currentMB) {
    val ceiling = max(peakMB, currentMB) * 1.15f
    max(ceiling, 100f) // minimum 100MB range
  }

  // The chart with centered overlay text
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(100.dp)
  ) {
    XYGraph<Float, Float>(
      xAxisModel = rememberFloatLinearAxisModel(
        range = 0f..(sampleCount - 1).toFloat(),
        minViewExtent = (sampleCount - 1).toFloat() * 0.3f,
        maxViewExtent = (sampleCount - 1).toFloat(),
        minimumMajorTickIncrement = (sampleCount - 1).toFloat() * 0.25f,
        minimumMajorTickSpacing = 40.dp,
        minorTickCount = 0
      ),
      yAxisModel = rememberFloatLinearAxisModel(
        range = 0f..maxY,
        minViewExtent = (maxY - 0f) * 0.3f,
        maxViewExtent = maxY,
        minimumMajorTickIncrement = maxY * 0.25f,
        minimumMajorTickSpacing = 30.dp,
        minorTickCount = 0
      ),
      horizontalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.0f),
      verticalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.10f),
      horizontalMinorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0f),
      verticalMinorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0f),
      xAxisStyle = rememberAxisStyle(color = Color.White, tickPosition = TickPosition.None, lineWidth = 0.5.dp),
      yAxisStyle = rememberAxisStyle(
        color = Color.White, majorTickSize = 3.dp, minorTickSize = 0.dp,
        tickPosition = TickPosition.Outside, lineWidth = 0.5.dp, labelRotation = 0
      ),
      xAxisLabels = { _: Float -> "" },
      yAxisLabels = @Composable { yVal: Float ->
        val l = if (yVal >= 1000f) "${(yVal / 1000f).toInt()}k" else yVal.toInt().toString()
        Text(l, style = MaterialTheme.typography.caption, color = Color.LightGray)
      }
    ) {
      AreaPlot2(
        data = series,
        areaStyle = AreaStyle(
          brush = Brush.verticalGradient(listOf(memColor.copy(alpha = 0.4f), Color.Transparent)),
          alpha = 1.0f
        ),
        areaBaseline = AreaBaseline.ConstantLine(0f)
      )

      LinePlot2(
        data = series,
        lineStyle = LineStyle(
          brush = Brush.linearGradient(listOf(memColor, memColor.copy(alpha = 0.85f))),
          strokeWidth = 1.2.dp,
          alpha = 0.95f
        )
      )
    }

    // Centered overlay text — faint, matching Mini Overlay style
    Column(
      modifier = Modifier.align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "${currentMB}MB",
        color = Color.White.copy(alpha = 0.45f),
        fontSize = 18.sp,
        fontWeight = FontWeight.Light
      )
      if (peakMB > 0) {
        Text(
          text = "peak ${peakMB}MB",
          color = Color.White.copy(alpha = 0.30f),
          fontSize = 10.sp,
          fontWeight = FontWeight.Light
        )
      }
    }
  }
}
