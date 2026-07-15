package com.reoky.raidframer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.rememberTrayState
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.messageBox
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.app_tray_reset_positions
import raid_framer_desktop.composeapp.generated.resources.app_tray_title
import raid_framer_desktop.composeapp.generated.resources.general_about
import raid_framer_desktop.composeapp.generated.resources.general_exit
import raid_framer_desktop.composeapp.generated.resources.general_help_window_postions_reset
import raid_framer_desktop.composeapp.generated.resources.general_settings
import raid_framer_desktop.composeapp.generated.resources.raidframer

@Composable
fun ApplicationScope.SystemTrayComponent(
  wm: WindowManager,
  onExit: () -> Unit
) {
  val trayState = rememberTrayState()
  val titleStr = stringResource(Res.string.app_tray_title)
  val settingsStr = stringResource(Res.string.general_settings)
  val aboutStr = stringResource(Res.string.general_about)
  val resetStr = stringResource(Res.string.app_tray_reset_positions)
  val exitStr = stringResource(Res.string.general_exit)
  val helpStr = stringResource(Res.string.general_help_window_postions_reset)

  Tray(
    state = trayState,
    icon = painterResource(Res.drawable.raidframer),
    tooltip = titleStr,
    menu = {
      Item(
        text = titleStr,
        onClick = {}
      )
      Item(
        text = settingsStr,
        onClick = { wm.openWindow(OverlayType.SETTINGS) }
      )
      Item(
        text = aboutStr,
        onClick = { wm.openWindow(OverlayType.ABOUT) }
      )
      Item(
        text = resetStr,
        onClick = {
          wm.resetAllWindowPositions()
          messageBox(AppGlobals.APP_NAME, helpStr)
        }
      )
      Item(
        text = exitStr,
        onClick = onExit
      )
    }
  )
}
