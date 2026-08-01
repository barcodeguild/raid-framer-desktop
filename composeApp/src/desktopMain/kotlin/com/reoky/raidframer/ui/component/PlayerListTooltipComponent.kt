package com.reoky.raidframer.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.localizedDisplayNameRes
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.helpers.RFColors
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.raid_composition_more_players

private const val MAX_TOOLTIP_PLAYERS = 15

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayerListTooltipComponent(
  players: List<PlayerCard>,
  modifier: Modifier = Modifier,
  hoverColor: Color = RFColors.CardBorderAccent,
  content: @Composable (Modifier) -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  var pointerPosition by remember { mutableStateOf(IntOffset.Zero) }
  content(
    modifier
      .hoverable(interactionSource)
      .onPointerEvent(PointerEventType.Move) { event ->
        val position = event.changes.first().position
        pointerPosition = IntOffset(position.x.toInt(), position.y.toInt())
      }
  )

  if (isHovered && players.isNotEmpty()) {
    Popup(
      popupPositionProvider = object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
          val x = (anchorBounds.left + pointerPosition.x - popupContentSize.width - 12)
            .coerceIn(8, (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8))
          val y = (anchorBounds.top + pointerPosition.y + 12)
            .coerceIn(8, (windowSize.height - popupContentSize.height - 8).coerceAtLeast(8))
          return IntOffset(x, y)
        }
      }
    ) {
      Surface(
        shape = RoundedCornerShape(6.dp),
        elevation = 6.dp,
        color = RFColors.PopupBackground.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, RFColors.CardBorder.copy(alpha = 0.7f)),
        modifier = Modifier.hoverable(interactionSource)
      ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp).widthIn(max = 300.dp)) {
          players.take(MAX_TOOLTIP_PLAYERS).forEach { player ->
            val spec = SpecType.fromName(player.currentBuild)
            Row {
              Text(player.name, color = Color.White, fontSize = 10.sp)
              Spacer(Modifier.width(8.dp))
              Text(player.lastKnownGearScore.takeIf { it > 0 }?.toString() ?: "?", color = RFColors.TextTertiary, fontSize = 10.sp)
              Spacer(Modifier.width(8.dp))
              Text(spec?.let { stringResource(it.localizedDisplayNameRes) } ?: player.currentBuild.ifBlank { "?" }, color = RFColors.TextSecondary, fontSize = 10.sp)
            }
          }
          if (players.size > MAX_TOOLTIP_PLAYERS) {
            Text(
              stringResource(Res.string.raid_composition_more_players, players.size - MAX_TOOLTIP_PLAYERS),
              color = RFColors.TextTertiary,
              fontSize = 9.sp,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }
      }
    }
  }
}
