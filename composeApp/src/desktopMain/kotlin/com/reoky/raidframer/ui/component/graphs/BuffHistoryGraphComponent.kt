package com.reoky.raidframer.ui.component.graphs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.definitions.RAID_BUFF_DEFINITIONS
import com.reoky.raidframer.core.definitions.RaidBuffKey
import com.reoky.raidframer.core.definitions.RaidBuffRequirements
import com.reoky.raidframer.core.definitions.lootBuffTotalForIds
import com.reoky.raidframer.core.definitions.matches
import com.reoky.raidframer.core.definitions.raidBuffDefinition
import com.reoky.raidframer.core.helpers.RFTypography
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.verticalBandBrush
import com.reoky.raidframer.core.model.RaidBuffSnapshot
import com.reoky.raidframer.core.model.RaidRosterFrame
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
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_buffed_share_value
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_individual_buff
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_loot_percent
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_loot_percentage
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_metric_interval
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_metric_peak
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_metric_share
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_not_scannable
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_session_start
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_title
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_unbuffed
import kotlin.math.max

/** Approximate number of time labels wanted across the x-axis. */
private const val TARGET_TIME_LABELS = 8

/** Smallest bucket (used when a session is so short only a single bucket fits). */
private const val MIN_BUCKET_MS = 30_000L

/** Band fill alpha at a band's top edge and at its baseline, per series. */
private data class BandFill(val topAlpha: Float, val bottomAlpha: Float)

private val BuffedFill = BandFill(topAlpha = 0.40f, bottomAlpha = 0.10f)
private val UnbuffedFill = BandFill(topAlpha = 0.30f, bottomAlpha = 0.07f)
private val NotScannableFill = BandFill(topAlpha = 0.20f, bottomAlpha = 0.04f)
private val LootFill = BandFill(topAlpha = 0.40f, bottomAlpha = 0.06f)

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
      if (h > 0) "${h}h ${m}m" else "${h}h"
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

/**
 * Roster that was present at [atMs]: the latest recorded roster frame at or before that moment per
 * raid (falling back to each log's first frame for buckets that predate recording). Null when no
 * roster was ever recorded, so callers fall back to the current roster.
 */
private fun resolveBuffHistoryRoster(
  rosterTimeline: Map<Int, List<RaidRosterFrame>>,
  atMs: Long
): Set<String>? {
  if (rosterTimeline.values.all { it.isEmpty() }) return null
  return rosterTimeline.values.mapNotNull { frames ->
    frames.lastOrNull { it.observedAt <= atMs }?.playerNames
      ?: frames.firstOrNull()?.playerNames
  }.flatMapTo(LinkedHashSet()) { it }
}

