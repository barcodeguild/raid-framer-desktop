package com.reoky.raidframer

import com.reoky.raidframer.ui.component.graphs.GraphMetricType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * Used for ephemeral app state that needs to be shared across multiple features, but does not need to be persisted.
 */
object AppState {

  // selected player name for displaying detailed info
  private val _selectedPlayer = MutableStateFlow<String?>(null)
  var selectedPlayer = _selectedPlayer.asStateFlow()
  fun selectPlayer(name: String?) {
    _selectedPlayer.value = name
  }

  // selected metric type for graphs
  private val _selectedMetricType = MutableStateFlow<GraphMetricType>(GraphMetricType.DAMAGE)
  var selectedMetricType = _selectedMetricType.asStateFlow()
  fun selectMetricType(type: GraphMetricType) {
    _selectedMetricType.value = type
  }

  // selected target name for displaying detailed info (tied to tab target feature in the game)
  private val _selectedTarget = MutableStateFlow<String?>(null)
  var selectedTarget = _selectedTarget.asStateFlow()
  fun selectTarget(name: String?) {
    _selectedTarget.value = name
  }

  // handles whether overlays should be visible based on tab detection and obstruction detection
  private val _isEverythingVisible = MutableStateFlow(true)
  var isEverythingVisible = _isEverythingVisible.asStateFlow()
  fun setEverythingVisible(visible: Boolean) {
    _isEverythingVisible.value = visible
  }

  // set on launch if a stale session was cleared (crash recovery)
  var crashRecoverySessionTitle: String? = null
  private val _crashRecoveryDismissed = MutableStateFlow(false)
  val crashRecoveryDismissed = _crashRecoveryDismissed.asStateFlow()
  fun dismissCrashRecovery() {
    _crashRecoveryDismissed.value = true
  }

}