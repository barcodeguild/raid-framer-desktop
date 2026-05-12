package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.interactor.GameMonitorInteractor
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import com.reoky.raidframer.ui.component.graphs.GroupSpec
import com.reoky.raidframer.ui.component.graphs.MultiPlayerMetricLineChart
import com.reoky.raidframer.ui.component.graphs.PlayerMetricMiniLineGraphComponent
import kotlin.String

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
  RFConfig.state.collectAsState().let {
    PlayerMetricMiniLineGraphComponent(
      playerName = it.value.playerName,
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .padding(12.dp)
    )
  }
}
