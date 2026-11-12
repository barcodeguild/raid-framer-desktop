package com.reoky.raidframer.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.*
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.factionHighlightColor
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.model.Faction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.attendance_allow_pve
import raid_framer_desktop.composeapp.generated.resources.attendance_any_events
import raid_framer_desktop.composeapp.generated.resources.attendance_active_participation
import raid_framer_desktop.composeapp.generated.resources.attendance_best_attendance
import raid_framer_desktop.composeapp.generated.resources.attendance_best_cc
import raid_framer_desktop.composeapp.generated.resources.attendance_best_dmg
import raid_framer_desktop.composeapp.generated.resources.attendance_best_heals
import raid_framer_desktop.composeapp.generated.resources.attendance_content_events
import raid_framer_desktop.composeapp.generated.resources.attendance_events_summary
import raid_framer_desktop.composeapp.generated.resources.attendance_month_of
import raid_framer_desktop.composeapp.generated.resources.attendance_month_so_far
import raid_framer_desktop.composeapp.generated.resources.attendance_monthly_cc
import raid_framer_desktop.composeapp.generated.resources.attendance_monthly_damage
import raid_framer_desktop.composeapp.generated.resources.attendance_monthly_heals
import raid_framer_desktop.composeapp.generated.resources.attendance_no_player_selected
import raid_framer_desktop.composeapp.generated.resources.attendance_player_select
import raid_framer_desktop.composeapp.generated.resources.attendance_pvp_only

// Event types that count as "content" when the calendar is filtered down.
private val CONTENT_EVENT_TYPES = setOf(
  "Black Dragon", "Charbydis", "Kraken", "Garden Anthalon",
  "Delphinad Ghost Ship (DGS)", "Jola, Meina, Glenn (JMG)",
  "Siege Calmlands", "Siege Nuimari", "Siege Heedmar", "Siege Marcala",
  "Snake (Nehliya)", "Scrims", "World Bosses"
)

// "Active participation" floors: a session only counts when the player dealt damage,
// healed, or landed CC above these amounts — filters out idle attendees.
private const val ACTIVE_MIN_DAMAGE = 25_000L
private const val ACTIVE_MIN_HEALS = 25_000L
private const val ACTIVE_MIN_CC = 25

enum class AttendancePvpFilter { PVP_ONLY, ALLOW_PVE }

private data class AttendanceDayData(
  val day: Int,
  val sessions: List<PlayerSessionTotalsEntity>,
  val rawAttended: List<PlayerSessionTotalsEntity>,
  val events: List<SessionSummary>,
  val present: Boolean
)

