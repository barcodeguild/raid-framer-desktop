package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.Surface
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.TextButton
import androidx.compose.material.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import com.reoky.raidframer.ui.component.PocketEntryThumbnail
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.pocket.PocketDraftCoordinator
import com.reoky.raidframer.core.pocket.PocketEntry
import com.reoky.raidframer.core.pocket.PocketHtmlExporter
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.LocalDragLock
import com.reoky.raidframer.ui.component.TitleBarComponent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.spag_presenting
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

  Box(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.66f)))
    SpagPresentingBackground()

    Column(modifier = Modifier.fillMaxSize()) {
    TitleBarComponent(
      title = "Pocket Journal",
      onClose = { wm?.closeWindow(OverlayType.POCKET_JOURNAL) },
      rightActions = {
        IconButton(
          onClick = {
            scope.launch {
              PocketDraftCoordinator.createDraft()
              wm?.openWindow(OverlayType.POCKET_EDITOR)
            }
          },
          modifier = Modifier.size(28.dp)
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
      BasicTextField(
        value = search,
        onValueChange = { search = it },
        modifier = Modifier
          .weight(1f)
          .height(36.dp)
          .background(Color.Black.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
          .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp)),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
        cursorBrush = SolidColor(RFColors.AccentRed),
        decorationBox = { innerTextField ->
          Box(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
            if (search.isEmpty()) {
              Text("Search title or tag", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            }
            innerTextField()
          }
        }
      )
      Button(
        onClick = {
          dragLock.value = true
          datePickerOpen = true
        },
        modifier = Modifier.width(124.dp).height(36.dp),
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
          modifier = Modifier.height(36.dp),
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
                },
                onExport = {
                  scope.launch {
                    PocketHtmlExporter.exportEntryToHtml(item.entry)?.let { folder ->
                      java.awt.Desktop.getDesktop().open(folder.toFile())
                    }
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
}

@Composable
private fun SpagPresentingBackground() {
  val painter = painterResource(Res.drawable.spag_presenting)
  Row(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f))) {
    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
      Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.fillMaxHeight(0.55f).graphicsLayer { alpha = 0.90f },
        contentScale = ContentScale.Fit
      )
    }
    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
      Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.fillMaxHeight(0.55f).graphicsLayer {
          alpha = 0.90f
          scaleX = -1f
        },
        contentScale = ContentScale.Fit
      )
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
  onExport: () -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var popupHovered by remember(entry.metadata.id) { mutableStateOf(false) }
    var showPopup by remember(entry.metadata.id) { mutableStateOf(false) }
    // Keep the popup open while hovering the card OR the popup itself, with a short
    // debounce so moving the cursor from the card into the popup doesn't flicker.
    LaunchedEffect(isHovered, popupHovered) {
      if (isHovered || popupHovered) {
        showPopup = true
      } else {
        kotlinx.coroutines.delay(200)
        if (!isHovered && !popupHovered) showPopup = false
      }
    }
    val textContent: @Composable () -> Unit = {
      Column {
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
    val editing = PocketDraftCoordinator.activeDraftId.collectAsState().value == entry.metadata.id
    val deleteInteraction = remember { MutableInteractionSource() }
    val isDeleteHovered by deleteInteraction.collectIsHoveredAsState()
    val deleteButton: @Composable () -> Unit = {
      IconButton(
        onClick = onDelete,
        enabled = !editing,
        modifier = Modifier.size(32.dp)
      ) {
        Text(
          "X",
          color = if (editing) RFColors.TextDisabled else if (isDeleteHovered) Color.Red else Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.hoverable(interactionSource = deleteInteraction)
        )
      }
    }
    val exportInteraction = remember { MutableInteractionSource() }
    val isExportHovered by exportInteraction.collectIsHoveredAsState()
    val exportButton: @Composable () -> Unit = {
      IconButton(
        onClick = onExport,
        modifier = Modifier.size(32.dp)
      ) {
        Text(
          "\uF0C7",
          fontFamily = FontsHelper.faSolid(),
          fontSize = 13.sp,
          color = if (isExportHovered) Color.Red else Color.White,
          modifier = Modifier.hoverable(interactionSource = exportInteraction)
        )
      }
    }
    // Text always hugs the spline; export sits next to delete with delete outermost so the
    // left/right cards mirror each other across the spline (Y-axis symmetry).
    val card: @Composable () -> Unit = {
      val cardShape = TimelineCardShape(arrowPointsRight = isLeft)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(RFColors.CardBackground, cardShape)
          .border(1.dp, Color.White.copy(alpha = 0.12f), cardShape)
          .hoverable(interactionSource)
          .clickable(onClick = onOpen)
          .padding(start = 12.dp, end = if (isLeft) 18.dp else 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isLeft) {
          // spline on the right: text hugs the right (spline) edge; actions pinned to the left.
          deleteButton()
          exportButton()
          Spacer(Modifier.weight(1f))
          textContent()
        } else {
          // spline on the left: text hugs the left (spline) edge; actions pinned to the right.
          textContent()
          Spacer(Modifier.weight(1f))
          exportButton()
          deleteButton()
        }
      }
    }
    // Add horizontal margin on the spline-facing side so the extruded notch clears the spline.
    if (isLeft) {
      Box(Modifier.weight(1f).padding(end = 8.dp)) {
        card()
        if (showPopup) EntryHoverPopup(entry, isLeft, onHoverChange = { popupHovered = it })
      }
      TimelineNode()
      Spacer(Modifier.weight(1f))
    } else {
      Spacer(Modifier.weight(1f))
      TimelineNode()
      Box(Modifier.weight(1f).padding(start = 8.dp)) {
        card()
        if (showPopup) EntryHoverPopup(entry, isLeft, onHoverChange = { popupHovered = it })
      }
    }
  }
}

@Composable
private fun BoxScope.EntryHoverPopup(
  entry: PocketEntry,
  isLeft: Boolean,
  onHoverChange: (Boolean) -> Unit,
) {
  // Track hover on the popup itself so moving the cursor onto it keeps it open.
  val popupInteractionSource = remember { MutableInteractionSource() }
  val isPopupHovered by popupInteractionSource.collectIsHoveredAsState()
  LaunchedEffect(isPopupHovered) { onHoverChange(isPopupHovered) }
  Popup(
    alignment = if (isLeft) Alignment.TopEnd else Alignment.TopStart,
    offset = IntOffset(if (isLeft) 16 else -16, 44)
  ) {
    Surface(
      modifier = Modifier.width(300.dp).hoverable(popupInteractionSource),
      shape = RoundedCornerShape(8.dp),
      elevation = 8.dp,
      color = Color(0xFF171717),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
      PocketEntryThumbnail(entry)
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

/**
 * A custom card shape whose border extrudes into a triangular notch/pointer that faces the
 * timeline spline. `arrowPointsRight` carries the point off the right edge (for cards placed
 * left of the spline); otherwise the point extrudes off the left edge.
 */
private class TimelineCardShape(
  private val cornerRadius: Dp = 8.dp,
  private val arrowLength: Dp = 10.dp,
  private val arrowWidth: Dp = 16.dp,
  private val arrowPointsRight: Boolean,
) : Shape {

  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density
  ): Outline {
    val path = Path().apply {
      val c = with(density) { cornerRadius.toPx() }
      val aLen = with(density) { arrowLength.toPx() }
      val aWid = with(density) { arrowWidth.toPx() }
      val left = 0f
      val right = size.width
      val top = 0f
      val bottom = size.height
      val centerY = bottom / 2f

      moveTo(left + c, top)
      lineTo(right - c, top)
      arcTo(
        rect = Rect(right - c * 2, top, right, top + c * 2),
        startAngleDegrees = 270f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
      )

      if (arrowPointsRight) {
        // Point extrudes off the right edge, aimed at the spline.
        lineTo(right, centerY - aWid / 2f)
        lineTo(right + aLen, centerY)
        lineTo(right, centerY + aWid / 2f)
        lineTo(right, bottom - c)
      } else {
        lineTo(right, bottom - c)
      }
      arcTo(
        rect = Rect(right - c * 2, bottom - c * 2, right, bottom),
        startAngleDegrees = 0f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
      )
      lineTo(left + c, bottom)
      arcTo(
        rect = Rect(left, bottom - c * 2, left + c * 2, bottom),
        startAngleDegrees = 90f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
      )

      if (!arrowPointsRight) {
        // Point extrudes off the left edge, aimed at the spline.
        lineTo(left, centerY + aWid / 2f)
        lineTo(left - aLen, centerY)
        lineTo(left, centerY - aWid / 2f)
        lineTo(left, top + c)
      } else {
        lineTo(left, top + c)
      }
      arcTo(
        rect = Rect(left, top, left + c * 2, top + c * 2),
        startAngleDegrees = 180f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
      )
      close()
    }
    return Outline.Generic(path)
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
