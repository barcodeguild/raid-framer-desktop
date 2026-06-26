package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.database.LeadershipRole
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.model.PlayerCard
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.player_details_leadership_role
import raid_framer_desktop.composeapp.generated.resources.player_details_title

@Composable
fun PlayerDetailsSection(card: PlayerCard, onLeadershipChange: (Int) -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .background(
        Color.DarkGray.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
      )
      .padding(12.dp)
  ) {
    Text(
      text = stringResource(Res.string.player_details_title),
      color = Color.White,
      fontWeight = FontWeight.Bold,
      fontSize = 18.sp,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    Row(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.weight(1f)) {
        DetailRow("Name", card.name)
        DetailRow("Faction", card.lastKnownFaction)
        DetailRow("Faction Status", card.lastKnownFactionStatus)
        DetailRow("Guild", card.lastKnownGuild.ifEmpty { "N/A" })
        DetailRow("Gear Score", if (card.lastKnownGearScore <= 0) "Please Click on Player" else card.lastKnownGearScore.toString())
        DetailRow("Build", card.currentBuild)
        DetailRow("Current Role", stringResource(LeadershipRole.fromInt(card.leaderships).friendlyNameRes))
      }

      Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
        Text(
          text = stringResource(Res.string.player_details_leadership_role),
          color = Color.LightGray,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 4.dp)
        )
        LeadershipSelector(
          currentLeadership = card.leaderships,
          onLeadershipChange = onLeadershipChange
        )
      }
    }
  }
}

@Composable
fun DetailRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = "$label:", color = Color.LightGray, fontSize = 13.sp)
    Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
fun LeadershipSelector(currentLeadership: Int, onLeadershipChange: (Int) -> Unit) {
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    LeadershipRole.entries.forEach { leadershipRole ->
      Row(
        modifier = Modifier
          .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        RadioButton(
          selected = currentLeadership == leadershipRole.value,
          onClick = { onLeadershipChange(leadershipRole.value) },
          colors = RadioButtonDefaults.colors(
            selectedColor = RFColors.AccentRed,
            unselectedColor = Color.Gray
          )
        )
        Text(
          text = stringResource(leadershipRole.friendlyNameRes),
          color = if (currentLeadership == leadershipRole.value) RFColors.AccentRed else Color.LightGray,
          fontSize = 11.sp
        )
      }
    }
  }
}

