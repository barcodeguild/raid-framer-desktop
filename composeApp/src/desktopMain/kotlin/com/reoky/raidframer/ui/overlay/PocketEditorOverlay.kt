package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import com.reoky.raidframer.core.pocket.PocketDraftCoordinator
import com.reoky.raidframer.core.pocket.PocketMarkdownBlock
import com.reoky.raidframer.core.pocket.PocketMarkdownInline
import com.reoky.raidframer.core.pocket.PocketAttachmentPicker
import com.reoky.raidframer.core.pocket.PocketAttachmentRejection
import com.reoky.raidframer.core.pocket.PocketAttachmentResult
import com.reoky.raidframer.core.pocket.parsePocketMarkdown
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.database.PocketAttachmentEntity
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.component.SESSION_TYPES
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.pocket_attachment_content_desc
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_cancel
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_err_attachment_limit
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_err_entry_not_found
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_err_invalid_path
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_err_no_entry_open
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_err_not_image
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_insert
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_insert_link_title
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_link_text_default
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_link_title_label
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_link_url_label
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_markdown_label
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_tab_edit
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_tab_preview
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_title
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_title_label
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_toolbar_image
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_toolbar_link
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_toolbar_current_target
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_toolbar_target_guild
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_toolbar_event
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_toolbar_raid
import raid_framer_desktop.composeapp.generated.resources.pocket_editor_event_search_hint
import com.reoky.raidframer.core.helpers.togglePocketJournal
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.LocalDragLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.file.Path
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.datatransfer.DataFlavor
import java.awt.Toolkit
import java.io.File
import java.util.UUID
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.ImageInfo

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PocketEditorOverlay(wm: WindowManager? = null) {
  val draft by PocketDraftCoordinator.activeDraft.collectAsState()
  var title by remember(draft?.metadata?.id) { mutableStateOf(draft?.metadata?.title.orEmpty()) }
  var markdownValue by remember(draft?.metadata?.id) { mutableStateOf(TextFieldValue(draft?.markdown.orEmpty())) }
  val markdown = markdownValue.text
  val draftId = draft?.metadata?.id
  val allEntries by PocketDraftCoordinator.entries.collectAsState()
  var selectedTab by remember(draftId) {
    val createdAt = draft?.metadata?.createdAt
    val newestAt = allEntries.maxOfOrNull { it.metadata.createdAt }
    val isNewestOrDraft = createdAt != null && newestAt != null && createdAt >= newestAt
    mutableStateOf(if (isNewestOrDraft) 0 else 1)
  }
  var attachmentMessage by remember { mutableStateOf<String?>(null) }
  var linkDialogOpen by remember { mutableStateOf(false) }
  var linkUrl by remember { mutableStateOf("") }
  var linkTitle by remember { mutableStateOf("") }
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  val dragLock = LocalDragLock.current
  val errNotImage = stringResource(Res.string.pocket_editor_err_not_image)
  val errNoEntryOpen = stringResource(Res.string.pocket_editor_err_no_entry_open)
  val errAttachmentLimit = stringResource(Res.string.pocket_editor_err_attachment_limit)
  val errEntryNotFound = stringResource(Res.string.pocket_editor_err_entry_not_found)
  val errInvalidPath = stringResource(Res.string.pocket_editor_err_invalid_path)
  val linkTextDefault = stringResource(Res.string.pocket_editor_link_text_default)
  var lastSelection by remember(draft?.metadata?.id) { mutableStateOf<TextRange?>(null) }
  var editorFocused by remember { mutableStateOf(false) }
  var editorHovered by remember { mutableStateOf(false) }
  var titleFocused by remember { mutableStateOf(false) }
  var titleHovered by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { dragLock.value = true }
  LaunchedEffect(editorFocused, editorHovered, titleFocused, titleHovered) {
    dragLock.value = (editorFocused && editorHovered) || (titleFocused && titleHovered)
  }

  LaunchedEffect(title, markdown, draft?.metadata?.id) {
    if (draft != null) {
      delay(750L)
      PocketDraftCoordinator.updateDraft(title, markdown)
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.66f))) {
    TitleBarComponent(
      title = stringResource(Res.string.pocket_editor_title),
      onClose = {
        PocketDraftCoordinator.closeEditorSession()
        wm?.closeWindow(OverlayType.POCKET_EDITOR)
      },
rightActions = {
        val editorInteractionSource = remember { MutableInteractionSource() }
        val isEditorHovered by editorInteractionSource.collectIsHoveredAsState()
        IconButton(
          onClick = { togglePocketJournal(wm) },
          modifier = Modifier.size(28.dp).padding(end = 2.dp)
        ) {
          Text(
            "\uf02d",
            color = if (isEditorHovered) RFColors.AccentRed else Color.White,
            fontFamily = FontsHelper.faSolid(),
            fontSize = 14.sp,
            modifier = Modifier.hoverable(editorInteractionSource)
          )
        }
      }
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp)
        .background(Color(0xFF141414).copy(alpha = 0.78f), RoundedCornerShape(14.dp))
        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
        .padding(2.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      listOf(Res.string.pocket_editor_tab_edit, Res.string.pocket_editor_tab_preview).forEachIndexed { index, labelRes ->
        Button(
          onClick = { selectedTab = index },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = if (selectedTab == index) RFColors.AccentRed.copy(alpha = 0.82f) else Color.Transparent,
            contentColor = Color.White
          ),
          elevation = ButtonDefaults.elevation(defaultElevation = 2.dp)
        ) { Text(stringResource(labelRes), fontSize = 12.sp) }
      }
    }
    if (selectedTab == 0) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          modifier = Modifier.weight(1f)
            .onFocusChanged { titleFocused = it.isFocused }
            .onPointerEvent(PointerEventType.Enter) { titleHovered = true }
            .onPointerEvent(PointerEventType.Exit) { titleHovered = false },
          singleLine = true,
          label = { Text(stringResource(Res.string.pocket_editor_title_label), color = Color.White.copy(alpha = 0.85f)) },
          textStyle = TextStyle(color = Color.White),
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
      PocketTagChips(draft?.tags.orEmpty().map { it.tag })
      MarkdownToolbar(
        onAction = { action ->
          when (action) {
            MarkdownAction.IMAGE -> {
              scope.launch {
                val selected = PocketAttachmentPicker.chooseImage(
                  temporarilyHide = listOfNotNull(
                    wm?.nativeWindow(OverlayType.POCKET_EDITOR),
                    wm?.nativeWindow(OverlayType.POCKET_JOURNAL)
                  )
                )
                if (selected != null) {
                  addImageFromSource(
                    source = selected,
                    markdown = markdown,
                    onMarkdownUpdated = { markdownValue = it },
                    onMessage = { attachmentMessage = it },
                    errNotImage = errNotImage,
                    errNoEntryOpen = errNoEntryOpen,
                    errAttachmentLimit = errAttachmentLimit,
                    errEntryNotFound = errEntryNotFound,
                    errInvalidPath = errInvalidPath,
                  )
                }
              }
            }

            MarkdownAction.LINK -> {
              linkUrl = ""
              linkTitle = markdownValue.selectedText().ifBlank { linkTextDefault }
              linkDialogOpen = true
            }

            MarkdownAction.BOLD -> markdownValue = applyMarkdown(markdownValue, "**", lastSelection)
            MarkdownAction.ITALIC -> markdownValue = applyMarkdown(markdownValue, "*", lastSelection)
            MarkdownAction.STRIKE -> markdownValue = applyMarkdown(markdownValue, "~~", lastSelection)
            MarkdownAction.BULLET -> markdownValue = insertMarkdown(markdownValue, "- ")
            MarkdownAction.CODE -> markdownValue = applyMarkdown(markdownValue, "`", lastSelection)
            MarkdownAction.CURRENT_TARGET -> {
              val target = AppState.selectedTarget.value
              if (!target.isNullOrBlank()) {
                markdownValue = insertMarkdown(markdownValue, "@$target ")
              }
            }
            MarkdownAction.TARGET_GUILD -> {
              val target = AppState.selectedTarget.value
              if (!target.isNullOrBlank()) {
                val card = PlayerCacheInteractor.getCard(target)
                val guild = card?.lastKnownGuild
                if (!guild.isNullOrBlank()) {
                  val guildTag = guild.replace(" ", "_")
                  markdownValue = insertMarkdown(markdownValue, "#$guildTag ")
                }
              }
            }
            MarkdownAction.RAID -> {
              val raidParties = PlayerCacheInteractor.getRaidById(0).value
              val names = raidParties.flatten().map { it.playerName }.filter { it.isNotBlank() }
              if (names.isNotEmpty()) {
                val tagString = names.joinToString(" ") { "@$it" } + " "
                markdownValue = insertMarkdown(markdownValue, tagString)
              }
            }
            MarkdownAction.EVENT -> { /* handled via onEventSelected */ }
          }
        },
        onEventSelected = { sessionType ->
          val tag = sessionType
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
          if (tag.isNotBlank() && tag != "don_t_care") {
            markdownValue = insertMarkdown(markdownValue, "#$tag ")
          }
        }
      )
      AttachmentStrip(
        attachments = draft?.attachments.orEmpty(),
        onRemove = { attachmentId ->
          scope.launch {
            PocketDraftCoordinator.removeAttachment(attachmentId, markdown)
              ?.let {
                markdownValue = TextFieldValue(it.markdown)
                attachmentMessage = null
              }
          }
        }
      )
      attachmentMessage?.let {
        Text(it, color = RFColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp))
      }
      OutlinedTextField(
        value = markdownValue,
        onValueChange = {
          markdownValue = it
          if (!it.selection.collapsed) lastSelection = it.selection
        },
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(10.dp)
          .onFocusChanged { editorFocused = it.isFocused }
          .onPointerEvent(PointerEventType.Enter) { editorHovered = true }
          .onPointerEvent(PointerEventType.Exit) { editorHovered = false }
          .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.V && event.isCtrlPressed) {
              val tempFile = readClipboardImage()
              if (tempFile != null) {
                scope.launch {
                  addImageFromSource(
                    source = tempFile.toPath(),
                    markdown = markdown,
                    onMarkdownUpdated = { markdownValue = it },
                    onMessage = { attachmentMessage = it },
                    errNotImage = errNotImage,
                    errNoEntryOpen = errNoEntryOpen,
                    errAttachmentLimit = errAttachmentLimit,
                    errEntryNotFound = errEntryNotFound,
                    errInvalidPath = errInvalidPath,
                  )
                }
                true
              } else {
                false
              }
            } else {
              false
            }
          },
        label = { Text(stringResource(Res.string.pocket_editor_markdown_label), color = Color.White.copy(alpha = 0.85f)) },
        textStyle = TextStyle(color = Color.White),
        colors = TextFieldDefaults.outlinedTextFieldColors(
          textColor = Color.White,
          cursorColor = RFColors.AccentRed,
          focusedBorderColor = RFColors.AccentRed,
          unfocusedBorderColor = Color.White.copy(alpha = 0.30f),
          focusedLabelColor = Color.White,
          unfocusedLabelColor = Color.White.copy(alpha = 0.78f)
        )
      )
    } else {
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(10.dp)
          .verticalScroll(rememberScrollState())
      ) {
        PocketMarkdownPreview(markdown, draft?.metadata?.markdownPath)
      }
    }
    if (linkDialogOpen) {
      AlertDialog(
        onDismissRequest = { linkDialogOpen = false },
        title = { Text(stringResource(Res.string.pocket_editor_insert_link_title), color = Color.White) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = linkTitle,
              onValueChange = { linkTitle = it },
              label = { Text(stringResource(Res.string.pocket_editor_link_title_label), color = Color.White) },
              textStyle = TextStyle(color = Color.White),
              colors = editorFieldColors()
            )
            OutlinedTextField(
              value = linkUrl,
              onValueChange = { linkUrl = it },
              label = { Text(stringResource(Res.string.pocket_editor_link_url_label), color = Color.White) },
              textStyle = TextStyle(color = Color.White),
              colors = editorFieldColors()
            )
          }
        },
        confirmButton = {
          TextButton(onClick = {
            if (linkUrl.isNotBlank()) markdownValue =
              insertMarkdown(markdownValue, "[${linkTitle.ifBlank { linkUrl }}]($linkUrl)")
            linkDialogOpen = false
          }) { Text(stringResource(Res.string.pocket_editor_insert), color = RFColors.AccentRed) }
        },
        dismissButton = {
          TextButton(onClick = { linkDialogOpen = false }) {
            Text(
              stringResource(Res.string.pocket_editor_cancel),
              color = Color.White
            )
          }
        },
        backgroundColor = Color(0xFF202020),
        contentColor = Color.White
      )
    }
  }
}

