package com.reoky.raidframer.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.FaIcon
import com.reoky.raidframer.core.helpers.RFTypography
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.factionHighlightColor
import com.reoky.raidframer.core.helpers.skillTreeIconPainterFor
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.sortedByDisplayOrder
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.ui.component.graphs.BuffHistoryPlayerStatus
import com.reoky.raidframer.ui.component.graphs.BuffHistorySnapshotCard
import com.reoky.raidframer.ui.component.graphs.formatBuffHistoryTime
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_buffed
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_buffed_share_value
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_in_raid
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_most_recent
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_not_scannable
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_snapshot
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_unbuffed
import kotlin.math.roundToInt

/** Ideal snapshot-card width for the masonry; columns range from 1 up to [BuffHistoryCardMaxColumns]. */
val BuffHistoryCardIdealWidth = 340.dp
private const val BuffHistoryCardMaxColumns = 3

/** Chevron glyph marking the expand/collapse control on a snapshot card header. */
private const val ChevronsGlyph = "\uF078"

/** Width reserved for spec-tree icons in a roster row, so names align across columns. */
private val SpecIconSlotWidth = 41.dp

/** A clicked player in a Buff History snapshot, plus the popup anchor in window coordinates. */
data class BuffHistoryPlayerDetail(
  val playerName: String,
  val status: BuffHistoryPlayerStatus,
  val buffIds: Set<Int>,
  val timeLabel: String,
  val anchor: IntOffset
)

/**
 * Snapshot cards in a responsive masonry: a single column on narrow windows, up to
 * [BuffHistoryCardMaxColumns] side by side when there is room. Cards flow newest-first so the
 * first column always starts with the most recent snapshot. An expanded card leaves the masonry
 * flow and takes a full-width row, so its whole roster is visible at once; the cards around it
 * reflow into the remaining rows.
 */
