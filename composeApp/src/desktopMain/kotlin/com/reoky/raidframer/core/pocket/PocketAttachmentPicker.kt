package com.reoky.raidframer.core.pocket

import com.reoky.raidframer.core.config.RFConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path
import java.awt.Window
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/** Opens the native image picker without coupling Swing behavior to Pocket composables. */
object PocketAttachmentPicker {
  suspend fun chooseImage(temporarilyHide: List<Window> = emptyList()): Path? = withContext(Dispatchers.IO) {
    val visibleWindows = temporarilyHide.filter { it.isVisible }
    visibleWindows.forEach { it.isVisible = false }
    try {
    val chooser = JFileChooser()
    chooser.dialogTitle = "Attach Image"
    chooser.fileFilter = FileNameExtensionFilter(
      "Images (*.png, *.jpg, *.jpeg, *.gif, *.bmp)",
      "png", "jpg", "jpeg", "gif", "bmp"
    )
    val configuredGameDirectory = RFConfig.state.value.defaultArcheRageDirectory
    val screenshotsDirectory = if (configuredGameDirectory.isBlank()) {
      null
    } else {
      File(configuredGameDirectory, "ScreenShots")
    }
    if (screenshotsDirectory?.isDirectory == true) chooser.currentDirectory = screenshotsDirectory
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return@withContext null
    chooser.selectedFile?.toPath()
    } finally {
      visibleWindows.forEach { it.isVisible = true }
    }
  }
}
