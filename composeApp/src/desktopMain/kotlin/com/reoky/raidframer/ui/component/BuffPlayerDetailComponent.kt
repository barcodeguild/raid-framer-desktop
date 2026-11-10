package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import com.reoky.raidframer.core.definitions.RaidBuffKey
import com.reoky.raidframer.core.definitions.RaidBuffRequirements
import com.reoky.raidframer.core.definitions.lootBuffById
import com.reoky.raidframer.core.definitions.matches
import com.reoky.raidframer.core.definitions.matchedDefinitions
import com.reoky.raidframer.core.definitions.missingKeys
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.serialization.BuffPayload
import com.reoky.raidframer.core.serialization.RaidFramePayload
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.raid_buff_blessed_elixir
import raid_framer_desktop.composeapp.generated.resources.raid_buff_ancients_potion
import raid_framer_desktop.composeapp.generated.resources.raid_buff_chocolate
import raid_framer_desktop.composeapp.generated.resources.raid_buff_cookfire
import raid_framer_desktop.composeapp.generated.resources.raid_buff_dahutas_bubble
import raid_framer_desktop.composeapp.generated.resources.raid_buff_egg_of_fortune
import raid_framer_desktop.composeapp.generated.resources.raid_buff_feast_ribs
import raid_framer_desktop.composeapp.generated.resources.raid_buff_faction_war_time
import raid_framer_desktop.composeapp.generated.resources.raid_buff_fairy_protection
import raid_framer_desktop.composeapp.generated.resources.raid_buff_gallants_blessing
import raid_framer_desktop.composeapp.generated.resources.raid_buff_goblet
import raid_framer_desktop.composeapp.generated.resources.raid_buff_golden_taffy
import raid_framer_desktop.composeapp.generated.resources.raid_buff_guild_buff
import raid_framer_desktop.composeapp.generated.resources.raid_buff_guild_buff_covers_all
import raid_framer_desktop.composeapp.generated.resources.raid_buff_has_buffs
import raid_framer_desktop.composeapp.generated.resources.raid_buff_hunting_elixir
import raid_framer_desktop.composeapp.generated.resources.raid_buff_jinhui_wish
import raid_framer_desktop.composeapp.generated.resources.raid_buff_loot_buffs
import raid_framer_desktop.composeapp.generated.resources.raid_buff_loot_cake
import raid_framer_desktop.composeapp.generated.resources.raid_buff_longing
import raid_framer_desktop.composeapp.generated.resources.raid_buff_missing_buffs
import raid_framer_desktop.composeapp.generated.resources.raid_buff_monster_hunters_dream
import raid_framer_desktop.composeapp.generated.resources.raid_buff_moonlight_juice
import raid_framer_desktop.composeapp.generated.resources.raid_buff_none_found
import raid_framer_desktop.composeapp.generated.resources.raid_buff_not_passed
import raid_framer_desktop.composeapp.generated.resources.raid_buff_passed
import raid_framer_desktop.composeapp.generated.resources.raid_buff_red_flower_fruit
import raid_framer_desktop.composeapp.generated.resources.raid_buff_blue_flower_fruit
import raid_framer_desktop.composeapp.generated.resources.raid_buff_secret_gift
import raid_framer_desktop.composeapp.generated.resources.raid_buff_shortbread_cookie
import raid_framer_desktop.composeapp.generated.resources.raid_buff_statue
import raid_framer_desktop.composeapp.generated.resources.raid_buff_war_drum
import raid_framer_desktop.composeapp.generated.resources.raid_buff_whisper
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_player_not_scannable

private const val GUILD_BUFF_ID = 9002633

