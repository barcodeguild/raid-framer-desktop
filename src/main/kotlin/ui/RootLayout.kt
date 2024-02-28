package ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import viewmodel.RootLayoutModel
import kotlin.system.exitProcess

@Composable
@Preview
fun RootLayout(model: RootLayoutModel) {
  Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(text = "Raid Framer Console", color = Color.White, modifier = Modifier.padding(16.dp))
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(text = ": Not Currently in a Raid :", color = Color.White, modifier = Modifier.padding(16.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
      Button(onClick = { exitProcess(0) }, modifier = Modifier.padding(16.dp)) {
        Text("Close")
      }
    }
  }
}