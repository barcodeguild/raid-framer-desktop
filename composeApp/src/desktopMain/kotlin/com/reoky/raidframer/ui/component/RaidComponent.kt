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
import com.reoky.raidframer.core.serialization.RaidFramePayload
import com.reoky.raidframer.ui.RaidColors

@Composable
fun RaidComponent(
    parties: List<List<RaidFramePayload>>,
    modifier: Modifier = Modifier
) {
    // Chunk parties into rows of 5. This handles unlimited parties dynamically.
    val partyRows = parties.chunked(5)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp) // Gap between rows
    ) {
        partyRows.forEach { rowParties ->
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Iterate 0..4 to ensure we print empty slots for alignment if a row is incomplete
                for (col in 0 until 5) {
                    val party = rowParties.getOrNull(col)
                    if (party != null) {
                        RaidPartyColumn(party)
                    } else {
                        // Empty slot to keep spacing and alignment consistent with other rows
                        Box(modifier = Modifier.width(66.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RaidPartyColumn(party: List<RaidFramePayload>) {
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
}

@Composable
fun RaidMemberFrame(member: RaidFramePayload) {
    val roleColor = when (member.role) {
        0 -> RaidColors.Blue
        1 -> RaidColors.Green
        2 -> RaidColors.Pink
        3 -> RaidColors.Red
        4 -> RaidColors.Purple
        else -> RaidColors.FrameBorder
    }
    val frameShape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 4.dp, bottomEnd = 4.dp)

    if (member.playerName.isNotEmpty()) {
        Box(modifier = Modifier.width(65.75.dp)) {
            // Mana Bar (Background Layer)
            Box(
                modifier = Modifier
                    .padding(top = 20.75.dp)
                    .size(width = 65.75.dp, height = 9.5.dp)
                    .background(RaidColors.ManaBarBlue, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            )

            // Nameplate Frame (Foreground Layer)
            Surface(
                modifier = Modifier.size(width = 65.75.dp, height = 26.dp),
                shape = frameShape,
                color = roleColor,
                elevation = 4.dp
            ) {
                Text(
                    text = member.playerName,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
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