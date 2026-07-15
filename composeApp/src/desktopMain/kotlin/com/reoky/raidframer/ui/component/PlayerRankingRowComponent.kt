package com.reoky.raidframer.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.reoky.raidframer.core.model.PlayerCard
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.font.FontWeight
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.definitions.sortedByDisplayOrder
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.localizedDisplayNameRes
import com.reoky.raidframer.core.helpers.getFactionHighlightColor
import com.reoky.raidframer.core.helpers.skillTreeIconPainterFor
import com.reoky.raidframer.core.model.Faction
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayerRankingRow(
  index: Int,
  card: PlayerCard,
  valueText: String,
  valueColor: Color,
  isRetribution: Boolean,
  flashingColor: Color,
  onClick: () -> Unit,
  isOwnCharacter: Boolean = false
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  var showSpecTooltip by remember { mutableStateOf(false) }

  // Resolve SpecType from the card's build string
  val spec = remember(card.currentBuild) { SpecType.fromName(card.currentBuild) }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .background(if (isHovered) Color.Red.copy(alpha = 0.25f) else Color.Transparent)
      .hoverable(interactionSource = interactionSource)
      .padding(vertical = 2.dp)
  ) {

    // 1. Rank Number
    Text(
      text = "${index + 1}. ",
      color = Color.White,
      overflow = TextOverflow.Ellipsis,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1
    )

    // 2. Class Icons Pill (Skill Trees)
    // Only render if we successfully resolved a spec
    Row(
      modifier = Modifier
        .padding(end = 6.dp)
        .background(Color.Transparent, RoundedCornerShape(4.dp))
        .onPointerEvent(PointerEventType.Enter) { showSpecTooltip = true }
        .onPointerEvent(PointerEventType.Exit) { showSpecTooltip = false },
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (spec == null || spec == SpecType.UNKNOWN) {
        val unknownPainter = skillTreeIconPainterFor(null)
        for (i in 1..3) {
          Image(
            painter = unknownPainter,
            contentDescription = "?",
            modifier = Modifier.size(16.dp).padding(horizontal = 1.dp)
          )
        }
      } else {
        spec.trees.sortedByDisplayOrder().forEach { treeName ->
          val painter = skillTreeIconPainterFor(treeName)
          Image(
            painter = painter,
            contentDescription = "Eek",
            modifier = Modifier.size(16.dp).padding(horizontal = 1.dp)
          )
        }
      }
    }

    // 3. name only
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = card.name,
        color = Color.White,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
      )

      Spacer(modifier = Modifier.width(4.dp))

      Box(
        modifier = Modifier
          .size(6.dp)
          .offset(y = 1.dp)
          .background(Faction.fromString(RFConfig.state.value.playerFaction).getFactionHighlightColor(Faction.fromString(card.lastKnownFaction)), CircleShape)
      )
    }

    Spacer(modifier = Modifier.width(6.dp))

    // 4. Value (Totals)
    // Always render value text and a separate asterisk (or invisible placeholder of same width)
    // to keep all rows right-aligned at the same position
    Text(
      text = valueText,
      color = valueColor,
      maxLines = 1,
      textAlign = TextAlign.End
    )
    if (isOwnCharacter) {
      Text(
        text = "*",
        color = valueColor,
        maxLines = 1
      )
    } else {
      Text(
        text = "*",
        color = Color.Transparent,
        maxLines = 1
      )
    }

    // 5. Retribution Icon
    Box(
      modifier = Modifier.width(24.dp),
      contentAlignment = Alignment.Center
    ) {
      if (isRetribution) {
        Text(
          text = "⛨",
          color = flashingColor,
          maxLines = 1,
          fontSize = 12.sp
        )
      }
    }
  }

  // Tooltip for Spec/Class details
  if (showSpecTooltip && spec != null) {
    val resolvedTrees = spec.trees.sortedByDisplayOrder().map { stringResource(it.localizedDisplayNameRes) }
    val treeNames = resolvedTrees.joinToString(", ")
    Popup(
      alignment = Alignment.TopStart,
      offset = androidx.compose.ui.unit.IntOffset(x = 20, y = 20)
    ) {
      Surface(
        shape = RoundedCornerShape(4.dp),
        elevation = 4.dp,
        color = Color.Black.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color.Gray)
      ) {
        Text(
          text = "${stringResource(spec.localizedDisplayNameRes)} ($treeNames)",
          color = Color.White,
          modifier = Modifier.padding(8.dp),
          fontSize = 12.sp
        )
      }
    }
  }
}

