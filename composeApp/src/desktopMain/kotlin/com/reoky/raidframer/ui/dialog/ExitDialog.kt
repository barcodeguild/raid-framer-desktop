package com.reoky.raidframer.ui.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.quit
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.exit_dialog_title
import raid_framer_desktop.composeapp.generated.resources.exit_dialog_message
import raid_framer_desktop.composeapp.generated.resources.exit_dialog_dismiss
import raid_framer_desktop.composeapp.generated.resources.general_exit

@Composable
fun exitDialog(shouldShowExitDialog: MutableState<Boolean>) {
  if (shouldShowExitDialog.value) {
    AlertDialog(
      onDismissRequest = { shouldShowExitDialog.value = false },
      title = { Text(stringResource(Res.string.exit_dialog_title), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          stringResource(Res.string.exit_dialog_message),
          color = Color.White
        )
      },
      backgroundColor = Color.Black,
      confirmButton = {
        Button(
          onClick = { quit() },
          colors = ButtonDefaults.buttonColors(Color.Red.copy(alpha = 0.75f)),
          modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
        ) {
          Text(stringResource(Res.string.general_exit), color = Color.White)
        }
      },
      dismissButton = {
        Button(
          onClick = { shouldShowExitDialog.value = false },
          colors = ButtonDefaults.buttonColors(Color.White),
          modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
        ) {
          Text(stringResource(Res.string.exit_dialog_dismiss), color = Color.Black)
        }
      }
    )
  }
}