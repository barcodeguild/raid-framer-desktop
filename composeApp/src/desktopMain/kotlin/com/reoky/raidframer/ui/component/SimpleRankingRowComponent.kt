package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 *  Like player ranking row except without all the complexity. No icons, no tooltips, just basic name and value.
 *  Using this for ranking other things like spell damages, utility items, etc.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SimpleRankingRow(
  index: Int,
  name: String,
  valueText: String,
  valueColor: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .background(if (isHovered) Color.Red.copy(alpha = 0.25f) else Color.Transparent)
      .hoverable(interactionSource = interactionSource)
      .padding(vertical = 2.dp, horizontal = 4.dp)
  ) {

    // 1. Rank Number
    Text(
      text = "${index + 1}. ",
      color = Color.White,
      overflow = TextOverflow.Ellipsis,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1
    )

    // 2. Name (no icons / tooltip)
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = name,
        color = Color.White,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
      )
    }

    Spacer(modifier = Modifier.width(6.dp))

    // 3. Value (Totals)
    Text(
      text = valueText,
      color = valueColor,
      maxLines = 1,
      textAlign = TextAlign.End
    )
  }
}
