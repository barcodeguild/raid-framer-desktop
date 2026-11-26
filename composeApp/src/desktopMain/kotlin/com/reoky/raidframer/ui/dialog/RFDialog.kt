package com.reoky.raidframer.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.RFColors

/*
 * An in-window dialog, replacing system dialogs throughout the app.
 *
 * Compose's AlertDialog and Dialog each open a separate operating-system window, which
 * does not inherit the always-on-top flag of the overlay windows — so on this game
 * overlay those dialogs appear behind the game, cannot be focused, and cannot even be
 * alt-tabbed to. RFDialog draws inside the current window instead: a click-blocking
 * scrim over the whole window with the dialog card centered on top of it.
 *
 * Place it as a direct child of the window's root container (after the regular
 * content) so the scrim covers the whole window.
 */
@Composable
fun RFDialog(
  title: String? = null,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  text: String? = null,
  content: (@Composable ColumnScope.() -> Unit)? = null,
  confirmLabel: String? = null,
  dismissLabel: String? = null,
  confirmColor: Color = RFColors.AccentRed,
  onConfirm: (() -> Unit)? = null,
  dismissOnClickOutside: Boolean = true
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.60f))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) {
        if (dismissOnClickOutside) onDismissRequest()
      }
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = RFColors.CardBackground,
      elevation = 8.dp,
      modifier = Modifier
        .widthIn(min = 340.dp, max = 640.dp)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClickLabel = null
        ) {} // swallow clicks so they do not fall through to the scrim
    ) {
      Column(
        modifier = Modifier
          .border(1.dp, RFColors.CardBorder, RoundedCornerShape(10.dp))
          .padding(20.dp)
      ) {
        title?.let {
          Text(
            text = it,
            color = RFColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(12.dp))
        }
        text?.let {
          Text(
            text = it,
            color = RFColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
          )
          Spacer(modifier = Modifier.height(16.dp))
        }
        content?.invoke(this)
        if (confirmLabel != null || dismissLabel != null) {
          if (content != null) Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            RFDialogDismissButton(dismissLabel, onDismissRequest)
            confirmLabel?.let { label ->
              Button(
                onClick = { onConfirm?.invoke() },
                colors = ButtonDefaults.buttonColors(confirmColor)
              ) {
                Text(text = label, color = Color.White, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }
    }
  }
}

/** The neutral left-hand action of a dialog; nothing is rendered when [label] is null. */
@Composable
private fun RowScope.RFDialogDismissButton(label: String?, onClick: () -> Unit) {
  label?.let {
    Button(
      onClick = onClick,
      colors = ButtonDefaults.buttonColors(RFColors.CardBorder)
    ) {
      Text(text = it, color = RFColors.TextPrimary, fontWeight = FontWeight.SemiBold)
    }
    Spacer(modifier = Modifier.width(12.dp))
  }
}
