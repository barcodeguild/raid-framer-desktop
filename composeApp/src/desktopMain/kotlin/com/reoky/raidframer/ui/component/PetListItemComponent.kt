package com.reoky.raidframer.ui.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.getPetIcon
import com.reoky.raidframer.core.helpers.RFColors
import lol.rfcloud.core.helpers.humanReadableAbbreviation

@Composable
fun PetListItem(
  petName: String,
  owner: String,
  damage: Long,
  debuffs: List<String>,
  petType: String = "default",
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  val content = Modifier
    .fillMaxWidth()
    .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

  Surface(
    modifier = modifier.then(content),
    color = RFColors.CardBackground,
    elevation = 4.dp,
    shape = RoundedCornerShape(12.dp),
  ) {
    Box(
      modifier = Modifier
        .border(
          width = 1.dp,
          color = RFColors.CardBorder,
          shape = RoundedCornerShape(12.dp)
        )
    ) {
      Row(
        modifier = Modifier
          .padding(12.dp)
          .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Icon with subtle red border
        Box(
          modifier = Modifier
            .size(48.dp)
            .background(
              color = RFColors.IconBackground,
              shape = CircleShape
            )
            .border(1.dp, RFColors.IconBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          // inner clipped area that crops the image to a circle and zooms it slightly
          val zoom = 1.25f
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(6.dp) // optional inner padding to keep border visible
              .clip(CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Image(
              painter = getPetIcon(petType),
              contentDescription = "$petType icon",
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = zoom; scaleY = zoom },
              contentScale = ContentScale.Crop
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = petName,
              fontSize = 16.sp,
              color = RFColors.TextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .background(color = RFColors.BadgeBackground, shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(text = owner, fontSize = 12.sp, color = RFColors.TextSecondary)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (debuffs.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Text(text = "Debuffs:", fontSize = 12.sp, color = RFColors.TextTertiary)
              Spacer(modifier = Modifier.width(8.dp))
              val displayed = debuffs.take(3)
              displayed.forEachIndexed { idx, d ->
                Box(
                  modifier = Modifier
                    .background(color = RFColors.DebuffBadgeBackground, shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .then(if (idx > 0) Modifier.padding(start = 6.dp) else Modifier)
                ) {
                  Text(text = d, fontSize = 11.sp, color = RFColors.AccentRedMuted)
                }
              }
              if (debuffs.size > 3) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "+${debuffs.size - 3}", fontSize = 12.sp, color = RFColors.TextTertiary)
              }
            }
          } else {
            Text(text = petType, fontSize = 12.sp, color = RFColors.TextDisabled)
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${damage.humanReadableAbbreviation()}",
            fontSize = 18.sp,
            color = RFColors.dpsOrange
          )
          Text(text = "Companion dmg", fontSize = 11.sp, color = RFColors.TextTertiary)
        }
      }
    }
  }
}


@Preview
@Composable
fun PetListItemPreview() {
  val sampleDebuffs = listOf("Clinging Flame", "Dragon Roar", "Dragon Flap")
  PetListItem(
    petName = "Fluffy",
    owner = "Reoky",
    damage = 11239081L,
    debuffs = sampleDebuffs,
    petType = "green_dragon",
    modifier = Modifier.padding(8.dp),
    onClick = { /* preview click */ }
  )
}