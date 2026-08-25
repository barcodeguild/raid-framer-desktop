package com.reoky.raidframer.core.helpers

import com.reoky.raidframer.core.definitions.MetaSpecsRepo
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.model.EdgeHeuristic
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.NodeHeuristic
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.TechAnalysisResult
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.tech_cc_rival
import raid_framer_desktop.composeapp.generated.resources.tech_didnt_charm
import raid_framer_desktop.composeapp.generated.resources.tech_distress_combo
import raid_framer_desktop.composeapp.generated.resources.tech_focused_target
import raid_framer_desktop.composeapp.generated.resources.tech_heal_focus
import raid_framer_desktop.composeapp.generated.resources.tech_heal_loop
import raid_framer_desktop.composeapp.generated.resources.tech_high_dmg_no_kills
import raid_framer_desktop.composeapp.generated.resources.tech_mvp_cc_format
import raid_framer_desktop.composeapp.generated.resources.tech_mvp_dps_format
import raid_framer_desktop.composeapp.generated.resources.tech_mvp_heals_format
import raid_framer_desktop.composeapp.generated.resources.tech_needs_heals
import raid_framer_desktop.composeapp.generated.resources.tech_spell_dominance
import raid_framer_desktop.composeapp.generated.resources.tech_no_silences
import raid_framer_desktop.composeapp.generated.resources.tech_no_tiger_strikes
import raid_framer_desktop.composeapp.generated.resources.tech_no_defiance

object TechnicalAnalysisHelper {

  private const val HEAL_CIRCULAR_THRESHOLD = 50_000L
  private const val HEAL_CIRCULAR_RATIO = 0.10f
  private const val HEAL_FOCUS_THRESHOLD = 50_000L
  private const val HEAL_FOCUS_RATIO = 0.10f
  private const val DAMAGE_FOCUSED_THRESHOLD = 150_000L
  private const val DAMAGE_FOCUSED_RATIO = 0.10f
  private const val HIGH_DAMAGE_THRESHOLD = 500_000L
  private const val SPELL_DOMINANCE_DAMAGE_THRESHOLD = 300_000L
  private const val NEEDS_HEALS_THRESHOLD = 0.5f // ony 50% of received damage healed
  private const val CC_RIVAL_THRESHOLD = 20
  private const val CC_RIVAL_RATIO = 0.03f // CC applied to target must be at least 3% of source's total CC

  // for our nation's cats of course ~
  // I mean category ~
  private const val CAT_HEALS = "HEALS"
  private const val CAT_DAMAGE = "DAMAGE"
  private const val CAT_CC = "CC"
  private const val CAT_ALL = "ALL"

  // I might want to move these into a spell definition and internationalize them but this requires launching the game
  // in Korean again which I really don't want to do right now
  private val CRASHING_WAVE_SPELLS = setOf("Crashing Wave", "심연의 파동", "深渊波动", "Сгустки гнева")
  private val ARC_LIGHTNING_SPELLS = setOf("Arc Lightning", "분노의 벼락", "闪电术", "Молния гнева")
  private val METEOR_STRIKE_SPELLS = setOf("Meteor Strike", "별똥별: 파도", "裂空星陨", "Метеор")

  private val SPELL_DOMINANCE_SPELLS = CRASHING_WAVE_SPELLS + ARC_LIGHTNING_SPELLS + METEOR_STRIKE_SPELLS

