package com.reoky.raidframer.ui.component.graphs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.model.PlayerCard
import io.github.koalaplot.core.line.AreaBaseline
import io.github.koalaplot.core.line.AreaPlot2
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.AreaStyle
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// small data holder unchanged
private data class MinuteSample(val minuteStartMillis: Long, val damageSum: Long)

// Group descriptor - filter is typed as PlayerCard -> Boolean
data class GroupSpec(
  val name: String,
  val filter: (PlayerCard) -> Boolean,
  val color: Color
)

/*
 * ArcheAge PvP event data can be very spiky, so I'm hoping that any smoothing at all could help, friends..!
 */
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

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun MultiPlayerMetricLineChart(
  groups: List<GroupSpec>,
  minutesWindow: Int = 15,
  mode: GameMonitorInteractor.MonitorModes = GameMonitorInteractor.MonitorModes.MONITOR,
  forceSlidingWindow: Boolean = false,
  smoothing: Boolean = false,
  smoothingWindow: Int = 3,
  minXAxisLabels: Int = 2,
  maxXAxisLabels: Int = 8,
  modifier: Modifier = Modifier
) {
  // clamp groups to 1..3
  val usedGroups = groups.take(3).ifEmpty {
    listOf(GroupSpec("All", { true }, Color(0xFFEF5350)))
  }

  var samplesPerGroup by remember { mutableStateOf<List<List<MinuteSample>>>(emptyList()) }
  var minuteRangeStart by remember { mutableStateOf(0L) }
  var minuteRangeEnd by remember { mutableStateOf(0L) }

  LaunchedEffect(usedGroups, minutesWindow, mode, forceSlidingWindow) {
    while (true) {
      val now = System.currentTimeMillis()
      val groupCards = usedGroups.map { spec ->
        PlayerCacheInteractor.getGroupCards { pc -> spec.filter(pc) }
      }

      // collect all events across groups to decide global range
      val allEvents = groupCards.flatMap { cards -> cards.flatMap { it.recentHealEvents } }

      if (allEvents.isEmpty()) {
        val nowMinuteStart = (now / 60_000L) * 60_000L
        val baseline = (minutesWindow - 1 downTo 0).map { i -> MinuteSample(nowMinuteStart - i * 60_000L, 0L) }
        samplesPerGroup = List(usedGroups.size) { baseline }
        minuteRangeStart = baseline.first().minuteStartMillis
        minuteRangeEnd = baseline.last().minuteStartMillis
        delay(5_000L)
        continue
      }

      val useSliding = forceSlidingWindow || (mode != GameMonitorInteractor.MonitorModes.REPLAY)

      if (useSliding) {
        val windowStart = now - minutesWindow * 60_000L
        val nowMinuteStart = (now / 60_000L) * 60_000L
        // buckets per group keyed by minuteStartMillis
        val perGroupBuckets = groupCards.map { cards ->
          val buckets = mutableMapOf<Long, Long>()
          cards.forEach { c ->
            c.recentHealEvents.forEach { e ->
              if (e.timestamp >= windowStart) {
                val minuteStart = (e.timestamp / 60_000L) * 60_000L
                buckets[minuteStart] = (buckets[minuteStart] ?: 0L) + e.amount
              }
            }
          }
          buckets
        }

        val minutes = (minutesWindow - 1 downTo 0).map { i -> nowMinuteStart - i * 60_000L }
        val computedPerGroup = perGroupBuckets.map { buckets ->
          minutes.map { m -> MinuteSample(m, buckets[m] ?: 0L) }
        }

        samplesPerGroup = computedPerGroup
        minuteRangeStart = minutes.first()
        minuteRangeEnd = minutes.last()
      } else {
        // replay: union range across all events
        val oldest = allEvents.minOf { it.timestamp }
        val newest = allEvents.maxOf { it.timestamp }
        val startMinute = (oldest / 60_000L) * 60_000L
        val endMinute = (newest / 60_000L) * 60_000L
        val minutesRange = ((endMinute - startMinute) / 60_000L).toInt().coerceAtLeast(0)
        val minutes = (0..minutesRange).map { i -> startMinute + i * 60_000L }

        val perGroupBuckets = groupCards.map { cards ->
          val buckets = mutableMapOf<Long, Long>()
          cards.forEach { c ->
            c.recentHealEvents.forEach { e ->
              val minuteStart = (e.timestamp / 60_000L) * 60_000L
              buckets[minuteStart] = (buckets[minuteStart] ?: 0L) + e.amount
            }
          }
          buckets
        }

        val computedPerGroup = perGroupBuckets.map { buckets ->
          minutes.map { m -> MinuteSample(m, buckets[m] ?: 0L) }
        }

        samplesPerGroup = computedPerGroup
        minuteRangeStart = minutes.first()
        minuteRangeEnd = minutes.last()
      }

      delay(5_000L)
    }
  }

  val minuteFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

  Column(modifier = modifier) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .background(Color.Transparent)
        .padding(8.dp),
      contentAlignment = Alignment.Center
    ) {
      if (samplesPerGroup.isEmpty() || samplesPerGroup.first().isEmpty()) {
        Text("No data", color = Color.LightGray)
      } else {
        // keep minute timestamps aligned source
        val minutes = remember(samplesPerGroup) { samplesPerGroup.first().map { it.minuteStartMillis } }
        val count = minutes.size.coerceAtLeast(1)

        // Use index-based X to ensure equal spacing across the X axis (0 .. count-1)
        val minX = 0f
        val maxX = (count - 1).toFloat().coerceAtLeast(1f)

        // build data series per group using index as X
        val rawSeries = remember(samplesPerGroup) {
          samplesPerGroup.map { samples ->
            samples.mapIndexed { idx, s -> DefaultPoint(idx.toFloat(), s.damageSum.toFloat()) }
          }
        }

        val dataSeries = remember(rawSeries, smoothing, smoothingWindow) {
          if (smoothing && smoothingWindow > 1) {
            rawSeries.map { series -> simpleMovingAverage(series, smoothingWindow) }
          } else rawSeries
        }

        val maxY = remember(samplesPerGroup) {
          val maxVal = samplesPerGroup.flatten().maxOfOrNull { it.damageSum } ?: 1000L
          max(maxVal.toFloat(), 100f) * 1.1f
        }

        val totalRange = (maxX - minX).coerceAtLeast(1f)

        val desiredLabelCount = max(
          minXAxisLabels,
          min(maxXAxisLabels, count) // cannot have more labels than points mhmm
        )

        // compute evenly spaced label *indices* between 0 and count-1
        val labelIndices: List<Int> = if (desiredLabelCount <= 1) {
          listOf(0)
        } else {
          (0 until desiredLabelCount).map { i ->
            ((i.toFloat() * (count - 1)) / (desiredLabelCount - 1)).roundToInt()
          }.distinct().sorted()
        }

        // precompute index -> formatted time for only those label positions
        val indexToLabel: Map<Int, String> = labelIndices.associateWith { idx ->
          val millis = minutes[idx.coerceIn(0, minutes.lastIndex)]
          minuteFmt.format(Date(millis))
        }

        val xAxisModel = rememberFloatLinearAxisModel(
          range = minX..maxX,
          minimumMajorTickSpacing = 60.dp,
          minorTickCount = 0
        )

        val tickPositionsState = remember { mutableStateOf<List<Float>>(emptyList()) }
        val labeledTicksState = remember { mutableStateOf<Set<Float>>(emptySet()) }

        XYGraph<Float, Float>(
          xAxisModel = xAxisModel,
          yAxisModel = rememberFloatLinearAxisModel(0f..maxY),
          horizontalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.3f),
          verticalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.3f),
          horizontalMinorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0f),
          verticalMinorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0f),

          xAxisLabels = @Composable { xVal: Float ->
            val currentTicks = tickPositionsState.value
            if (!currentTicks.contains(xVal)) {
              val updated = (currentTicks + xVal).distinct().sorted()
              tickPositionsState.value = updated

              val tickCount = updated.size
              if (tickCount > 0) {
                val maxLabels = maxXAxisLabels.coerceAtLeast(minXAxisLabels)
                val indicesToKeep: List<Int> = if (tickCount <= maxLabels) {
                  (0 until tickCount).toList()
                } else {
                  val step = (tickCount - 1).toFloat() / (maxLabels - 1)
                  (0 until maxLabels).map { i -> (i * step).roundToInt().coerceIn(0, tickCount - 1) }.distinct().sorted()
                }
                labeledTicksState.value = indicesToKeep.map { updated[it] }.toSet()
              }
            }

            val labeledTicks = labeledTicksState.value
            val showLabel = labeledTicks.isEmpty() || labeledTicks.contains(xVal)

            if (!showLabel) {
              Text("", style = MaterialTheme.typography.caption)
              return@XYGraph
            }

            val nearestIndex = xVal.roundToInt().coerceIn(0, minutes.lastIndex)
            val millis = minutes[nearestIndex]
            val labelText = minuteFmt.format(Date(millis))

            Text(
              labelText,
              style = MaterialTheme.typography.caption,
              color = Color.LightGray
            )
          },

          yAxisLabels = @Composable { yVal: Float ->
            val label = if (yVal >= 1000f) "${(yVal / 1000f).toInt()}k" else yVal.toInt().toString()
            Text(label, style = MaterialTheme.typography.caption, color = Color.LightGray)
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
              areaBaseline = AreaBaseline.ConstantLine(0f)
            )
          }

          dataSeries.forEachIndexed { idx, series ->
            val color = usedGroups.getOrNull(idx)?.color ?: Color(0xFFEF5350)
            LinePlot2<Float, Float>( // why no type detect :(
              data = series,
              lineStyle = LineStyle(
                brush = Brush.linearGradient(listOf(color, color.copy(alpha = 0.85f))),
                strokeWidth = 1.5.dp,
                alpha = 0.95f
              )
            )
          }
        }
      }
    }
  }
}

@Composable
fun PlayerMetricLineChart(
  playerName: String,
  minutesWindow: Int = 15,
  mode: GameMonitorInteractor.MonitorModes = GameMonitorInteractor.MonitorModes.MONITOR,
  forceSlidingWindow: Boolean = false,
  smoothing: Boolean = false,
  smoothingWindow: Int = 3,
  modifier: Modifier = Modifier
) {
  MultiPlayerMetricLineChart(
    groups = listOf(
      GroupSpec(
        name = playerName,
        filter = { it.name == playerName },
        color = Color(0xFF42A5F5))
    ),
    minutesWindow = minutesWindow,
    mode = mode,
    forceSlidingWindow = forceSlidingWindow,
    smoothing = smoothing,
    smoothingWindow = smoothingWindow,
    modifier = modifier
  )
}