/** Computes a card per bucket recording each player's buff status and buffs at that point in time. */
fun computeSnapshotCards(
  timeline: Map<String, List<RaidBuffSnapshot>>,
  sessionStart: Long,
  allPlayerNames: List<String>,
  requirements: RaidBuffRequirements,
  individualBuffKey: RaidBuffKey?,
  mode: BuffHistoryMode,
  allowGuildBuff: Boolean = true,
  /** Recorded raid rosters; each bucket uses the roster that was present instead of [allPlayerNames]. */
  rosterTimeline: Map<Int, List<RaidRosterFrame>> = emptyMap()
): List<BuffHistorySnapshotCard> {
  if (timeline.isEmpty() && rosterTimeline.values.all { it.isEmpty() }) return emptyList()
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

  return buckets.starts.mapNotNull { bucketStart ->
    val evaluatedAt = bucketStart + buckets.bucketMs / 2
    // The bucket's roster is whoever was actually in the raid then: departed players are excluded
    // (no ghosts) and newcomers only appear from the bucket they joined. Empty-roster buckets
    // (everyone had left) carry no card; the graph line simply ends at the last populated bucket.
    val roster = resolveBuffHistoryRoster(rosterTimeline, evaluatedAt) ?: allPlayerNames
    if (roster.isEmpty()) return@mapNotNull null
    val playerNameToStatus = mutableMapOf<String, BuffHistoryPlayerStatus>()
    val playerNameToBuffIds = mutableMapOf<String, Set<Int>>()

    roster.forEach { playerName ->
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

/** Session summary for the graph's metrics strip, derived purely from the plotted points. */
data class BuffHistoryMetrics(
  val buffedCount: Int,
  val totalPlayers: Int,
  val buffedShare: Float,
  val currentBuffed: Int,
  val currentLootPercent: Int,
  val peakValue: Int,
  val peakSeconds: Long,
  val intervalSeconds: Long
)

/** Summarises the latest, peak and interval state of [points] for [mode]. */
fun computeBuffHistoryMetrics(
  points: List<BuffHistoryDataPoint>,
  mode: BuffHistoryMode
): BuffHistoryMetrics? {
  if (points.isEmpty()) return null
  val first = points.first()
  val last = points.last()

  fun lootAvg(dp: BuffHistoryDataPoint): Int =
    if (dp.lootPercentCount > 0) dp.lootPercentSum / dp.lootPercentCount else 0

  val peak = when (mode) {
    BuffHistoryMode.LOOT_PERCENTAGE -> points.maxByOrNull { lootAvg(it) }
    else -> points.maxByOrNull { it.buffedCount }
  } ?: last

  return BuffHistoryMetrics(
    buffedCount = last.buffedCount,
    totalPlayers = last.totalPlayers,
    buffedShare = if (last.buffedCount + last.unbuffedCount > 0) {
      last.buffedCount * 100f / (last.buffedCount + last.unbuffedCount)
    } else 0f,
    currentBuffed = last.buffedCount,
    currentLootPercent = lootAvg(last),
    peakValue = when (mode) {
      BuffHistoryMode.LOOT_PERCENTAGE -> lootAvg(peak)
      else -> peak.buffedCount
    },
    peakSeconds = (peak.timestamp - first.timestamp) / 1000L,
    intervalSeconds = if (points.size > 1) {
      (last.timestamp - first.timestamp) / 1000L / (points.size - 1)
    } else 0L
  )
}

private data class BuffHistoryMetricSpec(val label: String, val value: String, val sub: String?, val color: Color)

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
  val metrics = remember(dataPoints, mode) { computeBuffHistoryMetrics(dataPoints, mode) }

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

  val isLoot = mode == BuffHistoryMode.LOOT_PERCENTAGE
  val graphColor = if (isLoot) RFColors.buffHistoryLootBuff else RFColors.buffHistoryBuffed

  // Per-band gradient fill: bright under each band's top edge, fading toward its baseline. Bands
  // are stacked and disjoint, so every gradient samples only its own span of the plot surface.
  fun bandBrush(color: Color, fill: BandFill, topValue: Float, bottomValue: Float) =
    color.verticalBandBrush(
      topFraction = 1f - (topValue / maxY).coerceIn(0f, 1f),
      bottomFraction = 1f - (bottomValue / maxY).coerceIn(0f, 1f),
      topAlpha = fill.topAlpha,
      bottomAlpha = fill.bottomAlpha
    )

  Column(modifier = modifier) {
    // Panel title: accent-ticked heading on the left, series legend kept clear on the right.
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 7.dp),
      horizontalArrangement = Arrangement.spacedBy(7.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(modifier = Modifier.size(width = 3.dp, height = 11.dp).background(graphColor, RoundedCornerShape(1.dp)))
      Text(
        text = (if (isLoot) {
          stringResource(Res.string.raid_buff_history_loot_percentage)
        } else {
          stringResource(Res.string.raid_buff_history_title)
        }).uppercase(),
        style = RFTypography.SectionTitle,
        color = RFColors.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        BuffHistoryLegendItem(
          color = graphColor,
          label = if (isLoot) {
            stringResource(Res.string.raid_buff_history_loot_percent)
          } else {
            stringResource(Res.string.raid_buff_history_buffed)
          }
        )
        if (!isLoot) {
          BuffHistoryLegendItem(color = RFColors.buffHistoryUnbuffed, label = stringResource(Res.string.raid_buff_history_unbuffed))
          BuffHistoryLegendItem(color = RFColors.buffHistoryNotScannable, label = stringResource(Res.string.raid_buff_history_not_scannable))
        }
      }
    }

    // Metrics strip: the session's headline numbers, derived from the same data as the series.
    metrics?.let { m ->
      val specs = if (isLoot) {
        listOf(
          BuffHistoryMetricSpec(
            label = stringResource(Res.string.raid_buff_history_loot_percent),
            value = "${m.currentLootPercent}%",
            sub = null,
            color = RFColors.buffHistoryLootBuff
          ),
          BuffHistoryMetricSpec(
            label = stringResource(Res.string.raid_buff_history_metric_peak),
            value = "${m.peakValue}%",
            sub = formatBuffHistoryTime(m.peakSeconds).ifEmpty { null },
            color = RFColors.TextPrimary
          )
        )
      } else {
        listOf(
          BuffHistoryMetricSpec(
            label = stringResource(Res.string.raid_buff_history_buffed),
            value = "${m.currentBuffed}",
            sub = "/ ${m.totalPlayers}",
            color = RFColors.buffHistoryBuffed
          ),
          BuffHistoryMetricSpec(
            label = stringResource(Res.string.raid_buff_history_metric_share),
            value = String.format(stringResource(Res.string.raid_buff_history_buffed_share_value), m.buffedShare),
            sub = null,
            color = RFColors.buffHistoryUptime
          ),
          BuffHistoryMetricSpec(
            label = stringResource(Res.string.raid_buff_history_metric_peak),
            value = "${m.peakValue}",
            sub = formatBuffHistoryTime(m.peakSeconds).ifEmpty { null },
            color = RFColors.TextPrimary
          )
        )
      }
      val allSpecs = if (m.intervalSeconds > 0) {
        specs + BuffHistoryMetricSpec(
          label = stringResource(Res.string.raid_buff_history_metric_interval),
          value = formatBuffHistoryTime(m.intervalSeconds),
          sub = null,
          color = RFColors.TextPrimary
        )
      } else {
        specs
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        allSpecs.forEachIndexed { index, spec ->
          if (index > 0) {
            Box(modifier = Modifier.width(1.dp).height(26.dp).background(RFColors.buffHistoryHairline))
          }
          BuffHistoryMetric(
            label = spec.label,
            value = spec.value,
            sub = spec.sub,
            color = spec.color,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Plot well: a recessed, bordered inset surface the grid and series sit on.
    Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(RFColors.buffHistoryPlotWell, RoundedCornerShape(4.dp))
          .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
          .padding(start = 4.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)
      ) {
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
          horizontalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.05f),
          verticalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.04f),
          xAxisStyle = rememberAxisStyle(
            color = Color.White.copy(alpha = 0.30f),
            tickPosition = TickPosition.Outside,
            majorTickSize = 3.dp
          ),
          yAxisStyle = rememberAxisStyle(
            color = Color.White.copy(alpha = 0.30f),
            tickPosition = TickPosition.Outside,
            majorTickSize = 3.dp
          ),
          xAxisLabels = @Composable { xVal ->
            val idx = xVal.toInt().coerceIn(0, count - 1)
            val relSecs = (dataPoints[idx].timestamp - firstTs) / 1000L
            Text(
              text = formatBuffHistoryTime(relSecs),
              style = RFTypography.Meta,
              color = RFColors.TextSecondary
            )
          },
          yAxisLabels = @Composable { yVal ->
            val label = if (isLoot) "${yVal.toInt()}%" else yVal.toInt().toString()
            Text(text = label, style = RFTypography.Meta, color = RFColors.TextSecondary)
          }
        ) {
          if (isLoot) {
            val lootSeries = dataPoints.mapIndexed { idx, dp ->
              val avg = if (dp.lootPercentCount > 0) (dp.lootPercentSum / dp.lootPercentCount).toFloat() else 0f
              DefaultPoint(idx.toFloat(), avg)
            }
            val maxLoot = dataPoints.maxOf {
              if (it.lootPercentCount > 0) (it.lootPercentSum / it.lootPercentCount).toFloat() else 0f
            }
            AreaPlot2(
              data = lootSeries,
              areaBaseline = AreaBaseline.HorizontalLine(0f),
              areaStyle = AreaStyle(bandBrush(graphColor, LootFill, maxLoot, 0f)),
              lineStyle = LineStyle(SolidColor(graphColor), strokeWidth = 1.5.dp)
            )
          } else {
            // Stacked bands: buffed (bottom) → unbuffed → not scannable, so the cyan edge reads the
            // buffed player count straight off the y-axis and every band keeps its own visible fill.
            val buffedSeries = dataPoints.mapIndexed { idx, dp -> DefaultPoint(idx.toFloat(), dp.buffedCount.toFloat()) }
            val scannedSeries = dataPoints.mapIndexed { idx, dp ->
              DefaultPoint(idx.toFloat(), (dp.buffedCount + dp.unbuffedCount).toFloat())
            }
            val totalSeries = dataPoints.mapIndexed { idx, dp ->
              DefaultPoint(idx.toFloat(), (dp.buffedCount + dp.unbuffedCount + dp.notScannableCount).toFloat())
            }
            StackedAreaPlot(
              stacks = listOf(
                StackArea.LinearStackArea(
                  buffedSeries,
                  StackedAreaStyle(
                    lineStyle = LineStyle(SolidColor(RFColors.buffHistoryBuffed), strokeWidth = 2.dp),
                    areaStyle = AreaStyle(bandBrush(RFColors.buffHistoryBuffed, BuffedFill, dataPoints.maxOf { it.buffedCount.toFloat() }, 0f))
                  )
                ),
                StackArea.LinearStackArea(
                  scannedSeries,
                  StackedAreaStyle(
                    lineStyle = LineStyle(SolidColor(RFColors.buffHistoryUnbuffed), strokeWidth = 1.5.dp),
                    areaStyle = AreaStyle(
                      bandBrush(RFColors.buffHistoryUnbuffed, UnbuffedFill, dataPoints.maxOf { (it.buffedCount + it.unbuffedCount).toFloat() }, 0f)
                    )
                  )
                ),
                StackArea.LinearStackArea(
                  totalSeries,
                  StackedAreaStyle(
                    lineStyle = LineStyle(SolidColor(RFColors.buffHistoryNotScannable), strokeWidth = 1.25.dp),
                    areaStyle = AreaStyle(
                      bandBrush(
                        RFColors.buffHistoryNotScannable,
                        NotScannableFill,
                        dataPoints.maxOf { (it.buffedCount + it.unbuffedCount + it.notScannableCount).toFloat() },
                        0f
                      )
                    )
                  )
                )
              ),
              baseline = AreaBaseline.HorizontalLine(0f)
            )
          }
        }
      }
    }

    // Session start marker: sits below the well, pointing up at the 0s label.
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 34.dp, top = 4.dp, bottom = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = "▲", color = RFColors.TextTertiary.copy(alpha = 0.7f), fontSize = 7.sp)
      Text(
        text = stringResource(Res.string.raid_buff_history_session_start),
        style = RFTypography.Meta,
        color = RFColors.TextTertiary
      )
    }
  }
}

/** One entry of the graph legend: a series-coloured swatch bar and its label. */
@Composable
private fun BuffHistoryLegendItem(color: Color, label: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
    // Same nudge as the card stat swatch so both align to the text's visual midline.
    Box(
      modifier = Modifier
        .offset(y = 1.dp)
        .size(width = 8.dp, height = 3.dp)
        .background(color, RoundedCornerShape(1.5.dp))
    )
    Text(text = label, style = RFTypography.Meta, color = RFColors.TextSecondary, maxLines = 1)
  }
}

/** One metrics-strip block: an uppercase kicker over a value with an optional trailing sub-value. */
@Composable
private fun BuffHistoryMetric(
  label: String,
  value: String,
  sub: String?,
  color: Color,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.padding(horizontal = 2.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Text(text = label.uppercase(), style = RFTypography.Kicker, color = RFColors.TextTertiary, maxLines = 1)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
      Text(text = value, style = RFTypography.Metric, color = color, maxLines = 1)
      if (sub != null) {
        Text(
          text = sub,
          style = RFTypography.Meta,
          color = RFColors.TextTertiary,
          maxLines = 1,
          modifier = Modifier.padding(bottom = 1.dp)
        )
      }
    }
  }
}
