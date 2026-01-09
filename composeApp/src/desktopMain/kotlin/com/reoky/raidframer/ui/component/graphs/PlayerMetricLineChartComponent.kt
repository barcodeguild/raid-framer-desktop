package com.reoky.raidframer.ui.component.graphs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.*
import io.github.koalaplot.core.line.AreaBaseline
import io.github.koalaplot.core.line.AreaBaseline.ConstantLine
import io.github.koalaplot.core.line.AreaPlot2
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.graphs_no_recent_data
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.get
import kotlin.compareTo
import kotlin.div
import kotlin.math.max
import kotlin.math.min
import kotlin.text.compareTo
import kotlin.text.get
import kotlin.text.toFloat
import kotlin.times

private data class TimeSample(val timestamp: Long, val valueSum: Long)
private data class Peak(val index: Int, val value: Float) // used for the white line

data class GroupSpec(
  val name: String,
  val filter: (PlayerCard) -> Boolean,
  val color: Color
)

enum class GraphMetricType(val displayName: String) {
  DAMAGE("Damage"),
  HEALING("Healing"),
  CC("Crowd Control")
}

private fun simpleMovingAverage(series: List<DefaultPoint<Float, Float>>, window: Int): List<DefaultPoint<Float, Float>> {
  if (window <= 1 || series.size <= 1) return series
  val half = window / 2
  return series.mapIndexed { idx, pt ->
    val start = max(0, idx - half)
    val end = min(series.lastIndex, idx + half)
    val count = (end - start + 1)
    val sumY = series.subList(start, end + 1).sumOf { it.y.toDouble() }
    DefaultPoint(pt.x, (sumY / count).toFloat())
  }
}

