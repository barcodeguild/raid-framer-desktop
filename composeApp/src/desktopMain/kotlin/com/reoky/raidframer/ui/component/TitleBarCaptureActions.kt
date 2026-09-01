package com.reoky.raidframer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.awt.ComposeWindow
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.capture.ComposeWindowCaptureService
import com.reoky.raidframer.ui.capture.PocketWindowCaptureCoordinator
import kotlinx.coroutines.launch

@Composable
fun titleBarCaptureActions(
  window: ComposeWindow,
  windowManager: WindowManager,
  title: String,
): @Composable androidx.compose.foundation.layout.RowScope.() -> Unit {
  val scope = rememberCoroutineScope()
  return {
    PocketWindowCaptureMenu(
      onSaveToPocket = {
        val image = ComposeWindowCaptureService.capture(window) ?: return@PocketWindowCaptureMenu
        scope.launch { PocketWindowCaptureCoordinator.saveToPocket(image, title, windowManager) }
      },
      onExportPng = {
        val image = ComposeWindowCaptureService.capture(window) ?: return@PocketWindowCaptureMenu
        scope.launch { PocketWindowCaptureCoordinator.exportPng(image, title) }
      }
    )
  }
}
