package com.reoky.raidframer.ui.component.graphs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.definitions.RaidBuffKey
import com.reoky.raidframer.core.definitions.RaidBuffRequirements
import com.reoky.raidframer.core.definitions.lootBuffTotalForIds
import com.reoky.raidframer.core.definitions.matches
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.model.RaidBuffSnapshot
import com.reoky.raidframer.core.serialization.BuffPayload
import com.reoky.raidframer.core.serialization.RaidFramePayload
import io.github.koalaplot.core.line.AreaBaseline.ConstantLine
import io.github.koalaplot.core.line.AreaPlot2
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.*
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_all_buffs
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_buffed
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_individual_buff
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_loot_percent
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_loot_percentage
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_not_scannable
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_session_start
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_unbuffed
import kotlin.math.max

/**
 * Mode for what the Buff History graph displays.
 */
enum class BuffHistoryMode(
  val labelRes: org.jetbrains.compose.resources.StringResource
) {
  ALL_BUFFS(Res.string.raid_buff_history_all_buffs),
  INDIVIDUAL_BUFF(Res.string.raid_buff_history_individual_buff),
  LOOT_PERCENTAGE(Res.string.raid_buff_history_loot_percentage)
}

/**
 * A single data point for the buff history graph at a specific time bucket.
 */
data class BuffHistoryDataPoint(
  val timestamp: Long,
  val buffedCount: Int,
  val notScannableCount: Int,
  val unbuffedCount: Int, // scanned but not buffed (total - buffed - not scannable)
  val totalPlayers: Int,
  val lootPercentSum: Int, // sum of loot percentages across all scannable players
  val lootPercentCount: Int, // number of scannable players with loot data
  val individualBuffCounts: Map<RaidBuffKey, Int> // per-buff-key counts
)

/**
 * Formats a relative time offset (in seconds from session start) as a human-readable label.
 */
fun formatBuffHistoryTime(seconds: Long): String {
  return when {
    seconds < 0 -> ""
    seconds == 0L -> "0s"
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> {
      val m = seconds / 60
      val s = seconds % 60
      if (s > 0) "${m}m ${s}s" else "${m}m"
    }
    seconds < 86400 -> {
      val h = seconds / 3600
      val m = (seconds % 3600) / 60
      if (m > 0) "${h}h ${m}m" else "${h}h"
    }
    else -> {
      val d = seconds / 86400
      val h = (seconds % 86400) / 3600
      if (h > 0) "${d}d ${h}h" else "${d}d"
    }
  }
}

/**
 * Computes the aggregated buff history data points from the timeline data.
 * [sessionStart] is the session start timestamp in epoch millis.
 * [bucketSizeMs] controls the granularity of data points (default 5 minutes = 300_000ms).
 * [requirements] determines what counts as "buffed" for ALL_BUFFS mode.
 * [individualBuffKey] is the specific buff key for INDIVIDUAL_BUFF mode.
 * [mode] determines what metric is shown.
 * [allPlayerNames] is the list of all raid member names.
 */
