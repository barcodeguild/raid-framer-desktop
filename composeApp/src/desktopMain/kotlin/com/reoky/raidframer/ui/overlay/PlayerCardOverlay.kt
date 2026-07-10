// kotlin
package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.RFGraphColor
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.GraphMetricType
import com.reoky.raidframer.ui.component.graphs.GroupSpec
import com.reoky.raidframer.ui.component.graphs.MultiPlayerMetricLineChart
import com.reoky.raidframer.ui.component.PlayerDetailsSection
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.graphs_trend_graph
import raid_framer_desktop.composeapp.generated.resources.player_card_damage_by_skill
import raid_framer_desktop.composeapp.generated.resources.player_card_heals_by_skill
import raid_framer_desktop.composeapp.generated.resources.player_card_cc_by_skill
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_buffs
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_cc
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_charms
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_damage
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_debuffs
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_distress
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_glider
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_healing
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_items
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_kills_killing_blow
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_kills_most_damage
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_potions
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_silence
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_total_damage_taken
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_total_heals_received
import raid_framer_desktop.composeapp.generated.resources.player_card_no_cached_data
import raid_framer_desktop.composeapp.generated.resources.player_card_recent_buffs
import raid_framer_desktop.composeapp.generated.resources.player_card_recent_damage
import raid_framer_desktop.composeapp.generated.resources.player_card_recent_debuffs
import raid_framer_desktop.composeapp.generated.resources.player_card_recent_heals
import raid_framer_desktop.composeapp.generated.resources.player_card_recent_item_uses
import raid_framer_desktop.composeapp.generated.resources.player_card_recent_kd_short
import raid_framer_desktop.composeapp.generated.resources.player_card_session_totals
import raid_framer_desktop.composeapp.generated.resources.player_card_lifetime_totals
import raid_framer_desktop.composeapp.generated.resources.player_card_title_format
import java.text.SimpleDateFormat
import java.util.Date

private enum class SortOrder(val indicator: String) {
  DESC("▼"), ASC("▲"), NONE("");
}

@Preview
@Composable
fun PreviewPlayerCardOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    PlayerCardOverlay()
  }
}

