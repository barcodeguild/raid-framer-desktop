// kotlin
package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.GroupSpec
import com.reoky.raidframer.ui.component.graphs.MultiPlayerMetricLineChart
import com.reoky.raidframer.core.helpers.pickNextColor
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.graphs_trend_graph
import java.text.SimpleDateFormat
import java.util.Date

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
            .height(350.dp)
            .padding(12.dp),
        )

        Divider(color = Color.DarkGray, thickness = 1.dp)

        card?.let { card ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {

            // ROW 1: Recent Damage, Heals, Debuffs
            Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
              // Damage
              EventListColumn(
                title = str("Recent Damage"),
                items = card.recentDamageEvents.take(50),
                modifier = Modifier.weight(1f)
              ) { evt ->
                RowItemWithTime(evt.timestamp) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF4FC3F7))) { append(evt.spell) } // Light Blue
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
                items = card.recentHealEvents.take(50),
                modifier = Modifier.weight(1f)
              ) { evt ->
                RowItemWithTime(evt.timestamp) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF66BB6A))) { append(evt.spell) } // Green
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
                items = card.recentDebuffAppliedEvents.take(50),
                modifier = Modifier.weight(1f)
              ) { evt ->
                RowItemWithTime(evt.timestamp) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFAB47BC))) { append(evt.debuff) } // Purple
                    append(" -> ")
                    withStyle(SpanStyle(color = Color.White)) { append(evt.target) }
                  })
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ROW 2: Buffs, Items, K/D
            Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
              // Buffs
              EventListColumn(
                title = str("Buffs Applied"),
                items = card.recentBuffAppliedEvents.take(50),
                modifier = Modifier.weight(1f)
              ) { evt ->
                RowItemWithTime(evt.timestamp) {
                  append(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFFFCA28))) { append(evt.buff) } // Amber
                    append(" -> ")
                    withStyle(SpanStyle(color = Color.White)) { append(evt.target) }
                  })
                }
              }

              // Item Uses
              // Explicitly map usage to Pairs for clarity
              val skillsSorted = card.recentSkillItemUsages.toList()
                .sortedByDescending { it.first }
                .take(50)

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
              // Use Triple(Timestamp, Type, Name)
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

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(16.dp))

            // Row 3: Totals
            Row(modifier = Modifier.fillMaxWidth()) {
              // Session Totals
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = str("Session Totals"),
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  modifier = Modifier.padding(bottom = 8.dp)
                )
                StatRow(str("Total Damage"), card.sessionDamageTotal)
                StatRow(str("Total Healing"), card.sessionHealTotal)
                StatRow(str("CC Delivered"), card.sessionCCTotal.toLong())
                StatRow(str("Debuffs"), card.sessionDebuffTotal.toLong())
                StatRow(str("Charms"), card.sessionCharmTotal.toLong())
                StatRow(str("Distresses"), card.sessionDistressTotal.toLong())
                StatRow(str("Silences"), card.sessionSilenceTotal.toLong())
                StatRow(str("Glider Uses"), card.sessionGliderTotal.toLong())
                StatRow(str("Item Uses"), card.sessionItemSkillTotal.toLong())
                StatRow(str("Potions"), card.sessionPotionTotal.toLong())
                StatRow(str("Kills"), card.sessionKillTotal.toLong())
                StatRow(str("Deaths"), card.sessionDeathTotal.toLong())
              }

              Spacer(modifier = Modifier.width(32.dp))

              // Lifetime Totals
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = str("Lifetime Totals"),
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  modifier = Modifier.padding(bottom = 8.dp)
                )
                val cache = card.cache
                if (cache != null) {
                  StatRow(str("Total Damage"), cache.lifetimeTotalDamage)
                  StatRow(str("Total Healing"), cache.lifetimeTotalHealing)
                  StatRow(str("CC Delivered"), cache.lifetimeTotalCCDelivered)
                  StatRow(str("Debuffs"), cache.lifetimeTotalDebuffsApplied)
                  StatRow(str("Charms"), cache.lifetimeTotalCharms)
                  StatRow(str("Distresses"), cache.lifetimeTotalDistresses)
                  StatRow(str("Silences"), cache.lifetimeTotalSilences)
                  StatRow(str("Glider Uses"), cache.lifetimeTotalGliderUses)
                  StatRow(str("Item Uses"), cache.lifetimeTotalItemSkillsUsed)
                  StatRow(str("Kills"), cache.lifetimeTotalKills)
                  StatRow(str("Deaths"), cache.lifetimeTotalDeaths)
                  StatRow(str("Damage Taken"), cache.lifetimeTotalDamageTaken)
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
    Text(text = formatNumber(value), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
  }
}

private fun formatTime(ts: Long): String {
  val sdf = SimpleDateFormat("HH:mm:ss")
  return sdf.format(Date(ts))
}

private fun formatNumber(count: Long): String {
  if (count < 1000) return count.toString()
  val exp = (Math.log(count.toDouble()) / Math.log(1000.0)).toInt()
  return String.format("%.1f%c", count / Math.pow(1000.0, exp.toDouble()), "kMGTPE"[exp - 1])
}
