package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.file.Path
import javax.imageio.ImageIO
import java.awt.image.BufferedImage

@Composable
fun PocketEditorOverlay(wm: WindowManager? = null) {
  val draft by PocketDraftCoordinator.activeDraft.collectAsState()
  var title by remember(draft?.metadata?.id) { mutableStateOf(draft?.metadata?.title.orEmpty()) }
  var markdown by remember(draft?.metadata?.id) { mutableStateOf(draft?.markdown.orEmpty()) }
  var selectedTab by remember { mutableStateOf(0) }
  var attachmentMessage by remember { mutableStateOf<String?>(null) }
  val scope = androidx.compose.runtime.rememberCoroutineScope()

  LaunchedEffect(title, markdown, draft?.metadata?.id) {
    if (draft != null) {
      delay(750L)
      PocketDraftCoordinator.updateDraft(title, markdown)
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.66f))) {
    TitleBarComponent(
      title = "Pocket Editor",
      onClose = {
        PocketDraftCoordinator.closeEditorSession()
        wm?.closeWindow(OverlayType.POCKET_EDITOR)
      }
    )
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        modifier = Modifier.weight(1f),
        singleLine = true,
        label = { Text("Title (optional)") },
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
      Button(onClick = {
        if (wm?.isVisible(OverlayType.POCKET_JOURNAL)?.value != true) {
          wm?.openWindow(OverlayType.POCKET_JOURNAL)
        }
      },
        colors = androidx.compose.material.ButtonDefaults.buttonColors(
          backgroundColor = Color.White.copy(alpha = 0.12f),
          contentColor = Color.White
        )
      ) {
        Text("Journal")
      }
    }
    AttachmentStrip(
      attachments = draft?.attachments.orEmpty(),
      onRemove = { attachmentId ->
        scope.launch {
            PocketDraftCoordinator.removeAttachment(attachmentId, markdown)
              ?.let {
                markdown = it.markdown
                attachmentMessage = null
              }
        }
      }
    )
    PocketTagChips(draft?.tags.orEmpty().map { it.tag })
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = {
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
                attachmentMessage = "Selected file is not a readable image."
              } else {
                val draftId = draft?.metadata?.id
                if (draftId == null) {
                  attachmentMessage = "No Pocket entry is currently open."
                } else {
                  val name = PocketDraftCoordinator.nextAttachmentName(draftId)
                  if (name == null) {
                    attachmentMessage = "This Pocket entry already has 10 attachments."
                  } else {
                    when (val result = PocketDraftCoordinator.addAttachment(
                      source = selected,
                      relativePath = name,
                      mimeType = "image/png",
                      markdown = appendImageReference(markdown, name)
                    )) {
                      is PocketAttachmentResult.Added -> {
                        markdown = result.entry.markdown
                        attachmentMessage = null
                      }
                      is PocketAttachmentResult.Rejected -> {
                        attachmentMessage = when (result.reason) {
                          PocketAttachmentRejection.ATTACHMENT_LIMIT_REACHED -> "This Pocket entry already has 10 attachments."
                          PocketAttachmentRejection.ENTRY_NOT_FOUND -> "The Pocket entry is no longer available."
                          PocketAttachmentRejection.INVALID_ATTACHMENT_PATH -> "The attachment path was invalid."
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        },
        colors = ButtonDefaults.buttonColors(backgroundColor = RFColors.AccentRed, contentColor = Color.White)
      ) { Text("Import Image") }
      attachmentMessage?.let { Text(it, color = RFColors.TextSecondary, fontSize = 11.sp) }
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)
        .background(Color(0xFF141414).copy(alpha = 0.78f), RoundedCornerShape(14.dp))
        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
        .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      listOf("Edit", "Preview").forEachIndexed { index, label ->
        Button(
          onClick = { selectedTab = index },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
          colors = androidx.compose.material.ButtonDefaults.buttonColors(
            backgroundColor = if (selectedTab == index) Color.White.copy(alpha = 0.14f) else Color.Transparent,
            contentColor = Color.White
          ),
          elevation = androidx.compose.material.ButtonDefaults.elevation(defaultElevation = 2.dp)
        ) { Text(label, fontSize = 12.sp) }
      }
    }
    if (selectedTab == 0) {
      OutlinedTextField(
        value = markdown,
        onValueChange = { markdown = it },
        modifier = Modifier.fillMaxSize().padding(10.dp),
        label = { Text("Markdown") },
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
    } else {
      Column(
        modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())
      ) {
        PocketMarkdownPreview(markdown, draft?.metadata?.markdownPath)
      }
    }
  }
}

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
  attachments: List<com.reoky.raidframer.core.database.PocketAttachmentEntity>,
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
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
          fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
          modifier = Modifier.background(Color.Black.copy(alpha = 0.45f)).padding(8.dp)
        )
      }
    }
  }
}