private enum class MarkdownAction { BOLD, ITALIC, STRIKE, CODE, BULLET, LINK, IMAGE, CURRENT_TARGET, TARGET_GUILD, EVENT, RAID }

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MarkdownToolbar(onAction: (MarkdownAction) -> Unit, onEventSelected: (String) -> Unit) {
  var eventMenuExpanded by remember { mutableStateOf(false) }
  var eventSearchQuery by remember { mutableStateOf("") }
  var eventHighlightedIndex by remember { mutableStateOf(0) }
  val eventFocusRequester = remember { FocusRequester() }
  val normalizedQuery = eventSearchQuery.trim()
  val filteredEvents = SESSION_TYPES
    .filter { it != "Don't Care" && (normalizedQuery.isBlank() || it.contains(normalizedQuery, ignoreCase = true)) }
    .sortedWith(compareBy { !it.startsWith(normalizedQuery, ignoreCase = true) })

  LaunchedEffect(eventMenuExpanded) {
    if (eventMenuExpanded) {
      eventSearchQuery = ""
      eventHighlightedIndex = 0
      eventFocusRequester.requestFocus()
    }
  }

  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
      .background(Color(0xFF141414).copy(alpha = 0.92f), RoundedCornerShape(10.dp))
      .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
      .padding(horizontal = 4.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    listOf(
      MarkdownAction.BOLD to "B", MarkdownAction.ITALIC to "I", MarkdownAction.STRIKE to "S",
      MarkdownAction.CODE to "<>", MarkdownAction.BULLET to "•",
      MarkdownAction.LINK to stringResource(Res.string.pocket_editor_toolbar_link),
      MarkdownAction.IMAGE to stringResource(Res.string.pocket_editor_toolbar_image),
      MarkdownAction.CURRENT_TARGET to stringResource(Res.string.pocket_editor_toolbar_current_target),
      MarkdownAction.TARGET_GUILD to stringResource(Res.string.pocket_editor_toolbar_target_guild)
    ).forEach { (action, label) ->
      TextButton(onClick = { onAction(action) }) {
        Text(label, color = if (action == MarkdownAction.IMAGE || action == MarkdownAction.CURRENT_TARGET || action == MarkdownAction.TARGET_GUILD) RFColors.AccentRed else Color.White, fontSize = 11.sp)
      }
    }
    Box {
      TextButton(onClick = { eventMenuExpanded = true }) {
        Text(stringResource(Res.string.pocket_editor_toolbar_event), color = RFColors.AccentRed, fontSize = 11.sp)
      }
      if (eventMenuExpanded) {
        Popup(
          onDismissRequest = { eventMenuExpanded = false },
          properties = PopupProperties(focusable = true)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E1E1E),
            border = BorderStroke(1.dp, RFColors.CardBorder),
            elevation = 8.dp,
            modifier = Modifier.width(260.dp).heightIn(max = 320.dp)
          ) {
            Column(modifier = Modifier.padding(4.dp)) {
              OutlinedTextField(
                value = eventSearchQuery,
                onValueChange = { eventSearchQuery = it; eventHighlightedIndex = 0 },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                  .focusRequester(eventFocusRequester)
                  .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) false
                    else when (event.key) {
                      Key.DirectionDown -> { if (filteredEvents.isNotEmpty()) eventHighlightedIndex = (eventHighlightedIndex + 1) % filteredEvents.size; true }
                      Key.DirectionUp -> { if (filteredEvents.isNotEmpty()) eventHighlightedIndex = (eventHighlightedIndex - 1 + filteredEvents.size) % filteredEvents.size; true }
                      Key.Enter -> { filteredEvents.getOrNull(eventHighlightedIndex)?.let { onEventSelected(it); eventMenuExpanded = false }; true }
                      Key.Escape -> { eventMenuExpanded = false; true }
                      else -> false
                    }
                  },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                placeholder = { Text(stringResource(Res.string.pocket_editor_event_search_hint), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                  textColor = Color.White,
                  cursorColor = RFColors.AccentRed,
                  focusedBorderColor = RFColors.AccentRed,
                  unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                  focusedLabelColor = Color.White,
                  unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                )
              )
              Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(max = 260.dp)) {
                filteredEvents.forEachIndexed { index, type ->
                  DropdownMenuItem(
                    onClick = { onEventSelected(type); eventMenuExpanded = false },
                    modifier = Modifier.background(
                      if (index == eventHighlightedIndex) RFColors.AccentRed.copy(alpha = 0.15f) else Color.Transparent
                    )
                  ) {
                    Text(type, color = RFColors.TextPrimary, fontSize = 12.sp, maxLines = 1)
                  }
                }
              }
            }
          }
        }
      }
    }
    TextButton(onClick = { onAction(MarkdownAction.RAID) }) {
      Text(stringResource(Res.string.pocket_editor_toolbar_raid), color = RFColors.AccentRed, fontSize = 11.sp)
    }
  }
}

