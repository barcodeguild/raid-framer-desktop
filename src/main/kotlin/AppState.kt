import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import core.database.Schema
import java.nio.file.Path

object AppState {
  val isAboutOverlayVisible: MutableState<Boolean> = mutableStateOf(true)
  val isEverythingResizable: MutableState<Boolean> = mutableStateOf(false)
  val isEverythingVisible: MutableState<Boolean> = mutableStateOf(false)
  val isCombatOverlayVisible: MutableState<Boolean> = mutableStateOf(false)
  val isTrackerOverlayVisible: MutableState<Boolean> = mutableStateOf(false)
  val isAggroOverlayVisible: MutableState<Boolean> = mutableStateOf(false)
  val isSettingsOverlayVisible: MutableState<Boolean> = mutableStateOf(false)

  fun toggleTrackerOverlayVisibility() {
    isTrackerOverlayVisible.value = !isTrackerOverlayVisible.value
  }

  fun toggleSettingsOverlayVisibility() {
    isSettingsOverlayVisible.value = !isSettingsOverlayVisible.value
  }

  var currentTargetName: MutableState<String> = mutableStateOf("")

  var tessTempDirectory: Path? = null
  var windowStates = Schema.RFWindowStates()
  var config = Schema.RFConfig()

}
