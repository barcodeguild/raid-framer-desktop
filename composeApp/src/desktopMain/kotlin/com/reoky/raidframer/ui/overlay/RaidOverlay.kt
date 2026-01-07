package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.serialization.RaidMember
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.RaidColors
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent

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

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(4.dp)
  ) {

    Column(
      modifier = Modifier.fillMaxSize().padding(1.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

      TitleBarComponent(
        title = "Raid Overlay",
        onClose = { wm?.closeWindow(OverlayType.RAID) }
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Ensure exactly two rows (top and bottom)
      for (rowIndex in 0 until 2) {
        val rowParties = partyRows.getOrNull(rowIndex) ?: emptyList()

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          for (col in 0 until 5) {
            val party = rowParties.getOrNull(col)
            if (party != null) {
              Column(
                modifier = Modifier
                  .width(66.dp)
                  .background(Color.Black.copy(alpha = 0.25F), RoundedCornerShape(2.dp))
                  .padding(bottom = 0.5.dp),
                verticalArrangement = Arrangement.spacedBy(0.75.dp)
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
  val frameShape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 4.dp, bottomEnd = 4.dp)

  if (member.name.isNotEmpty()) {
    Box(modifier = Modifier.width(65.75.dp)) {
      // Mana Bar (Background Layer)
      Box(
        modifier = Modifier
          .padding(top = 20.75.dp) // Position to tuck under the frame's bottom corners
          .size(width = 65.75.dp, height = 9.5.dp) // 6dp overlap + 3dp visible bar
          .background(RaidColors.ManaBarBlue, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
      )

      // Nameplate Frame (Foreground Layer)
      Surface(
        modifier = Modifier.size(width = 65.75.dp, height = 26.dp),
        shape = frameShape,
        color = roleColor,
        elevation = 4.dp // Casts shadow onto the mana bar below
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
  } else {
    // Empty frame for missing member
    Box(
      modifier = Modifier
        .size(width = 65.75.dp, height = 26.dp)
        .background(Color.Transparent, frameShape)
    )
  }
}