  fun analyze(cards: List<PlayerCard>): TechAnalysisResult {
    val cardsByName = cards.associateBy { it.name }
    val edgeHeuristics = mutableListOf<EdgeHeuristic>()
    val nodeHeuristics = mutableListOf<NodeHeuristic>()

    analyzeHealing(cards, cardsByName, edgeHeuristics, nodeHeuristics)
    analyzeDPS(cards, cardsByName, edgeHeuristics, nodeHeuristics)
    analyzeCC(cards, cardsByName, edgeHeuristics, nodeHeuristics)

    // Skill-tree heuristics: show on ALL graph modes for any real player
    cards.filter { it.isRealPlayer }.forEach { card ->
      val spec = SpecType.fromName(card.currentBuild) ?: return@forEach

      // Didn't Charm? - has Songcraft but 0 charms
      if (SkillTreeType.SONGCRAFT in spec.trees && card.sessionCharmTotal == 0) {
        nodeHeuristics.add(NodeHeuristic(card.name, Res.string.tech_didnt_charm, color = RFColors.techDidntCharm, category = CAT_ALL))
      }

      // No Tiger Strikes? - has Battlerage but 0 tiger strikes
      if (SkillTreeType.BATTLERAGE in spec.trees && card.sessionTigerStrikeTotal == 0) {
        nodeHeuristics.add(NodeHeuristic(card.name, Res.string.tech_no_tiger_strikes, color = RFColors.techNoTigerStrikes, category = CAT_ALL))
      }

      // No Silence? - has Witchcraft but 0 silences
      if (SkillTreeType.WITCHCRAFT in spec.trees && card.sessionSilenceTotal == 0) {
        nodeHeuristics.add(NodeHeuristic(card.name, Res.string.tech_no_silences, color = RFColors.techDistressCombo, category = CAT_ALL))
      }

      // Distress Combo? - has Defense but 0 distress
      if (SkillTreeType.DEFENSE in spec.trees && card.sessionDistressTotal == 0) {
        nodeHeuristics.add(NodeHeuristic(card.name, Res.string.tech_distress_combo, color = RFColors.techDistressCombo, category = CAT_ALL))
      }
    }

    // No Defiance? - no regular or garden defiance this session
    cards.filter { it.isRealPlayer && it.sessionDefianceTotal == 0 && it.sessionGardenDefianceTotal == 0 }.forEach { card ->
      nodeHeuristics.add(NodeHeuristic(card.name, Res.string.tech_no_defiance, color = RFColors.defianceGold, category = CAT_ALL))
    }

    return TechAnalysisResult(
      edgeHeuristics = edgeHeuristics,
      nodeHeuristics = nodeHeuristics
    )
  }

