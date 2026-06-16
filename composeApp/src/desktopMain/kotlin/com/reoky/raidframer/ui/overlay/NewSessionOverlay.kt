package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.new_session_title

@Composable
fun NewSessionOverlay(wm: WindowManager? = null) {
  val config by RFConfig.state.collectAsState()
  val scrollState = rememberScrollState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF121212))
      .verticalScroll(scrollState)
  ) {
    Column {
      TitleBarComponent(
        title = stringResource(Res.string.new_session_title),
        onClose = { wm?.closeWindow(OverlayType.NEW_SESSION) }
      )
    }
    Column {

    }
  }
}