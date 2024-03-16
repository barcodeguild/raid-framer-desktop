package ui.dialog

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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.system.exitProcess

@Composable
fun exitDialog(shouldShowExitDialog: MutableState<Boolean>) {
  if (shouldShowExitDialog.value) {
    AlertDialog(
      onDismissRequest = { shouldShowExitDialog.value = false },
      title = { Text("Are you sure?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "This will exit and close any open overlay windows. You can launch .:Raid Framer:. again from the Windows Start Menu..",
          color = Color.White
        )
      },
      backgroundColor = Color.Black,
      confirmButton = {
        Button(
          onClick = { exitProcess(0) },
          colors = ButtonDefaults.buttonColors(Color.Red.copy(alpha = 0.75f)),
          modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
        ) {
          Text("Exit", color = Color.White)
        }
      },
      dismissButton = {
        Button(
          onClick = { shouldShowExitDialog.value = false },
          colors = ButtonDefaults.buttonColors(Color.White),
          modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
        ) {
          Text("Nope", color = Color.Black)
        }
      }
    )
  }
}