fun computeBuffHistoryDataPoints(
  timeline: Map<String, List<RaidBuffSnapshot>>,
  sessionStart: Long,
  allPlayerNames: List<String>,
  requirements: RaidBuffRequirements,
  individualBuffKey: RaidBuffKey?,
  mode: BuffHistoryMode,
  bucketSizeMs: Long = 5 * 60_000L
): List<BuffHistoryDataPoint> {
  if (timeline.isEmpty() || allPlayerNames.isEmpty()) return emptyList()

  // Find the time range
  val allTimestamps = timeline.values.flatten().map { it.observedAt }
  val minTs = allTimestamps.minOrNull() ?: sessionStart
  val maxTs = allTimestamps.maxOrNull() ?: System.currentTimeMillis()

  // Align to bucket boundaries relative to session start, not epoch 0
  val bucketStart = if (sessionStart > 0L) sessionStart else (minTs / bucketSizeMs) * bucketSizeMs
  val bucketEnd = ((maxTs - bucketStart) / bucketSizeMs + 1) * bucketSizeMs + bucketStart

  val dataPoints = mutableListOf<BuffHistoryDataPoint>()

  var currentBucket = bucketStart
  while (currentBucket < bucketEnd) {
    val nextBucket = currentBucket + bucketSizeMs

    // For each player, find the most recent snapshot within this bucket
    var buffedCount = 0
    var notScannableCount = 0
    var lootPercentSum = 0
    var lootPercentCount = 0
    val individualBuffCounts = mutableMapOf<RaidBuffKey, Int>()

    allPlayerNames.forEach { playerName ->
      val snapshots = timeline[playerName] ?: return@forEach // not scannable
      // Find the most recent snapshot at or before the bucket midpoint
      val bucketMidpoint = currentBucket + bucketSizeMs / 2
      val latestSnapshot = snapshots
        .filter { it.observedAt <= bucketMidpoint }
        .maxByOrNull { it.observedAt }

      if (latestSnapshot == null) {
        notScannableCount++
      } else {
        val buffIds = latestSnapshot.buffIds
        val fakeMember = RaidFramePayload(
          playerName = playerName,
          buffs = buffIds.map { BuffPayload(buff_id = it) }
        )

        // Check buffed status based on mode
        val isBuffed = when (mode) {
          BuffHistoryMode.ALL_BUFFS -> {
            requirements.matches(fakeMember)
          }
          BuffHistoryMode.INDIVIDUAL_BUFF -> {
            val key = individualBuffKey
            if (key != null) {
              val def = com.reoky.raidframer.core.definitions.raidBuffDefinition(key)
              def.matches(buffIds)
            } else false
          }
          BuffHistoryMode.LOOT_PERCENTAGE -> {
            // For loot mode, "buffed" means has any loot buff
            lootBuffTotalForIds(buffIds) > 100
          }
        }

        if (isBuffed) buffedCount++

        // Always compute loot percentage
        val lootTotal = lootBuffTotalForIds(buffIds)
        lootPercentSum += lootTotal
        lootPercentCount++

        // Track individual buff counts
        com.reoky.raidframer.core.definitions.RAID_BUFF_DEFINITIONS.forEach { def ->
          if (def.matches(buffIds)) {
            individualBuffCounts[def.key] = individualBuffCounts.getOrDefault(def.key, 0) + 1
          }
        }
      }
    }

    dataPoints.add(BuffHistoryDataPoint(
      timestamp = currentBucket,
      buffedCount = buffedCount,
      notScannableCount = notScannableCount,
      unbuffedCount = allPlayerNames.size - buffedCount - notScannableCount,
      totalPlayers = allPlayerNames.size,
      lootPercentSum = lootPercentSum,
      lootPercentCount = lootPercentCount,
      individualBuffCounts = individualBuffCounts
    ))

    currentBucket = nextBucket
  }

  return dataPoints
}

/**
 * A card showing which players had a specific buff at a given time checkpoint.
 */
data class BuffHistorySnapshotCard(
  val timestamp: Long,
  val playerNameToStatus: Map<String, BuffHistoryPlayerStatus>,
  val sessionStart: Long
)

enum class BuffHistoryPlayerStatus {
  BUFFED,
  NOT_BUFFED,
  NOT_SCANNABLE
}

/**
 * Computes snapshot cards at regular intervals (every 5 minutes) for the card list below the graph.
 */
