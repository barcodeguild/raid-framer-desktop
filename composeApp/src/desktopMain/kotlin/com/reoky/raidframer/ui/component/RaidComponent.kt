package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.serialization.RaidFramePayload
import com.reoky.raidframer.core.helpers.RaidColors
import com.reoky.raidframer.core.helpers.getRaidColor
import com.reoky.raidframer.core.model.PlayerRole

private val PARTY_COUNT = 10
private val PARTIES_PER_ROW = 5
private val PARTY_WIDTH = 66.dp
private val NAMEPLATE_HEIGHT = 26.dp
private val MANA_TOP_PADDING = 20.75.dp
private val MANA_HEIGHT = 9.5.dp
private val MEMBER_FRAME_HEIGHT = MANA_TOP_PADDING + MANA_HEIGHT

@Composable
fun RaidComponent(
  parties: List<List<RaidFramePayload>>,
  modifier: Modifier = Modifier
) {
  val paddedParties = parties.toMutableList()
  while (paddedParties.size < PARTY_COUNT) paddedParties.add(emptyList())
  val partyRows = paddedParties.chunked(PARTIES_PER_ROW)

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    partyRows.forEach { rowParties ->
      Row(
        modifier = Modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        rowParties.forEach { party ->
          RaidPartyColumn(party)
        }
      }
    }
  }
}

@Composable
private fun RaidPartyColumn(party: List<RaidFramePayload>) {
  Column(
    modifier = Modifier
      .width(PARTY_WIDTH)
      .background(Color.Black.copy(alpha = 0.40F), RoundedCornerShape(2.dp))
      .padding(bottom = 1.dp),
    verticalArrangement = Arrangement.spacedBy(1.dp)
  ) {
    for (i in 0 until 5) {
      val member = party.getOrNull(i)
      RaidMemberFrame(member ?: RaidFramePayload(playerName = ""))
    }
  }
}

@Composable
fun RaidMemberFrame(member: RaidFramePayload) {
  val roleColor = PlayerRole.fromInt(member.role).getRaidColor()
  val frameShape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 4.dp, bottomEnd = 4.dp)

  Box(modifier = Modifier.size(width = PARTY_WIDTH, height = MEMBER_FRAME_HEIGHT)) {
    // Mana bar only for active members
    if (member.playerName.isNotEmpty()) {
      Box(
        modifier = Modifier
          .padding(top = MANA_TOP_PADDING)
          .size(width = PARTY_WIDTH, height = MANA_HEIGHT)
          .background(RaidColors.ManaBarBlue, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
      )
    }

    // Use the same Surface for both filled and empty slots so elevation/shadow and sizing match exactly.
    Surface(
      modifier = Modifier.size(width = PARTY_WIDTH, height = NAMEPLATE_HEIGHT),
      shape = frameShape,
      color = if (member.playerName.isNotEmpty()) roleColor else Color.Transparent,
      elevation = 4.dp
    ) {
      if (member.playerName.isNotEmpty()) {
        Text(
          text = member.playerName,
          color = Color.LightGray,
          fontFamily = FontsHelper.arKorean(),
          fontSize = 11.sp,
          fontWeight = FontWeight.Normal,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .fillMaxSize()
            .padding(start = 3.dp, top = 3.dp)
        )
      } else {
        Spacer(modifier = Modifier.fillMaxSize())
      }
    }
  }
}