@Composable
fun AttendanceCalendarTab() {
  // Observed, not snapshotted: the config may settle after this window is first
  // composed, so the "own character" default has to track it live.
  val config by RFConfig.state.collectAsState()
  var selectedPlayerOverride by remember { mutableStateOf<String?>(null) }
  val selectedPlayer = selectedPlayerOverride ?: config.playerName.takeIf { it.isNotBlank() }

  var contentOnly by remember { mutableStateOf(false) }
  var pvpFilter by remember { mutableStateOf(AttendancePvpFilter.PVP_ONLY) }
  var activeOnly by remember { mutableStateOf(false) }
  val now = remember { LocalDate.now() }
  var viewYear by remember { mutableStateOf(now.year) }
  var viewMonth by remember { mutableStateOf(now.monthValue) }
  var bestOpen by remember { mutableStateOf(false) }
  var bestMode by remember { mutableStateOf(0) } // 0=dmg, 1=heals, 2=cc
  var bestData by remember { mutableStateOf<List<PlayerAttendanceRank>>(emptyList()) }

  val month = YearMonth.of(viewYear, viewMonth)
  val monthStart = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
  val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
  val isCurrentMonth = month == YearMonth.from(now)

  // The selected player's sessions for the viewed month
  val playerSessions by produceState<List<PlayerSessionTotalsEntity>>(
    initialValue = emptyList(), key1 = selectedPlayer, key2 = viewYear, key3 = viewMonth
  ) {
    value = if (selectedPlayer.isNullOrBlank()) emptyList()
    else try {
      withContext(Dispatchers.IO) {
        RFDao.playerSessionDao.getSessionsForPlayerInRange(selectedPlayer, monthStart, monthEnd)
      }
    } catch (e: Exception) { emptyList() }
  }

  // Distinct session starts across all players (denominator for attendance %)
  val monthSessions by produceState<List<SessionSummary>>(
    initialValue = emptyList(), key1 = viewYear, key2 = viewMonth
  ) {
    value = try {
      withContext(Dispatchers.IO) { RFDao.playerSessionDao.getDistinctSessionsInRange(monthStart, monthEnd) }
    } catch (e: Exception) { emptyList() }
  }

  val filteredSessions = remember(playerSessions, contentOnly, pvpFilter, activeOnly) {
    playerSessions.filter { s ->
      (!contentOnly || s.sessionType.isBlank() || CONTENT_EVENT_TYPES.any { s.sessionType.contains(it, ignoreCase = true) }) &&
        (pvpFilter == AttendancePvpFilter.ALLOW_PVE || s.totalDamage > 0 || s.totalHealing > 0 || s.totalCC > 0) &&
        (!activeOnly || s.totalDamage >= ACTIVE_MIN_DAMAGE || s.totalHealing >= ACTIVE_MIN_HEALS || s.totalCC >= ACTIVE_MIN_CC)
    }
  }

  val dayData = remember(playerSessions, filteredSessions, monthSessions, month) {
    val zone = ZoneId.systemDefault()
    val dayOf: (Long) -> Int = { Instant.ofEpochMilli(it).atZone(zone).dayOfMonth }
    val attendedByDay = filteredSessions.groupBy { dayOf(it.sessionStart) }
    val rawAttendedByDay = playerSessions.groupBy { dayOf(it.sessionStart) }
    val eventsByDay = monthSessions.groupBy { dayOf(it.sessionStart) }
    List(month.lengthOfMonth()) { d ->
      val attended = attendedByDay[d + 1].orEmpty()
      AttendanceDayData(
        day = d + 1,
        sessions = attended,
        rawAttended = rawAttendedByDay[d + 1].orEmpty(),
        events = eventsByDay[d + 1].orEmpty(),
        present = attended.isNotEmpty()
      )
    }
  }

  val attendancePct = if (monthSessions.isEmpty()) 0 else filteredSessions.size * 100 / monthSessions.size
  val sortedBest = remember(bestData, bestMode) {
    bestData.sortedWith(
      compareByDescending<PlayerAttendanceRank> { it.sessionCount }
        .thenByDescending { if (bestMode == 1) it.totalHealing else if (bestMode == 2) it.totalCC.toLong() else it.totalDamage }
    ).take(25)
  }

  LaunchedEffect(bestOpen, activeOnly) {
    if (bestOpen) {
      bestData = try {
        withContext(Dispatchers.IO) {
          RFDao.playerSessionDao.getPlayerLifetimeAttendanceStats(activeOnly, ACTIVE_MIN_DAMAGE, ACTIVE_MIN_HEALS, ACTIVE_MIN_CC)
        }
      } catch (e: Exception) { emptyList() }
    }
  }

  val monthLabel = if (isCurrentMonth) stringResource(Res.string.attendance_month_so_far)
  else stringResource(Res.string.attendance_month_of, month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()))

  Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
    // Player search + session filters
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      PlayerSearchBox(
        currentName = selectedPlayer ?: "",
        onSelect = { selectedPlayerOverride = it },
        onClear = { selectedPlayerOverride = null },
        modifier = Modifier.weight(1f),
        placeholder = stringResource(Res.string.attendance_player_select),
        highlightBorder = selectedPlayer.isNullOrBlank()
      )
      AttendanceSegmentedToggle(
        options = listOf(
          stringResource(Res.string.attendance_any_events) to false,
          stringResource(Res.string.attendance_content_events) to true
        ),
        selected = contentOnly,
        onSelect = { contentOnly = it },
        modifier = Modifier.width(190.dp)
      )
      AttendanceSegmentedToggle(
        options = listOf(
          stringResource(Res.string.attendance_pvp_only) to AttendancePvpFilter.PVP_ONLY,
          stringResource(Res.string.attendance_allow_pve) to AttendancePvpFilter.ALLOW_PVE
        ),
        selected = pvpFilter,
        onSelect = { pvpFilter = it },
        modifier = Modifier.width(210.dp)
      )
      // Optional participation floor — hides idle attendees from the calendar and rankings
      Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
          checked = activeOnly,
          onCheckedChange = { activeOnly = it },
          colors = CheckboxDefaults.colors(
            checkedColor = RFColors.AccentRed,
            uncheckedColor = Color.White,
            checkmarkColor = Color.White
          ),
          modifier = Modifier.size(32.dp)
        )
        Text(
          stringResource(Res.string.attendance_active_participation),
          color = RFColors.TextSecondary,
          fontSize = 12.sp,
          modifier = Modifier.padding(start = 4.dp)
        )
      }
    }

    Spacer(Modifier.height(10.dp))

    // Month label + stats on the left, month navigation on the right
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
          Text(monthLabel, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          Text(
            stringResource(Res.string.attendance_events_summary, filteredSessions.size, monthSessions.size, attendancePct),
            color = RFColors.TextSecondary,
            fontSize = 12.sp
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          AttendanceStatItem(stringResource(Res.string.attendance_monthly_damage), filteredSessions.sumOf { it.totalDamage }.humanReadableAbbreviation(), RFColors.dpsOrange)
          AttendanceStatItem(stringResource(Res.string.attendance_monthly_heals), filteredSessions.sumOf { it.totalHealing }.humanReadableAbbreviation(), RFColors.healsGreen)
          AttendanceStatItem(stringResource(Res.string.attendance_monthly_cc), filteredSessions.sumOf { it.totalCC }.toLong().humanReadableAbbreviation(), RFColors.ccCyan)
        }
        // Best Attendance lives in the header row, next to the monthly stats
        AttendanceChip(stringResource(Res.string.attendance_best_attendance)) { bestOpen = !bestOpen }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { month.minusMonths(1).let { viewYear = it.year; viewMonth = it.monthValue } }, modifier = Modifier.height(32.dp)) {
          Text("\u25C0", color = Color.White, fontSize = 13.sp)
        }
        // The reset control keeps its slot (and the arrows' positions) even when hidden
        val resetAlpha by animateFloatAsState(if (isCurrentMonth) 0f else 1f, label = "resetAlpha")
        IconButton(
          onClick = { viewYear = now.year; viewMonth = now.monthValue },
          enabled = !isCurrentMonth,
          modifier = Modifier.height(32.dp).alpha(resetAlpha)
        ) {
          Text("Reset", color = RFColors.AccentRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = { month.plusMonths(1).let { viewYear = it.year; viewMonth = it.monthValue } }, modifier = Modifier.height(32.dp)) {
          Text("\u25B6", color = Color.White, fontSize = 13.sp)
        }
      }
    }

    if (bestOpen) {
      Spacer(Modifier.height(8.dp))
      AttendanceSegmentedToggle(
        options = listOf(
          stringResource(Res.string.attendance_best_dmg) to 0,
          stringResource(Res.string.attendance_best_heals) to 1,
          stringResource(Res.string.attendance_best_cc) to 2
        ),
        selected = bestMode,
        onSelect = { bestMode = it },
        modifier = Modifier.width(360.dp)
      )
      Spacer(Modifier.height(6.dp))
      BestAttendancePanel(sortedBest, bestMode)
      Spacer(Modifier.height(8.dp))
    }

    // Calendar fills the remaining viewport, scaling cells to fit
    if (selectedPlayer.isNullOrBlank()) {
      Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        Text(
          stringResource(Res.string.attendance_no_player_selected),
          color = RFColors.TextTertiary,
          fontSize = 14.sp,
          textAlign = TextAlign.Center
        )
      }
    } else {
      CalendarGrid(
        dayData,
        firstDayOfWeek = month.atDay(1).dayOfWeek.value % 7,
        monthShort = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 4.dp)
      )
    }
  }
}

