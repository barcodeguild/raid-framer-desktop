import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import ui.CombatOverlayLayout
import ui.FileSelectionDialog
import ui.OverlayWindow
import viewmodel.CombatOverlayModel
import java.awt.Dimension
import java.awt.Point
import kotlin.system.exitProcess

var windowSize = IntSize(478, 192)

fun main() = application {

  val icon = painterResource("desktop.ico")

  /*
   * Starts the application by attaching the interactors and listeners.
   */
  fun prepareApplication() {
    CombatEventInteractor.start()
  }

  /*
   * Performs tear-down and exits the application.
   */
  fun exitApplication() {
    CombatEventInteractor.stop()
    exitProcess(0)
  }

  /* Shows combat-related statistics for the raid. */
  OverlayWindow(
    ".: Raid Framer Combat Overlay :.",
    initialPosition = WindowPosition(0.dp, 0.dp),
    initialSize = DpSize(200.dp, 120.dp),
    ::exitApplication
  ) {
    CombatOverlayLayout(CombatOverlayModel())
  }

  /* Shows who current has boss aggro. */
  OverlayWindow(
    ".: Raid Framer Boss Aggro Overlay :.",
    initialPosition = WindowPosition(100.dp, 100.dp),
    initialSize = DpSize(128.dp, 48.dp),
    ::exitApplication
  ) {
    CombatOverlayLayout(CombatOverlayModel())
  }

  /* Raid Framer settings window. */
  Window(
    onCloseRequest = ::exitApplication,
    icon = icon,
    resizable = false,
    title = "Raid Framer Settings",
    alwaysOnTop = true,
    focusable = true
  ) {
    window.size = Dimension(1000, 1000)
    window.location = Point(100, 100)
    window.isVisible = true
    Box(
      modifier = Modifier
        .background(Color(0f, 0f, 0f, 0.43f))
        .fillMaxSize()
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        Text("Raid Framer Settings", color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Select the combat log file to use for raid statistics.", color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = { prepareApplication() },
          modifier = Modifier.padding(16.dp),
          colors = ButtonDefaults.buttonColors(Color(1f, 1f, 1f, 0.43f))
        ) {
          Text("Select File")
        }
      }
    }
    val showDialog = remember { mutableStateOf(true) }
    val selectedItem = remember { mutableStateOf("") }

    FileSelectionDialog(CombatEventInteractor.possiblePaths, showDialog, selectedItem)
    LaunchedEffect(selectedItem.value) {
      CombatEventInteractor.selectedPath = selectedItem.value
    }
  }
}