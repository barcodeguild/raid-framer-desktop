package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.core.helpers.FontsHelper
import androidx.compose.material.TextButton
import androidx.compose.material.IconButton

@Composable
fun TitleBarComponent(
  title: String?,
  onClose: () -> Unit,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  captureActions: (@Composable RowScope.() -> Unit)? = null,
  leadingActions: @Composable RowScope.() -> Unit = {},
  rightActions: @Composable RowScope.() -> Unit = {},
  trailingContent: @Composable RowScope.() -> Unit = {},
  transparent: Boolean = false,
  height: Dp = 46.dp,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .then(if (!transparent) Modifier.background(Color.Black.copy(alpha = 0.35f)) else Modifier)
      .then(if (!transparent) Modifier.drawBehind {
        // subtle diagonal grippable texture
        val step = 12f
        var offsetX = -size.height
        val lineColor = Color.White.copy(alpha = 0.03f)
        while (offsetX < size.width + size.height) {
          drawLine(
            color = lineColor,
            strokeWidth = 1f,
            start = Offset(offsetX, 0f),
            end = Offset(offsetX - size.height, size.height)
          )
          offsetX += step
        }

        // a faint highlight above a darker thin separator
        val separatorStroke = 1.dp.toPx()
        val highlightStroke = 0.5.dp.toPx()
        val darkSeparator = Color.Black.copy(alpha = 0.80f)
        val topHighlight = Color.White.copy(alpha = 0.20f)

        val yDark = size.height - separatorStroke / 2f
        val yLight = yDark - highlightStroke

        drawLine(
          color = topHighlight,
          strokeWidth = highlightStroke,
          start = Offset(0f, yLight),
          end = Offset(size.width, yLight)
        )
        drawLine(
          color = darkSeparator,
          strokeWidth = separatorStroke,
          start = Offset(0f, yDark),
          end = Offset(size.width - 0, yDark)
        )
      } else Modifier)
  ) {

    if (title != null) {
      Text(
        text = title,
        color = Color.White,
        modifier = Modifier.align(Alignment.Center),
        style = TextStyle(
          fontFamily = FontsHelper.arKorean()
        )
      )
    }

    if (actionLabel != null && onAction != null) {
      TextButton(
        onClick = onAction,
        modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp)
      ) {
        Text(actionLabel, color = Color.White)
      }
    }

    Row(
      modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      leadingActions()
    }

    Row(
      modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      rightActions()
      Spacer(Modifier.width(8.dp))
      captureActions?.invoke(this)
      trailingContent()
      CloseButton(onClose = onClose)
    }
  }
}
