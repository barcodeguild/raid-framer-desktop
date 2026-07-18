package com.reoky.raidframer.ui.component.graphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.sortedByDisplayOrder
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.skillTreeIconPainterFor
import com.reoky.raidframer.core.interactor.BattleGraphData
import com.reoky.raidframer.core.interactor.BattleGraphMode
import com.reoky.raidframer.core.interactor.GraphEdge
import com.reoky.raidframer.core.model.Faction

@Composable
fun BattleGraphComponent(
  graphData: BattleGraphData,
  mode: BattleGraphMode,
  modifier: Modifier = Modifier
) {
  var scale by remember { mutableStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  val nodes = graphData.nodes
  val edges = graphData.edges

  val textMeasurer = rememberTextMeasurer()

  // Run force-directed layout
  val layoutNodes = remember(graphData) {
    val layoutNodes = nodes.map { node ->
      LayoutNode(
        name = node.name,
        spec = node.spec,
        gearScore = node.gearScore,
        faction = node.faction,
        x = node.x,
        y = node.y
      )
    }
    runForceDirectedLayout(layoutNodes, edges)
    layoutNodes
  }

  val edgeColor = when (mode) {
    BattleGraphMode.DAMAGE -> RFColors.dpsOrange
    BattleGraphMode.HEALS -> RFColors.healsGreen
    BattleGraphMode.CC -> RFColors.ccCyan
  }

  BoxWithConstraints(
    modifier = modifier
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
          scale = (scale * zoom).coerceIn(0.25f, 4f)
          offset += pan
        }
      }
  ) {
    val parentWidth = constraints.maxWidth.toFloat()
    val parentHeight = constraints.maxHeight.toFloat()
    val centerX = parentWidth / 2f
    val centerY = parentHeight / 2f

    // Canvas for edges
    Canvas(
      modifier = Modifier.fillMaxSize()
    ) {
      edges.forEach { edge ->
        val sourceNode = layoutNodes.find { it.name == edge.source }
        val targetNode = layoutNodes.find { it.name == edge.target }
        if (sourceNode != null && targetNode != null) {
          val startX = sourceNode.x * scale + offset.x + centerX
          val startY = sourceNode.y * scale + offset.y + centerY
          val endX = targetNode.x * scale + offset.x + centerX
          val endY = targetNode.y * scale + offset.y + centerY

          val strokeWidth = (1f + edge.normalizedWeight * 7f) * scale
          val alpha = 0.3f + edge.normalizedWeight * 0.5f

          drawLine(
            color = edgeColor.copy(alpha = alpha),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth
          )

          // Edge label at midpoint
          val midX = (startX + endX) / 2
          val midY = (startY + endY) / 2
          drawText(
            textMeasurer = textMeasurer,
            text = edge.displayValue,
            topLeft = Offset(midX - 20f * scale, midY - 14f * scale),
            style = TextStyle(
              fontSize = (10f * scale).sp,
              color = edgeColor.copy(alpha = alpha)
            )
          )
        }
      }
    }

    // Nodes overlay
    val density = LocalDensity.current.density
    layoutNodes.forEach { node ->
      val nodeX = node.x * scale + offset.x + centerX
      val nodeY = node.y * scale + offset.y + centerY

      NodeComponent(
        node = node,
        mode = mode,
        modifier = Modifier
          .offset(
            x = (nodeX / density - 50).dp,
            y = (nodeY / density - 50).dp
          )
          .graphicsLayer {
            scaleX = scale
            scaleY = scale
          }
      )
    }
  }
}

@Composable
private fun NodeComponent(
  node: LayoutNode,
  mode: BattleGraphMode,
  modifier: Modifier = Modifier
) {
  val factionColor = when (node.faction) {
    Faction.HARANYA -> RFColors.factionHaranya
    Faction.NUIA -> RFColors.factionNuia
    Faction.PIRATE -> RFColors.factionPirate
    else -> RFColors.TextSecondary
  }

  val spec = node.spec
  val trees: List<SkillTreeType> = spec?.trees?.sortedByDisplayOrder()?.take(3) ?: emptyList()

  Column(
    modifier = modifier
      .width(100.dp)
      .clip(CircleShape)
      .background(Color.Black.copy(alpha = 0.75f))
      .drawBehind {
        drawCircle(
          color = factionColor.copy(alpha = 0.3f),
          radius = size.minDimension / 2f
        )
        drawCircle(
          color = factionColor.copy(alpha = 0.6f),
          radius = size.minDimension / 2f,
          style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
      }
      .padding(6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // Character name
    Text(
      text = node.name,
      color = RFColors.TextPrimary,
      fontSize = 10.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth()
    )

    // Skill tree icons
    if (trees.isNotEmpty()) {
      Row(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center
      ) {
        trees.forEach { tree ->
          SkillTreeIcon(tree = tree)
        }
      }
    }

    // Spec name
    val specDisplayName = spec?.let {
      it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() }
    } ?: "Unknown"
    Text(
      text = specDisplayName,
      color = RFColors.TextSecondary,
      fontSize = 8.sp,
      maxLines = 1,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth()
    )

    // Gear score
    if (node.gearScore > 0) {
      Text(
        text = String.format("%,d", node.gearScore),
        color = RFColors.TextTertiary,
        fontSize = 8.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@Composable
private fun SkillTreeIcon(tree: SkillTreeType) {
  Image(
    painter = skillTreeIconPainterFor(tree),
    contentDescription = tree.name,
    modifier = Modifier
      .size(14.dp)
      .padding(horizontal = 1.dp)
  )
}

private data class LayoutNode(
  val name: String,
  val spec: SpecType?,
  val gearScore: Int,
  val faction: Faction,
  var x: Float,
  var y: Float,
  var vx: Float = 0f,
  var vy: Float = 0f
)

private fun runForceDirectedLayout(nodes: List<LayoutNode>, edges: List<GraphEdge>) {
  if (nodes.size <= 1) return

  val iterations = 200
  val repulsionForce = 5000f
  val attractionForce = 0.01f
  val damping = 0.9f
  val centerGravity = 0.005f

  repeat(iterations) {
    // Reset forces
    nodes.forEach { it.vx = 0f; it.vy = 0f }

    // Repulsion between all pairs
    for (i in nodes.indices) {
      for (j in i + 1 until nodes.size) {
        val dx = nodes[j].x - nodes[i].x
        val dy = nodes[j].y - nodes[i].y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val force = repulsionForce / (dist * dist)
        val fx = (dx / dist) * force
        val fy = (dy / dist) * force
        nodes[i].vx -= fx
        nodes[i].vy -= fy
        nodes[j].vx += fx
        nodes[j].vy += fy
      }
    }

    // Attraction along edges
    edges.forEach { edge ->
      val source = nodes.find { it.name == edge.source }
      val target = nodes.find { it.name == edge.target }
      if (source != null && target != null) {
        val dx = target.x - source.x
        val dy = target.y - source.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val force = attractionForce * dist * edge.normalizedWeight
        val fx = (dx / dist.coerceAtLeast(1f)) * force
        val fy = (dy / dist.coerceAtLeast(1f)) * force
        source.vx += fx
        source.vy += fy
        target.vx -= fx
        target.vy -= fy
      }
    }

    // Center gravity
    nodes.forEach { node ->
      node.vx -= node.x * centerGravity
      node.vy -= node.y * centerGravity
    }

    // Apply velocities with damping
    nodes.forEach { node ->
      node.vx *= damping
      node.vy *= damping
      node.x += node.vx
      node.y += node.vy
    }
  }
}
