package com.reoky.raidframer.ui.component.graphs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.definitions.RAID_BUFF_DEFINITIONS
import com.reoky.raidframer.core.definitions.RaidBuffKey
import com.reoky.raidframer.core.definitions.RaidBuffRequirements
import com.reoky.raidframer.core.definitions.lootBuffTotalForIds
import com.reoky.raidframer.core.definitions.matches
import com.reoky.raidframer.core.definitions.raidBuffDefinition
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.model.RaidBuffSnapshot
import com.reoky.raidframer.core.serialization.BuffPayload
import com.reoky.raidframer.core.serialization.RaidFramePayload
import io.github.koalaplot.core.line.AreaBaseline
import io.github.koalaplot.core.line.AreaPlot2
import io.github.koalaplot.core.line.StackArea
import io.github.koalaplot.core.line.StackedAreaPlot
import io.github.koalaplot.core.line.StackedAreaStyle
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

/** Approximate number of time labels wanted across the x-axis. */
private const val TARGET_TIME_LABELS = 8

/** Smallest bucket (used when a session is so short only a single bucket fits). */
private const val MIN_BUCKET_MS = 30_000L

enum class BuffHistoryMode(
  val labelRes: org.jetbrains.compose.resources.StringResource
) {
  ALL_BUFFS(Res.string.raid_buff_history_all_buffs),
  INDIVIDUAL_BUFF(Res.string.raid_buff_history_individual_buff),
  LOOT_PERCENTAGE(Res.string.raid_buff_history_loot_percentage)
}

data class BuffHistoryDataPoint(
  val timestamp: Long,
  val buffedCount: Int,
  val notScannableCount: Int,
  val unbuffedCount: Int,
  val totalPlayers: Int,
  val lootPercentSum: Int,
  val lootPercentCount: Int,
  val individualBuffCounts: Map<RaidBuffKey, Int>
)

/** Formats a time offset (seconds from session start) as a human-readable label. */
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

/** Shorter sessions get finer buckets (30s–5min) so the graph keeps meaningful granularity. */
private fun adaptiveBucketMs(sessionDurationMs: Long): Long = when {
  sessionDurationMs <= 5 * 60_000L -> 30_000L
  sessionDurationMs <= 15 * 60_000L -> 60_000L
  sessionDurationMs <= 30 * 60_000L -> 120_000L
  else -> 300_000L
}

private data class BuffHistoryBuckets(val bucketMs: Long, val starts: List<Long>)

/**
 * Bucket starts from the session start (or the first scan when the session is unknown/older than
 * the retained timeline) up to the newest snapshot, so index 0 is always the graph's 0 marker.
 */
private fun buffHistoryBuckets(timeline: Map<String, List<RaidBuffSnapshot>>, sessionStart: Long): BuffHistoryBuckets? {
  val timestamps = timeline.values.flatten().map { it.observedAt }
  val minTs = timestamps.minOrNull() ?: return null
  val maxTs = timestamps.maxOrNull() ?: return null
  val startMs = if (sessionStart in minTs..maxTs) sessionStart else minTs
  val bucketMs = adaptiveBucketMs(maxTs - startMs)

  val starts = mutableListOf<Long>()
  var current = startMs
  while (current <= maxTs) {
    starts += current
    current += bucketMs
  }
  return BuffHistoryBuckets(bucketMs, starts)
}

/** Most recent snapshot recorded at or before [atMs], or null when the player had not been scanned yet. */
private fun latestSnapshotAt(snapshots: List<RaidBuffSnapshot>, atMs: Long): RaidBuffSnapshot? {
  val idx = snapshots.binarySearchBy(atMs) { it.observedAt }
  return snapshots.getOrNull(if (idx >= 0) idx else -idx - 2)
}

data class BuffHistorySnapshotCard(
  val timestamp: Long,
  val playerNameToStatus: Map<String, BuffHistoryPlayerStatus>,
  /** Buffs each player had at this snapshot; absent for players that could not be scanned. */
  val playerNameToBuffIds: Map<String, Set<Int>>
)

enum class BuffHistoryPlayerStatus {
  BUFFED,
  NOT_BUFFED,
  NOT_SCANNABLE
}

