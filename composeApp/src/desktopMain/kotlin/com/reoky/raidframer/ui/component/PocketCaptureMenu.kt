package com.reoky.raidframer.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.reoky.raidframer.ui.LocalDragLock
import kotlinx.coroutines.delay

@Composable
fun PocketCaptureMenu(
  onReferenceInPocket: () -> Unit,
  onExportPng: () -> Unit,
  onCopyToClipboard: () -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  val dragLock = LocalDragLock.current

  LaunchedEffect(expanded) {
    dragLock.value = expanded
  }

  LaunchedEffect(pendingAction) {
    val action = pendingAction ?: return@LaunchedEffect
    delay(150)
    pendingAction = null
    action()
  }

  Column {
    IconButton(onClick = { expanded = !expanded }) {
      Text("▣", color = androidx.compose.ui.graphics.Color.White)
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      DropdownMenuItem(onClick = {
        expanded = false
        pendingAction = onReferenceInPocket
      }) { Text("Reference in Pocket") }
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
}
