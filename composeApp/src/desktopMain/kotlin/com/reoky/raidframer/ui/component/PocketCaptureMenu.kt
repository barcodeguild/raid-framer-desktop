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

@Composable
fun PocketCaptureMenu(
  onReferenceInPocket: () -> Unit,
  onExportPng: () -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val dragLock = LocalDragLock.current

  LaunchedEffect(expanded) {
    dragLock.value = expanded
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
        onReferenceInPocket()
      }) { Text("Reference in Pocket") }
      DropdownMenuItem(onClick = {
        expanded = false
        onExportPng()
      }) { Text("Export PNG") }
    }
  }
}
