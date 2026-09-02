package com.reoky.raidframer.ui.component

import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.ui.LocalDragLock

@Composable
fun PocketWindowCaptureMenu(
  onSaveToPocket: () -> Unit,
  onExportPng: () -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val dragLock = LocalDragLock.current
  dragLock.value = expanded

  IconButton(
    onClick = { expanded = !expanded },
    modifier = Modifier.size(36.dp)
  ) {
    Text("\uf03e", color = Color.White, fontFamily = FontsHelper.faSolid(), fontSize = 18.sp)
  }
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = { expanded = false }
  ) {
    DropdownMenuItem(onClick = {
      expanded = false
      onSaveToPocket()
    }) { Text("Save to Pocket") }
    DropdownMenuItem(onClick = {
      expanded = false
      onExportPng()
    }) { Text("Export PNG") }
  }
}
