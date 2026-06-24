package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.model.PlayerCard

private const val DEFAULT_BIN_SIZE = 500
private const val BAR_HEIGHT = 16
private const val BAR_SPACING = 2
private const val LABEL_WIDTH = 40
private const val MAX_BAR_WIDTH = 280

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
        text = "No data",
        color = Color.Gray,
        fontSize = 10.sp,
        modifier = Modifier.align(Alignment.Center)
      )
    }
    return
  }

  val knownPlayers = players.filter { it.lastKnownGearScore > 0 }
  val unknownCount = players.size - knownPlayers.size

  val minGear = knownPlayers.minOfOrNull { it.lastKnownGearScore } ?: 0
  val maxGear = knownPlayers.maxOfOrNull { it.lastKnownGearScore } ?: 0

  val cappedMin = (minGear / binSize) * binSize
  val cappedMax = ((maxGear / binSize) + 1) * binSize

  val numBins = if (cappedMax > cappedMin) (cappedMax - cappedMin) / binSize else 1
  val bins = IntArray(numBins)

  knownPlayers.forEach { player ->
    val gs = player.lastKnownGearScore
    val binIndex = ((gs - cappedMin) / binSize).coerceIn(0, numBins - 1)
    bins[binIndex]++
  }

  val maxCount = bins.maxOrNull()?.coerceAtLeast(1) ?: 1

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(0.dp)
  ) {
    for (i in bins.indices.reversed()) {
      val count = bins[i]
      if (count <= 0) continue

      val barWidth = (count.toFloat() / maxCount * MAX_BAR_WIDTH).toInt().coerceAtLeast(6)
      val fraction = i.toFloat() / numBins.toFloat()
      val barColor = gearScoreColor(fraction)
      val binStart = cappedMin + i * binSize

      HorizontalBarRow(
        label = "$binStart",
        count = count,
        barWidth = barWidth,
        color = barColor
      )
    }

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

    if (unknownCount > 0) {
      val unknownBarWidth = (unknownCount.toFloat() / maxCount * MAX_BAR_WIDTH).toInt().coerceAtLeast(8)
      HorizontalBarRow(
        label = "?",
        count = unknownCount,
        barWidth = unknownBarWidth,
        color = Color(0xFF666666),
        isUnknown = true,
        labelColor = Color(0xFF666666)
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
  labelColor: Color = Color.Gray
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(BAR_HEIGHT.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier.width(LABEL_WIDTH.dp),
      contentAlignment = Alignment.CenterEnd
    ) {
      Text(
        text = label,
        color = labelColor,
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
      ) {
        if (barWidth > 40) {
          Text(
            text = count.toString(),
            color = if (color.luminance() > 0.5f) Color.Black else Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .align(Alignment.Center)
              .offset(x = 2.dp)
          )
        }
      }
    }
    if (barWidth <= 40 && barWidth > 0) {
      Text(
        text = count.toString(),
        color = Color.LightGray,
        fontSize = 7.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp)
      )
    }
  }
}

private fun gearScoreColor(fraction: Float): Color {
  return when {
    fraction < 0.2f -> lerp(Color(0xFFFF1744), Color(0xFFFF9800), fraction / 0.2f)
    fraction < 0.4f -> lerp(Color(0xFFFF9800), Color(0xFFFFEB3B), (fraction - 0.2f) / 0.2f)
    fraction < 0.6f -> lerp(Color(0xFFFFEB3B), Color(0xFF8BC34A), (fraction - 0.4f) / 0.2f)
    fraction < 0.8f -> lerp(Color(0xFF8BC34A), Color(0xFF03A9F4), (fraction - 0.6f) / 0.2f)
    else -> lerp(Color(0xFF03A9F4), Color(0xFF00E5FF), (fraction - 0.8f) / 0.2f)
  }
}

private fun Color.luminance(): Float {
  return (red * 0.299f + green * 0.587f + blue * 0.114f)
}
