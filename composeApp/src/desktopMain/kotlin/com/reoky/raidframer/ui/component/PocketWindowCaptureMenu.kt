package com.reoky.raidframer.ui.component

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.ui.LocalDragLock
import kotlinx.coroutines.delay

@Composable
fun PocketWindowCaptureMenu(
  onSaveToPocket: () -> Unit,
  onExportPng: () -> Unit,
  onCopyToClipboard: () -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  val dragLock = LocalDragLock.current
  dragLock.value = expanded

  LaunchedEffect(pendingAction) {
    val action = pendingAction ?: return@LaunchedEffect
    delay(150)
    pendingAction = null
    action()
  }

  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  IconButton(
    onClick = { expanded = !expanded },
    modifier = Modifier.size(36.dp)
  ) {
    Text(
      "\uf03e",
      color = if (isHovered) RFColors.AccentRed else Color.White,
      fontFamily = FontsHelper.faSolid(),
      fontSize = 18.sp,
      modifier = Modifier.hoverable(interactionSource)
    )
  }
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = { expanded = false }
  ) {
    DropdownMenuItem(onClick = {
      expanded = false
      pendingAction = onSaveToPocket
    }) { Text("Save to Pocket") }
    DropdownMenuItem(onClick = {
      expanded = false
      pendingAction = onExportPng
    }) { Text("Export PNG") }
    DropdownMenuItem(onClick = {
      expanded = false
      pendingAction = onCopyToClipboard
    }) { Text("Copy to Clipboard") }
  }
}