private fun applyMarkdown(
  value: TextFieldValue,
  marker: String,
  rememberedSelection: TextRange?
): TextFieldValue {
  val selection = if (!value.selection.collapsed) value.selection else rememberedSelection ?: value.selection
  if (selection.collapsed) return value.copy(
    text = value.text + marker + marker,
    selection = TextRange(value.text.length + marker.length * 2)
  )
  val selected = value.text.substring(selection.min, selection.max)
  val replacement = "$marker$selected$marker"
  val text = value.text.replaceRange(selection.min, selection.max, replacement)
  return value.copy(
    text = text,
    selection = TextRange(selection.min, selection.min + replacement.length)
  )
}

private fun insertMarkdown(value: TextFieldValue, insertion: String): TextFieldValue {
  val selection = value.selection
  val prefix = if (value.text.isBlank() || value.text.endsWith("\n")) "" else "\n"
  val replacement = prefix + insertion
  val text = value.text.replaceRange(selection.min, selection.max, replacement)
  val cursor = selection.min + replacement.length
  return value.copy(text = text, selection = TextRange(cursor))
}

private fun TextFieldValue.selectedText(): String = text.substring(selection.min, selection.max)

@Composable
private fun editorFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
  textColor = Color.White,
  cursorColor = RFColors.AccentRed,
  focusedBorderColor = RFColors.AccentRed,
  unfocusedBorderColor = Color.White.copy(alpha = 0.30f),
  focusedLabelColor = Color.White,
  unfocusedLabelColor = Color.White.copy(alpha = 0.78f)
)