  // TA for Heals Graphs
  private fun analyzeHealing(
    cards: List<PlayerCard>,
    cardsByName: Map<String, PlayerCard>,
    edgeHeuristics: MutableList<EdgeHeuristic>,
    nodeHeuristics: MutableList<NodeHeuristic>
  ) {
    val healers = cards.filter { card ->
      card.isRealPlayer && SpecType.fromName(card.currentBuild) in MetaSpecsRepo.current.healer
    }

    // Heuristic - Circular healing loop (two friends healing each others)
    for (i in healers.indices) {
      for (j in i + 1 until healers.size) {
        val a = healers[i]
        val b = healers[j]
        val aHealsB = a.sessionHealToPlayer[b.name] ?: 0L
        val bHealsA = b.sessionHealToPlayer[a.name] ?: 0L
        if (aHealsB >= HEAL_CIRCULAR_THRESHOLD && bHealsA >= HEAL_CIRCULAR_THRESHOLD
          && (a.sessionHealTotal <= 0 || aHealsB.toFloat() / a.sessionHealTotal >= HEAL_CIRCULAR_RATIO)
          && (b.sessionHealTotal <= 0 || bHealsA.toFloat() / b.sessionHealTotal >= HEAL_CIRCULAR_RATIO)
        ) {
          edgeHeuristics.add(EdgeHeuristic(a.name, b.name, Res.string.tech_heal_loop, RFColors.techHealLoop, category = CAT_HEALS))
          edgeHeuristics.add(EdgeHeuristic(b.name, a.name, Res.string.tech_heal_loop, RFColors.techHealLoop, category = CAT_HEALS))
        }
      }
    }

    // Heuristic - Healer focused on tank/dancer
    healers.forEach { healer ->
      val totalHeals = healer.sessionHealTotal
      if (totalHeals <= 0) return@forEach

      healer.sessionHealToPlayer.forEach { (targetName, healAmount) ->
        if (healAmount < HEAL_FOCUS_THRESHOLD) return@forEach
        val ratio = healAmount.toFloat() / totalHeals
        if (ratio < HEAL_FOCUS_RATIO) return@forEach

        val targetCard = cardsByName[targetName] ?: return@forEach
        val targetSpec = SpecType.fromName(targetCard.currentBuild)
        if (targetSpec in MetaSpecsRepo.current.cc) {
          edgeHeuristics.add(EdgeHeuristic(healer.name, targetName, Res.string.tech_heal_focus, RFColors.techHealFocus, category = CAT_HEALS))
        } else if (targetSpec in MetaSpecsRepo.current.dancer) {
          edgeHeuristics.add(EdgeHeuristic(healer.name, targetName, Res.string.tech_heal_focus, RFColors.techHealFocus, category = CAT_HEALS))
        }
      }
    }

    // Heuristic - tanks/dancers not receiving enough heals
    cards.filter { card ->
      card.isRealPlayer && SpecType.fromName(card.currentBuild) in (MetaSpecsRepo.current.cc + MetaSpecsRepo.current.dancer)
    }.forEach { card ->
      if (card.sessionHealsReceivedTotal < card.sessionDamageTakenTotal * NEEDS_HEALS_THRESHOLD) {
        nodeHeuristics.add(NodeHeuristic(card.name, Res.string.tech_needs_heals, RFColors.techNeedsHeals, category = CAT_HEALS))
      }
    }

    // Heuristic - MVP Heals Top 3 per faction
    val realCards = cards.filter { it.isRealPlayer }
    listOf(Faction.HARANYA, Faction.NUIA, Faction.PIRATE).forEach { faction ->
      val top3 = realCards
        .filter { Faction.fromString(it.lastKnownFaction) == faction && it.sessionHealTotal > 0 }
        .sortedByDescending { it.sessionHealTotal }
        .take(3)
      top3.forEachIndexed { index, card ->
        nodeHeuristics.add(
          NodeHeuristic(
            card.name,
            Res.string.tech_mvp_heals_format,
            RFColors.techMvpHeals,
            listOf(index + 1),
            isMvp = true,
            category = CAT_HEALS,
            playerFaction = faction
          )
        )
      }
    }
  }

