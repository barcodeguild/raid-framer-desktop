package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.reoky.raidframer.core.database.PlayerSessionTotalsEntity
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.helpers.showCsvSaveChooser
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

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val rowFmt = SimpleDateFormat("MMM d HH:mm", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryOverlay(wm: WindowManager?) {
  val dragLock = LocalDragLock.current
  val selectedPlayer by AppState.selectedPlayer.collectAsState()
  // Local override so the user can switch players without leaving this window.
  // Initialized from AppState on open; null means "follow AppState".
  var playerOverride by remember { mutableStateOf<String?>(null) }
  val playerName = playerOverride ?: selectedPlayer

  val sessions by produceState<List<PlayerSessionTotalsEntity>>(
    initialValue = emptyList(), key1 = playerName
  ) {
    val name = playerName
    value = if (name.isNullOrBlank()) emptyList() else try {
      withContext(Dispatchers.IO) { RFDao.playerSessionDao.getSessionsForPlayer(name) }
    } catch (e: Exception) { emptyList() }
  }

  var selectedDate by remember(playerName) { mutableStateOf<LocalDate?>(null) }
  var datePickerOpen by remember { mutableStateOf(false) }
  var selectedIndex by remember(playerName) { mutableStateOf(0) }
  var checked by remember(playerName) { mutableStateOf(setOf<Long>()) }
  var exportStatus by remember(playerName) { mutableStateOf("") }

  val filtered = remember(sessions, selectedDate) {
    if (selectedDate == null) sessions
    else sessions.filter {
      Instant.ofEpochMilli(it.sessionEnd).atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
    }
  }
  LaunchedEffect(filtered.size) {
    if (selectedIndex >= filtered.size) selectedIndex = 0
  }
  LaunchedEffect(datePickerOpen) { dragLock.value = datePickerOpen }

  val selected = filtered.getOrNull(selectedIndex)
  val name = playerName ?: ""

  Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E).copy(alpha = 0.97f))) {
    TitleBarComponent(
      title = "Session History — $name (${filtered.size})",
      onClose = { wm?.closeWindow(OverlayType.SESSION_HISTORY) }
    )

    // Toolbar: player search + date filter + select all/none + export
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      PlayerSearchBox(
        currentName = name,
        highlightBorder = true,
        onSelect = { picked ->
          AppState.selectPlayer(picked)
          playerOverride = picked
          selectedDate = null
          selectedIndex = 0
          checked = emptySet()
        }
      )
      Button(
        onClick = { dragLock.value = true; datePickerOpen = true },
        modifier = Modifier.width(124.dp).height(32.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = Color.White.copy(alpha = 0.10f),
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text(
          selectedDate?.format(dateFmt) ?: "Filter by day",
          color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
      }
      if (selectedDate != null) {
        TextButton(onClick = { selectedDate = null }, modifier = Modifier.height(32.dp)) {
          Text("Clear", color = RFColors.TextSecondary, fontSize = 11.sp)
        }
      }
      Spacer(Modifier.weight(1f))
      Text(
        if (checked.isEmpty()) "Export: all ${filtered.size}" else "Export: ${checked.size}",
        color = RFColors.TextTertiary, fontSize = 10.sp
      )
      Text(
        if (checked.size == filtered.size && filtered.isNotEmpty()) "None" else "All",
        color = RFColors.AccentRed, fontSize = 11.sp,
        modifier = Modifier.clickable {
          checked = if (checked.size == filtered.size) emptySet()
          else filtered.map { it.sessionStart }.toSet()
        }.padding(horizontal = 4.dp)
      )
      Button(
        onClick = {
          exportHistorySessions(filtered, checked, name, wm) { exportStatus = it }
        },
        colors = ButtonDefaults.buttonColors(RFColors.AccentRed),
        modifier = Modifier.height(32.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
      ) {
        Text("Export CSV", color = Color.White, fontSize = 11.sp)
      }
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
          }) { Text("Apply", color = RFColors.AccentRed) }
        },
        dismissButton = {
          Material3TextButton(onClick = { datePickerOpen = false; dragLock.value = false }) {
            Text("Cancel", color = Color.White)
          }
        }
      ) { DatePicker(state = pickerState) }
    }
    if (exportStatus.isNotBlank()) {
      Text(exportStatus, color = RFColors.TextTertiary, fontSize = 10.sp,
        modifier = Modifier.padding(horizontal = 12.dp))
    }

    // Side-by-side: picker left, preview right
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp).padding(bottom = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Left: session list
      Column(Modifier.weight(0.9f).fillMaxHeight()) {
        if (filtered.isEmpty()) {
          Text("No historical sessions.", color = RFColors.TextDisabled, fontSize = 12.sp)
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF141414))
              .border(1.dp, RFColors.CardBorder, RoundedCornerShape(8.dp))
              .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            itemsIndexed(filtered, key = { _, s -> s.sessionStart }) { i, s ->
              val isSel = i == selectedIndex
              Row(
                modifier = Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSel) RFColors.AccentRed.copy(alpha = 0.18f) else Color(0xFF1E1E1E))
                  .clickable { selectedIndex = i }
                  .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = checked.contains(s.sessionStart),
                  onCheckedChange = { on ->
                    checked = if (on) checked + s.sessionStart else checked - s.sessionStart
                  },
                  colors = CheckboxDefaults.colors(
                    checkedColor = RFColors.AccentRed,
                    uncheckedColor = RFColors.TextTertiary
                  )
                )
                Column(Modifier.weight(1f)) {
                  Text(
                    rowFmt.format(Date(s.sessionEnd)) +
                      (if (s.sessionType.isNotBlank()) " · ${s.sessionType}" else "") +
                      (if (s.sessionTitle.isNotBlank()) " · ${s.sessionTitle}" else ""),
                    color = RFColors.TextPrimary, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    "Dmg ${s.totalDamage.humanReadableAbbreviation()} • Heal ${s.totalHealing.humanReadableAbbreviation()} • CC ${s.totalCC} • K ${s.totalKills}/${s.totalKillsKB}",
                    color = RFColors.TextTertiary, fontSize = 10.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }

      // Right: preview with pager + 3-column stats
      Column(
        Modifier.weight(1.4f).fillMaxHeight()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF141414))
          .border(1.dp, RFColors.CardBorder, RoundedCornerShape(8.dp))
          .padding(10.dp)
          .verticalScroll(rememberScrollState())
      ) {
        if (selected == null) {
          Text("Select a session to preview.", color = RFColors.TextTertiary, fontSize = 12.sp)
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              "< Prev",
              color = if (selectedIndex > 0) RFColors.AccentRed else RFColors.TextDisabled,
              fontSize = 12.sp, fontWeight = FontWeight.Bold,
              modifier = Modifier.clickable(enabled = selectedIndex > 0) { selectedIndex-- }.padding(4.dp)
            )
            Text("${selectedIndex + 1} / ${filtered.size}", color = RFColors.TextSecondary, fontSize = 11.sp)
            Text(
              "Next >",
              color = if (selectedIndex < filtered.size - 1) RFColors.AccentRed else RFColors.TextDisabled,
              fontSize = 12.sp, fontWeight = FontWeight.Bold,
              modifier = Modifier.clickable(enabled = selectedIndex < filtered.size - 1) { selectedIndex++ }.padding(4.dp)
            )
          }
          Spacer(Modifier.height(4.dp))
          SessionStatGrid(SessionTotals.fromEntity(selected), columns = 3)
        }
      }
    }
  }
}

private fun exportHistorySessions(
  filtered: List<PlayerSessionTotalsEntity>,
  checked: Set<Long>,
  playerName: String,
  wm: WindowManager?,
  onExportStatus: (String) -> Unit
) {
  val rows = if (checked.isEmpty()) filtered else filtered.filter { checked.contains(it.sessionStart) }
  if (rows.isEmpty()) {
    onExportStatus("Nothing to export.")
    return
  }
  val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
  val safeName = playerName.replace(Regex("[^A-Za-z0-9_.-]"), "")
  val suggested = "player-history_${safeName}_${stamp}_${rows.size}sessions.csv"
  // Independent scope: closing the window disposes the composition scope,
  // which would cancel the save-dialog callback if we used it.
  val exportScope = CoroutineScope(Dispatchers.IO)
  wm?.closeWindow(OverlayType.SESSION_HISTORY)
  showCsvSaveChooser(
    suggestedName = suggested,
    dialogTitle = "Export Player History CSV",
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