/** Computes a card per bucket recording each player's buff status and buffs at that point in time. */
fun computeSnapshotCards(
  timeline: Map<String, List<RaidBuffSnapshot>>,
  sessionStart: Long,
  allPlayerNames: List<String>,
  requirements: RaidBuffRequirements,
  individualBuffKey: RaidBuffKey?,
  mode: BuffHistoryMode,
  allowGuildBuff: Boolean = true
): List<BuffHistorySnapshotCard> {
  if (timeline.isEmpty() || allPlayerNames.isEmpty()) return emptyList()
  val buckets = buffHistoryBuckets(timeline, sessionStart) ?: return emptyList()

  fun isBuffed(buffIds: Set<Int>): Boolean = when (mode) {
    BuffHistoryMode.ALL_BUFFS -> requirements.matches(
      RaidFramePayload(playerName = "", buffs = buffIds.map { BuffPayload(buff_id = it) }),
      allowGuildBuff
    )
    BuffHistoryMode.INDIVIDUAL_BUFF -> individualBuffKey
      ?.let { raidBuffDefinition(it).matches(buffIds) } ?: false
    BuffHistoryMode.LOOT_PERCENTAGE -> lootBuffTotalForIds(buffIds) > 100
  }

  return buckets.starts.map { bucketStart ->
    val evaluatedAt = bucketStart + buckets.bucketMs / 2
    val playerNameToStatus = mutableMapOf<String, BuffHistoryPlayerStatus>()
    val playerNameToBuffIds = mutableMapOf<String, Set<Int>>()

    allPlayerNames.forEach { playerName ->
      val latest = timeline[playerName]?.let { latestSnapshotAt(it, evaluatedAt) }
      if (latest == null) {
        playerNameToStatus[playerName] = BuffHistoryPlayerStatus.NOT_SCANNABLE
      } else {
        playerNameToBuffIds[playerName] = latest.buffIds
        playerNameToStatus[playerName] =
          if (isBuffed(latest.buffIds)) BuffHistoryPlayerStatus.BUFFED else BuffHistoryPlayerStatus.NOT_BUFFED
      }
    }

    BuffHistorySnapshotCard(bucketStart, playerNameToStatus, playerNameToBuffIds)
  }
}

