// kotlin
package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.foundation.Image
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
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
import com.reoky.raidframer.core.database.PlayerSessionTotalsEntity
import com.reoky.raidframer.core.database.unpackItemUsageCounter
import com.reoky.raidframer.core.database.unpackItemUsageDate
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.definitions.GliderSpell
import com.reoky.raidframer.core.definitions.ItemSpell
import com.reoky.raidframer.core.helpers.RFColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import com.reoky.raidframer.core.helpers.RFGraphColor
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.helpers.timeAgo
import com.reoky.raidframer.core.helpers.resolveLocalizedString
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.SessionStatRows
import com.reoky.raidframer.ui.component.SessionTotals
import com.reoky.raidframer.ui.component.StatRow
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.GraphMetricType
import com.reoky.raidframer.ui.component.graphs.GroupSpec
import com.reoky.raidframer.ui.component.graphs.MultiPlayerMetricLineChart
import com.reoky.raidframer.ui.component.PlayerDetailsSection
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.crystal_wings
import raid_framer_desktop.composeapp.generated.resources.glider_name_bd_glider
import raid_framer_desktop.composeapp.generated.resources.glider_name_crystal_wings
import raid_framer_desktop.composeapp.generated.resources.glider_name_feathered_dragon
import raid_framer_desktop.composeapp.generated.resources.glider_name_moonshadow
import raid_framer_desktop.composeapp.generated.resources.glider_name_ravenspine
import raid_framer_desktop.composeapp.generated.resources.glider_name_rocket_wings
import raid_framer_desktop.composeapp.generated.resources.glider_name_sky_emperor
import raid_framer_desktop.composeapp.generated.resources.glider_name_twt
import raid_framer_desktop.composeapp.generated.resources.graphs_trend_graph
import raid_framer_desktop.composeapp.generated.resources.halcy_neck
import raid_framer_desktop.composeapp.generated.resources.honor_nodachi
import raid_framer_desktop.composeapp.generated.resources.item_name_halcy_neck
import raid_framer_desktop.composeapp.generated.resources.item_name_honor_nodachi
import raid_framer_desktop.composeapp.generated.resources.item_name_jola_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_kraken_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_kraken_scepter
import raid_framer_desktop.composeapp.generated.resources.item_name_kraken_spear
import raid_framer_desktop.composeapp.generated.resources.item_name_lib_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_library_greatclub
import raid_framer_desktop.composeapp.generated.resources.item_name_soul_neck
import raid_framer_desktop.composeapp.generated.resources.jola_shield
import raid_framer_desktop.composeapp.generated.resources.kraken_glider
import raid_framer_desktop.composeapp.generated.resources.kraken_scepter
import raid_framer_desktop.composeapp.generated.resources.kraken_spear
import raid_framer_desktop.composeapp.generated.resources.kraken_shield
import raid_framer_desktop.composeapp.generated.resources.lib_shield
import raid_framer_desktop.composeapp.generated.resources.library_greatclub
import raid_framer_desktop.composeapp.generated.resources.moonshadow_glider
import raid_framer_desktop.composeapp.generated.resources.player_card_damage_by_skill
import raid_framer_desktop.composeapp.generated.resources.player_card_heals_by_skill
import raid_framer_desktop.composeapp.generated.resources.player_card_cc_by_skill
import raid_framer_desktop.composeapp.generated.resources.player_card_no_historical_data
import raid_framer_desktop.composeapp.generated.resources.player_card_session_scope_all
import raid_framer_desktop.composeapp.generated.resources.player_card_session_scope_current
import raid_framer_desktop.composeapp.generated.resources.player_card_session_scope_last_n
import raid_framer_desktop.composeapp.generated.resources.player_card_session_scope_previous
import raid_framer_desktop.composeapp.generated.resources.player_card_totals_scope_label
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
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_tiger_strikes
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_freezes
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_trips
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_bubbles
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_bracings
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_shield_strip
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_weapon_disables
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_potion_disables
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_bd_glider
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_crystal_wings
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_glider_disables
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_provoked
import raid_framer_desktop.composeapp.generated.resources.player_card_no_cached_data
import raid_framer_desktop.composeapp.generated.resources.player_card_recent_buffs
import raid_framer_desktop.composeapp.generated.resources.player_card_recent_damage
import raid_framer_desktop.composeapp.generated.resources.player_card_recent_debuffs
import raid_framer_desktop.composeapp.generated.resources.player_card_recent_heals
import raid_framer_desktop.composeapp.generated.resources.player_card_recent_item_uses
import raid_framer_desktop.composeapp.generated.resources.player_card_recent_kd_short
import raid_framer_desktop.composeapp.generated.resources.player_card_lifetime_totals
import raid_framer_desktop.composeapp.generated.resources.player_card_lifetime_totals_sessions
import raid_framer_desktop.composeapp.generated.resources.player_card_title_format
import raid_framer_desktop.composeapp.generated.resources.player_inventory_title
import raid_framer_desktop.composeapp.generated.resources.player_inventory_last_used
import raid_framer_desktop.composeapp.generated.resources.player_inventory_uses
import raid_framer_desktop.composeapp.generated.resources.player_inventory_never
import raid_framer_desktop.composeapp.generated.resources.player_inventory_controls_hint
import raid_framer_desktop.composeapp.generated.resources.ravenspine_glider
import raid_framer_desktop.composeapp.generated.resources.rocket_glider
import raid_framer_desktop.composeapp.generated.resources.sky_emp_glider
import raid_framer_desktop.composeapp.generated.resources.soul_neck
import raid_framer_desktop.composeapp.generated.resources.twt_glider
import raid_framer_desktop.composeapp.generated.resources.bd_glider
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_deep_tranquility
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_deepend_debuff
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_throw_dagger
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_stuns
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_staggers
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_petrification
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_absorb_lifeforce
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_corrosive_barrage
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_blinded_by_crows
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_mist_sunder
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_regular_sunder
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_impale_immunity
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_protective_wings
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_courageous_action
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_mana_barrier
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_revive
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_defiance
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_garden_defiance
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_purges
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_sac_dances
import raid_framer_desktop.composeapp.generated.resources.player_inventory_time_ago
import raid_framer_desktop.composeapp.generated.resources.time_ago_just_now
import raid_framer_desktop.composeapp.generated.resources.time_ago_minutes_one
import raid_framer_desktop.composeapp.generated.resources.time_ago_minutes_other
import raid_framer_desktop.composeapp.generated.resources.time_ago_hours_one
import raid_framer_desktop.composeapp.generated.resources.time_ago_hours_other
import raid_framer_desktop.composeapp.generated.resources.time_ago_days_one
import raid_framer_desktop.composeapp.generated.resources.time_ago_days_other
import raid_framer_desktop.composeapp.generated.resources.time_ago_weeks_one
import raid_framer_desktop.composeapp.generated.resources.time_ago_weeks_other
import raid_framer_desktop.composeapp.generated.resources.time_ago_months_one
import raid_framer_desktop.composeapp.generated.resources.time_ago_months_other
import java.text.SimpleDateFormat
import java.util.Date

