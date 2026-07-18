package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.sortedByDisplayOrder
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.PlayerCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.sqrt

enum class BattleGraphMode { DAMAGE, HEALS, CC }

data class GraphNode(
  val name: String,
  val spec: SpecType?,
  val gearScore: Int,
  val faction: Faction,
  var x: Float = 0f,
  var y: Float = 0f,
  var vx: Float = 0f,
  var vy: Float = 0f
)

data class GraphEdge(
  val source: String,
  val target: String,
  val weight: Long,
  val normalizedWeight: Float = 0f,
  val displayValue: String = ""
)

data class BattleGraphData(
  val nodes: List<GraphNode> = emptyList(),
  val edges: List<GraphEdge> = emptyList(),
  val maxValue: Long = 0L
)

object BattleGraphInteractor : Interactor() {

  private val _graphData = MutableStateFlow(BattleGraphData())
  val graphData: StateFlow<BattleGraphData> = _graphData.asStateFlow()

  private val _selectedMode = MutableStateFlow(BattleGraphMode.DAMAGE)
  val selectedMode: StateFlow<BattleGraphMode> = _selectedMode.asStateFlow()

  private var damageThresholdMin = 1000L
  private var damageThresholdMax = 10_000_000L
  private var healThresholdMin = 1000L
  private var healThresholdMax = 10_000_000L
  private var ccThresholdMin = 0
  private var ccThresholdMax = 5000

  override suspend fun interact() {
    combine(
      PlayerCacheInteractor.topDamage,
      _selectedMode
    ) { cards, mode ->
      cards to mode
    }.collect { (cards, mode) ->
      rebuildGraph(cards, mode)
    }
  }

  fun setMode(mode: BattleGraphMode) {
    _selectedMode.value = mode
  }

  fun setDamageThreshold(min: Long, max: Long) {
    damageThresholdMin = min
    damageThresholdMax = max
    rebuildGraphFromCurrentState()
  }

  fun setHealThreshold(min: Long, max: Long) {
    healThresholdMin = min
    healThresholdMax = max
    rebuildGraphFromCurrentState()
  }

  fun setCCThreshold(min: Int, max: Int) {
    ccThresholdMin = min
    ccThresholdMax = max
    rebuildGraphFromCurrentState()
  }

  private fun rebuildGraphFromCurrentState() {
    scope.launch {
      val cards = PlayerCacheInteractor.topDamage.value
      val mode = _selectedMode.value
      rebuildGraph(cards, mode)
    }
  }

  private fun rebuildGraph(cards: List<PlayerCard>, mode: BattleGraphMode) {
    val allowPvE = RFConfig.state.value.allowPVEDamage
    val filteredCards = cards.filter { it.isRealPlayer || allowPvE }
    if (filteredCards.isEmpty()) {
      _graphData.value = BattleGraphData()
      return
    }

    val nodeMap = mutableMapOf<String, GraphNode>()
    val edges = mutableListOf<GraphEdge>()

    filteredCards.forEach { card ->
      val spec = SpecType.fromName(card.currentBuild)
      val faction = Faction.fromString(card.lastKnownFaction)
      nodeMap[card.name] = GraphNode(
        name = card.name,
        spec = spec,
        gearScore = card.lastKnownGearScore,
        faction = faction
      )
    }

    when (mode) {
      BattleGraphMode.DAMAGE -> {
        filteredCards.forEach { sourceCard ->
          sourceCard.sessionDamageToPlayer.forEach { (targetName, damage) ->
            if (damage >= damageThresholdMin && damage <= damageThresholdMax) {
              val incoming = nodeMap[targetName]?.let {
                filteredCards.find { it.name == targetName }?.sessionDamageFromPlayer?.get(sourceCard.name) ?: 0L
              } ?: 0L
              edges.add(GraphEdge(
                source = sourceCard.name,
                target = targetName,
                weight = damage,
                displayValue = "${humanReadableShort(damage)} dmg"
              ))
            }
          }
        }
      }
      BattleGraphMode.HEALS -> {
        filteredCards.forEach { sourceCard ->
          sourceCard.sessionHealToPlayer.forEach { (targetName, heals) ->
            if (heals >= healThresholdMin && heals <= healThresholdMax) {
              edges.add(GraphEdge(
                source = sourceCard.name,
                target = targetName,
                weight = heals,
                displayValue = "${humanReadableShort(heals)} heal"
              ))
            }
          }
        }
      }
      BattleGraphMode.CC -> {
        filteredCards.forEach { sourceCard ->
          sourceCard.sessionCCToPlayer.forEach { (targetName, cc) ->
            if (cc >= ccThresholdMin && cc <= ccThresholdMax) {
              edges.add(GraphEdge(
                source = sourceCard.name,
                target = targetName,
                weight = cc.toLong(),
                displayValue = "$cc CC"
              ))
            }
          }
        }
      }
    }

    val maxValue = edges.maxOfOrNull { it.weight } ?: 1L
    val normalizedEdges = edges.map { edge ->
      val normalized = (edge.weight.toFloat() / maxValue).coerceIn(0.1f, 1f)
      edge.copy(normalizedWeight = normalized)
    }

    val activeNodeNames = normalizedEdges.flatMap { listOf(it.source, it.target) }.toSet()
    val activeNodes = nodeMap.values.filter { it.name in activeNodeNames }

    initializePositions(activeNodes)

    _graphData.value = BattleGraphData(
      nodes = activeNodes,
      edges = normalizedEdges,
      maxValue = maxValue
    )
  }

  private fun initializePositions(nodes: List<GraphNode>) {
    val count = nodes.size
    if (count == 0) return
    val radius = 150f * sqrt(count.toFloat())
    nodes.forEachIndexed { index, node ->
      val angle = 2.0 * Math.PI * index / count
      node.x = (radius * kotlin.math.cos(angle)).toFloat()
      node.y = (radius * kotlin.math.sin(angle)).toFloat()
      node.vx = 0f
      node.vy = 0f
    }
  }

  private fun humanReadableShort(value: Long): String {
    return when {
      value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
      value >= 1_000 -> String.format("%.1fk", value / 1_000.0)
      else -> value.toString()
    }
  }
}
