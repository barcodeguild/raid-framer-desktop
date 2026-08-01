package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.window.Popup
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.model.PlayerCard
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.histogram_more_players
import raid_framer_desktop.composeapp.generated.resources.histogram_no_data
import raid_framer_desktop.composeapp.generated.resources.histogram_tooltip_label
import raid_framer_desktop.composeapp.generated.resources.histogram_unknown_label
import raid_framer_desktop.composeapp.generated.resources.histogram_under_10k

data class GearFactionSeries(val label: String, val players: List<PlayerCard>, val color: Color)

@Composable
fun OverlaidGearScoreChart(
  series: List<GearFactionSeries>,
  gearScoreLabel: String,
  playerCountLabel: String,
  modifier: Modifier = Modifier
) {
  val minGear = 10_000
  val maxGear = 22_000
  val binSize = 1_000
  val binCount = (maxGear - minGear) / binSize
  val countsByFaction = series.map { faction ->
    faction.players
      .filter { it.lastKnownGearScore in minGear..maxGear }
      .groupingBy { ((it.lastKnownGearScore - minGear) / binSize).coerceIn(0, binCount - 1) }
      .eachCount()
  }
  val maxObservedPlayers = countsByFaction.flatMap { it.values }.maxOrNull() ?: 0
  val maxPlayers = (((maxObservedPlayers.coerceAtLeast(1) + 4) / 5) * 5).coerceAtLeast(5)
  BoxWithConstraints(modifier.fillMaxWidth().height(360.dp)) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val left = with(density) { 58.dp.toPx() }
    val top = with(density) { 42.dp.toPx() }
    val right = with(density) { 18.dp.toPx() }
    val bottom = with(density) { 48.dp.toPx() }
    Canvas(Modifier.fillMaxWidth().height(360.dp)) {
      val chartWidth = size.width - left - right
      val chartHeight = size.height - top - bottom
      fun x(count: Int) = left + count.toFloat() / maxPlayers * chartWidth
      fun y(score: Int) = top + (maxGear - score).toFloat() / (maxGear - minGear) * chartHeight
      val axisColor = Color(0xFFD8D8D8).copy(alpha = 0.72f)
      val axisWidth = 1.25.dp.toPx()

      for (score in minGear..maxGear step 1000) {
        drawLine(RFColors.CardBorder.copy(alpha = 0.55f), Offset(left, y(score)), Offset(size.width - right, y(score)), 1.dp.toPx())
        drawText(
          textMeasurer = textMeasurer,
          text = "${score / 1000}k",
          topLeft = Offset(left - 34.dp.toPx(), y(score) - 6.dp.toPx()),
          style = TextStyle(color = RFColors.TextTertiary, fontSize = 9.sp)
        )
      }
      drawLine(axisColor, Offset(left, top), Offset(left, size.height - bottom), axisWidth)
      drawLine(axisColor, Offset(left, size.height - bottom), Offset(size.width - right, size.height - bottom), axisWidth)
      for (count in 0..maxPlayers) {
        if (count <= 20 || count % 10 == 0) {
          drawLine(RFColors.CardBorder.copy(alpha = 0.28f), Offset(x(count), top), Offset(x(count), size.height - bottom), 1.dp.toPx())
          drawText(
            textMeasurer = textMeasurer,
            text = count.toString(),
            topLeft = Offset(x(count) - 4.dp.toPx(), size.height - bottom + 8.dp.toPx()),
            style = TextStyle(color = RFColors.TextTertiary, fontSize = 8.sp)
          )
        }
      }
      series.forEachIndexed { index, faction ->
        val counts = countsByFaction[index]
        if (counts.isEmpty()) return@forEachIndexed
        val points = (0..binCount).map { boundary ->
          val count = if (boundary == binCount) 0 else counts[boundary] ?: 0
          Offset(x(count), y(minGear + boundary * binSize))
        }
        val area = Path().apply {
          moveTo(points.first().x, points.first().y)
          points.drop(1).forEach { point -> lineTo(point.x, point.y) }
          lineTo(left, points.last().y)
          lineTo(left, points.first().y)
          close()
        }
        if (points.any { it.x > left }) {
          drawPath(area, faction.color.copy(alpha = 0.10f))
        }
        val line = Path().apply {
          moveTo(points.first().x, points.first().y)
          points.drop(1).forEach { point -> lineTo(point.x, point.y) }
        }
        drawPath(line, faction.color.copy(alpha = 0.85f), style = Stroke(width = 1.5.dp.toPx()))
      }
    }
    Row(Modifier.padding(start = 62.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      series.forEach { faction ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
          Box(Modifier.size(8.dp).background(faction.color, RoundedCornerShape(2.dp)))
          Text(faction.label, color = RFColors.TextSecondary, fontSize = 9.sp)
        }
      }
    }
    Text(gearScoreLabel, color = RFColors.TextTertiary, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 0.dp).layout { measurable, constraints ->
      val placeable = measurable.measure(constraints)
      layout(placeable.height, placeable.width) {
        placeable.placeRelative(-18.dp.roundToPx(), (placeable.width - placeable.height) / 2)
      }
    }.rotate(-90f))
    Text(playerCountLabel, color = RFColors.TextTertiary, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
  }
}

