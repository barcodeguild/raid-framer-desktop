package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.nio.file.Path
import kotlin.io.path.pathString

@Composable
fun FileSelectionDialog(possiblePaths: List<Path>, showDialog: MutableState<Boolean>, selectedItem: MutableState<String>) {
  if (showDialog.value) {
    AlertDialog(
      onDismissRequest = { showDialog.value = false },
      title = { Text(color = Color.White, text = "Please Select the Path to Your Combat Logfile", textAlign = TextAlign.Center) },
      modifier = Modifier.background(Color.Transparent),
      contentColor = Color.Gray,
      backgroundColor = Color.DarkGray,
      text = {
        LazyColumn(Modifier.background(Color.DarkGray)) {
          items(count = possiblePaths.count(), itemContent = { itemId ->
            Box(modifier = Modifier
              .fillMaxWidth()
              .background(if (possiblePaths[itemId].pathString == selectedItem.value) Color(0,0,0,128) else Color.Transparent)
              .clickable { selectedItem.value = possiblePaths[itemId].pathString }
              .padding(16.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = "$itemId. ", color = Color.White, fontSize = 12.sp)
                Text(
                  text = possiblePaths[itemId].pathString,
                  modifier = Modifier.weight(1f).padding(start = 8.dp),
                  color = Color.White,
                  fontSize = 12.sp,
                  textAlign = TextAlign.Start
                )
              }
            }
          })
        }
      },
      confirmButton = {
        Button(
          onClick = {
            println("Selected item: ${selectedItem.value}")
            showDialog.value = false
          },
          colors = ButtonDefaults.buttonColors(Color.White)
        ) {
          Text(
            color = Color.Black,
            text = "OK"
          )
        }
      }
    )
  }
}