package com.reoky.raidframer.ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helper.UpdateInfo
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.update_dialog_title
import raid_framer_desktop.composeapp.generated.resources.update_dialog_message
import raid_framer_desktop.composeapp.generated.resources.update_dialog_install
import raid_framer_desktop.composeapp.generated.resources.update_dialog_later

@Composable
fun updateDialog(
  shouldShowUpdateDialog: MutableState<Boolean>,
  updateInfo: UpdateInfo?,
  onDownloadAndInstall: (UpdateInfo) -> Unit
) {
  if (shouldShowUpdateDialog.value && updateInfo != null) {
    Dialog(onDismissRequest = { shouldShowUpdateDialog.value = false }) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Black,
        border = BorderStroke(1.dp, RFColors.CardBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            stringResource(Res.string.update_dialog_title),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            stringResource(Res.string.update_dialog_message, updateInfo.version, AppGlobals.APP_VERSION),
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 15.sp
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Button(
              onClick = { shouldShowUpdateDialog.value = false },
              colors = ButtonDefaults.buttonColors(Color.White),
              modifier = Modifier.padding(end = 8.dp)
            ) {
              Text(stringResource(Res.string.update_dialog_later), color = Color.Black, fontSize = 12.sp)
            }
            Button(
              onClick = {
                shouldShowUpdateDialog.value = false
                onDownloadAndInstall(updateInfo)
              },
              colors = ButtonDefaults.buttonColors(RFColors.UpdateGreen)
            ) {
              Text(stringResource(Res.string.update_dialog_install), color = Color.White, fontSize = 12.sp)
            }
          }
        }
      }
    }
  }
}
