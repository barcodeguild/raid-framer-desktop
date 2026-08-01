package com.reoky.raidframer.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.definitions.SKILL_TREE_DISPLAY_ORDER
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.helpers.RFColors
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.resources.painterResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.*

data class FactionComposition(
  val factionLabel: String,
  val playerCount: Int,
  val treeCounts: Map<SkillTreeType, Int>,
  val color: Color
)

data class CompositionBreakdown(val label: String, val count: Int)

fun percentage(count: Int, total: Int): String = if (total == 0) "0%" else "${count * 100 / total}%"

@Composable
fun CompositionBreakdownList(
  title: String,
  total: Int,
  items: List<CompositionBreakdown>,
  modifier: Modifier = Modifier
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(title, color = RFColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    items.sortedByDescending { it.count }.forEach { item ->
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(item.label, color = RFColors.TextSecondary, fontSize = 11.sp, maxLines = 2, modifier = Modifier.weight(1f))
        Spacer(Modifier.size(8.dp))
        Text("${item.count}", color = RFColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("  ${percentage(item.count, total)}", color = RFColors.TextTertiary, fontSize = 11.sp)
      }
    }
  }
}

private fun SkillTreeType.compositionColor() = when (this) {
  SkillTreeType.ARCHERY -> RFColors.treeArchery
  SkillTreeType.AURAMANCY -> RFColors.treeAuramancy
  SkillTreeType.BATTLERAGE -> RFColors.treeBattlerage
  SkillTreeType.DEFENSE -> RFColors.treeDefense
  SkillTreeType.GUNSLINGER -> RFColors.treeGunslinger
  SkillTreeType.MALEDICTION -> RFColors.treeMalediction
  SkillTreeType.OCCULTISM -> RFColors.treeOccultism
  SkillTreeType.SHADOWPLAY -> RFColors.treeShadowplay
  SkillTreeType.SONGCRAFT -> RFColors.treeSongcraft
  SkillTreeType.SORCERY -> RFColors.treeSorcery
  SkillTreeType.SPELLDANCE -> RFColors.treeSpelldance
  SkillTreeType.SWIFTBLADE -> RFColors.treeSwiftblade
  SkillTreeType.VITALISM -> RFColors.treeVitalism
  SkillTreeType.WITCHCRAFT -> RFColors.treeWitchcraft
}

