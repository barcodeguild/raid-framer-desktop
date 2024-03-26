import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import core.database.Schema
import dorkbox.systemTray.SystemTray
import java.nio.file.Path

object AppState {

  val isEverythingResizable: MutableState<Boolean> = mutableStateOf(true)

  /* User Wants Windows Visible */
  val isAboutOverlayVisible: MutableState<Boolean> = mutableStateOf(false)
  val isEverythingVisible: MutableState<Boolean> = mutableStateOf(false)
  val isCombatOverlayVisible: MutableState<Boolean> = mutableStateOf(true)
  val isTrackerOverlayVisible: MutableState<Boolean> = mutableStateOf(true)
  val isAggroOverlayVisible: MutableState<Boolean> = mutableStateOf(false)
  val isSettingsOverlayVisible: MutableState<Boolean> = mutableStateOf(false)
  val isFiltersOverlayVisible: MutableState<Boolean> = mutableStateOf(false)

  /* Window Obstructing Game Window */
  val isCombatObstructing: MutableState<Boolean> = mutableStateOf(false)
  val isAggroObstructing: MutableState<Boolean> = mutableStateOf(false)
  val isTrackerObstructing: MutableState<Boolean> = mutableStateOf(false)

  /* Filters and Mappings */
  val filterNames: MutableState<List<String>> = mutableStateOf(listOf()) // ignore these names
  val remappedNames: MutableState<Map<String, String>> = mutableStateOf(mapOf()) // interpret as: oldName -> newName

  fun toggleFiltersOverlayVisibility() {
    isFiltersOverlayVisible.value = !isFiltersOverlayVisible.value
  }

  fun toggleTrackerOverlayVisibility() {
    isTrackerOverlayVisible.value = !isTrackerOverlayVisible.value
  }

  fun toggleSettingsOverlayVisibility() {
    isSettingsOverlayVisible.value = !isSettingsOverlayVisible.value
  }

  var currentTargetName: MutableState<String> = mutableStateOf("")
  var trackerTargets: MutableState<List<String>> = mutableStateOf(listOf())

  var tessTempDirectory: Path? = null
  var windowStates = Schema.RFWindowStates()
  var config = Schema.RFConfig()
  var tray: SystemTray? = null

}