/** Flat toggle chip (replaces Material Button, whose inner padding clips 11.sp text). */
@Composable
private fun AttendanceChip(label: String, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF2A2A2A))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 7.dp)
  ) {
    Text(label, color = Color.White, fontSize = 11.sp, maxLines = 1)
  }
}

/** App-style segmented control (same chip pattern as NearbyFilterControls). */
@Composable
private fun <T> AttendanceSegmentedToggle(
  options: List<Pair<String, T>>,
  selected: T,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .height(32.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
      .border(1.dp, RFColors.CardBorder, RoundedCornerShape(6.dp)),
    verticalAlignment = Alignment.CenterVertically
  ) {
    options.forEach { (label, value) ->
      val isSelected = value == selected
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .background(if (isSelected) RFColors.AccentRed.copy(alpha = 0.75f) else Color.Transparent)
          .clickable { onSelect(value) },
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = label,
          color = if (isSelected) Color.White else RFColors.TextSecondary,
          fontSize = 10.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
          maxLines = 1
        )
      }
    }
  }
}

@Composable
private fun AttendanceStatItem(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Text(label, color = RFColors.TextTertiary, fontSize = 9.sp)
  }
}

@Composable
private fun BestAttendancePanel(stats: List<PlayerAttendanceRank>, mode: Int) {
  Card(
    modifier = Modifier.fillMaxWidth().height(220.dp),
    backgroundColor = Color(0xFF141414),
    shape = RoundedCornerShape(8.dp)
  ) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
      if (stats.isEmpty()) {
        item {
          Text(stringResource(Res.string.attendance_no_player_selected), color = RFColors.TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
        }
      }
      itemsIndexed(stats) { index, entry ->
        val valueText = when (mode) {
          1 -> entry.totalHealing.humanReadableAbbreviation()
          2 -> entry.totalCC.toLong().humanReadableAbbreviation()
          else -> entry.totalDamage.humanReadableAbbreviation()
        }
        val statColor = when (mode) {
          1 -> RFColors.healsGreen
          2 -> RFColors.ccCyan
          else -> RFColors.dpsOrange
        }
        val faction = Faction.fromString(entry.lastKnownFaction)
        val factionColor = if (faction == Faction.UNKNOWN) Color.Transparent else factionHighlightColor(faction)
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("#${index + 1}", color = RFColors.TextTertiary, fontSize = 10.sp, modifier = Modifier.width(24.dp))
          Box(
            modifier = Modifier
              .padding(end = 4.dp)
              .height(8.dp).width(8.dp)
              .background(factionColor, RoundedCornerShape(4.dp))
          )
          Text(entry.playerName, color = RFColors.TextPrimary, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text("${entry.sessionCount}", color = RFColors.TextSecondary, fontSize = 9.sp, modifier = Modifier.width(30.dp))
          Spacer(Modifier.width(10.dp))
          Text(valueText, color = statColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }
  }
}

/**
 * Calendar that scales to its viewport: rows share the remaining height evenly,
 * but cell height is capped relative to cell width so cells keep a sane aspect
 * ratio (extra vertical space is centered instead of stretching the cells).
 */
@Composable
private fun CalendarGrid(
  dayData: List<AttendanceDayData>,
  firstDayOfWeek: Int,
  monthShort: String,
  modifier: Modifier = Modifier
) {
  val dayHeaders = remember {
    listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
      .map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
  }
  // Pad the leading empty cells and trailing remainder so every week has exactly 7 cells
  val weeks = remember(dayData, firstDayOfWeek) {
    val cells: List<AttendanceDayData?> = List(firstDayOfWeek) { null } + dayData
    cells + List((7 - cells.size % 7) % 7) { null }
  }.chunked(7)

  BoxWithConstraints(modifier = modifier) {
    val headerHeight = 20.dp
    val cellWidth = maxWidth / 7
    val naturalCellHeight = (maxHeight - headerHeight) / weeks.size
    val cellHeight = minOf(naturalCellHeight, cellWidth * 0.85f)

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center
    ) {
      Row(modifier = Modifier.fillMaxWidth().height(headerHeight), verticalAlignment = Alignment.CenterVertically) {
        dayHeaders.forEach { header ->
          Text(header, color = RFColors.TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
      }
      weeks.forEach { week ->
        Row(modifier = Modifier.fillMaxWidth().height(cellHeight)) {
          week.forEach { cell ->
            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(2.dp)) {
              if (cell != null) CalendarDayCell(cell, monthShort, cellHeight, Modifier.fillMaxSize())
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CalendarDayCell(day: AttendanceDayData, monthShort: String, cellHeight: Dp, modifier: Modifier = Modifier) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  val missed = !day.present && day.events.isNotEmpty()

  Box(modifier) {
    Card(
      backgroundColor = when {
        day.present -> Color(0xFF1A2E1A)              // attended
        missed -> Color(0xFF241A1A)                   // event happened, player absent
        else -> Color(0xFF1A1A1A)                     // no events
      },
      shape = RoundedCornerShape(6.dp),
      modifier = Modifier.fillMaxSize().hoverable(interactionSource)
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          day.day.toString(),
          color = when {
            day.present -> RFColors.healsGreen
            missed -> RFColors.TextSecondary
            else -> RFColors.TextDisabled
          },
          fontSize = (cellHeight.value * 0.22f).sp,
          fontWeight = if (day.present) FontWeight.Bold else FontWeight.Normal
        )
        when {
          day.present -> {
            Text("\u2713", color = RFColors.healsGreen, fontSize = (cellHeight.value * 0.24f).sp, fontWeight = FontWeight.Bold)
            if (cellHeight >= 44.dp) {
              Text(
                "${day.sessions.size} event${if (day.sessions.size > 1) "s" else ""}",
                color = RFColors.TextSecondary,
                fontSize = (cellHeight.value * 0.15f).sp,
                maxLines = 1
              )
            }
          }
          missed -> Text("\u2715", color = RFColors.AccentRed.copy(alpha = 0.6f), fontSize = (cellHeight.value * 0.22f).sp)
          else -> Text("\u2014", color = RFColors.TextDisabled, fontSize = (cellHeight.value * 0.25f).sp)
        }
      }
    }
    if (isHovered && day.events.isNotEmpty()) {
      DayEventsTooltip(day, monthShort)
    }
  }
}

private const val MAX_TOOLTIP_EVENTS = 5

/**
 * Day briefing popup: every event that day, attended ones with the player's stats,
 * missed ones marked. Positioned above the cell when there is room (so it never
 * lands under the pointer and kills the hover), below otherwise, clamped to the window.
 */
@Composable
private fun DayEventsTooltip(day: AttendanceDayData, monthShort: String) {
  Popup(popupPositionProvider = object : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
      val gap = 8
      val above = anchorBounds.top - popupContentSize.height - gap
      val y = if (above >= 8) above
      else (anchorBounds.bottom + gap).coerceAtMost(windowSize.height - popupContentSize.height - 8).coerceAtLeast(8)
      val x = anchorBounds.left.coerceIn(8, (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8))
      return IntOffset(x, y)
    }
  }) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      elevation = 8.dp,
      color = RFColors.PopupBackground.copy(alpha = 0.97f),
      border = BorderStroke(1.dp, RFColors.CardBorder.copy(alpha = 0.7f))
    ) {
      Column(modifier = Modifier.width(230.dp).padding(horizontal = 12.dp, vertical = 9.dp)) {
        val attendedCount = day.rawAttended.size
        // Header: the date, and how much of the day's schedule the player made
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("$monthShort ${day.day}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
          Text(
            if (attendedCount == day.events.size) "${day.events.size} event${if (day.events.size > 1) "s" else ""}"
            else "$attendedCount/${day.events.size} events",
            color = if (attendedCount == day.events.size) RFColors.healsGreen else RFColors.AccentRedMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
        Spacer(Modifier.height(6.dp))
        day.events.take(MAX_TOOLTIP_EVENTS).forEachIndexed { index, event ->
          val row = day.rawAttended.firstOrNull { it.sessionStart == event.sessionStart }
          if (index > 0) Divider(color = RFColors.CardBorder.copy(alpha = 0.5f))
          Column(modifier = Modifier.padding(vertical = 5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                if (row != null) "\u2713" else "\u2715",
                color = if (row != null) RFColors.healsGreen else RFColors.AccentRed.copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(16.dp)
              )
              Text(
                event.sessionType.ifBlank { event.sessionTitle.ifBlank { "Session" } },
                color = if (row != null) Color.White else RFColors.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )
            }
            if (row != null) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
              ) {
                if (row.totalDamage > 0) TooltipStat("DMG", row.totalDamage.humanReadableAbbreviation(), RFColors.dpsOrange)
                if (row.totalHealing > 0) TooltipStat("HL", row.totalHealing.humanReadableAbbreviation(), RFColors.healsGreen)
                if (row.totalCC > 0) TooltipStat("CC", row.totalCC.toString(), RFColors.ccCyan)
                if (row.totalDamage == 0L && row.totalHealing == 0L && row.totalCC == 0) {
                  Text("No participation recorded", color = RFColors.TextTertiary, fontSize = 9.sp)
                }
              }
            }
          }
        }
        if (day.events.size > MAX_TOOLTIP_EVENTS) {
          Text(
            "+${day.events.size - MAX_TOOLTIP_EVENTS} more",
            color = RFColors.TextTertiary,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun TooltipStat(label: String, value: String, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(label, color = RFColors.TextTertiary, fontSize = 9.sp)
    Spacer(Modifier.width(3.dp))
    Text(value, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
  }
}
