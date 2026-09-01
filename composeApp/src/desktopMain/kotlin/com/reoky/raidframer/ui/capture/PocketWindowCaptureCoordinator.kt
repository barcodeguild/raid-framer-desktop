package com.reoky.raidframer.ui.capture

import androidx.compose.ui.awt.ComposeWindow
import com.reoky.raidframer.core.helpers.getDocumentsDirectory
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
  suspend fun saveToPocket(
    image: BufferedImage,
    title: String,
    windowManager: WindowManager,
  ) {
    val draft = PocketDraftCoordinator.activeDraft.value
      ?: PocketDraftCoordinator.createDraft(title = title)
    val name = PocketDraftCoordinator.nextAttachmentName(draft.metadata.id) ?: return
    val temporary = Files.createTempFile("raid-framer-window-", ".png")
    try {
      ImageIO.write(image, "png", temporary.toFile())
      val markdown = PocketDraftCoordinator.activeDraft.value?.markdown.orEmpty()
      val separator = if (markdown.isBlank() || markdown.endsWith("\n")) "" else "\n"
      PocketDraftCoordinator.addAttachment(
        temporary,
        name,
        "image/png",
        "$markdown${separator}\n![${title}]($name)\n"
      )
      windowManager.openWindow(OverlayType.POCKET_EDITOR)
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
