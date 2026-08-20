package com.reoky.raidframer.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.RFColors

/**
 * A reusable banner shown when a performance setting disables a feature.
 * Used across overlays to inform users why certain data is not appearing.
 */
@Composable
fun PerformanceDisabledBanner(message: String) {
  Surface(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    shape = RoundedCornerShape(6.dp),
    color = RFColors.AccentRed.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, RFColors.AccentRed.copy(alpha = 0.4f))
  ) {
    Text(
      text = message,
      color = RFColors.AccentRed,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
    )
  }
}
