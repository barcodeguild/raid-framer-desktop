package com.reoky.raidframer.ui.capture

import androidx.compose.ui.awt.ComposeWindow
import com.reoky.raidframer.core.helpers.getDocumentsDirectory
import com.reoky.raidframer.core.pocket.PocketAttachmentResult
import com.reoky.raidframer.core.pocket.PocketDraftCoordinator
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

object PocketWindowCaptureCoordinator {

  /** Result of storing a snipped capture: where the PNG copy lives and the images folder. */
  data class SnippetResult(
    val snippetFile: Path,
    val snippetsDirectory: Path,
  )

  /**
   * Persists a copy of a snipped [image] to `RFExports/<year>/<month>/snippets/`
   * with a human-readable timestamped filename. Returns paths for both, or null on failure.
   */
  fun saveSnippet(image: BufferedImage): SnippetResult? {
    val documents = getDocumentsDirectory() ?: return null
    val now = java.time.LocalDateTime.now()
    val directory = Path.of(documents, "RFExports", now.year.toString(), "%02d".format(now.monthValue), "snippets")
    return runCatching {
      Files.createDirectories(directory)
      val fileName = "game-snippet_${DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(now)}.png"
      val output = directory.resolve(fileName)
      ImageIO.write(image, "png", output.toFile())
      SnippetResult(output, directory)
    }.getOrNull()
  }

  /**
   * Saves [image] into the user's Pocket journal: appends to the most recent draft when it is
   * still fresh (within the last 30 minutes), otherwise starts a new journal entry. Each journal
   * entry can hold up to [MAX_POCKET_ATTACHMENTS] attachments. Returns true on success.
   */
  suspend fun saveToPocket(
    image: BufferedImage,
    title: String,
    windowManager: WindowManager,
  ): Boolean {
    val entryId = PocketDraftCoordinator.resolveRecentDraftId()
      ?: PocketDraftCoordinator.createDraft(title = title).metadata.id
    val name = PocketDraftCoordinator.nextAttachmentName(entryId) ?: return false
    val temporary = Files.createTempFile("raid-framer-window-", ".png")
    return try {
      ImageIO.write(image, "png", temporary.toFile())
      val markdown = PocketDraftCoordinator.readEntryMarkdown(entryId).orEmpty()
      val separator = if (markdown.isBlank() || markdown.endsWith("\n")) "" else "\n"
      val result = PocketDraftCoordinator.addAttachmentToEntry(
        entryId,
        temporary,
        name,
        "image/png",
        "$markdown${separator}\n![${title}]($name)\n"
      )
      val added = result is PocketAttachmentResult.Added
      if (added) windowManager.openWindow(OverlayType.POCKET_EDITOR)
      added
    } finally {
      Files.deleteIfExists(temporary)
    }
  }

  fun exportPng(image: BufferedImage, title: String): Path? {
    val documents = getDocumentsDirectory() ?: return null
    val date = LocalDate.now()
    val directory = Path.of(documents, "RFExports", date.year.toString(), "%02d".format(date.monthValue))
    Files.createDirectories(directory)
    val fileName = "${safeFileName(title)}_${DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now())}.png"
    val output = directory.resolve(fileName)
    ImageIO.write(image, "png", output.toFile())
    return output
  }

  private fun safeFileName(value: String): String = value
    .replace(Regex("[^a-zA-Z0-9._ -]"), "")
    .trim()
    .replace(Regex("\\s+"), " ")
    .ifBlank { "Raid-Framer-Window" }
}
