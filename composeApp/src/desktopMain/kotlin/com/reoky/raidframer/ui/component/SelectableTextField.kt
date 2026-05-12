package com.reoky.raidframer.ui.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SelectableTextField(
  value: String,
  modifier: Modifier = Modifier,
  minHeight: Dp = 56.dp
) {
  TextField(
    value = value,
    onValueChange = {}, // read-only
    readOnly = true,
    textStyle = TextStyle(fontSize = 14.sp),
    colors = TextFieldDefaults.textFieldColors(
      textColor = Color.White,
      backgroundColor = Color.Transparent,
      focusedIndicatorColor = Color.Red,
      unfocusedIndicatorColor = Color.White,
      placeholderColor = Color.LightGray,
      cursorColor = Color.Red
    ),
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = minHeight),
    maxLines = 10
  )
}
