package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CloseButton(
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    IconButton(
      onClick = onClose,
      modifier = Modifier
        .size(32.dp)
        .background(
          if (isHovered) Color.Red.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.20f),
          MaterialTheme.shapes.small
        )
        .shadow(
          elevation = 0.dp,
          clip = true,
          ambientColor = Color.Transparent,
          spotColor = Color.Transparent
        )
        .hoverable(interactionSource = interactionSource)
        .clip(RoundedCornerShape(8.dp))
    ) {
      Text(
        "✕",
        fontSize = 18.sp,
        color = Color.White,
        textAlign = TextAlign.Center
      )
    }
  }
}