package com.reoky.raidframer.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

object OverlayHoverState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val hoveredOverlays = MutableStateFlow<Set<OverlayType>>(emptySet())

    val isAnyOverlayHovered: StateFlow<Boolean> = hoveredOverlays
        .map { it.isNotEmpty() }
        .stateIn(scope, SharingStarted.Eagerly, false)

    fun setHovered(type: OverlayType, hovered: Boolean) {
        hoveredOverlays.value = if (hovered) {
            hoveredOverlays.value + type
        } else {
            hoveredOverlays.value - type
        }
    }
}
