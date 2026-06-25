package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.model.PlayerCard
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.histogram_more_players
import raid_framer_desktop.composeapp.generated.resources.histogram_no_data
import raid_framer_desktop.composeapp.generated.resources.histogram_tooltip_label
import raid_framer_desktop.composeapp.generated.resources.histogram_unknown_label

private const val DEFAULT_BIN_SIZE = 1000
private const val GS_FIXED_MIN = 0
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
      label = "<10k",
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

  Box {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(BAR_HEIGHT.dp),
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
