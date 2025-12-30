package com.reoky.raidframer.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Convenience function to create a labeled checkbox to reduce code bulk. Takes an initial state
 * and then a callback for when the state changes.
 */
@Composable
fun CheckBoxComponent(
  label: String,
  initialChecked: Boolean = false,
  onCheckedChange: ((Boolean) -> Unit)? = null,
  modifier: Modifier = Modifier,
  textColor: Color = Color.White,
  spacing: Dp = 8.dp
) {
  var checked by rememberSaveable { mutableStateOf(initialChecked) }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
  ) {
    Checkbox(
      checked = checked,
      onCheckedChange = { new ->
        checked = new
        onCheckedChange?.invoke(new)
      },
      colors = CheckboxDefaults.colors(
        checkmarkColor = Color.White,
        checkedColor = Color.Red,
        uncheckedColor = Color.White
      )
    )
    Spacer(modifier = Modifier.width(spacing))
    Text(text = label, color = textColor)
  }
}