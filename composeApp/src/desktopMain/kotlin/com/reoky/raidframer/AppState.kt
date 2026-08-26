package com.reoky.raidframer

import com.reoky.raidframer.ui.component.graphs.GraphMetricType
import com.reoky.raidframer.ui.overlay.RaidTab
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

/**
 * Ephemeral state shared between the RaidOverlay Buffs tab and the Settings overlay so the
 * "check for loot buffs?" toggle stays in sync while the app is running. Unlike the loot-buff
 * threshold value (which is persisted in config), this enabled flag is intentionally NOT
 * persisted — the checkbox resets to unchecked on the next app launch per the feature spec.
 */
object RaidCallerSync {
  private val _lootBuffEnabled = MutableStateFlow(false)
  val lootBuffEnabled = _lootBuffEnabled.asStateFlow()
  fun setLootBuffEnabled(enabled: Boolean) {
    _lootBuffEnabled.value = enabled
  }
}

/**
 * One-shot, cross-overlay navigation + highlight requests, fired from the Raid Caller overlay
 * (or any other overlay) and consumed by the target overlay in a `LaunchedEffect`, which then
 * clears the value so each request only fires once.
 */
object OverlayNav {
  // Raid overlay: which tab to select on the next open.
  val pendingRaidTab = MutableStateFlow<RaidTab?>(null)
  // Raid overlay -> Buffs tab: flash the buff-selection pane to draw attention.
  val highlightRaidBuffSelect = MutableStateFlow(false)
  // Summary overlay: which dropdown index to select on the next open.
  val pendingSummaryTabIndex = MutableStateFlow<Int?>(null)
  // Settings overlay: scroll to + flash the General Settings section.
  val highlightSettingsGeneral = MutableStateFlow(false)
  // Item Use overlay: flash the border when first enabled so the user can find it.
  val highlightItemUseOverlay = MutableStateFlow(false)
  // Raid Caller overlay: flash the border when first enabled so the user can find it.
  val highlightRaidCallerOverlay = MutableStateFlow(false)
  // Mini Graph overlay: flash the border when first enabled so the user can find it.
  val highlightMiniGraphOverlay = MutableStateFlow(false)
}