@Composable
private fun PocketInlineText(content: List<PocketMarkdownInline>, markdownPath: String?) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    content.forEach { inline ->
      when (inline) {
        is PocketMarkdownInline.Image -> {
          PocketMarkdownImage(inline, markdownPath)
        }
        else -> androidx.compose.foundation.text.BasicText(
          text = inline.toPlainText(),
          style = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp)
        )
      }
    }
  }
}

@Composable
private fun PocketMarkdownImage(image: PocketMarkdownInline.Image, markdownPath: String?) {
  val bitmap = remember(markdownPath, image.destination) {
    loadPocketImage(image.destination, markdownPath)?.let { bufferedImage ->
      val colorType = org.jetbrains.skia.ColorType.RGBA_8888
      val imageInfo = org.jetbrains.skia.ImageInfo(
        bufferedImage.width,
        bufferedImage.height,
        colorType,
        org.jetbrains.skia.ColorAlphaType.UNPREMUL
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
      org.jetbrains.skia.Image.makeRaster(imageInfo, bytes, bufferedImage.width * 4).toComposeImageBitmap()
    }
  }
  if (bitmap == null) {
    Text("[Image: ${image.alt.ifBlank { image.destination }}]", color = Color.White, fontSize = 12.sp)
  } else {
    Image(
      bitmap = bitmap,
      contentDescription = image.alt.ifBlank { "Pocket attachment" },
      modifier = Modifier.fillMaxWidth(),
      contentScale = ContentScale.Fit
    )
  }
}

private fun loadPocketImage(destination: String, markdownPath: String?): BufferedImage? {
  val path = runCatching {
    val reference = Path.of(destination)
    if (reference.isAbsolute) reference else markdownPath?.let { Path.of(it).parent.resolve(reference).normalize() }
  }.getOrNull() ?: return null
  return runCatching { ImageIO.read(path.toFile()) }.getOrNull()
}

private fun PocketMarkdownInline.toPlainText(): String = when (this) {
  is PocketMarkdownInline.Plain -> value
  is PocketMarkdownInline.Strong -> content.toPlainText()
  is PocketMarkdownInline.Emphasis -> content.toPlainText()
  is PocketMarkdownInline.Code -> value
  is PocketMarkdownInline.Link -> label.toPlainText()
  is PocketMarkdownInline.Image -> "[Image: ${alt.ifBlank { destination }}]"
  PocketMarkdownInline.Break -> "\n"
}

private fun List<PocketMarkdownInline>.toPlainText(): String = joinToString("") { inline ->
  when (inline) {
    is PocketMarkdownInline.Plain -> inline.value
    is PocketMarkdownInline.Strong -> inline.content.toPlainText()
    is PocketMarkdownInline.Emphasis -> inline.content.toPlainText()
    is PocketMarkdownInline.Code -> inline.value
    is PocketMarkdownInline.Link -> inline.label.toPlainText()
    is PocketMarkdownInline.Image -> "[Image: ${inline.alt.ifBlank { inline.destination }}]"
    PocketMarkdownInline.Break -> "\n"
  }
}