@OptIn(InternalResourceApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PlayerCardOverlay(wm: WindowManager? = null) {

  val currentPlayer by AppState.selectedPlayer.collectAsState()
  val metricType by AppState.selectedMetricType.collectAsState()

  val currentDateString = java.time.LocalDate.now().let {
    "${it.monthValue}/${it.dayOfMonth}"
  }
  val defaultColor = remember(metricType) {
    when (metricType) {
      GraphMetricType.DAMAGE -> RFGraphColor.RED
      GraphMetricType.HEALING -> RFGraphColor.GREEN
      GraphMetricType.CC -> RFGraphColor.CYAN
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.66f))) {
    TitleBarComponent(
      title = stringResource(Res.string.player_card_title_format, currentPlayer ?: "") + " (${
        defaultColor.name.lowercase().capitalize(Locale.current)
      }) - ${metricType.displayName} ${stringResource(Res.string.graphs_trend_graph)} (${currentDateString})",
      onClose = { wm?.closeWindow(OverlayType.PLAYER_CARD) }
    )

    currentPlayer?.let { playerName ->
      val card by PlayerCacheInteractor.observeCard(playerName).collectAsState()
      Column(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
      ) {
        // Top Graph
        MultiPlayerMetricLineChart(
          groups = listOf(
            GroupSpec(
              name = playerName,
              filter = { it.name == playerName },
              color = defaultColor.color
            )
          ),
          metricType = metricType,
          smoothing = false,
          smoothingWindow = 1,
          mode = GameMonitorInteractor.currentMode,
          modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .padding(12.dp),
        )

        // No longer looks good having a divider here
        //Divider(color = RFColors.CardBorder, thickness = 1.dp)

        card?.let { card ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {

            // Spacer(modifier = Modifier.height(8.dp))

            // Recent Damage, Heals, Debuffs
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray.copy(alpha = 0.3f))
                .padding(8.dp)
                .height(300.dp)
            ) {
              // Damage
              SortableEventListColumn(
                title = stringResource(Res.string.player_card_recent_damage),
                items = card.recentDamageEvents.take(200),
                defaultSortDescending = true,
                modifier = Modifier.weight(1f)
              ) { evt ->
                RowItemWithTime(evt.timestamp) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF4FC3F7))) { append(evt.spell) }
                    append(" ")
                    withStyle(SpanStyle(color = Color(0xFF29B6F6), fontSize = 10.sp)) { append("(${evt.spellId})") }
                    append(" -> ")
                    withStyle(SpanStyle(color = Color(0xFFEF5350))) { append(evt.target) }
                    append(": ")
                    withStyle(SpanStyle(color = RFColors.TextPrimary, fontWeight = FontWeight.SemiBold)) { append(evt.damage.toString()) }
                  })
                }
              }

              // Heals
              SortableEventListColumn(
                title = stringResource(Res.string.player_card_recent_heals),
                items = card.recentHealEvents.take(200),
                defaultSortDescending = true,
                modifier = Modifier.weight(1f)
              ) { evt ->
                RowItemWithTime(evt.timestamp) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF66BB6A))) { append(evt.spell) }
                    append(" ")
                    withStyle(SpanStyle(color = Color(0xFF43A047), fontSize = 10.sp)) { append("(${evt.spellId})") }
                    append(" -> ")
                    withStyle(SpanStyle(color = Color(0xFFA5D6A7))) { append(evt.target) }
                    append(": ")
                    withStyle(SpanStyle(color = RFColors.TextPrimary, fontWeight = FontWeight.SemiBold)) { append(evt.amount.toString()) }
                  })
                }
              }

              // Debuffs
              SortableEventListColumn(
                title = stringResource(Res.string.player_card_recent_debuffs),
                items = card.recentDebuffAppliedEvents.take(200),
                defaultSortDescending = true,
                modifier = Modifier.weight(1f)
              ) { evt ->
                RowItemWithTime(evt.timestamp) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFAB47BC))) { append(evt.debuff) }
                    append(" ")
                    withStyle(SpanStyle(color = Color(0xFF8E24AA), fontSize = 10.sp)) { append("(${evt.debuffId})") }
                    append(" -> ")
                    withStyle(SpanStyle(color = RFColors.TextPrimary)) { append(evt.target) }
                  })
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buffs, Items, K/D
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray.copy(alpha = 0.3f))
                .padding(8.dp)
                .height(300.dp)
            ) {
              SortableEventListColumn(
                title = stringResource(Res.string.player_card_recent_buffs),
                items = card.recentBuffAppliedEvents.take(200),
                defaultSortDescending = true,
                modifier = Modifier.weight(1f)
              ) { evt ->
                RowItemWithTime(evt.timestamp) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFFFCA28))) { append(evt.buff) }
                    append(" ")
                    withStyle(SpanStyle(color = Color(0xFFFFA000), fontSize = 10.sp)) { append("(${evt.buffId})") }
                    append(" -> ")
                    withStyle(SpanStyle(color = RFColors.TextPrimary)) { append(evt.target) }
                  })
                }
              }

              // Item Uses
              val skillsSorted: List<Pair<Long, String>> = card.recentSkillItemUsages
                .sortedByDescending { it.first }
                .take(50)
                .map { triple ->
                  triple.first to "${stringResource(triple.second)} -> ${triple.third}"
                }

              SortableEventListColumn(
                title = stringResource(Res.string.player_card_recent_item_uses),
                items = skillsSorted,
                defaultSortDescending = true,
                modifier = Modifier.weight(1f)
              ) { item ->
                RowItemWithTime(item.first) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF29B6F6))) { append(item.second) }
                  })
                }
              }

              // K/D
              val kills: List<Triple<Long, String, String>> = card.recentKills.map {
                Triple(it.key, "Killed", it.value)
              }
              val deaths: List<Triple<Long, String, String>> = card.recentKilledBys.map {
                Triple(it.key, "Killed By", it.value)
              }

              val kdSorted = (kills + deaths)
                .sortedByDescending { it.first }
                .take(50)

              SortableEventListColumn(
                title = stringResource(Res.string.player_card_recent_kd_short),
                items = kdSorted,
                defaultSortDescending = true,
                modifier = Modifier.weight(1f)
              ) { item ->
                val (timestamp, type, name) = item
                RowItemWithTime(timestamp) {
                  append(buildAnnotatedString {
                    val color = if (type == "Killed") Color.Green else Color.Red
                    withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append(type) }
                    append(" ")
                    withStyle(SpanStyle(color = RFColors.TextPrimary)) { append(name) }
                  })
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Damage by Skill Section
            val spellDamageSorted = remember(card) {
              card.sessionSpellDamageMap.entries
                .sortedByDescending { it.value }
            }

            if (spellDamageSorted.isNotEmpty()) {
              SectionCard(
                title = stringResource(Res.string.player_card_damage_by_skill),
                accentColor = RFColors.dpsOrange
              ) {
                spellDamageSorted.forEachIndexed { index, entry ->
                  SkillBarRow(
                    rank = index + 1,
                    skillName = entry.key,
                    value = entry.value,
                    maxValue = spellDamageSorted.first().value,
                    valueColor = RFColors.dpsOrange,
                    barColor = RFColors.dpsOrange
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))
            }

            // Heals by Skill Section
            val spellHealSorted = remember(card) {
              card.sessionSpellHealMap.entries
                .sortedByDescending { it.value }
            }

            if (spellHealSorted.isNotEmpty()) {
              SectionCard(
                title = stringResource(Res.string.player_card_heals_by_skill),
                accentColor = RFColors.healsGreen
              ) {
                spellHealSorted.forEachIndexed { index, entry ->
                  SkillBarRow(
                    rank = index + 1,
                    skillName = entry.key,
                    value = entry.value,
                    maxValue = spellHealSorted.first().value,
                    valueColor = RFColors.healsGreen,
                    barColor = RFColors.healsGreen
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))
            }

            // CC by Skill Section
            val spellCCSorted = remember(card) {
              card.sessionSpellCCMap.entries
                .sortedByDescending { it.value }
            }

            if (spellCCSorted.isNotEmpty()) {
              SectionCard(
                title = stringResource(Res.string.player_card_cc_by_skill),
                accentColor = RFColors.ccCyan
              ) {
                spellCCSorted.forEachIndexed { index, entry ->
                  SkillBarRow(
                    rank = index + 1,
                    skillName = entry.key,
                    value = entry.value.toLong(),
                    maxValue = spellCCSorted.first().value.toLong(),
                    valueColor = RFColors.ccCyan,
                    barColor = RFColors.ccCyan
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))
            }

            // Player Details Section
            PlayerDetailsSection(
              card = card,
              onLeadershipChange = { newLeadership ->
                PlayerCacheInteractor.updatePlayerLeadershipFor(playerName, newLeadership)
              }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Totals Row
            Row(modifier = Modifier.fillMaxWidth()) {

              // Session Totals
              SectionCard(
                title = stringResource(Res.string.player_card_session_totals),
                modifier = Modifier.weight(1f),
                accentColor = RFColors.AccentRed
              ) {
                StatRow(stringResource(Res.string.player_card_stat_damage), card.sessionDamageTotal, RFColors.dpsOrange)
                StatRow(stringResource(Res.string.player_card_stat_healing), card.sessionHealTotal, RFColors.healsGreen)
                StatRow(stringResource(Res.string.player_card_stat_cc), card.sessionCCTotal.toLong(), RFColors.ccCyan)
                StatRow(stringResource(Res.string.player_card_stat_buffs), card.sessionBuffTotal.toLong(), RFColors.itemSkillYellow)
                StatRow(stringResource(Res.string.player_card_stat_debuffs), card.sessionDebuffTotal.toLong(), Color(0xFFAB47BC))
                StatRow(stringResource(Res.string.player_card_stat_charms), card.sessionCharmTotal.toLong(), RFColors.charmPink)
                StatRow(stringResource(Res.string.player_card_stat_distress), card.sessionDistressTotal.toLong(), RFColors.distressPurple)
                StatRow(stringResource(Res.string.player_card_stat_silence), card.sessionSilenceTotal.toLong(), RFColors.silencePurple)
                StatRow(stringResource(Res.string.player_card_stat_glider), card.sessionGliderTotal.toLong(), RFColors.gliderBlue)
                StatRow(stringResource(Res.string.player_card_stat_items), card.sessionItemSkillTotal.toLong(), RFColors.itemSkillYellow)
                StatRow(stringResource(Res.string.player_card_stat_potions), card.sessionPotionTotal.toLong(), RFColors.potionTeal)
                StatRow(stringResource(Res.string.player_card_stat_kills_most_damage), card.sessionKillTotal.toLong(), RFColors.dpsOrange)
                StatRow(stringResource(Res.string.player_card_stat_kills_killing_blow), card.sessionKillTotalKB.toLong(), RFColors.killsHaranyaGreen)
                StatRow(stringResource(Res.string.player_card_stat_total_damage_taken), card.sessionDamageTakenTotal.toLong(), Color(0xFFEF5350))
                StatRow(stringResource(Res.string.player_card_stat_total_heals_received), card.sessionHealsReceivedTotal.toLong(), RFColors.healsGreen)
              }

              Spacer(modifier = Modifier.width(16.dp))

              // Lifetime Totals
              SectionCard(
                title = stringResource(Res.string.player_card_lifetime_totals),
                modifier = Modifier.weight(1f),
                accentColor = RFColors.AccentRed
              ) {
                val cache = card.cache
                if (cache != null) {
                  StatRow(stringResource(Res.string.player_card_stat_damage), cache.lifetimeTotalDamage, RFColors.dpsOrange)
                  StatRow(stringResource(Res.string.player_card_stat_healing), cache.lifetimeTotalHealing, RFColors.healsGreen)
                  StatRow(stringResource(Res.string.player_card_stat_cc), cache.lifetimeTotalCCDelivered, RFColors.ccCyan)
                  StatRow(stringResource(Res.string.player_card_stat_buffs), cache.lifetimeTotalBuffsApplied, RFColors.itemSkillYellow)
                  StatRow(stringResource(Res.string.player_card_stat_debuffs), cache.lifetimeTotalDebuffsApplied, Color(0xFFAB47BC))
                  StatRow(stringResource(Res.string.player_card_stat_charms), cache.lifetimeTotalCharms, RFColors.charmPink)
                  StatRow(stringResource(Res.string.player_card_stat_distress), cache.lifetimeTotalDistresses, RFColors.distressPurple)
                  StatRow(stringResource(Res.string.player_card_stat_silence), cache.lifetimeTotalSilences, RFColors.silencePurple)
                  StatRow(stringResource(Res.string.player_card_stat_glider), cache.lifetimeTotalGliderUses, RFColors.gliderBlue)
                  StatRow(stringResource(Res.string.player_card_stat_items), cache.lifetimeTotalItemSkillsUsed, RFColors.itemSkillYellow)
                  StatRow(stringResource(Res.string.player_card_stat_potions), cache.lifetimeTotalPotionUsages, RFColors.potionTeal)
                  StatRow(stringResource(Res.string.player_card_stat_kills_most_damage), cache.lifetimeTotalKills, RFColors.dpsOrange)
                  StatRow(stringResource(Res.string.player_card_stat_kills_killing_blow), cache.lifetimeTotalKillsKB, RFColors.killsHaranyaGreen)
                  StatRow(stringResource(Res.string.player_card_stat_total_damage_taken), cache.lifetimeTotalDamageTaken, Color(0xFFEF5350))
                  StatRow(stringResource(Res.string.player_card_stat_total_heals_received), cache.lifetimeTotalHealsReceived, RFColors.healsGreen)
                } else {
                  Text(stringResource(Res.string.player_card_no_cached_data), color = RFColors.TextDisabled, fontSize = 13.sp)
                }
              }
            }
            Spacer(modifier = Modifier.height(24.dp))
          }
        }
      }
    }
  }
}