@Composable
fun BuffHistorySnapshotList(
  cards: List<BuffHistorySnapshotCard>,
  refStart: Long,
  playerCards: Map<String, PlayerCard>,
  showSpecIcons: Boolean,
  onPlayerClick: (BuffHistoryPlayerDetail) -> Unit,
  modifier: Modifier = Modifier,
  newestExpanded: Boolean = false
) {
  val newest = cards.lastOrNull()
  // Expansion lives here, keyed by timestamp (stable across the periodic snapshot recompute),
  // so the layout can reflow when a card expands or collapses.
  var expandedIds by remember {
    mutableStateOf(if (newestExpanded && newest != null) setOf(newest.timestamp) else emptySet<Long>())
  }
  val toggleExpanded: (Long) -> Unit = { id ->
    expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id
  }
  BoxWithConstraints(modifier) {
    val columnCount = (maxWidth / BuffHistoryCardIdealWidth).toInt().coerceIn(1, BuffHistoryCardMaxColumns)
    // Newest first. Each expanded card becomes its own full-width segment; the collapsed cards
    // between two expanded ones form a round-robin masonry chunk.
    val segments = mutableListOf<List<BuffHistorySnapshotCard>>()
    var chunk = mutableListOf<BuffHistorySnapshotCard>()
    cards.asReversed().forEach { card ->
      if (card.timestamp in expandedIds) {
        if (chunk.isNotEmpty()) {
          segments += chunk
          chunk = mutableListOf()
        }
        segments += listOf(card)
      } else {
        chunk += card
      }
    }
    if (chunk.isNotEmpty()) segments += chunk

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      segments.forEach { segment ->
        if (segment.size == 1 && segment[0].timestamp in expandedIds) {
          val card = segment[0]
          BuffHistorySnapshotCardView(
            card = card,
            timeLabel = formatBuffHistoryTime((card.timestamp - refStart) / 1000L),
            isNewest = newest != null && card === newest,
            playerCards = playerCards,
            showSpecIcons = showSpecIcons,
            expanded = true,
            onToggleExpanded = { toggleExpanded(card.timestamp) },
            onPlayerClick = onPlayerClick,
            modifier = Modifier.fillMaxWidth()
          )
        } else {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val columns = List(columnCount) { mutableListOf<BuffHistorySnapshotCard>() }
            segment.forEachIndexed { index, card -> columns[index % columnCount].add(card) }
            columns.forEach { column ->
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                column.forEach { card ->
                  BuffHistorySnapshotCardView(
                    card = card,
                    timeLabel = formatBuffHistoryTime((card.timestamp - refStart) / 1000L),
                    isNewest = newest != null && card === newest,
                    playerCards = playerCards,
                    showSpecIcons = showSpecIcons,
                    expanded = card.timestamp in expandedIds,
                    onToggleExpanded = { toggleExpanded(card.timestamp) },
                    onPlayerClick = onPlayerClick,
                    modifier = Modifier.fillMaxWidth()
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * One snapshot of the roster: the timestamp heading, the compact buff counts, the buffed-share
 * badge and the aligned player rows - one cohesive surface rather than a card per player. Cards
 * start collapsed to their summary; [onToggleExpanded] flips the roster grid, which the list
 * lays out full-width so every member stays visible at once.
 */
@Composable
fun BuffHistorySnapshotCardView(
  card: BuffHistorySnapshotCard,
  timeLabel: String,
  isNewest: Boolean,
  playerCards: Map<String, PlayerCard>,
  showSpecIcons: Boolean,
  onPlayerClick: (BuffHistoryPlayerDetail) -> Unit,
  expanded: Boolean,
  onToggleExpanded: () -> Unit,
  modifier: Modifier = Modifier
) {
  val buffedCount = card.playerNameToStatus.values.count { it == BuffHistoryPlayerStatus.BUFFED }
  val notBuffedCount = card.playerNameToStatus.values.count { it == BuffHistoryPlayerStatus.NOT_BUFFED }
  val notScannableCount = card.playerNameToStatus.values.count { it == BuffHistoryPlayerStatus.NOT_SCANNABLE }
  val totalScanned = buffedCount + notBuffedCount
  val uptimePercent = if (totalScanned > 0) (buffedCount * 100f / totalScanned) else 0f

  val interaction = remember { MutableInteractionSource() }
  val hovered by interaction.collectIsHoveredAsState()

  Surface(
    color = RFColors.buffHistoryCard,
    shape = RoundedCornerShape(5.dp),
    border = BorderStroke(
      1.dp,
      if (isNewest) RFColors.buffHistoryBuffed.copy(alpha = 0.28f) else RFColors.buffHistoryHairline
    ),
    modifier = modifier
  ) {
    Column(modifier = Modifier.animateContentSize()) {
      // Header: timestamp as the heading, buff counts beneath it, buffed share at the right.
      // Clicking anywhere on it collapses or restores the roster grid below.
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(if (hovered) Color.White.copy(alpha = 0.06f) else RFColors.buffHistoryHeaderTint)
          .hoverable(interaction)
          .clickable(interactionSource = interaction, indication = null) { onToggleExpanded() }
          .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 7.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.Bottom
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(Res.string.raid_buff_history_snapshot, "").trim().uppercase(),
              style = RFTypography.Kicker,
              color = RFColors.TextTertiary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Row(
              modifier = Modifier.padding(top = 3.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = timeLabel,
                style = RFTypography.Headline,
                color = RFColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (isNewest) {
                BuffHistoryMostRecentChip()
              }
            }
          }
          BuffHistoryShareBadge(uptimePercent)
          val chevronRotation by animateFloatAsState(if (expanded) 0f else -90f)
          FaIcon(
            codepoint = ChevronsGlyph,
            sizeSp = 8,
            color = if (hovered) RFColors.TextSecondary else RFColors.TextTertiary,
            modifier = Modifier.rotate(chevronRotation)
          )
        }
        FlowRow(
          modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.Center
        ) {
          BuffHistoryStat(
            color = RFColors.buffHistoryUptime,
            value = buffedCount,
            label = stringResource(Res.string.raid_buff_history_buffed)
          )
          BuffHistoryStat(
            color = RFColors.buffHistoryUnbuffed,
            value = notBuffedCount,
            label = stringResource(Res.string.raid_buff_history_unbuffed)
          )
          BuffHistoryStat(
            color = RFColors.buffHistoryNotScannable,
            value = notScannableCount,
            label = stringResource(Res.string.raid_buff_history_not_scannable)
          )
          BuffHistoryTotalStat(
            value = card.playerNameToStatus.size,
            label = stringResource(Res.string.raid_buff_history_in_raid)
          )
        }
      }
      Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RFColors.buffHistoryHairline))

      // Roster: aligned columns and fixed-height rows instead of a card per player. The
      // roster is whatever the card's bucket recorded, so departed players never linger here.
      if (expanded) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
          val columns = (maxWidth.value / 155f).toInt().coerceIn(2, 8)
          val players = card.playerNameToStatus.keys.sorted()
          Column(modifier = Modifier.fillMaxWidth()) {
            players.chunked(columns).forEachIndexed { rowIndex, rowPlayers ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(20.dp)
                  .background(if (rowIndex % 2 == 1) RFColors.buffHistoryRowTint else Color.Transparent),
                verticalAlignment = Alignment.CenterVertically
              ) {
                rowPlayers.forEachIndexed { columnIndex, playerName ->
                  val status = card.playerNameToStatus[playerName] ?: BuffHistoryPlayerStatus.NOT_SCANNABLE
                  BuffHistoryPlayerCell(
                    playerName = playerName,
                    status = status,
                    playerCard = playerCards[playerName],
                    showSpecIcons = showSpecIcons,
                    showColumnDivider = columnIndex > 0,
                    onClick = { anchor ->
                      onPlayerClick(
                        BuffHistoryPlayerDetail(
                          playerName = playerName,
                          status = status,
                          buffIds = card.playerNameToBuffIds[playerName].orEmpty(),
                          timeLabel = timeLabel,
                          anchor = anchor
                        )
                      )
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                  )
                }
                // Pad out the last row so its columns line up with the rows above
                repeat(columns - rowPlayers.size) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }
    }
  }
}

/** Green mini-metric with the snapshot's buffed share: the value over a small uppercase label. */
@Composable
private fun BuffHistoryShareBadge(uptimePercent: Float) {
  Column(
    modifier = Modifier
      .background(RFColors.buffHistoryUptime.copy(alpha = 0.09f), RoundedCornerShape(4.dp))
      .border(1.dp, RFColors.buffHistoryUptime.copy(alpha = 0.22f), RoundedCornerShape(4.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.End
  ) {
    Text(
      text = String.format(stringResource(Res.string.raid_buff_history_buffed_share_value), uptimePercent),
      style = RFTypography.Metric.copy(fontSize = 13.sp, lineHeight = 15.sp),
      color = RFColors.buffHistoryUptime,
      maxLines = 1
    )
    Text(
      text = stringResource(Res.string.raid_buff_history_buffed).uppercase(),
      style = RFTypography.Micro,
      letterSpacing = 1.sp,
      color = RFColors.buffHistoryUptime.copy(alpha = 0.62f),
      maxLines = 1
    )
  }
}

/** Cyan chip marking the newest snapshot in the history. */
@Composable
private fun BuffHistoryMostRecentChip() {
  Text(
    text = stringResource(Res.string.raid_buff_history_most_recent).uppercase(),
    style = RFTypography.Micro,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.8.sp,
    color = RFColors.buffHistoryBuffed.copy(alpha = 0.90f),
    maxLines = 1,
    modifier = Modifier
      .background(RFColors.buffHistoryBuffed.copy(alpha = 0.10f), RoundedCornerShape(3.dp))
      .padding(horizontal = 6.dp, vertical = 3.dp)
  )
}

/** Compact count + label pair used for the secondary buff statistics in a snapshot header. */
@Composable
fun BuffHistoryStat(color: Color, value: Int, label: String) {
  Row(
    modifier = Modifier.height(14.dp),
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // The swatch sits slightly high against the text's visual midline; nudge it onto the glyph line.
    Box(
      modifier = Modifier
        .offset(y = 1.dp)
        .size(width = 8.dp, height = 3.dp)
        .background(color, RoundedCornerShape(1.5.dp))
    )
    Text(
      text = "$value",
      style = RFTypography.Meta,
      fontWeight = FontWeight.SemiBold,
      color = color,
      maxLines = 1
    )
    Text(
      text = label,
      style = RFTypography.Meta,
      color = RFColors.TextTertiary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/** Neutral roster total for a snapshot: unswatched so it reads as a sum, not a status. */
@Composable
private fun BuffHistoryTotalStat(value: Int, label: String) {
  Row(
    modifier = Modifier.height(14.dp),
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "$value",
      style = RFTypography.Meta,
      fontWeight = FontWeight.SemiBold,
      color = RFColors.TextSecondary,
      maxLines = 1
    )
    Text(
      text = label,
      style = RFTypography.Meta,
      color = RFColors.TextTertiary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/**
 * One player line inside a snapshot roster: fixed-width status mark, spec icons, name (coloured by
 * buff state, as before) and the faction dot pinned to the right edge so columns stay aligned.
 */
@Composable
fun BuffHistoryPlayerCell(
  playerName: String,
  status: BuffHistoryPlayerStatus,
  playerCard: PlayerCard?,
  showSpecIcons: Boolean,
  showColumnDivider: Boolean,
  onClick: (IntOffset) -> Unit,
  modifier: Modifier = Modifier
) {
  val spec = remember(playerCard?.currentBuild) { playerCard?.currentBuild?.let { SpecType.fromName(it) } }
  val statusColor = when (status) {
    BuffHistoryPlayerStatus.BUFFED -> RFColors.buffHistoryUptime
    BuffHistoryPlayerStatus.NOT_BUFFED -> RFColors.buffHistoryUnbuffed
    BuffHistoryPlayerStatus.NOT_SCANNABLE -> RFColors.TextTertiary
  }
  val statusMark = when (status) {
    BuffHistoryPlayerStatus.BUFFED -> "✓"
    BuffHistoryPlayerStatus.NOT_BUFFED -> "✗"
    BuffHistoryPlayerStatus.NOT_SCANNABLE -> "?"
  }
  val faction = playerCard?.let { Faction.fromString(it.lastKnownFaction) } ?: Faction.UNKNOWN
  var anchor by remember { mutableStateOf(IntOffset.Zero) }
  val interactionSource = remember { MutableInteractionSource() }
  val hovered by interactionSource.collectIsHoveredAsState()

  Row(
    modifier = modifier
      .onGloballyPositioned { coords ->
        val pos = coords.positionInWindow()
        anchor = IntOffset((pos.x + coords.size.width).roundToInt(), pos.y.roundToInt())
      }
      .hoverable(interactionSource)
      .clickable { onClick(anchor) }
      .background(if (hovered) Color.White.copy(alpha = 0.07f) else Color.Transparent, RoundedCornerShape(3.dp))
      .then(
        if (showColumnDivider) {
          Modifier.drawBehind {
            val stroke = 1.dp.toPx()
            val x = stroke / 2f
            drawLine(
              color = RFColors.buffHistoryHairline,
              start = Offset(x, 0f),
              end = Offset(x, size.height),
              strokeWidth = stroke
            )
          }
        } else {
          Modifier
        }
      )
      .padding(horizontal = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(modifier = Modifier.width(10.dp), contentAlignment = Alignment.Center) {
      Text(
        text = statusMark,
        color = statusColor,
        fontSize = 10.sp,
        lineHeight = 10.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
    if (showSpecIcons) {
      // Fixed-width slot so player names stay column-aligned whatever a spec's tree count is.
      Box(
        modifier = Modifier.width(SpecIconSlotWidth),
        contentAlignment = Alignment.CenterStart
      ) {
        if (spec != null && spec != SpecType.UNKNOWN) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            spec.trees.sortedByDisplayOrder().forEach { tree ->
              Image(
                painter = skillTreeIconPainterFor(tree),
                contentDescription = null,
                modifier = Modifier.size(11.dp)
              )
            }
          }
        }
      }
    }
    Text(
      text = playerName,
      color = statusColor,
      style = RFTypography.Body,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
    Box(
      modifier = Modifier
        .size(4.dp)
        .background(factionHighlightColor(faction), RoundedCornerShape(2.dp))
    )
  }
}
