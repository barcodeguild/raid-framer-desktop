package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton as Material3TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.browser.exportSessionCsv
import com.reoky.raidframer.core.browser.exportSessionsToFolder
import com.reoky.raidframer.core.database.PlayerSessionTotalsEntity
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.database.SessionSummary
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.sortedByDisplayOrder
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.helpers.skillTreeIconPainterFor
import com.reoky.raidframer.core.helpers.showCsvSaveChooser
import com.reoky.raidframer.core.helpers.showFolderChooser
import com.reoky.raidframer.ui.LocalDragLock
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.PlayerSearchBox
import com.reoky.raidframer.ui.component.SessionStatGrid
import com.reoky.raidframer.ui.component.SessionTotals
import com.reoky.raidframer.ui.component.TitleBarComponent
import java.awt.Desktop
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.session_history_apply
import raid_framer_desktop.composeapp.generated.resources.session_history_cancel
import raid_framer_desktop.composeapp.generated.resources.session_history_clear
import raid_framer_desktop.composeapp.generated.resources.session_history_export_all_format
import raid_framer_desktop.composeapp.generated.resources.session_history_export_checked_format
import raid_framer_desktop.composeapp.generated.resources.session_history_export_csv
import raid_framer_desktop.composeapp.generated.resources.session_history_export_dialog_title
import raid_framer_desktop.composeapp.generated.resources.session_history_filter_by_day
import raid_framer_desktop.composeapp.generated.resources.session_history_next
import raid_framer_desktop.composeapp.generated.resources.session_history_no_sessions
import raid_framer_desktop.composeapp.generated.resources.session_history_nothing_to_export
import raid_framer_desktop.composeapp.generated.resources.session_history_pager_format
import raid_framer_desktop.composeapp.generated.resources.session_history_prev
import raid_framer_desktop.composeapp.generated.resources.session_history_row_summary_format
import raid_framer_desktop.composeapp.generated.resources.session_history_select_all
import raid_framer_desktop.composeapp.generated.resources.session_history_select_none
import raid_framer_desktop.composeapp.generated.resources.session_history_select_prompt
import raid_framer_desktop.composeapp.generated.resources.session_history_title_format

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val rowFmt = SimpleDateFormat("MMM d HH:mm", Locale.US)
private enum class HistoryMode { ENTIRE, PLAYER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryOverlay(wm: WindowManager?) {
  val dragLock = LocalDragLock.current
  val selectedPlayer by AppState.selectedPlayer.collectAsState()
  var playerOverride by remember { mutableStateOf<String?>(null) }
  val playerName = playerOverride ?: selectedPlayer
  // Default: entire-session mode when opened with no player, player mode from PlayerCardOverlay.
  var mode by remember { mutableStateOf(if (playerName.isNullOrBlank()) HistoryMode.ENTIRE else HistoryMode.PLAYER) }

  var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
  var datePickerOpen by remember { mutableStateOf(false) }
  var exportStatus by remember { mutableStateOf("") }

  val filterByDayText = stringResource(Res.string.session_history_filter_by_day)
  val clearText = stringResource(Res.string.session_history_clear)
  val exportCsvText = stringResource(Res.string.session_history_export_csv)
  val applyText = stringResource(Res.string.session_history_apply)
  val cancelText = stringResource(Res.string.session_history_cancel)
  val noSessionsText = stringResource(Res.string.session_history_no_sessions)
  val nothingToExportText = stringResource(Res.string.session_history_nothing_to_export)
  val exportDialogTitle = stringResource(Res.string.session_history_export_dialog_title)