private fun appendImageReference(markdown: String, fileName: String): String {
  val separator = if (markdown.isBlank() || markdown.endsWith("\n")) "" else "\n"
  return "$markdown${separator}\n![Screenshot]($fileName)\n"
}

@Composable
private fun PocketTagChips(tags: List<String>) {
  if (tags.isEmpty()) return
  FlowRow(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    tags.forEach { tag ->
      Text(
        text = "#$tag",
        color = Color.White,
        fontSize = 11.sp,
        modifier = Modifier
          .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
          .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp, vertical = 3.dp)
      )
    }
  }
}

@Composable
private fun AttachmentStrip(
  attachments: List<PocketAttachmentEntity>,
  onRemove: (String) -> Unit,
) {
  if (attachments.isEmpty()) return
  FlowRow(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    attachments.forEach { attachment ->
      Row(
        modifier = Modifier.background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp)).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(attachment.relativePath, color = Color.White, fontSize = 11.sp)
        IconButton(onClick = { onRemove(attachment.id) }) {
          Text("X", color = RFColors.AccentRed, fontSize = 11.sp)
        }
      }
    }
  }
}

@Composable
private fun PocketMarkdownPreview(markdown: String, markdownPath: String?) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    parsePocketMarkdown(markdown).forEach { block ->
      when (block) {
        is PocketMarkdownBlock.Paragraph -> PocketInlineText(block.content, markdownPath)
        is PocketMarkdownBlock.Heading -> Text(
          text = block.content.toPlainText(),
          color = Color.White,
          fontSize = (22 - block.level * 2).coerceAtLeast(14).sp
        )

        is PocketMarkdownBlock.BulletList -> block.items.forEach { item ->
          Row { Text("• ", color = Color.White); PocketInlineText(item, markdownPath) }
        }

        is PocketMarkdownBlock.OrderedList -> block.items.forEachIndexed { index, item ->
          Row { Text("${index + 1}. ", color = Color.White); PocketInlineText(item, markdownPath) }
        }

        is PocketMarkdownBlock.Quote -> Text(
          text = "> ${block.content.toPlainText()}",
          color = RFColors.TextSecondary,
          fontSize = 14.sp
        )

        is PocketMarkdownBlock.CodeBlock -> Text(
          text = block.code,
          color = Color.White,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.background(Color.Black.copy(alpha = 0.45f)).padding(8.dp)
        )
      }
    }
  }
}