@OptIn(ExperimentalKoalaPlotApi::class, ExperimentalComposeUiApi::class)
@Composable
fun MultiPlayerMetricLineChart(
  metricType: GraphMetricType,
  groups: List<GroupSpec>,
  initialMinutesWindow: Int = 15,
  mode: GameMonitorInteractor.MonitorModes = GameMonitorInteractor.MonitorModes.MONITOR,
  forceSlidingWindow: Boolean = false,
  smoothing: Boolean = false,
  smoothingWindow: Int = 3,
  modifier: Modifier = Modifier
) {
  // clamp groups to 1..3
  val usedGroups = groups.take(3).ifEmpty {
    listOf(GroupSpec("All", { true }, Color(0xFFEF5350)))
  }

  var selectedMinutes by remember(initialMinutesWindow) { mutableStateOf(initialMinutesWindow) }
  var samplesPerGroup by remember { mutableStateOf<List<List<TimeSample>>>(emptyList()) }

  // Update loop
  LaunchedEffect(usedGroups, selectedMinutes, mode, forceSlidingWindow, metricType) {
    while (true) {
      val now = System.currentTimeMillis()
      val groupCards = usedGroups.map { spec ->
        PlayerCacheInteractor.getGroupCards { pc -> spec.filter(pc) }
      }

      val bucketSize = 1000L // 1 second resolution
      val windowStart = now - selectedMinutes * 60_000L
      val nowBucketStart = (now / bucketSize) * bucketSize
      val totalBuckets = selectedMinutes * 60

      // Generate X axis timestamps (descending from now)
      val bucketTimestamps = (totalBuckets - 1 downTo 0).map { i -> nowBucketStart - i * bucketSize }

      val useSliding = forceSlidingWindow || (mode != GameMonitorInteractor.MonitorModes.REPLAY)

      val computedPerGroup = groupCards.map { cards ->
        val buckets = mutableMapOf<Long, Long>()
        cards.forEach { card ->
          // Select events based on metric type
          val events: List<CombatEvent> = when (metricType) {
            GraphMetricType.DAMAGE -> card.recentDamageEvents
            GraphMetricType.HEALING -> card.recentHealEvents
            GraphMetricType.CC -> card.recentDebuffAppliedEvents
          }

          events.forEach { e ->
            if (!useSliding || e.timestamp >= windowStart) {
              val bucketStart = (e.timestamp / bucketSize) * bucketSize

              val amount = when (metricType) {
                GraphMetricType.DAMAGE -> (e as? DamageEvent)?.damage?.toLong() ?: 0L
                GraphMetricType.HEALING -> (e as? HealEvent)?.amount?.toLong() ?: 0L
                GraphMetricType.CC -> 1L // Count
              }

              buckets[bucketStart] = (buckets[bucketStart] ?: 0L) + amount
            }
          }
        }

        // Map back to the generated timestamp list
        bucketTimestamps.map { m -> TimeSample(m, buckets[m] ?: 0L) }
      }

      samplesPerGroup = computedPerGroup
      delay(1000L)
    }
  }

  val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

  Column(modifier = modifier) {
    var isHovered by remember { mutableStateOf(false) }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .background(Color.Transparent)
        .padding(8.dp)
        .pointerMoveFilter(
          onEnter = { isHovered = true; false },
          onExit = { isHovered = false; false }
        ),
      contentAlignment = Alignment.Center
    ) {
      if (samplesPerGroup.isEmpty() || samplesPerGroup.first().isEmpty()) {
        Text(text = stringResource(Res.string.graphs_no_recent_data), color = Color.LightGray)
      } else {
        val timestamps = remember(samplesPerGroup) { samplesPerGroup.first().map { it.timestamp } }
        val count = timestamps.size

        // X axis is 0..count-1
        val minX = 0f
        val maxX = (count - 1).toFloat().coerceAtLeast(1f)

        val rawSeries = remember(samplesPerGroup) {
          samplesPerGroup.map { samples ->
            samples.mapIndexed { idx, s -> DefaultPoint(idx.toFloat(), s.valueSum.toFloat()) }
          }
        }

        val dataSeries = remember(rawSeries, smoothing, smoothingWindow) {
          if (smoothing && smoothingWindow > 1) {
            rawSeries.map { series -> simpleMovingAverage(series, smoothingWindow) }
          } else rawSeries
        }

        val maxY = remember(samplesPerGroup) {
          val maxVal = samplesPerGroup.flatten().maxOfOrNull { it.valueSum } ?: 10L
          max(maxVal.toFloat(), 10f) * 1.1f
        }

        XYGraph<Float, Float>(
          xAxisModel = rememberFloatLinearAxisModel(
            range = minX..maxX,
            minViewExtent = maxX, // Show whole range
            maxViewExtent = maxX,
            minimumMajorTickSpacing = 80.dp, // Tweakable
            minorTickCount = 0
          ),
          yAxisModel = rememberFloatLinearAxisModel(
            range = 0f..maxY,
            minimumMajorTickSpacing = 50.dp,
            minorTickCount = 0
          ),
          horizontalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.2f),
          verticalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.2f),
          xAxisStyle = rememberAxisStyle(color = Color.White, tickPosition = TickPosition.Outside),
          yAxisStyle = rememberAxisStyle(color = Color.White, tickPosition = TickPosition.Outside),
          // FIX: Explicitly mark lambdas as @Composable to resolve overload ambiguity
          xAxisLabels = @Composable { xVal ->
            val idx = xVal.toInt()
            val label = if (idx in 0 until count) timeFmt.format(Date(timestamps[idx])) else ""
            Text(text = label, style = typography.caption, color = Color.LightGray)
          },
          yAxisLabels = @Composable { yVal ->
            val label = if (yVal >= 1000f) "${(yVal / 1000f).toInt()}k" else yVal.toInt().toString()
            Text(text = label, style = typography.caption, color = Color.LightGray)
          }
        ) {
          dataSeries.forEachIndexed { idx, series ->
            val color = usedGroups.getOrNull(idx)?.color ?: Color(0xFFEF5350)
            AreaPlot2<Float, Float>(
              data = series,
              areaStyle = AreaStyle(
                brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)),
                alpha = 1.0f
              ),
              // FIX: Explicitly specifying generic types to help type inference
              areaBaseline = ConstantLine<Float, Float>(0f)
            )
            LinePlot2<Float, Float>(
              data = series,
              lineStyle = LineStyle(
                brush = Brush.linearGradient(listOf(color, color.copy(alpha = 0.85f))),
                strokeWidth = 1.5.dp,
                alpha = 0.95f
              )
            )
            dataSeries.firstOrNull()?.let { firstSeries ->
              findTwoHighestPeaks(firstSeries)?.let { (peak1, peak2) ->
                val midY = maxY / 2f

                if (peak1.value > midY && peak2.value > midY) {
                  // Calculate slope
                  val slope = (peak2.value - peak1.value) / (peak2.index - peak1.index)

                  // Calculate angle in degrees
                  val angleRadians = kotlin.math.atan(slope)
                  val angleDegrees = kotlin.math.abs(Math.toDegrees(angleRadians.toDouble()))

                  // Only render if angle is less than 40 degrees
                  if (angleDegrees < 40.0) {
                    // Extend line to graph boundaries
                    var leftX = 0f
                    var leftY = peak1.value - (peak1.index * slope)

                    var rightX = maxX
                    var rightY = peak1.value + ((maxX - peak1.index) * slope)

                    // Check if extended points go below any curve values
                    // Find leftmost safe point
                    for (i in 0 until peak1.index) {
                      val projectedY = peak1.value - ((peak1.index - i) * slope)
                      if (projectedY <= firstSeries[i].y) {
                        leftX = i.toFloat()
                        leftY = projectedY
                        break
                      }
                    }

                    // Find rightmost safe point
                    for (i in firstSeries.lastIndex downTo peak2.index + 1) {
                      val projectedY = peak2.value + ((i - peak2.index) * slope)
                      if (projectedY <= firstSeries[i].y) {
                        rightX = i.toFloat()
                        rightY = projectedY
                        break
                      }
                    }

                    val trendLine = listOf(
                      DefaultPoint(leftX, leftY),
                      DefaultPoint(rightX, rightY)
                    )

                    LinePlot2<Float, Float>(
                      data = trendLine,
                      lineStyle = LineStyle(
                        brush = SolidColor(Color.White),
                        strokeWidth = 3.dp,
                        alpha = 0.85f
                      )
                    )
                  }
                }
              }
            }
          }
        }

        // Time-frame selection buttons
        val buttonsAlpha by animateFloatAsState(
          targetValue = if (isHovered) 1f else 0f,
          animationSpec = tween(durationMillis = 250)
        )

        Row(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .alpha(buttonsAlpha),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          val options = listOf(1 to "1m", 5 to "5m", 15 to "15m", 60 to "1h")

          options.forEach { (mins, label) ->
            var btnHovered by remember { mutableStateOf(false) }
            val isSelected = mins == selectedMinutes
            val bgAlpha = when {
              isSelected -> 0.9f
              btnHovered -> 0.55f
              else -> 0.35f
            }

            Box(
              modifier = Modifier
                .height(20.dp)
                .width(36.dp)
                .background(
                  if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.8f)
                  else Color.White.copy(alpha = bgAlpha),
                  shape = MaterialTheme.shapes.small
                )
                .pointerMoveFilter(
                  onEnter = { btnHovered = true; false },
                  onExit = { btnHovered = false; false }
                )
                .clickable { selectedMinutes = mins },
              contentAlignment = Alignment.Center
            ) {
              Text(
                label,
                color = if (isSelected) Color.White else Color.Black,
                style = typography.caption
              )
            }
          }
        }
      }
    }
  }
}

