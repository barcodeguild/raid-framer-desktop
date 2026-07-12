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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.GraphDataInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.*
import com.reoky.raidframer.ui.LocalDragLock
import io.github.koalaplot.core.line.AreaBaseline.ConstantLine
import io.github.koalaplot.core.line.AreaPlot2
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.graphs_no_recent_data
import kotlin.math.max
import kotlin.math.min

private data class TimeSample(val timestamp: Long, val valueSum: Long)
private data class Peak(val index: Int, val value: Float)
private data class PeakPair(val left: Peak, val right: Peak)

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

private fun formatLabel(seconds: Long): String {
  return when {
    seconds < 0 -> ""
    seconds == 0L -> "0"
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> {
      val m = seconds / 60
      val s = seconds % 60
      if (s > 0) "${m}m${s}s" else "${m}m"
    }
    else -> {
      val h = seconds / 3600
      val m = (seconds % 3600) / 60
      if (m > 0) "${h}h${m}m" else "${h}h"
    }
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
  val usedGroups = remember(groups) {
    groups.take(3).ifEmpty {
      listOf(GroupSpec("All", { true }, Color(0xFFEF5350)))
    }
  }

  val stableGroupKey = remember(usedGroups.map { Pair(it.name, it.color) }) {
    usedGroups.map { Pair(it.name, it.color) }
  }

  var selectedMinutes by remember(initialMinutesWindow) { mutableStateOf(initialMinutesWindow) }
  var samplesPerGroup by remember { mutableStateOf<List<List<TimeSample>>>(emptyList()) }
  var viewportOffset by remember { mutableStateOf(Float.POSITIVE_INFINITY) }
  var isDragging by remember { mutableStateOf(false) }
  var dragSessionId by remember { mutableStateOf(0L) }
  val dragLock = LocalDragLock.current

  val displayBucketSizeMs = remember(selectedMinutes) {
    when {
      selectedMinutes <= 1 -> 1000L
      selectedMinutes <= 5 -> 2000L
      selectedMinutes <= 15 -> 5000L
      else -> 30000L
    }
  }

  val historyMinutes = remember(selectedMinutes, displayBucketSizeMs) {
    val viewportDataPoints = selectedMinutes * 60L * 1000L / displayBucketSizeMs
    val historyDataPoints = (viewportDataPoints * 1.5).toLong()
    val minutes = (historyDataPoints * displayBucketSizeMs / 60_000.0).toInt()
    max(minutes, selectedMinutes + 10)
  }

  val labelIncrementSec = remember(selectedMinutes) {
    when {
      selectedMinutes <= 1 -> 10L
      selectedMinutes <= 5 -> 30L
      selectedMinutes <= 15 -> 60L
      else -> 300L
    }
  }

  val tickIncrementIndices = remember(labelIncrementSec, displayBucketSizeMs) {
    max(1f, (labelIncrementSec * 1000L / displayBucketSizeMs).toFloat())
  }

  val latestGraphStartX = remember(samplesPerGroup, selectedMinutes, displayBucketSizeMs) {
    val sampleCount = samplesPerGroup.firstOrNull()?.size ?: 0
    val maxX = (sampleCount - 1).toFloat().coerceAtLeast(1f)
    val viewportWidth = (selectedMinutes * 60L * 1000L / displayBucketSizeMs)
      .toFloat()
      .coerceAtLeast(1f)
      .coerceAtMost(maxX)
    (maxX - viewportWidth).coerceAtLeast(0f)
  }

  // The polling coroutine must always observe the latest interaction state.
  // Otherwise, it can apply an old live-edge fetch after dragging starts.
  val currentIsDragging by rememberUpdatedState(isDragging)
  val currentViewportOffset by rememberUpdatedState(viewportOffset)
  val currentDragSessionId by rememberUpdatedState(dragSessionId)
  val currentLatestGraphStartX by rememberUpdatedState(latestGraphStartX)

  LaunchedEffect(stableGroupKey, selectedMinutes, mode, forceSlidingWindow, metricType) {
    val bucketSize = displayBucketSizeMs
    while (true) {
      val fetchStartedAtLatest =
        !currentIsDragging &&
            (!currentViewportOffset.isFinite() ||
                currentViewportOffset >= currentLatestGraphStartX - 1f)
      if (fetchStartedAtLatest) {
        val fetchDragSessionId = currentDragSessionId
        val computedPerGroup = withContext(Dispatchers.Default) {
          val now = System.currentTimeMillis()
          val groupCards = usedGroups.map { spec ->
            PlayerCacheInteractor.getGroupCards { pc -> spec.filter(pc) }
          }
          val windowStart = now - historyMinutes * 60_000L
          val nowBucketStart = (now / bucketSize) * bucketSize
          val totalBuckets = (historyMinutes * 60_000L) / bucketSize
          val bucketTimestamps = (totalBuckets - 1 downTo 0).map { i -> nowBucketStart - i * bucketSize }

          groupCards.map { cards ->
            val buckets = mutableMapOf<Long, Long>()
            cards.forEach { card ->
              val rawGraphData = GraphDataInteractor.getAggregatedData(
                playerName = card.name,
                metricType = metricType,
                startTimeMs = windowStart,
                endTimeMs = now
              )
              for ((ts, value) in rawGraphData) {
                val aggTs = (ts / bucketSize) * bucketSize
                buckets[aggTs] = (buckets[aggTs] ?: 0L) + value
              }
            }
            bucketTimestamps.map { m -> TimeSample(m, buckets[m] ?: 0L) }
          }
        }
        // Do not apply a fetch that started before the user began dragging.
        // Applying it can replace the current viewport and make the graph jump.
        if (!currentIsDragging && currentDragSessionId == fetchDragSessionId) {
          // Only reset to the live edge if the viewport is still there when
          // the fetch completes. A drag may have started while the fetch ran.
          val stillAtLatest =
            !currentViewportOffset.isFinite() ||
                currentViewportOffset >= currentLatestGraphStartX - 1f

          if (fetchStartedAtLatest && stillAtLatest) {
            viewportOffset = Float.POSITIVE_INFINITY
          }
          samplesPerGroup = computedPerGroup
        }
      }
      delay(1000L)
    }
  }

  LaunchedEffect(selectedMinutes) {
    viewportOffset = Float.POSITIVE_INFINITY
  }

  val sessionStart by remember { derivedStateOf { RFConfig.state.value.lastSessionStart } }

  Column(modifier = modifier) {
    var isHovered by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
      dragLock.value = isHovered
    }

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

        val viewportWidth = (selectedMinutes * 60L * 1000L / displayBucketSizeMs).toFloat().coerceAtLeast(1f)
          .coerceAtMost(maxX)
        val latestStartX = (maxX - viewportWidth).coerceAtLeast(0f)
        val visibleStartX = remember(viewportOffset, maxX, viewportWidth) {
          (if (viewportOffset.isFinite()) viewportOffset else latestStartX)
            .coerceIn(0f, latestStartX)
        }
        val currentVisibleStartX by rememberUpdatedState(visibleStartX)
        val currentLatestStartX by rememberUpdatedState(latestStartX)

        val trendLines: List<List<DefaultPoint<Float, Float>>?> = remember(dataSeries, maxY) {
          dataSeries.map { series: List<DefaultPoint<Float, Float>> ->
            createTrendLine(series, maxY)
          }
        }
        val visibleEndX = remember(visibleStartX, maxX, viewportWidth) {
          (visibleStartX + viewportWidth).coerceAtMost(maxX)
        }

        val visibleDataSeries = remember(dataSeries, visibleStartX, visibleEndX) {
          dataSeries.map { series ->
            val startIdx = (visibleStartX.toInt() - 1).coerceAtLeast(0)
            val endIdx = (visibleEndX.toInt() + 2).coerceAtMost(series.size)
            series.subList(startIdx, endIdx)
          }
        }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .pointerInput(maxX, viewportWidth) {
              awaitPointerEventScope {
                var dragging = false
                var lastX = 0f
                var dragStartOffset = 0f
                while (true) {
                  val event = awaitPointerEvent()
                  when (event.type) {
                    PointerEventType.Press -> {
                      dragging = true
                      dragSessionId++
                      isDragging = true
                      lastX = event.changes.first().position.x
                      dragStartOffset = currentVisibleStartX
                      dragLock.value = true
                    }
                    PointerEventType.Release -> {
                      dragging = false
                      isDragging = false

                    }
                    PointerEventType.Move -> {
                      if (dragging && event.changes.any { it.pressed }) {
                        val currentX = event.changes.first().position.x
                        val deltaX = currentX - lastX
                        lastX = currentX
                        if (size.width > 0 && viewportWidth > 0f) {
                          val pixelsPerUnit = size.width / viewportWidth
                          dragStartOffset = (dragStartOffset - deltaX / pixelsPerUnit)
                            .coerceIn(0f, currentLatestStartX)
                          viewportOffset = dragStartOffset
                        }
                      }
                    }
                  }
                }
              }
            }
        ) {
          XYGraph<Float, Float>(
            xAxisModel = rememberFloatLinearAxisModel(
              range = visibleStartX..visibleEndX,
              minViewExtent = (visibleEndX - visibleStartX),
              maxViewExtent = (visibleEndX - visibleStartX),
              minimumMajorTickIncrement = tickIncrementIndices,
              minimumMajorTickSpacing = 60.dp,
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
          xAxisLabels = @Composable { xVal ->
            val idx = xVal.toInt()
            val label = if (idx in 0 until count && sessionStart > 0L) {
              val relSecs = (timestamps[idx] - sessionStart) / 1000L
              if (relSecs <= 0) "" else formatLabel((relSecs / labelIncrementSec) * labelIncrementSec)
            } else ""
            Text(text = label, style = typography.caption, color = Color.LightGray)
          },
          yAxisLabels = @Composable { yVal ->
            val label = if (yVal >= 1000f) "${(yVal / 1000f).toInt()}k" else yVal.toInt().toString()
            Text(text = label, style = typography.caption, color = Color.LightGray)
          }
        ) {
          visibleDataSeries.forEachIndexed { idx, series ->
            val color = usedGroups.getOrNull(idx)?.color ?: Color(0xFFEF5350)
            AreaPlot2<Float, Float>(
              data = series,
              areaStyle = AreaStyle(
                brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)),
                alpha = 1.0f
              ),
              areaBaseline = ConstantLine(0f)
            )
            LinePlot2<Float, Float>(
              data = series,
              lineStyle = LineStyle(
                brush = Brush.linearGradient(listOf(color, color.copy(alpha = 0.85f))),
                strokeWidth = 1.5.dp,
                alpha = 0.95f
              )
            )
             trendLines.getOrNull(idx)?.let { trendLine ->
                LinePlot2<Float, Float>(
                  data = trendLine,
                  lineStyle = LineStyle(
                    brush = SolidColor(Color.White),
                    strokeWidth = 3.dp,
                    alpha = 0.90f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                  )
                )
              }
          }
        }

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

}

