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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.reoky.raidframer.core.pocket.PocketDraftCoordinator
import com.reoky.raidframer.core.pocket.PocketEntry
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val journalDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val journalDateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")

@Composable
fun PocketJournalOverlay(wm: WindowManager? = null) {
  val scope = rememberCoroutineScope()
  val entries by PocketDraftCoordinator.entries.collectAsState()
  var search by remember { mutableStateOf("") }
  var activeDeleteMessage by remember { mutableStateOf(false) }

  val filtered = entries.filter { entry ->
    search.isBlank() || entry.metadata.title.contains(search, ignoreCase = true) ||
      entry.tags.any { it.tag.contains(search, ignoreCase = true) }
  }

  Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.66f))) {
    TitleBarComponent(
      title = "Pocket Journal",
      onClose = { wm?.closeWindow(OverlayType.POCKET_JOURNAL) }
    )
    var filtersExpanded by remember { mutableStateOf(true) }
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Button(
        onClick = { filtersExpanded = !filtersExpanded },
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White.copy(alpha = 0.12f), contentColor = Color.White)
      ) { Text(if (filtersExpanded) "Hide Filters" else "Show Filters", fontSize = 12.sp) }
      Spacer(Modifier.width(8.dp))
      Button(
        onClick = {
          scope.launch {
            PocketDraftCoordinator.createDraft()
            wm?.openWindow(OverlayType.POCKET_EDITOR)
          }
        },
        colors = ButtonDefaults.buttonColors(backgroundColor = RFColors.AccentRed, contentColor = Color.White)
      ) { Text("New Entry") }
    }
    if (filtersExpanded) {
      OutlinedTextField(
        value = search,
        onValueChange = { search = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
        singleLine = true,
        label = { Text("Search tags or titles") },
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
        colors = TextFieldDefaults.outlinedTextFieldColors(
          textColor = Color.White,
          cursorColor = Color.White,
          focusedBorderColor = Color.White,
          unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
          focusedLabelColor = Color.White,
          unfocusedLabelColor = Color.White.copy(alpha = 0.70f)
        )
      )
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
private fun TimelineEntryRow(
  entry: PocketEntry,
  isLeft: Boolean,
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
        PocketTagChips(entry.tags.map { it.tag })
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
private fun PocketTagChips(tags: List<String>) {
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