@Composable
fun CompositionChartComponent(
  composition: FactionComposition,
  treeLabels: Map<SkillTreeType, String>,
  modifier: Modifier = Modifier
) {
  val maxCount = composition.treeCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
  val axes = SKILL_TREE_DISPLAY_ORDER.size
  Column(
    modifier = modifier
      .background(RFColors.CardBackground.copy(alpha = 0.78f), RoundedCornerShape(14.dp))
      .border(1.dp, RFColors.CardBorder, RoundedCornerShape(14.dp))
      .padding(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(composition.factionLabel, color = composition.color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    Text("${composition.playerCount}", color = RFColors.TextSecondary, fontSize = 12.sp)
    BoxWithConstraints(Modifier.fillMaxWidth().height(290.dp)) {
      val density = LocalDensity.current
      val iconSize = 22.dp
      val chartHeightPx = with(density) { 290.dp.toPx() }
      val widthPx = with(density) { maxWidth.toPx() }
      val radius = (widthPx / 2f).coerceAtMost(chartHeightPx / 2f) - with(density) { 42.dp.toPx() }
      val centerX = widthPx / 2f
      val centerY = chartHeightPx / 2f
      fun vertex(index: Int): Offset {
        val angle = -Math.PI / 2 + (index * 2 * Math.PI / axes)
        return Offset(centerX + cos(angle).toFloat() * radius, centerY + sin(angle).toFloat() * radius)
      }
      Box(Modifier.fillMaxWidth().height(290.dp)) {
        Canvas(Modifier.fillMaxWidth().height(290.dp)) {
          val center = Offset(size.width / 2f, size.height / 2f)
          fun point(index: Int, scale: Float): Offset {
            val v = vertex(index)
            return Offset(center.x + (v.x - center.x) * scale, center.y + (v.y - center.y) * scale)
          }
          for (level in 1..4) {
            val ring = Path().apply {
              repeat(axes) { index -> if (index == 0) moveTo(point(index, level / 4f).x, point(index, level / 4f).y) else lineTo(point(index, level / 4f).x, point(index, level / 4f).y) }
              close()
            }
            drawPath(ring, RFColors.CardBorder.copy(alpha = 0.8f), style = Stroke(width = 1.dp.toPx()))
          }
          repeat(axes) { index -> drawLine(RFColors.CardBorder, center, point(index, 1f), strokeWidth = 1.dp.toPx()) }
          val data = Path().apply {
            SKILL_TREE_DISPLAY_ORDER.forEachIndexed { index, tree ->
              val scale = (composition.treeCounts[tree] ?: 0).toFloat() / maxCount
              val p = point(index, scale)
              if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
          }
          drawPath(data, composition.color.copy(alpha = 0.28f))
          drawPath(data, composition.color, style = Stroke(width = 2.dp.toPx()))
          repeat(axes) { index ->
            val tree = SKILL_TREE_DISPLAY_ORDER[index]
            val count = composition.treeCounts[tree] ?: 0
            val scale = count.toFloat() / maxCount
            val p = point(index, scale)
            drawCircle(composition.color, 2.dp.toPx(), p)
          }
          // Point labels are rendered by the overlay below; keep the graph itself uncluttered.
        }
        SKILL_TREE_DISPLAY_ORDER.forEachIndexed { index, tree ->
          val p = vertex(index)
          val count = composition.treeCounts[tree] ?: 0
          val percentageText = percentage(count, composition.playerCount)
          Image(
            painter = painterResource(tree.iconResource()),
            contentDescription = treeLabels[tree],
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .size(iconSize)
              .offset(
                with(density) { p.x.toDp() } - iconSize / 2,
                with(density) { p.y.toDp() } - iconSize / 2
              )
          )
          Text(
            text = percentageText,
            color = RFColors.TextTertiary.copy(alpha = 0.72f),
            fontSize = 8.sp,
            modifier = Modifier.offset(
              with(density) { p.x.toDp() } - 8.dp,
              with(density) { p.y.toDp() } + 12.dp
            )
          )
        }
      }
    }
     CompositionTreeBreakdown(composition, treeLabels)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompositionTreeBreakdown(composition: FactionComposition, treeLabels: Map<SkillTreeType, String>) {
  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Text("Skill trees", color = RFColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp), maxItemsInEachRow = 2) {
      SKILL_TREE_DISPLAY_ORDER.forEach { tree ->
        Row(Modifier.fillMaxWidth(0.48f).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
          Image(painterResource(tree.iconResource()), treeLabels[tree], modifier = Modifier.size(13.dp))
          Text(treeLabels[tree] ?: tree.name, color = tree.compositionColor(), fontSize = 10.sp, maxLines = 1, modifier = Modifier.weight(1f))
          Text("${composition.treeCounts[tree] ?: 0}", color = RFColors.TextTertiary, fontSize = 10.sp)
        }
      }
    }
  }
}

private fun SkillTreeType.iconResource() = when (this) {
  SkillTreeType.ARCHERY -> Res.drawable.archery
  SkillTreeType.AURAMANCY -> Res.drawable.auramancy
  SkillTreeType.BATTLERAGE -> Res.drawable.battlerage
  SkillTreeType.DEFENSE -> Res.drawable.defense
  SkillTreeType.GUNSLINGER -> Res.drawable.gunslinger
  SkillTreeType.MALEDICTION -> Res.drawable.malediction
  SkillTreeType.OCCULTISM -> Res.drawable.occultism
  SkillTreeType.SHADOWPLAY -> Res.drawable.shadowplay
  SkillTreeType.SONGCRAFT -> Res.drawable.songcraft
  SkillTreeType.SORCERY -> Res.drawable.sorcery
  SkillTreeType.SPELLDANCE -> Res.drawable.spelldance
  SkillTreeType.SWIFTBLADE -> Res.drawable.swiftblade
  SkillTreeType.VITALISM -> Res.drawable.vitalism
  SkillTreeType.WITCHCRAFT -> Res.drawable.witchcraft
}