fun computeSnapshotCards(
  timeline: Map<String, List<RaidBuffSnapshot>>,
  sessionStart: Long,
  allPlayerNames: List<String>,
  requirements: RaidBuffRequirements,
  individualBuffKey: RaidBuffKey?,
  mode: BuffHistoryMode,
  bucketSizeMs: Long = 5 * 60_000L
): List<BuffHistorySnapshotCard> {
  if (timeline.isEmpty() || allPlayerNames.isEmpty()) return emptyList()

  val allTimestamps = timeline.values.flatten().map { it.observedAt }
  val minTs = allTimestamps.minOrNull() ?: sessionStart
  val maxTs = allTimestamps.maxOrNull() ?: System.currentTimeMillis()

  // Align to bucket boundaries relative to session start, not epoch 0
  val bucketStart = if (sessionStart > 0L) sessionStart else (minTs / bucketSizeMs) * bucketSizeMs
  val bucketEnd = ((maxTs - bucketStart) / bucketSizeMs + 1) * bucketSizeMs + bucketStart

  val cards = mutableListOf<BuffHistorySnapshotCard>()

  var currentBucket = bucketStart
  while (currentBucket < bucketEnd) {
    val bucketMidpoint = currentBucket + bucketSizeMs / 2

    val playerStatus = mutableMapOf<String, BuffHistoryPlayerStatus>()

    allPlayerNames.forEach { playerName ->
      val snapshots = timeline[playerName] ?: run {
        playerStatus[playerName] = BuffHistoryPlayerStatus.NOT_SCANNABLE
        return@forEach
      }

      val latestSnapshot = snapshots
        .filter { it.observedAt <= bucketMidpoint }
        .maxByOrNull { it.observedAt }

      if (latestSnapshot == null) {
        playerStatus[playerName] = BuffHistoryPlayerStatus.NOT_SCANNABLE
      } else {
        val buffIds = latestSnapshot.buffIds
        val fakeMember = RaidFramePayload(
          playerName = playerName,
          buffs = buffIds.map { BuffPayload(buff_id = it) }
        )

        val isBuffed = when (mode) {
          BuffHistoryMode.ALL_BUFFS -> requirements.matches(fakeMember)
          BuffHistoryMode.INDIVIDUAL_BUFF -> {
            val key = individualBuffKey
            if (key != null) {
              val def = com.reoky.raidframer.core.definitions.raidBuffDefinition(key)
              def.matches(buffIds)
            } else false
          }
          BuffHistoryMode.LOOT_PERCENTAGE -> lootBuffTotalForIds(buffIds) > 100
        }

        playerStatus[playerName] = if (isBuffed) BuffHistoryPlayerStatus.BUFFED else BuffHistoryPlayerStatus.NOT_BUFFED
      }
    }

    cards.add(BuffHistorySnapshotCard(
      timestamp = currentBucket,
      playerNameToStatus = playerStatus,
      sessionStart = sessionStart
    ))

    currentBucket += bucketSizeMs
  }

  return cards
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun BuffHistoryGraph(
  dataPoints: List<BuffHistoryDataPoint>,
  sessionStart: Long,
  sessionEnd: Long,
  mode: BuffHistoryMode,
  modifier: Modifier = Modifier
) {
  val count = dataPoints.size
  val maxX = (count - 1).toFloat().coerceAtLeast(1f)

  // Compute Y-axis max
  val maxY = when (mode) {
    BuffHistoryMode.LOOT_PERCENTAGE -> {
      // For loot %, show average loot % — max around 300-400%
      val maxAvg = dataPoints.maxOfOrNull { dp ->
        if (dp.lootPercentCount > 0) dp.lootPercentSum / dp.lootPercentCount else 0
      } ?: 100
      max(maxAvg * 1.2f, 150f)
    }
    else -> {
      // For buffed/not-scannable, max is total players
      max(dataPoints.maxOfOrNull { it.totalPlayers } ?: 1, 10) * 1.1f
    }
  }

  // Time labels
  val firstTs = dataPoints.first().timestamp
  val lastTs = dataPoints.last().timestamp
  val totalSeconds = (lastTs - firstTs) / 1000L

  // Determine label increment based on total time span
  val labelIncrementSec = when {
    totalSeconds <= 300 -> 30L
    totalSeconds <= 900 -> 60L
    totalSeconds <= 3600 -> 300L
    else -> 600L
  }

  val tickIncrementIndices = max(1f, (labelIncrementSec * 1000L / 5_000_000L).toFloat())
    .coerceAtLeast(1f)

  val graphColor = if (mode == BuffHistoryMode.LOOT_PERCENTAGE) RFColors.buffHistoryLootBuff else RFColors.buffHistoryBuffed

  Column(modifier = modifier) {
    Box(modifier = Modifier.fillMaxSize()) {
      XYGraph<Float, Float>(
        xAxisModel = rememberFloatLinearAxisModel(
          range = 0f..maxX,
          minViewExtent = maxX,
          maxViewExtent = maxX,
          minimumMajorTickIncrement = tickIncrementIndices,
          minimumMajorTickSpacing = 60.dp,
          minorTickCount = 0
        ),
        yAxisModel = rememberFloatLinearAxisModel(
          range = 0f..maxY,
          minimumMajorTickSpacing = 40.dp,
          minorTickCount = 0
        ),
        horizontalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.15f),
        verticalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.15f),
        xAxisStyle = rememberAxisStyle(color = Color.White, tickPosition = TickPosition.Outside),
        yAxisStyle = rememberAxisStyle(color = Color.White, tickPosition = TickPosition.Outside),
        xAxisLabels = @Composable { xVal ->
          val idx = xVal.toInt().coerceIn(0, count - 1)
          // Use session-relative seconds; if sessionStart is 0, fall back to relative to first data point
          val baseTs = if (sessionStart > 0L) sessionStart else dataPoints.first().timestamp
          val relSecs = (dataPoints[idx].timestamp - baseTs) / 1000L
          val snappedSecs = (relSecs / labelIncrementSec) * labelIncrementSec
          val label = formatBuffHistoryTime(snappedSecs)
          Text(text = label, style = typography.caption, color = Color.LightGray)
        },
        yAxisLabels = @Composable { yVal ->
          val label = when (mode) {
            BuffHistoryMode.LOOT_PERCENTAGE -> "${yVal.toInt()}%"
            else -> yVal.toInt().toString()
          }
          Text(text = label, style = typography.caption, color = Color.LightGray)
        }
      ) {
        // Session start marker
        if (sessionStart > 0L) {
          val startIdx = dataPoints.indexOfFirst { it.timestamp >= sessionStart }
          if (startIdx >= 0) {
            LinePlot2(
              data = listOf(
                io.github.koalaplot.core.xygraph.DefaultPoint(startIdx.toFloat(), 0f),
                io.github.koalaplot.core.xygraph.DefaultPoint(startIdx.toFloat(), maxY)
              ),
              lineStyle = LineStyle(
                SolidColor(Color.White.copy(alpha = 0.5f)),
                strokeWidth = 1.dp,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
              )
            )
          }
        }

        // For non-loot modes: stacked area chart showing not-scannable, unbuffed, and buffed
        if (mode != BuffHistoryMode.LOOT_PERCENTAGE) {
          // Not scannable area (grey) — baseline layer
          val notScannableSeries = dataPoints.mapIndexed { idx, dp ->
            io.github.koalaplot.core.xygraph.DefaultPoint(idx.toFloat(), dp.notScannableCount.toFloat())
          }
          AreaPlot2<Float, Float>(
            data = notScannableSeries,
            areaStyle = AreaStyle(
              brush = Brush.verticalGradient(listOf(RFColors.buffHistoryNotScannable.copy(alpha = 0.35f), Color.Transparent)),
              alpha = 1.0f
            ),
            areaBaseline = ConstantLine(0f)
          )
          LinePlot2<Float, Float>(
            data = notScannableSeries,
            lineStyle = LineStyle(
              SolidColor(RFColors.buffHistoryNotScannable),
              strokeWidth = 1.dp,
              alpha = 0.6f
            )
          )

          // Unbuffed area (light red) — stacked on top of not-scannable
          // This shows scanned-but-not-buffed players, filling the gap between not-scannable and total
          val unbuffedStackedSeries = dataPoints.mapIndexed { idx, dp ->
            val stackedTop = (dp.notScannableCount + dp.unbuffedCount).toFloat()
            io.github.koalaplot.core.xygraph.DefaultPoint(idx.toFloat(), stackedTop)
          }
          AreaPlot2<Float, Float>(
            data = unbuffedStackedSeries,
            areaStyle = AreaStyle(
              brush = Brush.verticalGradient(listOf(RFColors.buffHistoryUnbuffed.copy(alpha = 0.25f), Color.Transparent)),
              alpha = 1.0f
            ),
            areaBaseline = ConstantLine(0f)
          )
          // Draw line at the top of the unbuffed area (not-scannable + unbuffed)
          LinePlot2<Float, Float>(
            data = unbuffedStackedSeries,
            lineStyle = LineStyle(
              SolidColor(RFColors.buffHistoryUnbuffed),
              strokeWidth = 1.dp,
              alpha = 0.5f
            )
          )

          // Buffed area (cyan) — stacked on top of unbuffed (not-scannable + unbuffed + buffed = total scanned)
          val buffedStackedSeries = dataPoints.mapIndexed { idx, dp ->
            val stackedTop = (dp.notScannableCount + dp.unbuffedCount + dp.buffedCount).toFloat()
            io.github.koalaplot.core.xygraph.DefaultPoint(idx.toFloat(), stackedTop)
          }
          AreaPlot2<Float, Float>(
            data = buffedStackedSeries,
            areaStyle = AreaStyle(
              brush = Brush.verticalGradient(listOf(graphColor.copy(alpha = 0.35f), Color.Transparent)),
              alpha = 1.0f
            ),
            areaBaseline = ConstantLine(0f)
          )
          LinePlot2<Float, Float>(
            data = buffedStackedSeries,
            lineStyle = LineStyle(
              SolidColor(graphColor),
              strokeWidth = 1.5.dp,
              alpha = 0.95f
            )
          )
        } else {
          // Loot % mode — single area chart (purple)
          val lootSeries = dataPoints.mapIndexed { idx, dp ->
            val avg = if (dp.lootPercentCount > 0) (dp.lootPercentSum / dp.lootPercentCount).toFloat() else 0f
            io.github.koalaplot.core.xygraph.DefaultPoint(idx.toFloat(), avg)
          }
          AreaPlot2<Float, Float>(
            data = lootSeries,
            areaStyle = AreaStyle(
              brush = Brush.verticalGradient(listOf(graphColor.copy(alpha = 0.35f), Color.Transparent)),
              alpha = 1.0f
            ),
            areaBaseline = ConstantLine(0f)
          )
          LinePlot2<Float, Float>(
            data = lootSeries,
            lineStyle = LineStyle(
              SolidColor(graphColor),
              strokeWidth = 1.5.dp,
              alpha = 0.95f
            )
          )
        }
      }

      // Legend in top-right
      Column(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(8.dp).background(graphColor))
          Text(
            text = if (mode == BuffHistoryMode.LOOT_PERCENTAGE) stringResource(Res.string.raid_buff_history_loot_percent) else stringResource(Res.string.raid_buff_history_buffed),
            color = Color.White,
            fontSize = 10.sp
          )
        }
        if (mode != BuffHistoryMode.LOOT_PERCENTAGE) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(RFColors.buffHistoryUnbuffed))
            Text(text = stringResource(Res.string.raid_buff_history_unbuffed), color = Color.White, fontSize = 10.sp)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(RFColors.buffHistoryNotScannable))
            Text(text = stringResource(Res.string.raid_buff_history_not_scannable), color = Color.White, fontSize = 10.sp)
          }
        }
      }

      // Session start/end markers
      Row(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(4.dp)
      ) {
        Text(
          text = "▶ ${stringResource(Res.string.raid_buff_history_session_start)}",
          color = Color.White.copy(alpha = 0.5f),
          fontSize = 9.sp
        )
      }
    }
  }
}
