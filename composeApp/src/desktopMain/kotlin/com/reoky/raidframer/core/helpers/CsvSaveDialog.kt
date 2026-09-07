package com.reoky.raidframer.core.helpers

import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

fun showCsvSaveChooser(
  suggestedName: String,
  dialogTitle: String,
  onFileSelected: (File) -> Unit,
  onCancel: () -> Unit = {}
) {
  try {
    SwingUtilities.invokeLater {
      try {
        val chooser = JFileChooser()
        chooser.dialogTitle = dialogTitle
        chooser.selectedFile = File(suggestedName)
        chooser.setFileFilter(FileNameExtensionFilter("CSV (*.csv)", "csv"))
        val parent = try {
          java.awt.Window.getWindows().firstOrNull { it.isVisible && it is java.awt.Frame } as? java.awt.Component
        } catch (e: Exception) { null }
        val result = chooser.showSaveDialog(parent)
        if (result == JFileChooser.APPROVE_OPTION) {
          var file = chooser.selectedFile
          if (!file.name.endsWith(".csv", ignoreCase = true)) {
            file = File(file.parentFile, "${file.nameWithoutExtension}.csv")
          }
          onFileSelected(file)
        } else {
          onCancel()
        }
      } catch (e: Exception) { onCancel() }
    }
  } catch (e: Exception) { onCancel() }
}
