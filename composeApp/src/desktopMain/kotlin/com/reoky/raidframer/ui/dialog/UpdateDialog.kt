package com.reoky.raidframer.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.UpdateInfo
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.update_dialog_install
import raid_framer_desktop.composeapp.generated.resources.update_dialog_later
import raid_framer_desktop.composeapp.generated.resources.update_dialog_message
import raid_framer_desktop.composeapp.generated.resources.update_dialog_title

/*
 * Shown inside the combat overlay window when an update is available. Drawn in-window
 * (see RFDialog) so it can never end up behind the game.
 */
@Composable
fun updateDialog(
  shouldShowUpdateDialog: MutableState<Boolean>,
  updateInfo: UpdateInfo?,
  onDownloadAndInstall: (UpdateInfo) -> Unit
) {
  if (shouldShowUpdateDialog.value && updateInfo != null) {
    RFDialog(
      title = stringResource(Res.string.update_dialog_title),
      text = stringResource(Res.string.update_dialog_message, updateInfo.version, AppGlobals.APP_VERSION),
      onDismissRequest = { shouldShowUpdateDialog.value = false },
      confirmLabel = stringResource(Res.string.update_dialog_install),
      confirmColor = RFColors.UpdateGreen,
      dismissLabel = stringResource(Res.string.update_dialog_later),
      onConfirm = {
        shouldShowUpdateDialog.value = false
        onDownloadAndInstall(updateInfo)
      }
    )
  }
}