private const val DEFAULT_BIN_SIZE = 1000
//private const val GS_FIXED_MIN = 0
private const val GS_FIXED_MAX = 22000
private const val LOW_GS_THRESHOLD = 10000
private const val BAR_HEIGHT = 16
private const val BAR_SPACING = 2
private const val LABEL_WIDTH = 48
private const val MAX_BAR_WIDTH = 280
private const val TOOLTIP_MAX_PLAYERS = 10

@Composable
fun GearScoreHistogram(
  players: List<PlayerCard>,
  modifier: Modifier = Modifier,
  binSize: Int = DEFAULT_BIN_SIZE
) {
  if (players.isEmpty()) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .height(30.dp)
    ) {
      Text(
        text = stringResource(Res.string.histogram_no_data),
        color = Color.Gray,
        fontSize = 10.sp,
        modifier = Modifier.align(Alignment.Center)
      )
    }
    return
  }

  val knownPlayers = players.filter { it.lastKnownGearScore > 0 }
  val unknownCount = players.size - knownPlayers.size

  val lowGsPlayers = knownPlayers.filter { it.lastKnownGearScore < LOW_GS_THRESHOLD }
  val binnablePlayers = knownPlayers.filter { it.lastKnownGearScore >= LOW_GS_THRESHOLD }

  val numBins = (GS_FIXED_MAX - LOW_GS_THRESHOLD) / binSize
  val bins = List<MutableList<PlayerCard>>(numBins) { mutableListOf() }

  binnablePlayers.forEach { player ->
    val gs = player.lastKnownGearScore
    val binIndex = ((gs - LOW_GS_THRESHOLD) / binSize).coerceIn(0, numBins - 1)
    bins[binIndex].add(player)
  }

  val maxCount = maxOf(bins.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1, lowGsPlayers.size)

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(0.dp)
  ) {
    // render bins from highest gear to lowest (top to bottom), always showing all buckets
    for (i in bins.indices.reversed()) {
      val binPlayers = bins[i]
      val count = binPlayers.size
      val barWidth = if (count > 0) {
        (count.toFloat() / maxCount * MAX_BAR_WIDTH).toInt().coerceAtLeast(6)
      } else {
        0
      }
      val fraction = i.toFloat() / numBins.toFloat()
      val barColor = gearScoreColor(fraction)
      val binStart = LOW_GS_THRESHOLD + i * binSize
      val binEnd = binStart + binSize

      HorizontalBarRow(
        label = "$binStart",
        count = count,
        barWidth = barWidth,
        color = barColor,
        players = binPlayers,
        binRange = "$binStart-${binEnd}"
      )
    }

    // render low GS bucket (< 10k), always shown for alignment
    val lowCount = lowGsPlayers.size
    val lowBarWidth = if (lowCount > 0) {
      (lowCount.toFloat() / maxCount * MAX_BAR_WIDTH).toInt().coerceAtLeast(6)
    } else {
      0
    }
    HorizontalBarRow(
      label = stringResource(Res.string.histogram_under_10k),
      count = lowCount,
      barWidth = lowBarWidth,
      color = RFColors.gearRed,
      players = lowGsPlayers,
      binRange = "0-${LOW_GS_THRESHOLD}"
    )

    // visual separator between known and unknown gear buckets
    if (unknownCount > 0 && numBins > 0) {
      Spacer(modifier = Modifier.height(2.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = LABEL_WIDTH.dp)
      ) {
        Text(
          text = "...",
          color = Color.Gray,
          fontSize = 9.sp,
          modifier = Modifier.padding(bottom = 2.dp)
        )
      }
    }

    // unknown gear bucket at the bottom (players with 0 or undetected gear)
    if (unknownCount > 0) {
      val unknownPlayers = players.filter { it.lastKnownGearScore <= 0 }
      val unknownBarWidth = (unknownCount.toFloat() / maxCount * MAX_BAR_WIDTH).toInt().coerceAtLeast(8)
      HorizontalBarRow(
        label = "?",
        count = unknownCount,
        barWidth = unknownBarWidth,
        color = RFColors.gearUnknown,
        isUnknown = true,
        labelColor = RFColors.gearUnknown,
        players = unknownPlayers,
        binRange = stringResource(Res.string.histogram_unknown_label)
      )
    }
  }
}