// Section card with accent border and background
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SectionCard(
  title: String,
  modifier: Modifier = Modifier,
  accentColor: Color = RFColors.AccentRed,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = modifier
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(Color.DarkGray.copy(alpha = 0.3f))
      .padding(12.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(bottom = 8.dp)
    ) {
      Box(
        modifier = Modifier
          .size(3.dp, 16.dp)
          .background(accentColor, RoundedCornerShape(2.dp))
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = title,
        color = RFColors.TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
      )
    }
    content()
  }
}

// Helper to render rows with timestamp
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RowItemWithTime(timestamp: Long, content: AnnotatedString.Builder.() -> Unit) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  val fullText = remember { buildAnnotatedString { content() } }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 1.dp)
      .clip(RoundedCornerShape(4.dp))
      .background(if (isHovered) Color.White.copy(alpha = 0.05f) else Color.Transparent)
      .hoverable(interactionSource = interactionSource)
      .padding(horizontal = 4.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = formatTime(timestamp),
      color = RFColors.TextTertiary,
      fontSize = 10.sp,
      modifier = Modifier.width(50.dp)
    )
    Text(
      text = fullText,
      fontSize = 12.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
  }

  if (isHovered) {
    Popup(
      alignment = Alignment.TopStart,
      offset = androidx.compose.ui.unit.IntOffset(x = 20, y = 24)
    ) {
      Surface(
        shape = RoundedCornerShape(4.dp),
        elevation = 4.dp,
        color = Color.Black.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color.Gray)
      ) {
        Text(
          text = fullText,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          fontSize = 11.sp
        )
      }
    }
  }
}