private fun findTwoHighestPeaks(series: List<DefaultPoint<Float, Float>>): Pair<Peak, Peak>? {
  if (series.size < 3) return null

  // Find local maxima (peaks) - allow equal values
  val peaks = mutableListOf<Peak>()
  for (i in 1 until series.size - 1) {
    val prev = series[i - 1].y
    val curr = series[i].y
    val next = series[i + 1].y

    if (curr >= prev && curr >= next && curr > 0f) {
      peaks.add(Peak(i, curr))
    }
  }

  // If not enough peaks, try taking the 2 highest values (not necessarily local maxima)
  if (peaks.size < 2) {
    val allPoints = series.mapIndexed { idx, pt -> Peak(idx, pt.y) }
      .filter { it.value > 0f }
      .sortedByDescending { it.value }
      .take(2)

    if (allPoints.size < 2) return null

    return if (allPoints[0].index < allPoints[1].index) {
      Pair(allPoints[0], allPoints[1])
    } else {
      Pair(allPoints[1], allPoints[0])
    }
  }

  // Sort by value descending and take top 2
  val topTwo = peaks.sortedByDescending { it.value }.take(2)

  // Return in chronological order (left to right)
  return if (topTwo[0].index < topTwo[1].index) {
    Pair(topTwo[0], topTwo[1])
  } else {
    Pair(topTwo[1], topTwo[0])
  }
}

