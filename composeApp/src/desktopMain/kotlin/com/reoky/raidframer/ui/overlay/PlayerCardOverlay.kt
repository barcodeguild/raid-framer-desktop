// kotlin
package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.GroupSpec
import com.reoky.raidframer.ui.component.graphs.MultiPlayerMetricLineChart
import com.reoky.raidframer.core.helpers.pickNextColor
import com.reoky.raidframer.ui.component.PlayerDetailsSection
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.graphs_trend_graph
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
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.ln
import kotlin.math.pow

// Temporary placeholder for resources
fun str(s: String): String = s

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

@OptIn(InternalResourceApi::class)
@Composable
fun PlayerCardOverlay(wm: WindowManager? = null) {

  val currentPlayer by AppState.selectedPlayer.collectAsState()
  val metricType by AppState.selectedMetricType.collectAsState()

  val currentDateString = java.time.LocalDate.now().let {
    "${it.monthValue}/${it.dayOfMonth}"
  }
  val defaultColor = pickNextColor(setOf())

  Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.90f))) {
    TitleBarComponent(
      title = "PlayerCard for $currentPlayer (${
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

        Divider(color = Color.DarkGray, thickness = 1.dp)

        card?.let { card ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {

            Spacer(modifier = Modifier.height(8.dp))

            // Recent Damage, Heals, Debuffs
            Row(modifier = Modifier.fillMaxWidth().height(300.dp)) {
              // Damage
              EventListColumn(
                title = str("Recent Damage"),
                items = card.recentDamageEvents.take(200),
                modifier = Modifier.weight(1f)
              ) { evt ->
                RowItemWithTime(evt.timestamp) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF4FC3F7))) { append(evt.spell) } // Light Blue
                    append("(")
                    withStyle(SpanStyle(color = Color(0xFF29B6F6))) { append(evt.spellId.toString()) } // Blue
                    append(")")
                    append(" -> ")
                    withStyle(SpanStyle(color = Color(0xFFEF5350))) { append(evt.target) } // Red
                    append(": ")
                    withStyle(SpanStyle(color = Color.White)) { append(evt.damage.toString()) }
                  })
                }
              }

              // Heals
              EventListColumn(
                title = str("Recent Heals"),
                items = card.recentHealEvents.take(200).sortedByDescending { it.timestamp },
                modifier = Modifier.weight(1f)
              ) { evt ->
                RowItemWithTime(evt.timestamp) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF66BB6A))) { append(evt.spell) } // Green
                    append("(")
                    withStyle(SpanStyle(color = Color(0xFF43A047))) { append(evt.spellId.toString()) } // Dark Green
                    append(")")
                    append(" -> ")
                    withStyle(SpanStyle(color = Color(0xFFA5D6A7))) { append(evt.target) } // Light Green
                    append(": ")
                    withStyle(SpanStyle(color = Color.White)) { append(evt.amount.toString()) }
                  })
                }
              }

              // Debuffs
              EventListColumn(
                title = str("Debuffs Applied"),
                items = card.recentDebuffAppliedEvents.take(200).sortedByDescending { it.timestamp },
                modifier = Modifier.weight(1f)
              ) { evt ->
                RowItemWithTime(evt.timestamp) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFAB47BC))) { append(evt.debuff) } // Purple
                    append("(")
                    withStyle(SpanStyle(color = Color(0xFF8E24AA))) { append(evt.debuffId.toString()) } // Dark Purple
                    append(")")
                    append(" -> ")
                    withStyle(SpanStyle(color = Color.White)) { append(evt.target) }
                  })
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ROW 2: Buffs, Items, K/D
            Row(modifier = Modifier.fillMaxWidth().height(300.dp)) {
              EventListColumn(
                title = str("Buffs Applied"),
                items = card.recentBuffAppliedEvents.take(200).sortedByDescending { it.timestamp },
                modifier = Modifier.weight(1f)
              ) { evt ->
                RowItemWithTime(evt.timestamp) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFFFCA28))) { append(evt.buff) } // Amber
                    append("(")
                    withStyle(SpanStyle(color = Color(0xFFFFA000))) { append(evt.buffId.toString()) } // Dark Amber
                    append(")")
                    append(" -> ")
                    withStyle(SpanStyle(color = Color.White)) { append(evt.target) }
                  })
                }
              }

              // Item Uses
              // Explicitly map usage to Pairs for clarity
              val skillsSorted: List<Pair<Long, String>> = card.recentSkillItemUsages
                .sortedByDescending { it.first } // Triple.first is the timestamp
                .take(50)
                .map { triple ->
                  triple.first to "${stringResource(triple.second)} -> ${triple.third}"
                }

              EventListColumn(
                title = str("Item Uses"),
                items = skillsSorted,
                modifier = Modifier.weight(1f)
              ) { item ->
                RowItemWithTime(item.first) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF29B6F6))) { append(item.second) } // Light Blue
                  })
                }
              }

              // K/D
              // Use Triple(Timestamp, Type, Name) literally just merging these together because there's no field that has both
              val kills: List<Triple<Long, String, String>> = card.recentKills.map {
                Triple(it.key, "Killed", it.value)
              }
              val deaths: List<Triple<Long, String, String>> = card.recentKilledBys.map {
                Triple(it.key, "Killed By", it.value)
              }

              val kdSorted = (kills + deaths)
                .sortedByDescending { it.first }
                .take(50)

              EventListColumn(
                title = str("Recent K/D"),
                items = kdSorted,
                modifier = Modifier.weight(1f)
              ) { item ->
                val (timestamp, type, name) = item
                RowItemWithTime(timestamp) {
                  append(buildAnnotatedString {
                    val color = if (type == "Killed") Color.Green else Color.Red
                    withStyle(SpanStyle(color = color)) { append(type) }
                    append(" ")
                    withStyle(SpanStyle(color = Color.White)) { append(name) }
                  })
                }
              }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Player Details Section
            Row(Modifier.fillMaxWidth()) {
              PlayerDetailsSection(
                card = card,
                onLeadershipChange = { newLeadership ->
                  PlayerCacheInteractor.updatePlayerLeadershipFor(playerName, newLeadership)
                }
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Totals Row
            Row(modifier = Modifier.fillMaxWidth()) {

              // ~~~ Session Totals ~~~
              Column(modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .background(
                  Color.DarkGray.copy(alpha = 0.3f),
                  shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
              ) {
                Text(
                  text = str("Session Totals"),
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  modifier = Modifier.padding(bottom = 8.dp)
                )
                StatRow(stringResource(Res.string.player_card_stat_damage), card.sessionDamageTotal)
                StatRow(stringResource(Res.string.player_card_stat_healing), card.sessionHealTotal)
                StatRow(stringResource(Res.string.player_card_stat_cc), card.sessionCCTotal.toLong())
                StatRow(stringResource(Res.string.player_card_stat_debuffs), card.sessionDebuffTotal.toLong())
                StatRow(stringResource(Res.string.player_card_stat_charms), card.sessionCharmTotal.toLong())
                StatRow(stringResource(Res.string.player_card_stat_distress), card.sessionDistressTotal.toLong())
                StatRow(stringResource(Res.string.player_card_stat_silence), card.sessionSilenceTotal.toLong())
                StatRow(stringResource(Res.string.player_card_stat_glider), card.sessionGliderTotal.toLong())
                StatRow(stringResource(Res.string.player_card_stat_items), card.sessionItemSkillTotal.toLong())
                StatRow(stringResource(Res.string.player_card_stat_potions), card.sessionPotionTotal.toLong())
                StatRow(stringResource(Res.string.player_card_stat_kills_most_damage), card.sessionKillTotal.toLong())
                StatRow(stringResource(Res.string.player_card_stat_kills_killing_blow), card.sessionKillTotalKB.toLong())
                StatRow(stringResource(Res.string.player_card_stat_total_damage_taken), card.sessionDamageTakenTotal.toLong())
                StatRow(stringResource(Res.string.player_card_stat_total_heals_received), card.sessionHealsReceivedTotal.toLong())
              }

              Spacer(modifier = Modifier.width(32.dp))

              // ~~~ Lifetime Totals ~~~
              Column(modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .background(
                  Color.DarkGray.copy(alpha = 0.3f),
                  shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
              ) {
                Text(
                  text = str("Lifetime Totals"),
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  modifier = Modifier.padding(bottom = 8.dp)
                )
                val cache = card.cache
                if (cache != null) {
                  StatRow(stringResource(Res.string.player_card_stat_damage), cache.lifetimeTotalDamage)
                  StatRow(stringResource(Res.string.player_card_stat_healing), cache.lifetimeTotalHealing)
                  StatRow(stringResource(Res.string.player_card_stat_cc), cache.lifetimeTotalCCDelivered)
                  StatRow(stringResource(Res.string.player_card_stat_debuffs), cache.lifetimeTotalDebuffsApplied)
                  StatRow(stringResource(Res.string.player_card_stat_charms), cache.lifetimeTotalCharms)
                  StatRow(stringResource(Res.string.player_card_stat_distress), cache.lifetimeTotalDistresses)
                  StatRow(stringResource(Res.string.player_card_stat_silence), cache.lifetimeTotalSilences)
                  StatRow(stringResource(Res.string.player_card_stat_glider), cache.lifetimeTotalGliderUses)
                  StatRow(stringResource(Res.string.player_card_stat_items), cache.lifetimeTotalItemSkillsUsed)
                  StatRow(stringResource(Res.string.player_card_stat_potions), cache.lifetimeTotalPotionUsages)
                  StatRow(stringResource(Res.string.player_card_stat_kills_most_damage), cache.lifetimeTotalKills)
                  StatRow(stringResource(Res.string.player_card_stat_kills_killing_blow), cache.lifetimeTotalKillsKB)
                  StatRow(stringResource(Res.string.player_card_stat_total_damage_taken), cache.lifetimeTotalDamageTaken)
                  StatRow(stringResource(Res.string.player_card_stat_total_heals_received), cache.lifetimeTotalHealsReceived)
                } else {
                  Text("No cached data available.", color = Color.Gray)
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

// Helper to render rows with timestamp
@Composable
fun RowItemWithTime(timestamp: Long, content: AnnotatedString.Builder.() -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = formatTime(timestamp),
      color = Color.Gray,
      fontSize = 10.sp,
      modifier = Modifier.width(50.dp)
    )
    Text(
      text = buildAnnotatedString { content() },
      fontSize = 12.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

// Generic Column List Component
@Composable
fun <T> EventListColumn(
  title: String,
  items: List<T>,
  modifier: Modifier = Modifier,
  renderItem: @Composable (T) -> Unit
) {
  Column(modifier = modifier.padding(4.dp)) {
    Text(
      text = title,
      color = Color.LightGray,
      fontWeight = FontWeight.Bold,
      fontSize = 14.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    )

    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
      ) {
        if (items.isEmpty()) {
          Text("-", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        } else {
          items.forEach { item ->
            renderItem(item)
          }
        }
      }
    }
  }
}

@Composable
fun StatRow(label: String, value: Long) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, color = Color.LightGray, fontSize = 13.sp)
    Text(text = value.humanReadableAbbreviation(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
  }
}

private fun formatTime(ts: Long): String {
  val sdf = SimpleDateFormat("HH:mm:ss")
  return sdf.format(Date(ts))
}