/** Localized label for every raid buff definition, shared by the Buffs tab and Buff History tab. */
@Composable
fun localizedBuffLabels(): Map<RaidBuffKey, String> = mapOf(
  RaidBuffKey.GOBLET to stringResource(Res.string.raid_buff_goblet),
  RaidBuffKey.FEAST_RIBS to stringResource(Res.string.raid_buff_feast_ribs),
  RaidBuffKey.LONGING to stringResource(Res.string.raid_buff_longing),
  RaidBuffKey.WHISPER to stringResource(Res.string.raid_buff_whisper),
  RaidBuffKey.BLESSED_ELIXIR to stringResource(Res.string.raid_buff_blessed_elixir),
  RaidBuffKey.ANCIENTS_POTION to stringResource(Res.string.raid_buff_ancients_potion),
  RaidBuffKey.JINHUI_WISH to stringResource(Res.string.raid_buff_jinhui_wish),
  RaidBuffKey.SECRET_GIFT to stringResource(Res.string.raid_buff_secret_gift),
  RaidBuffKey.FAIRY_PROTECTION to stringResource(Res.string.raid_buff_fairy_protection),
  RaidBuffKey.COOKFIRE to stringResource(Res.string.raid_buff_cookfire),
  RaidBuffKey.WAR_DRUM to stringResource(Res.string.raid_buff_war_drum),
  RaidBuffKey.DAHUTAS_BUBBLE to stringResource(Res.string.raid_buff_dahutas_bubble),
  RaidBuffKey.MONSTER_HUNTERS_DREAM to stringResource(Res.string.raid_buff_monster_hunters_dream),
  RaidBuffKey.RED_FLOWER_FRUIT to stringResource(Res.string.raid_buff_red_flower_fruit),
  RaidBuffKey.BLUE_FLOWER_FRUIT to stringResource(Res.string.raid_buff_blue_flower_fruit),
  RaidBuffKey.STATUE_BUFF to stringResource(Res.string.raid_buff_statue),
  RaidBuffKey.FACTION_WAR_TIME to stringResource(Res.string.raid_buff_faction_war_time),
  RaidBuffKey.GALLANTS_BLESSING to stringResource(Res.string.raid_buff_gallants_blessing),
  RaidBuffKey.MOONLIGHT_JUICE to stringResource(Res.string.raid_buff_moonlight_juice),
  RaidBuffKey.HUNTING_ELIXIR to stringResource(Res.string.raid_buff_hunting_elixir),
  RaidBuffKey.CHOCOLATE to stringResource(Res.string.raid_buff_chocolate),
  RaidBuffKey.LOOT_CAKE to stringResource(Res.string.raid_buff_loot_cake),
  RaidBuffKey.SHORTBREAD_COOKIE to stringResource(Res.string.raid_buff_shortbread_cookie),
  RaidBuffKey.GOLDEN_TAFFY to stringResource(Res.string.raid_buff_golden_taffy),
  RaidBuffKey.EGG_OF_FORTUNE to stringResource(Res.string.raid_buff_egg_of_fortune)
)

/** Places a popup to the right of [anchor], flipping to its left side when it would overflow the window. */
fun buffDetailPopupPositionProvider(anchor: IntOffset): PopupPositionProvider =
  object : PopupPositionProvider {
    override fun calculatePosition(
      anchorBounds: IntRect,
      windowSize: IntSize,
      layoutDirection: LayoutDirection,
      popupContentSize: IntSize
    ): IntOffset {
      val gap = 16
      val rightX = anchor.x + gap
      val leftX = anchor.x - popupContentSize.width - gap
      val maxX = (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8)
      val x = if (rightX <= maxX) rightX else leftX.coerceIn(8, maxX)
      val y = (anchor.y + gap)
        .coerceIn(8, (windowSize.height - popupContentSize.height - 8).coerceAtLeast(8))
      return IntOffset(x, y)
    }
  }

/**
 * Player buff details shown when a raid frame (Buffs tab) or a snapshot player (Buff History tab)
 * is clicked: which required buffs they had, which they were missing, and their loot buff breakdown.
 *
 * @param buffIds the buffs the player had at the point in time being inspected
 * @param notScannable renders a "not scannable" notice instead of buff sections
 * @param footerText optional footer line (e.g. scan freshness or snapshot time)
 * @param onClose optional close button handler
 */