  // TA for DPS Graphs
  private fun analyzeDPS(
    cards: List<PlayerCard>,
    cardsByName: Map<String, PlayerCard>,
    edgeHeuristics: MutableList<EdgeHeuristic>,
    nodeHeuristics: MutableList<NodeHeuristic>
  ) {
    val dpsSpecs = MetaSpecsRepo.current.mage + MetaSpecsRepo.current.melee + MetaSpecsRepo.current.ranged

    cards.filter { card ->
      card.isRealPlayer && SpecType.fromName(card.currentBuild) in dpsSpecs
    }.forEach { card ->

      // Heuristic - Focused Target - 150k+ damage to a single target, at least 10% of total damage
      card.sessionDamageToPlayer.forEach { (targetName, damage) ->
        if (damage >= DAMAGE_FOCUSED_THRESHOLD
          && (card.sessionDamageTotal <= 0 || damage.toFloat() / card.sessionDamageTotal >= DAMAGE_FOCUSED_RATIO)
        ) {
          edgeHeuristics.add(
            EdgeHeuristic(
              card.name,
              targetName,
              Res.string.tech_focused_target,
              color = RFColors.techFocusedTarget,
              category = CAT_DAMAGE
            )
          )
        }
      }

      // Heuristic -  High Damage No Kills - 500k+ damage but 0 kills
      if (card.sessionDamageTotal >= HIGH_DAMAGE_THRESHOLD && card.sessionKillTotal == 0) {
        nodeHeuristics.add(
          NodeHeuristic(
            card.name,
            Res.string.tech_high_dmg_no_kills,
            color = RFColors.techHighDmgNoKills,
            category = CAT_DAMAGE
          )
        )
      }

      // Heuristic - Spell Dominance - mage/ranged whose damage comes primarily from key spells
      val spec = SpecType.fromName(card.currentBuild)
      if (spec != null && (spec in MetaSpecsRepo.current.mage || spec in MetaSpecsRepo.current.ranged)) {
        val totalDmg = card.sessionDamageTotal
        if (totalDmg > 0) {
          val spellDmg = card.sessionSpellDamageMap.entries
            .filter { (spell, _) -> spell in SPELL_DOMINANCE_SPELLS }
            .sumOf { it.value }

          // significant damage with high-damage channeled spells like wave meteor, arc, etc
          // we put a Spell Dominance badge on the player
          if (spellDmg >= SPELL_DOMINANCE_DAMAGE_THRESHOLD) {
            nodeHeuristics.add(
              NodeHeuristic(
                card.name,
                Res.string.tech_spell_dominance,
                color = RFColors.techSpellDominance,
                category = CAT_DAMAGE
              )
            )
          }
        }
      }
    }

    // Heuristic - MVP DPS Top 3 per faction
    val realCards = cards.filter { it.isRealPlayer }
    listOf(Faction.HARANYA, Faction.NUIA, Faction.PIRATE).forEach { faction ->
      val top3 = realCards
        .filter { Faction.fromString(it.lastKnownFaction) == faction && it.sessionDamageTotal > 0 }
        .sortedByDescending { it.sessionDamageTotal }
        .take(3)
      top3.forEachIndexed { index, card ->
        nodeHeuristics.add(
          NodeHeuristic(
            card.name,
            Res.string.tech_mvp_dps_format,
            RFColors.techMvpDps,
            listOf(index + 1),
            isMvp = true,
            category = CAT_DAMAGE,
            playerFaction = faction
          )
        )
      }
    }
  }

  // TA for CC Graph
  private fun analyzeCC(
    cards: List<PlayerCard>,
    cardsByName: Map<String, PlayerCard>,
    edgeHeuristics: MutableList<EdgeHeuristic>,
    nodeHeuristics: MutableList<NodeHeuristic>
  ) {
    // Heuristic  MVP CC Top 3 per faction
    val realCards = cards.filter { it.isRealPlayer }
    listOf(Faction.HARANYA, Faction.NUIA, Faction.PIRATE).forEach { faction ->
      val top3 = realCards
        .filter { Faction.fromString(it.lastKnownFaction) == faction && it.sessionCCTotal > 0 }
        .sortedByDescending { it.sessionCCTotal }
        .take(3)
      top3.forEachIndexed { index, card ->
        nodeHeuristics.add(
          NodeHeuristic(
            card.name,
            Res.string.tech_mvp_cc_format,
            RFColors.techMvpCc,
            listOf(index + 1),
            isMvp = true,
            category = CAT_CC,
            playerFaction = faction
          )
        )
      }
    }

    // Heuristic - CC Rival? - someone applying significant CC stacks to a CC tank (IE they are fighting back against cc with cc)
    val ccTanks = cards.filter { card ->
      card.isRealPlayer && SpecType.fromName(card.currentBuild) in MetaSpecsRepo.current.cc
    }
    cards.filter { it.isRealPlayer }.forEach { source ->
      val totalCC = source.sessionCCTotal
      if (totalCC <= 0) return@forEach
      ccTanks.forEach { tank ->
        if (source.name == tank.name) return@forEach
        val ccApplied = source.sessionCCToPlayer[tank.name] ?: 0
        val ratio = ccApplied.toFloat() / totalCC
        if (ccApplied >= CC_RIVAL_THRESHOLD && ratio >= CC_RIVAL_RATIO) {
          edgeHeuristics.add(
            EdgeHeuristic(
              source.name,
              tank.name,
              Res.string.tech_cc_rival,
              color = RFColors.techNeedsHeals,
              category = CAT_CC
            )
          )
        }
      }
    }
  }
}
