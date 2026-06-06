package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.CloseButton
import org.jetbrains.compose.resources.painterResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.raidframer

@Preview
@Composable
fun PreviewInstallationOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    InstallationOverlay()
  }
}

@Composable
fun InstallationOverlay(wm: WindowManager? = null) {
  Box(modifier = Modifier.fillMaxSize()) {

     CloseButton(
       onClose = { wm?.closeWindow(OverlayType.ABOUT) },
       modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
     )

    // content area
    Column(
      modifier = Modifier.fillMaxSize().padding(12.dp)
    ) {

      // Header Logo and Version
      Row {
        Column(modifier = Modifier.weight(0.33f)) {
          val image = painterResource(Res.drawable.raidframer)
          Image(
            painter = image,
            contentDescription = "Raid Framer Icon",
            modifier = Modifier.align(Alignment.CenterHorizontally)
          )
        }

      }

      Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))
    }
  }
}