// Sortable Column List Component with clickable header
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> SortableEventListColumn(
  title: String,
  items: List<T>,
  defaultSortDescending: Boolean = true,
  modifier: Modifier = Modifier,
  renderItem: @Composable (T) -> Unit
) {
  var sortOrder by remember { mutableStateOf(if (defaultSortDescending) SortOrder.DESC else SortOrder.NONE) }

  val sortedItems = remember(items, sortOrder) {
    when (sortOrder) {
      SortOrder.DESC -> items
      SortOrder.ASC -> items.reversed()
      SortOrder.NONE -> items
    }
  }

  Column(modifier = modifier) {
    // Header with sort toggle
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(4.dp))
        .clickable {
          sortOrder = when (sortOrder) {
            SortOrder.DESC -> SortOrder.ASC
            SortOrder.ASC -> SortOrder.NONE
            SortOrder.NONE -> SortOrder.DESC
          }
        }
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = title,
        color = RFColors.TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
      )
      if (sortOrder != SortOrder.NONE) {
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = sortOrder.indicator,
          color = RFColors.AccentRed,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
      if (items.isEmpty()) {
        Text("-", color = RFColors.TextDisabled, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize()
        ) {
          itemsIndexed(sortedItems, key = { index, item -> "${title}_${index}_${item.hashCode()}" }) { _, item ->
            renderItem(item)
          }
        }
      }
    }
  }
}

