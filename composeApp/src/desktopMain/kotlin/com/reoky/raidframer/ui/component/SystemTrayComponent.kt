package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.helpers.FaIcon
import com.reoky.raidframer.core.interactor.CombatLogInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.messageBox
import com.reoky.raidframer.quitAfterSessionStop
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.app_tray_reset_positions
import raid_framer_desktop.composeapp.generated.resources.app_tray_title
import raid_framer_desktop.composeapp.generated.resources.general_about
import raid_framer_desktop.composeapp.generated.resources.general_exit
import raid_framer_desktop.composeapp.generated.resources.general_help_window_postions_reset
import raid_framer_desktop.composeapp.generated.resources.general_settings
import raid_framer_desktop.composeapp.generated.resources.tray_abort_session
import raid_framer_desktop.composeapp.generated.resources.tray_battle_graph
import raid_framer_desktop.composeapp.generated.resources.tray_battle_summary
import raid_framer_desktop.composeapp.generated.resources.tray_close
import raid_framer_desktop.composeapp.generated.resources.tray_dragon_breaths
import raid_framer_desktop.composeapp.generated.resources.tray_pocket_journal
import raid_framer_desktop.composeapp.generated.resources.tray_lua_options
import raid_framer_desktop.composeapp.generated.resources.tray_help
import raid_framer_desktop.composeapp.generated.resources.tray_take_screenshot
import raid_framer_desktop.composeapp.generated.resources.tray_new_session
import raid_framer_desktop.composeapp.generated.resources.tray_raid_management
import raid_framer_desktop.composeapp.generated.resources.tray_save_session
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowFocusListener

@Composable
fun ApplicationScope.SystemTrayComponent(
  wm: WindowManager,
  onExit: () -> Unit
) {
  var menuVisible by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val isRecording by CombatLogInteractor.isRecording.collectAsState()
  val config by RFConfig.state.collectAsState()

  val titleStr = stringResource(Res.string.app_tray_title)
  val settingsStr = stringResource(Res.string.general_settings)
  val aboutStr = stringResource(Res.string.general_about)
  val resetStr = stringResource(Res.string.app_tray_reset_positions)
  val exitStr = stringResource(Res.string.general_exit)
  val dragonBreathsStr = stringResource(Res.string.tray_dragon_breaths)
  val pocketJournalStr = stringResource(Res.string.tray_pocket_journal)
  val raidManagementStr = stringResource(Res.string.tray_raid_management)
  val battleSummaryStr = stringResource(Res.string.tray_battle_summary)
  val battleGraphStr = stringResource(Res.string.tray_battle_graph)
  val newSessionStr = stringResource(Res.string.tray_new_session)
  val saveSessionStr = stringResource(Res.string.tray_save_session)
  val abortSessionStr = stringResource(Res.string.tray_abort_session)
  val luaOptionsStr = stringResource(Res.string.tray_lua_options)
  val helpStr = stringResource(Res.string.tray_help)
  val takeScreenshotStr = stringResource(Res.string.tray_take_screenshot)
  val closeStr = stringResource(Res.string.tray_close)

  DisposableEffect(Unit) {
    val trayIcon = createTrayIcon { menuVisible = !menuVisible }
    val systemTray = SystemTray.getSystemTray()
    systemTray.add(trayIcon)
    onDispose {
      systemTray.remove(trayIcon)
    }
  }

  if (menuVisible) {
    val pointer = remember { MouseInfo.getPointerInfo()?.location }
    val screen = remember {
      val pt = pointer
      if (pt != null) {
        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
          .map { it.defaultConfiguration.bounds }
          .firstOrNull { it.contains(pt) }
      } else null
    } ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds

    val mouseX = pointer?.x ?: (screen.x + screen.width)
    val mouseY = pointer?.y ?: (screen.y + screen.height)

    // Dynamic height based on items shown
    var itemCount = 7 // Pocket Journal, Settings, Lua Options, About, Reset, Exit, Close
    if (isRecording) itemCount += 2 else itemCount += 1 // Save+Abort or New
    itemCount += 3 // Dragon Breaths, Raid Mgmt, Battle Summary
    if (config.performanceBattleGraphEnabled) itemCount += 1
    val dividerCount = 3
    val itemHeight = 26 // Fixed height per item in dp — consistent across languages
    val dividerHeight = 2
    val menuHeight = itemCount * itemHeight + dividerCount * dividerHeight + 10
    val menuWidth = 160

    Window(
      onCloseRequest = { menuVisible = false },
      state = WindowState(
        width = menuWidth.dp,
        height = menuHeight.dp,
        position = WindowPosition.Absolute(
          x = (mouseX - menuWidth).coerceAtLeast(screen.x).dp,
          y = (mouseY - menuHeight).coerceAtLeast(screen.y).dp
        )
      ),
      title = titleStr,
      undecorated = true,
      resizable = false,
      alwaysOnTop = true,
      focusable = true
    ) {
      dismissWhenFocusIsLost(window) { menuVisible = false }

      Column(
        modifier = Modifier
          .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
          .padding(vertical = 4.dp)
      ) {
        // --- Session Actions ---
        if (isRecording) {
          TrayMenuItem(iconCode = "\uf0c7", text = saveSessionStr) {
            menuVisible = false
            scope.launch {
              PlayerCacheInteractor.stopSession()
            }
          }
          TrayMenuItem(iconCode = "\uf057", text = abortSessionStr) {
            menuVisible = false
            scope.launch {
              PlayerCacheInteractor.abortSession()
            }
          }
        } else {
          TrayMenuItem(iconCode = "\uf067", text = newSessionStr) {
            menuVisible = false
            wm.openWindow(OverlayType.NEW_SESSION)
          }
        }

        TrayMenuDivider()

        // --- Overlays ---
        TrayMenuItem(iconCode = "\uf6d5", text = dragonBreathsStr) {
          menuVisible = false
          wm.openWindow(OverlayType.POKEMON)
        }
        TrayMenuItem(iconCode = "\uf0c0", text = raidManagementStr) {
          menuVisible = false
          wm.openWindow(OverlayType.RAID)
        }
        TrayMenuItem(iconCode = "\uf080", text = battleSummaryStr) {
          menuVisible = false
          wm.openWindow(OverlayType.SUMMARY)
        }
        if (config.performanceBattleGraphEnabled) {
          TrayMenuItem(iconCode = "\uf1e0", text = battleGraphStr) {
            menuVisible = false
            wm.openWindow(OverlayType.BATTLE_GRAPH)
          }
        }

        TrayMenuItem(iconCode = "\uf02d", text = pocketJournalStr) {
          menuVisible = false
          wm.openWindow(OverlayType.POCKET_JOURNAL)
        }
        TrayMenuItem(iconCode = "\uf030", text = takeScreenshotStr) {
          menuVisible = false
          wm.openWindow(OverlayType.SCREENSHOT_PREVIEW)
        }

        TrayMenuDivider()

        // --- App ---
        TrayMenuItem(iconCode = "\uf013", text = settingsStr) {
          menuVisible = false
          wm.openWindow(OverlayType.SETTINGS)
        }
        TrayMenuItem(iconCode = "\uf12c", text = luaOptionsStr) {
          menuVisible = false
          wm.openWindow(OverlayType.COMPANION)
        }
        TrayMenuItem(iconCode = "\uf059", text = aboutStr) {
          menuVisible = false
          wm.openWindow(OverlayType.ABOUT)
        }
        TrayMenuItem(iconCode = "\uf128", text = helpStr) {
          menuVisible = false
          wm.openWindow(OverlayType.HELP)
        }

        TrayMenuDivider()

        // --- Actions ---
        TrayMenuItem(iconCode = "\uf0e2", text = resetStr) {
          menuVisible = false
          wm.resetAllWindowPositions()
        }
        TrayMenuItem(iconCode = "\uf011", text = exitStr) {
          menuVisible = false
          scope.launch { quitAfterSessionStop() }
        }
        TrayMenuItem(iconCode = "\uf00d", text = closeStr) {
          menuVisible = false
        }
      }
    }
  }
}