private enum class SortOrder(val indicator: String) {
  DESC("▼"), ASC("▲"), NONE("");
}

private val WellShape = RoundedCornerShape(8.dp)
private val WellColor = RFColors.CardBackground.copy(alpha = 0.72f)
private val WellBorder = RFColors.CardBorder

// Selects which set of session totals the "Session" totals card should show.
// CURRENT reads from the in-memory PlayerCard; everything else reads from
// the player_session_totals table aggregated by the PlayerCacheInteractor.
// `limit` is the max number of archived sessions to aggregate (null = unbounded).
private enum class SessionScope(
  val isCurrent: Boolean = false,
  val limit: Int? = null
) {
  CURRENT(isCurrent = true),
  PREVIOUS(limit = 1),
  LAST_2(limit = 2),
  LAST_3(limit = 3),
  LAST_5(limit = 5),
  ALL(limit = null);
}

// View-model-agnostic shape so the totals card can render either the in-memory
// session or an aggregated historical one without branching on the source.
// Moved to SessionTotalsComponent.kt as shared SessionTotals.

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

  var playerSessionCount by remember { mutableStateOf(0) }
  LaunchedEffect(currentPlayer) {
    val name = currentPlayer
    if (!name.isNullOrBlank()) {
      playerSessionCount = PlayerCacheInteractor.getSessionCountForPlayer(name)
    }
  }

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

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Divider(
            modifier = Modifier.weight(1f),
            color = RFColors.CardBorder,
            thickness = 1.dp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier = Modifier
              .width(28.dp)
              .height(2.dp)
              .clip(RoundedCornerShape(1.dp))
              .background(RFColors.AccentRed)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Divider(
            modifier = Modifier.weight(1f),
            color = RFColors.CardBorder,
            thickness = 1.dp
          )
        }

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
                .padding(horizontal = 8.dp)
                .clip(WellShape)
                .background(WellColor)
                .border(1.dp, WellBorder, WellShape)
                .padding(12.dp)
                .heightIn(min = 100.dp, max = 300.dp)
            ) {
              // Damage
              SortableEventListColumn(
                title = stringResource(Res.string.player_card_recent_damage),
                items = card.recentDamageEvents.sortedByDescending { it.timestamp }.take(500),
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
                items = card.recentHealEvents.sortedByDescending { it.timestamp }.take(500),
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
                items = card.recentDebuffAppliedEvents.sortedByDescending { it.timestamp }.take(500),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Buffs, Items, K/D
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(WellShape)
                .background(WellColor)
                .border(1.dp, WellBorder, WellShape)
                .padding(12.dp)
                .heightIn(min = 100.dp, max = 300.dp)
            ) {
              SortableEventListColumn(
                title = stringResource(Res.string.player_card_recent_buffs),
                items = card.recentBuffAppliedEvents.sortedByDescending { it.timestamp }.take(500),
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

            // Player Inventory Section
            PlayerInventorySection(card)

            // Player Details Section
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(WellShape)
                .background(WellColor)
                .border(1.dp, WellBorder, WellShape)
                .padding(12.dp)
            ) {
              PlayerDetailsSection(
                card = card,
                onLeadershipChange = { newLeadership ->
                  PlayerCacheInteractor.updatePlayerLeadershipFor(playerName, newLeadership)
                }
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Totals Row
            var selectedScope by remember(playerName) { mutableStateOf(SessionScope.CURRENT) }
            // Fetch historical data once and pass to the totals card so both columns can
            // share the load (and we don't double-query on a state change).
            val historical by produceState<PlayerSessionTotalsEntity?>(
              initialValue = null,
              key1 = playerName,
              key2 = selectedScope
            ) {
              if (selectedScope.isCurrent) {
                value = null
              } else {
                value = PlayerCacheInteractor.getHistoricalTotalsForPlayer(playerName, selectedScope.limit)
              }
            }
            val sessionTotals: SessionTotals? = when {
              selectedScope.isCurrent -> SessionTotals.fromPlayerCard(card)
              else -> historical?.let { SessionTotals.fromEntity(it) }
            }

            // Filters bar above the totals row. Hosts the SessionScope dropdown so the two
            // totals cards below stay visually symmetric; the dropdown's selection also
            // drives the title of the left (manipulated) totals card.
            TotalsFiltersBar(
              selectedScope = selectedScope,
              onScopeChange = { selectedScope = it },
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {

              // Manipulated (left) totals card. Title is dynamic so the user can tell
              // which scope they're looking at without the dropdown being inside the card.
              SectionCard(
                title = sessionScopeLabel(selectedScope),
                modifier = Modifier.weight(1f),
                accentColor = RFColors.AccentRed
              ) {
                if (sessionTotals == null) {
                  Text(
                    text = stringResource(Res.string.player_card_no_historical_data),
                    color = RFColors.TextDisabled,
                    fontSize = 13.sp
                  )
                } else {
                  SessionStatRows(sessionTotals)
                }
              }

              Spacer(modifier = Modifier.width(16.dp))

              // Lifetime Totals
              SectionCard(
                title = stringResource(Res.string.player_card_lifetime_totals_sessions, playerSessionCount),
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
                  StatRow(stringResource(Res.string.player_card_stat_tiger_strikes), cache.lifetimeTotalTigerStrikes, RFColors.techNoTigerStrikes)
                  StatRow(stringResource(Res.string.player_card_stat_freezes), cache.lifetimeTotalFreezes, RFColors.freezeIceBlue)
                  StatRow(stringResource(Res.string.player_card_stat_trips), cache.lifetimeTotalTrips, RFColors.tripsAmber)
                  StatRow(stringResource(Res.string.player_card_stat_bubbles), cache.lifetimeTotalBubbles, RFColors.bubblesCyan)
                  StatRow(stringResource(Res.string.player_card_stat_bracings), cache.lifetimeTotalBracings, RFColors.bracingsGreen)
                  StatRow(stringResource(Res.string.player_card_stat_shield_strip), cache.lifetimeTotalShieldStrip, RFColors.shieldStripOrange)
                  StatRow(stringResource(Res.string.player_card_stat_weapon_disables), cache.lifetimeTotalWeaponDisables, RFColors.weaponDisablesRed)
                  StatRow(stringResource(Res.string.player_card_stat_potion_disables), cache.lifetimeTotalPotionDisables, RFColors.potionDisablesPurple)
                  StatRow(stringResource(Res.string.player_card_stat_bd_glider), cache.lifetimeTotalBdGlider, RFColors.bdGliderTeal)
                  StatRow(stringResource(Res.string.player_card_stat_crystal_wings), cache.lifetimeTotalCrystalWings, RFColors.crystalWingsBlue)
                  StatRow(stringResource(Res.string.player_card_stat_glider_disables), cache.lifetimeTotalGliderDisables, RFColors.gliderDisablesPink)
                  StatRow(stringResource(Res.string.player_card_stat_provoked), cache.lifetimeTotalProvoked, RFColors.provokesDeepPurple)
                  StatRow(stringResource(Res.string.player_card_stat_defiance), cache.lifetimeTotalDefiance, RFColors.defianceGold)
                  StatRow(stringResource(Res.string.player_card_stat_garden_defiance), cache.lifetimeTotalGardenDefiance, RFColors.gardenDefianceBlue)
                  StatRow(stringResource(Res.string.player_card_stat_purges), cache.lifetimeTotalPurges, RFColors.purgeGreen)
                  StatRow(stringResource(Res.string.player_card_stat_sac_dances), cache.lifetimeTotalSacDances, RFColors.sacDancePurple)
                  StatRow(stringResource(Res.string.player_card_stat_deep_tranquility), cache.lifetimeTotalDeepTranquility, RFColors.deepTranquilityTeal)
                  StatRow(stringResource(Res.string.player_card_stat_deepend_debuff), cache.lifetimeTotalDeependDebuff, RFColors.deedendDebuffRed)
                  StatRow(stringResource(Res.string.player_card_stat_throw_dagger), cache.lifetimeTotalThrowDagger, RFColors.throwDaggerAmber)
                  StatRow(stringResource(Res.string.player_card_stat_stuns), cache.lifetimeTotalStuns, RFColors.stunDeepRed)
                  StatRow(stringResource(Res.string.player_card_stat_staggers), cache.lifetimeTotalStaggers, RFColors.staggerBrown)
                  StatRow(stringResource(Res.string.player_card_stat_petrification), cache.lifetimeTotalPetrification, RFColors.petrificationGray)
                  StatRow(stringResource(Res.string.player_card_stat_absorb_lifeforce), cache.lifetimeTotalAbsorbLifeforce, RFColors.absorbLifeforceMagenta)
                  StatRow(stringResource(Res.string.player_card_stat_corrosive_barrage), cache.lifetimeTotalCorrosiveBarrage, RFColors.corrosiveBarrageLime)
                  StatRow(stringResource(Res.string.player_card_stat_blinded_by_crows), cache.lifetimeTotalBlindedByCrows, RFColors.blindedByCrowsDark)
                  StatRow(stringResource(Res.string.player_card_stat_mist_sunder), cache.lifetimeTotalMistSunder, RFColors.mistSunderCyan)
                  StatRow(stringResource(Res.string.player_card_stat_regular_sunder), cache.lifetimeTotalRegularSunder, RFColors.regularSunderOrange)
                  StatRow(stringResource(Res.string.player_card_stat_impale_immunity), cache.lifetimeTotalImpaleImmunity, RFColors.impaleImmunitySteel)
                  StatRow(stringResource(Res.string.player_card_stat_protective_wings), cache.lifetimeTotalProtectiveWings, RFColors.protectiveWingsGold)
                  StatRow(stringResource(Res.string.player_card_stat_courageous_action), cache.lifetimeTotalCourageousAction, RFColors.courageousActionBright)
                  StatRow(stringResource(Res.string.player_card_stat_mana_barrier), cache.lifetimeTotalManaBarrier, RFColors.manaBarrierBlue)
                  StatRow(stringResource(Res.string.player_card_stat_revive), cache.lifetimeTotalRevive, RFColors.reviveGhostWhite)
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
            Spacer(modifier = Modifier.height(12.dp))
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
      .padding(horizontal = 8.dp)
      .clip(WellShape)
      .background(WellColor)
      .border(1.dp, WellBorder, WellShape)
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
        fontSize = 13.sp
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

// StatRow moved to SessionTotalsComponent.kt as shared composable.

private fun formatTime(ts: Long): String {
  val sdf = SimpleDateFormat("HH:mm:ss")
  return sdf.format(Date(ts))
}

// Localized label for a session scope. Used by both the dropdown's selected display and its menu rows.
@Composable
private fun sessionScopeLabel(scope: SessionScope): String = when (scope) {
  SessionScope.CURRENT -> stringResource(Res.string.player_card_session_scope_current)
  SessionScope.PREVIOUS -> stringResource(Res.string.player_card_session_scope_previous)
  SessionScope.LAST_2 -> stringResource(Res.string.player_card_session_scope_last_n, 2)
  SessionScope.LAST_3 -> stringResource(Res.string.player_card_session_scope_last_n, 3)
  SessionScope.LAST_5 -> stringResource(Res.string.player_card_session_scope_last_n, 5)
  SessionScope.ALL -> stringResource(Res.string.player_card_session_scope_all)
}

// Compact dropdown styled to match SessionTypeDropdown. Picking a new scope bubbles up via
// onSelected; the parent decides which data source to drive the totals card with.
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SessionScopeDropdown(
  selected: SessionScope,
  onSelected: (SessionScope) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = modifier
  ) {
    TextField(
      value = sessionScopeLabel(selected),
      onValueChange = {},
      readOnly = true,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
      singleLine = true,
      colors = TextFieldDefaults.textFieldColors(
        textColor = RFColors.TextPrimary,
        backgroundColor = Color(0xFF1E1E1E),
        focusedIndicatorColor = RFColors.AccentRed,
        unfocusedIndicatorColor = RFColors.CardBorder,
        cursorColor = RFColors.AccentRed
      ),
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
      },
      textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
      maxLines = 1
    )

    MaterialTheme(
      colors = MaterialTheme.colors.copy(surface = RFColors.CardBackground)
    ) {
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
          .width(210.dp)
          .border(1.dp, RFColors.CardBorder, RoundedCornerShape(8.dp))
      ) {
        SessionScope.entries.forEach { scope ->
          DropdownMenuItem(
            onClick = {
              onSelected(scope)
              expanded = false
            },
            content = {
              Text(
                text = sessionScopeLabel(scope),
                color = if (scope == selected) RFColors.AccentRed else RFColors.TextPrimary,
                fontWeight = if (scope == selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1
              )
            }
          )
        }
      }
    }
  }
}

// Slim bar that sits above the totals row and hosts the SessionScope dropdown.
// Lifting the dropdown out of the totals cards keeps the two cards below
// visually symmetric; the dropdown's selection drives the title of the left
// (manipulated) card so the user can still tell which scope they're viewing.
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
private fun TotalsFiltersBar(
  selectedScope: SessionScope,
  onScopeChange: (SessionScope) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.padding(horizontal = 8.dp),
    shape = WellShape,
    color = WellColor,
    border = BorderStroke(1.dp, WellBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = stringResource(Res.string.player_card_totals_scope_label),
          color = RFColors.TextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = stringResource(Res.string.player_inventory_controls_hint),
          color = RFColors.TextTertiary,
          fontSize = 11.sp
        )
      }

      SessionScopeDropdown(
        selected = selectedScope,
        onSelected = onScopeChange,
        modifier = Modifier.width(210.dp)
      )
    }
  }
}

// SessionStatRows moved to SessionTotalsComponent.kt as shared composable.

// Data class representing a single inventory item (glider or utility item) for display
private data class InventoryItem(
  val nameRes: StringResource,
  val iconRes: DrawableResource,
  val usageCount: Long,
  val lastUsedDate: Date?,
  val usedInSession: Boolean
)

// Player Inventory Section - shows gliders and utility items the player has used
@Composable
private fun PlayerInventorySection(card: PlayerCard) {
  val cache = card.cache ?: return
  val lastSessionStart by remember { derivedStateOf { RFConfig.state.value.lastSessionStart } }
  val previousSessionStart by remember { derivedStateOf { RFConfig.state.value.previousSessionStart } }

  // Build inventory items from all glider and utility item enum entries
  val allItems = remember(cache, lastSessionStart, previousSessionStart) {
    fun buildItem(nameRes: StringResource, iconRes: DrawableResource, packed: Long): InventoryItem {
      val lastUsed = packed.unpackItemUsageDate()
      val sessionStart = if (lastSessionStart > 0L) lastSessionStart else previousSessionStart
      val usedInSession = sessionStart > 0L && lastUsed.time >= sessionStart
      return InventoryItem(
        nameRes = nameRes,
        iconRes = iconRes,
        usageCount = packed.unpackItemUsageCounter(),
        lastUsedDate = lastUsed,
        usedInSession = usedInSession
      )
    }
    val gliderItems = GliderSpell.entries.map { glider ->
      buildItem(glider.friendlyNameRes, glider.iconRes, glider.packedUsageField(cache))
    }
    val utilityItems = ItemSpell.entries.map { item ->
      buildItem(item.friendlyNameRes, item.iconRes, item.packedUsageField(cache))
    }
    (gliderItems + utilityItems)
      .filter { it.usageCount > 0 }
      .sortedByDescending { it.usageCount }
  }

  if (allItems.isEmpty()) return

  SectionCard(
    title = stringResource(Res.string.player_inventory_title),
    accentColor = RFColors.gliderBlue
  ) {
    // Render items in a FlowRow-like layout (wrapped rows)
    val chunked = allItems.chunked(6) // 6 items per row
    chunked.forEach { rowItems ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        rowItems.forEach { item ->
          InventoryItemCard(item, Modifier.weight(1f))
        }
        // Fill remaining space in the row with empty boxes if needed
        repeat(6 - rowItems.size) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun InventoryItemCard(item: InventoryItem, modifier: Modifier = Modifier) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  val dateFormat = remember { SimpleDateFormat("MMM d, yyyy") }
  val itemName = stringResource(item.nameRes)
  val sessionHighlight = item.usedInSession

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(if (isHovered) Color.White.copy(alpha = 0.05f) else Color.Transparent)
      .hoverable(interactionSource = interactionSource)
      .padding(6.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Icon (48x48)
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color.Black.copy(alpha = 0.3f))
        .border(
          width = if (sessionHighlight) 2.dp else 1.dp,
          color = if (sessionHighlight) RFColors.graphNodeAllied else WellBorder,
          shape = RoundedCornerShape(8.dp)
        ),
      contentAlignment = Alignment.Center
    ) {
      Image(
        painter = painterResource(item.iconRes),
        contentDescription = itemName,
        modifier = Modifier.size(40.dp),
        contentScale = ContentScale.Fit
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Name
    Text(
      text = itemName,
      color = if (sessionHighlight) RFColors.graphNodeAllied else RFColors.TextPrimary,
      fontSize = 10.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )

    // Usage count
    Text(
      text = stringResource(Res.string.player_inventory_uses, item.usageCount),
      color = if (sessionHighlight) RFColors.graphNodeAllied.copy(alpha = 0.8f) else RFColors.TextTertiary,
      fontSize = 9.sp,
      textAlign = TextAlign.Center
    )

    // Last used date
    val lastUsedText = if (item.lastUsedDate != null && item.lastUsedDate.time > 0) {
      dateFormat.format(item.lastUsedDate)
    } else {
      stringResource(Res.string.player_inventory_never)
    }
    Text(
      text = stringResource(Res.string.player_inventory_last_used, lastUsedText),
      color = RFColors.TextTertiary,
      fontSize = 9.sp,
      textAlign = TextAlign.Center
    )

    // Time ago
    val timeAgoResult = if (item.lastUsedDate != null && item.lastUsedDate.time > 0) {
      item.lastUsedDate.time.timeAgo()
    } else null
    if (timeAgoResult != null) {
      val timeAgoText = timeAgoResult.resolveLocalizedString()
      Text(
        text = stringResource(Res.string.player_inventory_time_ago, timeAgoText),
        color = RFColors.TextDisabled,
        fontSize = 8.sp,
        textAlign = TextAlign.Center
      )
    }
  }
}
