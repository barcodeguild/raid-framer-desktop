package com.reoky.raidframer.core.helpers

import java.awt.Frame
import java.io.File
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import javax.swing.filechooser.FileNameExtensionFilter

/*
 * Shows [chooser] inside an always-on-top dialog and blocks until the user is done.
 * System dialogs opened without a parent (or parented to our undecorated overlay
 * windows) appear behind the game — Raid Framer is a game overlay — where they cannot
 * be focused or even closed. Hosting the chooser in a topmost dialog keeps it reachable.
 * Returns one of JFileChooser's APPROVE / CANCEL / ERROR option results.
 */
internal fun showFileChooserTopmost(chooser: JFileChooser): Int {
  return if (SwingUtilities.isEventDispatchThread()) {
    showFileChooserTopmostOnEdt(chooser)
  } else {
    var decision = JFileChooser.ERROR_OPTION
    try {
      SwingUtilities.invokeAndWait { decision = showFileChooserTopmostOnEdt(chooser) }
    } catch (_: Exception) {
      // Interrupted or dispatch failed; treat as if the user cancelled.
    }
    decision
  }
}

private fun showFileChooserTopmostOnEdt(chooser: JFileChooser): Int {
  var decision = JFileChooser.ERROR_OPTION
  lateinit var dialog: JDialog
  chooser.addActionListener { event ->
    when (event.actionCommand) {
      JFileChooser.APPROVE_SELECTION -> decision = JFileChooser.APPROVE_OPTION
      JFileChooser.CANCEL_SELECTION -> decision = JFileChooser.CANCEL_OPTION
    }
    dialog.isVisible = false
    dialog.dispose()
  }
  dialog = JDialog(null as Frame?, chooser.dialogTitle, true)
  dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
  dialog.isAlwaysOnTop = true
  dialog.contentPane.add(chooser)
  dialog.pack()
  dialog.setLocationRelativeTo(null)
  dialog.isVisible = true // blocks until the user is done
  return decision
}

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
        val result = showFileChooserTopmost(chooser)
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

fun showFolderChooser(
  dialogTitle: String,
  onFolderSelected: (File) -> Unit,
  onCancel: () -> Unit = {}
) {
  try {
    SwingUtilities.invokeLater {
      try {
        val chooser = JFileChooser()
        chooser.dialogTitle = dialogTitle
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.isAcceptAllFileFilterUsed = false
        val result = showFileChooserTopmost(chooser)
        if (result == JFileChooser.APPROVE_OPTION) {
          onFolderSelected(chooser.selectedFile)
        } else {
          onCancel()
        }
      } catch (e: Exception) { onCancel() }
    }
  } catch (e: Exception) { onCancel() }
}
