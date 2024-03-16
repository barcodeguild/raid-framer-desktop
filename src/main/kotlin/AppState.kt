import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

object AppState {
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
}