/** Derives the graph series from the snapshot cards, which are already evaluated per bucket. */
fun computeBuffHistoryDataPoints(cards: List<BuffHistorySnapshotCard>): List<BuffHistoryDataPoint> {
  if (cards.isEmpty()) return emptyList()

  val points = cards.map { card ->
    var buffedCount = 0
    var notScannableCount = 0
    var lootPercentSum = 0
    var lootPercentCount = 0
    val individualBuffCounts = mutableMapOf<RaidBuffKey, Int>()

    card.playerNameToStatus.forEach { (playerName, status) ->
      if (status == BuffHistoryPlayerStatus.NOT_SCANNABLE) {
        notScannableCount++
        return@forEach
      }
      if (status == BuffHistoryPlayerStatus.BUFFED) buffedCount++

      val buffIds = card.playerNameToBuffIds[playerName].orEmpty()
      lootPercentSum += lootBuffTotalForIds(buffIds)
      lootPercentCount++
      RAID_BUFF_DEFINITIONS.forEach { def ->
        if (def.matches(buffIds)) individualBuffCounts[def.key] = (individualBuffCounts[def.key] ?: 0) + 1
      }
    }

    val totalPlayers = card.playerNameToStatus.size
    BuffHistoryDataPoint(
      timestamp = card.timestamp,
      buffedCount = buffedCount,
      notScannableCount = notScannableCount,
      unbuffedCount = totalPlayers - buffedCount - notScannableCount,
      totalPlayers = totalPlayers,
      lootPercentSum = lootPercentSum,
      lootPercentCount = lootPercentCount,
      individualBuffCounts = individualBuffCounts
    )
  }

  // KoalaPlot cannot render a line for a single point, so pad a lone bucket out by one bucket.
  return if (points.size == 1) points + points[0].copy(timestamp = points[0].timestamp + MIN_BUCKET_MS) else points
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun BuffHistoryGraph(
  dataPoints: List<BuffHistoryDataPoint>,
  mode: BuffHistoryMode,
  modifier: Modifier = Modifier
) {
  if (dataPoints.isEmpty()) return
  val count = dataPoints.size
  val maxX = (count - 1).toFloat().coerceAtLeast(1f)
  val firstTs = dataPoints.first().timestamp
  val lastTs = dataPoints.last().timestamp

  val maxY = when (mode) {
    BuffHistoryMode.LOOT_PERCENTAGE -> {
      val maxAvg = dataPoints.maxOf { dp ->
        if (dp.lootPercentCount > 0) dp.lootPercentSum / dp.lootPercentCount else 0
      }
      max(maxAvg * 1.2f, 150f)
    }
    else -> max(dataPoints.maxOf { it.totalPlayers }, 10) * 1.1f
  }

  // Label every Nth bucket (~TARGET_TIME_LABELS labels) so each label lands on an exact bucket time.
  val bucketSec = ((lastTs - firstTs) / 1000L / (count - 1)).coerceAtLeast(1L)
  val totalSec = (lastTs - firstTs) / 1000L
  val labelStep = ((totalSec + TARGET_TIME_LABELS * bucketSec - 1) / (TARGET_TIME_LABELS * bucketSec))
    .coerceAtLeast(1L)
    .coerceAtMost(max(count - 1, 1).toLong())
    .toInt()

  val graphColor = if (mode == BuffHistoryMode.LOOT_PERCENTAGE) RFColors.buffHistoryLootBuff else RFColors.buffHistoryBuffed

  Column(modifier = modifier) {
    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
      XYGraph<Float, Float>(
        xAxisModel = rememberFloatLinearAxisModel(
          range = 0f..maxX,
          minViewExtent = maxX,
          maxViewExtent = maxX,
          minimumMajorTickIncrement = labelStep.toFloat(),
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
          val relSecs = (dataPoints[idx].timestamp - firstTs) / 1000L
          Text(text = formatBuffHistoryTime(relSecs), style = typography.caption, color = Color.LightGray)
        },
        yAxisLabels = @Composable { yVal ->
          val label = when (mode) {
            BuffHistoryMode.LOOT_PERCENTAGE -> "${yVal.toInt()}%"
            else -> yVal.toInt().toString()
          }
          Text(text = label, style = typography.caption, color = Color.LightGray)
        }
      ) {
        if (mode != BuffHistoryMode.LOOT_PERCENTAGE) {
          // Stacked bands: buffed (bottom) → unbuffed → not scannable, so the cyan edge reads the
          // buffed player count straight off the y-axis and every band keeps its own visible fill.
          val buffedSeries = dataPoints.mapIndexed { idx, dp ->
            DefaultPoint(idx.toFloat(), dp.buffedCount.toFloat())
          }
          val scannedSeries = dataPoints.mapIndexed { idx, dp ->
            DefaultPoint(idx.toFloat(), (dp.buffedCount + dp.unbuffedCount).toFloat())
          }
          val totalSeries = dataPoints.mapIndexed { idx, dp ->
            DefaultPoint(idx.toFloat(), (dp.buffedCount + dp.unbuffedCount + dp.notScannableCount).toFloat())
          }
          StackedAreaPlot(
            stacks = listOf(
              StackArea.LinearStackArea<Float, Float>(
                buffedSeries,
                StackedAreaStyle(
                  lineStyle = LineStyle(SolidColor(RFColors.buffHistoryBuffed), strokeWidth = 2.dp),
                  areaStyle = AreaStyle(SolidColor(RFColors.buffHistoryBuffed.copy(alpha = 0.5f)))
                )
              ),
              StackArea.LinearStackArea<Float, Float>(
                scannedSeries,
                StackedAreaStyle(
                  lineStyle = LineStyle(SolidColor(RFColors.buffHistoryUnbuffed), strokeWidth = 1.5.dp),
                  areaStyle = AreaStyle(SolidColor(RFColors.buffHistoryUnbuffed.copy(alpha = 0.6f)))
                )
              ),
              StackArea.LinearStackArea<Float, Float>(
                totalSeries,
                StackedAreaStyle(
                  lineStyle = LineStyle(SolidColor(RFColors.buffHistoryNotScannable), strokeWidth = 1.dp),
                  areaStyle = AreaStyle(SolidColor(RFColors.buffHistoryNotScannable.copy(alpha = 0.45f)))
                )
              )
            ),
            baseline = AreaBaseline.HorizontalLine(0f)
          )
        } else {
          val lootSeries = dataPoints.mapIndexed { idx, dp ->
            val avg = if (dp.lootPercentCount > 0) (dp.lootPercentSum / dp.lootPercentCount).toFloat() else 0f
            DefaultPoint(idx.toFloat(), avg)
          }
          AreaPlot2(
            data = lootSeries,
            areaBaseline = AreaBaseline.HorizontalLine(0f),
            areaStyle = AreaStyle(SolidColor(graphColor.copy(alpha = 0.5f))),
            lineStyle = LineStyle(SolidColor(graphColor), strokeWidth = 1.5.dp)
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
            text = if (mode == BuffHistoryMode.LOOT_PERCENTAGE) stringResource(Res.string.raid_buff_history_loot_percent)
            else stringResource(Res.string.raid_buff_history_buffed),
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
    }

    // Session start marker: sits below the graph at the left, pointing up at the 0 marker.
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 34.dp, top = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = "▲", color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp)
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = stringResource(Res.string.raid_buff_history_session_start),
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 9.sp
      )
    }
  }
}
