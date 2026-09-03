package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.capture.PocketWindowCaptureCoordinator
import com.reoky.raidframer.ui.capture.ScreenshotPreviewCoordinator
import com.reoky.raidframer.ui.capture.ScreenshotCopyHotkey
import com.reoky.raidframer.ui.component.TitleBarComponent
import kotlinx.coroutines.launch

@Composable
fun ScreenshotPreviewOverlay(wm: WindowManager? = null) {
  val scope = rememberCoroutineScope()
  val pending by ScreenshotPreviewCoordinator.pending.collectAsState()
  var savedToPocket by remember { mutableStateOf(false) }
  var feedback by remember { mutableStateOf<String?>(null) }
  var copyPulse by remember { mutableStateOf(false) }
  val imageFlash by androidx.compose.animation.core.animateFloatAsState(
    targetValue = if (copyPulse) 0.325f else 0f,
    animationSpec = androidx.compose.animation.core.tween(220)
  )

  // Close the window automatically once there is nothing left to preview.
  LaunchedEffect(pending) {
    if (pending == null) {
      ScreenshotCopyHotkey.stop()
      wm?.closeWindow(OverlayType.SCREENSHOT_PREVIEW)
    } else {
      ScreenshotCopyHotkey.start {
        ScreenshotPreviewCoordinator.copyToClipboard()
        feedback = "Copied to clipboard"
        copyPulse = true
      }
    }
  }

  androidx.compose.runtime.DisposableEffect(Unit) {
    onDispose { ScreenshotCopyHotkey.stop() }
  }

  val screenshot = pending
  if (screenshot == null) return

  Box(
    Modifier
      .fillMaxSize()
      .background(Color(0xFF171717))
      .onPreviewKeyEvent { event: KeyEvent ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.C && (event.isCtrlPressed || event.isMetaPressed)) {
          ScreenshotPreviewCoordinator.copyToClipboard()
          feedback = "Copied to clipboard"
          copyPulse = true
          true
        } else {
          false
        }
      }
  ) {
    Column(Modifier.fillMaxSize()) {
      TitleBarComponent(
        title = "Screenshot Preview",
        onClose = {
          ScreenshotPreviewCoordinator.clear()
          wm?.closeWindow(OverlayType.SCREENSHOT_PREVIEW)
        },
        rightActions = {
          val trashInteractionSource = remember { MutableInteractionSource() }
          val isTrashHovered by trashInteractionSource.collectIsHoveredAsState()
          IconButton(
            onClick = {
              ScreenshotPreviewCoordinator.discard()
              wm?.closeWindow(OverlayType.SCREENSHOT_PREVIEW)
            },
            modifier = Modifier.size(28.dp)
          ) {
            Text(
              "\uf2ed",
              color = if (isTrashHovered) RFColors.AccentRed else Color.White,
              fontFamily = FontsHelper.faSolid(),
              fontSize = 14.sp,
              modifier = Modifier.hoverable(trashInteractionSource)
            )
          }
        }
      )

      Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        color = Color.Black.copy(alpha = 0.30f),
        shape = RoundedCornerShape(10.dp),
        elevation = 3.dp
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          PreviewAction("\uf2ed", "Discard") { ScreenshotPreviewCoordinator.discard(); wm?.closeWindow(OverlayType.SCREENSHOT_PREVIEW) }
          PreviewAction("\uf02d", "Save to Pocket", enabled = !savedToPocket, accent = !savedToPocket) {
            scope.launch {
              val ok = PocketWindowCaptureCoordinator.saveToPocket(screenshot.image, "Game Screenshot", wm ?: return@launch)
              if (ok) { savedToPocket = true; feedback = "Saved to Pocket journal" }
              else feedback = "Could not save (attachment limit reached?)"
            }
          }
          PreviewAction("\uf0c5", "Copy to Clipboard", highlighted = copyPulse) {
            ScreenshotPreviewCoordinator.copyToClipboard()
            feedback = "Copied to clipboard"
            copyPulse = true
          }
          PreviewAction("\uf019", "Save to Exports") { ScreenshotPreviewCoordinator.showExportsInExplorer() }
        }
      }

      LaunchedEffect(copyPulse) {
        if (copyPulse) {
          kotlinx.coroutines.delay(220)
          copyPulse = false
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth().weight(1f).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp))) {
          Image(
            bitmap = remember(screenshot.snippetFile) { screenshot.image.toComposeImageBitmap() },
            contentDescription = "Screenshot preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
          )
          Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = imageFlash)))
        }
        Spacer(Modifier.height(4.dp))
        Text(screenshot.snippetFile.fileName.toString(), color = RFColors.TextTertiary, fontSize = 11.sp, textAlign = TextAlign.Center)
        feedback?.let { Spacer(Modifier.height(6.dp)); Text(it, color = RFColors.UpdateGreen, fontSize = 12.sp) }
        Spacer(Modifier.height(4.dp))
        Text("Ctrl+C to copy", color = RFColors.TextTertiary, fontSize = 10.sp)
      }
    }
  }
}

@Composable
private fun RowScope.PreviewAction(
  icon: String,
  label: String,
  accent: Boolean = false,
  enabled: Boolean = true,
  highlighted: Boolean = false,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.weight(1f).hoverable(interactionSource)
  ) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
      Text(
        icon,
        color = if (enabled && (highlighted || accent || isHovered)) RFColors.AccentRed else Color.White,
        fontFamily = FontsHelper.faSolid(),
        fontSize = 15.sp,
        modifier = Modifier
      )
    }
    Text(label, color = if (enabled) Color.White else RFColors.TextTertiary, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center)
  }
}