private fun findTwoHighestPeaks(series: List<DefaultPoint<Float, Float>>): PeakPair? {
  if (series.size < 3) return null

  val peaks = series.mapIndexedNotNull { index, point ->
    if (index == 0 || index == series.lastIndex) return@mapIndexedNotNull null
    val previous = series[index - 1].y
    val next = series[index + 1].y
    if (point.y > 0f && point.y > previous && point.y > next) {
      Peak(index, point.y)
    } else null
  }

  val first = peaks.maxByOrNull { it.value } ?: return null
  val second = peaks
    .asSequence()
    .filter { kotlin.math.abs(it.index - first.index) >= 3 }
    .maxByOrNull { it.value }
    ?: return null

  return if (first.index < second.index) {
    PeakPair(first, second)
  } else {
    PeakPair(second, first)
  }
}

private fun createTrendLine(
  series: List<DefaultPoint<Float, Float>>,
  maxY: Float
): List<DefaultPoint<Float, Float>>? {
  val peaks = findTwoHighestPeaks(series) ?: return null
  val peak1 = peaks.left
  val peak2 = peaks.right
  val thresholdY = maxY / 3f
  if (peak1.value <= thresholdY || peak2.value <= thresholdY) return null

  val slope = (peak2.value - peak1.value) / (peak2.index - peak1.index)
  val angleDegrees = kotlin.math.abs(
    Math.toDegrees(kotlin.math.atan(slope).toDouble())
  )
  if (angleDegrees >= 40.0) return null

  var leftX = 0f
  var leftY = peak1.value - peak1.index * slope
  var rightX = series.lastIndex.toFloat()
  var rightY = peak1.value + (series.lastIndex - peak1.index) * slope

  for (i in 0 until peak1.index) {
    val projectedY = peak1.value - (peak1.index - i) * slope
    if (projectedY <= series[i].y) {
      leftX = i.toFloat()
      leftY = projectedY
      break
    }
  }

  for (i in series.lastIndex downTo peak2.index + 1) {
    val projectedY = peak2.value + (i - peak2.index) * slope
    if (projectedY <= series[i].y) {
      rightX = i.toFloat()
      rightY = projectedY
      break
    }
  }

  return listOf(
    DefaultPoint(leftX, leftY),
    DefaultPoint(rightX, rightY)
  )
}
