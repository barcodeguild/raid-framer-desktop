package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.helpers.TechnicalAnalysisHelper
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.TechAnalysisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random

enum class BattleGraphMode { DAMAGE, HEALS, CC, KILLS, BUFFS, DEBUFFS, CHARMS, DISTRESS, SILENCE }

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
  val displayValue: String = "",
  val spellBreakdown: Map<String, Long> = emptyMap()  // spell/debuff name -> value for hover tooltip
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

  private val _isPaused = MutableStateFlow(false)
  val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

  private val _showTechAnalysis = MutableStateFlow(false)
  val showTechAnalysis: StateFlow<Boolean> = _showTechAnalysis.asStateFlow()

  private val _showMvpOnly = MutableStateFlow(false)
  val showMvpOnly: StateFlow<Boolean> = _showMvpOnly.asStateFlow()

  private val _techAnalysisResult = MutableStateFlow(TechAnalysisResult())
  val techAnalysisResult: StateFlow<TechAnalysisResult> = _techAnalysisResult.asStateFlow()

  private var damageThresholdMin = 1000L
  private var healThresholdMin = 1000L
  private var ccThresholdMin = 0
  private var killThresholdMin = 1
  private var buffThresholdMin = 1
  private var debuffThresholdMin = 1
  private var charmThresholdMin = 1
  private var distressThresholdMin = 1
  private var silenceThresholdMin = 1
  private var searchQuery = ""
  private var maxEdges = 50
  private var selectedBuffSpell: String? = null
  private var selectedDebuffSpell: String? = null
  private var localizedSkillTreeNames: Map<SkillTreeType, String> = emptyMap()

  private var lastRebuildTime = 0L
  private val rebuildThrottleMs = 3_000L

  override suspend fun interact() {
    combine(
      PlayerCacheInteractor.topDamage,
      PlayerCacheInteractor.topHeals,
      PlayerCacheInteractor.topCC,
      _selectedMode
    ) { damages, heals, cc, mode ->
      // Merge in kills, buffs, debuffs from their current values
      val kills = PlayerCacheInteractor.topKills.value
      val buffs = PlayerCacheInteractor.topBuffs.value
      val debuffs = PlayerCacheInteractor.topDebuff.value
      val merged = (damages + heals + cc + kills + buffs + debuffs).distinctBy { it.name }
      merged to mode
    }.collect { (cards, mode) ->
      if (_isPaused.value) return@collect
      val now = System.currentTimeMillis()
      if (now - lastRebuildTime >= rebuildThrottleMs) {
        lastRebuildTime = now
        rebuildGraph(cards, mode)
      }
    }
  }

  fun setMode(mode: BattleGraphMode) {
    _selectedMode.value = mode
    rebuildGraphFromCurrentState()
  }

  fun setDamageThreshold(min: Long) {
    damageThresholdMin = min
    rebuildGraphFromCurrentState()
  }

  fun setHealThreshold(min: Long) {
    healThresholdMin = min
    rebuildGraphFromCurrentState()
  }

  fun setCCThreshold(min: Int) {
    ccThresholdMin = min
    rebuildGraphFromCurrentState()
  }

  fun setKillThreshold(min: Int) {
    killThresholdMin = min
    rebuildGraphFromCurrentState()
  }

  fun setBuffThreshold(min: Int) {
    buffThresholdMin = min
    rebuildGraphFromCurrentState()
  }

  fun setDebuffThreshold(min: Int) {
    debuffThresholdMin = min
    rebuildGraphFromCurrentState()
  }

  fun setCharmThreshold(min: Int) {
    charmThresholdMin = min
    rebuildGraphFromCurrentState()
  }

  fun setDistressThreshold(min: Int) {
    distressThresholdMin = min
    rebuildGraphFromCurrentState()
  }

  fun setSilenceThreshold(min: Int) {
    silenceThresholdMin = min
    rebuildGraphFromCurrentState()
  }

  fun setSelectedBuffSpell(spell: String?) {
    selectedBuffSpell = spell
    rebuildGraphFromCurrentState()
  }

  fun setSelectedDebuffSpell(spell: String?) {
    selectedDebuffSpell = spell
    rebuildGraphFromCurrentState()
  }

  fun setSearchQuery(query: String) {
    searchQuery = query
    rebuildGraphFromCurrentState()
  }

  fun setLocalizedSkillTreeNames(names: Map<SkillTreeType, String>) {
    localizedSkillTreeNames = names
  }

  fun setMaxEdges(max: Int) {
    maxEdges = max
    rebuildGraphFromCurrentState()
  }

  fun togglePause() {
    _isPaused.value = !_isPaused.value
    if (!_isPaused.value) {
      // On resume, rebuild immediately with current data
      rebuildGraphFromCurrentState()
    }
  }

  fun setTechAnalysis(show: Boolean) {
    _showTechAnalysis.value = show
    rebuildGraphFromCurrentState()
  }

  fun setMvpOnly(show: Boolean) {
    _showMvpOnly.value = show
    rebuildGraphFromCurrentState()
  }

  private fun rebuildGraphFromCurrentState() {
    scope.launch {
      val damages = PlayerCacheInteractor.topDamage.value
      val heals = PlayerCacheInteractor.topHeals.value
      val cc = PlayerCacheInteractor.topCC.value
      val kills = PlayerCacheInteractor.topKills.value
      val buffs = PlayerCacheInteractor.topBuffs.value
      val debuffs = PlayerCacheInteractor.topDebuff.value
      val cards = (damages + heals + cc + kills + buffs + debuffs).distinctBy { it.name }
      val mode = _selectedMode.value
      rebuildGraph(cards, mode)
    }
  }

  private suspend fun rebuildGraph(cards: List<PlayerCard>, mode: BattleGraphMode) {
    val allowPvE = RFConfig.state.value.allowPVEDamage
    val queryLower = searchQuery.trim().lowercase()
    val filteredCards = cards.filter { card ->
      (card.isRealPlayer || allowPvE) && (queryLower.isEmpty() ||
        card.name.lowercase().contains(queryLower) ||
        card.currentBuild.lowercase().contains(queryLower) ||
        SpecType.fromName(card.currentBuild)?.let { spec ->
          spec.name.replace("_", " ").lowercase().contains(queryLower) ||
          spec.trees.any { tree ->
            tree.name.replace("_", " ").lowercase().contains(queryLower) ||
            localizedSkillTreeNames[tree]?.lowercase()?.contains(queryLower) == true
          }
        } == true ||
        card.sessionSpellDamageMap.keys.any { it.lowercase().contains(queryLower) } ||
        card.sessionSpellHealMap.keys.any { it.lowercase().contains(queryLower) } ||
        card.sessionSpellCCMap.keys.any { it.lowercase().contains(queryLower) })
    }
    if (filteredCards.isEmpty()) {
      _graphData.value = BattleGraphData()
      return
    }

    // Determine if the query matches any spell (not just player name/class)
    // If so, we can filter edges by spell breakdown. If not, show all edges for matched cards.
    val queryMatchesSpell = queryLower.isNotEmpty() && filteredCards.any { card ->
      card.sessionSpellDamageMap.keys.any { it.lowercase().contains(queryLower) } ||
      card.sessionSpellHealMap.keys.any { it.lowercase().contains(queryLower) } ||
      card.sessionSpellCCMap.keys.any { it.lowercase().contains(queryLower) }
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
          if (damage >= damageThresholdMin) {
            val breakdown = sourceCard.sessionDamageToPlayerBySpell[targetName] ?: emptyMap()
            // Only filter breakdown by search if query matches a spell name
            val filteredBreakdown = if (queryMatchesSpell) {
              breakdown.filterKeys { it.lowercase().contains(queryLower) }
            } else breakdown
            if (filteredBreakdown.isEmpty() && queryMatchesSpell) return@forEach
            val killCount = sourceCard.sessionKillsToPlayer[targetName] ?: 0
            val displayValue = if (killCount > 0) {
              "${damage.humanReadableAbbreviation()} dmg ($killCount kills)"
            } else {
              "${damage.humanReadableAbbreviation()} dmg"
            }
            edges.add(GraphEdge(
              source = sourceCard.name,
              target = targetName,
              weight = damage,
              displayValue = displayValue,
              spellBreakdown = filteredBreakdown
            ))
          }
        }
      }
    }
    BattleGraphMode.HEALS -> {
      filteredCards.forEach { sourceCard ->
        sourceCard.sessionHealToPlayer.forEach { (targetName, heals) ->
          if (heals >= healThresholdMin) {
            val breakdown = sourceCard.sessionHealToPlayerBySpell[targetName] ?: emptyMap()
            val filteredBreakdown = if (queryMatchesSpell) {
              breakdown.filterKeys { it.lowercase().contains(queryLower) }
            } else breakdown
            if (filteredBreakdown.isEmpty() && queryMatchesSpell) return@forEach
            edges.add(GraphEdge(
              source = sourceCard.name,
              target = targetName,
              weight = heals,
              displayValue = "${heals.humanReadableAbbreviation()} heal",
              spellBreakdown = filteredBreakdown
            ))
          }
        }
      }
    }
    BattleGraphMode.CC -> {
      filteredCards.forEach { sourceCard ->
        sourceCard.sessionCCToPlayer.forEach { (targetName, cc) ->
          if (cc >= ccThresholdMin) {
            val breakdown = sourceCard.sessionCCToPlayerBySpell[targetName]?.mapValues { it.value.toLong() } ?: emptyMap()
            val filteredBreakdown = if (queryMatchesSpell) {
              breakdown.filterKeys { it.lowercase().contains(queryLower) }
            } else breakdown
            if (filteredBreakdown.isEmpty() && queryMatchesSpell) return@forEach
            edges.add(GraphEdge(
              source = sourceCard.name,
              target = targetName,
              weight = cc.toLong(),
              displayValue = "$cc CC",
              spellBreakdown = filteredBreakdown
            ))
          }
        }
      }
    }
    BattleGraphMode.KILLS -> {
      filteredCards.forEach { sourceCard ->
        sourceCard.sessionKillsToPlayer.forEach { (targetName, kills) ->
          if (kills >= killThresholdMin) {
            val breakdown = sourceCard.sessionKillsToPlayerBySpell[targetName] ?: emptyMap()
            val filteredBreakdown = if (queryMatchesSpell) {
              breakdown.filterKeys { it.lowercase().contains(queryLower) }
            } else breakdown
            if (filteredBreakdown.isEmpty() && queryMatchesSpell) return@forEach
            edges.add(GraphEdge(
              source = sourceCard.name,
              target = targetName,
              weight = kills.toLong(),
              displayValue = "$kills kill${if (kills != 1) "s" else ""}",
              spellBreakdown = filteredBreakdown
            ))
          }
        }
      }
    }
    BattleGraphMode.BUFFS -> {
      filteredCards.forEach { sourceCard ->
        sourceCard.sessionBuffToPlayer.forEach { (targetName, buffs) ->
          val breakdown = sourceCard.sessionBuffToPlayerBySpell[targetName]?.mapValues { it.value.toLong() } ?: emptyMap()
          // Apply both search query (only if it matches a spell) and spell dropdown filter
          val searchFiltered = if (queryMatchesSpell) {
            breakdown.filterKeys { it.lowercase().contains(queryLower) }
          } else breakdown
          val filtered = selectedBuffSpell?.let { spell -> searchFiltered.filterKeys { it == spell } } ?: searchFiltered
          if (filtered.isNotEmpty()) {
            val filteredCount = filtered.values.sum()
            if (filteredCount >= buffThresholdMin) {
              edges.add(GraphEdge(
                source = sourceCard.name,
                target = targetName,
                weight = filteredCount,
                displayValue = if (selectedBuffSpell != null || queryMatchesSpell) "$filteredCount buff${if (filteredCount != 1L) "s" else ""}" else "$buffs buff${if (buffs != 1) "s" else ""}",
                spellBreakdown = filtered
              ))
            }
          }
        }
      }
    }
    BattleGraphMode.DEBUFFS -> {
      filteredCards.forEach { sourceCard ->
        sourceCard.sessionDebuffToPlayer.forEach { (targetName, debuffs) ->
          val breakdown = sourceCard.sessionDebuffToPlayerBySpell[targetName]?.mapValues { it.value.toLong() } ?: emptyMap()
          // Apply both search query (only if it matches a spell) and spell dropdown filter
          val searchFiltered = if (queryMatchesSpell) {
            breakdown.filterKeys { it.lowercase().contains(queryLower) }
          } else breakdown
          val filtered = selectedDebuffSpell?.let { spell -> searchFiltered.filterKeys { it == spell } } ?: searchFiltered
          if (filtered.isNotEmpty()) {
            val filteredCount = filtered.values.sum()
            if (filteredCount >= debuffThresholdMin) {
              edges.add(GraphEdge(
                source = sourceCard.name,
                target = targetName,
                weight = filteredCount,
                displayValue = if (selectedDebuffSpell != null || queryMatchesSpell) "$filteredCount debuff${if (filteredCount != 1L) "s" else ""}" else "$debuffs debuff${if (debuffs != 1) "s" else ""}",
                spellBreakdown = filtered
              ))
            }
          }
        }
      }
    }
    BattleGraphMode.CHARMS -> {
      filteredCards.forEach { sourceCard ->
        sourceCard.sessionCharmToPlayer.forEach { (targetName, charms) ->
          if (charms >= charmThresholdMin) {
            val breakdown = sourceCard.sessionCharmToPlayerBySpell[targetName]?.mapValues { it.value.toLong() } ?: emptyMap()
            val filteredBreakdown = if (queryMatchesSpell) {
              breakdown.filterKeys { it.lowercase().contains(queryLower) }
            } else breakdown
            if (filteredBreakdown.isEmpty() && queryMatchesSpell) return@forEach
            edges.add(GraphEdge(
              source = sourceCard.name,
              target = targetName,
              weight = charms.toLong(),
              displayValue = "$charms charm${if (charms != 1) "s" else ""}",
              spellBreakdown = filteredBreakdown
            ))
          }
        }
      }
    }
    BattleGraphMode.DISTRESS -> {
      filteredCards.forEach { sourceCard ->
        sourceCard.sessionDistressToPlayer.forEach { (targetName, distress) ->
          if (distress >= distressThresholdMin) {
            val breakdown = sourceCard.sessionDistressToPlayerBySpell[targetName]?.mapValues { it.value.toLong() } ?: emptyMap()
            val filteredBreakdown = if (queryMatchesSpell) {
              breakdown.filterKeys { it.lowercase().contains(queryLower) }
            } else breakdown
            if (filteredBreakdown.isEmpty() && queryMatchesSpell) return@forEach
            edges.add(GraphEdge(
              source = sourceCard.name,
              target = targetName,
              weight = distress.toLong(),
              displayValue = "$distress distress${if (distress != 1) "es" else ""}",
              spellBreakdown = filteredBreakdown
            ))
          }
        }
      }
    }
    BattleGraphMode.SILENCE -> {
      filteredCards.forEach { sourceCard ->
        sourceCard.sessionSilenceToPlayer.forEach { (targetName, silence) ->
          if (silence >= silenceThresholdMin) {
            val breakdown = sourceCard.sessionSilenceToPlayerBySpell[targetName]?.mapValues { it.value.toLong() } ?: emptyMap()
            val filteredBreakdown = if (queryMatchesSpell) {
              breakdown.filterKeys { it.lowercase().contains(queryLower) }
            } else breakdown
            if (filteredBreakdown.isEmpty() && queryMatchesSpell) return@forEach
            edges.add(GraphEdge(
              source = sourceCard.name,
              target = targetName,
              weight = silence.toLong(),
              displayValue = "$silence silence${if (silence != 1) "s" else ""}",
              spellBreakdown = filteredBreakdown
            ))
          }
        }
      }
    }
  }

    val maxValue = edges.maxOfOrNull { it.weight } ?: 1L

    // Select the strongest edges first, but keep the reverse edge whenever it
    // exists. Otherwise, maxEdges can select only one direction of a pair and
    // make an otherwise bidirectional relationship appear one-way.
    val edgesByDirection = edges.associateBy { it.source to it.target }
    val selectedEdges = edges
      .sortedByDescending { it.weight }
      .take(maxEdges.coerceAtLeast(1))
      .toMutableList()

    selectedEdges.toList().forEach { edge ->
      val reverseEdge = edgesByDirection[edge.target to edge.source]
      if (reverseEdge != null && reverseEdge !in selectedEdges) {
        selectedEdges.add(reverseEdge)
      }
    }

    val normalizedEdges = selectedEdges.map { edge ->
      val normalized = (edge.weight.toFloat() / maxValue).coerceIn(0.1f, 1f)
      edge.copy(normalizedWeight = normalized)
    }

    val activeNodeNames = normalizedEdges.flatMap { listOf(it.source, it.target) }.toSet()

    // Load edge targets from the local database when they aren't already in the
    // in-memory card cache.  This gives us faction / gear-score / spec data for
    // players who haven't produced any tracked event themselves.
    val knownNames = cards.associateBy { it.name }
    activeNodeNames.filter { it !in knownNames }.forEach { name ->
      PlayerCacheInteractor.loadPlayerFromDbIfExists(name)
    }

    // Add one-hop neighbors that aren't in the filtered set.
    // A target may not have a PlayerCard yet if it has not produced any
    // tracked event, but it must still have a node so the relationship edge
    // can be rendered immediately.
    val allCardsByName = cards.associateBy { it.name }.toMutableMap()
    // Merge in any cards we just loaded from the database
    activeNodeNames.forEach { name ->
      if (name !in allCardsByName) {
        PlayerCacheInteractor.observeCard(name).value?.let { allCardsByName[name] = it }
      }
    }
    activeNodeNames.forEach { name ->
      if (name !in nodeMap) {
        val card = allCardsByName[name]
        if (card != null) {
          val spec = SpecType.fromName(card.currentBuild)
          val faction = Faction.fromString(card.lastKnownFaction)
          nodeMap[name] = GraphNode(
            name = name,
            spec = spec,
            gearScore = card.lastKnownGearScore,
            faction = faction
          )
        } else {
          nodeMap[name] = GraphNode(
            name = name,
            spec = null,
            gearScore = 0,
            faction = Faction.UNKNOWN
          )
        }
      }
    }

    val activeNodes = nodeMap.values.filter { it.name in activeNodeNames }

    initializePositions(activeNodes)

    _graphData.value = BattleGraphData(
      nodes = activeNodes,
      edges = normalizedEdges,
      maxValue = maxValue
    )

    // Run tech analysis on all real cards in the graph
    if (_showTechAnalysis.value || _showMvpOnly.value) {
      val allCardsByName = cards.associateBy { it.name }
      val graphCards = activeNodeNames.mapNotNull { allCardsByName[it] }
      val result = TechnicalAnalysisHelper.analyze(graphCards)
      _techAnalysisResult.value = result.copy(
        edgeHeuristics = result.edgeHeuristics.filter {
          it.category == mode.name && (
            (_showTechAnalysis.value && !it.isMvp) || (_showMvpOnly.value && it.isMvp)
          )
        },
        nodeHeuristics = result.nodeHeuristics.filter {
          it.category == mode.name && (
            (_showTechAnalysis.value && !it.isMvp) || (_showMvpOnly.value && it.isMvp)
          )
        }
      )
    } else {
      _techAnalysisResult.value = TechAnalysisResult()
    }
  }

  private fun initializePositions(nodes: List<GraphNode>) {
    val count = nodes.size
    if (count == 0) return
    val radius = 200f * sqrt(count.toFloat())
    nodes.forEachIndexed { index, node ->
      val angle = 2.0 * Math.PI * index / count + Random.nextFloat() * 0.5f
      val jitter = 0.7f + Random.nextFloat() * 0.6f
      node.x = (radius * jitter * kotlin.math.cos(angle)).toFloat()
      node.y = (radius * jitter * kotlin.math.sin(angle)).toFloat()
      node.vx = 0f
      node.vy = 0f
    }
  }
}
