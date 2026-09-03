package com.reoky.raidframer.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.pocket.PocketEntry
import com.reoky.raidframer.core.pocket.PocketMarkdownBlock
import com.reoky.raidframer.core.pocket.PocketMarkdownInline
import com.reoky.raidframer.core.pocket.parsePocketMarkdown
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.pocket_attachment_content_desc
import raid_framer_desktop.composeapp.generated.resources.pocket_thumbnail_untitled
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

private val thumbnailDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")

/** Width cap for preview images relative to the surrounding thumbnail area. */
private const val THUMBNAIL_IMAGE_MAX_WIDTH = 0.85f

/**
 * A compact, reusable preview of a [PocketEntry] (title, date, tags and a snippet of the
 * rendered markdown) that can be surfaced anywhere in the app without opening the editor.
 */
@Composable
fun PocketEntryThumbnail(entry: PocketEntry) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = entry.metadata.title.ifBlank { stringResource(Res.string.pocket_thumbnail_untitled) },
      color = RFColors.TextPrimary,
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = thumbnailDateFormatter.format(Instant.ofEpochMilli(entry.metadata.createdAt).atZone(ZoneId.systemDefault())),
      color = RFColors.TextSecondary,
      fontSize = 10.sp
    )
    if (entry.tags.isNotEmpty()) {
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        entry.tags.forEach { tag ->
          Text(
            text = "#${tag.tag}",
            color = Color.White,
            fontSize = 9.sp,
            modifier = Modifier
              .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
    ThumbnailMarkdown(entry.markdown, entry.metadata.markdownPath)
  }
}

@Composable
private fun ThumbnailMarkdown(markdown: String, markdownPath: String?) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    parsePocketMarkdown(markdown).forEach { block ->
      when (block) {
        is PocketMarkdownBlock.Paragraph -> ThumbnailInlineContent(block.content, markdownPath)
        is PocketMarkdownBlock.Heading -> Text(
          text = block.content.toPlainText(),
          color = Color.White,
          fontWeight = FontWeight.SemiBold,
          fontSize = (16 - block.level * 2).coerceAtLeast(13).sp
        )

        is PocketMarkdownBlock.BulletList -> block.items.forEach { item ->
          Row { Text("• ", color = Color.White, fontSize = 12.sp); ThumbnailInlineContent(item, markdownPath) }
        }

        is PocketMarkdownBlock.OrderedList -> block.items.forEachIndexed { index, item ->
          Row { Text("${index + 1}. ", color = Color.White, fontSize = 12.sp); ThumbnailInlineContent(item, markdownPath) }
        }

        is PocketMarkdownBlock.Quote -> Text(
          text = "> ${block.content.toPlainText()}",
          color = RFColors.TextSecondary,
          fontSize = 12.sp
        )

        is PocketMarkdownBlock.CodeBlock -> Text(
          text = block.code,
          color = Color.White,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          maxLines = 6,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.background(Color.Black.copy(alpha = 0.45f)).padding(6.dp)
        )
      }
    }
  }
}

@Composable
private fun ThumbnailInlineContent(content: List<PocketMarkdownInline>, markdownPath: String?) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    content.forEach { inline ->
      when (inline) {
        is PocketMarkdownInline.Image -> ThumbnailImage(inline, markdownPath)
        PocketMarkdownInline.Break -> Unit
        else -> Text(
          text = inline.toPlainText(),
          color = RFColors.TextSecondary,
          fontSize = 12.sp
        )
      }
    }
  }
}

@Composable
private fun ThumbnailImage(image: PocketMarkdownInline.Image, markdownPath: String?) {
  val bitmap = remember(markdownPath, image.destination) {
    loadPocketImage(image.destination, markdownPath)?.let { buffered ->
      val colorType = org.jetbrains.skia.ColorType.RGBA_8888
      val imageInfo = org.jetbrains.skia.ImageInfo(
        buffered.width,
        buffered.height,
        colorType,
        org.jetbrains.skia.ColorAlphaType.UNPREMUL
      )
      val pixels = IntArray(buffered.width * buffered.height)
      buffered.getRGB(0, 0, buffered.width, buffered.height, pixels, 0, buffered.width)
      val bytes = ByteArray(pixels.size * 4)
      pixels.forEachIndexed { index, pixel ->
        bytes[index * 4] = ((pixel shr 16) and 0xff).toByte()
        bytes[index * 4 + 1] = ((pixel shr 8) and 0xff).toByte()
        bytes[index * 4 + 2] = (pixel and 0xff).toByte()
        bytes[index * 4 + 3] = ((pixel ushr 24) and 0xff).toByte()
      }
      org.jetbrains.skia.Image.makeRaster(imageInfo, bytes, buffered.width * 4).toComposeImageBitmap()
    }
  }
  if (bitmap == null) {
    Text("[Image: ${image.alt.ifBlank { image.destination }}]", color = Color.White, fontSize = 12.sp)
  } else {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      Image(
        bitmap = bitmap,
        contentDescription = image.alt.ifBlank { stringResource(Res.string.pocket_attachment_content_desc) },
        modifier = Modifier
          .fillMaxWidth(THUMBNAIL_IMAGE_MAX_WIDTH)
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

private fun PocketMarkdownInline.toPlainText(): String = when (this) {
  is PocketMarkdownInline.Plain -> value
  is PocketMarkdownInline.Strong -> content.toPlainText()
  is PocketMarkdownInline.Emphasis -> content.toPlainText()
  is PocketMarkdownInline.Strikethrough -> content.toPlainText()
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
    is PocketMarkdownInline.Strikethrough -> inline.content.toPlainText()
    is PocketMarkdownInline.Code -> inline.value
    is PocketMarkdownInline.Link -> inline.label.toPlainText()
    is PocketMarkdownInline.Image -> "[Image: ${inline.alt.ifBlank { inline.destination }}]"
    PocketMarkdownInline.Break -> "\n"
  }
}