@Composable
private fun PocketInlineText(content: List<PocketMarkdownInline>, markdownPath: String?) {
  val uriHandler = LocalUriHandler.current
  val style = TextStyle(color = Color.White, fontSize = 14.sp)
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    content.forEach { inline ->
      when (inline) {
        is PocketMarkdownInline.Image -> PocketMarkdownImage(inline, markdownPath)
        is PocketMarkdownInline.Link -> ClickableText(
          text = inline.toAnnotatedString(),
          style = style,
          modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
          onClick = { uriHandler.openUri(inline.destination) }
        )
        else -> BasicText(
          text = inline.toAnnotatedString(),
          style = style
        )
      }
    }
  }
}

private fun PocketMarkdownInline.toAnnotatedString(): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
  fun appendInline(inline: PocketMarkdownInline, style: SpanStyle = SpanStyle()) {
    when (inline) {
      is PocketMarkdownInline.Plain -> withStyle(style) { append(inline.value) }
      is PocketMarkdownInline.Strong -> inline.content.forEach { appendInline(it, style.merge(SpanStyle(fontWeight = FontWeight.Bold))) }
      is PocketMarkdownInline.Emphasis -> inline.content.forEach { appendInline(it, style.merge(SpanStyle(fontStyle = FontStyle.Italic))) }
      is PocketMarkdownInline.Strikethrough -> inline.content.forEach { appendInline(it, style.merge(SpanStyle(textDecoration = TextDecoration.LineThrough))) }
      is PocketMarkdownInline.Code -> withStyle(style.merge(SpanStyle(fontFamily = FontFamily.Monospace))) { append(inline.value) }
      is PocketMarkdownInline.Link -> inline.label.forEach { appendInline(it, style.merge(SpanStyle(color = Color(0xFF64B5F6), textDecoration = TextDecoration.Underline))) }
      is PocketMarkdownInline.Break -> append("\n")
      is PocketMarkdownInline.Image -> append("[Image: ${inline.alt.ifBlank { inline.destination }}]")
    }
  }
  appendInline(this@toAnnotatedString)
}

