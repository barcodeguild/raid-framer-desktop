package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.Party
import com.reoky.raidframer.RaidMember
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
    RaidOverlay(raid = listOf())
  }
}

@Composable
fun RaidOverlay(wm: WindowManager? = null, raid: List<Party>) {
  Row(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    raid.forEach { party ->
      Column(
        modifier = Modifier
          .background(Color.DarkGray, RoundedCornerShape(8.dp))
          .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        party.forEach { member ->
          RaidMemberFrame(member)
        }
      }
    }
  }
}

@Composable
fun RaidMemberFrame(member: RaidMember) {
  val roleColor = when (member.role) {
    "Tank" -> RaidColors.Green
    "Healer" -> RaidColors.Pink
    "DPS" -> RaidColors.Red
    else -> RaidColors.FrameBorder
  }
  Box(
    modifier = Modifier
      .size(width = 120.dp, height = 32.dp)
      .background(roleColor, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
      .padding(4.dp)
  ) {
    Text(
      text = member.name,
      color = Color.White,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold
    )
  }
}