@Composable
private fun SkillBarRow(rank: Int, skillName: String, value: Long, maxValue: Long, valueColor: Color, barColor: Color) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(4.dp))
      .background(if (isHovered) Color.White.copy(alpha = 0.05f) else Color.Transparent)
      .hoverable(interactionSource = interactionSource)
      .padding(horizontal = 4.dp, vertical = 3.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "$rank.",
      color = RFColors.TextTertiary,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.width(24.dp)
    )
    Text(
      text = skillName,
      color = RFColors.TextPrimary,
      fontSize = 12.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
    Spacer(modifier = Modifier.width(8.dp))
    // Mini bar
    val fraction = if (maxValue > 0) value.toFloat() / maxValue else 0f
    Box(
      modifier = Modifier
        .width(60.dp)
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp))
        .background(Color.White.copy(alpha = 0.1f))
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(fraction)
          .clip(RoundedCornerShape(3.dp))
          .background(barColor)
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = value.humanReadableAbbreviation(),
      color = valueColor,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.End,
      modifier = Modifier.width(50.dp)
    )
  }
}

@Composable
fun StatRow(label: String, value: Long, valueColor: Color = RFColors.TextPrimary) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, color = RFColors.TextSecondary, fontSize = 13.sp)
    Text(text = value.humanReadableAbbreviation(), color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
  }
}

private fun formatTime(ts: Long): String {
  val sdf = SimpleDateFormat("HH:mm:ss")
  return sdf.format(Date(ts))
}