@Composable
private fun PocketMarkdownImage(image: PocketMarkdownInline.Image, markdownPath: String?) {
  val bitmap = remember(markdownPath, image.destination) {
    loadPocketImage(image.destination, markdownPath)?.let { bufferedImage ->
      val colorType = ColorType.RGBA_8888
      val imageInfo = ImageInfo(
        bufferedImage.width,
        bufferedImage.height,
        colorType,
        ColorAlphaType.UNPREMUL
      )
      val pixels = IntArray(bufferedImage.width * bufferedImage.height)
      bufferedImage.getRGB(0, 0, bufferedImage.width, bufferedImage.height, pixels, 0, bufferedImage.width)
      val bytes = ByteArray(pixels.size * 4)
      pixels.forEachIndexed { index, pixel ->
        bytes[index * 4] = ((pixel shr 16) and 0xff).toByte()
        bytes[index * 4 + 1] = ((pixel shr 8) and 0xff).toByte()
        bytes[index * 4 + 2] = (pixel and 0xff).toByte()
        bytes[index * 4 + 3] = ((pixel ushr 24) and 0xff).toByte()
      }
      SkiaImage.makeRaster(imageInfo, bytes, bufferedImage.width * 4).toComposeImageBitmap()
    }
  }
  if (bitmap == null) {
    Text("[Image: ${image.alt.ifBlank { image.destination }}]", color = Color.White, fontSize = 12.sp)
  } else {
    // Cap the width to 2/3 of the available area while letting the height follow the
    // image's natural aspect ratio so it renders in position and at full size.
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      Image(
        bitmap = bitmap,
        contentDescription = image.alt.ifBlank { stringResource(Res.string.pocket_attachment_content_desc) },
        modifier = Modifier
          .fillMaxWidth(2f / 3f)
          .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat()),
        contentScale = ContentScale.Fit
      )
    }
  }
}

