package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.Party
import com.reoky.raidframer.core.model.RaidMember
import com.reoky.raidframer.ui.RaidColors
import com.reoky.raidframer.ui.WindowManager

@Preview
@Composable
fun PreviewRaidOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    RaidOverlay()
  }
}

@Composable
fun RaidOverlay(wm: WindowManager? = null) {
  val raid = PlayerCacheInteractor.getRaidById(0).collectAsState() // first raid

  // Take up to 10 parties and split into two rows of up to 5 each
  val partyRows = raid.value.take(10).chunked(5)

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Ensure exactly two rows (top and bottom)
    for (rowIndex in 0 until 2) {
      val rowParties = partyRows.getOrNull(rowIndex) ?: emptyList()

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Render exactly 5 slots per row. Use weight so columns size evenly.
        for (col in 0 until 5) {
          val party = rowParties.getOrNull(col)
          if (party != null) {
            Column(
              modifier = Modifier
                .width(72.dp)
                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                .padding(bottom = 0.5.dp),
              verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
              party.forEach { member ->
                RaidMemberFrame(member)
              }
            }
          } else {
            // Empty slot to keep spacing and alignment consistent
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
fun RaidMemberFrame(member: RaidMember) {
  val roleColor = when (member.role) {
    0 -> RaidColors.Blue
    1 -> RaidColors.Green
    2 -> RaidColors.Pink
    3 -> RaidColors.Red
    4 -> RaidColors.Purple
    else -> RaidColors.FrameBorder
  }
  Box(
    modifier = Modifier
      .size(width = 72.dp, height = 28.dp)
      .background(roleColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
  ) {
    Text(
      text = member.name,
      color = Color.LightGray,
      fontSize = 12.sp,
      fontWeight = FontWeight.Normal,
      maxLines = 1, // prevent overflow
      overflow = TextOverflow.Ellipsis
    )
  }
}