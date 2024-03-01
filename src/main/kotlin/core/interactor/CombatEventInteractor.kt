import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.nio.file.*
import kotlin.io.path.pathString

class CombatEventInteractor(private val onNewLines: (List<String>) -> Unit) {
  private var lastLineCount = 0
  private val scope = CoroutineScope(Dispatchers.IO)

  private var possiblePaths = mutableListOf<Path>()

  @Composable
  fun SelectItemDialog(showDialog: MutableState<Boolean>, selectedItem: MutableState<String>) {
    if (showDialog.value) {
      AlertDialog(
        onDismissRequest = { showDialog.value = false },
        title = { Text(text = "Select an item") },
        text = {
          LazyColumn {
            items(count = possiblePaths.count(), itemContent = { itemId ->
              Text(
                text = possiblePaths[itemId].pathString,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedItem.value = possiblePaths[itemId].pathString }
                  .background(if (possiblePaths[itemId].pathString == selectedItem.value) Color.LightGray else Color.Transparent)
                  .padding(16.dp),
                textAlign = TextAlign.Center
              )
            })
          }
        },
        confirmButton = {
          Button(onClick = {
            println("Selected item: ${selectedItem.value}")
            showDialog.value = false
          }) {
            Text("OK")
          }
        }
      )
    }
  }

  // called before the interactor event loop is started
  init {
    locateCombatLog()
  }

  /*
   * Recursively search for a file named Combat.log in the user's Documents folder and populates a
   * list of possible paths to the file for the user to choose from.
   */
  private fun locateCombatLog() {

    val oneDriveDocumentsPath = Paths.get(System.getProperty("user.home"), "OneDrive", "Documents")
    val documentsPath = Paths.get(System.getProperty("user.home"), "Documents")

    val baseDir = if (Files.exists(oneDriveDocumentsPath)) oneDriveDocumentsPath else documentsPath

    fun seek(baseDir: Path) {
      val stream = Files.list(baseDir)
      stream.use { paths ->
        paths.forEach { path ->
          if (Files.isDirectory(path) && Files.isReadable(path)) {
            seek(path)
          } else if (path.fileName.toString() == "Combat.log") {
            println("Found Combat.log at ${path.pathString}")
            possiblePaths.add(path)
          }
        }
      }
    }

    seek(baseDir)
  }

  @Composable
  fun start() {
    val showDialog = remember { mutableStateOf(true) }
    val selectedItem = remember { mutableStateOf("") }
    SelectItemDialog(showDialog, selectedItem)
    scope.launch {
      while (isActive) {
//        val lines = Files.readAllLines(filePath)
//        if (lines.size > lastLineCount) {
//          onNewLines(lines.subList(lastLineCount, lines.size))
//          lastLineCount = lines.size
//        }
        delay(1000) // delay for a while before checking the file again
      }
    }
  }

  fun stop() {
    scope.cancel()
  }
}