@Composable
private fun HorizontalBarRow(
  label: String,
  count: Int,
  barWidth: Int,
  color: Color,
  isUnknown: Boolean = false,
  labelColor: Color = Color.Gray,
  players: List<PlayerCard> = emptyList(),
  binRange: String = ""
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()

  Box(
    modifier = Modifier.height(BAR_HEIGHT.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // y-axis label (gear score bin start value)
      Box(
        modifier = Modifier.width(LABEL_WIDTH.dp),
        contentAlignment = Alignment.CenterEnd
      ) {
        Text(
          text = label,
          color = if (count == 0) Color.DarkGray else labelColor,
          fontSize = if (isUnknown) 10.sp else 8.sp,
          fontWeight = if (isUnknown) FontWeight.Bold else FontWeight.Normal
        )
      }
      Spacer(modifier = Modifier.width(6.dp))
      if (barWidth > 0) {
        Box(
          modifier = Modifier
            .width(barWidth.dp)
            .height((BAR_HEIGHT - BAR_SPACING).dp)
            .shadow(2.dp, RoundedCornerShape(3.dp))
            .background(color, RoundedCornerShape(3.dp))
            .hoverable(interactionSource),
          contentAlignment = Alignment.Center
        ) {
          if (barWidth > 40) {
            Text(
              text = count.toString(),
              color = if (color.luminance() > 0.5f) Color.Black else Color.White,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
      if (barWidth in 1..40) {
        Text(
          text = count.toString(),
          color = Color.LightGray,
          fontSize = 7.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 4.dp)
        )
      }
    }

    // hover tooltip showing player names and gear scores in this bucket
    if (isHovered && players.isNotEmpty()) {
      Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(0, (-players.size.coerceAtMost(8) * 16 - 10))
      ) {
        Box(
          modifier = Modifier
            .widthIn(max = 250.dp)
            .background(RFColors.CardBackground.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
            .shadow(8.dp, RoundedCornerShape(6.dp))
            .padding(8.dp)
        ) {
          Column {
            Text(
              text = String.format(stringResource(Res.string.histogram_tooltip_label), binRange),
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(bottom = 4.dp)
            )
            players.take(TOOLTIP_MAX_PLAYERS).forEach { player ->
              Text(
                text = "${player.name} (${player.lastKnownGearScore})",
                color = Color.LightGray,
                fontSize = 9.sp,
                modifier = Modifier.padding(bottom = 1.dp)
              )
            }
            if (players.size > TOOLTIP_MAX_PLAYERS) {
              Text(
                text = String.format(stringResource(Res.string.histogram_more_players), players.size - TOOLTIP_MAX_PLAYERS),
                color = Color.Gray,
                fontSize = 8.sp,
                fontStyle = FontStyle.Italic
              )
            }
          }
        }
      }
    }
  }
}

private fun gearScoreColor(fraction: Float): Color {
  return when {
    fraction < 0.25f -> lerp(RFColors.gearOrange, RFColors.gearYellow, fraction / 0.25f)
    fraction < 0.5f -> lerp(RFColors.gearYellow, RFColors.gearGreen, (fraction - 0.25f) / 0.25f)
    fraction < 0.75f -> lerp(RFColors.gearGreen, RFColors.gearBlue, (fraction - 0.5f) / 0.25f)
    else -> lerp(RFColors.gearBlue, RFColors.gearCyan, (fraction - 0.75f) / 0.25f)
  }
}

private fun Color.luminance(): Float {
  return (red * 0.299f + green * 0.587f + blue * 0.114f)
}
