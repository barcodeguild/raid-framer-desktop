package com.reoky.raidframer.ui.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.reoky.raidframer.core.helpers.getPetIcon
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.contrastTextColor
import com.reoky.raidframer.core.model.RiderCastEvent
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.pet_cast_fallback_format
import raid_framer_desktop.composeapp.generated.resources.pet_cast_overflow_format
import raid_framer_desktop.composeapp.generated.resources.pet_companion_dmg_label
import raid_framer_desktop.composeapp.generated.resources.pet_damage_label_format
import raid_framer_desktop.composeapp.generated.resources.pet_debuffs_label
import raid_framer_desktop.composeapp.generated.resources.pet_icon_desc_format
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PetListItem(
  petName: String,
  owner: String,
  damage: Long,
  debuffs: List<String>,
  petTypes: Set<String> = setOf("default"),
  breathCasts: List<RiderCastEvent> = listOf(),
  rocketCasts: List<RiderCastEvent> = listOf(),
  ownerFactionColor: Color = RFColors.BadgeBackground,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  val primaryType = petTypes.firstOrNull() ?: "default"
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
        // Icon with subtle red border (hoverable for multi-type tooltip)
        val iconInteraction = remember { MutableInteractionSource() }
        val isIconHovered by iconInteraction.collectIsHoveredAsState()
        Box(
          modifier = Modifier
            .size(48.dp)
            .background(
              color = RFColors.IconBackground,
              shape = CircleShape
            )
            .border(1.dp, RFColors.IconBorder, CircleShape)
            .hoverable(interactionSource = iconInteraction),
          contentAlignment = Alignment.Center
        ) {
          val zoom = 1.25f
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(6.dp)
              .clip(CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Image(
              painter = getPetIcon(primaryType),
              contentDescription = stringResource(Res.string.pet_icon_desc_format, primaryType),
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = zoom; scaleY = zoom },
              contentScale = ContentScale.Crop
            )
          }

          // Multi-type tooltip
          if (isIconHovered && petTypes.size > 1) {
            Popup(
              alignment = Alignment.TopStart,
              offset = IntOffset(x = 56, y = 0)
            ) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                elevation = 4.dp,
                color = RFColors.PopupBackground.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, RFColors.CardBorder)
              ) {
                Column(modifier = Modifier.padding(8.dp)) {
                  petTypes.forEach { type ->
                    Text(
                      text = type,
                      color = RFColors.TextSecondary,
                      fontSize = 10.sp
                    )
                  }
                }
              }
            }
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
                .background(color = ownerFactionColor, shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(text = owner, fontSize = 12.sp, color = ownerFactionColor.contrastTextColor())
            }
            if (breathCasts.isNotEmpty() || rocketCasts.isNotEmpty()) {
              Spacer(modifier = Modifier.width(6.dp))
              breathCasts.forEachIndexed { idx, cast ->
                CastEmoji(
                  emoji = cast.emoji,
                  castIndex = idx + 1,
                  cast = cast
                )
              }
              rocketCasts.forEachIndexed { idx, cast ->
                CastEmoji(
                  emoji = "\uD83D\uDE80",
                  castIndex = idx + 1,
                  cast = cast
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (debuffs.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Text(text = stringResource(Res.string.pet_debuffs_label), fontSize = 12.sp, color = RFColors.TextTertiary)
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
            Text(text = primaryType, fontSize = 12.sp, color = RFColors.TextDisabled)
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${damage.humanReadableAbbreviation()}",
            fontSize = 18.sp,
            color = RFColors.dpsOrange
          )
          Text(text = stringResource(Res.string.pet_companion_dmg_label), fontSize = 11.sp, color = RFColors.TextTertiary)
        }
      }
    }
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CastEmoji(emoji: String, castIndex: Int, cast: RiderCastEvent) {
  val emojiInteraction = remember { MutableInteractionSource() }
  val isEmojiHovered by emojiInteraction.collectIsHoveredAsState()
  val popupInteraction = remember { MutableInteractionSource() }
  val isPopupHovered by popupInteraction.collectIsHoveredAsState()
  val showTooltip = isEmojiHovered || isPopupHovered
  val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

  Box(
    modifier = Modifier.hoverable(interactionSource = emojiInteraction)
  ) {
    Text(text = emoji, fontSize = 14.sp)

    if (showTooltip) {
      Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(x = 16, y = 24)
      ) {
        Surface(
          shape = RoundedCornerShape(4.dp),
          elevation = 4.dp,
          color = RFColors.PopupBackground.copy(alpha = 0.95f),
          border = BorderStroke(1.dp, RFColors.CardBorder),
          modifier = Modifier.hoverable(interactionSource = popupInteraction)
        ) {
          Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).widthIn(max = 220.dp)) {
            Text(
              text = cast.spellName.ifEmpty { stringResource(Res.string.pet_cast_fallback_format, castIndex) },
              color = RFColors.TextPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = stringResource(Res.string.pet_damage_label_format, cast.damage.humanReadableAbbreviation()),
                color = RFColors.dpsOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = timeFormat.format(Date(cast.timestamp)),
                color = RFColors.TextTertiary,
                fontSize = 10.sp
              )
            }

            // Damage by target breakdown
            if (cast.damageByTarget.isNotEmpty()) {
              Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
              Spacer(modifier = Modifier.height(3.dp))
              val sortedTargets = cast.damageByTarget.entries.sortedByDescending { it.value }
              val maxTargetDamage = sortedTargets.first().value
              sortedTargets.take(20).forEach { (target, dmg) ->
                val pct = if (maxTargetDamage > 0) dmg.toFloat() / maxTargetDamage else 0f
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = target,
                    color = RFColors.TextSecondary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                  )
                  Box(
                    modifier = Modifier
                      .width(32.dp)
                      .height(3.dp)
                      .clip(RoundedCornerShape(2.dp))
                      .background(Color.White.copy(alpha = 0.1f))
                  ) {
                    Box(
                      modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = pct)
                        .clip(RoundedCornerShape(2.dp))
                        .background(RFColors.dpsOrange.copy(alpha = 0.7f))
                    )
                  }
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = dmg.humanReadableAbbreviation(),
                    color = RFColors.dpsOrange,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
              if (sortedTargets.size > 20) {
                Text(
                  text = stringResource(Res.string.pet_cast_overflow_format, sortedTargets.size - 20),
                  color = RFColors.TextTertiary,
                  fontSize = 8.sp,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}


@Preview
@Composable
fun PetListItemPreview() {
  val sampleDebuffs = listOf("Clinging Flame", "Dragon Roar", "Dragon Flap")
  val sampleBreathCasts = listOf(
    RiderCastEvent(timestamp = System.currentTimeMillis(), damage = 5420L, spellName = "Red Dragon's Breath (Rider)", emoji = "\uD83D\uDD25"),
    RiderCastEvent(timestamp = System.currentTimeMillis(), damage = 3100L, spellName = "Thunderbreath (Rider)", emoji = "\u2744\uFE0F")
  )
  PetListItem(
    petName = "Fluffy",
    owner = "Reoky",
    damage = 11239081L,
    debuffs = sampleDebuffs,
    petTypes = setOf("green_dragon", "Typhoon Drake"),
    breathCasts = sampleBreathCasts,
    ownerFactionColor = Color.Red.copy(alpha = 0.75f),
    modifier = Modifier.padding(8.dp),
    onClick = { /* preview click */ }
  )
}
