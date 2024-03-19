package ui.dialog

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
      title = {
        Text(
          modifier = Modifier.fillMaxWidth(),
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.W500,
          text = "Select Combat Logfile",
          textAlign = TextAlign.Start
        )
      },
      modifier = Modifier.background(Color.Transparent),
      backgroundColor = Color(64,64,64,255),
      text = {
        LazyColumn(Modifier.background(Color.Transparent)) {
          items(count = possiblePaths.count(), itemContent = { itemId ->
            Box(
            modifier = Modifier
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
            showDialog.value = false
          },
          colors = ButtonDefaults.buttonColors(Color(32,32,32,255)),
          modifier = Modifier.padding(16.dp)
        ) {
          Text(
            text = "OK",
            color = Color.White
          )
        }
      }
    )
  }
}

@Preview
@Composable
fun PreviewFileSelectionDialog() {
  val possiblePaths = emptyList<Path>()
  val showDialog = mutableStateOf(true)
  val selectedItem = mutableStateOf("")
  Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
    FileSelectionDialog(possiblePaths, showDialog, selectedItem)
  }
}