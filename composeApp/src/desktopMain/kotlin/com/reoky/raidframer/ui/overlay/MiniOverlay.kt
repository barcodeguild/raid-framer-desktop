package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.helpers.rememberSectionPulse
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.graphs.PlayerMetricMiniLineGraphComponent
import com.reoky.raidframer.OverlayNav

@Preview
@Composable
fun MiniOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    MiniOverlay()
  }
}

@Composable
fun MiniOverlay(wm: WindowManager? = null) {
  // Flash the overlay border when first enabled so the user can find it.
  var miniPulseActive by remember { mutableStateOf(false) }
  LaunchedEffect(OverlayNav.highlightMiniGraphOverlay.value) {
    if (OverlayNav.highlightMiniGraphOverlay.value) {
      miniPulseActive = true
      OverlayNav.highlightMiniGraphOverlay.value = false
    }
  }
  val miniBorder = rememberSectionPulse(miniPulseActive, restColor = Color.Transparent)

  RFConfig.state.collectAsState().let {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .border(2.dp, miniBorder, RoundedCornerShape(4.dp))
    ) {
      PlayerMetricMiniLineGraphComponent(
        playerName = it.value.playerName,
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .padding(12.dp)
      )
    }
  }
}