  fun dateOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
  fun matchesDate(end: Long) = selectedDate == null || dateOf(end) == selectedDate

  Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E).copy(alpha = 0.97f))) {
    TitleBarComponent(
      title = "Session History",
      onClose = { wm?.closeWindow(OverlayType.SESSION_HISTORY) }
    )

    // Single control strip: mode toggle + summary + filter by day + select + export
    if (mode == HistoryMode.ENTIRE) {
      EntireSessionsView(
        wm = wm, mode = mode, onMode = { mode = it; exportStatus = "" },
        selectedDate = selectedDate, matchesDate = ::matchesDate,
        filterByDayText = filterByDayText, clearText = clearText, applyText = applyText,
        cancelText = cancelText, noSessionsText = noSessionsText, exportCsvText = exportCsvText,
        nothingToExportText = nothingToExportText,
        datePickerOpen = datePickerOpen, onDatePickerOpen = { datePickerOpen = it },
        exportStatus = exportStatus, onExportStatus = { exportStatus = it },
        onSelectDate = { selectedDate = it }, dragLock = dragLock
      )
    } else {
      PlayerSessionsView(
        wm = wm, mode = mode, onMode = { mode = it; exportStatus = "" },
        playerName = playerName, selectedDate = selectedDate, matchesDate = ::matchesDate,
        filterByDayText = filterByDayText, clearText = clearText, applyText = applyText,
        cancelText = cancelText, noSessionsText = noSessionsText, exportCsvText = exportCsvText,
        nothingToExportText = nothingToExportText, exportDialogTitle = exportDialogTitle,
        datePickerOpen = datePickerOpen, onDatePickerOpen = { datePickerOpen = it },
        exportStatus = exportStatus, onExportStatus = { exportStatus = it },
        onSelectDate = { selectedDate = it }, dragLock = dragLock,
        onPickPlayer = { picked ->
          AppState.selectPlayer(picked)
          playerOverride = picked
          selectedDate = null
        },
        onClearPlayer = {
          AppState.selectPlayer(null)
          playerOverride = null
          selectedDate = null
        }
      )
    }

    if (datePickerOpen) {
      val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
      )
      DatePickerDialog(
        onDismissRequest = { datePickerOpen = false; dragLock.value = false },
        confirmButton = {
          Material3TextButton(onClick = {
            pickerState.selectedDateMillis?.let { millis ->
              selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            }
            datePickerOpen = false
            dragLock.value = false
          }) { Text(applyText, color = RFColors.AccentRed) }
        },
        dismissButton = {
          Material3TextButton(onClick = { datePickerOpen = false; dragLock.value = false }) {
            Text(cancelText, color = Color.White)
          }
        }
      ) { DatePicker(state = pickerState) }
    }
  }
}

@Composable
private fun ModeSegment(mode: HistoryMode, onMode: (HistoryMode) -> Unit) {
  Row(
    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E1E1E))
      .border(1.dp, RFColors.CardBorder, RoundedCornerShape(8.dp)).padding(2.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    SegmentBtn("Entire Sessions", mode == HistoryMode.ENTIRE) { onMode(HistoryMode.ENTIRE) }
    SegmentBtn("Player Sessions", mode == HistoryMode.PLAYER) { onMode(HistoryMode.PLAYER) }
  }
}

@Composable
private fun SegmentBtn(label: String, selected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier.clip(RoundedCornerShape(6.dp))
      .background(if (selected) RFColors.AccentRed else Color.Transparent)
      .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(label, color = if (selected) Color.White else RFColors.TextSecondary, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
  }
}

