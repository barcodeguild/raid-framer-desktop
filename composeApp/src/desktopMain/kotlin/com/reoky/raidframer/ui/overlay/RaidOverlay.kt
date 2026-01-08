package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.RaidComponent
import com.reoky.raidframer.ui.component.TitleBarComponent

// ... PreviewRaidOverlay remains the same ...

@Composable
fun RaidOverlay(wm: WindowManager? = null) {
  // Collect data for both raids
  val mainRaid = PlayerCacheInteractor.getRaidById(0).collectAsState()
  val coRaid = PlayerCacheInteractor.getRaidById(1).collectAsState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(4.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(1.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      TitleBarComponent(
        title = "Raid Overlay",
        onClose = { wm?.closeWindow(OverlayType.RAID) }
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Use a Row to place the two raid sections side-by-side
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp) // Gap between the Main and Co-Raid
      ) {
        // --- Main Raid Section ---
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "Main Raid",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          RaidComponent(
            parties = mainRaid.value,
            modifier = Modifier.fillMaxWidth()
          )
        }

        // --- Co-Raid Section ---
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "Co-Raid",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          RaidComponent(
            parties = coRaid.value,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }
  }
}