@Composable
private fun TrayMenuDivider() {
  Spacer(
    modifier = Modifier
      .fillMaxWidth()
      .height(2.dp)
      .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
  )
}

private fun createTrayIcon(onClick: () -> Unit): TrayIcon {
  val classLoader = AppGlobals::class.java.classLoader

  val resourcePaths = listOf(
    "composeResources/raid_framer_desktop.composeapp.generated.resources/drawable/raidframer.ico",
    "drawable/raidframer.ico",
    "raidframer.ico"
  )

  val rawImage = resourcePaths.firstNotNullOfOrNull { path ->
    classLoader.getResourceAsStream(path)?.use { stream ->
      val bytes = stream.readBytes()
      val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)
      val pngBytes = skiaImage.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG, 100) ?: return@use null
      javax.imageio.ImageIO.read(pngBytes.bytes.inputStream())
    }
  } ?: java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)

  // Scale up small icons so they display at a reasonable size in the Windows tray
  val targetSize = 32
  val image = if (rawImage.width < targetSize || rawImage.height < targetSize) {
    val scaled = java.awt.image.BufferedImage(targetSize, targetSize, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    val g = scaled.createGraphics()
    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.drawImage(rawImage, 0, 0, targetSize, targetSize, null)
    g.dispose()
    scaled
  } else rawImage

  val trayIcon = TrayIcon(image, "Raid Framer")
  trayIcon.isImageAutoSize = true

  trayIcon.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      if (e.isPopupTrigger || e.button == MouseEvent.BUTTON3 || e.button == MouseEvent.BUTTON1) {
        onClick()
      }
    }
  })

  return trayIcon
}

@Composable
private fun dismissWhenFocusIsLost(window: ComposeWindow, onDismiss: () -> Unit) {
  DisposableEffect(window) {
    var focused = true
    val listener = object : WindowFocusListener {
      override fun windowGainedFocus(e: java.awt.event.WindowEvent?) { focused = true }
      override fun windowLostFocus(e: java.awt.event.WindowEvent?) {
        focused = false
        Thread.sleep(100)
        if (!focused) onDismiss()
      }
    }
    window.addWindowFocusListener(listener)
    onDispose { window.removeWindowFocusListener(listener) }
  }
}

@Composable
private fun TrayMenuItem(iconCode: String, text: String, onClick: () -> Unit) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Start,
    modifier = Modifier
      .fillMaxWidth()
      .height(26.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp)
  ) {
    FaIcon(codepoint = iconCode, useSolid = true, sizeSp = 13)
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = text,
      color = MaterialTheme.colorScheme.onSurface,
      fontSize = 12.sp,
      maxLines = 1
    )
  }
}
