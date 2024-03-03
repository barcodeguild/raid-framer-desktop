package ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import viewmodel.CombatOverlayModel
import kotlin.system.exitProcess

@Preview
@Composable
fun PreviewCombatOverlay() {
  CombatOverlayLayout(CombatOverlayModel())
}

@Composable
fun CombatOverlayLayout(state: CombatOverlayModel? = null) {
  Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(text = state?.text?.value ?: "", color = Color.White, modifier = Modifier.padding(16.dp))
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
      Text(text = "Raid statistics will become available once you join a raid. ~", color = Color.White, modifier = Modifier.padding(16.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
      Button(onClick = { exitProcess(0) }, modifier = Modifier.padding(16.dp), colors = ButtonDefaults.buttonColors(Color(1f, 1f, 1f, 0.43f))) {
        Text("Close")
      }
    }
  }
}