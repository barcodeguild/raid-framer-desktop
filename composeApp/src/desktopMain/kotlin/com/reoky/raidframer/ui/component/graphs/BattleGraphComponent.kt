package com.reoky.raidframer.ui.component.graphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.sortedByDisplayOrder
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.skillTreeIconPainterFor
import com.reoky.raidframer.core.interactor.BattleGraphData
import com.reoky.raidframer.core.interactor.BattleGraphMode
import com.reoky.raidframer.core.interactor.GraphNode
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.ui.LocalDragLock
import kotlin.math.sqrt

@Composable
fun BattleGraphComponent(
  graphData: BattleGraphData,
  mode: BattleGraphMode,
  onOpenPlayerCard: (String) -> Unit,
  onFilterByName: (String) -> Unit,
  onFilterBySpec: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var scale by remember { mutableFloatStateOf(1f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }

  val nodes = graphData.nodes
  val edges = graphData.edges

  val textMeasurer = rememberTextMeasurer()
  val dragLock = LocalDragLock.current
  val density = LocalDensity.current.density

  // Right-click context menu state
  var contextMenuNode by remember { mutableStateOf<GraphNode?>(null) }
  var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }

  // Left-click selection state
  var selectedNode by remember { mutableStateOf<GraphNode?>(null) }

  // Animated node positions - initialized once, preserved across updates
  var initialized by remember { mutableStateOf(false) }
  val animatedX = remember { mutableStateListOf<Float>() }
  val animatedY = remember { mutableStateListOf<Float>() }

  // Sync list size with node count without overwriting existing positions
  LaunchedEffect(nodes.size) {
    while (animatedX.size < nodes.size) {
      val idx = animatedX.size
      animatedX.add(nodes[idx].x)
      animatedY.add(nodes[idx].y)
    }
    while (animatedX.size > nodes.size) {
      animatedX.removeAt(animatedX.lastIndex)
      animatedY.removeAt(animatedY.lastIndex)
    }
    if (!initialized && nodes.isNotEmpty()) {
      initialized = true
    }
  }

  // Run force-directed layout in background, animating toward targets
  LaunchedEffect(graphData) {
    if (nodes.size <= 1) {
      return@LaunchedEffect
    }

    val simNodes = nodes.mapIndexed { i, node ->
      val x = if (i < animatedX.size) animatedX[i] else node.x
      val y = if (i < animatedY.size) animatedY[i] else node.y
      SimNode(x, y)
    }

    val iterations = 200
    val repulsionForce = 20000f
    val attractionForce = 0.005f
    val damping = 0.85f
    val centerGravity = 0.002f
    val lerpFactor = 0.15f
    val iterationsPerFrame = 4

    repeat(iterations) { iter ->
      simNodes.forEach { it.vx = 0f; it.vy = 0f }

      for (i in simNodes.indices) {
        for (j in i + 1 until simNodes.size) {
          val dx = simNodes[j].x - simNodes[i].x
          val dy = simNodes[j].y - simNodes[i].y
          val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
          val force = repulsionForce / (dist * dist)
          val fx = (dx / dist) * force
          val fy = (dy / dist) * force
          simNodes[i].vx -= fx
          simNodes[i].vy -= fy
          simNodes[j].vx += fx
          simNodes[j].vy += fy
        }
      }

      edges.forEach { edge ->
        val srcIdx = nodes.indexOfFirst { it.name == edge.source }
        val tgtIdx = nodes.indexOfFirst { it.name == edge.target }
        if (srcIdx >= 0 && tgtIdx >= 0) {
          val src = simNodes[srcIdx]
          val tgt = simNodes[tgtIdx]
          val dx = tgt.x - src.x
          val dy = tgt.y - src.y
          val dist = sqrt(dx * dx + dy * dy)
          val force = attractionForce * dist * edge.normalizedWeight
          val fx = (dx / dist.coerceAtLeast(1f)) * force
          val fy = (dy / dist.coerceAtLeast(1f)) * force
          src.vx += fx
          src.vy += fy
          tgt.vx -= fx
          tgt.vy -= fy
        }
      }

      simNodes.forEach { node ->
        node.vx -= node.x * centerGravity
        node.vy -= node.y * centerGravity
      }

      simNodes.forEach { node ->
        node.vx *= damping
        node.vy *= damping
        node.x += node.vx
        node.y += node.vy
      }

      for (i in animatedX.indices) {
        if (i < simNodes.size) {
          animatedX[i] = animatedX[i] + (simNodes[i].x - animatedX[i]) * lerpFactor
          animatedY[i] = animatedY[i] + (simNodes[i].y - animatedY[i]) * lerpFactor
        }
      }

      if (iter % iterationsPerFrame == 0) {
        withFrameNanos { }
      }
    }
  }

  val edgeColor = when (mode) {
    BattleGraphMode.DAMAGE -> RFColors.dpsOrange
    BattleGraphMode.HEALS -> RFColors.healsGreen
    BattleGraphMode.CC -> RFColors.ccCyan
  }

  BoxWithConstraints(
    modifier = modifier
      .pointerInput(Unit) {
        awaitPointerEventScope {
          var lastPanX = 0f
          var lastPanY = 0f
          var isPanning = false
          while (true) {
            val event = awaitPointerEvent()
            // Skip events already consumed by child composables (e.g. TextField, Slider)
            if (event.changes.any { it.isConsumed }) {
              if (isPanning) {
                isPanning = false
                dragLock.value = false
              }
              continue
            }
            when (event.type) {
              PointerEventType.Scroll -> {
                val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                if (scrollDelta != 0f) {
                  scale = (scale * (1f - scrollDelta * 0.1f)).coerceIn(0.25f, 4f)
                  event.changes.forEach { it.consume() }
                }
              }
              PointerEventType.Move -> {
                val pressedChange = event.changes.firstOrNull { it.pressed }
                if (pressedChange != null && event.changes.size == 1) {
                  if (!isPanning) {
                    lastPanX = pressedChange.position.x
                    lastPanY = pressedChange.position.y
                    isPanning = true
                    dragLock.value = true
                  } else {
                    panOffset += Offset(
                      pressedChange.position.x - lastPanX,
                      pressedChange.position.y - lastPanY
                    )
                    lastPanX = pressedChange.position.x
                    lastPanY = pressedChange.position.y
                  }
                  pressedChange.consume()
                }
              }
              PointerEventType.Release -> {
                if (isPanning) {
                  isPanning = false
                  dragLock.value = false
                }
              }
            }
          }
        }
      }
  ) {
    val parentWidth = constraints.maxWidth.toFloat()
    val parentHeight = constraints.maxHeight.toFloat()
    val centerX = parentWidth / 2f
    val centerY = parentHeight / 2f

    key(edges.size, edges.map { it.source to it.target }.hashCode()) {
    Canvas(
      modifier = Modifier.fillMaxSize()
    ) {
      val nodeRadiusPx = 50.dp.toPx()
      val scaledRadius = nodeRadiusPx * scale
      val arrowLen = 10f * scale
      val arrowWidth = 5f * scale

      edges.forEach { edge ->
        val srcIdx = nodes.indexOfFirst { it.name == edge.source }
        val tgtIdx = nodes.indexOfFirst { it.name == edge.target }
        if (srcIdx >= 0 && tgtIdx >= 0 && srcIdx < animatedX.size && tgtIdx < animatedX.size) {
          val srcCx = animatedX[srcIdx] * scale + panOffset.x + centerX
          val srcCy = animatedY[srcIdx] * scale + panOffset.y + centerY
          val tgtCx = animatedX[tgtIdx] * scale + panOffset.x + centerX
          val tgtCy = animatedY[tgtIdx] * scale + panOffset.y + centerY

          val dx = tgtCx - srcCx
          val dy = tgtCy - srcCy
          val dist = sqrt(dx * dx + dy * dy)

          if (dist <= scaledRadius * 2f) return@forEach

          val nx = dx / dist
          val ny = dy / dist

          // Start/end from circle perimeters with small gap
          val edgeMargin = 6f * scale
          val startX = srcCx + nx * (scaledRadius + edgeMargin)
          val startY = srcCy + ny * (scaledRadius + edgeMargin)
          val endX = tgtCx - nx * (scaledRadius + edgeMargin)
          val endY = tgtCy - ny * (scaledRadius + edgeMargin)

          val strokeWidth = (1f + edge.normalizedWeight * 7f) * scale
          val alpha = 0.3f + edge.normalizedWeight * 0.5f
          val color = edgeColor.copy(alpha = alpha)

          // Bezier curve: control point offset perpendicular to midpoint
          val midX = (startX + endX) / 2f
          val midY = (startY + endY) / 2f
          val perpX = -ny
          val perpY = nx
          val curvature = (dist * 0.12f).coerceIn(8f, 60f) * scale
          val ctrlX = midX + perpX * curvature
          val ctrlY = midY + perpY * curvature

          // Compute arrow geometry first to know where line should stop
          val tangentDx = endX - ctrlX
          val tangentDy = endY - ctrlY
          val tangentLen = sqrt(tangentDx * tangentDx + tangentDy * tangentDy)
          val arrowBaseX: Float
          val arrowBaseY: Float
          if (tangentLen > 0.1f) {
            val tnx = tangentDx / tangentLen
            val tny = tangentDy / tangentLen
            // Arrow tip at perimeter, base pulled back by arrowLen
            arrowBaseX = endX - tnx * arrowLen
            arrowBaseY = endY - tny * arrowLen
          } else {
            arrowBaseX = endX
            arrowBaseY = endY
          }

          // Draw curved edge to arrow BASE (not tip) so line doesn't overlap arrow
          val steps = 20
          var prevX = startX
          var prevY = startY
          for (s in 1..steps) {
            val t = s.toFloat() / steps
            val invT = 1f - t
            val px = invT * invT * startX + 2f * invT * t * ctrlX + t * t * arrowBaseX
            val py = invT * invT * startY + 2f * invT * t * ctrlY + t * t * arrowBaseY
            drawLine(color, Offset(prevX, prevY), Offset(px, py), strokeWidth)
            prevX = px
            prevY = py
          }

          // Arrowhead at the end (tip at perimeter, base at arrowBase)
          if (tangentLen > 0.1f) {
            val tnx = tangentDx / tangentLen
            val tny = tangentDy / tangentLen
            val arrowLeftX = arrowBaseX + tny * arrowWidth
            val arrowLeftY = arrowBaseY - tnx * arrowWidth
            val arrowRightX = arrowBaseX - tny * arrowWidth
            val arrowRightY = arrowBaseY + tnx * arrowWidth

            val path = androidx.compose.ui.graphics.Path().apply {
              moveTo(endX, endY)
              lineTo(arrowLeftX, arrowLeftY)
              lineTo(arrowRightX, arrowRightY)
              close()
            }
            drawPath(path, color)
          }

          // Edge label at curve midpoint (t=0.5 on bezier)
          val labelX = 0.25f * startX + 0.5f * ctrlX + 0.25f * arrowBaseX
          val labelY = 0.25f * startY + 0.5f * ctrlY + 0.25f * arrowBaseY
          drawText(
            textMeasurer = textMeasurer,
            text = edge.displayValue,
            topLeft = Offset(labelX - 20f * scale, labelY - 14f * scale),
            style = TextStyle(
              fontSize = (10f * scale).sp,
              color = color
            )
          )
        }
      }
    }
    } // key

    // Nodes overlay with right-click support
    nodes.forEachIndexed { idx, node ->
      if (idx < animatedX.size) {
        val nodeCx = animatedX[idx] * scale + panOffset.x + centerX
        val nodeCy = animatedY[idx] * scale + panOffset.y + centerY

        NodeComponent(
          node = node,
          mode = mode,
          isSelected = selectedNode?.name == node.name,
          modifier = Modifier
            .graphicsLayer {
              translationX = nodeCx - 50.dp.toPx() * scale
              translationY = nodeCy - 50.dp.toPx() * scale
              scaleX = scale
              scaleY = scale
            }
            .pointerInput(node) {
              awaitPointerEventScope {
                while (true) {
                  val event = awaitPointerEvent()
                  if (event.type == PointerEventType.Press) {
                    val change = event.changes.firstOrNull()
                    if (change != null) {
                      // Left-click: open context menu
                      contextMenuNode = node
                      contextMenuPosition = Offset(nodeCx, nodeCy)
                      selectedNode = node
                    }
                  }
                }
              }
            }
        )
      }
    }

    // Context menu
    contextMenuNode?.let { node ->
      DropdownMenu(
        expanded = true,
        onDismissRequest = { contextMenuNode = null },
        offset = DpOffset((contextMenuPosition.x / density).dp, (contextMenuPosition.y / density).dp)
      ) {
        DropdownMenuItem(onClick = {
          onOpenPlayerCard(node.name)
          contextMenuNode = null
        }) {
          Text("Open Player Card", fontSize = 12.sp)
        }
        DropdownMenuItem(onClick = {
          onFilterByName(node.name)
          contextMenuNode = null
        }) {
          Text("Filter by Name", fontSize = 12.sp)
        }
        DropdownMenuItem(onClick = {
          node.spec?.let { spec -> onFilterBySpec(spec.name) }
          contextMenuNode = null
        }) {
          Text("Filter by Spec", fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun NodeComponent(
  node: GraphNode,
  mode: BattleGraphMode,
  isSelected: Boolean = false,
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
      .size(100.dp)
      .clip(CircleShape)
      .drawBehind {
        drawCircle(
          color = factionColor.copy(alpha = 0.3f),
          radius = size.minDimension / 2f
        )
        // Highlight border for selected node
        if (isSelected) {
          drawCircle(
            color = RFColors.TextPrimary.copy(alpha = 0.8f),
            radius = size.minDimension / 2f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
          )
        }
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
    Text(
      text = node.name,
      color = RFColors.TextPrimary,
      fontSize = 10.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth()
    )

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

private data class SimNode(
  var x: Float,
  var y: Float,
  var vx: Float = 0f,
  var vy: Float = 0f
)
