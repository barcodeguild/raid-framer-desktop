package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextButton
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton as Material3TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.pocket.PocketDraftCoordinator
import com.reoky.raidframer.core.pocket.PocketEntry
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.LocalDragLock
import com.reoky.raidframer.ui.component.TitleBarComponent
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val journalDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val journalDateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketJournalOverlay(wm: WindowManager? = null) {
  val scope = rememberCoroutineScope()
  val entries by PocketDraftCoordinator.entries.collectAsState()
  var search by remember { mutableStateOf("") }
  var activeTag by remember { mutableStateOf<String?>(null) }
  var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
  var datePickerOpen by remember { mutableStateOf(false) }
  var activeDeleteMessage by remember { mutableStateOf(false) }
  val dragLock = LocalDragLock.current
  dragLock.value = datePickerOpen

  val filtered = entries.filter { entry ->
    (search.isBlank() || entry.metadata.title.contains(search, ignoreCase = true) ||
        entry.tags.any { it.tag.contains(search, ignoreCase = true) }) &&
        (activeTag == null || entry.tags.any { it.normalizedTag == activeTag }) &&
        isOnDate(entry.metadata.createdAt, selectedDate)
  }

  Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.66f))) {
    TitleBarComponent(
      title = "Pocket Journal",
      onClose = { wm?.closeWindow(OverlayType.POCKET_JOURNAL) },
      rightActions = {
        Button(
          onClick = {
            scope.launch {
              PocketDraftCoordinator.createDraft()
              wm?.openWindow(OverlayType.POCKET_EDITOR)
            }
          },
          colors = ButtonDefaults.buttonColors(backgroundColor = RFColors.AccentRed, contentColor = Color.White),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
          modifier = Modifier.height(32.dp)
        ) { Text("\uf303", color = Color.White, fontFamily = FontsHelper.faSolid(), fontSize = 14.sp) }
      }
    )
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
        .background(Color(0xFF141414).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
        .padding(8.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = search,
        onValueChange = { search = it },
        modifier = Modifier.weight(1f),
        singleLine = true,
        placeholder = { Text("Search title or tag", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp) },
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
        colors = journalFieldColors()
      )
      Button(
        onClick = {
          dragLock.value = true
          datePickerOpen = true
        },
        modifier = Modifier.width(124.dp).height(48.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = Color.White.copy(alpha = 0.10f),
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text(selectedDate?.format(journalDateFormatter) ?: "Filter by day", color = Color.White, fontSize = 11.sp)
      }
      if (search.isNotBlank() || activeTag != null || selectedDate != null) {
        TextButton(
          onClick = { search = ""; activeTag = null; selectedDate = null },
          modifier = Modifier.height(48.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
          Text("Clear", color = RFColors.TextSecondary, fontSize = 11.sp)
        }
      }
    }
    if (datePickerOpen) {
      val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
      )
      DatePickerDialog(
        onDismissRequest = { datePickerOpen = false },
        confirmButton = {
          Material3TextButton(onClick = {
            pickerState.selectedDateMillis?.let { millis ->
              selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            }
            datePickerOpen = false
          }) { Text("Apply", color = RFColors.AccentRed) }
        },
        dismissButton = {
          Material3TextButton(onClick = { datePickerOpen = false }) {
            Text("Cancel", color = Color.White)
          }
        }
      ) { DatePicker(state = pickerState) }
    }
    activeTag?.let { tag ->
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Filtering by #$tag", color = Color.White, fontSize = 11.sp)
        Spacer(Modifier.width(8.dp))
        Button(
          onClick = { activeTag = null },
          colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.White.copy(alpha = 0.12f),
            contentColor = Color.White
          )
        ) { Text("Clear", fontSize = 11.sp) }
      }
    }
    if (activeDeleteMessage) {
      Text(
        text = "Currently Editing",
        color = RFColors.TextTertiary,
        fontSize = 11.sp,
        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp)
      )
    }

    if (filtered.isEmpty()) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No Pocket entries yet.", color = RFColors.TextSecondary)
      }
    } else {
      Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
          val centerX = size.width / 2f
          drawLine(
            color = Color.White.copy(alpha = 0.24f),
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = 2.dp.toPx()
          )
        }
        val timelineItems = buildList<TimelineItem> {
          filtered.forEachIndexed { index, entry ->
            val previousDate = filtered.getOrNull(index - 1)?.metadata?.createdAt
            val entryDate = localDate(entry.metadata.createdAt)
            if (previousDate == null || localDate(previousDate) != entryDate) {
              add(TimelineItem.Day(entryDate))
            }
            add(TimelineItem.Entry(entry, index % 2 == 0))
          }
        }
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp).padding(bottom = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(timelineItems, key = {
            when (it) {
              is TimelineItem.Day -> "day-${it.date}"
              is TimelineItem.Entry -> "entry-${it.entry.metadata.id}"
            }
          }) { item ->
            when (item) {
              is TimelineItem.Day -> TimelineDayMarker(item.date)
              is TimelineItem.Entry -> TimelineEntryRow(
                entry = item.entry,
                isLeft = item.isLeft,
                onOpen = {
                  scope.launch {
                    PocketDraftCoordinator.openDraft(item.entry.metadata.id)
                    wm?.openWindow(OverlayType.POCKET_EDITOR)
                  }
                },
                onTagClick = { tag ->
                  search = ""
                  activeTag = tag.lowercase()
                },
                onDelete = {
                  if (PocketDraftCoordinator.activeDraftId.value != item.entry.metadata.id) {
                    scope.launch {
                      PocketDraftCoordinator.deleteDraft(item.entry.metadata.id)
                      PocketDraftCoordinator.refreshEntries()
                    }
                  } else {
                    activeDeleteMessage = true
                  }
                }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TimelineDayMarker(date: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.Center
  ) {
    Text(
      text = date,
      color = RFColors.TextPrimary,
      fontSize = 11.sp,
      modifier = Modifier
        .background(Color(0xFF202020), RoundedCornerShape(10.dp))
        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
        .padding(horizontal = 12.dp, vertical = 4.dp)
    )
  }
}

@Composable
private fun journalFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
  textColor = Color.White,
  cursorColor = RFColors.AccentRed,
  focusedBorderColor = RFColors.AccentRed,
  unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
  focusedLabelColor = RFColors.AccentRed,
  unfocusedLabelColor = RFColors.TextTertiary
)

private fun isOnDate(timestamp: Long, selectedDate: LocalDate?): Boolean {
  val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
  return selectedDate == null || date == selectedDate
}

@Composable
private fun TimelineEntryRow(
  entry: PocketEntry,
  isLeft: Boolean,
  onTagClick: (String) -> Unit,
  onOpen: () -> Unit,
  onDelete: () -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    val textContent: @Composable () -> Unit = {
      Column(modifier = Modifier.weight(1f)) {
        val created = formatDateTime(entry.metadata.createdAt)
        val edited = formatDateTime(entry.metadata.updatedAt)
        Text(
          text = entry.metadata.title.ifBlank { "Journal Entry $created" },
          color = RFColors.TextPrimary,
          fontSize = 14.sp
        )
        Text("Created $created", color = RFColors.TextSecondary, fontSize = 10.sp)
        Text("Last Edited $edited", color = RFColors.TextTertiary, fontSize = 10.sp)
        entry.markdown.lineSequence().firstOrNull { it.isNotBlank() }?.let {
          Text(it, color = RFColors.TextSecondary, fontSize = 12.sp, maxLines = 2)
        }
        PocketTagChips(entry.tags.map { it.tag }, onTagClick)
      }
    }
    val deleteButton: @Composable () -> Unit = {
      val editing = PocketDraftCoordinator.activeDraftId.collectAsState().value == entry.metadata.id
      IconButton(onClick = onDelete, enabled = !editing) {
        Text("X", color = if (editing) RFColors.TextDisabled else RFColors.AccentRed)
      }
    }
    val card: @Composable () -> Unit = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(RFColors.CardBackground, RoundedCornerShape(8.dp))
          .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
          .clickable(onClick = onOpen)
          .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isLeft) {
          deleteButton()
          textContent()
        } else {
          textContent()
          deleteButton()
        }
      }
    }
    if (isLeft) {
      Box(Modifier.weight(1f)) { card() }
      TimelineNode()
      Spacer(Modifier.weight(1f))
    } else {
      Spacer(Modifier.weight(1f))
      TimelineNode()
      Box(Modifier.weight(1f)) { card() }
    }
  }
}

@Composable
private fun PocketTagChips(tags: List<String>, onTagClick: (String) -> Unit) {
  if (tags.isEmpty()) return
  FlowRow(
    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    tags.take(6).forEach { tag ->
      Text(
        text = "#$tag",
        color = Color.White,
        fontSize = 9.sp,
        modifier = Modifier
          .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
          .padding(horizontal = 6.dp, vertical = 2.dp)
          .clickable { onTagClick(tag) }
      )
    }
  }
}

@Composable
private fun RowScope.TimelineNode() {
  Box(
    modifier = Modifier.width(18.dp),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .background(RFColors.AccentRed, androidx.compose.foundation.shape.CircleShape)
        .padding(4.dp)
    )
  }
}

private fun localDate(timestamp: Long): String = journalDateFormatter.format(
  Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
)

private fun formatDateTime(timestamp: Long): String = journalDateTimeFormatter.format(
  Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
)

private sealed interface TimelineItem {
  data class Day(val date: String) : TimelineItem
  data class Entry(val entry: PocketEntry, val isLeft: Boolean) : TimelineItem
}