@Composable
fun BuffPlayerDetailPane(
  playerName: String,
  buffIds: Set<Int>,
  requirements: RaidBuffRequirements,
  allowGuildBuff: Boolean,
  modifier: Modifier = Modifier,
  notScannable: Boolean = false,
  footerText: String? = null,
  onClose: (() -> Unit)? = null
) {
  val labels = localizedBuffLabels()
  val member = RaidFramePayload(playerName = playerName, buffs = buffIds.map { BuffPayload(buff_id = it) })
  val hasGuildBuff = allowGuildBuff && GUILD_BUFF_ID in buffIds
  val displayName = if (playerName.length > 26) playerName.take(26) + "\u2026" else playerName

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(displayName, color = RFColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        if (!notScannable) {
          val meetsRequirements = requirements.matches(member, allowGuildBuff)
          Text(
            if (meetsRequirements) "(${stringResource(Res.string.raid_buff_passed)})"
            else "(${stringResource(Res.string.raid_buff_not_passed)})",
            color = if (meetsRequirements) Color(0xFF7CFF8A) else Color(0xFFFF7777),
            fontSize = 10.sp
          )
        }
      }
      onClose?.let { close ->
        IconButton(onClick = close, modifier = Modifier.size(22.dp)) {
          Text("X", color = RFColors.TextSecondary, fontSize = 11.sp)
        }
      }
    }
    Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

    if (notScannable) {
      Text(
        stringResource(Res.string.raid_buff_history_player_not_scannable),
        color = RFColors.TextTertiary,
        fontSize = 11.sp
      )
    } else {
      Text(
        stringResource(Res.string.raid_buff_has_buffs),
        color = Color(0xFF7CFF8A),
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp
      )
      if (hasGuildBuff) {
        Text(
          labels[RaidBuffKey.GALLANTS_BLESSING].orEmpty() + " (" + stringResource(Res.string.raid_buff_guild_buff) + ")",
          color = RFColors.TextSecondary,
          fontSize = 11.sp
        )
      } else {
        Text(
          requirements.matchedDefinitions(member, allowGuildBuff)
            .joinToString(", ") { labels[it.key].orEmpty() }
            .ifBlank { stringResource(Res.string.raid_buff_none_found) },
          color = RFColors.TextSecondary,
          fontSize = 11.sp
        )
      }

      Text(
        stringResource(Res.string.raid_buff_missing_buffs),
        color = Color(0xFFFF7777),
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 4.dp)
      )
      if (hasGuildBuff) {
        Text(
          stringResource(Res.string.raid_buff_guild_buff_covers_all),
          color = RFColors.TextSecondary,
          fontSize = 11.sp
        )
      } else {
        Text(
          requirements.missingKeys(member, allowGuildBuff)
            .mapNotNull { labels[it] }
            .joinToString(", ")
            .ifBlank { stringResource(Res.string.raid_buff_none_found) },
          color = RFColors.TextSecondary,
          fontSize = 11.sp
        )
      }

      Text(
        stringResource(Res.string.raid_buff_loot_buffs),
        color = RFColors.lootBuffColor,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 4.dp)
      )
      val playerLootBuffs = buffIds.mapNotNull { lootBuffById(it) }
        .filter { it.lootPercent > 0 }
        .sortedByDescending { it.lootPercent }
      if (playerLootBuffs.isEmpty()) {
        Text(stringResource(Res.string.raid_buff_none_found), color = RFColors.TextSecondary, fontSize = 11.sp)
      } else {
        val maxLootAmount = playerLootBuffs.first().lootPercent.coerceAtLeast(1)
        playerLootBuffs.forEach { lootBuff ->
          Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
              lootBuff.name,
              color = RFColors.TextSecondary,
              fontSize = 11.sp,
              maxLines = 1,
              modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.width(44.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.1f))) {
              Box(
                modifier = Modifier
                  .fillMaxHeight()
                  .fillMaxWidth((lootBuff.lootPercent.toFloat() / maxLootAmount).coerceIn(0f, 1f))
                  .clip(RoundedCornerShape(3.dp))
                  .background(RFColors.lootBuffColor)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "${lootBuff.lootPercent}%",
              color = RFColors.lootBuffColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.width(38.dp)
            )
          }
        }
      }
    }

    footerText?.let {
      Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
      Text(it, color = RFColors.TextTertiary, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
    }
  }
}
