package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.nio.file.Path
import kotlin.io.path.pathString
@Composable
fun FileSelectionDialog(possiblePaths: List<Path>, showDialog: MutableState<Boolean>, selectedItem: MutableState<String>) {
  if (showDialog.value) {
    AlertDialog(
      onDismissRequest = { showDialog.value = false },
      title = { Text(text = "Select an item") },
      modifier = Modifier.background(Color.Red),
      text = {
        LazyColumn(Modifier.background(Color.Black)) {
          items(count = possiblePaths.count(), itemContent = { itemId ->
            Text(
              text = possiblePaths[itemId].pathString,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedItem.value = possiblePaths[itemId].pathString }
                .background(if (possiblePaths[itemId].pathString == selectedItem.value) Color.LightGray else Color.Transparent)
                .padding(16.dp),
              color = Color.White,
              textAlign = TextAlign.Center
            )
          })
        }
      },
      confirmButton = {
        Button(
          onClick = {
          println("Selected item: ${selectedItem.value}")
          showDialog.value = false
          },
          colors = ButtonDefaults.buttonColors(Color(0f, 0f, 0f, 0.43f))
        ) {
          Text("OK")
        }
      }
    )
  }
}