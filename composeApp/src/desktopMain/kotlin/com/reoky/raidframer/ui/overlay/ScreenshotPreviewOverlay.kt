package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.capture.PocketWindowCaptureCoordinator
import com.reoky.raidframer.ui.capture.ScreenshotPreviewCoordinator
import com.reoky.raidframer.ui.component.TitleBarComponent
import kotlinx.coroutines.launch

@Composable
fun ScreenshotPreviewOverlay(wm: WindowManager? = null) {
  val scope = rememberCoroutineScope()
  val pending by ScreenshotPreviewCoordinator.pending.collectAsState()
  var savedToPocket by remember { mutableStateOf(false) }
  var feedback by remember { mutableStateOf<String?>(null) }

  // Close the window automatically once there is nothing left to preview.
  LaunchedEffect(pending) {
    if (pending == null) wm?.closeWindow(OverlayType.SCREENSHOT_PREVIEW)
  }

  val screenshot = pending
  if (screenshot == null) return

  Box(Modifier.fillMaxSize().background(Color(0xFF171717))) {
    Column(Modifier.fillMaxSize()) {
      TitleBarComponent(
        title = "Screenshot Preview",
        onClose = {
          ScreenshotPreviewCoordinator.clear()
          wm?.closeWindow(OverlayType.SCREENSHOT_PREVIEW)
        }
      )

      Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Image(
          bitmap = remember(screenshot.snippetFile) { screenshot.image.toComposeImageBitmap() },
          contentDescription = "Screenshot preview",
          modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).clip(RoundedCornerShape(8.dp)),
          contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(8.dp))
        Text(
          screenshot.snippetFile.fileName.toString(),
          color = RFColors.TextTertiary,
          fontSize = 11.sp,
          textAlign = TextAlign.Center
        )

        feedback?.let {
          Spacer(Modifier.height(6.dp))
          Text(it, color = RFColors.UpdateGreen, fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ActionButton("Show in Explorer") {
            ScreenshotPreviewCoordinator.showInExplorer()
          }
          ActionButton("Save to Pocket", accent = !savedToPocket, enabled = !savedToPocket) {
            scope.launch {
              val ok = PocketWindowCaptureCoordinator.saveToPocket(
                screenshot.image,
                "Game Screenshot",
                wm ?: return@launch
              )
              if (ok) {
                savedToPocket = true
                feedback = "Saved to Pocket journal"
              } else {
                feedback = "Could not save (attachment limit reached?)"
              }
            }
          }
          ActionButton("Copy to Clipboard") {
            ScreenshotPreviewCoordinator.copyToClipboard()
            feedback = "Copied to clipboard"
          }
          TextButton(
            onClick = {
              ScreenshotPreviewCoordinator.discard()
              wm?.closeWindow(OverlayType.SCREENSHOT_PREVIEW)
            },
            modifier = Modifier.fillMaxWidth().height(40.dp)
          ) {
            Text("Don't Save", color = RFColors.TextSecondary, fontSize = 13.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun ActionButton(
  label: String,
  accent: Boolean = false,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth().height(40.dp),
    colors = ButtonDefaults.buttonColors(
      backgroundColor = if (accent) RFColors.AccentRed else Color.White.copy(alpha = 0.10f),
      contentColor = Color.White
    ),
    shape = RoundedCornerShape(8.dp)
  ) {
    Text(label, fontSize = 13.sp)
  }
}