private fun loadPocketImage(destination: String, markdownPath: String?): BufferedImage? {
  val path = runCatching {
    val reference = Path.of(destination)
    if (reference.isAbsolute) reference else markdownPath?.let { Path.of(it).parent.resolve(reference).normalize() }
  }.getOrNull() ?: return null
  return runCatching { ImageIO.read(path.toFile()) }.getOrNull()
}

private fun readClipboardImage(): File? {
  return try {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val contents = clipboard.getContents(null) ?: return null
    if (!contents.isDataFlavorSupported(DataFlavor.imageFlavor)) return null
    val image = contents.getTransferData(DataFlavor.imageFlavor) as? BufferedImage ?: return null
    val tempFile = File.createTempFile("pocket-paste-${UUID.randomUUID()}", ".png")
    tempFile.deleteOnExit()
    ImageIO.write(image, "png", tempFile)
    tempFile
  } catch (_: Exception) {
    null
  }
}

private suspend fun addImageFromSource(
  source: Path,
  markdown: String,
  onMarkdownUpdated: (TextFieldValue) -> Unit,
  onMessage: (String?) -> Unit,
  errNotImage: String,
  errNoEntryOpen: String,
  errAttachmentLimit: String,
  errEntryNotFound: String,
  errInvalidPath: String,
) {
  val image = runCatching { ImageIO.read(source.toFile()) }.getOrNull()
  if (image == null) {
    onMessage(errNotImage)
    return
  }
  val draft = PocketDraftCoordinator.activeDraft.value
  val draftId = draft?.metadata?.id
  if (draftId == null) {
    onMessage(errNoEntryOpen)
    return
  }
  val name = PocketDraftCoordinator.nextAttachmentName(draftId)
  if (name == null) {
    onMessage(errAttachmentLimit)
    return
  }
  when (val result = PocketDraftCoordinator.addAttachment(
    source = source,
    relativePath = name,
    mimeType = "image/png",
    markdown = appendImageReference(markdown, name)
  )) {
    is PocketAttachmentResult.Added -> {
      onMarkdownUpdated(TextFieldValue(result.entry.markdown))
      onMessage(null)
    }
    is PocketAttachmentResult.Rejected -> {
      onMessage(when (result.reason) {
        PocketAttachmentRejection.ATTACHMENT_LIMIT_REACHED -> errAttachmentLimit
        PocketAttachmentRejection.ENTRY_NOT_FOUND -> errEntryNotFound
        PocketAttachmentRejection.INVALID_ATTACHMENT_PATH -> errInvalidPath
      })
    }
  }
}

private fun List<PocketMarkdownInline>.toPlainText(): String = joinToString("") { inline ->
  when (inline) {
    is PocketMarkdownInline.Plain -> inline.value
    is PocketMarkdownInline.Strong -> inline.content.toPlainText()
    is PocketMarkdownInline.Emphasis -> inline.content.toPlainText()
    is PocketMarkdownInline.Strikethrough -> inline.content.toPlainText()
    is PocketMarkdownInline.Code -> inline.value
    is PocketMarkdownInline.Link -> inline.label.toPlainText()
    is PocketMarkdownInline.Image -> "[Image: ${inline.alt.ifBlank { inline.destination }}]"
    PocketMarkdownInline.Break -> "\n"
  }
}
