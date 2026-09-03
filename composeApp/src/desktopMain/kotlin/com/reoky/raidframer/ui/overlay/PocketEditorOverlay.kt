package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
  LaunchedEffect(Unit) { dragLock.value = true }
  LaunchedEffect(editorFocused, editorHovered) {
    dragLock.value = editorFocused && editorHovered
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
          modifier = Modifier.weight(1f),
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
                  val image = runCatching { ImageIO.read(selected.toFile()) }.getOrNull()
                  if (image == null) {
                    attachmentMessage = errNotImage
                  } else {
                    val draftId = draft?.metadata?.id
                    if (draftId == null) {
                      attachmentMessage = errNoEntryOpen
                    } else {
                      val name = PocketDraftCoordinator.nextAttachmentName(draftId)
                      if (name == null) {
                        attachmentMessage = errAttachmentLimit
                      } else {
                        when (val result = PocketDraftCoordinator.addAttachment(
                          source = selected,
                          relativePath = name,
                          mimeType = "image/png",
                          markdown = appendImageReference(markdown, name)
                        )) {
                          is PocketAttachmentResult.Added -> {
                            markdownValue = TextFieldValue(result.entry.markdown)
                            attachmentMessage = null
                          }

                          is PocketAttachmentResult.Rejected -> {
                            attachmentMessage = when (result.reason) {
                              PocketAttachmentRejection.ATTACHMENT_LIMIT_REACHED -> errAttachmentLimit
                              PocketAttachmentRejection.ENTRY_NOT_FOUND -> errEntryNotFound
                              PocketAttachmentRejection.INVALID_ATTACHMENT_PATH -> errInvalidPath
                            }
                          }
                        }
                      }
                    }
                  }
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
          .onPointerEvent(PointerEventType.Exit) { editorHovered = false },
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

private enum class MarkdownAction { BOLD, ITALIC, STRIKE, CODE, BULLET, LINK, IMAGE }

@Composable
private fun MarkdownToolbar(onAction: (MarkdownAction) -> Unit) {
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
      MarkdownAction.IMAGE to stringResource(Res.string.pocket_editor_toolbar_image)
    ).forEach { (action, label) ->
      TextButton(onClick = { onAction(action) }) {
        Text(label, color = if (action == MarkdownAction.IMAGE) RFColors.AccentRed else Color.White, fontSize = 11.sp)
      }
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