@Composable
private fun ControlStrip(
  mode: HistoryMode, onMode: (HistoryMode) -> Unit,
  summaryText: String,
  filterLabel: String, clearText: String, hasDate: Boolean,
  onFilter: () -> Unit, onClear: () -> Unit,
  statusText: String, toggleText: String, onToggle: () -> Unit,
  exportText: String, onExport: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    ModeSegment(mode = mode, onMode = onMode)
    Text(summaryText, color = RFColors.TextTertiary, fontSize = 10.sp, modifier = Modifier.weight(1f),
      maxLines = 1, overflow = TextOverflow.Ellipsis)
    Button(
      onClick = onFilter,
      modifier = Modifier.width(124.dp).height(32.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
      colors = ButtonDefaults.buttonColors(backgroundColor = Color.White.copy(alpha = 0.10f), contentColor = Color.White),
      shape = RoundedCornerShape(6.dp)
    ) {
      Text(filterLabel, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (hasDate) {
      TextButton(onClick = onClear, modifier = Modifier.height(32.dp)) {
        Text(clearText, color = RFColors.TextSecondary, fontSize = 11.sp)
      }
    }
    Text(statusText, color = RFColors.TextTertiary, fontSize = 10.sp)
    Text(toggleText, color = RFColors.AccentRed, fontSize = 11.sp,
      modifier = Modifier.clickable(onClick = onToggle).padding(horizontal = 4.dp))
    Button(
      onClick = onExport,
      colors = ButtonDefaults.buttonColors(RFColors.AccentRed),
      modifier = Modifier.height(32.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
      Text(exportText, color = Color.White, fontSize = 11.sp)
    }
  }
}

@Composable
private fun SessionRowCard(
  title: String, subtitle: String, checked: Boolean, selected: Boolean,
  onCheck: (Boolean) -> Unit, onSelect: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .background(if (selected) RFColors.AccentRed.copy(alpha = 0.18f) else Color(0xFF1E1E1E))
      .clickable(onClick = onSelect)
      .padding(horizontal = 6.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Checkbox(
      checked = checked, onCheckedChange = onCheck,
      colors = CheckboxDefaults.colors(checkedColor = RFColors.AccentRed, uncheckedColor = RFColors.TextTertiary)
    )
    Column(Modifier.weight(1f)) {
      Text(title, color = RFColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(subtitle, color = RFColors.TextTertiary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Composable
private fun EntireSessionsView(
  wm: WindowManager?,
  mode: HistoryMode, onMode: (HistoryMode) -> Unit,
  selectedDate: LocalDate?,
  matchesDate: (Long) -> Boolean,
  filterByDayText: String, clearText: String, applyText: String, cancelText: String,
  noSessionsText: String, exportCsvText: String, nothingToExportText: String,
  datePickerOpen: Boolean, onDatePickerOpen: (Boolean) -> Unit,
  exportStatus: String, onExportStatus: (String) -> Unit,
  onSelectDate: (LocalDate?) -> Unit,
  dragLock: androidx.compose.runtime.MutableState<Boolean>
) {
  val summaries by produceState<List<SessionSummary>>(initialValue = emptyList(), key1 = Unit) {
    value = try { withContext(Dispatchers.IO) { RFDao.playerSessionDao.getDistinctSessions() } } catch (e: Exception) { emptyList() }
  }
  val filtered = remember(summaries, selectedDate) { summaries.filter { matchesDate(it.sessionEnd) } }
  var selectedIndex by remember { mutableStateOf(0) }
  var checked by remember { mutableStateOf(setOf<Long>()) }
  var flyoutPlayer by remember { mutableStateOf<PlayerSessionTotalsEntity?>(null) }
  LaunchedEffect(filtered.size) { if (selectedIndex >= filtered.size) selectedIndex = 0 }
  LaunchedEffect(datePickerOpen) { dragLock.value = datePickerOpen }

  val selected = filtered.getOrNull(selectedIndex)
  val roster by produceState<List<PlayerSessionTotalsEntity>>(initialValue = emptyList(), key1 = selected?.sessionStart) {
    val start = selected?.sessionStart
    value = if (start == null) emptyList() else try {
      withContext(Dispatchers.IO) { RFDao.playerSessionDao.getRowsForSession(start) }
    } catch (e: Exception) { emptyList() }
  }
  LaunchedEffect(roster) { flyoutPlayer = flyoutPlayer?.let { fp -> roster.firstOrNull { it.playerName == fp.playerName } } }

  val topDmg = remember(roster) { roster.maxByOrNull { it.totalDamage } }
  val topCharm = remember(roster) { roster.maxByOrNull { it.totalCharms } }
  val statusText = if (checked.isEmpty()) "Export: all ${filtered.size}" else "Export: ${checked.size}"
  val toggleText = if (checked.size == filtered.size && filtered.isNotEmpty()) "None" else "All"

  ControlStrip(
    mode = mode, onMode = onMode,
    summaryText = "Export entire sessions — one CSV per session, all players in each file.",
    filterLabel = selectedDate?.format(dateFmt) ?: filterByDayText, clearText = clearText,
    hasDate = selectedDate != null,
    onFilter = { dragLock.value = true; onDatePickerOpen(true) }, onClear = { onSelectDate(null) },
    statusText = statusText, toggleText = toggleText,
    onToggle = { checked = if (checked.size == filtered.size) emptySet() else filtered.map { it.sessionStart }.toSet() },
    exportText = exportCsvText,
    onExport = {
      val starts = if (checked.isEmpty()) filtered.map { it.sessionStart } else filtered.filter { checked.contains(it.sessionStart) }.map { it.sessionStart }
      if (starts.isEmpty()) { onExportStatus(nothingToExportText); return@ControlStrip }
      val exportScope = CoroutineScope(Dispatchers.IO)
      wm?.closeWindow(OverlayType.SESSION_HISTORY)
      showFolderChooser(
        dialogTitle = "Select folder for session CSVs",
        onFolderSelected = { folder ->
          exportScope.launch {
            try {
              val rows = withContext(Dispatchers.IO) { RFDao.playerSessionDao.getRowsForSessions(starts) }
              val bySession = rows.groupBy { it.sessionStart }
              val out = exportSessionsToFolder(bySession, folder)
              try { Desktop.getDesktop().open(out) } catch (e: Exception) { }
              onExportStatus("Wrote ${bySession.size} CSVs to ${out.name}")
            } catch (e: Exception) { onExportStatus("Export failed: ${e.message}") }
            withContext(Dispatchers.Main) { wm?.openWindow(OverlayType.SESSION_HISTORY) }
          }
        },
        onCancel = { wm?.openWindow(OverlayType.SESSION_HISTORY) }
      )
    }
  )
  if (exportStatus.isNotBlank()) {
    Text(exportStatus, color = RFColors.TextTertiary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 12.dp))
  }

  Box(Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp).padding(bottom = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Column(Modifier.weight(0.9f).fillMaxHeight()) {
        if (filtered.isEmpty()) {
          Text(noSessionsText, color = RFColors.TextDisabled, fontSize = 12.sp)
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color(0xFF141414))
              .border(1.dp, RFColors.CardBorder, RoundedCornerShape(8.dp)).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            itemsIndexed(filtered, key = { _, s -> s.sessionStart }) { i, s ->
              SessionRowCard(
                title = rowFmt.format(Date(s.sessionEnd)) +
                  (if (s.sessionType.isNotBlank()) " · ${s.sessionType}" else "") +
                  (if (s.sessionTitle.isNotBlank()) " · ${s.sessionTitle}" else ""),
                subtitle = "${s.playerCount} players",
                checked = checked.contains(s.sessionStart), selected = i == selectedIndex,
                onCheck = { on -> checked = if (on) checked + s.sessionStart else checked - s.sessionStart },
                onSelect = { selectedIndex = i }
              )
            }
          }
        }
      }
      Column(
        Modifier.weight(1.4f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(Color(0xFF141414))
          .border(1.dp, RFColors.CardBorder, RoundedCornerShape(8.dp)).padding(10.dp)
          .verticalScroll(rememberScrollState())
      ) {
        if (selected == null) {
          Text("Select a session to preview.", color = RFColors.TextTertiary, fontSize = 12.sp)
        } else {
          Text(
            rowFmt.format(Date(selected.sessionEnd)) +
              (if (selected.sessionType.isNotBlank()) " · ${selected.sessionType}" else "") +
              (if (selected.sessionTitle.isNotBlank()) " · ${selected.sessionTitle}" else ""),
            color = RFColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold
          )
          Text("${roster.size} players present", color = RFColors.TextSecondary, fontSize = 11.sp)
          if (topDmg != null) Text("Top dmg: ${topDmg.playerName} (${topDmg.totalDamage.humanReadableAbbreviation()})", color = RFColors.TextTertiary, fontSize = 10.sp)
          if (topCharm != null && (topCharm.totalCharms > 0)) Text("Top charms: ${topCharm.playerName} (${topCharm.totalCharms})", color = RFColors.TextTertiary, fontSize = 10.sp)
          Spacer(Modifier.height(6.dp))
          // Resolve specs for skill-tree icons (batched, cached per roster).
          val specsByName by produceState<Map<String, SpecType?>>(initialValue = emptyMap(), key1 = roster.map { it.playerName }) {
            value = try {
              withContext(Dispatchers.IO) {
                roster.associate { r ->
                  val specName = try { RFDao.playerCacheDao.getPlayerCacheFor(r.playerName)?.lastKnownSpec.orEmpty() } catch (e: Exception) { "" }
                  r.playerName to try { SpecType.fromName(specName) } catch (e: Exception) { SpecType.UNKNOWN }
                }
              }
            } catch (e: Exception) { emptyMap() }
          }
          roster.forEachIndexed { idx, r ->
            RosterPlayerRow(index = idx, row = r, spec = specsByName[r.playerName], onClick = { flyoutPlayer = r })
            Spacer(Modifier.height(2.dp))
          }
        }
      }
    }
    // Player flyout (Player Browser style): session totals for the clicked player
    flyoutPlayer?.let { fp ->
      Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { flyoutPlayer = null }) {
        Column(
          Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(300.dp)
            .clip(RoundedCornerShape(10.dp)).background(RFColors.PopupBackground)
            .border(1.dp, RFColors.CardBorder, RoundedCornerShape(10.dp)).padding(12.dp)
            .clickable(enabled = false) { }.verticalScroll(rememberScrollState())
        ) {
          Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(fp.playerName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("✕", color = RFColors.TextTertiary, fontSize = 13.sp, modifier = Modifier.clickable { flyoutPlayer = null }.padding(4.dp))
          }
          Text("SESSION TOTALS", color = RFColors.TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
          Spacer(Modifier.height(4.dp))
          SessionStatGrid(SessionTotals.fromEntity(fp), columns = 1)
        }
      }
    }
  }
}

@Composable
private fun RosterStat(label: String, value: String, color: Color) {
  // RaidCaller LabeledValue style: lighter semibold label + bold colored value.
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(label, color = RFColors.TestCaption, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    Text(value, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun RosterPlayerRow(
  index: Int,
  row: PlayerSessionTotalsEntity,
  spec: SpecType?,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  Column(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
      .background(if (isHovered) RFColors.AccentRed.copy(alpha = 0.18f) else Color(0xFF1E1E1E))
      .hoverable(interactionSource).clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 5.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("${index + 1}.", color = RFColors.TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.width(30.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (spec == null || spec == SpecType.UNKNOWN) {
          Image(skillTreeIconPainterFor(null), "?", Modifier.size(16.dp))
          Image(skillTreeIconPainterFor(null), "?", Modifier.size(16.dp))
          Image(skillTreeIconPainterFor(null), "?", Modifier.size(16.dp))
        } else {
          spec.trees.sortedByDisplayOrder().forEach { tree ->
            Image(skillTreeIconPainterFor(tree), null, Modifier.size(16.dp).padding(horizontal = 1.dp))
          }
        }
      }
      Spacer(Modifier.width(6.dp))
      Text(row.playerName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(2.dp))
    Row(
      modifier = Modifier.padding(start = 30.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      RosterStat("Dmg", row.totalDamage.humanReadableAbbreviation(), RFColors.dpsOrange)
      RosterStat("Heal", row.totalHealing.humanReadableAbbreviation(), RFColors.healsGreen)
      RosterStat("CC", row.totalCC.toLong().humanReadableAbbreviation(), RFColors.ccCyan)
      RosterStat("K", "${row.totalKills}/${row.totalKillsKB}", RFColors.killsRed)
    }
  }
}

@Composable
private fun PlayerSessionsView(
  wm: WindowManager?,
  mode: HistoryMode, onMode: (HistoryMode) -> Unit,
  playerName: String?,
  selectedDate: LocalDate?,
  matchesDate: (Long) -> Boolean,
  filterByDayText: String, clearText: String, applyText: String, cancelText: String,
  noSessionsText: String, exportCsvText: String, nothingToExportText: String, exportDialogTitle: String,
  datePickerOpen: Boolean, onDatePickerOpen: (Boolean) -> Unit,
  exportStatus: String, onExportStatus: (String) -> Unit,
  onSelectDate: (LocalDate?) -> Unit,
  dragLock: androidx.compose.runtime.MutableState<Boolean>,
  onPickPlayer: (String) -> Unit,
  onClearPlayer: () -> Unit
) {
  val sessions by produceState<List<PlayerSessionTotalsEntity>>(
    initialValue = emptyList(), key1 = playerName
  ) {
    val name = playerName
    value = if (name.isNullOrBlank()) emptyList() else try {
      withContext(Dispatchers.IO) { RFDao.playerSessionDao.getSessionsForPlayer(name) }
    } catch (e: Exception) { emptyList() }
  }
  val filtered = remember(sessions, selectedDate) { sessions.filter { matchesDate(it.sessionEnd) } }
  var selectedIndex by remember(playerName) { mutableStateOf(0) }
  var checked by remember(playerName) { mutableStateOf(setOf<Long>()) }
  LaunchedEffect(filtered.size) { if (selectedIndex >= filtered.size) selectedIndex = 0 }
  LaunchedEffect(datePickerOpen) { dragLock.value = datePickerOpen }

  val selected = filtered.getOrNull(selectedIndex)
  val prevText = stringResource(Res.string.session_history_prev, "<")
  val nextText = stringResource(Res.string.session_history_next, ">")
  val pagerText = stringResource(Res.string.session_history_pager_format, selectedIndex + 1, filtered.size)
  val exportAllText = stringResource(Res.string.session_history_export_all_format, filtered.size)
  val exportCheckedText = stringResource(Res.string.session_history_export_checked_format, checked.size)
  val selectToggleText = if (checked.size == filtered.size && filtered.isNotEmpty()) stringResource(Res.string.session_history_select_none) else stringResource(Res.string.session_history_select_all)
  val selectPromptText = stringResource(Res.string.session_history_select_prompt)

  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    PlayerSearchBox(currentName = playerName ?: "", highlightBorder = true, onSelect = onPickPlayer, onClear = onClearPlayer)
  }
  ControlStrip(
    mode = mode, onMode = onMode,
    summaryText = "Export one player's sessions — one CSV with that player's per-session stats.",
    filterLabel = selectedDate?.format(dateFmt) ?: filterByDayText, clearText = clearText,
    hasDate = selectedDate != null,
    onFilter = { dragLock.value = true; onDatePickerOpen(true) }, onClear = { onSelectDate(null) },
    statusText = if (checked.isEmpty()) exportAllText else exportCheckedText, toggleText = selectToggleText,
    onToggle = { checked = if (checked.size == filtered.size) emptySet() else filtered.map { it.sessionStart }.toSet() },
    exportText = exportCsvText,
    onExport = { exportHistorySessions(filtered, checked, playerName ?: "", wm, onExportStatus, nothingToExportText, exportDialogTitle) }
  )
  if (exportStatus.isNotBlank()) {
    Text(exportStatus, color = RFColors.TextTertiary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 12.dp))
  }
  Row(
    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp).padding(bottom = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Column(Modifier.weight(0.9f).fillMaxHeight()) {
      if (filtered.isEmpty()) {
        Text(noSessionsText, color = RFColors.TextDisabled, fontSize = 12.sp)
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color(0xFF141414))
            .border(1.dp, RFColors.CardBorder, RoundedCornerShape(8.dp)).padding(6.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          itemsIndexed(filtered, key = { _, s -> s.sessionStart }) { i, s ->
            SessionRowCard(
              title = rowFmt.format(Date(s.sessionEnd)) +
                (if (s.sessionType.isNotBlank()) " · ${s.sessionType}" else "") +
                (if (s.sessionTitle.isNotBlank()) " · ${s.sessionTitle}" else ""),
              subtitle = stringResource(
                Res.string.session_history_row_summary_format,
                s.totalDamage.humanReadableAbbreviation(),
                s.totalHealing.humanReadableAbbreviation(),
                s.totalCC.toString(), s.totalKills.toString(), s.totalKillsKB.toString()
              ),
              checked = checked.contains(s.sessionStart), selected = i == selectedIndex,
              onCheck = { on -> checked = if (on) checked + s.sessionStart else checked - s.sessionStart },
              onSelect = { selectedIndex = i }
            )
          }
        }
      }
    }
    Column(
      Modifier.weight(1.4f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(Color(0xFF141414))
        .border(1.dp, RFColors.CardBorder, RoundedCornerShape(8.dp)).padding(10.dp)
        .verticalScroll(rememberScrollState())
    ) {
      if (selected == null) {
        Text(selectPromptText, color = RFColors.TextTertiary, fontSize = 12.sp)
      } else {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
          Text(prevText, color = if (selectedIndex > 0) RFColors.AccentRed else RFColors.TextDisabled,
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(enabled = selectedIndex > 0) { selectedIndex-- }.padding(4.dp))
          Text(pagerText, color = RFColors.TextSecondary, fontSize = 11.sp)
          Text(nextText, color = if (selectedIndex < filtered.size - 1) RFColors.AccentRed else RFColors.TextDisabled,
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(enabled = selectedIndex < filtered.size - 1) { selectedIndex++ }.padding(4.dp))
        }
        Spacer(Modifier.height(4.dp))
        SessionStatGrid(SessionTotals.fromEntity(selected), columns = 3)
      }
    }
  }
}

private fun exportHistorySessions(
  filtered: List<PlayerSessionTotalsEntity>,
  checked: Set<Long>,
  playerName: String,
  wm: WindowManager?,
  onExportStatus: (String) -> Unit,
  nothingToExportText: String,
  exportDialogTitle: String
) {
  val rows = if (checked.isEmpty()) filtered else filtered.filter { checked.contains(it.sessionStart) }
  if (rows.isEmpty()) {
    onExportStatus(nothingToExportText)
    return
  }
  val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
  val safeName = playerName.replace(Regex("[^A-Za-z0-9_.-]"), "")
  val suggested = "player-history_${safeName}_${stamp}_${rows.size}sessions.csv"
  val exportScope = CoroutineScope(Dispatchers.IO)
  wm?.closeWindow(OverlayType.SESSION_HISTORY)
  showCsvSaveChooser(
    suggestedName = suggested,
    dialogTitle = exportDialogTitle,
    onFileSelected = { file ->
      exportScope.launch {
        try {
          val out = exportSessionCsv(rows, file)
          try { Desktop.getDesktop().open(out.parentFile) } catch (e: Exception) { }
        } catch (e: Exception) { }
        withContext(Dispatchers.Main) { wm?.openWindow(OverlayType.SESSION_HISTORY) }
      }
    },
    onCancel = { wm?.openWindow(OverlayType.SESSION_HISTORY) }
  )
}
