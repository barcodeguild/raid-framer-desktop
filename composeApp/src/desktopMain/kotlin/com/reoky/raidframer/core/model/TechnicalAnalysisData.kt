package com.reoky.raidframer.core.model

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.StringResource

data class TechAnalysisResult(
  val edgeHeuristics: List<EdgeHeuristic> = emptyList(),
  val nodeHeuristics: List<NodeHeuristic> = emptyList()
)

data class EdgeHeuristic(
  val source: String,
  val target: String,
  val labelRes: StringResource,
  val color: Color,
  val labelArgs: List<Any> = emptyList(),
  val isMvp: Boolean = false,
  val category: String = ""
)

data class NodeHeuristic(
  val nodeName: String,
  val labelRes: StringResource,
  val color: Color,
  val labelArgs: List<Any> = emptyList(),
  val isMvp: Boolean = false,
  val category: String = ""
)
