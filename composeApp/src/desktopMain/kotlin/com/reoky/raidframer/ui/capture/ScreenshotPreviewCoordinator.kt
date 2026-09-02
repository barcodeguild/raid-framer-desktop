package com.reoky.raidframer.ui.capture

import com.reoky.raidframer.core.helpers.copyImageToClipboard
import com.reoky.raidframer.core.helpers.showInExplorer
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the single screenshot currently awaiting the user's decision in the dedicated
 * screenshot preview window. Only one preview is shown at a time.
 */
object ScreenshotPreviewCoordinator {

  data class PendingScreenshot(
    val image: BufferedImage,
    val snippetFile: Path,
    val snippetsDirectory: Path,
  )

  private val _pending = MutableStateFlow<PendingScreenshot?>(null)
  val pending: StateFlow<PendingScreenshot?> = _pending.asStateFlow()

  /** Registers a freshly-sniped [snippet] as the screenshot awaiting a decision. */
  fun show(snippet: PocketWindowCaptureCoordinator.SnippetResult) {
    val image = runCatching { ImageIO.read(snippet.snippetFile.toFile()) }.getOrNull() ?: return
    _pending.value = PendingScreenshot(image, snippet.snippetFile, snippet.snippetsDirectory)
  }

  /** Clears the pending screenshot without touching the saved snippet file. */
  fun clear() {
    _pending.value = null
  }

  fun showInExplorer() {
    _pending.value?.let { showInExplorer(it.snippetFile) }
  }

  fun copyToClipboard() {
    _pending.value?.let { copyImageToClipboard(it.image) }
  }

  /** Deletes the snippet file and clears the preview. */
  fun discard() {
    _pending.value?.let { runCatching { Files.deleteIfExists(it.snippetFile) } }
    _pending.value = null
  }
}